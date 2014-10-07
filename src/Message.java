import java.io.*;
import java.util.logging.Logger;

public class Message {
	
	
	
	/**
	 * message IDs for peer communication
	 */
	public static final byte M_Choke = 0;
	public static final byte M_Unchoke= 1;
	public static final byte M_Interested =2;
	public static final byte M_Uninterested =3;
	public static final byte M_Have = 4;	
	public static final byte M_Bitfield=5;
	public static final byte M_Request=6;
	public static final byte M_Piece= 7;
	public static final byte M_KeepAlive=-1;
	/**
	 * Messages for peer communication
	 */
	public static final Message Choke= new Message(1,M_Choke);
	public static final Message Unchoke= new Message(1,M_Unchoke);
	public static final Message Interested= new Message(1,M_Interested);
	public static final Message Uninterested= new Message(1,M_Uninterested);
	public static final Message keepAlive=new Message(0,M_KeepAlive);
	
	/**
	 * length of message
	 */
	private final int length;
	/**
	 * message id of message (byte val)
	 */
	private final byte MessageID;
	
	
	protected Message(final int length, final byte M_ID){
		this.length= length;
		this.MessageID= M_ID;
	}
	
	public byte getMessageId() {
		return this.MessageID;
	}
	public void writePayload(final DataOutputStream dos) throws IOException {
		// Nothing here
	}
	/**
	 * 
	 * @param readMe
	 * the dataInputStream with the peer message
	 * @return message on the stream, waiting until message arrives
	 * @throws IOException
	 */
	public static Message read( DataInputStream readMe) throws IOException{
		
		final int length = readMe.readInt();
		//length 0 => keepAlive message
		if(length==0){
			return Message.keepAlive;
		}
		//get messageID
		final byte ID= readMe.readByte();
		
		int pieceIndex;
		int byteOffset;
		int request_length;
		byte[] block;
		
		switch(ID){
		//choke message
		case M_Choke:
			return Choke;
		//unchoke message
		case M_Unchoke:
			return Unchoke;
		//interested message
		case M_Interested:
			return Interested;
		//uninterested message
		case M_Uninterested:
			return Uninterested;
		//have message
		case M_Have:
			pieceIndex=readMe.readInt();
			return new Message_Have(pieceIndex);
		//request message
		case M_Request:
		pieceIndex=readMe.readInt();
		byteOffset=readMe.readInt();
		request_length= readMe.readInt();
		return new Message_Request(pieceIndex,byteOffset,request_length);
		//piece message
		case M_Piece:
		pieceIndex=readMe.readInt();
		byteOffset=readMe.readInt();
		block= new byte[length-9];
		readMe.readFully(block);
		return new PieceMessage(pieceIndex,byteOffset,block);
		//bitfield message
		case M_Bitfield:
		byte[] bitfield = new byte[length-1];
		readMe.readFully(bitfield);
		return new Message_Bitfield(length, bitfield);
		
		default:
			break;
		}
			
		return null;
	}
	
	public void write(final DataOutputStream outStream, Message msg) throws IOException {
		
		if (msg.length > 0) {
			try{
			outStream.writeInt(this.length);
			switch(msg.MessageID){
			case M_Bitfield:{
				Message_Bitfield message= (Message_Bitfield) msg;
				byte[] bytes =message.getBitfield();
				outStream.write(bytes);
				break;
			}
			case M_Piece:{
			PieceMessage pm= (PieceMessage) msg;
			outStream.writeInt(pm.pieceIndex);
			outStream.writeInt(pm.offset);
			outStream.write(pm.block);
			break;
			}
			case M_Request:{
				Message_Request rm= (Message_Request) msg;
				outStream.writeInt(rm.pieceIndex);
				outStream.writeInt(rm.offset);
				outStream.writeInt(rm.length);
				break;
			}
			case M_Have:{
				Message_Have hm = (Message_Have) msg;
				outStream.writeInt(hm.Index);
				break;
			}
		
				
			}
			outStream.flush();
			}catch(NullPointerException npe){
			    throw new IOException("Cannot write to a null stream.");
			  }
			//outStream.writeByte(this.MessageID);
			// Write payload for Have, Bitfield, Request, Piece, Cancel
			// messages
			//this.writePayload(outStream);
		}

		outStream.flush();
	}
	public static class Message_Have extends Message{
		private final int Index;
		
		public Message_Have(int PieceIndex){
			super(4,M_Have);
			this.Index=PieceIndex;
		}
		public void sendPayLoad(DataOutputStream os ) throws IOException{
			os.writeInt(this.Index);
		}
	}
	
	public static class Message_Request extends Message{
		private final int pieceIndex;
		private final int offset;
		private final int length;
		public Message_Request(int index, int offset, int length){
			super(13, M_Request);
			this.pieceIndex=index;
			this.offset=offset;
			this.length=length;
		}
		public void setPayload(final DataOutputStream os) throws IOException {
			os.writeInt(this.pieceIndex);
			os.writeInt(this.offset);
			os.writeInt(this.length);
		}

	}
	public static class PieceMessage extends Message{
		private final int pieceIndex;
		private final int offset;
		private final byte[] block;
		
		public  PieceMessage(int pieceIndex, int offset, byte[] b){
				super(9+ b.length, M_Piece);
				this.pieceIndex=pieceIndex;
				this.offset=offset;
				this.block=b;
		}
		public void setPayload(final DataOutputStream os) throws IOException {
			os.writeInt(this.pieceIndex);
			os.writeInt(this.offset);
			os.write(this.block);
		}
	}
	public static class Message_Bitfield extends Message{
		private final byte[] bitfield;
		public Message_Bitfield(int length,byte[] bitfield){
				super(length +1, M_Bitfield);
				this.bitfield=bitfield;
		}
		
		public void setPayload(final DataOutputStream os) throws IOException {
			os.write(this.bitfield);
		}
		public byte[] getBitfield(){
			return this.bitfield;
		}
	}
	
}
