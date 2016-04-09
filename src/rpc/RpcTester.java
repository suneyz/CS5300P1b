package rpc;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;


public class RpcTester {

	
	public static void main(String[] args) {
				
		Thread serverThread = new Thread(new Runnable(){

			@Override
			public void run() {
				
				// TODO Auto-generated method stub
				RpcServer server = new RpcServer();
				try {
					System.out.println("running server thread");
					server.rpcCallRequestProcessor();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		
		Thread clientThread = new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				//RpcClient client = new RpcClient();
				try{
					System.out.println("client thread sending request");
					InetAddress destAddress = InetAddress.getLocalHost();
					InetAddress[] destAddresses = new InetAddress[1];
					destAddresses[0]=destAddress;
					
					Response res = RpcClient.sessionReadClient("01_01_01", 0L, destAddresses);
					System.out.println("Reading result is: "+res.resStatus);
					
				}catch(IOException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		serverThread.start();
		clientThread.start();
	}

}
