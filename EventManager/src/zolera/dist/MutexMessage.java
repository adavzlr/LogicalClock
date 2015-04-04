package zolera.dist;

import zolera.dist.EventManager.Event;
import zolera.dist.EventManager.EventHandler;

public class MutexMessage
extends EventMessage {
	private String resource;
	
	// new mutex message for a given resource name
	public MutexMessage(String res, Event ev) {
		super(ev);
		
		if (ev != Event.REQUEST_CS && ev != Event.ACKNOWLEDGE_CS && ev != Event.RELEASE_CS)
			throw new IllegalArgumentException("Event '"+ev+"'");
		if (res == null || !res.matches("^\\w{1,10}$"))
			throw new IllegalArgumentException("Resource '"+res+"'");
		
		resource = res;
	}
	
	// Only used by the handler's newMessage() method
	private MutexMessage() {
		super(null);
		resource = null;
	}
	
	// Transform the message into a string version for transmission
	public String marshall() {
		return super.marshall() + resource + ":";
	}
	
	// Parse a transmitted string back into a message
	public String unmarshall(String text) {
		text = super.unmarshall(text);
		
		String[] tokens = text.split(":");
		if (tokens.length < 1)
			throw new IllegalArgumentException("Length " + tokens.length);
		
		resource = tokens[0];
		
		int length = tokens[0].length() + 1;
		return text.substring(length);
	}
	
	public String getResource() {
		return resource;
	}
	
	
	
	// Handler for MutexMessage's
	public static final EventHandler HANDLER = new EventHandler() {
		@Override
		public EventMessage newMessage() {
			return new MutexMessage();
		}
		
		@Override
		public void process(EventMessage evmsg) {
			MutexMessage msg = (MutexMessage) evmsg;
			
			switch (msg.getEvent()) {
			case REQUEST_CS:
				System.out.println("Request received for " + msg.getResource() + " from " + msg.getSender());
				LockManager.processLockMessage(msg);
				break;
			case RELEASE_CS:
				System.out.println("Release received for " + msg.getResource() + " from " + msg.getSender());
				LockManager.processReleaseMessage(msg);
				break;
			case ACKNOWLEDGE_CS:
				System.out.println("Ack received for " + msg.getResource() + " from " + msg.getSender());
				LockManager.processAcknowledgeMessage(msg);
				break;
			default:
				throw new IllegalArgumentException("Invalid message type "+msg.getEvent());
			}
		}
	};
}
