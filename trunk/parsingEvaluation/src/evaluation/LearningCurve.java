package evaluation;

import java.io.File;
import java.sql.*;


public abstract class LearningCurve {
	protected File source; 
	protected String performancetable;
	protected String performancedatabase;
	protected String benchmarkdatabase;
	
	protected String label;
	protected String[] parameters;
	protected Object performance;
	
	static protected Connection conn = null;
	static protected String username = "termsuser";
	static protected String password = "termspassword";

	public LearningCurve(String learningcurvetestsetfolder, String[] parameters, String performancedatabase, String performancetable, String benchmarkdatabase, String label) {
		this.source = new File(learningcurvetestsetfolder);
		this.performancetable = performancetable;
		this.performancedatabase = performancedatabase;
		this.benchmarkdatabase = benchmarkdatabase;
		this.label = label;
		this.parameters = parameters;
		this.performance =new Object();
		
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create database if not exists "+this.performancedatabase);
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * run the program to be evaluated folder by folder
	 * record folder info and performance scores in a database table
	 */
	public void run(){
		File[] testfolders = source.listFiles();
		for(int i = 0; i<testfolders.length; i++){
			File atestfolder = testfolders[i];
			executeCommand(atestfolder);	//save performance in performance object
			scorePerformance();
			recordPerformance(atestfolder);
		}
	}
	
	protected abstract void executeCommand(File atestfolder);
	protected abstract void scorePerformance();
	protected abstract void recordPerformance(File atestfolder);
	


}
