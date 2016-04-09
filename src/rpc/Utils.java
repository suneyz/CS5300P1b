package rpc;

public class Utils {
	public static final String OPERATION_SESSION_READ = "read";
	public static final String OPERATION_SESSION_WRITE = "write";
	public static final String OPERATION_SESSION_CREATE_NEW ="create";
	public static final int PROJECT1_PORT_NUMBER = 5300;
	public static final int MAX_PACKET_LENGTH = 512;
	public static final String SPLITTER = "_";
	//need to figure out where to get these information, may be from simpleDB
	public static final int N = 5;
	public static final int W = 4;
	public static final int WQ = 3;
	public static final int R = 2;
	
	public static final String[] RESPONSE_FLAGS_READING = new String[]{"SUCCESS", "TIME_OUT", "WRONG_FOUND_VERSION", "NOT_FOUND"};
	public static final String[] RESPONSE_FLAGS_WRITING = new String[] {"SUCCESS", "WRITING_FAILED","TIME_OUT"};
}
