package Client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import customTools.utils;
import Messages.Message;
import Messages.Message.Message_Bitfield;
import Messages.Message.Message_Have;
import Messages.Message.Message_Request;
import Messages.Message.PieceMessage;


public class Peer extends Thread {
	//duration client should wait to hear from  peer
	public static final int WAIT_FOR_MESS= 130;
	//max num of piece messages for queue
	public static final int MAX_PIECES_QUEUE= 20;
	//inputstream from the peer
	private InputStream inStream= null;
	//outputstream to the pear
	private OutputStream outStream= null;
	//starting piece of Bit torrent handshake protocol
	private static final byte[] Protocol = {'B', 'i', 't', 'T', 'o',
		'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o',
		'l' };
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
	 private volatile PieceMessage reqPiece=null;
	 //to keep track if peer has already validated handshake
	 private volatile boolean hasPerformedHandShake= false;
	 //if client is interested=true
	 private volatile boolean localInterested = false;
	 //check if peer is seed
	 private boolean isSeed= false;

	  /**
	   * {@code True} if the remote peer is interested in this client.
	   */
	  private volatile boolean remoteInterested = false;

	  /**
	   * {@code True} if this client is choked by the remote peer.
	   */
	  private volatile boolean localChoked = true;

	  /**
	   * {@code True} if the remote peer is choked by this client.
	   */
	  private volatile boolean remoteChoked = true;

	
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
		this.inStream = this.socket.getInputStream();
		this.outStream = this.socket.getOutputStream();
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
	
	public void run(){
		try {
			connect();
			createHandShake(peerID,info_hash);
			boolean verify=verifyHandShake(this.info_hash);
			if(verify==true){
				//initiatePeerInteraction();
			}
			
			//System.out.println("Available: "+this.inStream.available());
			
			//send interested message
			
		
			
			
			//read response
			System.out.println("reading instream");
			Message m = Message.read(this.inStream);
			handleMessage(m);
			if(this.isSeed== false){
				System.out.println("this is not a seed!");
			}
			if(this.isSeed== true){
				System.out.println("this is a seed!");
				Message.write(this.outStream, Message.Interested);
				System.out.println("Interested message sent!");
				if(this.inStream==null){
					System.out.println("instream null (peer)");
				}
			}
			if(this.bitfield==null){
				System.out.println("bitfield= null");
			}
			
			if(m==null){
				System.out.println("Message is null");
			}
			if(inStream==null){
				System.out.println("InStream is null");
			}
			
			m=Message.read(this.inStream);
			Message_Request mr= new Message_Request(0,0,16384);
			Message.write(this.outStream, mr);
			m=Message.read(this.inStream);
			disconnect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Error: "+e.getMessage());
		}
		
	}

public boolean handleMessage(Message m){
		byte MID= m.getMessageId();
		switch(MID){
		case Message.M_Have:
			Message_Have mes =  (Message_Have)m;
			this.bitfield[mes.getPieceIndex()]=1;
			System.out.println("peer has piece at index:"+ mes.getPieceIndex());
			break;
		case Message.M_Interested:
			this.remoteInterested= true;
			System.out.println("peer:" + this.toString()+"is interested");
			break;
		case Message.M_Uninterested:
			this.remoteInterested= false;
			System.out.println("peer:" + this.toString()+"is NOT interested");
			break;
		case Message.M_Piece:
			PieceMessage pM= (PieceMessage) m;
			//means peer sent the piece without the client requesting it
			if(this.reqPiece== null){
				System.out.println("Client did not request this data!");
				return false;
			}
			//make sure the piece is the one the client requested
			if(this.reqPiece.getPieceIndex()== pM.getPieceIndex()){
				//check to make sure block isnt too large
				if(pM.getOffset() + pM.getBlockData().length >this.reqPiece.getBlockData().length){
					System.out.println("the block of data sent was too large!");
				this.reqPiece= null;//reset req piece
				return false;
				}
			System.arraycopy(pM.getBlockData(),0,this.reqPiece.getBlockData(), pM.getOffset(), pM.getBlockData().length);
			
			}break;
		case Message.M_Bitfield:
			System.out.println("bitfieldMessage");
			Message_Bitfield bitMes= (Message_Bitfield)m;
			this.bitfield=bitMes.getBitfield();
			this.bitfieldBool=utils.bitsToBool(this.bitfield);
			int check= bitMes.getBool().length-numPieces;
			System.out.println("difference:"+check);
			if(check <0 || check>=8){
				System.out.println("payload is not correct number of pieces as specified by torrent file!");
				this.bitfield=null;
				this.bitfieldBool=null;
				return false;
			}
			isSeed();
			break;
		case Message.M_Unchoke:
			System.out.println("peer sent unchoke message now request pieces");
		case Message.M_Choke:
			System.out.println("peer sent choke message");
		}
		
		return true;//returns true if all goes well
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
	
}
