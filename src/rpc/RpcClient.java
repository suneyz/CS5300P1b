package rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import rpc.Utils.*;
import session.Session;

public class RpcClient {

	private static final boolean TEST1 = true;
	/*method:generate a new unique callIDs
	 * 
	 */
	public static String genCallID(){
		String resultID = UUID.randomUUID().toString();
		resultID.replace(Utils.SPLITTER, "\\");
		return resultID;
	}
	
	/*method:SessionReadClient
	 * 
	 */
	public static Response sessionReadClient(String sessionID, Long versionNumber, InetAddress[] destAdds) throws IOException{
		DatagramSocket rpcSocket = new DatagramSocket();
		String callID = sessionID; //genCallID();  //TODO: if it is ok to use sessionID
		String resultMessage="";
		String resultStatus="";
		
		String sentInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, Utils.OPERATION_SESSION_READ, sessionID, String.valueOf(versionNumber) ));
		byte[] outBuf = sentInfo.getBytes();
		System.out.println("---Client sent info "+sentInfo);
		
		
		//TODO: figure out whether this is Parallel or Sequential
		for(InetAddress destArr : destAdds){
			System.out.println("--Client Sending Request");
			DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destArr, Utils.PROJECT1_PORT_NUMBER);
			rpcSocket.send(sendPkt);
		}
		byte[] inBuf = new byte[Utils.MAX_PACKET_LENGTH];
		DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
		
		System.out.println("--Client start waiting for res");
		try{
			boolean continueListening = true;
			//record the number of received response, jump out before timeout when all responses has been received
			int numberOfReceivedPkt=0;
			
			//TODO: double check the logic here
			InetAddress lastReceivedInetAddr = null;
			do{
				recvPkt.setLength(inBuf.length);
				
				//receive method will be blocked while no packet arriving, but the following won't be,
				//so condition-check is necessary
				System.out.println("--Client_Waiting_For_Server");
				rpcSocket.receive(recvPkt);
				System.out.println("--Client_Received_Pakcet_FROM_Server");
				//Since we are expecting different packet from N instances when writing, the address of each of 
				// the arriving packet should be different
				
//				if( recvPkt.getAddress()!=null &&( lastReceivedInetAddr== null || 
//						!lastReceivedInetAddr.equals(recvPkt.getAddress()) )   ){
				if( recvPkt.getAddress()!=null) {
					System.out.println("--Client_Processing_Pakcet_FROM_Server");
					lastReceivedInetAddr = recvPkt.getAddress();
					
					byte[] receivedByteArray = recvPkt.getData();
					String receivedInfo = new String(receivedByteArray);
					
					if(receivedInfo.equals(Utils.NOT_FOUND)) {
						// TODO: Handle not found case here.
						System.out.println("NOT FOUND!!!!!!");
					}
					System.out.println("Received info is: " + receivedInfo);
					String[] receivedInfoArray = receivedInfo.split(Utils.SPLITTER); 
					String receivedCallID = receivedInfoArray[0];
					//String receivedReadResult = receivedInfoArray[1];
					//String receivedSessionID = receivedInfoArray[2];
					String receivedSessionID = receivedInfoArray[1];
					
					//A successful read: same callID, a SessionFound status flag and versionNumber+1;
					
					//if( receivedCallID.equals(callID) && (true||receivedSessionID.equals(sessionID)) ){
					if( receivedCallID.equals(callID) ){	
						numberOfReceivedPkt++;
						
						/*if(receivedReadResult.equals("1")){
							
							Long receivedVersionNumber = Long.parseLong(receivedInfoArray[3]);
							if( receivedVersionNumber == 0 || versionNumber+1==receivedVersionNumber ){
								resultMessage = "SUCCESS";
								resultData = receivedInfoArray[4];
								
								// if a success response has been received, stop the looping which means stop listening
								continueListening = false;
							}
							else{
								resultMessage = "WRONG_FOUND_VERSION";
								
							}
						}*/
						System.out.println("--Client : received packet got the right callID");
						
						if(TEST1) {
							for(int i = 0; i < receivedInfoArray.length; i++) {
								System.out.println("Received information array " + i + " content: " + receivedInfoArray[i]);
							}
						}
						
						Long receivedVersionNumber = Long.parseLong(receivedInfoArray[2]);
						System.out.println("--Client: receivedVersionNumber: "+receivedVersionNumber+", versionNumber :"+versionNumber);
						if( versionNumber==receivedVersionNumber || TEST1&&receivedVersionNumber==0){
							System.out.println("--Client:Got the right version number");
							resultStatus = "SUCCESS";
							resultMessage = receivedInfoArray[3];
							System.out.println("resultMessage in client read is: " + resultMessage);
							// if a success response has been received, stop the looping which means stop listening
							continueListening = false;
						}
						else{
							resultStatus = "WRONG_FOUND_VERSION";
							
						}
						
						//no action for wrong response
					}
					
				}
				//this is when all WQ repsonses are received but no expcted data, kill the loop and return not found
				if(continueListening && numberOfReceivedPkt == Utils.WQ) {
					if(resultMessage.equals("")) resultStatus = "NOT_FOUND";
					continueListening = false;
				}
				
			}while(continueListening);
			
		} catch(SocketTimeoutException stoe){
			//TODO: how to set the timeout time???How long???
			// timeout
			recvPkt = null;
			resultStatus = "TIME_OUT";
		}
		
		//generate response and return
		
		Response res = new Response();
		res.resStatus = resultStatus;
		res.resMessage = resultMessage;
		rpcSocket.close();
		return res;
	}
	
	
	public static Response sessionWriteClient(String sessionID, Long versionNumber, Date date, InetAddress[] destAddrs) throws IOException{
		return sessionWriteClient(sessionID, versionNumber, Session.DEFAULT_MESSAGE, date, destAddrs);
	}
	
	
	/*method:SessionWriteClient
	 * 
	 * */
	public static Response sessionWriteClient(String sessionID, Long versionNumber, String message, Date date, InetAddress[] destAddrs) throws IOException{
		if(TEST1)System.out.println("SessionWrite called");
		DatagramSocket rpcSocket = new DatagramSocket();
		String callID = sessionID; //genCallID(); //TODO: if it is ok to use sessionID 
		
		SimpleDateFormat sdf = new SimpleDateFormat(Utils.DATE_TIME_FORMAT);
		String expireTimeStr = sdf.format(date);
		
		String sentInfo = new String();
		sentInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, Utils.OPERATION_SESSION_WRITE, 
				sessionID, String.valueOf(versionNumber), message, expireTimeStr ));
		System.out.println("client sent info content: "+sentInfo);
		// TODO: what if length over 512bytes? currently regard the length of message within 512bytes
		byte[] outBuf = sentInfo.getBytes();
