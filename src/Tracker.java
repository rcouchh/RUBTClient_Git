import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import GivenTools.Bencoder2;
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
    private String info_Hash_url;
     int downloaded;
     int left;
     int uploaded;
     URL url;
    @SuppressWarnings("deprecation")
    public Tracker(String AnnounceUrl, String peer_id,int port, byte[] infoHash, int downloaded, int left, int uploaded ) throws UnsupportedEncodingException{
          this.AnnounceUrl= AnnounceUrl;
          this.peer_id= peer_id;
          String iHash= encode(infoHash);
          this.info_Hash_url=URLEncoder.encode(iHash,"UTF-8");
          this.port= port;
         this.uploaded= uploaded;
          this.downloaded= downloaded;
          this.left=left;


        try {
            this.url = new URL(AnnounceUrl+"?"+"info_hash="+info_Hash_url+"&"+"peer_id="+peer_id+"port="+port+"&"+"uploaded="+uploaded+"&"+"downloaded="+downloaded+"&"+"left="+left);
        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
          HttpURLConnection conn;
          int connected=0;
        try{

                conn = (HttpURLConnection)url.openConnection();
                //conn.setRequestMethod("GET");

                //get response code to see if connection success
                connected = conn.getResponseCode();
                System.out.println("Response code: "+connected);

                // read each byte from input stream, write to output stream
                final InputStream is = conn.getInputStream();
                final ByteArrayOutputStream buff = new ByteArrayOutputStream();
                final byte[] data = new byte[16384];
                int i;
                while((i = is.read(data, 0, data.length))!= -1){
                    buff.write(data, 0, i);
                }
                Bencoder2.decode(data);
                System.out.println(url.toString());


            }catch(IOException e){
                System.out.println("IOException error!");
                e.printStackTrace();
            }catch(Exception e){
                System.out.println("Exception error!");
                e.printStackTrace();
            }
    }
    static final char[] CHAR_FOR_BYTE = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
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
}
