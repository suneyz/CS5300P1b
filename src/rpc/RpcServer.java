package rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

import rpc.Utils.*;
import servelet.SessionServelet;
import session.Session;

public class RpcServer {
	/*RPC Server Side method
	 * 
	 */
	public void rpcCallRequestProcessor() throws IOException{
		DatagramSocket rpcSocket = new DatagramSocket(Utils.PROJECT1_PORT_NUMBER);
		
		while(true){
			byte[] inBuf = new byte[Utils.MAX_PACKET_LENGTH];
			DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
			rpcSocket.receive(recvPkt);
			InetAddress returnAddr = recvPkt.getAddress();
			int returnPort = recvPkt.getPort();
			String requestInfo = new String(recvPkt.getData());
			String[] requestInfoArray = requestInfo.split(Utils.SPLITTER);
			String operationCode = requestInfoArray[1];
			
			byte[] outBuf = null;
			
			switch(operationCode){
				case Utils.OPERATION_SESSION_READ:
					outBuf = sessionRead(requestInfo);
					break;
				case Utils.OPERATION_SESSION_WRITE:
					
					
			}
			
		}
	}
	
	/*sessionRead
	 * read session message and return as byte array
	 */
	public byte[] sessionRead(String info){
		String[] infoArray = info.split(Utils.SPLITTER);
		String callID = infoArray[0];
		String sessionID = infoArray[2];
		String message;
		
		Session session = SessionServelet.getSessionByID(sessionID);
		String readSuccess = "1";
		if(session == null) {
			readSuccess = "0";
		}
		
		// TODO: deal with situation where there is no matched session
		message = session.getMessage();
		String result = String.join(Utils.SPLITTER, Arrays.asList(callID, sessionID, message, readSuccess));
		byte[] outBuf = new byte[Utils.MAX_PACKET_LENGTH];
		outBuf = result.getBytes();
		
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
		 
		 // TODO:deal with situation where there is no matched session
		 Session session = SessionServelet.getSessionByID(sessionID);
		 String success = "1";
		 if(session == null) {
			 success = "0";
		 }
		 session.setMessage(message);
		 String result = String.join(Utils.SPLITTER, Arrays.asList(callID, sessionID, success));
		 return result.getBytes();
	 }
}
