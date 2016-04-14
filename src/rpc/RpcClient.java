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
	
	/*
	 * generate a new unique callIDs
	 */
//	public static String genCallID(){
//		String resultID = UUID.randomUUID().toString();
//		resultID.replace(Utils.SPLITTER, "\\");
//		return resultID;
//	}
	
	/*
	 * SessionReadClient: send read request and handle response from RPC server
	 * @param: String sessionID; Long versionNumber; InetAddress[] destAddrs
	 * @return Response
	 */
	public static Response sessionReadClient(String sessionID, Long versionNumber, InetAddress[] destAdds) throws IOException{
		DatagramSocket rpcSocket = new DatagramSocket();
		String callID = sessionID; //genCallID();  //TODO: if it is ok to use sessionID
		String resultMessage="";
		String resultStatus="";
		String servIDOfReceivedData="";
		
		String sentInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, Utils.OPERATION_SESSION_READ, sessionID, String.valueOf(versionNumber) ));
		byte[] outBuf = sentInfo.getBytes();
		
		//TODO: figure out whether this is Parallel or Sequential
		for(InetAddress destArr : destAdds){
			DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destArr, Utils.PROJECT1_PORT_NUMBER);
			rpcSocket.send(sendPkt);
		}
		byte[] inBuf = new byte[Utils.MAX_PACKET_LENGTH];
		DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);

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
				System.out.println("--Client_Reading_Waiting_For_Server");
				rpcSocket.receive(recvPkt);
				System.out.println("--Client_Reading_Received_Pakcet_FROM_Server");
				//Since we are expecting different packet from N instances when writing, the address of each of 
				//the arriving packet should be different

				if( recvPkt.getAddress()!=null) {
					System.out.println("--Client_Processing_Pakcet_FROM_Server");
					lastReceivedInetAddr = recvPkt.getAddress();
					
					byte[] receivedByteArray = recvPkt.getData();
					String receivedInfo = new String(receivedByteArray);
					
					if(receivedInfo.equals(Utils.NOT_FOUND)) {
						// TODO: Handle not found case here.
						System.out.println("NOT FOUND!!!!!!");
					}
                    System.out.println("RPC read received string :"+receivedInfo);
					String[] receivedInfoArray = receivedInfo.split(Utils.SPLITTER); 
					String receivedCallID = receivedInfoArray[0];
					String receivedSessionID = receivedInfoArray[1];
					
					//A successful read: same callID, a SessionFound status flag and versionNumber+1;
					
					if( receivedCallID.equals(callID) ){	
						numberOfReceivedPkt++;
						
						Long receivedVersionNumber = Long.parseLong(receivedInfoArray[2]);
						if( versionNumber==receivedVersionNumber || receivedVersionNumber==0){
							resultStatus = "SUCCESS";
							servIDOfReceivedData=receivedInfoArray[4];
							resultMessage = receivedInfoArray[3];
							// if a success response has been received, stop the looping which means stop listening
							continueListening = false;
						}
						else{
							resultStatus = "WRONG_FOUND_VERSION";
							
						}
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
		System.out.println("leaving RPC client read, response status is :"+resultStatus);
		Response res = new Response();
		res.resStatus = resultStatus;
		res.resMessage = resultMessage;
		res.serverID = servIDOfReceivedData;
		rpcSocket.close();
		return res;
	}
	
	
	public static Response sessionWriteClient(String sessionID, Long versionNumber, Date date, InetAddress[] destAddrs) throws IOException{
		return sessionWriteClient(sessionID, versionNumber, Session.DEFAULT_MESSAGE, date, destAddrs);
	}
	
	
	/*
	 * SessionWriteClient: Send write request and handle response from RPC server
	 * @param: String sessionID; Long versionNumber; String message; Date date; InetAddress[] destAddrs
	 * @return: Response
	 */
	public static Response sessionWriteClient(String sessionID, Long versionNumber, String message, Date date, InetAddress[] destAddrs) throws IOException{

		DatagramSocket rpcSocket = new DatagramSocket();
		String callID = sessionID; 
		
		SimpleDateFormat sdf = new SimpleDateFormat(Utils.DATE_TIME_FORMAT);
		String expireTimeStr = sdf.format(date);
		
		String sentInfo = new String();
		sentInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, Utils.OPERATION_SESSION_WRITE, 
				sessionID, String.valueOf(versionNumber), message, expireTimeStr ));
		System.out.println("client sent info content: "+sentInfo);
		// TODO: what if length over 512bytes? currently regard the length of message within 512bytes
		byte[] outBuf = sentInfo.getBytes();
		// currently assume the InetAddress[] to be M ip randomly chosen from N instances
		for(InetAddress destAddr : destAddrs){
			DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAddr, Utils.PROJECT1_PORT_NUMBER);
			rpcSocket.send(sendPkt);
		}

		// listening to response from bricks
		
		byte[] inBuf = new byte[Utils.MAX_PACKET_LENGTH];
		DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
		String resultFlag="";
		String resultData="";
		ArrayList <String> locationDataList = new ArrayList<String>();
		boolean continueListening = true;
	    //TODO: rpc.setSocketTimeout();
		try{
			
			InetAddress lastReceivedAddress = null;
			int numberOfReceivedPkt = 0;
			int responseNumberForSuccessfulWriting = 0;
			
			do{
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);	
				//execute when packets are actually coming
					
				if(recvPkt.getAddress()!=null) {
					lastReceivedAddress = recvPkt.getAddress();
					byte[] receivedByteArray = recvPkt.getData();
					String receivedInfo = new String(receivedByteArray);
					String[] receivedInfoArray = receivedInfo.split(Utils.SPLITTER); 
					String receivedCallID = receivedInfoArray[0];
					String receivedServID = receivedInfoArray[1];
					String receivedSessionID = receivedInfoArray[2];
					
					//A successful write: same callID, unchanged SessionID, versionNumber+1 or versionNumber = 0;
					//Difference is : we want successful writes!
					
					// a countable packet
					if( receivedCallID.equals(callID) ){
						numberOfReceivedPkt+=1;
						
						Long receivedVersionNumber = Long.parseLong( receivedInfoArray[3].trim() );

						if( versionNumber==receivedVersionNumber ){
							// a countable successful packet
							responseNumberForSuccessfulWriting++;
							locationDataList.add(receivedServID);
							
							if(responseNumberForSuccessfulWriting == Utils.WQ) {
								// success
								resultFlag = "SUCCESS";
								break;
							}
							if(numberOfReceivedPkt == Utils.W && responseNumberForSuccessfulWriting<Utils.WQ){
								resultFlag = "WRTING_FAILED";
								break;
							
							}
						}
					}	
				}
				
			}while(continueListening);
			
		} catch(SocketTimeoutException stoe){
			//TODO: timeout ??? Do we need to set the timeout here?
			recvPkt = null;
			resultFlag = "TIME_OUT";
					
		} 
		//TODO: store WQ metadata
		Response res = new Response();
		res.resStatus = resultFlag;
		res.resMessage = resultData;
		if(locationDataList.size() == 1){
			res.locationData = locationDataList.get(0);
		}else{
			res.locationData = String.join(Utils.SPLITTER, locationDataList);
		}
		System.out.println("leaving RPC client write, response status is :"+res.resStatus);
		rpcSocket.close();
		return res;
	}
	
	/*
	 * Construct session from information string received from RPC server
	 * @param: String information
	 * @return: Session
	 */
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
