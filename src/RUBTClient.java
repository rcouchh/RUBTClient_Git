
import java.io.*;
import java.net.*;

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
	   public final TorrentInfo tInfo;// torrentinfo object

	   public final String writeFile; //file to write to
	
	   public String event; //event passed from tracker
	   
	   //hard code first 4 bytes of client PID
	   static final byte[] First_Bytes = { 'R', 'C', 'D', 'T' };

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
        
        //initialize client
        client = new RUBTClient(tInfo,args[1]);
     
        //initialize tracker, make connection
        System.out.println("Initializing tracker, connecting...");
        try {
			Tracker track = new Tracker(client);
			
		} catch (Exception e) {
			System.out.println("Error creating/connecting to the tracker!");
			System.exit(1);
		}
        
        
    }//end main






   public RUBTClient(TorrentInfo info, String WriteFile){

       this.tInfo= info;
       this.writeFile= WriteFile;
   

  



   }




  


}//end public class
