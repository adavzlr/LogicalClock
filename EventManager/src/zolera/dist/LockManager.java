package zolera.dist;

import java.util.Queue;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import zolera.dist.EventManager.Event;

public class LockManager {
	private static Map<String, Queue<LockRequest>> resources = new HashMap<>();
	private static Thread reqThread = null;
	
	
	/**
	 * Sends a lock request to every process and waits until the lock is given
	 * to the current thread
	 * 
	 * @param res Resource being accessed at
	 */
	public static synchronized void lock(String res) {
		// Create and prepare the request message
		MutexMessage msg = new MutexMessage(res, Event.REQUEST_CS);
		CommunicationsManager.prepareMsg(null, msg);
		
		// Get resource's queue
		Queue<LockRequest> resourceQueue = resources.get(res);
		if(resourceQueue == null){
			resourceQueue = new PriorityQueue<>();
			resources.put(res, resourceQueue);
		}
		
		// Add request to my own queue
		LockRequest myOwnRequest = new LockRequest(res, CommunicationsManager.getName(), msg.getClock());		
		resourceQueue.add(myOwnRequest);
		
		// Broadcast a request message to every receiver (same timestamp)	
		CommunicationsManager.sendAll(msg, true);
		
		
		// Wait like a champ until haveLock(res)==true
		reqThread = Thread.currentThread();
		while(!haveLock(res)){
			try {
				Thread.currentThread().wait();
			} catch (InterruptedException e) {}
		}
	}
	
	public static synchronized void release(String res) {
		// Broadcast a release message to every receiver
		MutexMessage msg = new MutexMessage(res, Event.RELEASE_CS);
		CommunicationsManager.sendAll(msg);
		
		// Get the resource queue and validate exists
		Queue<LockRequest> resourceQueue = resources.get(res);
		if(resourceQueue == null){
			throw new IllegalArgumentException("Invalid resource, " + res);
		}
		
		// Delete my own request (supposedly at the head of the queue)
		if (resourceQueue.peek().requester.equals(CommunicationsManager.getName())){
			resourceQueue.remove();
		}
		else {
			throw new IllegalArgumentException("Request wasn't at the top of the queue."+resourceQueue.peek().requester+", "+res);
		}
		
		// Going through all requests that have acknowledges == 0 and send acknowledges
		for(LockRequest currRequest : resourceQueue){
			if(currRequest.acknowledges == 0){
				MutexMessage ackMsg = new MutexMessage(currRequest.resource, Event.ACKNOWLEDGE_CS);
				CommunicationsManager.send(currRequest.requester, ackMsg);
				currRequest.acknowledges = 1;
			}
		}
	}
	
	
	
	// called when other process sends a lock request message
	public static synchronized void processLockMessage(MutexMessage msg) {
		String resource  = msg.getResource();
		String requester = msg.getSender();
		long   clock     = msg.getClock();
		
		Queue<LockRequest> queue = resources.get(resource);
		if (queue == null) {
			// if this resource is not on the resources dictionary, add it
			queue = new PriorityQueue<LockRequest>();
			resources.put(resource, queue);
		}
		
		// verify there's not a pending request for this resource from the same guy
		for (LockRequest req : queue) {
			if (req.requester.equals(requester))
				throw new IllegalArgumentException("Repeated request received "+requester+", "+resource);
		}
		
		LockRequest request = new LockRequest(resource, requester, clock);
		queue.add(request);
		
		if (!haveLock(resource)) {
			// send the acknowledge here if I'm not in my CS, else send while releasing
			MutexMessage ackmsg = new MutexMessage(resource, Event.ACKNOWLEDGE_CS);
			CommunicationsManager.send(requester, ackmsg);
			request.acknowledges = 1; // Used to remember that we already sent acknowledge
		}
	}
	
	// called when other process sends a lock release message
	public static synchronized void processReleaseMessage(MutexMessage msg) {
		String requester = msg.getSender();
		String resource  = msg.getResource();
		
		Queue<LockRequest> queue = resources.get(resource);
		if (queue == null)
			throw new IllegalArgumentException("Invalid resource "+requester+", "+resource);
		
		// search the lock request
		LockRequest request = null;
		for (LockRequest req : queue) {
			if (req.requester.equals(requester)) {
				request = req;
				break;
			}
		}
		
		// remove the lock request
		if (request != null)
			queue.remove(request);
		else
			throw new IllegalArgumentException("Invalid release "+requester+", "+resource);
	}
	
	// called when other process acknowledges a lock request from us
	public static synchronized void processAcknowledgeMessage(MutexMessage msg) {
		String  sender   = msg.getSender();
		String  resource = msg.getResource();
		boolean found    = false;
		
		Queue<LockRequest> queue = resources.get(resource);
		if (queue == null)
			throw new IllegalArgumentException("Invalid resource "+sender+", "+resource);
		
		// find the request belonging to this process
		for (LockRequest req : queue) {
			if (req.requester.equals(CommunicationsManager.getName())) {
				req.acknowledges++;   // increment acknowledges count
				
				// Let the waiting process know it can continue when all acks are received
				if (req.acknowledges >= CommunicationsManager.receiversCount()) {
					reqThread.notify();
					reqThread = null;
				}
				
				found = true;
				break;
			}
		}
		
		if (!found)
			throw new IllegalArgumentException("Invalid acknowledge "+sender+", "+resource);
	}
	
	
	
	// Am I currently in my CS?
	private static boolean haveLock(String res) {
		Queue<LockRequest> queue = resources.get(res);
		LockRequest head = queue.peek();
		
		// If I'm the requester at the queue head and I have received all acks, then I have the lock
		if (head.requester.equals(CommunicationsManager.getName())
			&& head.acknowledges >= CommunicationsManager.receiversCount())
			return true;
		else
			return false;
	}
	
	
	
	// Information about a request for a lock
	private static class LockRequest implements Comparable<LockRequest> {
		public String resource;
		public String requester;
		public int    acknowledges;
		public long   clock;
		
		public LockRequest(String res, String req, long clk) {
			resource     = res;
			requester    = req;
			clock        = clk;
			acknowledges = 0;
		}

		// Requests are compared by clock and ties are broken by process ID
		@Override
		public int compareTo(LockRequest otherReq) {
			return	clock < otherReq.clock ? -1 :
					clock > otherReq.clock ? 1  :
					CommunicationsManager.getReceiverId(requester) - CommunicationsManager.getReceiverId(otherReq.requester);
		}
	}
}
