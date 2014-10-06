import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;


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
	
	
	public byte[] createHandshake(byte[] peerID, byte[] info_hash) throws IOException{
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
		
		System.out.println("sending handshake msg");
		this.outStream.write(handshakeMsg);
		this.outStream.flush();
		byte[] peerHandshakeMsg = new byte[68];
		//this.inStream.read(peerHandshakeMsg);
		//System.out.println("retrieving peer handshake");
		//System.out.println("Peer handshake: "+new String(peerHandshakeMsg));
		return handshakeMsg;
	}
	
	public boolean verifyHandshake(byte[] infoHash) {
		byte[] torrInfoHash = infoHash;
		byte[] handshakeInfoHash = new byte[20];
		byte[] handshakeResponseMsg = new byte[68];
		int index=0;
		
		try{
			//read response handshake msg from peer
			this.inStream.readFully(handshakeResponseMsg);
			System.arraycopy(handshakeResponseMsg, 28, handshakeInfoHash, 0, 20);
			
			//verify length
			if(handshakeResponseMsg.length!=68){
				System.out.println("Incorrect length of response!");
				return false;
			}
			
			//verify protocol
			final byte[] otherProtocol = new byte[19];
			System.arraycopy(handshakeResponseMsg, 1, otherProtocol, 0,
			Peer.Protocol.length);
			if (!Arrays.equals(otherProtocol, Peer.Protocol)) {
				System.out.println("Incorrect protocol of response!");
				return false;
			}
			
			// Check info hash against info hash from .torrent file
			final byte[] otherInfoHash = new byte[20];
			System.arraycopy(handshakeResponseMsg, 28, otherInfoHash, 0, 20);
			if (!Arrays.equals(otherInfoHash, this.info_hash)) {
				System.out.println("Info hashes dont match!");
				return false;
			}
				//handshakeconfirmed = true;
				System.out.println("true - match");
				return true;
			
		}catch (Exception e){
			System.out.println("Error reading handshake response msg!");
		}
		return true;
	}
	
	
}
