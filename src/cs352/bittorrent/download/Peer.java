package cs352.bittorrent.download;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import cs352.bittorrent.customTools.utils;
import cs352.bittorrent.main.RUBTClient;
import cs352.bittorrent.messages.Message;
import cs352.bittorrent.messages.peerMessage;
import cs352.bittorrent.messages.Message.Message_Bitfield;
import cs352.bittorrent.messages.Message.Message_Have;
import cs352.bittorrent.messages.Message.Message_Request;
import cs352.bittorrent.messages.Message.PieceMessage;
/**
 * 
 * @author Dan Torres,Ryan Couch
 * 
 *
 */

public class Peer extends Thread {
	// Set up timer 
	//keepAlive timeout
	private static final long KEEP_ALIVE_TIMEOUT = 120000;
	//keepalive timer
	private final Timer keepAliveInterval = new Timer();
	//last time message was sent
	private long lastMessageSent = System.currentTimeMillis();
	//inputstream from the peer
	private InputStream inStream= null;
	//outputstream to the pear
	private OutputStream outStream= null;
	//starting piece of Bit torrent handshake protocol
	private static final byte[] Protocol = {'B', 'i', 't', 'T', 'o',
		'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o',
		'l' };
	private static ByteBuffer[] pieceHash;
	private LinkedBlockingQueue<peerMessage> toDo = new LinkedBlockingQueue<peerMessage>();
	//peers ip address
	 final String ip;
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
	 //track pieces that the client will want to download
	 private  byte [] bitfield= null;
	 //boolean representation of peers bitfield
	 private boolean[] bitfieldBool=null;
	  // Total number of pieces in the torrent. 
	  private int numPieces;
	 //the piece that client is currently  requesting from peer
	 private byte[] currPiece;
	 //index of curr piece
	 private int curIndex;
	 //lenght of currpiece
	 private int currPlength;
	 //blockoffset for next request
	 private int currBoffset;
	 //length of lastblock in the piece
	 private int lastBlockLength;
	  final static int defaultLength= 16384; // = 16Kb
	 //to keep track if peer has already validated handshake
	 private volatile boolean hasPerformedHandShake= false;
	 //if client is interested=true
	 private volatile boolean localInterested = false;
	//if peer is interested
	  private volatile boolean remoteInterested = false;
	 //check if peer is seed
	 private boolean isSeed= false;
	 //check if client is choked
	 private boolean localChoked =false;
	 //check for peer is choked
	 private volatile boolean remoteChoked = true;
	 //the client this peer is interacting with
	private static RUBTClient client;
	//keep peer running
	private boolean cantStopWontStop= true;
	
	public Peer(final byte[] peerID, final String ip, final int port, 
			final byte[] info_hash, final byte[] clientID, final int numPieces){
		this.peerID = peerID;
		this.ip = ip;
		this.port = port;
		this.info_hash = info_hash;
		this.clientID = clientID;
		this.numPieces=numPieces;
		
	}
	public String getIP(){
		return this.ip;
	}
	public int getPort(){
		return this.port;
	}
	public byte[] getPeerId(){
		return this.peerID;
	}
	
