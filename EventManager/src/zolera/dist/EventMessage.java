package zolera.dist;

import zolera.dist.EventManager.Event;
import zolera.dist.EventManager.EventHandler;

public class EventMessage {
	private String       sender;
	private String       receiver;
	private long         clock;
	private Event        event;
	
	public EventMessage(Event ev) {
		sender   = null;
		receiver = null;
		event    = ev;
		clock    = 0;
	}
	
	// Transform the message into a string version for transmission
	public String marshall() {
		return sender+":"+receiver+":"+clock+":"+event+":";
	}
	
	// Parse a transmitted string back into a message
	public String unmarshall(String text) {
		String[] tokens = text.split(":");
		if (tokens.length < 4)
			throw new IllegalArgumentException("Length " + tokens.length);
		
		sender    = tokens[0];
		receiver  = tokens[1];
		clock     = Long.parseLong(tokens[2]);
		event     = Event.valueOf(tokens[3]);
		
		int length = tokens[0].length() + tokens[1].length() + tokens[2].length() + tokens[3].length() + 4;
		return text.substring(length);
	}
	
	public String getSender() {
		return sender;
	}
	
	public void setSender(String snd) {
		sender = snd;
	}
	
	public String getReceiver() {
		return receiver;
	}
	
	public void setReceiver(String rcv) {
		receiver = rcv;
	}
	
	public long getClock() {
		return clock;
	}
	
	public void setClock(long clk) {
		clock = clk;
	}
	
	public Event getEvent() {
		return event;
	}
	
	public void setEvent(Event ev) {
		event = ev;
	}
	
	
	
	// A simple EventHandler to dump a message when received
	// It is recommended that each message type declares its own handler as a static final member
	public static final EventHandler HANDLER = new EventHandler() {
		@Override
		public EventMessage newMessage() {
			return new EventMessage(null);
		}
		
		@Override
		public void process(EventMessage msg) {
			// Dump message
			System.out.println(	"\nFrom:\t"  + msg.getSender()   +
								"\nTo:\t"    + msg.getReceiver() +
								"\nEvent:\t" + msg.getEvent()    +
								"\nClock:\t" + msg.getClock()    +
								"\n");
		}
	};
}
