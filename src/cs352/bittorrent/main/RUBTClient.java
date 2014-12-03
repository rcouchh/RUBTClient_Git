
package cs352.bittorrent.main;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import cs352.bittorrent.customTools.*;
import cs352.bittorrent.messages.Message;
import cs352.bittorrent.messages.Message.Message_Bitfield;
import cs352.bittorrent.messages.Message.Message_Have;
import cs352.bittorrent.messages.Message.Message_Request;
import cs352.bittorrent.messages.Message.PieceMessage;
import cs352.bittorrent.download.Peer;
import cs352.bittorrent.download.Tracker;
import cs352.bittorrent.givenTools.*;
import cs352.bittorrent.messages.peerMessage;

/**
 *
 * @author Ryan Couch, Daniel Torres
 *
 *
 * Your assignment should basically do the following:

    1. Take as a command-line argument the name of the .torrent file to be loaded and the name of the file to save the data to. For example:
       java -cp . RUBTClient somefile.torrent picture.jpeg
    2. Open the .torrent file and parse the data inside. You may use the Bencoder2.java class to decode the data.
    3. Send an HTTP GET request to the tracker at the IP address and port specified by the TorrentFile object. The java.net.URL class is very useful for this.
         a. Capture the response from the tracker and decode it in order to get the list of peers. From this list of peers, use only the peers at IP address with peer_id prefix RUBT11. You must extract this IP from the list, hard-coding it is not acceptable.
    4. Open a TCP socket on the local machine and contact the peer using the BT peer protocol and request a piece of the file.
    5. Download the piece of the file and verify its SHA-1 hash against the hash stored in the metadata file. The first time you begin the download, you need to contact the tracker and let it know you are starting to download.
    6. After a piece is downloaded and verified, the peer is notified that you have completed the piece.
    7. Repeat steps 5-7 (using the same TCP connection) for the rest of the file.
    8. When the file is finished, you must contact the tracker and send it the completed event and properly close all TCP connections
    9. Save the file to the hard disk according to the second command-line
 *
 */
public class RUBTClient extends Thread{

    /**
     * @param args the command line arguments
     */
	   
    public static void main(String[] args) {
       
        if(args[0] == null | args[1] == null){
            System.out.println("Incorrect # of args!");
            System.exit(1);
        }
        
        //global vars
        RUBTClient client;
        Tracker tracker;

        String torr = args[0];
        String file = args[1];

        byte[] torrentBytes = null;
       

        try{

        //open the .torrent file
        final File torrentFile = new File("files/"+torr);
       // final File torrentFile = new File(torr);
        final DataInputStream torrentDataIn =
                new DataInputStream(new FileInputStream(torrentFile));

        torrentBytes = new byte[(int)torrentFile.length()];
        torrentDataIn.readFully(torrentBytes);
        torrentDataIn.close();


        }catch (FileNotFoundException f){
            System.out.println(torr);
            System.out.println("File not found!");
            System.exit(1);
        }catch (IOException e){
            System.out.println("IO Exception!");
            System.exit(1);
        }//end try

        //if nothing loaded into torrentBytes
        if(torrentBytes==null){
            System.out.println("Torrent file corrupt! Unable to read.");
            System.exit(1);
        }

        TorrentInfo tInfo = null;
        try{
            tInfo = new TorrentInfo(torrentBytes);

        }catch (final BencodingException b){
            System.out.println("Bencoding error! Unable to create TorrentInfo object.");
        }
        RUBTClient localClient;
    	   //initialize client
          try{
          localClient = new RUBTClient(tInfo,args[1]);
          localClient.start();
          localClient.join();
          }catch(final InterruptedException e){
        	  
          }
        //initialize tracker, make connection
        System.out.println("Initializing tracker, connecting...");
        
        
        
    }//end main
    public String command;
    public BufferedReader userInput  = null;
    public final TorrentInfo tInfo;// torrentinfo object
    private boolean onethirty= false;//keep track if ip .130 added
    private boolean onethirtyone=false;//keep track if ip 131
    private final String writeFileName; // name of desired file
	private final byte[] clientId= utils.generateMyPeerId();
	private Peer peer;
    private RandomAccessFile writeFile;//actual file
    private int fileLength;//length of write file
    public String event; //event passed from tracker
	private int port= 6881;	
	private static final int BLOCK_LENGTH = 16384; // = 16Kb
	//clients bitfield
	private byte[] bitfield;
	//clients bitfield in bools
	private boolean[] bitfieldBools;
	//list of the peers client is interacting with
	private final List<Peer> peers = Collections
			.synchronizedList(new LinkedList<Peer>());
	//timer for announces
	final Timer announceTimer = new Timer();
	//# bytes downloaded by client at given point
	private int downloaded;
	//for phase 2 uploading to peers
	private int uploaded;
	
