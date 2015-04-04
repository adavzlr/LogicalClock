package zolera.dist;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EventManager {
	private static long clock = 0;
	private static Map<Long,List<String>> dict = new HashMap<>();
	
	// Cannot instantiate this class
	private EventManager() {}
	
	// increment clock value
	private static synchronized void tick() {
		clock++;
	}
	
	// record an event in the log dictionary and assign it a clock time
	public static synchronized void record(Event ev) {
		tick();
		addLogRecord(clock, clock + "/REC/" + CommunicationsManager.getName() + "/" + ev);
	}
	
	// record the submission of a message with a new clock time and timestamp the message
	public static synchronized void stamp(EventMessage msg) {
		tick();
		addLogRecord(clock, clock + "/SND/" + CommunicationsManager.getName() + "/" + msg.getEvent()
		                    + "/" + msg.getSender() + "/" + msg.getReceiver());
		
		msg.setClock(clock);
	}
	
	// record the reception of a message with a clock time depending on our current time and msg time
	public static synchronized void update(EventMessage msg) {
		// Lamport step for incoming messages
		if (msg.getClock() > clock)
			clock = msg.getClock();
		
		tick();
		addLogRecord(clock, clock + "/RCV/" + CommunicationsManager.getName() + "/" + msg.getEvent()
		                    + "/" + msg.getSender() + "/" + msg.getReceiver());
	}
	
	
	
	// Add an entry to the log dictionary
	private static void addLogRecord(long clk, String record) {
		if (!dict.containsKey(clk))
			dict.put(clk, new LinkedList<>());
		
		List<String> list = dict.get(clk);
		list.add(record);
	}
	
	// Transform the log dictionary into a string representation of the log
	// The log has the following form <record>\n<record>\n...<record>\n meaning
	// every record is followed by a end-of-line marker, even the last one
	public static String getLog() {
		StringBuilder output = new StringBuilder();
		
		// for each equivalent clock time
		for (long clk : dict.keySet()) {
			List<String> list = dict.get(clk);
			
			// add to log each record in the equivalued list
			for (String record : list)
				output.append(record+"\n");
		}
		
		return output.toString();
	}
	
	// break up current log dictionary into chunks and send them to a receiver
	public static void sendLog(String rcvname) {
		String log = getLog();
		
		// each chunk has the form <record>\n...\n<record>\n...\n<record> so we remove
		// the end-of-line markers that separate records of different chunks
		while (log.length() > 0) {
			int breakpoint = log.lastIndexOf("\n", LogMessage.MAX_CHUNK_SIZE);
			LogMessage msg = new LogMessage(log.substring(0, breakpoint));
			log = log.substring(breakpoint+1); // skip breakpoint '\n'
			CommunicationsManager.send(rcvname, msg);
		}
	}
	
	// merge a set of records into the current log dictionary
	public static void mergeLog(String[] records) {
		for (String record : records) {
			String[] tokens = record.split("/");
			
			if (tokens.length < 3)
				throw new IllegalArgumentException("Length of record "+tokens.length);
			
			long clk = Long.parseLong(tokens[0]);
			addLogRecord(clk, record);
		}
	}
	
	
	
	// An event handler lets the comms mgr interact
	// with specific events that have specific message types
	public static interface EventHandler {
		// produce a new instance of the specific message type
		// so polymorphism can take care of unmarshalling
		public EventMessage newMessage();
		
		// given a message of the expected type by this handler (see newMessage())
		// the process() method of the handler serves as a callback to be
		// triggered by the comms mgr on the arrival of a message associated with
		// an event that is handled by this handler
		public void process(EventMessage msg);
	}
	
	// The Event enumeration
	public static enum Event {
		// Events log messages
		MERGE_LOG_CHUNK (LogMessage.HANDLER,   true),
		EVENT_START     (TestMutex.HANDLER,    true),
		EVENT_END       (TestMutex.HANDLER,    true),
		
		// Non-messaging events
		EVENT_1 (EventMessage.HANDLER, false),
		EVENT_2 (EventMessage.HANDLER, false),
		EVENT_3 (EventMessage.HANDLER, false),
		
		// MutexMessage's
		REQUEST_CS     (MutexMessage.HANDLER, false),
		ACKNOWLEDGE_CS (MutexMessage.HANDLER, false),
		RELEASE_CS     (MutexMessage.HANDLER, false),
		
		// ControlMessage's
		PLAY   (ControlMessage.HANDLER, false),
		STOP   (ControlMessage.HANDLER, false),
		VOLUME (ControlMessage.HANDLER, false),
		
		// PartyMessage's
		JOIN (PartyMessage.HANDLER, false),
		EXIT (PartyMessage.HANDLER, false),
		
		// MastershipMessage's
		REQUEST_MASTERSHIP  (MastershipMessage.HANDLER, false),
		TRANSFER_MASTERSHIP (MastershipMessage.HANDLER, false);
		
		private EventHandler handler;
		private boolean stealth;
		
		// construct a new event with given handler (may be null)
		private Event(EventHandler hdlfunc, boolean stealth) {
			this.handler = hdlfunc;
			this.stealth = stealth;
		}
		
		// return the event handler
		public EventHandler getHandler() {
			return handler;
		}
		
		// return whether this event is a stealth message
		public boolean isStealth() {
			return stealth;
		}
	}
}
