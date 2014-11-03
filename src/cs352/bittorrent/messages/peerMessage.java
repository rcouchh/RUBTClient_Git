package cs352.bittorrent.messages;
/**
 * this class is used to allow for storage and simple retrieval
 * of a message and its corresponding peer
 * @author Dan
 *
 */
import cs352.bittorrent.download.Peer;
public class peerMessage {
	private final Peer p;
	private final Message m;
	
	public peerMessage(final Peer peer, final Message mes){
		this.p=peer;
		this.m=mes;
	}
	public Peer getPeer(){
		return this.p;
	}
	public Message getMessage(){
		return this.m;
	}
}