	//bytes left to download
	private int left;
	
	//total pieces of file
	private final int totalPieces;
	//length of the pieces 
	private final int pieceLength;
	// the tracker this client is interfacing with
	Tracker tracker;
	private final LinkedBlockingQueue<peerMessage> toDo = new LinkedBlockingQueue<peerMessage>();
	//boolean to keep all threads running until done
	private volatile boolean cantStopWontStop= true;
	private int peerLimit=4;//upload and download at least 2 simultaneously
	private int unchokedPeers=0;//keep track of peers that are interacting with
	private int getDownloaded(){
		return this.downloaded;
	}
	private int getLeft(){
		return this.left;
	}
	private TorrentInfo getTorrInfo(){
		return this.tInfo;
	}

	/**
	 * update the downloaded field of client
	 * @param int Downloaded
	 * 
	 */
public synchronized void addDownloaded(int down){
	System.out.println("Client has downloaded:"+ this.downloaded+down);
	this.downloaded+=down;
}
public byte[] getBitfield(){
	return this.bitfield;
}
public int getUploaded(){
	return this.uploaded;
}
public synchronized void addToDo(peerMessage m){
	this.toDo.add(m);
}
private static class TrackerAnnounce extends TimerTask{
	private  RUBTClient client;
	public TrackerAnnounce(RUBTClient c){
		this.client=c;
	}
	@Override
	public void run(){
		LinkedList<Peer> p= new LinkedList<Peer>();
		p=this.client.tracker.announceToTracker(this.client.getDownloaded(), this.client.getUploaded(), this.client.getLeft(), "");
		if(!p.isEmpty() && p!=null){
			this.client.addPeers(p);
		}
		this.client.announceTimer.schedule(this,this.client.tracker.getInterval()*1000);
	}
}

