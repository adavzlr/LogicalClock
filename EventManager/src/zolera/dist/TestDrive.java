package zolera.dist;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;
import java.util.TimerTask;

import zolera.dist.EventManager.Event;
import zolera.dist.EventManager.EventHandler;

public class TestDrive extends TimerTask {
	
	private static int endCount = 0;
	
	private int[] ports = {1521, 1522, 1523, 1524};
	private String[] names = {"P1","P2","P3","P4"};
	private int currentProcess;
	private int eventCounter;
	
	private static Event [] validEvents = {	Event.ACKNOWLEDGE_CS, 
											Event.EXIT,
											Event.JOIN,
											Event.PLAY,
											Event.RELEASE_CS,
											Event.REQUEST_CS,
											Event.REQUEST_MASTERSHIP,
											Event.STOP,
											Event.TRANSFER_MASTERSHIP,
											Event.VOLUME,
											Event.EVENT_1,
											Event.EVENT_2,
											Event.EVENT_3
											};
	
	public TestDrive(int proc)
	{
		CommunicationsManager.initialize(names[proc], ports[proc]);
		currentProcess = proc;
		addRecipients();
	}
	
	private void addRecipients() {
		try{
			for(int i = 0; i < ports.length; i++) {
				if(i != currentProcess)
					CommunicationsManager.addReceiver("P"+i, InetAddress.getLocalHost(), ports[i]);
			}
		}
		catch(UnknownHostException uhe) {
			CommunicationsManager.destroy();
			throw new IllegalStateException("Illegal host");
		}
	}
	
	@Override
	public void run() {
		Random randomGen = new Random();
		int randomProcess = randomGen.nextInt(ports.length);
		Event randomMessage = validEvents[randomGen.nextInt(validEvents.length)];
		EventMessage msg = new EventMessage(randomMessage);
		CommunicationsManager.send(names[randomProcess], msg);
		
	}
	
	public static void main(String [] args)
	{
		Scanner scan = new Scanner(System.in);
		int proc= scan.nextInt();
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
			TestDrive.endCount++;
		}
	};
}
