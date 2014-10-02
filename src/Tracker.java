import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import GivenTools.TorrentInfo;


public class Tracker extends RUBTClient{
	final URL url;
	final byte[] infoHash;
	final int port ;
	String peer_id;
	public Tracker(TorrentInfo Torr){
		    this.url = (Torr.announce_url);
		    this.port= this.url.getPort();
		    this.infoHash= Torr.info_hash.array();
		    this.peer_id=null;	
		    	
		    		
	}
	
}
