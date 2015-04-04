package zolera.dist;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import zolera.dist.EventManager.Event;
import zolera.dist.EventManager.EventHandler;


public class TestMutex {

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
				TestMutex.endCount++;
			else {
				TestMutex.startFlag = true;
				System.out.println("Start received");
			}
		}
	};
	
	public final String [] filenames = {"file0.txt", "file1.txt", "file2.txt","file3.txt"};
	public final String outputFile = "output.txt";
	public final String resourceName = "MainFile";
	
	private static Scanner scan;
	private static boolean startFlag = false;	// Tells if we are ready to start
	private int[] ports = { 1521, 1522, 1523, 1524 };  		// Ports for each process
	private String[] names = { "P0", "P1", "P2", "P3" }; 	// Names for each process
	private int currentProcess; 					// Current process id
	private static int endCount = 0; 			// Counts every message sent
	
	public TestMutex(int proc){
		CommunicationsManager.initialize(names[proc], proc, ports[proc]);
		currentProcess = proc;
		addRecipients();
		System.out.println("Process " + this.currentProcess + " started");
		if(currentProcess == 0){
			sendStartSignal();
			startFlag = true;
		}
		else {
			waitForStartSignal();
		}
		startMutexJob();
		stopMutexJob();
	}

	private void stopMutexJob() {
		if(currentProcess != 0){
			try {
				while(true)
				{
					Thread.sleep(10000);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//EventMessage msg = new EventMessage(Event.EVENT_END);
			//CommunicationsManager.send(names[0], msg); // Send to process 0 the message log 
			//CommunicationsManager.destroy();
		} else {
			while (endCount < names.length - 1) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {}
			}
			System.out.println("Finishing P0");
			System.out.println(EventManager.getLog()); // Print the master log 
			CommunicationsManager.destroy();
			scan.close();
		}
	}

	/**
	 * Starts requesting for the lock every time the file is read
	 */
	private void startMutexJob() {
		String sCurrentLine;
		try (BufferedReader br = new BufferedReader(new FileReader(filenames[currentProcess]))){			
			while ((sCurrentLine = br.readLine()) != null) {
				LockManager.lock(resourceName);
				//System.out.println(sCurrentLine);
				System.out.println("Lock Granted to: " + "P" + this.currentProcess);
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
				out.println("-----------------------  Block started by " + this.currentProcess +"  ----------------------------------------");
				for(int i = 0; i < 10 && sCurrentLine != null; i++){
					out.println(sCurrentLine);
					if(i < 9)
					{
						sCurrentLine = br.readLine();
					}
				}
				out.println("-----------------------  Block finished by " + this.currentProcess +"  ----------------------------------------");
				out.close();
				LockManager.release(resourceName);
			}
			System.out.println("P"+currentProcess+" done writing lines. Sleeping");
		} catch (Exception e) {
			System.out.println("Error while reading input file: " + filenames[currentProcess]);
			e.printStackTrace();
		}
	}

	/**
	 * Waits for the start message from P0 to start requesting the lock
	 */
	public void waitForStartSignal()
	{
		while(!startFlag)
		{
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {}
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
	
	public static void main(String[] args) {
		System.out.println("Who are you? (Process Id from 0 to 3)");
		scan = new Scanner(System.in);
		int proc = scan.nextInt();
		if (proc > 3 && proc < 0) {   // Ensure valid input
			scan.close();
			throw new IllegalArgumentException("Illegal process number");
		}
		new TestMutex(proc);
	}

}
