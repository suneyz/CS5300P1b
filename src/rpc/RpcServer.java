package rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import servelet.SessionServelet;
import session.Session;

public class RpcServer {
	
	public static final boolean TEST = true;
	/*RPC Server Side method
	 * 
	 */
	public void rpcCallRequestProcessor() throws IOException, ParseException, NumberFormatException{
		//TODO:ÕâÀïÊÇ·ñÐèÒª·ÂÕÕRPCClientRead ½¨Á¢¶à¸öSocket
		DatagramSocket rpcSocket = new DatagramSocket(Utils.PROJECT1_PORT_NUMBER);
		//TODO: figure out what is the difference to be inside(original place) and outside
		System.out.println("server thread initialized!");
        //InetAddress lastReceivedAddr = null;
		while(true){
			//TODO: worried about the memory since this keeps generating new 
			byte[] inBuf = new byte[Utils.MAX_PACKET_LENGTH];
			byte[] outBuf = null;
			DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
			System.out.println("server waiting for incoming packet...");
			rpcSocket.receive(recvPkt);
			System.out.println("server received a packet!");
			
			//execute the following code when there arrives a packet
			
			if( recvPkt.getAddress()!=null ){
				System.out.println("A packet has arrived at server!");
				InetAddress returnAddr = recvPkt.getAddress();
				int returnPort = recvPkt.getPort();
				String requestInfo = new String(recvPkt.getData());
				System.out.println("Server : received String is "+requestInfo);
				String[] requestInfoArray = requestInfo.split(Utils.SPLITTER);
				String operationCode = requestInfoArray[1];
						
				switch(operationCode){
					case Utils.OPERATION_SESSION_READ:
						System.out.println("RPC server received read requset");
						outBuf = sessionRead(requestInfo);
						break;
					case Utils.OPERATION_SESSION_WRITE:
						outBuf = sessionWrite(requestInfo);
						break;	
				}
				
				DatagramPacket sentPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
				rpcSocket.send(sentPkt);	
				System.out.println("Server-packet sent ");
			}
			
		}
	}
	
	/*sessionRead
	 * read session message and return as byte array
	 */
	public byte[] sessionRead(String info) throws ParseException, NumberFormatException{
		
		//information extraction
		
		String[] infoArray = info.split(Utils.SPLITTER);
		String callID = infoArray[0];
		String sessionID = infoArray[2]; 
		Long requestVersionNumber = Long.parseLong(infoArray[3].trim());
		System.out.println("Server received versionNumber "+requestVersionNumber);
		String readMessage="";//version

		
		// there will always be matched session,cookie hasn't time out, session won't time out
		
		Session session = SessionServelet.getSessionByIDVersion(sessionID, String.valueOf(requestVersionNumber));

		if(session == null) {
			return Utils.NOT_FOUND.getBytes();
		}
		//readMessage = session.getMessageByVersionNumber(requestVersionNumber);
		readMessage = session.getMessage();
		
		byte[] outBuf = new byte[Utils.MAX_PACKET_LENGTH];
		
		outBuf = String.join(Utils.SPLITTER, Arrays.asList(callID, sessionID, String.valueOf(requestVersionNumber), readMessage) ).getBytes();
		System.out.println("outBuf is : " + outBuf);
		return outBuf;
	}
	
	/*sessionWrite
	 * write session message and return true or false
	 * */
	 public byte[] sessionWrite(String info) throws ParseException{
		 String[] infoArray = info.split(Utils.SPLITTER);
		 String requestCallID = infoArray[0];
		 String sessionID = infoArray[2];
		 Long requestVersionNumber = Long.parseLong(infoArray[3]);
		 String message = infoArray[4];	
		 String expireTime = infoArray[5].trim();
		 
		 System.out.println("Generate Date input "+expireTime);
		 //SimpleDateFormat formatter = new SimpleDateFormat(expireTime);
		 SimpleDateFormat formatter = new SimpleDateFormat(Utils.DATE_TIME_FORMAT);
		 Date expireDateTime = formatter.parse(expireTime);
		 Session newSession;

		 newSession = new Session(sessionID, message);
         newSession.setVersionNumber(requestVersionNumber);
		 newSession.setMessage(message);
		 //TODO : double check time
		 newSession.setExpireTime(expireDateTime);
		 SessionServelet.addSessionToTable(newSession);

		 //String result = String.join(Utils.SPLITTER, Arrays.asList(callID));
		 String result = String.join(Utils.SPLITTER, Arrays.asList(requestCallID, SessionServelet.getServID()+"",
				 newSession.getSessionID(), ""+newSession.getVersionNumber() ));
		 System.out.println("Returned message from sessionWrite() is: " + result);
		 return result.getBytes();
	 }
}
