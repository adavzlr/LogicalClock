package zolera.dist;

import zolera.dist.EventManager.Event;
import zolera.dist.EventManager.EventHandler;

public class ControlMessage
extends EventMessage {
	public static final int INVALID_VOLUME = -1;
	private int volume;
	
	// new control message for a given resource name
	public ControlMessage(int vol, Event ev) {
		super(ev);
		
		if (ev != Event.PLAY && ev != Event.STOP && ev != Event.VOLUME)
			throw new IllegalArgumentException("Event '"+ev+"'");
		if ((ev == Event.PLAY || ev == Event.STOP) && vol != INVALID_VOLUME)
			throw new IllegalArgumentException("Volume on PLAY/STOP "+vol);
		if (ev == Event.VOLUME && (vol < 0 || vol > 100))
			throw new IllegalArgumentException("Volume on VOLUME "+vol);
		
		volume = vol;
	}
	
	// Only used by the handler's newMessage() method
	private ControlMessage() {
		super(null);
		volume = INVALID_VOLUME;
	}
	
	// Transform the message into a string version for transmission
	public String marshall() {
		return super.marshall() + volume + ":";
	}
	
	// Parse a transmitted string back into a message
	public String unmarshall(String text) {
		text = super.unmarshall(text);
		
		String[] tokens = text.split(":");
		if (tokens.length < 1)
			throw new IllegalArgumentException("Length " + tokens.length);
		
		volume = Integer.parseInt(tokens[0]);
		
		int length = tokens[0].length() + 1;
		return text.substring(length);
	}
	
	public int getVolume() {
		return volume;
	}
	
	
	
	// Handler for MutexMessage's
	public static final EventHandler HANDLER = new EventHandler() {
		@Override
		public EventMessage newMessage() {
			return new ControlMessage();
		}
		
		@Override
		public void process(EventMessage evmsg) {
			ControlMessage msg = (ControlMessage) evmsg;
			EventMessage.HANDLER.process(evmsg);
			System.out.println("Volume:\t" + msg.getVolume() + "\n");
		}
	};
}
