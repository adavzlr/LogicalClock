package zolera.dist;

import java.util.Scanner;
import java.util.TimerTask;

import zolera.dist.EventManager.Event;

public class TestDrive extends TimerTask {
	public static int[] hosts = {1521, 1522, 1523, 1524};
	public static Event [] randomEvents = {	Event.ACKNOWLEDGE_CS, 
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
	
	public static void main(String [] args)
	{
		Scanner scan = new Scanner(System.in);
		String whoami = scan.next();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
	//public 
}
