package rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Date;

import rpc.Utils.*;
import servelet.SessionServelet;
import session.Session;

public class RpcServer {
	/*RPC Server Side method
	 * 
	 */
	public void rpcCallRequestProcessor() throws IOException{
		//TODO:ÕâÀïÊÇ·ñÐèÒª·ÂÕÕRPCClientRead ½¨Á¢¶à¸öSocket
		DatagramSocket rpcSocket = new DatagramSocket(Utils.PROJECT1_PORT_NUMBER);
		//TODO: figure out what is the difference to be inside(original place) and outside
		
        //InetAddress lastReceivedAddr = null;
		while(true){
			//TODO: worried about the memory since this keeps generating new 
			byte[] inBuf = new byte[Utils.MAX_PACKET_LENGTH];
			byte[] outBuf = null;
			DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
			rpcSocket.receive(recvPkt);
			
			//execute the following code when there arrives a packet
			
			if( recvPkt.getAddress()!=null ){
				
				InetAddress returnAddr = recvPkt.getAddress();
				int returnPort = recvPkt.getPort();
				String requestInfo = new String(recvPkt.getData());
				String[] requestInfoArray = requestInfo.split(Utils.SPLITTER);
				String operationCode = requestInfoArray[1];
						
				switch(operationCode){
					case Utils.OPERATION_SESSION_READ:
						outBuf = sessionRead(requestInfo);
						break;
					case Utils.OPERATION_SESSION_WRITE:
						outBuf = sessionWrite(requestInfo);
						break;	
				}
				
				DatagramPacket sentPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, returnPort);
				rpcSocket.send(sentPkt);	
			}
			
		}
	}
	
	/*sessionRead
	 * read session message and return as byte array
	 */
	public byte[] sessionRead(String info){
		
		//information extraction
		
		String[] infoArray = info.split(Utils.SPLITTER);
		String callID = infoArray[0];
		String sessionID = infoArray[2]; 
		Long requestVersionNumber = Long.parseLong(infoArray[3]);
		String readMessage="";//version
		String readResult;
		String responseInfo;
		
		Session session = SessionServelet.getSessionByID(sessionID);
		byte[] outBuf = new byte[Utils.MAX_PACKET_LENGTH];
		
		// no matched session
		// probably due to pass the discard_time, even when we are reading/refreshing, we should
		// create a new session, like we did in project1a
		
		if(session == null) {
			
			//TODO:we need a new method to generate a new sessionID with an old one
			String newSessionID = "";
			
			session = createNewSession(newSessionID);
			readResult = "1";
			responseInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, readResult, sessionID, ""+versionNumber, readMessage));
		}
		
		// has matched session
		else{
			//readResult = "1";
			//get the message with the specified version
			
			//TODO : implement a new bi-version structure inside a session, 
			//different version may relate to diff val
			Long curVersionNumber=session.getVersionNumber();
			//TODO: when reading, should we set the curVersion to be the request version+1? or oldversion +1?
			curVersionNumber++;
			session.setVersionNumber(requestVersionNumber+1);
			readMessage=session.getMessage();
			
			//if we get the message of right versionNumber, 
			readResult = "1";
			
			//else , we could set the readMessage to the description of what goes wrong
			readResult = "0";
			readMessage = "VERSION_NOT_FOUND";
			
			
			responseInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, readResult, sessionID, ""+versionNumber, readMessage));
		}
		
		outBuf = responseInfo.getBytes();
		
		return outBuf;
	}
	
	/*sessionWrite
	 * write session message and return true or false
	 * */
	 public byte[] sessionWrite(String info){
		 String[] infoArray = info.split(Utils.SPLITTER);
		 String callID = infoArray[0];
		 String sessionID = infoArray[2];
		 String message = infoArray[3];

		 
		 //TODO: Make sure when we can't find a session: 1. due to the fact that it has been deleted
		 //2. due to the fact that it is never created   
		 // ===> we create a new one, with default message
		 
//		 Session session = SessionServelet.getSessionByID(sessionID);
//		 if( session == null || new Date().after(session.getExpireTime()) ){
//			 //TODO: generate a new sessionID by a method to be implemented
//			 session = createNewSession(sessionID); 			 
//			 SessionServelet.addSessionToTable(session);
//		 }
		 
		 Session session = new Session(sessionID, message);
		 
		 SessionServelet.addSessionToTable(session);
		
		 String result = String.join(Utils.SPLITTER, Arrays.asList(callID, sessionID, success));
		 return result.getBytes();
	 }
	 
	 /*createNewSession
	  * create a new session with the given sessionID message
	  * */
	 public Session createNewSession(String sessionID){
		 
		 Session newSession = new Session(sessionID);
		 newSession.setVersionNumber(0);
		 newSession.setMessage(Session.DEFAULT_MESSAGE);
		 Date createdTime = new Date();
		 newSession.setCreateTime(createdTime);
         Date expireTime = new Date(createdTime.getTime()+ Session.SESSION_TIMEOUT_SECS);
         newSession.setExpireTime(expireTime);
         return newSession;
	 }
}
