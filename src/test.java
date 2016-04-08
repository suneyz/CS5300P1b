import java.net.DatagramPacket;

public class test {
    public static void main(String[] args){
    	byte[] inBuf = new byte[512];
    	DatagramPacket pkt = new DatagramPacket(inBuf, inBuf.length);
    	System.out.println(pkt.getAddress().equals(""));
    }
}
