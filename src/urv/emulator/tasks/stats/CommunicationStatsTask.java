package urv.emulator.tasks.stats;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.jgroups.Message;
import org.jgroups.View;

import urv.emulator.core.EmulationController;
import urv.emulator.tasks.EmulationGroupMembershipListener;
import urv.emulator.tasks.EmulationMessageListener;
import urv.emulator.tasks.EmulatorTask;
import urv.emulator.tasks.GroupMembershipNotifier;
import urv.emulator.tasks.MessageNotifier;
import urv.machannel.MChannel;
import urv.util.graph.HashMapSet;

/**
 * This task gathers information about all messages sent and received 
 * in the network by all applications
 * It verifies that all nodes that were in the view of the source node
 * received the multicast message
 * 
 * @author Marcel Arrufat Arias
 */
public class CommunicationStatsTask extends EmulatorTask implements EmulationMessageListener, EmulationGroupMembershipListener{

	//	CLASS FIELDS --
	
	public Hashtable<MessageIdentifier,View> sentMessages = new Hashtable <MessageIdentifier,View>();
	public HashSet<MessageIdentifier> receivedMessages = new HashSet<MessageIdentifier>(); 
	private HashMapSet<InetAddress,InetAddress> registeredMembership = new HashMapSet<InetAddress,InetAddress>();
	private Object lock = new Object();
	private HashSet<MessageIdentifier> removeIfMcastReceived = new HashSet<MessageIdentifier>();
	//Stats info
	private int ucastReceived=0;
	private int ucastNotReceived=0;
	private int mcastReceived=0;
	private int mcastNotReceived=0;
	private int partialsNotFound=0;
	private int partialsFound=0;
	private float resultUcast=0;
	private float resultMcast=0;
	private float resultMcastPartials=0;
	private int numSentMessages=0;	
	private int numReceivedMessages=0;

	//	CONSTRUCTORS --
	
	/**
	 * @param emulationController
	 */
	public CommunicationStatsTask() {
		super();		
	}

	//	OVERRIDDEN METHODS --

	/**
	 * Add the code that should be launched in the run method
	 */
	public void doSomething() {		
		//In first place, register the class as listener in order to intercept
		//network messages
		EmulationController controller = super.getEmulationController();
		MessageNotifier messageNotifier = controller.getMessageNotifier();
		messageNotifier.addMessageListener(this);		
		//Improved: now we will check if multicast messages are received by all
		//nodes that started the channel
		GroupMembershipNotifier membershipNotifier = controller.getGroupMembershipNotifier();
		membershipNotifier.addGroupMembershipListener(this);
		
		while (true){
			//Rest for a little while
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//Check all send messages have been received correctly
			checkMessages();
		}
	}
	public void onGroupCreated(InetAddress multicastAddress, InetAddress localAddress, MChannel mChannel) {
		synchronized (lock) {
			//Store info
			registeredMembership.addToSet(multicastAddress,localAddress);
		}		
	}
	public void onMessageReceived(Message msg, InetAddress src, InetAddress mainDst, InetAddress realDst, int seqNumber){
		//Maybe this lock could be removed since we are just retrieving information with
		synchronized (lock) {
			MessageIdentifier mid = new MessageIdentifier(src,mainDst,realDst,seqNumber);
			numReceivedMessages++;
			receivedMessages.add(mid);
		}		
	}
	public void onMessageSent(Message msg, InetAddress src, InetAddress dst, int seqNumber, View view) {		
		synchronized (lock) {
			sentMessages.put(new MessageIdentifier(src,dst,dst,seqNumber),view);
			numSentMessages++;
		}		
	}

	//	PRIVATE METHODS --