   public RUBTClient(TorrentInfo info, String WriteFile){
       this.tInfo= info;
       this.writeFileName= WriteFile;
       this.fileLength= info.file_length;
       try {
		this.tracker= new Tracker(this.clientId,this.tInfo.info_hash.array(),
				   this.tInfo.announce_url.toString(),this.port,info);
	} catch (UnsupportedEncodingException e) {
		System.out.println("uh oh unsupported encoding!");
	}
       this.downloaded=0;
       this.uploaded=0;
       this.left=this.fileLength;
       this.totalPieces=this.tInfo.piece_hashes.length;
       this.pieceLength=this.tInfo.piece_length;

   }
   public void run(){
	   List<Peer> p;
	   
	try{
		userInput  = new BufferedReader(new InputStreamReader(System.in));
		this.writeFile= new RandomAccessFile(this.writeFileName,"rw");
		
		if(this.writeFile.length()!=this.fileLength){
			this.writeFile.setLength(this.fileLength);
		}
		this.setBitfieldStart();
		this.event="started";
		printBitfield();
		System.out.println("length of file:"+tInfo.file_length);
		System.out.println("number of pieces:"+ tInfo.piece_hashes.length);
		p = tracker.announceToTracker(this.downloaded, this.uploaded, this.left, event);
		if(!p.isEmpty()&& p!=null){
			System.out.println("adding peers to list");
			this.addPeers(p);
		}
	}catch (final FileNotFoundException fnfe) {
			System.out.println("Unable to open output file for writing!");
		// Exit right now, since nothing else was started yet
		return;
	}catch (IOException ioe) {
		
		System.out.println("I/O exception encountered when accessing output file!");
				
			
		// Exit right now, since nothing else was started yet
		return;
	}
	catch (Exception e) {
		System.out.println("Error creating/connecting to the tracker!");
		System.exit(1);
	}
	{
		//scheduling the regular announces
		// int interval = this.tracker.getInterval();
	//	this.announceTimer.schedule(new TrackerAnnounce(this),
		//		interval * 1000);
	}

	while(cantStopWontStop){
		System.out.println("In client cantStopWontStop");
		
			try{
			 peerMessage handler= this.toDo.take(); 
			 Message msg= handler.getMessage();
			 Peer peer = handler.getPeer();
				
		 		//System.out.println("Message ID from client queue: "+msg.getMessageId());
			 switch (msg.getMessageId()){
			 case Message.M_KeepAlive:
				 peer.writeMessage(Message.keepAlive);
					break;
				case Message.M_Choke:
					// Update internal state
					peer.setLocalChoked(true);
					break;
				case Message.M_Unchoke:
					// Update internal state
					peer.setLocalChoked(false);

					if (!peer.isLocalChoke() && peer.isLocalInterested()) {
						this.choosePiece(peer);
					} else {
						peer.writeMessage(Message.keepAlive);
					}
					break;
				case Message.M_Interested:
					// Update internal state
					peer.setRemoteInterested(true);

					if (this.unchokedPeers < this.peerLimit) {
						this.unchokedPeers++;
						peer.writeMessage(Message.Unchoke);
						peer.setRemoteChoked(false);
					} else {
						peer.writeMessage(Message.Choke);
						peer.setRemoteChoked(true);
					}
					
					break;
				case Message.M_Uninterested:
					// Update internal state
					peer.setRemoteInterested(false);
					peer.writeMessage(Message.keepAlive);
					break;
				case Message.M_Bitfield:
					// Set peer bitfield
					final Message_Bitfield bitfieldMsg = (Message_Bitfield) msg;
					peer.setPeerBitfield(bitfieldMsg.getBitfield());
					System.out.println("setting bitfield for peer, handling the bitfield message");
					// Inspect bitfield
					peer.setLocalInterested(this.isLocalInterested(peer));
					if (!peer.isLocalChoke() && peer.isLocalInterested()) {
						peer.writeMessage(Message.Interested);
					} else if (peer.isLocalInterested()) {
						peer.writeMessage(Message.Interested);
					} else {
						peer.writeMessage(Message.keepAlive);
					}
					break;
					
					
					
					//check for error
				case Message.M_Have:
					final Message_Have haveMsg = (Message_Have) msg;
					if (peer.getBitField()== null) {
						peer.initializePeerBitfield(this.totalPieces);
					}
					peer.setPeerBitAtIndex(haveMsg.getPieceIndex());
					

					peer.setLocalInterested(this.isLocalInterested(peer));
					if (!peer.isLocalChoke() && peer.isLocalInterested()) {
						peer.writeMessage(Message.Interested);
						peer.setLocalInterested(true);
					} else {
						peer.writeMessage(Message.keepAlive);
					}
					break;
					
					
					
				case Message.M_Request:
					final Message_Request requestMsg = (Message_Request) msg;

					// Check for piece
					if (this.bitfieldBools[requestMsg.getIndex()]) {
						// Send block
						byte[] block = new byte[requestMsg.getBlockLength()];
						System.arraycopy(utils.fileToBytes(this.writeFile),
								requestMsg.getOffset(), block, 0,
								requestMsg.getBlockLength());
						final PieceMessage pieceMsg = new PieceMessage(
								requestMsg.getIndex(),
								requestMsg.getOffset(), block);
						peer.writeMessage(pieceMsg);
					} else {
						// Choke
						peer.writeMessage(Message.Choke);
					}

					break;
				case Message.M_Piece:
					final PieceMessage pieceMsg = (PieceMessage) msg;
					System.out.println("Piece Msg received by client!");
					
					// Verify piece
					if (this.verifyPiece(pieceMsg.getPieceIndex(),
							pieceMsg.getBlockData())) {
						// Updated downloaded
						this.downloaded = this.downloaded
								+ pieceMsg.getBlockData().length;
						if(this.downloaded== this.totalPieces){
							System.out.println("downloading last piece!");
						}
						// Write piece
						this.writeFile.seek(pieceMsg.getPieceIndex()
								* this.pieceLength);
						this.writeFile.write(pieceMsg.getBlockData());
						this.setBitAtIndex(pieceMsg.getPieceIndex());
						// Recalculate amount left to download
						this.left = this.left - pieceMsg.getBlockData().length;
						// Notify peers that the piece is complete
						this.notifyPeers(pieceMsg.getPieceIndex());
						System.out.println("Wrote piece to file!");
						
						
		//need to update client's bitfield, keeps downloading same piece
					} else {
						// Drop piece
						System.out.println("Dropping the piece at"+ pieceMsg.getPieceIndex());
						this.resetBitAtIndex(pieceMsg.getPieceIndex());
					
					}

					if (!peer.isLocalChoke() && peer.isLocalInterested()) {
						this.choosePiece(peer);
					} else {
						peer.writeMessage(Message.keepAlive);
					}
					break;
				default:
					System.out.println("unknown type");
					break;
				}
		 	} catch (InterruptedException ie){
		 		System.out.println("IE Exception from client!");
		 		continue;
		 	}
			catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		 		System.out.println("IO Exception from client!");

				
			} catch (final NullPointerException npe) {
				// TODO Auto-generated catch block
		 		System.out.println("NPE Exception from client!");
				npe.printStackTrace();
			}
		
	}
	
