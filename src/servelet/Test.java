package servelet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class Test {
	public static void main (String[] args) throws FileNotFoundException, UnsupportedEncodingException {
//		SessionServelet.saveRebootNum();
//		System.out.println("Retrieved reboot number is: " + SessionServelet.restoreRebootNum());
		
		
//			try (BufferedReader br = new BufferedReader(new FileReader("/server_info.txt")))
//			{
//				String serverID = br.readLine();
//				String rebootNum = br.readLine();
//				SessionServelet.servID = Long.parseLong(serverID);
//				SessionServelet.rebootNum = Long.parseLong(rebootNum);
//				return true;
//
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			return false;
		
		System.out.println("running");
			
		PrintWriter writer = new PrintWriter("the-file-name.txt", "UTF-8");
		writer.println("The first line");
		writer.println("The second line");
		writer.close();
		
		
	}
}
