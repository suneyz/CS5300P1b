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
		//TODO:这里是否需要仿照RPCClientRead 建立多个Socket
		DatagramSocket rpcSocket = new DatagramSocket(Utils.PROJECT1_PORT_NUMBER);
		//TODO: figure out what is the difference to be inside(original place) and outside
		
        //InetAddress lastReceivedAddr = null;
		while(true){
			//TODO: worried about the memory since this keeps generating new 
			byte[] inBuf = new byte[Utils.MAX_PACKET_LENGTH];
			byte[] outBuf = null;
			DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
			rpcSocket.receive(recvPkt);
			
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
		Long versionNumber = Long.parseLong(infoArray[3]);
		String readMessage="";//version
		String readResult;
		String responseInfo;
		
		Session session = SessionServelet.getSessionByID(sessionID);
		byte[] outBuf = new byte[Utils.MAX_PACKET_LENGTH];
		
		// no matched session(probably due to time_out, even when we are reading/refreshing, we should
		// create a new session, like we did in project1a)
		
		if(session == null) {
			readResult = "0";
			responseInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, readResult, sessionID, ""+versionNumber, readMessage));
		}
		
		// has matched session
		else{
			readResult = "1";
			Long curVersionNumber=session.getVersionNumber();
			curVersionNumber++;
			session.setVersionNumber(curVersionNumber);
			readMessage=session.getMessage();
			
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
		 
		 //TODO: Make sure when we can't find a session 1. due to the fact that it has been deleted
		 //2. due to the fact that it is never created   
		 // ===> we create a new one, with specified message
		 
		 Session session = SessionServelet.getSessionByID(sessionID);
		 if(session == null){
			 session = new Session(sessionID); 
			 session.setMessage(message);// create initial or user customered session is decide by the message generated in servlet
			 session.setVersionNumber(0);
			 Date createdTime = new Date();
			 session.setCreateTime(createdTime);
			 Date expireTime = new Date(createdTime.getTime()+10000);
			 session.setExpireTime(expireTime);
			 
			 //TODO: how to add a session into session table
		 }
		 session.setMessage(message);
		 String result = String.join(Utils.SPLITTER, Arrays.asList(callID, sessionID, success));
		 return result.getBytes();
	 }
}