	try {
		while((command=userInput.readLine())!= null){
			if(command.equalsIgnoreCase("terminate")){
				this.shutdownGracefully();
				userInput.close();
			}
		}
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
		 
}
  public void shutdownGracefully() throws IOException {
		System.out.println("Shutting down client.");
		this.cantStopWontStop = false;
		
		// Cancel any upcoming tracker announces
		this.announceTimer.cancel();
		// Disconnect all peers
		if (!this.peers.isEmpty()) {
			for (final Peer peer : this.peers) {
				peer.disconnect();
			}
		}

		this.tracker.announceToTracker(this.getDownloaded(), this.getUploaded(),
				this.getLeft(), "stopped");

		System.exit(1);
	}

  
  
  
  
  
  
  /*
   * problem here, client bitfield being set from here
   * 
   * client bitfield incorrect
   * 
   * 
   */
   /**
	 * Update the bitfield according to the existing file.
	 * allows for client to start off where they finished off last time.
	 * @throws IOException
	 */
   private void setBitfieldStart() throws IOException{
	   final int bytes = (int) Math.ceil(this.totalPieces / 8.0);
		this.bitfield = new byte[bytes];

		for (int pieceIndex = 0; pieceIndex < this.totalPieces; pieceIndex++) {
			byte[] temp;
			if (pieceIndex == this.totalPieces - 1) {
				// Last piece
				temp = new byte[this.fileLength % this.pieceLength];
			} else {
				temp = new byte[this.pieceLength];
			}
			this.writeFile.read(temp);
			if (this.verifyPiece(pieceIndex, temp)) {
				this.setBitAtIndex(pieceIndex);
				this.left = this.left - temp.length;
			} else {
				this.resetBitAtIndex(pieceIndex);
			}
		}
		
   }
	
   
   public void printBitfield(){
	/*
	 * CLIENT BITFIELD IS WRONG, SAYING HAS ALL PIECES BUT 4 AT THE END
	 * 
	 */
	for(int i=0; i<this.bitfieldBools.length; i++){
		if(this.bitfieldBools[i]){
			System.out.println("True");
		}
		else{
			System.out.println("False");
		}
	}
   }
   
