package testsetcreation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.*;
import java.util.regex.*;

/**
 * this class mapping file name such as 1.xml to the name of the taxon described in the file "f:familyname g:genusname"
 * save the mapping in a database table
 * @author hongcui
 *
 */
public class FileName2TaxonNameMapping {
	private File sourcefolder;
	private File descriptionsource;
	private String targetdatabase;
	private String targettable;
	static private Connection conn = null;
	static private String username = "termsuser";
	static private String password = "termspassword";

	private Hashtable abbr = new Hashtable();
	private ArrayList ranks = new ArrayList();
	private String[] names = null;

	public FileName2TaxonNameMapping(String sourcefolderfullpath, String descriptionsource, String targetdatabase, String targettable) {
		this.sourcefolder = new File(sourcefolderfullpath);
		this.descriptionsource = new File(descriptionsource);
		this.targetdatabase = targetdatabase;
		this.targettable = targettable;
		populateAbbr();
		populateRanks();
		names = new String[ranks.size()];
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+targetdatabase+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists "+targettable+"" +
						" (filename varchar(10) not null primary key, hasdescription tinyint(1)," +
						"family varchar(50), subfamily varchar(50), tribe varchar(50), subtribe varchar(50), " +
						"genus varchar(50), subgenus varchar(50), section varchar(50), subsection varchar(50), " +
						"species varchar(50), subspecies varchar(50), variety varchar(50))");
				stmt.execute("delete from "+targettable);
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void populateRanks(){
		ranks.add("family");
		ranks.add("subfamily");
		ranks.add("tribe");
		ranks.add("subtribe");
		ranks.add("genus");
		ranks.add("subgenus");
		ranks.add("section");
		ranks.add("subsection");
		ranks.add("species");
		ranks.add("subspecies");
		ranks.add("variety");
	}
	
	private void populateAbbr(){
		abbr.put("family", "");
		abbr.put("subfamily", "subfam.");
		abbr.put("tribe", "tribe");
		abbr.put("subtribe", ""); //TODO
		abbr.put("genus", "");
		abbr.put("subgenus", "subg.");
		abbr.put("section", "sect.");
		abbr.put("subsection", "subsect."); //TODO
		abbr.put("species", "");
		abbr.put("subspecies", "subsp.");
		abbr.put("variety", "var.");
	}
	/*
	 * read file names from source folder,
	 * create the corresponding taxon name by reading the taxon info from a file
	 * save the mapping between filename and taxon name in the targettable
	 * 
	 */
	public void mapping(){
		File[] files = sourcefolder.listFiles();
		try{
			for(int i = 1; i <=files.length; i++){
				File f = new File(sourcefolder, i+".xml");
				BufferedReader reader = new BufferedReader(new FileReader(f));
				String line = null;
				while ((line = reader.readLine()) != null) {
					if(line.indexOf("_name>") > 0 && line.indexOf("<common_") < 0 && line.indexOf("<synonym_") < 0 && line.indexOf("<past_")<0){
						setTaxonName(line);
					}
				}
				String taxonname = buildTaxonNameString();
				System.out.println(i +".xml  => "+taxonname);
				addMapping(i +".xml", taxonname);
				reader.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	/**
	 * <family_name>Asteraceae</family_name>
	 *  <subfamily_name>CaryophyllaceaeÂ Jussieu subfam. Polycarpoideae</subfamily_name>
 	 * <tribe_name>Asteraceae Martinov tribe Mutisieae</tribe_name>
 	<genus_name>HECASTOCLEIS</genus_name>
 	 <subgenus_name>ARTEMISIA Linnaeus subg. DRANCUNCULUS</subgenus_name>
 	  <section_name>Polygonum   Linnaeus sect. Polygonum</section_name>
 	<species_name>Hecastocleis shockleyi</species_name>
 	<subspecies_name>Artemisia campestris Linnaeus subsp. canadensis</subspecies_name>
 	 <variety_name>Cirsium hydrophilum (Greene) Jepson var. vaseyi</variety_name>
 
 	 * Set a name at a specific rank according to "line"; reset all names at lower ranks to "";
	 * @param line
	 */
	
	private void setTaxonName(String line){
		String rank = null;
		String name = null;
		Pattern p = Pattern.compile("\\s*<(.*?)_name>(.*?)</.*");
		Matcher m = p.matcher(line);
		if(m.matches()){
			rank = m.group(1);
			name = m.group(2);
			String abbrevation = (String) abbr.get(rank);
			if(abbrevation.equals("")){
				name = rank.equals("species")? name.replaceFirst(".*? ", "") : name;
			}else{
				name = name.replaceFirst(".*?"+abbrevation+" ", "").trim();
			}
		}else{
			System.err.println("problem: "+line+" doesn't matach pattern");
		}
		name = name.toLowerCase();
		int r = ranks.indexOf(rank);
		//set name for r
		names[r] = name;
		//reset name of lower taxa
		for(int i = r+1; i < names.length; i++){
			names[i] = "";
		}
		//System.out.println(line);
	}
	/**
	 * String up non-empty names from top to bottom of taxon ranks.
	 * @return
	 */
	private String buildTaxonNameString(){
		StringBuffer namesb = new StringBuffer("'");
		for(int i = 0; i < ranks.size(); i++){
				namesb.append(names[i]);
				namesb.append("','");
		}
		return namesb.toString().replaceFirst(",'$", "").trim();
	}
	
	private void addMapping(String filename, String taxonname){
		try{
			int hasdescription = (new File(descriptionsource, filename.replaceFirst(".xml",".txt"))).exists()? 1 : 0;
			Statement stmt = conn.createStatement();
			stmt.execute("insert into "+targettable+" values('"+filename+"',"+hasdescription+", "+taxonname+")");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fullpath = "C:\\FNA-v19\\target\\transformed";
		String descriptionsource = "C:\\FNA-v19\\target\\descriptions";
		String database = "fnav19n2_corpus";
		String table = "filename2taxon";
		
		FileName2TaxonNameMapping fn2tnm = new FileName2TaxonNameMapping(fullpath, descriptionsource, database, table);
		fn2tnm.mapping();
	}

}
