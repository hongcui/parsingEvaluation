package testsetcreation;

import java.util.*;

import java.sql.*;

import java.io.File;

//collect test files (really sentences) basing on taxonomic hierarchy.
//starts from family rank down.
//use the original order of the volume.

public class LearningCurveTestSetsByTaxon extends LearningCurveTestSets {
	private int setnumber = -1;
	private int next = 0;
	//private ArrayList prioritizedfiles = new ArrayList();
	//private ArrayList filefeeder = new ArrayList();

	public LearningCurveTestSetsByTaxon(String sourcedatabase,
			String sentencetable, String sourcefolder, String targetfolder, int step, float errorratio, int totalfiles) {
		super(sourcedatabase, sentencetable, sourcefolder, targetfolder, step, errorratio, totalfiles);
	}

	/*
	 * select family, count(family) from filename2taxon where hasdescription =1 group by family order by count(family) desc;
	 * select family, tribe, count(tribe) from filename2taxon where hasdescription =1 group by tribe order by count(tribe) desc;
	 * select family, tribe, genus, count(genus) from filename2taxon where hasdescription =1 group by genus order by count(genus) desc;
	 */
	/*private void sort(){
		File[] list = source.listFiles();
		for(int i =0; i<list.length; i++){
			prioritizedfiles.add((i+1)+".txt");
		}
		Collections.reverse(prioritizedfiles); //reversed so proposeFile take out a file from the end of the list.
	}*/
	/**
	 * propose a file to be added to a test set
	 */
	
	protected String proposeFile(int totalfiles, int setnumber) {
		if(this.setnumber != setnumber){
			//start a new set
			this.next = 1;
			this.setnumber = setnumber;
		}
		String f = next+".txt";
		next++;
		return f;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourcedatabase = "fnav19_benchmark";
		//String sourcetable = "filename2taxon";
		String sourcefolder = "C:\\FNA-v19\\target\\descriptions-dehyphened";
		String sentencetable = "sentence";
		int totalfiles = 949;
		int step = 500;
		float ratio = 1.1f;
		String targetfolder = "C:\\FNA-v19\\benchmarks\\learningcurve-bytaxon"+step;		

		LearningCurveTestSetsByTaxon lctsbt = new LearningCurveTestSetsByTaxon(sourcedatabase, sentencetable, sourcefolder, targetfolder, step, ratio,totalfiles );
		lctsbt.makeTestSets(1);

	}

}
