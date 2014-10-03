import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

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

public class Tracker extends RUBTClient{

    @SuppressWarnings("deprecation")
    public Tracker(String AnnounceUrl, String peer_id, byte[] infoHash,String port ) throws UnsupportedEncodingException{
          String u= AnnounceUrl;
          String pe= peer_id;
          String iHash= encode(infoHash);
          iHash=URLEncoder.encode(iHash,"UTF-8");
          String p= port;
          URL url=null;
        try {
            url = new URL(u+"?"+iHash+"&"+peer_id);
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
                ToolKit.printString(data, true, data.length);


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
