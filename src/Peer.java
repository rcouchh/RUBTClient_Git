import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;


public class Peer extends Thread {
	String ip;
	int port;
	
	private DataInputStream inStream = null;
	private DataOutputStream outStream = null;
	private static final byte[] Protocol = { 'B', 'i', 't', 'T', 'o',
		'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o',
		'l' };

	 final byte[] info_hash;
	 final byte[] peerID;
	 final byte[] clientID;
	 private Socket socket;
	
	public Peer(final byte[] peerID, final String ip, final int port, 
			final byte[] info_hash, final byte[] clientID){
		this.peerID = peerID;
		this.ip = ip;
		this.port = port;
		this.info_hash = info_hash;
		this.clientID = clientID;
		//this.clientID = clientID;
		
	}
	
	//destroys each socket/stream
	void disconnect() throws IOException {
		this.socket.close();
		this.inStream.close();
		this.outStream.close();
	}
	
	void connect() throws IOException{
		//create socket
		this.socket = null;
		try{
			System.out.println("Creating socket...");
			this.socket = new Socket(this.ip, this.port);
		}catch (UnknownHostException e){
			System.out.print("Unable to retrieve host IP!");
		}catch(IOException e){
			System.out.println("IOException!");
		}
		
		//check for socket connection
		if(this.socket==null){
			System.out.println("Unable to create socket!");
		}
		else{
			System.out.println("Socket created!");
		}
		this.inStream = new DataInputStream(this.socket.getInputStream());
		this.outStream = new DataOutputStream(this.socket.getOutputStream());
	}
	
	
	public byte[] createHandshake(byte[] peerID, byte[] info_hash){
		System.out.println("Generating handshake...");
		
		//allocate bytes for handshake
		byte[] handshakeMsg = new byte[68];
		
		//start msg with bytes 19Bitorrent protocol
		handshakeMsg[0] = 19;
		System.arraycopy(Peer.Protocol, 0, handshakeMsg, 1, Peer.Protocol.length);
		
		//add info_hash
		System.arraycopy(info_hash, 0, handshakeMsg, 28, info_hash.length);

		//add peer_id, should match info_hash
		System.arraycopy(peerID, 0, handshakeMsg, 48, peerID.length);
		System.out.println("Handshake Msg: ");
		System.out.println(new String(handshakeMsg));

		
		System.out.println("Handshake message generated successfully.");
		return handshakeMsg;
	}
	
}
