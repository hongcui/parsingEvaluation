package evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;

import java.sql.*;


public class LearningCurveClauseMarkup extends LearningCurve {

	private String resultdatabasename = null;
	private String markupperformance = null;
	private String posperformance = null;
	private String modifierperformance = null;
	private String wordnetperformance = null;
	private long runtime = 0;
	private int totaldescriptions = 0;
	private int totalclauses = 0;
	private int roundnumber = 0;
	
	public LearningCurveClauseMarkup(String learningcurvetestsetfolder,
			String[] parameters, String performancedatabase, String performancetable,
			String benchmarkdatabase, String label) {
		super(learningcurvetestsetfolder, parameters, performancedatabase,
				performancetable, benchmarkdatabase, label);
		File atestfolder = new File(learningcurvetestsetfolder);
		roundnumber = Integer.parseInt(label.substring(label.lastIndexOf("-")+1, label.length()));
		
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("create database if not exists "+this.performancedatabase );
			stmt.execute("create table if not exists "+this.performancedatabase+"."+this.performancetable+
					" (time timestamp not null primary key,label varchar(100), testset int, totaldescriptions int, totalclauses int, maccuracy float, taccuracy float, mpaccuracy float, tpaccuracy float, overallaccuracy float, " +
					"wordnettotalaccess int, wordnet0pos int, wordnet2nmorepos int, " +
					"totalwords int, learnedwords int, totalorgans int, learnedorgans int, organprecision float,organrecall float, totalcharacters int, learnedb int, percentoinb float,percentcinb float, characterrecall float" +
					"totalmodifiers int, modifierprecision float, modifierrecall float, " +
					"runtime bigint)");
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	protected void executeCommand(File atestfolder) {
		String workdir = atestfolder.getAbsolutePath()+"\\";
		totaldescriptions = atestfolder.list().length;
		String command = this.parameters[0];
		String mode = this.parameters[1];
		resultdatabasename = this.performancedatabase+"_"+roundnumber+"_"+atestfolder.getName().replaceAll("-", "_");
		String com = "perl ..//unsupervised//" + command +" " + workdir +" " + resultdatabasename + " "+mode;
		System.out.println("Run command: " + com);
		try {
			 runCommand(com);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void runCommand(String com) throws Exception {
		long time = System.currentTimeMillis();

		Process p = Runtime.getRuntime().exec(com);
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p
		.getInputStream()));

		// read the output from the command
		String s = "";
		long endtime = 0;
		while ((s = stdInput.readLine()) != null) {

			System.out.println(s + " at " + (System.currentTimeMillis() - time)
		/ 1000 + " seconds");
			if(s.indexOf("wordnet counts") >=0){
				this.wordnetperformance = s;
			}
			if(s.compareTo("Done:") == 0){
				endtime = System.currentTimeMillis();
			}
			
		}
		runtime = (endtime - time)/1000;
	}
	
	@Override
	protected void scorePerformance() {
		//check sentence/pos tables against benchmarkdatabase's sentence/pos
		if(executeCommandSuccessful()){
			scoreClauseMarkupPerformance();
			scoreRolePerformance();
			scoreModifierPerformance();
			scoreWordnetPerformance();
		}
	}
		
	
	//(wordnettotalaccess int, wordnet0pos int, wordnet2pos int,)
	private void scoreWordnetPerformance() {
		//wordnet counts: total not-in-wn multiple-pos
		this.wordnetperformance = wordnetperformance.replaceFirst("^\\s*wordnet counts:\\s*", "");
		this.wordnetperformance = wordnetperformance.replaceAll("\\s+", ",");
	}

	//(totalmodifiers int, modifierprecision float, modifierrecall float);
	//based on actual modifiers seen in the markup, ignore N modifiers
	private void scoreModifierPerformance() {
		ArrayList <String> nouns = new ArrayList<String>();
		ArrayList <String> tmodifiers  = new ArrayList<String>();
		ArrayList <String> fmodifiers  = new ArrayList<String>();
		ArrayList <String> words = new ArrayList<String>();
		
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select word from "+this.resultdatabasename+".unknownwords");
			while(rs.next()){
				words.add(rs.getString("word"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select word from "+this.benchmarkdatabase+".wordposall where pos in ('s','p')");
			while(rs.next()){
				nouns.add(rs.getString("word"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		int ttotal = 0;
		int rtotal = 0;
		int tp = 0;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct modifier from "+this.benchmarkdatabase+".sentence");
			while(rs.next()){
				String[] mwords = rs.getString("modifier").split("\\s+");
				for(int i = 0; i < mwords.length; i++){
					if(mwords[i].indexOf("[") >= 0 || mwords[i].indexOf("]") >= 0){
						continue;
					}
					if(words.contains(mwords[i]) && !tmodifiers.contains(mwords[i]) && !mwords[i].matches("\\b(and|or|nor)\\b") && !nouns.contains(mwords[i]) ){
						if(!tmodifiers.contains(mwords[i])){
							System.out.println("true modifier: "+mwords[i]);
							tmodifiers.add(mwords[i]);
						}
					}
				}
			}
			ttotal = tmodifiers.size();
			rs = stmt.executeQuery("select distinct modifier from "+this.resultdatabasename+".sentence");
			ArrayList<String> checked = new ArrayList<String>();
			while(rs.next()){
				String[] mwords = rs.getString("modifier").split("\\s+");
				for(int i = 0; i < mwords.length; i++){
					if(mwords[i].indexOf("[") >= 0 || mwords[i].indexOf("]") >= 0){
						continue;
						
					}
					if(!mwords[i].matches("\\b(and|or|nor)\\b") && !nouns.contains(mwords[i]) && !checked.contains(mwords[i])){
						rtotal++;
						checked.add(mwords[i]);
						if(tmodifiers.contains(mwords[i])){
							tp++;
						}else{
							if(!fmodifiers.contains(mwords[i])){
								System.out.println("wrong modifier: "+mwords[i]);
								fmodifiers.add(mwords[i]);
							}
						}
					}
				}
			}
			this.modifierperformance = ""+ttotal+","+tp/(float)rtotal+","+tp/(float)ttotal;
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	//"totalwords int, learnedwords int, totalorgans int, learnedorgans, organprecision float,organrecall float, learnedb int, percentoinb float,percentcinb float,"
	//organ-plural, organ-singular, character from wordroles table, used to check learned wordpos, wordposall
	private void scoreRolePerformance() {
		ArrayList<String> trueo = new ArrayList<String>();
		String ignored = "'zero','one','ones','first','two','second','three','third','thirds','four','fourth','fourths','quarter','five','fifth','fifths','six','sixth','sixths','seven','seventh','sevenths','eight','eighths','eighth','nine','ninths','ninth','tenths','tenth','all','each','every','some','few','individual','both','other','lengths','length','lengthed','width','widths','widthed','heights','height','character','characters','distribution','distributions','outline','outlines','profile','profiles','feature','features','form','forms','mechanism','mechanisms','nature','natures','shape','shapes','shaped','size','sizes','sized','group','groups','clusters','cluster','arrays','array','series','fascicles','fascicle','pairs','pair','rows','number','numbers','a','about','above','across','after','almost','along','also','although','amp','an','and','are','as','at','be','because','become','becomes','becoming','been','before','being','beneath','between','beyond','but','by','ca','can','could','did','do','does','doing','done','even','few','for','frequently','from','had','has','have','hence','here','how','if','in','into','inside','inward','is','it','its','less','may','might','more','most','much','near','no','not','occasionally','of','off','often','on','onto','or','out','outside','outward','over','rarely','should','so','sometimes','somewhat','soon','than','that','the','then','there','these','this','those','throughout','to','toward','towards','up','upward','very','was','well','were','what','when','where','which','why','with','within','without','would'";
		String word = null;
		int alltotal = 0;
		int ltotal = 0;
		int oalltotal = 0;
		int calltotal = 0;
		int oltotal = 0;
		int to = 0;
		int bltotal = 0;
		int oinb = 0;
		int cinb = 0;

		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select count(distinct word)from "+this.resultdatabasename+".unknownwords");
			rs.next();
			alltotal = rs.getInt(1);
			
			rs = stmt.executeQuery("select count(distinct word)from "+this.resultdatabasename+".wordpos");
			rs.next();
			ltotal = rs.getInt(1);
			
			rs = stmt.executeQuery("select distinct word from "+this.benchmarkdatabase+".wordroles where "+this.benchmarkdatabase+".wordroles.semanticrole in ('op', 'os') and "+this.benchmarkdatabase+".wordroles.word in (select word from "+this.resultdatabasename+".unknownwords)");
			while(rs.next()){
				trueo.add(rs.getString("word"));
				oalltotal++;
			}
			
			rs = stmt.executeQuery("select distinct word from "+this.benchmarkdatabase+".wordroles where "+this.benchmarkdatabase+".wordroles.semanticrole = 'c' and "+this.benchmarkdatabase+".wordroles.word in (select word from "+this.resultdatabasename+".unknownwords)");
			while(rs.next()){
				trueo.add(rs.getString("word"));
				calltotal++;
			}
			//" learnedorgans, organprecision float,organrecall float, learnedb int, percentoinb float,percentcinb float,"

			rs = stmt.executeQuery("select distinct word from "+this.resultdatabasename+".wordpos where pos in ('p', 's') and word in (select word from "+this.resultdatabasename+".unknownwords)");
			while(rs.next()){
				oltotal++;
				if(trueo.contains(rs.getString("word"))){
					to++;
				}
			}	
			
			
				
			rs = stmt.executeQuery("select count(distinct word) from "+this.resultdatabasename+".wordpos " +
					"where pos ='b' and " +
					"word not in ("+ignored+") and" +
							" word in (select word from "+this.resultdatabasename+".unknownwords)") ;
			rs.next();
			bltotal = rs.getInt(1);

			rs = stmt.executeQuery("select count(distinct word) from "+this.resultdatabasename+".wordpos " +
					"where pos = 'b' and " +
					"word not in ("+ignored+") and " +
							"word in (select word from "+this.resultdatabasename+".unknownwords) and " +
									"word in (select word from "+this.benchmarkdatabase+".wordroles where semanticrole in ('op','os')) ");
			rs.next();
			oinb = rs.getInt(1);
			

			rs = stmt.executeQuery("select count(distinct word) from "+this.resultdatabasename+".wordpos " +
					"where pos = 'b' and word not in ("+ignored+") and " +
							"word in (select word from "+this.resultdatabasename+".unknownwords) and " +
									"word in (select word from "+this.benchmarkdatabase+".wordroles where semanticrole = 'c') ");
			rs.next();
			cinb = rs.getInt(1);
				
			//"totalwords int, learnedwords int, totalorgans int, learnedorgans, organprecision float,organrecall float,                 learnedb int, percentoinb float,percentcinb float,"
			this.posperformance = ""+alltotal+","+ltotal+","+oalltotal+","+oltotal+","+to/(float)oltotal+","+to/(float)oalltotal+","+ calltotal + ","    +bltotal+","+oinb/(float)bltotal+","+cinb/(float)bltotal + ","+cinb/(float)calltotal ;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private void scoreClauseMarkupPerformance() {
		int totalclauses = 0;
		int totalmcorrect = 0;
		int totalmpcorrect = 0;
		int totaltcorrect = 0;
		int totaltpcorrect = 0;
		try{
			Statement rstmt = conn.createStatement();
			rstmt.execute("create table if not exists "+this.resultdatabasename+".verifiedsentence (source varchar(20) not null primary key, originalsentence varchar(1000), modifier varchar(100), tag varchar(150), correctmodifier varchar(100), correcttag varchar(100), mcorrect tinyint(1), mpartialcorrect tinyint(1), tcorrect tinyint(1), tpartialcorrect tinyint(1) )");
			rstmt.execute("delete from "+this.resultdatabasename+".verifiedsentence");
			ResultSet rrs = rstmt.executeQuery("select source, modifier, tag from "+this.resultdatabasename+".sentence");
			while(rrs.next()){
				totalclauses++;
				String source = rrs.getString("source");
				String rmodifier = rrs.getString("modifier") == null? "" : rrs.getString("modifier");
				String rtag = rrs.getString("tag") == null? "" : rrs.getString("tag");
				Statement astmt = conn.createStatement();
				ResultSet ars = astmt.executeQuery("select originalsent, modifier, tag from "+this.benchmarkdatabase+".sentence where source ='"+source+"'") ;
				if(ars.next()){
					String originalsentence = ars.getString("originalsent");				
					String amodifier = ars.getString("modifier") == null? "" : ars.getString("modifier");
					String atag = ars.getString("tag") == null? "" : ars.getString("tag");
					//compare answer modifier/tag with result modifier/tag
					int mcorrect = 0;
					int mpartialcorrect = 0;
					int tcorrect = 0;
					int tpartialcorrect = 0;
				
				
					if(rmodifier.equals(amodifier)){ 
						mcorrect  = 1; 
						totalmcorrect++;
					}else if(amodifier.replaceFirst("\\[.*?\\]", "").trim().equals(rmodifier) && rmodifier.length() > 0) {
						mpartialcorrect  = 1; 
						totalmpcorrect++;
					}else if(rmodifier.replaceFirst("\\[.*?\\]", "").trim().equals(amodifier)){
						mcorrect = 1;
						totalmcorrect++;
					}
				
					if(rtag.equals(atag)) {
						tcorrect = 1; 
						totaltcorrect++;
					}else if(atag.replaceFirst("\\[.*?\\]", "").trim().equals(rtag) && rtag.length() > 0) {
						tpartialcorrect  = 1; 
						totaltpcorrect++;
					}
				
					//add result statement in verifiedsentence
					if(rtag.length() > 150){
						rtag = rtag.substring(0, 149);
					}
					String values = "'"+source +"', '"+originalsentence+"', '"+ rmodifier +"', '" + rtag +"', '"+ amodifier +"', '" + atag +"', " + mcorrect +", "+ mpartialcorrect +", "+ tcorrect +", "+ tpartialcorrect;
					astmt.execute("insert into "+this.resultdatabasename+".verifiedsentence values ("+values+")");				
				}else{
					System.out.println("source "+source+" not in benchmark");
				}
			}
			rrs = rstmt.executeQuery("select count(*) from verifiedsentence where tcorrect = 1 and mcorrect = 1");
			rrs.next();
			int good = rrs.getInt(1);
			markupperformance = ""+totalmcorrect/(float)totalclauses+","+totaltcorrect/(float)totalclauses+","+(totalmcorrect+totalmpcorrect)/(float)totalclauses+","+ (totaltcorrect+totaltpcorrect)/(float)totalclauses+","+ good/(float)totalclauses;	
			//markupperformance = new float[]{totalmcorrect/(float)totalclauses, totaltcorrect/(float)totalclauses, (totalmcorrect+totalmpcorrect)/(float)totalclauses, (totaltcorrect+totaltpcorrect)/(float)totalclauses, good/(float)totalclauses, (float)runtime};	
		}catch(Exception e){
			e.printStackTrace();
		}

		
	}

	private boolean executeCommandSuccessful() {
		//TODO check to see if resultdatabasename has been created with sentence and pos table
		boolean success = false;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("show databases");
			while(rs.next()){
				if(rs.getString(1).toLowerCase().equals(this.resultdatabasename.toLowerCase())){
					Statement stmt2 = conn.createStatement();
					stmt2.execute("use "+this.resultdatabasename);
					ResultSet rs2 = stmt2.executeQuery("show tables");
					while(rs2.next()){
						if(rs2.getString(1).toLowerCase().equals("sentence")){
							Statement stmt1 = conn.createStatement();
							ResultSet rs1 = stmt1.executeQuery("select count(*) from sentence");
							rs1.next();
							totalclauses = rs1.getInt(1);
							if(totalclauses > 1){
								success = true;
								return success;
							}
						}
					}
				}
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return success;
	}

	@Override
	protected void recordPerformance(File atestfolder) {
		try{
			Timestamp time =new java.sql.Timestamp(new java.util.Date().getTime());
			int testset = Integer.parseInt(atestfolder.getName().replaceFirst("[-a-z]+", ""));
			//(time,label varchar(100), testset int, maccuracy float, taccuracy float, mpaccuracy float, tpaccuracy float, overallaccuracy float, wordnettotalaccess int, wordnet0pos int, wordnet2pos int, totalwords int, poscorrectwords int, totalmodifiers int, truepositivemodifiers int, truenegativemodifiers int, falsepositivemodifiers int, falsenegativemodifiers int, runtime bigint)");
			String values = "'"+time +"', '"+this.label+"', "+testset+", "+totaldescriptions+", "+totalclauses+", ";
			values = values+this.markupperformance+","+this.wordnetperformance+","+this.posperformance+","+this.modifierperformance+","+(float)runtime;
			/*for (int i = 0; i < this.markupperformance.length; i++){
				values += this.markupperformance[i]+", ";
			}
			values = values.replaceFirst(", $", "").trim();*/
			Statement stmt = conn.createStatement();
			stmt.execute("Insert into "+this.performancedatabase+"."+this.performancetable+" values("+values+")");
		}catch(Exception e){
			e.printStackTrace();
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Start evaluating FNA");
		String learningcurvetestsetfolder = null;
		LearningCurveClauseMarkup lc = null;
		String[] parameters = null;
		String performancedatabase = null;
		String performancetable = null;
		String benchmarkdatabase = null;
		int round = 0;

		parameters = new String[]{"unsupervisedClauseMarkupBenchmarked.pl", "adj"};
		performancedatabase ="benchmark_learningcurve_fna_v19";
		performancetable = "performance_clausemarkup";
		benchmarkdatabase = "fnav19_benchmark";
		round = 3;
		
		for(int i = 1; i <=round; i++){
			String label = "FNAtest-final"+"-"+i;
			learningcurvetestsetfolder = "C:\\FNA-v19\\benchmarks\\learningcurve-random500"+"-"+i;
			lc = new LearningCurveClauseMarkup(learningcurvetestsetfolder,
				parameters, performancedatabase, performancetable, benchmarkdatabase, label );
			lc.run();
		}
		
		System.out.println("Start evaluating Treatise");
		

		parameters = new String[]{"unsupervisedClauseMarkupBenchmarked.pl", "plain"};
		performancedatabase ="benchmark_learningcurve_treatise_H";
		performancetable = "performance_clausemarkup";
		benchmarkdatabase = "treatiseh_benchmark";
		round = 1;
		
		
		for(int i = 1; i <=round; i++){
			String label = "Treatisetest-final"+"-"+i;
			learningcurvetestsetfolder = "C:\\Treatise\\benchmarks\\learningcurve-random500"+"-"+i;
			lc = new LearningCurveClauseMarkup(learningcurvetestsetfolder,
				parameters, performancedatabase, performancetable, benchmarkdatabase, label );
			lc.run();
		}
	}

}
