package zolera.dist;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import zolera.dist.EventManager.Event;

public class TestMain {

	public static void main(String[] args) {
		Scanner scn = new Scanner(System.in);
		
		String[] names = {"Not used", "P1", "P2"};
		int[]    ports = {-1, 12345, 23456};
		
		System.out.print("Which process am I: ");
		int proc = scn.nextInt();
		if (proc != 1 && proc != 2) {
			scn.close();
			throw new IllegalArgumentException("Invalid process ID");
		}
		
		CommunicationsManager.initialize(names[proc], ports[proc]);
		
		int other_proc = (proc == 1 ? 2 : 1);
		try {
			CommunicationsManager.addReceiver(names[other_proc], InetAddress.getLocalHost(), ports[other_proc]);
		}
		catch (UnknownHostException uhe) {
			scn.close();
			CommunicationsManager.destroy();
			throw new IllegalStateException("Issue with LocalHost");
		}
		
		System.out.println("Say 'start'");
		while (!scn.nextLine().equals("start"));
		
		EventMessage msg;
		if (proc == 1)
			msg = new EventMessage(Event.EVENT_1);
		else
			msg = new EventMessage(Event.EVENT_2);
		CommunicationsManager.send(names[other_proc], msg);
		
		System.out.println("Say 'stop'");
		while (!scn.nextLine().equals("stop"));
		
		if (proc != 1) {
			EventManager.sendLog(names[1]);
			System.out.println(EventManager.getLog());
		}
		else
			System.out.println(EventManager.getLog());
		
		CommunicationsManager.destroy();
		scn.close();
	}

}
