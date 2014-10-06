import java.io.*;
public class Message {

	//message-IDs for peer communication
	public static final byte M_Choke = 0;
	public static final byte M_Unchoke= 1;
	public static final byte M_Interested =2;
	public static final byte M_Uninterested =3;
	public static final byte M_Have = 4;
	public static final byte M_Request=6;
	
	public static final byte M_Piece= 7;
	
	public static final Message Choke= new Message(1,M_Choke);
	public static final Message Unchoke= new Message(1,M_Unchoke);
	public static final Message Interested= new Message(1,M_Interested);
	public static final Message Uninterested= new Message(1,M_Uninterested);
	public static final Message Have= new Message(5,M_Have);
	public static final Message Request= new Message(13,M_Request);
	public static final Message keepAlive=new Message(0,(byte)0);
	
	
	private final int length;
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
	
	public static Message readDataIN(final DataInputStream readMe) throws IOException{
		final int length = readMe.readInt();
		if(length==0){
			return Message.keepAlive;
		}
		final byte ID= readMe.readByte();
		int pieceIndex;
		int byteOffset;
		int request_length;
		byte[] block;
		
		switch(ID){
		case M_Choke:
			return Choke;
		case M_Unchoke:
			return Unchoke;
		case M_Interested:
			return Interested;
		case M_Uninterested:
			return Uninterested;
		case M_Have:
			pieceIndex=readMe.readInt();
			return new Message_Have(pieceIndex);
		case M_Request:
		pieceIndex=readMe.readInt();
		byteOffset=readMe.readInt();
		request_length= readMe.readInt();
		return new Message_Request(pieceIndex,byteOffset,request_length);
		
		case M_Piece:
		pieceIndex=readMe.readInt();
		byteOffset=readMe.readInt();
		block= new byte[length-9];
		readMe.readFully(block);
		return new PieceMessage(pieceIndex,byteOffset,block);
		
		default:
			break;
		}
			
		return null;
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
	
}
