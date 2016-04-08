package session;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

public class Session implements Serializable{
	
	/**
	 * @author Yuzhuo Sun(ys684)
	 * Session class that contains the following field:
	 * sessionID: a unique ID to identify different session.
	 * versionNumber: identifies the version of the session data.
	 * message: the message in this session
	 * createTime: store the creation time 
	 * expireTime: record the expire time
	 */
	private static final long serialVersionUID = -5477386214535673194L;
	public static final String DEFAULT_MESSAGE = "Hello User!";
	public static final int EXPIRE_TIME = 5;
	
	private String serverID;
	private String sessionID;
	private long oldVersionNumber;
	private long versionNumber;
	private String oldMessage;
	private String message;
	private Date expireTime;
	private Date createTime;
	
	/*
	 * Constructor
	 * set create time to current time
	 * set expire time to 5 minutes from current time
	 * set message to be default message
	 */
	private Session() {
		// initialize the session class without a sessionID
		Calendar cal = Calendar.getInstance();
		setCreateTime(cal.getTime());
		cal.add(Calendar.MINUTE, EXPIRE_TIME); // set the expire time stamp to be 5 minutes later
		setExpireTime(cal.getTime());
		setMessage(DEFAULT_MESSAGE);
		setVersionNumber(1);
	}
	
	/*
	 * Constructor
	 * construct with sessionID information
	 * @param SessionID
	 */
	public Session(String sessionID) {
		// initialize from a sessionID
		this();
		sessionID = sessionID;
	}
	
	public Session(String sessionID, String serverID) {
		this(sessionID);
		serverID = sessionID;
	}
	
	/*
	 * This method is used to refresh current session
	 * Set new version number and expire time.
	 */
	public void refresh() {
		
		setOldVersionNumber(versionNumber);
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, EXPIRE_TIME);
		setExpireTime(cal.getTime());
		setVersionNumber(versionNumber + 1);
	}
	
	/*
	 * SessionID getter
	 */
	public String getSessionID() {
		return sessionID;
	}
	
	/*
	 * SessionID setter
	 */
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	
	/*
	 * VersionNumber getter
	 */
	public long getVersionNumber() {
		return versionNumber;
	}
	
	/*
	 * VersionNumber setter
	 */
	public void setVersionNumber(long versionNumber) {
		this.versionNumber = versionNumber;
	}
	
	/*
	 * ExpireTime getter
	 */
	public Date getExpireTime() {
		return expireTime;
	}
	
	/*
	 * ExpireTime setter
	 */
	public void setExpireTime(Date expireTime) {
		this.expireTime = expireTime;
	}
	
	/*
	 * CreateTime getter
	 */
	public Date getCreateTime() {
		return createTime;
	}
	
	/*
	 * CreateTime setter
	 */
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	
	/*
	 * Message getter
	 */
	public String getMessage() {
		return message;
	}
	
	/*
	 * Message setter
	 */
	public void setMessage(String message) {
		setOldMessage(message);
		this.message = message;
	}

	public String getServerID() {
		return serverID;
	}

	public void setServerID(String serverID) {
		this.serverID = serverID;
	}

	public long getOldVersionNumber() {
		return oldVersionNumber;
	}

	public void setOldVersionNumber(long oldVersionNumber) {
		this.oldVersionNumber = oldVersionNumber;
	}

	public String getOldMessage() {
		return oldMessage;
	}

	public void setOldMessage(String oldMessage) {
		this.oldMessage = oldMessage;
	}
	
}