//		System.out.println("SessionID size is: " + sessionID.length());
//		System.out.println("VersionNumber size is: " + versionNumber);
//		System.out.println("Message size is: " + message.length());
//		System.out.println("String size is: " + sentInfo.length());
//		System.out.println("confirm the byte data: " + new String(outBuf));
//		System.out.println("byte size is: " + outBuf.length);
		// currently assume the InetAddress[] to be M ip randomly chosen from N instances
		for(InetAddress destAddr : destAddrs){
			System.out.println("Client sending packet to server with IP: " + destAddr.getHostAddress());
			DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, Utils.PROJECT1_PORT_NUMBER);
			rpcSocket.send(sendPkt);
		}
		
		
		// listening to response from bricks
		
		byte[] inBuf = new byte[Utils.MAX_PACKET_LENGTH];
		DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
		String resultFlag="";
		String resultData="";
//		Session resultSession = new Session("-1");
		ArrayList <String> locationDataList = new ArrayList<String>();
		boolean continueListening = true;
	    //TODO: rpc.setSoTimeout();
		try{
			
			InetAddress lastReceivedAddress = null;
			int numberOfReceivedPkt = 0;
			int responseNumberForSuccessfulWriting = 0;
			
			do{
				recvPkt.setLength(inBuf.length);
				System.out.println("Client Waiting for coming packet");
				rpcSocket.receive(recvPkt);	
				System.out.println("Client-packet coming!!!");
				System.out.println("Client-packet data is: " + new String(recvPkt.getData()));
				//execute when packets are actually coming
				
//				if(recvPkt.getAddress()!=null && (lastReceivedAddress == null || 
//						!lastReceivedAddress.equals(recvPkt.getAddress()) )){
					
				if(recvPkt.getAddress()!=null) {
					System.out.println("The data received is not null!");
					lastReceivedAddress = recvPkt.getAddress();
					
					byte[] receivedByteArray = recvPkt.getData();
					String receivedInfo = new String(receivedByteArray);
					String[] receivedInfoArray = receivedInfo.split(Utils.SPLITTER); 
					String receivedCallID = receivedInfoArray[0];
					String receivedServID = receivedInfoArray[1];
					String receivedSessionID = receivedInfoArray[2];
					
					//A successful write: same callID, unchanged SessionID, versionNumber+1 or versionNumber = 0;
					//Difference is : we want ¡¾WQ¡¿ successful writes!
					
					// a countable packet
					if( receivedCallID.equals(callID) ){
						System.out.println("received the same call ID!");
						numberOfReceivedPkt++;
						// condition : when no more packet is coming, end the loop
						if( numberOfReceivedPkt==Utils.N ) {
							continueListening = false;
							resultFlag = "WRITING_FAILED";
							break; //TODO: TO SU LAO BAN: Why not use break???
						}
						
						Long receivedVersionNumber = Long.parseLong(receivedInfoArray[3].trim() );

//						if( receivedVersionNumber == 0 || versionNumber+1==receivedVersionNumber ){
							System.out.println("");
							// a countable successful packet
							responseNumberForSuccessfulWriting++;
							locationDataList.add(receivedServID);
							//condition to successfully quit looping
							if(responseNumberForSuccessfulWriting == (TEST1 ? 1 : Utils.WQ) ){
								System.out.println("Write success!");
								continueListening = false;
								resultFlag = "SUCCESS";
								break;
//								resultSession = getSessionFromTransferredString(receivedInfo);
							}
							
//						}
					}	
				}
				
			}while(continueListening);
			
		} catch(SocketTimeoutException stoe){
			// timeout ??? Do we need to set the timeout here?
			recvPkt = null;
			resultFlag = "TIME_OUT";
					
		} 
		//TODO: store WQ metadata
		Response res = new Response();
		res.resStatus = resultFlag;
		res.resMessage = resultData;
		res.locationData = String.join(Utils.SPLITTER, locationDataList);
//		res.session = resultSession;
		rpcSocket.close();
		return res;
	}
	
	public static Session getSessionFromTransferredString(String info) {
		String[] infoArray = info.split(Utils.SPLITTER);
		Session session = new Session(infoArray[2], infoArray[4]);
		SimpleDateFormat formatter = new SimpleDateFormat(Utils.DATE_TIME_FORMAT);
		try {
			session.setExpireTime(formatter.parse(infoArray[5].trim()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return session;
	}
}
