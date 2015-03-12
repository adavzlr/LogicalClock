package zolera.dist;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CommunicationsManager {
	private static String name = null;
	private static int id = -1;
	private static DatagramSocket socket = null;
	private static Thread listener;
	private static Map<String, Receiver> receivers = new HashMap<>();
	private static final int MAX_BUFFER_LENGTH = 1400;
	
	
	
	// Cannot instantiate this class
	private CommunicationsManager() {}
	
	// Sets the name and port for our communications channel and start a listener thread
	public static void initialize(String ourName, int ourId, int port) {
		if (ourName == null || !ourName.matches("\\w{1,10}") || port < 0 || ourId < 0)
			throw new IllegalArgumentException("name '"+ourName+"', "+ourId+", "+port);
		
		// initialize name, socket and listener thread
		try {
			name     = ourName;
			id       = ourId;
			socket   = new DatagramSocket(port);
			listener = new Thread(new ListenerRunnable());
		} catch (SocketException se) {
			se.printStackTrace();
		}
		
		// start listener thread
		listener.start();
	}
	
	// Close our communication channel and stop the listener thread
	public static void destroy() {
		if (name == null)
			throw new IllegalStateException("Haven't initialized");
		
		// Close our socket
		socket.close();
		
		// Stop listener thread
		listener.interrupt();
		while(listener.isAlive()) {
			try {
				listener.join();
			}
			catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		
		// Remove all receivers
		receivers.clear();
	}
	
	public static String getName() {
		if (name == null)
			throw new IllegalStateException("Haven't initialized");
		
		return name;
	}
	
	
	
	// Adds new receiver to the list and starts listening for incoming messages
	public static void addReceiver(String rcvname, int rcvid, InetAddress addr, int port) {
		if (name == null)
			throw new IllegalStateException("Haven't initialized");
		
		if (rcvname == null || !rcvname.matches("\\w{1,10}") || rcvname.equals("All") || addr == null || port <= 0)
			throw new IllegalArgumentException("name '" + rcvname + "', port " + port + ", addr " + addr);
		
		Receiver receiver = new Receiver();
		receiver.name     = rcvname;
		receiver.id       = rcvid;
		receiver.address  = addr;
		receiver.port     = port;
		
		receivers.put(rcvname, receiver);
	}
	
	// Removes a receiver from the list and stops its listening thread
	public static void removeReceiver(String rcvname) {
		if (name == null)
			throw new IllegalStateException("Haven't initialized");
		
		Receiver receiver = receivers.remove(rcvname);
		if (receiver == null)
			throw new IllegalArgumentException("Name '"+rcvname+"'");
	}
	
	// How many receivers are registered
	public static int receiversCount() {
		if (name == null)
			throw new IllegalStateException("Haven't initialized");
		
		return receivers.size();
	}
	
	// return whether the comms mgr knows about a receiver
	public static boolean knows(String rcvname) {
		if (name == null)
			throw new IllegalStateException("Haven't initialized");
		
		Receiver receiver = receivers.get(rcvname);
		if (receiver == null)
			return false;
		else
			return true;
	}
	
	// which id corresponds to a receiver
	public static int getReceiverId(String rcvname) {
		if (name == null)
			throw new IllegalStateException("Haven't initialized");
		
		if (rcvname == null || (!rcvname.equals(name) && receivers.get(rcvname) == null))
			throw new IllegalArgumentException("Invalid name "+rcvname);
		
		if (rcvname.equals(name))
			return id;
		else
			return receivers.get(rcvname).id;
	}
	
	
	
	// Broadcast a message to all registered receivers
	public static void sendAll(EventMessage msg) {
		if (name == null)
			throw new IllegalStateException("Haven't initialized");
		
		for (String rcvname : receivers.keySet()) {
			send(rcvname, msg);
		}
	}
	
	// Sends a message to a specific receiver
	public static void send(String rcvname, EventMessage msg) {
		if (name == null)
			throw new IllegalStateException("Haven't initialized");
		
		Receiver rcv = receivers.get(rcvname);
		if (rcv == null || msg == null || msg.getEvent() == null)
			throw new IllegalArgumentException("Name '"+rcvname+"', Message "+msg);
		
		// prepare message
		msg.setSender(name);
		msg.setReceiver(rcv.name);
		
		// transform message into a byte array
		byte[] buffer = prepare(msg).getBytes(StandardCharsets.UTF_8);
		if (buffer.length > MAX_BUFFER_LENGTH)
			throw new IllegalArgumentException("Message is too big: "+buffer.length);
		
		// send byte array through the socket
		try {
			DatagramPacket pkt = new DatagramPacket(buffer, buffer.length, rcv.address, rcv.port);
			socket.send(pkt);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	// Prepares a message for transmission (also marshalls it into a string)
	private static String prepare(EventMessage msg) {
		// Update EventManager clock and log and timestamp message
		if (!msg.getEvent().isStealth())
			EventManager.stamp(msg);
		
		// marshall message to string and return it
		String text = msg.marshall();
		return text;
	}
	
	
	
	// Receiving loop for a particular receiver. Gets messages and processes them.
	private static void receive() {
		byte[] buffer = new byte[MAX_BUFFER_LENGTH];
		int bufferlen = 0;
		
		// receive a byte array through the socket
		try {
			DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
			socket.receive(pkt);
			
			buffer    = pkt.getData();
			bufferlen = pkt.getLength();
		}
		catch (SocketException se) {
			return;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		// transform byte array into a message 
		EventMessage msg = process(new String(buffer, 0, bufferlen, StandardCharsets.UTF_8));
		if (receivers.get(msg.getSender()) == null || !msg.getReceiver().equals(name))
			throw new IllegalStateException("We '"+name+"', Sender '"+msg.getSender()+"', Receiver '"+msg.getReceiver()+"'");
		
		// Run any processing required by the received message
		if (msg.getEvent().getHandler() != null)
			msg.getEvent().getHandler().process(msg);
	}
	
	// Unmarshalls a received stream into a message and pre-processes it
	private static EventMessage process(String text) {
		// Find out which message type it is
		EventMessage msg = new EventMessage(null);
		msg.unmarshall(text);
		
		// If there's a special type of message for this event, create and re-unmarshall to the new message
		// At this point newMessage() returns an instance of the appropriate message type and polymorphism
		// takes care of appropriately unmarshalling the message.
		if (msg.getEvent().getHandler() != null) {
			msg = msg.getEvent().getHandler().newMessage();
			msg.unmarshall(text);
		}
		
		// Update EventManager clock and log
		if (!msg.getEvent().isStealth())
			EventManager.update(msg);
		
		return msg;
	}
	
	
	
	// A runnable object to create listening threads for new receivers
	private static class ListenerRunnable implements Runnable {
		@Override
		public void run() {
			while(!Thread.currentThread().isInterrupted()) {
				receive();
			}
		}
	}
	
	// small container for the information that defines a receiver
	private static class Receiver {
		public String name;
		public int id;
		public InetAddress address;
		public int port;
	}
}
