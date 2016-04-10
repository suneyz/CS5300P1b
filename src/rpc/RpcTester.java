package rpc;



import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.Calendar;


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
				} catch (ParseException e) {
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
					Long readVersionNumber = (long) 0;
					//System.out.println("Client sent versionNumber "+readVersionNumber);
					Response res = RpcClient.sessionReadClient("01-01-01", 1L, destAddresses);
					
					
					System.out.println("!!!Reading result is: "+res.resStatus+", "+res.resMessage);
					
					if(res.resStatus.equals("SUCCESS")){
						System.out.println("$$$$$$$time for write request test$$$$$$");
						res = RpcClient.sessionWriteClient("01-01-01", 1L, "New message for 2nd read",
								Calendar.getInstance().getTime(), destAddresses);
					    System.out.println("session write status: "+res.resStatus+" ,location data: "+res.locationData);
					}
					
				}catch(IOException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		
		serverThread.start();
		clientThread.start();
		//clientThread2.start();
	}

}
