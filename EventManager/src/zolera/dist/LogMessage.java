package zolera.dist;

import zolera.dist.EventManager.Event;
import zolera.dist.EventManager.EventHandler;

public class LogMessage extends EventMessage {
	public static final int MAX_CHUNK_SIZE = 1000;
	private String logchunk;
	
	// create new message for transmission
	public LogMessage(String chunk) {
		super(Event.MERGE_LOG_CHUNK);
		
		if (chunk == null)
			throw new IllegalArgumentException("Null chunk");
		if (chunk.length() > MAX_CHUNK_SIZE)
			throw new IllegalArgumentException("Chunk length "+chunk.length());
		
		logchunk = chunk;
	}
	
	// empty message to be populated during unmarshalling
	private LogMessage() {
		super(Event.MERGE_LOG_CHUNK);
		logchunk = null;
	}
	
	public String getChunk() {
		return logchunk;
	}
	
	public String marshall() {
		return super.marshall()+logchunk;
	}
	
	public String unmarshall(String text) {
		text = super.unmarshall(text);
		logchunk = text;
		
		return "";
	}
	
	public static final EventHandler HANDLER = new EventHandler() {
		// Merge received chunk with this Event Mgr's log
		@Override
		public void process(EventMessage msg) {
			LogMessage logmsg = (LogMessage) msg;
			String[] records = logmsg.getChunk().split("\n");
			EventManager.mergeLog(records);
		}
		
		@Override
		public EventMessage newMessage() {
			return new LogMessage();
		}
	};
}
