package rpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import rpc.Utils.*;

public class RpcClient {

	/*method:generate a new unique callIDs
	 * 
	 */
	public String genCallID(){
		String resultID = UUID.randomUUID().toString();
		resultID.replace(Utils.SPLITTER, "\\");
		return resultID;
	}
	
	/*method:SessionReadClient
	 * 
	 */
	public Response sessionReadClient(String sessionID, Long versionNumber, InetAddress[] destAdds) throws IOException{
		DatagramSocket rpcSocket = new DatagramSocket();
		String callID = genCallID();
		String resultMessage="";
		String resultData="";
		
		String sentInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, Utils.OPERATION_SESSION_READ, sessionID, ""+versionNumber ));
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
				rpcSocket.receive(recvPkt);
				
				//Since we are expecting different packet from N instances when writing, the address of each of 
				// the arriving packet should be different
				
				if( recvPkt.getAddress()!=null &&( lastReceivedInetAddr== null || 
						!lastReceivedInetAddr.equals(recvPkt.getAddress()) )   ){
					
					lastReceivedInetAddr = recvPkt.getAddress();
					
					byte[] receivedByteArray = recvPkt.getData();
					String receivedInfo = new String(receivedByteArray);
					String[] receivedInfoArray = receivedInfo.split(Utils.SPLITTER); 
					String receivedCallID = receivedInfoArray[0];
					String receivedReadResult = receivedInfoArray[1];
					String receivedSessionID = receivedInfoArray[2];
					
					//A successful read: same callID, a SessionFound status flag and versionNumber+1;
					
					if( receivedCallID.equals(callID) && receivedSessionID.equals(sessionID) ){
						
						numberOfReceivedPkt++;
						
						if(receivedReadResult.equals("1")){
							
							if(versionNumber+1==Long.parseLong(receivedInfoArray[3])){
								resultMessage = "SUCCESS";
								resultData = receivedInfoArray[4];
								
								// if a success response has been received, stop the looping which means stop listening
								continueListening = false;
							}
							else{
								resultMessage = "WRONG_FOUND_VERSION";
								
							}
						}
						
						//no action for wrong response
					}
					
				}
				//this is when all WQ repsonses are received but no expcted data, kill the loop and return not found
				if(continueListening && numberOfReceivedPkt == Utils.WQ) {
					if(resultMessage.equals("")) resultMessage = "NOT_FOUND";
					continueListening = false;
				}
				
			}while(continueListening);
			
		} catch(SocketTimeoutException stoe){
			//TODO: how to set the timeout time???How long???
			// timeout
			recvPkt = null;
			resultMessage = "TIME_OUT";
		}
		
		//generate response and return
		
		Response res = new Response();
		res.resStatus = resultMessage;
		//res.resPkt = recvPkt;
		res.resData = resultData;
		return res;
	}
	
	/*method:SessionWriteClient
	 * 
	 * */
	public Response sessionWriteClient(String sessionID, Long versionNumber, String message, InetAddress[] destAddrs) throws IOException{
		
		DatagramSocket rpcSocket = new DatagramSocket();
		String callID = genCallID();
		String sentInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, Utils.OPERATION_SESSION_WRITE, sessionID, ""+versionNumber, message ));
		
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
		boolean continueListening = true;
				
		try{
			
			InetAddress lastReceivedAddress = null;
			int numberOfReceivedPkt = 0;
			int responseNumberForSuccessfulWriting = 0;
			
			do{
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);	
				
				//execute when packets are actually coming
				if(recvPkt.getAddress()!=null &&(lastReceivedAddress == null || !lastReceivedAddress.equals(recvPkt.getAddress()) )){
					
					lastReceivedAddress = recvPkt.getAddress();
					
					byte[] receivedByteArray = recvPkt.getData();
					String receivedInfo = new String(receivedByteArray);
					String[] receivedInfoArray = receivedInfo.split(Utils.SPLITTER); 
					String receivedCallID = receivedInfoArray[0];
					String receivedReadResult = receivedInfoArray[1];
					String receivedSessionID = receivedInfoArray[2];
					
					//A successful read: same callID, a SessionFound status flag and versionNumber+1;
					//Difference is : we want ¡¾WQ¡¿ successful writes!
					
					// a countable packet
					if( receivedCallID.equals(callID) && receivedSessionID.equals(sessionID)){
						
						numberOfReceivedPkt++;
						// condition : when no more packet is coming, end the loop
						if( numberOfReceivedPkt==Utils.N ) {
							continueListening = false;
							resultFlag = "WRITING_FAILED";
						}
						
						if(receivedReadResult.equals("1") && versionNumber+1==Long.parseLong(receivedInfoArray[3])){
							// a countable successful packet
							responseNumberForSuccessfulWriting++;
							
							//condition to successfully quit looping
							if(responseNumberForSuccessfulWriting == Utils.WQ){
								continueListening = false;
								resultFlag = "SUCCESS";
							}
							
						}
					}	
				}
				
			}while(continueListening);
			
		} catch(SocketTimeoutException stoe){
			// timeout ??? Do we need to set the timeout here?
			recvPkt = null;
			resultFlag = "TIME_OUT";
					
		} 
		
		Response res = new Response();
		res.resStatus = resultFlag;
		res.resData = resultData;
		
		return res;
	}
}
