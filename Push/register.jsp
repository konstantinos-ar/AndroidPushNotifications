<%@ page language="java" 
  pageEncoding="ISO-8859-7"
  import="gr.zeys.server.*"
  import="java.sql.*"
  import="java.net.*"
  import="java.io.*"
%><%   
{ 
	request.setCharacterEncoding("ISO-8859-7");
	response.setHeader("Content-Type", "application/octet-stream");
	response.setHeader("Cache-Control", "no-store");

	session = request.getSession(true);
	
	String regId = request.getParameter("regId");
	String user = request.getParameter("username");
	//System.out.println("Username is : " + user);

	if (user != null)
	{		
		ZDB zdb = new ZDB();
			try
			{
				//System.out.println("Enter...");
				zdb.open(System.getProperty("ztrade.db_url"),"Cannot post code",System.getProperty("Cannot post code"), -1);
				Statement st = zdb.getStatement();
				//ResultSet rs = st.executeQuery("SELECT PushId from Cannot post code WHERE personid='cannot post code'");
				//if (rs.next())
				//	System.out.println("Test: " + rs.getString(1));
				st.execute("EXEC registerPush '" + regId + "', '" + user + "'");
				zdb.close();
			}
			catch (Exception e)
			{
				System.out.println("exception: " + e);
				zdb.close();
			}
  
	}
	  
}
%>
