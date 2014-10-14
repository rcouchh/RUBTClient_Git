
import java.io.*;
import java.net.*;
import java.util.Random;

import GivenTools.*;

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
        // java -cp . RUBTClient somefile.torrent picture.jpeg
        //args[0]="http://128.6.5.130:6969/announce";
        //args[1]="6969";
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
          localClient = new RUBTClient(tInfo,args[1]);
          localClient.start();
     
        //initialize tracker, make connection
        System.out.println("Initializing tracker, connecting...");
        
        
        
    }//end main
 
    public final TorrentInfo tInfo;// torrentinfo object
  
    private final String writeFileName; // name of desired file
	private final byte[] clientId= generateMyPeerId();
	private Peer peer;
    private RandomAccessFile writeFile;//actual file
    private int fileLength;//length of write file
    public String event; //event passed from tracker
	private int port= 6881;
	//hard code first 4 bytes of client PID
	static final byte[] First_Bytes = { 'R', 'C', 'D', 'T' };
	
	//clients bitfield
	private byte[] bitfield;
	
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
	//boolean to keep all threads running until done
	private volatile boolean dontStop= true;
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
synchronized void addDownloaded(int down){
	System.out.println("Client has downloaded:"+ this.downloaded+down);
	this.downloaded+=down;
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
	try{
		this.writeFile= new RandomAccessFile(this.writeFileName,"rw");
		
		if(this.writeFile.length()!=this.fileLength){
			this.writeFile.setLength(this.fileLength);
		}
		peer = tracker.announceToTracker(this.downloaded, this.uploaded, this.left, event);
		System.out.println("got peer:"+peer.peerID.toString());
		peer.start();
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

}
private static byte[] generateMyPeerId() {
final byte[] peerId = new byte[20];
// Hard code the first four bytes for easy identification
System.arraycopy(First_Bytes, 0, peerId, 0,
First_Bytes.length);
// Randomly generate remaining 16 bytes
final byte[] random = new byte[16];
new Random().nextBytes(random);
System.arraycopy(random, 0, peerId, 4, random.length);
return peerId;
}


  


}//end public class
