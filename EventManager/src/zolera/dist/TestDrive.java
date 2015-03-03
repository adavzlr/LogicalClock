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

	private static Scanner scan;
	private static int endCount = 0;
	private static boolean startFlag = false;
	private static final int eventLimit = 20;

	private Timer sendTimer;

	private int[] ports = { 1521, 1522, 1523, 1524 };
	private String[] names = { "P1", "P2", "P3", "P4" };
	private int currentProcess;
	private int eventCounter;

	private static Event[] validEvents = { Event.ACKNOWLEDGE_CS, Event.EXIT,
			Event.JOIN, Event.PLAY, Event.RELEASE_CS, Event.REQUEST_CS,
			Event.REQUEST_MASTERSHIP, Event.STOP, Event.TRANSFER_MASTERSHIP,
			Event.VOLUME, Event.EVENT_1, Event.EVENT_2, Event.EVENT_3 };

	/**
	 * Class constructor. Initializes which processes are we going to communicate with and also
	 * which process Id the current process has 
	 * @param proc
	 */
	public TestDrive(int proc) {
		CommunicationsManager.initialize(names[proc], ports[proc]);
		currentProcess = proc;
		eventCounter = 0;
		addRecipients();
		sendTimer = new Timer();
		sendTimer.schedule(this, 1000);
		if (currentProcess == 0) {
			startFlag = true;
			sendStartSignal();
		}
	}

	/**
	 * Tells everyones that we are ready to start sending events if we are the process with ID = 0
	 */
	private void sendStartSignal() {
		System.out.println("Say start when everyone is ready");
		String ready = scan.next();
		if (ready.equals("ready")) {
			for (int i = 1; i < ports.length; i++) {
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
					CommunicationsManager.addReceiver("P" + i,
							InetAddress.getLocalHost(), ports[i]);
			}
		} catch (UnknownHostException uhe) {
			CommunicationsManager.destroy();
			throw new IllegalStateException("Illegal host");
		}
	}

	/**
	 * 
	 */
	@Override
	public void run() {
		if (startFlag) {
			if (eventCounter < eventLimit) {
				Random randomGen = new Random();
				int randomProcess = randomGen.nextInt(ports.length - 1);
				if (randomProcess >= currentProcess)
					randomProcess++;
				Event randomMessage = validEvents[randomGen
						.nextInt(validEvents.length)];
				EventMessage msg = new EventMessage(randomMessage);
				switch (randomMessage) {
				case ACKNOWLEDGE_CS:
					msg = new MutexMessage("CS_" + randomGen.nextInt(30),
							Event.ACKNOWLEDGE_CS);
					break;
				case EXIT:
					break;
				case JOIN:
					break;
				case PLAY:
					msg = new ControlMessage(ControlMessage.INVALID_VOLUME,
							Event.PLAY);
					break;
				case RELEASE_CS:
					msg = new MutexMessage("CS_" + randomGen.nextInt(30),
							Event.RELEASE_CS);
					break;
				case REQUEST_CS:
					msg = new MutexMessage("CS_" + randomGen.nextInt(30),
							Event.REQUEST_CS);
					break;
				case REQUEST_MASTERSHIP:
					break;
				case STOP:
					msg = new ControlMessage(ControlMessage.INVALID_VOLUME,
							Event.STOP);
					break;
				case TRANSFER_MASTERSHIP:
					break;
				case VOLUME:
					msg = new ControlMessage(randomGen.nextInt(100),
							Event.VOLUME);
					break;
				case EVENT_1:
					break;
				case EVENT_2:
					break;
				case EVENT_3:
				default:
					throw new IllegalStateException("Invalid event");
				}
				CommunicationsManager.send(names[randomProcess], msg);
			} else {
				if (currentProcess != 0) {
					EventManager.sendLog(names[1]);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					EventMessage msg = new EventMessage(Event.EVENT_END);
					CommunicationsManager.send(names[0], msg);
					CommunicationsManager.destroy();
					scan.close();
				} else {
					if (endCount == names.length - 1) {
						System.out.println(EventManager.getLog());
						CommunicationsManager.destroy();
						scan.close();
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		scan = new Scanner(System.in);
		int proc = scan.nextInt();
		if (proc > 3 && proc < 0) {
			scan.close();
			throw new IllegalArgumentException("Illegal process number");
		}

	}

	public static final EventHandler HANDLER = new EventHandler() {
		@Override
		public EventMessage newMessage() {
			return new EventMessage(null);
		}

		@Override
		public void process(EventMessage msg) {
			if (msg.getEvent() == Event.EVENT_END)
				TestDrive.endCount++;
			else
				TestDrive.startFlag = true;
		}
	};
}