	private void setBitfield(byte[] bitfield) throws IOException {
			this.bitfield=bitfield;
			this.bitfieldBools=utils.bitsToBool(bitfield);
			return;
	}
	/**
	 * takes the piece index and the block of the given piece to verify the piece hash 
	 * @param pieceIndex
	 * @param block
	 * @return true for verified false for unverified
	 * @throws IOException
	 */
	private boolean verifyPiece(final int pieceIndex, final byte[] block)
			throws IOException {

		final byte[] piece = new byte[block.length];
		System.arraycopy(block, 0, piece, 0, block.length);
		//byte array to hold sha hash at the piece index
		byte[] hashCheck = null;
		//messagedigest used to run the sha-1 hash alg
		MessageDigest sha;
		
			try {
				sha = MessageDigest.getInstance("SHA-1");
				sha.update(piece);
				hashCheck = sha.digest();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				System.out.println("messagedigest unable to find sha-1 alg");
			}	
		if (MessageDigest.isEqual(this.tInfo.piece_hashes[pieceIndex].array(), hashCheck)) {
			System.out.println("the piece at index"+ pieceIndex +"has been verified");
			return true;
		}
		System.out.println("the piece at index"+ pieceIndex +"did not match the sha1");
		return false;
	}

private void setBitAtIndex(int pieceIndex) throws IOException{
	byte[] temp= this.getBitfield();
	temp=utils.setBitfieldAt(temp,pieceIndex);
	this.setBitfield(temp);
}

private void resetBitAtIndex(int pieceIndex)throws IOException{
	byte[] temp= this.getBitfield();
	temp=utils.resetBitfieldAt(temp,pieceIndex);
	this.setBitfield(temp);
}
private void addPeers(List<Peer> p){
	
	for(Peer newGuy :p){//
		if(newGuy!=null && (newGuy.getIP().equals("128.6.171.131") || newGuy.getIP().equals("128.6.171.130")) ){
	//	if(newGuy!=null && (newGuy.getIP().equals("128.6.171.131") ) ){
	
			if(!this.peers.contains(newGuy)){
				if(newGuy.getIP().equals("128.6.171.130")&& this.onethirty==false){
					this.onethirty=true;
					this.peers.add(newGuy);
					System.out.println(" -added peer:"+newGuy.getIP()+ " to list of peers");
					newGuy.setClient(this);
					newGuy.setToDo(this.toDo);
					Thread t= new Thread(newGuy);
					t.start();
				}if(newGuy.getIP().equals("128.6.171.131")&& this.onethirtyone==false){
					this.onethirtyone=true;
					this.peers.add(newGuy);
					System.out.println(" -added peer:"+newGuy.getIP()+ " to list of peers");
					newGuy.setClient(this);
					newGuy.setToDo(this.toDo);
					Thread t= new Thread(newGuy);
					t.start();
				}
			}
			
		}
	}
	return;
	
} 
private void notifyPeers(int indexDownloaded){
	for(Peer p: this.peers){
		Message_Have have= new Message_Have(indexDownloaded);
		try {
			p.writeMessage(have);
			System.out.println("Sending have msg for index: "+have.getPieceIndex());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
private boolean isLocalInterested(Peer p){
	final boolean[] peersBit=p.getBoolean();
	int Index;
	for(Index=0;Index<this.totalPieces;Index++){
		if(!this.bitfieldBools[Index] && peersBit[Index]){
			System.out.println("interested in this peer!");
			return true;
		}
	}
	return false;
}




/*
 * 
 *   found it
 * 
 * 
 */
//error choosing same piece # over and over, only 434 and 432
private void choosePiece(final Peer p) throws IOException{
	//is it updating client boolean array?
	
	//final boolean[] peersBit= utils.bitsToBool(p.getBitField());
	final boolean[] peersBit=p.getBoolean();
	
	/*
	//prints peer's boolean array
	for(int i=0; i<peersBit.length; i++){
		if(peersBit[i]){
			System.out.println("True");
		}
		else{
			System.out.println("False");
		}
	}
*/
	

	 
	for (int i=0; i<this.totalPieces; i++){
		if(!this.bitfieldBools[i] && peersBit[i]){
			int reqPieceLength=0;
			//if last piece
			if(i == this.totalPieces-1){
				reqPieceLength=this.fileLength % this.pieceLength;
				
			}else{
				reqPieceLength=this.pieceLength;
			}
			System.out.println("Choosing piece index : " + i);
			p.request(i, reqPieceLength);
			break;
		}
	}
}
}//end public class
