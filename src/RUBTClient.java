
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
public class RUBTClient {

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
        RUBTClient Client = new RUBTClient(tInfo,args[1]);
        System.out.println("yous a bitch ass nigga");
        System.out.println(tInfo.announce_url);

    }//end main

   private final String url;    //announce url to be added to for get request

   private final String peer_id; //unique peer_id

   private final TorrentInfo tInfo;// torrentinfo object

   private final String writeFile; //file to write to

   private final int port=6881;//port for client to listen on try 6881 first

   private final int fileLength;//length of file to download

   private final int pieceLength;//length of piece to be downloaded

   private final int downloaded;// number of bytes downloaed by client

   private final int left;//number of bytes left to be downloaded

   private final int uploaded;// number of bytes uploaded by client to peer;



   private final byte[] info_hash;

   public RUBTClient(TorrentInfo info, String WriteFile){

       this.tInfo= info;
       this.writeFile= WriteFile;
       this.url= tInfo.announce_url.toString();
       this.downloaded= 0;
       this.left=tInfo.file_length;
       this.uploaded=0;
       this.peer_id= generatePeerId();
       this.info_hash=tInfo.info_hash.array();
        this.pieceLength=tInfo.piece_length;
    this.fileLength=tInfo.file_length;

       try {
        Tracker tracker= new Tracker(this.url,this.peer_id,this.port,this.info_hash,this.downloaded,this.left,this.uploaded);
    } catch (UnsupportedEncodingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }



   }




    //method to generate a random peer_id
    public static String generatePeerId(){
        //first 4 digits remain the same to identify
        String str = "8008";

        //add 16 other random numbers to string
        for(int i=4; i<20; i++){
            str = str+((int)Math.floor(Math.random()*10)+1);
        }
        return str;
    }



}//end public class
