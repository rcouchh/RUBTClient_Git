import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;





public class Peer extends Thread {
	//duration client should wait to hear from  peer
	public static final int WAIT_FOR_MESS= 130;
	//max num of piece messages for queue
	public static final int MAX_PIECES_QUEUE= 20;
	//inputstream from the peer
	private DataInputStream inStream= null;
	//outputstream to the pear
	private DataOutputStream outStream= null;
	//starting piece of Bit torrent handshake protocol
	private static final byte[] Protocol = {'B', 'i', 't', 'T', 'o',
		'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o',
		'l' };
	//peers ip address
	private final String ip;
	//peers port number
	private final int port;
	//infohash of this torrent
	 final byte[] info_hash;
	 //this peers peerid
	 final byte[] peerID;
	 //the clients peerid
	 final byte[] clientID;
	 //socket connecting the client to peer
	 private Socket socket;
	 //the piece that client is currently  requesting from peer
	 private volatile Message.PieceMessage reqPiece=null;
	 //to keep track if peer has already validated handshake
	 private volatile boolean hasPerformedHandShake= false;
	
	public Peer(final byte[] peerID, final String ip, final int port, 
			final byte[] info_hash, final byte[] clientID){
		this.peerID = peerID;
		this.ip = ip;
		this.port = port;
		this.info_hash = info_hash;
		this.clientID = clientID;
		//this.clientID = clientID;
		
	}
	public String getIP(){
		return this.ip;
	}
	public int getPort(){
		return this.port;
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
			//createHandShake(this.peerID,this.info_hash);
		}
		this.inStream = new DataInputStream(this.socket.getInputStream());
		this.outStream = new DataOutputStream(this.socket.getOutputStream());
	}
	
	
	public void createHandShake(byte[] peerID, byte[] info_hash) throws IOException{
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
		//byte[] peerHandshakeMsg = new byte[68];
		//this.inStream.read(peerHandshakeMsg);
		//System.out.println("retrieving peer handshake");
		//System.out.println("Peer handshake: "+new String(peerHandshakeMsg));
	
		
	}
	
	public boolean verifyHandShake(byte[] infoHash) {
		byte[] torrInfoHash = infoHash;
		byte[] handshakeInfoHash = new byte[20];
		byte[] handshakeResponseMsg = new byte[68];
		int index=0;
		
		try{
			//read response handshake msg from peer
			this.inStream.read(handshakeResponseMsg);
			System.out.println(new String(handshakeResponseMsg));
			System.arraycopy(handshakeResponseMsg, 28, handshakeInfoHash, 0, 20);
			
			System.out.println("Verifying length...");
			//verify length
			if(handshakeResponseMsg.length!=68){
				System.out.println("Incorrect length of response!");
				return false;
			}
			
			System.out.println("Verifying protocol...");
			//verify protocol
			final byte[] otherProtocol = new byte[19];
			System.arraycopy(handshakeResponseMsg, 1, otherProtocol, 0,
			Peer.Protocol.length);
			System.out.println("Peer protocol:" +new String(otherProtocol));

			if (!Arrays.equals(otherProtocol, Peer.Protocol)) {
				System.out.println("Incorrect protocol of response!");
				return false;
			}
			
			System.out.println("Verifying info_hash...");

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
			
		}catch (IOException e){
			System.out.println("Error reading handshake response msg!");
			System.exit(1);
		}
		return true;
	}
	public void run(){
		try {
			connect();
			createHandShake(peerID,info_hash);
			boolean verify=verifyHandShake(this.info_hash);
			if(verify==true){
				System.out.println("nigga we made it");
			}
			disconnect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("unable to connect and verify handshake");
		}
		
		
		
	}
	
	
}
