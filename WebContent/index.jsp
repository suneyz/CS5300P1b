<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>CS5300 p1a</title>
</head>
<body>
	<h4>NetID: ys684&nbsp;&nbsp;Session:${session.sessionID}&nbsp;&nbsp;Version:${session.versionNumber}&nbsp;&nbsp;Date:${currTime}</h4>
	<h3>${session.message}</h3>
	<form action="<%=request.getContextPath() %>/session" method="POST">
	<input type="submit" name="req" value="Replace"> 
	<input type="text" name="message">
	<br>
	</form>
	<form action="<%=request.getContextPath() %>/session" method="GET">
	<input type="submit" name="req" value="Refresh">
	<br>
	</form>
	<form action="<%=request.getContextPath() %>/session" method="POST">
	<input type="submit" name="req" value="Logout">
	</form>
	<h4>Cookie:${cookieID} &nbsp;&nbsp; Expires:${session.expireTime}</h4>
</body>
</html>