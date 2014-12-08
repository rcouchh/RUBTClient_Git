package cs352.bittorrent.download;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import cs352.bittorrent.customTools.utils;
import cs352.bittorrent.givenTools.TorrentInfo;
import cs352.bittorrent.main.RUBTClient;
import cs352.bittorrent.messages.Message;
import cs352.bittorrent.messages.peerMessage;
import cs352.bittorrent.messages.Message.Message_Bitfield;
import cs352.bittorrent.messages.Message.Message_Have;
import cs352.bittorrent.messages.Message.Message_Request;
import cs352.bittorrent.messages.Message.PieceMessage;


public class IncomingConnection {
	//starting piece of Bit torrent handshake protocol
	private static final byte[] Protocol = {'B', 'i', 't', 'T', 'o',
		'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o',
		'l' };	
	DataInputStream peerIS;
	DataOutputStream peerOS;
	Socket peerSocket;
	private ServerSocket incoming;
	private int port;
	float upRate;
	TorrentInfo tInfo;
	RUBTClient client;
	public byte[] peerBitfield = null;
	private boolean is_alive=false;
	private boolean handshake_success=false;
	
	
	
	IncomingConnection(Tracker tracker, TorrentInfo tInfo, RUBTClient client){
		
		is_alive = true;
		port = tracker.getPort();
		this.tInfo = tInfo;
		this.client = client;
		
	}
	
	public void run(){	
		try{
			this.incoming = new ServerSocket(port);
			
			
			/*
			 * listens for incoming connection attempts from peers
			 */
			peerSocket = incoming.accept();
			InetAddress peerIP = peerSocket.getInetAddress();
			int peerPort = peerSocket.getPort();
			this.peerIS = new DataInputStream(peerSocket.getInputStream());
			this.peerOS = new DataOutputStream(peerSocket.getOutputStream());
		
			System.out.println("Incoming connection!");
			
			//read handshake
			byte[] peerMsg = new byte[68];
			peerIS.read(peerMsg);
			boolean match = true;
			
			//verify handshake
			
			//verify length
			if(peerMsg.length!=68){
				System.out.println("Incorrect length of response!");
				peerSocket.close();
				match=false;
			}
			
			//verify protocol
			final byte[] otherProtocol = new byte[19];
			System.arraycopy(peerMsg, 1, otherProtocol, 0, Protocol.length);
			if (!Arrays.equals(otherProtocol, Protocol)) {
				System.out.println("Incorrect protocol of response!");
				peerSocket.close();
				match=false;
			}
			// Check info hash against info hash from .torrent file
			final byte[] otherInfoHash = new byte[20];
			System.arraycopy(peerMsg, 28, otherInfoHash, 0, 20);
			if (!Arrays.equals(otherInfoHash, this.tInfo.info_hash.array())) {
				System.out.println("Info hashes dont match!");
				peerSocket.close();
				match=false;
			}
			
			if(match){
				System.out.println("Peer handshake msg verified!");
				handshake_success = true;
				
				
				//generate and send response handshake
					System.out.println("Generating response handshake msg...");
					
					//allocate bytes for handshake
					byte[] handshakeMsg = new byte[68];
					
					//start msg with bytes 19Bitorrent protocol
					handshakeMsg[0] = 19;
					System.arraycopy(Protocol, 0, handshakeMsg, 1, Protocol.length);
					
					//add info_hash
					System.arraycopy(this.client.tInfo.info_hash, 0, handshakeMsg, 28, this.client.tInfo.info_hash.array().length);

					//add peer_id, should match info_hash
					System.arraycopy(this.client.clientId, 0, handshakeMsg, 48, this.client.clientId.length);
					System.out.println("Handshake Msg: ");
					System.out.println(new String(handshakeMsg));

					
					System.out.println("Handshake message generated successfully.");
					
					System.out.println("sending handshake msg");
					this.peerOS.write(handshakeMsg);
					this.peerOS.flush();
				
				
			}
			
		}
		catch(IOException io){
			
		}
		
		
	}
	
	 public static void sendunchoke( Socket peerSocket, DataOutputStream client2peer, DataInputStream peer2client) throws IOException {
		 Message.write(client2peer, Message.Unchoke);
		 
		// this.isClientchoking=false;
		 }
		 public static void sendchoke( Socket peerSocket, DataOutputStream client2peer, DataInputStream peer2client) throws IOException {
		 Message.write(client2peer, Message.Choke);
		// this.isClientchoking=true;
		 }
	
	
	
}
