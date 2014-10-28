

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import GivenTools.Bencoder2;
import GivenTools.BencodingException;
import GivenTools.ToolKit;
import GivenTools.TorrentInfo;
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
    private int downloaded;
    private int left;
    private int uploaded;
    private URL url;
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
 private final static char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5',
 	'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
 

 
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
          String idURL = bytesToURL(clientId);
          this.peer_id = idURL;
          this.info_hash = info_hash;
          String iHash = bytesToURL(info_hash);
          this.info_Hash_url=iHash;
          this.port= port;
        
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
				peer=findPeerWithPrefix(peersList);
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
        	final Peer peer = new Peer(peer_ID, peer_IP, peer_port, info_hash, clientID);
        	//peerList.add(peer);
        	peers.add(peer);
    	}
    	printPeers(peers);
    	return peers;
    	
    }//end getpeers
    
    
    /*
     * Method iterates through list of peers for phase2: entry with 128.6.171.130 and 128.6.171.131
   	 * @return peer with the correct Ip prefix
     */
    
    public static Peer findPeerWithPrefix(LinkedList<Peer> p){
    	System.out.println("Searching for peer!");
    	LinkedList<Peer> temp;
    	for( temp= p; temp!=null; temp.pop()){
    		byte[] peerID =temp.peek().peerID;
    		String id = new String(peerID);
    		
    		//if ID has prefix 'RUBT' or '-AZ5400'
    		if(id.startsWith("RUBT") || id.startsWith("-AZ5400")){
    			System.out.println("Correct peer found!");
        		System.out.println("Peer ID: "+id);
    			return temp.peek();
    		}	
    	}
    	return null;
    }
    
/*
 * Method prints list of peers with their corresponding data
 * @param list of peers to be traversed
 */
    public static void printPeers(LinkedList<Peer> peers){
    	for(int i=0; i<peers.size(); i++){
    		System.out.println("Peer "+i+": ");
    		System.out.println("Peer IP: "+peers.get(i).getIP());
    		System.out.println("Peer Port: "+peers.get(i).getPort());
    		System.out.println("Peer_ID: "+new String(peers.get(i).peerID));
    		System.out.println("");
    		System.out.println("");
    		
    	}
    }
    
    
    
    
    
    
    
    /** Encode byte data as a hex string... hex chars are UPPERCASE*/
    public static String encode(byte[] data){
        if(data == null || data.length==0){
            return "";
        }
        char[] store = new char[data.length*2];
        for(int i=0; i<data.length; i++){
            final int val = (data[i]&0xFF);
            final int charLoc=i<<1;
            store[charLoc]=CHAR_FOR_BYTE[val>>>4];
            store[charLoc+1]=CHAR_FOR_BYTE[val&0x0F];
        }
        return new String(store);
    }
    

    protected static String bytesToHexStr(final byte[] byteArr) {
    	final char[] charArr = new char[byteArr.length * 2];
    	for (int i = 0; i < byteArr.length; i++) {
    		final int val = (byteArr[i] & 0xFF);
    		final int charLoc = i << 1;
    		charArr[charLoc] = HEX_CHARS[val >>> 4];
    		charArr[charLoc + 1] = HEX_CHARS[val & 0x0F];
    	}
    	final String hexString = new String(charArr);
    	return hexString;
    }
    /**
     * Converts a byte array to an escaped URL for a BT tracker announce.
     *
     * @param byteArr
     * the byte array to convert
     * @return the URL-converted byte array
     */
    public static String bytesToURL(final byte[] byteArr) {
    	final String hexString = bytesToHexStr(byteArr);
    	final int len = hexString.length();
    	final char[] charArr = new char[len + (len / 2)];
    	int i = 0;
    	int j = 0;
    	while (i < len) {
    		charArr[j++] = '%';
    		charArr[j++] = hexString.charAt(i++);
    		charArr[j++] = hexString.charAt(i++);
    	}
    	return new String(charArr);
    }
    
    
}

