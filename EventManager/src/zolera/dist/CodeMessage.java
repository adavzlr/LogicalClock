package zolera.dist;

import zolera.dist.EventManager.Event;
import zolera.dist.EventManager.EventHandler;

public class CodeMessage
extends EventMessage {
	private String code;
	
	public CodeMessage(String code, Event ev) {
		super(ev);
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}
	
	@Override
	public String marshall() {
		// append extra fields
		return super.marshall()+code;
	}
	
	@Override
	public String unmarshall(String text) {
		text = super.unmarshall(text);
		code = text;
		
		return "";
	}
	
	
	
	public static final EventHandler HANDLER = new EventHandler() {
		@Override
		public EventMessage newMessage() {
			return new CodeMessage(null, null);
		}
		
		@Override
		public void process(EventMessage msg) {
			CodeMessage codemsg = (CodeMessage) msg;
			System.out.println("\nCode '"+codemsg.getCode()+"' was received from '"+codemsg.getSender()+"'\n");
		}
	};
}
