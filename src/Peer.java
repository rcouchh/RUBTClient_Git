
public class Peer {
	String ip;
	int port;
	byte[] peer_id;
	
	public Peer(byte[] peer_id, String ip, int port){
		this.peer_id = peer_id;
		this.ip = ip;
		this.port = port;
		
	}
	
	
}
