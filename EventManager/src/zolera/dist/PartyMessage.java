package zolera.dist;

import zolera.dist.EventManager.Event;
import zolera.dist.EventManager.EventHandler;

public class PartyMessage
extends EventMessage {
	// new party message to join/leave a party
	public PartyMessage(Event ev) {
		super(ev);
		
		if (ev != Event.JOIN && ev != Event.EXIT)
			throw new IllegalArgumentException("Event '"+ev+"'");
	}
	
	// Only used by the handler's newMessage() method
	private PartyMessage() {
		super(null);
	}
	
	// Transform the message into a string version for transmission
	public String marshall() {
		return super.marshall();
	}
	
	// Parse a transmitted string back into a message
	public String unmarshall(String text) {
		text = super.unmarshall(text);
		return text;
	}
	
	
	
	// Handler for PartyMessage's
	public static final EventHandler HANDLER = new EventHandler() {
		@Override
		public EventMessage newMessage() {
			return new PartyMessage();
		}
		
		@Override
		public void process(EventMessage evmsg) {
			EventMessage.HANDLER.process(evmsg);
		}
	};
}