	public void setPieceHash(ByteBuffer [] b){
		this.pieceHash=b;
	}
	public void setClient(RUBTClient c){
	 this.client= c;
	}
	public void addToDo(peerMessage m){
		this.client.addToDo(m);
	}
	public void setLocalChoked(boolean set){
		this.localChoked=set;
	}
	public boolean isLocalChoke(){
		return this.localChoked;
	}
	public void setRemoteChoked(boolean set){
		this.remoteChoked=set;
	}
	public boolean isRemoteChoke(){
		return this.remoteChoked;
	}
	public void setRemoteInterested(boolean set){
		this.remoteInterested=set;
	}
	public boolean isRemoteInterested(){
		return this.remoteInterested;
	}
	public void setLocalInterested(boolean set){
		this.localInterested= set;
	}
	public boolean isLocalInterested(){
		return this.localInterested;
	}
	public void setToDo(LinkedBlockingQueue<peerMessage> toDo){
		this.toDo=toDo;
	}
	//destroys each socket/stream
	public void disconnect() throws IOException {
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
		this.inStream = this.socket.getInputStream();
		this.outStream = this.socket.getOutputStream();
	}
	
	
	public void initiateHandShake(byte[] peerID, byte[] info_hash) throws IOException{
		System.out.println("Generating handshake...");
		
		//allocate bytes for handshake
		byte[] handshakeMsg = new byte[68];
		
		//start msg with bytes 19Bitorrent protocol
		handshakeMsg[0] = 19;
		System.arraycopy(Peer.Protocol, 0, handshakeMsg, 1, Peer.Protocol.length);
		
		//add info_hash
		System.arraycopy(info_hash, 0, handshakeMsg, 28, info_hash.length);

		//add peer_id, should match info_hash
		System.arraycopy(this.clientID, 0, handshakeMsg, 48, peerID.length);
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
	
	public boolean verifyHandShake(byte[] infoHash) throws IOException {
		byte[] torrInfoHash = infoHash;
		byte[] handshakeInfoHash = new byte[20];
		byte[] handshakeResponseMsg = new byte[68];
		int index=0;
		
		try{
			//read response handshake msg from peer
			this.inStream.read(handshakeResponseMsg);
			System.out.println("Response handshake message: "+new String(handshakeResponseMsg));
			System.arraycopy(handshakeResponseMsg, 28, handshakeInfoHash, 0, 20);
			
			System.out.println("Verifying length...");
			//verify length
			if(handshakeResponseMsg.length!=68){
				System.out.println("Incorrect length of response!");
				this.disconnect();
				return false;
			}
			
			System.out.println("Verifying protocol...");
			//verify protocol
			final byte[] otherProtocol = new byte[19];
			System.arraycopy(handshakeResponseMsg, 1, otherProtocol, 0,
			Peer.Protocol.length);
			if (!Arrays.equals(otherProtocol, Peer.Protocol)) {
				System.out.println("Incorrect protocol of response!");
				this.disconnect();
				return false;
			}
			
			System.out.println("Verifying info_hash...");

			// Check info hash against info hash from .torrent file
			final byte[] otherInfoHash = new byte[20];
			System.arraycopy(handshakeResponseMsg, 28, otherInfoHash, 0, 20);
			if (!Arrays.equals(otherInfoHash, this.info_hash)) {
				System.out.println("Info hashes dont match!");
				this.disconnect();
				return false;
			}
				//handshakeconfirmed = true;
				System.out.println("Response handshake verified!");
				return true;
			
		}catch (IOException e){
			System.out.println("Error reading handshake response msg!");
			this.disconnect();
			System.exit(1);
		}
		return true;
	}
	public synchronized void writeMessage(Message m) throws IOException{
		if(this.outStream== null){
			throw new IOException("Output stream is null!");
		}
		Message.write(this.outStream, m);
		this.lastMessageSent=System.currentTimeMillis();
	}
	//checks to see when last message was sent and determines if a keep alive is needed to be sent
	protected void SendKeepAlive() throws Exception {
		final long now = System.currentTimeMillis();
		if ((now - this.lastMessageSent) > Peer.KEEP_ALIVE_TIMEOUT) {
			this.writeMessage(Message.keepAlive);
			// Validate that the timestamp was updated
			if (now > this.lastMessageSent) {
				throw new Exception(
						"Didn't update lastMessageTime when sending a keep-alive!");
			}
		}
	}
	int attempt=0;
	public void run(){
		try {
			this.keepAliveInterval.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					// Let the peer keep track of sending keepalive
					try {
						Peer.this.SendKeepAlive();
					} catch (final Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}, new Date(), 10000);
			this.connect();
			//intitiating handshake
			initiateHandShake(peerID,info_hash);
			//boolean verify=verifyHandShake(this.info_hash);
			if(!verifyHandShake(this.info_hash)){
				System.out.println("incorrect protocol of peers handshake");
				this.disconnect();
			}
			else{
				this.writeMessage(new Message.Message_Bitfield(this.client.getBitfield()));
				while(this.cantStopWontStop){
						System.out.println("inside peer loop:");
						Message m=Message.read(this.inStream);
						System.out.println("reading a :" + m.getMessageId());
						if(m.getMessageId()== Message.M_Piece){
						this.handlePieceMessage(m);//handle piece message
						}else{
						this.toDo.put(new peerMessage(this,m));
					}
						//this.client.Handle(new peerMessage(this,m));
						
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Error: "+e.getMessage());
		}
		
	}
//handling pieceMessage to build the actual piece from the blocks and request the other needed pieces
public boolean handlePieceMessage(Message m) throws IOException, InterruptedException{
			
		final PieceMessage pM= (PieceMessage) m;
			//make sure the piece is the one the client requested
			if(this.currBoffset!= pM.getOffset()){
				//check to make sure block isnt too large
				System.out.println("wrong piece recieved!");
				return false;
				}
			if(this.curIndex != pM.getPieceIndex()){
				System.out.println("wrong piece recieved!");
				return false;
			}else{
			if((pM.getOffset() + defaultLength  + this.lastBlockLength) == this.currPlength) {
				// Write the second to last block of piece
				System.arraycopy(pM.getBlockData(), 0, this.currPiece,
						this.currBoffset, Peer.defaultLength);
				Message_Request requestMsg;

				// Request another piece
				this.currBoffset = this.currBoffset + Peer.defaultLength;
				requestMsg = new Message_Request(this.curIndex,
						this.currBoffset, this.lastBlockLength);
				this.writeMessage(requestMsg);
			}else if(pM.getBlockData().length == this.lastBlockLength) {
				// Write the last block of piece
				System.arraycopy(pM.getBlockData(), 0, this.currPiece,
						this.currBoffset, this.lastBlockLength);
				// Queue the full piece
				final PieceMessage returnMsg = new PieceMessage(
						this.curIndex, 0, this.currPiece);
				this.toDo.put(new peerMessage(this, returnMsg));
			}else if (((pM.getOffset() + defaultLength + this.lastBlockLength) == this.currPlength) && this.lastBlockLength == 0) {
				// Write the last block of piece
				System.arraycopy(pM.getBlockData(), 0, this.currPiece,
						this.currBoffset, defaultLength);
				// Queue the full piece
				final PieceMessage returnMsg = new PieceMessage(
						this.curIndex, 0, this.currPiece);
				this.toDo.put(new peerMessage(this, returnMsg));
			}
			else {
				// Temporarily store the block
				System.arraycopy(pM.getBlockData(), 0, this.currPiece,
						this.currBoffset, defaultLength);
				Message_Request requestMsg;

				// Request another piece
				this.currBoffset = this.currBoffset + defaultLength;
				requestMsg = new Message_Request(this.curIndex,
						this.currBoffset, defaultLength);
				this.writeMessage(requestMsg);
			}
		}
			return true;//returns true if all goes well
			
}

public void request(final int pieceIndex, final int pieceLength)
		throws IOException {
	this.curIndex = pieceIndex;
	this.currPlength = pieceLength;
	this.currPiece = new byte[pieceLength];
	this.lastBlockLength = this.currPlength % Peer.defaultLength;

	this.currBoffset = 0;

	Message_Request requestMsg;
	if (this.lastBlockLength == this.currPlength) {
		// Request the last piece
		requestMsg = new Message_Request(this.curIndex, this.currBoffset,
				this.lastBlockLength);
	} else {
		requestMsg = new Message_Request(this.curIndex, this.currBoffset,
				Peer.defaultLength);
	}
	this.writeMessage(requestMsg);
}
	
	public byte[] getBitField(){
		return this.bitfield;
	}
	public boolean[] getBoolean(){
		return this.bitfieldBool;
	}
	public boolean checkSeed(){
		return this.isSeed;
	}
	private void isSeed(){
		boolean check= true;
		for(int i=0; i< numPieces;i++){
			if(bitfieldBool[i]!=true){
				check= false;
				break;
			}
		}
		if(check ==true){
			this.isSeed=true;
		}
		
	}
	//initializes a peers bitfield with given total pieces from metafile
	public void initializePeerBitfield(final int totalPieces) {
		final int bytes = (int) Math.ceil((double) totalPieces / 8);
		final byte[] tempBitfield = new byte[bytes];

		this.setPeerBitfield(tempBitfield);
	}
	public synchronized void setPeerBitfield(byte[]bits){
		this.bitfield=bits;
		this.bitfieldBool=utils.bitsToBool(bits);
	}
	public void setPeerBitAtIndex(int pieceIndex) throws IOException{
		byte[] temp= this.getBitField();
		temp=utils.setBitfieldAt(temp,pieceIndex);
		this.setPeerBitfield(temp);
	}
	public void resetPeerBitAtIndex(int pieceIndex)throws IOException{
		byte[] temp= this.getBitField();
		temp=utils.setBitfieldAt(temp,pieceIndex);
		this.setPeerBitfield(temp);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Peer)) {
			return false;
		}
		final Peer other = (Peer) obj;
		if (!Arrays.equals(this.peerID, other.peerID)) {
			return false;
		}
		if (this.ip == null) {
			if (other.ip != null) {
				return false;
			}
		} else if (!this.ip.equals(other.ip)) {
			return false;
		}
		return true;
	}
}
