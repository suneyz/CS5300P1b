package rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import rpc.Utils.*;
import servelet.SessionServelet;
import session.Session;

public class RpcServer {
	
	public static final boolean TEST = true;
	private HashMap<String, Session> mp = new HashMap<String, Session>();
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
		System.out.println("BUGGGGGGG "+infoArray[3]);
		String test = infoArray[3];
		Long requestVersionNumber = Long.parseLong(infoArray[3].trim());
		System.out.println("Server received versionNumber "+requestVersionNumber);
		String readMessage="";//version
		String readResult;
		String responseInfo;
		
		// there will always be matched session,cookie hasn't time out, session won't time out
		
		Session session = SessionServelet.getSessionByIDVersion(sessionID, String.valueOf(requestVersionNumber));
//		if(TEST) {
//			session.setVersionNumber(requestVersionNumber);
//			mp.put("01-01-01", session);
//		}
		
		if(session == null) {
			return Utils.NOT_FOUND.getBytes();
		}
		//readMessage = session.getMessageByVersionNumber(requestVersionNumber);
		readMessage = session.getMessage();
		
		byte[] outBuf = new byte[Utils.MAX_PACKET_LENGTH];
		
		
		
//		if(session == null) {
//			
//			//TODO:we need a new method to generate a new sessionID with an old one
//			String newSessionID = "";
//			
//			session = createNewSession(newSessionID);
//			readResult = "1";
//			responseInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, readResult, sessionID, ""+versionNumber, readMessage));
//		}
		
		// has matched session
//		else{
//			//readResult = "1";
//			//get the message with the specified version
//			
//			//TODO : implement a new bi-version structure inside a session
//			//different version may relate to diff val
//			Long curVersionNumber=session.getVersionNumber();
//			//TODO: when reading, should we set the curVersion to be the request version+1? or oldversion +1?
//			curVersionNumber++;
//			session.setVersionNumber(requestVersionNumber+1);
//			readMessage=session.getMessage();
//			
//			//if we get the message of right versionNumber
//			readResult = "1";
//			
//			//else , we could set the readMessage to the description of what goes wrong
//			readResult = "0";
//			readMessage = "VERSION_NOT_FOUND";
//			
//			
//			responseInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, readResult, sessionID, ""+versionNumber, readMessage));
//		}
		
		outBuf = String.join(Utils.SPLITTER, Arrays.asList(callID, sessionID, String.valueOf(requestVersionNumber), readMessage) ).getBytes();
//		if(TEST) {
			System.out.println("outBuf is : " + outBuf);
//		}
//		System.out.println("Server sent requestVersionNumber "+requestVersionNumber);
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
//		 if(TEST){
//        	 newSession = SessionServelet.getSessionByIDVersion(sessionID, String.valueOf(requestVersionNumber));
    		 
    		 newSession = new Session(sessionID, message);
             newSession.setVersionNumber(requestVersionNumber);
			 newSession.setMessage(message);
			 //TODO : double check time
			 newSession.setExpireTime(expireDateTime);
			 
			 SessionServelet.addSessionToTable(newSession);
//         }
//         else{
//        	 newSession = mp.get(sessionID);
//        	 if(newSession!=null) newSession.setMessage(message);
//        	 else {
//        		 newSession = new Session("2-2-2");
//        		 newSession.setMessage("failed to update the first one, so created a new one");
//        	 }
//         }
//		

		 //String result = String.join(Utils.SPLITTER, Arrays.asList(callID));
		 String result = String.join(Utils.SPLITTER, Arrays.asList(requestCallID, SessionServelet.getServID()+"",
				 newSession.getSessionID(), ""+newSession.getVersionNumber() ));
		 System.out.println("Returned message from sessionWrite() is: " + result);
		 return result.getBytes();
	 }
}
