package testsetcreation;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

//create folders containing test sets of variant size for creating learning curve plots
//based on number of sentences, not number of descriptions
//randomly select a set of descriptions that provide desired number of sentences
//run this after unsupervised.pl has created sentence and source tables.

public abstract class LearningCurveTestSets {
	protected String targetroot;
	//protected String sourcetable;
	protected int totalfiles;
	protected File source;
	protected int step;
	protected String sentencetable;
	protected float errorratio;
	protected ArrayList[] testsets = null;
	
	static protected Connection conn = null;
	static protected String username = "termsuser";
	static protected String password = "termspassword";
	
	//public LearningCurveTestSets(String sourcedatabase, String sourcetable, String sentencetable, String sourcefolder, String targetfolder, int step, float errorratio, int totalfiles) {
	public LearningCurveTestSets(String sourcedatabase, String sentencetable, String sourcefolder, String targetfolder, int step, float errorratio, int totalfiles) {
		this.totalfiles = totalfiles;
		this.targetroot = targetfolder;
		this.step = step;
		//this.sourcetable = sourcetable;
		this.source = new File(sourcefolder);
		this.sentencetable = sentencetable;
		this.errorratio = errorratio; //when exact steps can not be meet, what is the allowable range of leeway , 1.2?
		
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+sourcedatabase+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void makeTestSets(int count){
		for(int r = 1; r<=count; r++){
		 try{
			 Statement stmt = conn.createStatement();
			 ResultSet rs = stmt.executeQuery("select count(*) from "+sentencetable);
			 rs.next();
			 int totalsentences = rs.getInt(1); //total available sentences for testing
			 //rs = stmt.executeQuery("select count(*) from "+sourcetable);
			 //rs.next();
			 //int totalfiles = rs.getInt(1); //greater than total description files (some files don't have description paragraph)
			 
			 //int sets = total/step;
			 int sets = totalsentences/step;
			 //System.out.println("producing "+sets+" test sets from "+total+" files at step "+step);
			 System.out.println("round "+r+": producing "+sets+" test sets from "+totalsentences+" sentences at step "+step);

			 testsets = new ArrayList[sets];
			 for(int i = 0; i<sets; i++){
				 testsets[i] = new ArrayList();
			 }
			 System.out.println("===============>pick files for test sets");
			 populateTestSets(totalsentences, this.totalfiles, sets);
			 System.out.println("===============>write selected files to disk");
			 dumpTestSets(r);
		 }catch(Exception e){
			 e.printStackTrace();
		 }
		}
		}

	protected void populateTestSets(int totalsentences, int totalfiles, int sets){
		for(int i = 0; i < sets-1; i++){
			System.out.println("collecting sentences for set "+i);
			int targetsize = (i+1)*step;
			for(int j = 0; j < targetsize;){
				//collect targetsize sentences from description files that are randomly selected
				String file = proposeFile(totalfiles, i);
				int numofsentences = sentencesInFile(file);
				if(!testsets[i].contains(file) && numofsentences>0 && j+numofsentences < targetsize*this.errorratio){
					testsets[i].add(file);
					j += numofsentences;
					System.out.println("\t collected "+j+" sentences");
				}
			}
		}
	}
	
	protected abstract String proposeFile(int totalfiles, int setnumber);
	
	private int sentencesInFile(String file) {
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from "+sentencetable+" where source like '"+file+"%'");
			rs.next();
			return rs.getInt(1);
		}catch(Exception e){
			e.printStackTrace();
		}
		return 0;
	}
	
	protected void dumpTestSets(int round){
		try{
			File target = new File(targetroot+"-"+round);
			if(!target.exists()){
				target.mkdir();
			}
			for(int i = 0; i < testsets.length; i++){
				File output = new File(target, "test-"+i);
				output.mkdir();
				System.out.println("write test set "+i);
				Iterator it = testsets[i].iterator();
				while(it.hasNext()){
					String fname = (String)it.next();
					File from = new File(this.source, fname);
					File to = new File(output, fname);
					FileUtils.copyFile(from,to);
					System.out.println(to.getAbsolutePath());
				}
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
