package Client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;


import customTools.utils;
import GivenTools.*;

/*
http://some.tracker.com:999/announce
?info_hash=12345678901234567890
&peer_id=ABCDEFGHIJKLMNOPQRST
&ip=255.255.255.255
&port=6881
&downloaded=1234
&left=98765
&event=stopped
*/


public class Tracker {
    private String AnnounceUrl;
    private String peer_id;
    private int port;
    private static byte[] info_hash;
    private String info_Hash_url;
    private int interval;
    private URL url;
    private static int totalPieces;
    public static byte[] clientID;
    private Peer peer;
    //private static ArrayList<Peer> peerList = new ArrayList<Peer>();
    //private static ArrayList<String> peerIPList = new ArrayList<String>();
     HttpURLConnection conn;
     byte[] trackerBytes = null;
      DataInputStream is;
      ByteArrayOutputStream buff;
      int connected=0;
 static final char[] CHAR_FOR_BYTE = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
 public static ByteBuffer keyFAILURE = ByteBuffer.wrap(new byte[]{'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ', 'r', 'e', 'a', 's', 'o', 'n'});     
 private static final ByteBuffer Key_Peers = ByteBuffer.wrap(new byte[]{'p','e','e','r','s'});
 private static final ByteBuffer Key_Interval = ByteBuffer.wrap(new byte[]{'i','n','t','e','r','v','a','l'});
 private static final ByteBuffer Key_IP = ByteBuffer.wrap(new byte[] { 'i','p' });
 private static final ByteBuffer Key_Peer_ID = ByteBuffer.wrap(new byte[] {'p', 'e', 'e', 'r', ' ', 'i', 'd' });
 private static final ByteBuffer Key_Port = ByteBuffer.wrap(new byte[] {'p', 'o', 'r', 't' });

 

 
 /*
  * Method: Generates a connection to the tracker and reads in its response 
  * @return Map of the tracker's response
  * 
  */
 	@SuppressWarnings("unchecked")
	public Map<ByteBuffer, Object> connect(URL url)throws IOException{
 	     Map<ByteBuffer, Object> trkMap = null;

 		
 		//attempt connection with tracker
        try{	
        		
        		System.out.println(url.toString());
                conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("GET");

                //get response code to see if connection success
                connected = conn.getResponseCode();
                System.out.println("Response code: "+connected);
                
        } catch(Exception e){
                	System.out.println("Error connecting to tracker");
                }
        
        try{
                //receive response
                // read each byte from input stream, write to output stream
                is = new DataInputStream(conn.getInputStream());
               // buff = new ByteArrayOutputStream();
                int size = conn.getContentLength();
                trackerBytes = new byte[size];
                is.readFully(trackerBytes);
                is.close();   
        }catch(Exception e){
        	System.out.println("Error recieving response from tracker!");
        	System.exit(1);
        }
                
                //decode tracker response (byte array) to Map
                try{
                trkMap = (Map<ByteBuffer, Object>)Bencoder2.decode(trackerBytes);
         
            }catch(BencodingException be){
                System.out.println("Bencoding error for decoding tracker response!");
                System.exit(1);
                // e.printStackTrace();
            }
         
        //setpeerIPList(trkMap);
        //System.out.println(peerIPList);
        
 		return trkMap;
	
 	} //end connect method
 
 
 
    public Tracker(final byte[]clientId, final byte[] info_hash, final String announceUrl, final int port,TorrentInfo t) throws UnsupportedEncodingException{
          this.AnnounceUrl=announceUrl;
          this.clientID = clientId;
          String idURL = utils.bytesToURL(clientId);
          this.peer_id = idURL;
          this.info_hash = info_hash;
          String iHash = utils.bytesToURL(info_hash);
          this.info_Hash_url=iHash;
          this.port= port;
          this.totalPieces=t.piece_hashes.length;
        
    }
    
    public Peer announceToTracker(int bytesDownloaded, int bytesUploaded, int bytesLeft, String event){
    	 LinkedList<Peer> peersList;
    	String urlString = (AnnounceUrl+"?"
    		        +"info_hash="+info_Hash_url
    		        +"&peer_id="+peer_id+"&port="+port+
    		        "&uploaded="+bytesUploaded+
    		        "&downloaded="+bytesDownloaded+"&left="+bytesLeft);
    	 		if(event !=null && !event.isEmpty()){
    	 		urlString+="&event="+event;    	 	
    	 		}
    	 			
    		 try {
    	         url = new URL(urlString);
    	         
    	     } catch (MalformedURLException e1) {
    	    	 System.out.println("Error creating the URL!");
    	    	 return null;
    	     } 
    		 try {
				Map<ByteBuffer,Object> c= connect(url);
				this.interval=(Integer)c.get(Key_Interval);
				System.out.println("trackerInterval: " + interval);
				System.out.println("getting peerslist..");
				peersList=getPeers(c);
				peer=utils.findPeerWithPrefix(peersList);
				if(peer== null){
					System.out.println("peer not found!");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		 return peer;
    		 
    }
    /*
     * Method retrieve list of peers from tracker response
     */
    @SuppressWarnings("unchecked")
	public static LinkedList<Peer> getPeers(Map<ByteBuffer, Object> map) throws UnsupportedEncodingException {
    	System.out.println("Decoding peers");
    	
    	ArrayList<Map<ByteBuffer, Object>> encodedPeers = null;
    	if(map.containsKey(Key_Peers)){
    		System.out.println("Contains");
    		encodedPeers = (ArrayList<Map<ByteBuffer, Object>>)map.get(Key_Peers);
    		
    		//print encoded map
    	//	ToolKit.print(encodedPeers);
    	}    	
    	else{
    		System.out.println("No peer list in tracker response!");
    	}
 
    	final LinkedList<Peer> peers = new LinkedList<Peer>();
    	for(final Map<ByteBuffer, Object> peerMap : encodedPeers){
    		
    		//retrieve peer_id
    		ByteBuffer peer_IdBuff;
    		byte[] peer_ID = null;
    		if(peerMap.containsKey(Key_Peer_ID)){
    			peer_IdBuff = (ByteBuffer)peerMap.get(Key_Peer_ID);
    			peer_ID = peer_IdBuff.array();
    			//System.out.println(pID);
    		}
    		//retrieve peer IP
    		String peer_IP = null;
    		if(peerMap.containsKey(Key_IP)){
    			final ByteBuffer peer_IPBuff = (ByteBuffer)peerMap.get(Key_IP);
    			peer_IP = new String(peer_IPBuff.array(), "UTF-8");
    		}
    		//retrieve peer port
    		int peer_port = -1;
    		if(peerMap.containsKey(Key_Port)){
    			peer_port = (Integer)peerMap.get(Key_Port);
    		}
        	final Peer peer = new Peer(peer_ID, peer_IP, peer_port, info_hash, clientID,totalPieces);
        	peers.add(peer);
    	}
    	utils.printPeers(peers);
    	return peers;
    	
    }//end getpeers
    
}