	/**
	 * For each sent message, checks that a received message exists
	 * in the receivedMessages structure  
	 *
	 */
	private void checkMessages() {
		
		synchronized (lock) {			
			//for (MessageIdentifier mid:){
			Iterator<MessageIdentifier> it = sentMessages.keySet().iterator();
			while (it.hasNext()){
				MessageIdentifier mid = it.next();
				//MULTICAST messages
				InetAddress mainDst = mid.getMainDst();
				if (mainDst.isMulticastAddress()){					
					//View is not used now!
					View view = sentMessages.get(mid);					
					//For each node that the multicast was sent to
					//Check if they really received the multicast message
					InetAddress src = mid.getSrc();
					//InetAddress realDst = mid.getRealDst();
					int seqNum = mid.getSeqNumber();					
					boolean hasReceivedMulticast=true;
					//Now we won't ask the channel view, but all the nodes that started
					//a channel instead
					
					//Clear the tmp list of message to delete
					removeIfMcastReceived.clear();
					
					for(InetAddress dstInView:registeredMembership.getSet(mainDst)){
						MessageIdentifier dstMid = new MessageIdentifier(src,mainDst,dstInView,seqNum);
						if (!receivedMessages.contains(dstMid)){
							hasReceivedMulticast=false;
							partialsNotFound++;
						}else{
							removeIfMcastReceived.add(dstMid);
							partialsFound++;
						}
					}					
					if (hasReceivedMulticast){
						mcastReceived++;
						//The message was received ok, so remove the messages

						//If the message was processed and we have receive it
						//just substract 1 from the number of not received
						if (mid.isProcessed()){
							mcastNotReceived--;
						}
						
						//Remove the current sent message
						it.remove();
						//Remove all the messages kept in removeIfMcastReceived
						for (MessageIdentifier midToRemove:removeIfMcastReceived){
							receivedMessages.remove(midToRemove);
						}
					} else {
						//if the message was not received, mark it as processed
						//Just add the first time is processed as not received
						if (!mid.isProcessed()){
							mcastNotReceived++;
							mid.setProcessed(true);
						}
					}			
				}else{					
					//UNICAST messages
					if (receivedMessages.contains(mid)){
						if (mid.isProcessed()){
							ucastNotReceived--;
						}
						ucastReceived++;
						//Remove from sent messages
						it.remove();
						//Remove from received
						receivedMessages.remove(mid);
					}else{
						System.out.println("++ NO ++ Cannot find the message:"+mid);
						if (!mid.isProcessed()){
							ucastNotReceived++;
							mid.setProcessed(true);
						}
					}
				}
			}			
			System.out.println("Messages received but not removed ("+receivedMessages.size()+")");
			for (MessageIdentifier rcvdMsg:receivedMessages){
				System.out.println("\t"+rcvdMsg);
			}			
			System.out.println("Messages sent but not removed ("+sentMessages.size()+")");
			for (MessageIdentifier sentMsg:sentMessages.keySet()){
				System.out.println("\t"+sentMsg);
			}			
			String output = "";
			if ((ucastReceived+ucastNotReceived)>0)
				resultUcast = 100*(float)ucastReceived/(ucastReceived+ucastNotReceived);
			if ((mcastReceived+mcastNotReceived)>0)
				resultMcast = 100*(float)mcastReceived/(mcastReceived+mcastNotReceived);
			if ((partialsFound+partialsNotFound)>0)
				resultMcastPartials = 100*(float)partialsFound/(partialsFound+partialsNotFound);
			output += "#sentMgs="+numSentMessages+"\\rcvdMsgs="+numReceivedMessages+"\n";
			output += "UCAST: "+ucastReceived+ " ok;"+ucastNotReceived+" ko ("+resultUcast+"%)\n";
			output += "MCAST: "+mcastReceived+ " ok;"+mcastNotReceived+" ko ("+resultMcast+"%)\n";
			output += "MCAST: "+partialsFound+ " partials ok;"+partialsNotFound+" partials ko ("+resultMcastPartials+"%)\n";
			print(output,true);
		}
	}
	
	//	PRIVATE CLASSES --
	
	private class MessageIdentifier{
		
		//	CLASS FIELDS --
		
		private InetAddress src;
		private InetAddress mainDst;
		private int seqNumber;
		private InetAddress realDst;
		private boolean processed=false;
		
		//	CONSTRUCTORS --
		
		/**
		 * @param src
		 * @param mainDst
		 * @param realDst 
		 * @param seqNumber
		 */
		public MessageIdentifier(InetAddress src, InetAddress mainDst, InetAddress realDst, int seqNumber) {			
			this.src = src;
			this.mainDst = mainDst;
			this.realDst = realDst;
			this.seqNumber = seqNumber;
		}

		//	OVERRIDDEN METHODS --
		
		public boolean equals(Object obj){
			MessageIdentifier mid = (MessageIdentifier)obj;
			return mid.getMainDst().equals(this.mainDst) && mid.getSrc().equals(this.src) && mid.getRealDst().equals(this.realDst)&& mid.getSeqNumber()==this.seqNumber;
		}
		public int hashCode(){
			return src.hashCode()+mainDst.hashCode()+realDst.hashCode()+seqNumber;
		}
		public String toString(){
			return "src:"+src+";mainDst:"+mainDst+";realDst:"+realDst+";seqNo:"+seqNumber;			
		}
		
		//	ACCESS METHODS --
		
		/**
		 * @return Returns the mainDst.
		 */
		public InetAddress getMainDst() {
			return mainDst;
		}
		/**
		 * @return Returns the realDst.
		 */
		public InetAddress getRealDst() {
			return realDst;
		}		
		/**
		 * @return Returns the seqNumber.
		 */
		public int getSeqNumber() {
			return seqNumber;
		}		
		/**
		 * @return Returns the src.
		 */
		public InetAddress getSrc() {
			return src;
		}
		/**
		 * @return Returns the processed.
		 */
		public boolean isProcessed() {
			return processed;
		}
		/**
		 * @param processed The processed to set.
		 */
		public void setProcessed(boolean processed) {
			this.processed = processed;
		}
	}
}