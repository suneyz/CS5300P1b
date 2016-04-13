import java.net.DatagramPacket;

public class test {
    public static void main(String[] args){
    	String testLine = "\"10\"\"0\"";
//    	String newLine = testLine.replace("\"", " ");
    	String[] sa = testLine.split("\"+");
    	for(String s : sa) {
    		System.out.println(s);
    	}
    }
}
