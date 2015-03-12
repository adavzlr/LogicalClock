package zolera.dist;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import zolera.dist.EventManager.Event;
import zolera.dist.EventManager.EventHandler;

public class TestDrive extends TimerTask {
	
	/**
	 * Handler for TestDrive's event messages
	 */
	public static final EventHandler HANDLER = new EventHandler() {
		@Override
		public EventMessage newMessage() {
			return new EventMessage(null);
		}

		@Override
		public void process(EventMessage msg) {
			System.out.println("handler for " + msg.getEvent());
			if (msg.getEvent() == Event.EVENT_END)
				TestDrive.endCount++;
			else {
				TestDrive.startFlag = true;
				System.out.println("Start received");
			}
		}
	};
	
	private static Scanner scan;
	private static int endCount = 0; 			// Counts every message sent
	private static boolean startFlag = false;	// Tells if we are ready to start
	private static final int eventLimit = 5;   // Message limit 

	private Timer sendTimer; // Executes a block of code every tick

	private int[] ports = { 1521, 1522, 1523, 1524 };  		// Ports for each process
	private String[] names = { "P0", "P1", "P2", "P3" }; 	// Names for each process
	private int currentProcess; 					// Current process id
	private int eventCounter;						// Counts every event occurred

	/**
	 * List of valid events
	 */
	private static Event[] validEvents = { Event.EXIT, Event.JOIN, Event.PLAY,
			Event.REQUEST_MASTERSHIP, Event.STOP, Event.TRANSFER_MASTERSHIP,
			Event.VOLUME, Event.EVENT_1, Event.EVENT_2, Event.EVENT_3 };

	/**
	 * Class constructor. Initializes which processes are we going to communicate with and also
	 * which process Id the current process has 
	 * @param proc
	 */
	public TestDrive(int proc) {
		CommunicationsManager.initialize(names[proc], proc, ports[proc]);
		currentProcess = proc;
		eventCounter = 0;
		addRecipients();
		sendTimer = new Timer();
		sendTimer.schedule(this, 1000, 1000);
		System.out.println("Process " + this.currentProcess + " started");
		if (currentProcess == 0) {
			sendStartSignal();
			startFlag = true;
		}
	}

	/**
	 * Tells everyones that we are ready to start sending events if we are the process with ID = 0
	 */
	private void sendStartSignal() {
		System.out.println("Say ready when everyone is ready");
		String ready = scan.next();
		if (ready.equals("ready")) {
			for (int i = 1; i < ports.length; i++) {
				System.out.println("SENDING START MESSAGE TO P" + i);
				EventMessage msg = new EventMessage(Event.EVENT_START);
				CommunicationsManager.send("P" + i, msg);
			}
			startFlag = true;
		} else {
			sendStartSignal();
		}
	}

	/**
	 * Adds all the processes to a list which we can access any time to send event messages
	 */
	private void addRecipients() {
		try {
			for (int i = 0; i < ports.length; i++) {
				if (i != currentProcess)
					CommunicationsManager.addReceiver("P" + i, i,
							InetAddress.getLocalHost(), ports[i]);
			}
		} catch (UnknownHostException uhe) {
			CommunicationsManager.destroy();
			throw new IllegalStateException("Illegal host");
		}
	}

	/**
	 * Timer's method. Every tick finds a random recipient to send a new event message
	 */
	@Override
	public void run() {
		if (startFlag) {
			boolean sendFlag = true;
			if (eventCounter < eventLimit) {
				Random randomGen = new Random();
				int randomProcess = randomGen.nextInt(ports.length - 1);
				if (randomProcess >= currentProcess)
					randomProcess++;
				Event randomMessage = validEvents[randomGen.nextInt(validEvents.length)];
				EventMessage msg = null;
				
				switch (randomMessage) {
					case EXIT:
						msg = new PartyMessage(Event.EXIT);
						break;
					case JOIN:
						msg = new PartyMessage(Event.JOIN);
						break;
					case PLAY:
						msg = new ControlMessage(ControlMessage.INVALID_VOLUME, Event.PLAY);
						break;
					case REQUEST_MASTERSHIP:
						msg = new MastershipMessage(null, Event.REQUEST_MASTERSHIP);
						break;
					case STOP:
						msg = new ControlMessage(ControlMessage.INVALID_VOLUME,Event.STOP);
						break;
					case TRANSFER_MASTERSHIP:
						msg = new MastershipMessage(this.names[randomProcess], Event.TRANSFER_MASTERSHIP);
						break;
					case VOLUME:
						msg = new ControlMessage(randomGen.nextInt(100), Event.VOLUME);
						break;
					case EVENT_1:
					case EVENT_2:
					case EVENT_3:
						sendFlag = false;
						break;
					default:
						throw new IllegalStateException("Invalid event");
				}
				if(sendFlag){
					System.out.println("Sending message to " + names[randomProcess]);
					CommunicationsManager.send(names[randomProcess], msg);
				}
				else
					EventManager.record(randomMessage);
				eventCounter++;
			} else {
				if (currentProcess != 0) {  // Every process has finished sending event messages 
					EventManager.sendLog(names[0]);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					EventMessage msg = new EventMessage(Event.EVENT_END);
					CommunicationsManager.send(names[0], msg); // Send to process 0 the message log 
					CommunicationsManager.destroy();
					scan.close();
					sendTimer.cancel();
				} else {
					if (endCount == names.length - 1) {
						System.out.println("Finishing P0");
						System.out.println(EventManager.getLog()); // Print the master log 
						CommunicationsManager.destroy();
						scan.close();
						sendTimer.cancel();
					}
				}
			}
		}
	}

	/**
	 * Starts the application
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Who are you? (Process Id from 0 to 3)");
		scan = new Scanner(System.in);
		int proc = scan.nextInt();
		if (proc > 3 && proc < 0) {   // Ensure valid input
			scan.close();
			throw new IllegalArgumentException("Illegal process number");
		}
		new TestDrive(proc);
	}
}
