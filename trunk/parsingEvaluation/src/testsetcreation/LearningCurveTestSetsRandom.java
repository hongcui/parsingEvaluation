package testsetcreation;

import java.sql.Statement;
import java.sql.ResultSet;

import java.util.*;
import java.io.File;

//based on number of sentences, not number of descriptions
//randomly select a set of descriptions that provide desired number of sentences
public class LearningCurveTestSetsRandom extends LearningCurveTestSets {
	

	public LearningCurveTestSetsRandom(String sourcedatabase,
			String sentencetable, String sourcefolder, String targetfolder, int step, float errorratio, int totalfiles) {
		super(sourcedatabase, sentencetable, sourcefolder, targetfolder, step, errorratio, totalfiles);
	}

	
	


	protected String proposeFile(int totalfiles, int i) {
		Random rand = new Random();
		int f = rand.nextInt(totalfiles-1);
		String file = (f+1)+".txt";
		return file;
	}
	


	/*private boolean inSource(String file){
		File f = new File(source, file);
		return f.exists();
	}*/
	

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourcedatabase = "fnav19_benchmark";
		//String sourcetable = "filename2taxon";
		String sourcefolder = "C:\\FNA-v19\\target\\descriptions-dehyphened";
		String sentencetable = "sentence";
		int step = 500;
		int totalfiles = 949;
		float ratio = 1.1f;
		String targetfolder = "C:\\FNA-v19\\benchmarks\\learningcurve-random"+step;	
		//LearningCurveTestSetsRandom lctsr = new LearningCurveTestSetsRandom(sourcedatabase, sentencetable, sourcefolder, targetfolder, step, ratio, totalfiles);
		//lctsr.makeTestSets(3);
		
		
		sourcedatabase = "treatiseH_benchmark";
		//sourcetable = "filename2taxon";
		sourcefolder = "C:\\Treatise\\TreatiseH-dehyphened-numbered";
		sentencetable = "sentence";
		totalfiles = 2038;
		step = 500;
		ratio = 1.1f;
		targetfolder = "C:\\Treatise\\benchmarks\\learningcurve-random"+step;
		
		LearningCurveTestSetsRandom lctsr = new LearningCurveTestSetsRandom(sourcedatabase,  sentencetable, sourcefolder, targetfolder, step, ratio, totalfiles);
		lctsr.makeTestSets(3);
		
		

	}

}
