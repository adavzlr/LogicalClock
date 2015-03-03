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
	private static DatagramSocket socket = null;
	private static Thread listener;
	private static Map<String, Receiver> receivers = new HashMap<>();
	private static final int MAX_BUFFER_LENGTH = 1400;
	
	
	
	// Cannot instantiate this class
	private CommunicationsManager() {}
	
	// Sets the name and port for our communications channel and start a listener thread
	public static void initialize(String ourName, int port) {
		if (ourName == null || !ourName.matches("\\w{1,10}") || port < 0)
			throw new IllegalArgumentException("name '"+ourName+"'");
		
		// initialize name, socket and listener thread
		try {
			name     = ourName;
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
		return name;
	}
	
	
	
	// Adds new receiver to the list and starts listening for incoming messages
	public static void addReceiver(String rcvname, InetAddress addr, int port) {
		if (name == null || socket == null)
			throw new IllegalStateException("Cannot add until we have a name set");
		
		if (rcvname == null || !rcvname.matches("\\w{1,10}") || addr == null || port <= 0)
			throw new IllegalArgumentException("name '" + rcvname + "', port " + port + ", addr " + addr);
		
		Receiver receiver = new Receiver();
		receiver.name     = rcvname;
		receiver.address  = addr;
		receiver.port     = port;
		
		receivers.put(rcvname, receiver);
	}
	
	// Removes a receiver from the list and stops its listening thread
	public static void removeReceiver(String rcvname) {
		if (name == null || socket == null)
			throw new IllegalStateException("Cannot remove until we have a name set");
		
		Receiver receiver = receivers.remove(rcvname);
		if (receiver == null)
			throw new IllegalArgumentException("Name '"+rcvname+"'");
	}
	
	// return whether the comms mgr knows about a receiver
	public static boolean knows(String rcvname) {
		if (name == null || socket == null)
			throw new IllegalStateException("Cannot query until we have a name set");
		
		Receiver receiver = receivers.get(rcvname);
		if (receiver == null)
			return false;
		else
			return true;
	}
	
	
	
	// Sends a message to a specific receiver
	public static void send(String rcvname, EventMessage msg) {
		if (name == null || socket == null)
			throw new IllegalStateException("Cannot send until we have a name set");
		
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
	
	// Prepares a message for transmission (also marshalls it into a string)
	private static String prepare(EventMessage msg) {
		// Update EventManager clock and log and timestamp message
		if (!msg.getEvent().isStealth())
			EventManager.stamp(msg);
		
		// marshall message to string and return it
		String text = msg.marshall();
		return text;
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
		public InetAddress address;
		public int port;
	}
}
