package cs352.bittorrent.customTools;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Random;

import cs352.bittorrent.download.Peer;
/**
 * 
 * @author Dan Torres,Ryan Couch
 * 
 *
 */
public class utils {
	//hard code first 4 bytes of client PID
	private static final byte[] First_Bytes = { 'R', 'C', 'D', 'T' };
	private final static char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5',
		 	'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	/**
	 * method to generate unique peerid for client
	 * @return
	 */
	public static byte[] generateMyPeerId() {
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
	
	/*
	 * Method prints list of peers with their corresponding data
	 * @param list of peers to be traversed
	 */
	    public static void printPeers(LinkedList<Peer> peers){
	    	for(int i=0; i<peers.size(); i++){
	    		System.out.println("Peer "+i+": ");
	    		System.out.println("Peer IP: "+peers.get(i).getIP());
	    		System.out.println("Peer Port: "+peers.get(i).getPort());
	    		System.out.println("Peer_ID: "+new String(peers.get(i).getPeerId()));
	    		System.out.println("");
	    		System.out.println("");
	    		
	    	}
	    }
	    
	    /** Encode byte data as a hex string... hex chars are UPPERCASE*/
	    public static String bytesToHexStr(final byte[] byteArr) {
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
	    
	    /*
	     * Method iterates through list of peers for phase2: entry with 128.6.171.130 and 128.6.171.131
	   	 * @return peer with the correct Ip prefix
	     */
	    //  128.6.171.130 and 128.6.171.131
	    public static LinkedList <Peer> findPeersWithPrefix(LinkedList<Peer> p){
	    	System.out.println("Searching for peer!");
	    	LinkedList<Peer> temp= new LinkedList<Peer>();
	    	final LinkedList<Peer> ret = new LinkedList<Peer>();
	    	for( temp= p; temp!=null; temp.pop()){
	    		//byte[]peerID =temp.peek().getPeerId();
	    		String ip = temp.peek().getIP();
	    				//if ID has prefix 'RUBT' or '-AZ5400'
	    		if(ip.equals("128.6.171.130") || ip.equals("128.6.171.131")){
	    		System.out.println("Correct peer found!");
	        		System.out.println("Peer ID: "+ip);
	        		Peer add= temp.peek();
	    			ret.add(add);
	    		}	
	    	}
	    	return ret;
	    }
	    /*
	     * takes the bitfield and iterates through determining the boolean representation
	     * for each piece, useful for determining if a peer is desirable and if peer is seed
	     * @param bitfield
	     * @return boolean representation of bitfield
	     */
	   public static boolean[] bitsToBool(byte[] bitfield){
		   int numBits= bitfield.length*8;
		   boolean[] bool = new boolean[numBits];
		   int currBool=0;
		   //Iterate through byte array
		   for (int currByte=0; currByte< bitfield.length; currByte++){
			   //iterate through individual bits within each cell of byte array bytes are big-endian go right to left
			   for(int currBit=7; currBit>=0; currBit--){
				  
				  if((bitfield[currByte] >> currBit & 0x01)==1){
					  bool[currBool]= true;
				  }else{
					  bool[currBool]=false;
				  }
				  currBool++;
			   	}
		   	}
		   return bool;
	   }
	   //returns byte representation of file
	   public static byte[] fileToBytes(final RandomAccessFile file)
				throws IOException {
			final byte[] ret = new byte[(int) file.length()];
			file.readFully(ret);
			return ret;
		}
	 public static String printPeer(Peer p){
		 String ret="peerid:"+(String)p.getPeerId().toString()+"\n"+"peer ip:"+p.getIP();
		 return ret;
	 }
	 public static byte[] setBitfieldAt(byte[]bitfield, int pieceIndex){
		 //get byte index 
		 int byteIndex= pieceIndex/8;
		 //get bits position in the byte
		 int positionInByte= pieceIndex % 8;
		 byte curr= bitfield[byteIndex];
		 //or the current byte and the 1 being moved into its position at index
		 bitfield[byteIndex]= (byte) (curr | 1 << positionInByte);
		 return bitfield;
	 }
	 public static byte[] resetBitfieldAt(byte[] bitfield, int pieceIndex){
		//get byte index 
		 int byteIndex= pieceIndex/8;
		 //get bits position in the byte
		 int positionInByte= pieceIndex % 8;
		 byte curr= bitfield[byteIndex];
		 bitfield[byteIndex] =(byte)(curr & ~(1 << positionInByte));
		 return bitfield;
	 }
}
