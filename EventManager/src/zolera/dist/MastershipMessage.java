package zolera.dist;

import zolera.dist.EventManager.Event;
import zolera.dist.EventManager.EventHandler;

public class MastershipMessage
extends EventMessage {
	private String newMaster;
	
	// new mastership message to request mastership or transfer mastership to a slave
	public MastershipMessage(String mst, Event ev) {
		super(ev);
		
		if (ev != Event.REQUEST_MASTERSHIP && ev != Event.TRANSFER_MASTERSHIP)
			throw new IllegalArgumentException("Event '"+ev+"'");
		if (ev == Event.TRANSFER_MASTERSHIP && (mst == null || !CommunicationsManager.knows(mst)))
			throw new IllegalArgumentException("Master on TRANSFER '"+mst+"'");
		if (ev == Event.REQUEST_MASTERSHIP && mst != null)
			throw new IllegalArgumentException("Master on REQUEST");
		
		newMaster = mst;
	}
	
	// Only used by the handler's newMessage() method
	private MastershipMessage() {
		super(null);
		newMaster = null;
	}
	
	// Transform the message into a string version for transmission
	public String marshall() {
		return super.marshall() + newMaster + ":";
	}
	
	// Parse a transmitted string back into a message
	public String unmarshall(String text) {
		text = super.unmarshall(text);
		
		String[] tokens = text.split(":");
		if (tokens.length < 1)
			throw new IllegalArgumentException("Length " + tokens.length);
		
		newMaster = tokens[0];
		
		int length = tokens[0].length() + 1;
		return text.substring(length);
	}
	
	public String getNewMaster() {
		return newMaster;
	}
	
	
	
	// Handler for MastershipMessage's
	public static final EventHandler HANDLER = new EventHandler() {
		@Override
		public EventMessage newMessage() {
			return new MastershipMessage();
		}
		
		@Override
		public void process(EventMessage evmsg) {
			MastershipMessage msg = (MastershipMessage) evmsg;
			EventMessage.HANDLER.process(evmsg);
			System.out.println("NewMaster:\t" + msg.getNewMaster() + "\n");
		}
	};
}
