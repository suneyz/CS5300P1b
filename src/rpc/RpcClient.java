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
	public DatagramPacket sessionReadClient(String sessionID, InetAddress[] destAdds) throws IOException{
		DatagramSocket rpcSocket = new DatagramSocket();
		String callID = genCallID();

		String sentInfo = String.join("_", Arrays.asList(callID, Utils.OPERATION_SESSION_READ,sessionID ));
		byte[] outBuf = sentInfo.getBytes();
		// the number of destAdds should be R
		for(InetAddress destArr : destAdds){
			DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destArr, Utils.PROJECT1_PORT_NUMBER);
			rpcSocket.send(sendPkt);
		}
		byte[] inBuf = new byte[Utils.MAX_PACKET_LENGTH];
		DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
		try{
			String receivedCallID ="";
			do{
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);
				byte[] receivedByteArray = recvPkt.getData();
				String receivedInfo = new String(receivedByteArray);
				String[] receivedInfoArray = receivedInfo.split(Utils.SPLITTER);
			    
				
			}while(inBuf[0] != Byte.decode(callID));
		} catch(SocketTimeoutException stoe){
			// timeout
			recvPkt = null;
		} 
		return recvPkt;
	}
	
	/*method:SessionWriteClient
	 * 
	 * */
	public DatagramPacket sessionWriteClient(String sessionID, String message, InetAddress[] destAdds) throws IOException{
		DatagramSocket rpcSocket = new DatagramSocket();
		String callID = genCallID();
		String sentInfo = String.join(Utils.SPLITTER, Arrays.asList(callID, Utils.OPERATION_SESSION_WRITE, sessionID, message ));
		
		// TODO: what if length over 512bytes? currently regard the length of message within 512bytes
		byte[] outBuf = sentInfo.getBytes();
		// currently assume the InetAddress[] to be M ip randomly chosen from N instances
		for(InetAddress destAdd : destAdds){
			DatagramPacket sendPkt = new DatagramPacket(outBuf, outBuf.length, destAdd, Utils.PROJECT1_PORT_NUMBER);
			rpcSocket.send(sendPkt);
		}
		byte[] inBuf = new byte[Utils.MAX_PACKET_LENGTH];
		DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
		
		// receive data from the bricks by setting a while loop
		try{
			String receivedCallID="";
			do{
				recvPkt.setLength(inBuf.length);
				rpcSocket.receive(recvPkt);	
				byte[] receivedByteData = recvPkt.getData();
				String receivedInfo = new String(receivedByteData);
				String[] receivedInfoSplit = receivedInfo.split(Utils.SPLITTER);
				receivedCallID = receivedInfoSplit[0]; 
			}while(!receivedCallID.equals(callID));
		} catch(SocketTimeoutException stoe){
			// timeout ??? Do we need to set the timeout here?
			recvPkt = null;
		} 
		return recvPkt;
	}
}
