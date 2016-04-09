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
	
	public static final boolean TEST = true;
	/*RPC Server Side method
	 * 
	 */
	public void rpcCallRequestProcessor() throws IOException{
		//TODO:ÕâÀïÊÇ·ñÐèÒª·ÂÕÕRPCClientRead ½¨Á¢¶à¸öSocket
		DatagramSocket rpcSocket = new DatagramSocket(Utils.PROJECT1_PORT_NUMBER+1);
		//TODO: figure out what is the difference to be inside(original place) and outside
		System.out.println("server call processor running!");
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
				
				DatagramPacket sentPkt = new DatagramPacket(outBuf, outBuf.length, returnAddr, Utils.PROJECT1_PORT_NUMBER);
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
		
		// there will always be matched session,cookie hasn't time out, session won't time out
		
		Session session = TEST ? new Session("01_01_01") : SessionServelet.getSessionByID(sessionID);
		readMessage = session.getMessageByVersionNumber(requestVersionNumber);
		
		//TODO: find out reading by versionNumber could go wrong or not!
		
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
//			//TODO : implement a new bi-version structure inside a session, 
//			//different version may relate to diff val
//			Long curVersionNumber=session.getVersionNumber();
//			//TODO: when reading, should we set the curVersion to be the request version+1? or oldversion +1?
//			curVersionNumber++;
//			session.setVersionNumber(requestVersionNumber+1);
//			readMessage=session.getMessage();
//			
//			//if we get the message of right versionNumber, 
//			readResult = "1";
//			
//			//else , we could set the readMessage to the description of what goes wrong
//			readResult = "0";
//			readMessage = "VERSION_NOT_FOUND";
//			
//			
//			responseInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, readResult, sessionID, ""+versionNumber, readMessage));
//		}
		
		outBuf = String.join(Utils.SPLITTER, Arrays.asList(callID, sessionID, ""+requestVersionNumber, readMessage) ).getBytes();
		
		return outBuf;
	}
	
	/*sessionWrite
	 * write session message and return true or false
	 * */
	 public byte[] sessionWrite(String info){
		 String[] infoArray = info.split(Utils.SPLITTER);
		 String requestCallID = infoArray[0];
		 String sessionID = infoArray[2];
		 Long requestVersionNumber = Long.parseLong(infoArray[3]);
		 String message = infoArray[4];	
		 String expireTime = infoArray[5];
         
		 Session newSession = TEST? new Session("02_02_02") : SessionServelet.getSessionByID(sessionID);
		 
		 if(newSession == null){
			 newSession = new Session(sessionID, message);
			 if(!TEST) SessionServelet.addSessionToTable(newSession);
		 }
		 else{
			 newSession.refresh();
			 newSession.setMessage(message);
		 }
		

		 //String result = String.join(Utils.SPLITTER, Arrays.asList(callID));
		 String result = String.join(Utils.SPLITTER, Arrays.asList(requestCallID, SessionServelet.getServID()+"",
				 newSession.getSessionID(), ""+newSession.getVersionNumber() ));
		 return result.getBytes();
	 }
}
