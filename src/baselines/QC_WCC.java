package baselines;
import java.io.*;
import java.util.*;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;
import datastr.*;
/* This class implements the baseline work for extracting task clusters based on Lucchese's WSDM 2011 paper
 * The algorithm first constructs a graph of all the queries with edge weights being the similarities between the queries
 * then it prunes the edges based on a threshold edge weight following which it finds connected components of the graph 
 * which serve as the search task clusters
 * Reference: Identifying Task-based Sessions in Search Engine Query Logs (WSDM 2011)
 * Dependencies:
 * 				1) Uses the Algs4 package from http://algs4.cs.princeton.edu/code/
 * 				2) Simmetrics library https://github.com/Simmetrics/simmetrics
 */
public class QC_WCC {

	public static ArrayList<Task> taskList = new ArrayList<Task>();
	public static ArrayList<Query> queryList;
	public static double qNetwork[][];
	public static int networkSize;

	public static void main(String[] args) throws IOException, ClassNotFoundException{
		//jaccardDistance("test1", "test2");
		//System.exit(0);
		// Step 1: Load AOL_ST query list on which to find tasks
		loadQueryList();

		// Step 2: Form the Graph by calculating the query-query similarity
		// Step 2a: Find similarity between queries
		// Step 2b: Prune similarity and obtain 0/1 for each q-q pair
		// Step 2c: Populate the text file to be input to the CC.java class (format: http://algs4.cs.princeton.edu/41undirected/tinyG.txt)

		// Step 2a:
		findSimilarityBetweenQueries();

		// Step 3; Find connected components of the graph
		// Step 4: Populate the taskList from the connected components found

	}

	public static void findSimilarityBetweenQueries()
	{
		networkSize = queryList.size();
		qNetwork = new double[networkSize][networkSize];
		// now populate the q-q similarity network
		for(int i=0;i<networkSize;i++)
		{
			qNetwork[i][i] = -1;
			for(int j=i+1;j<networkSize;j++)// starts from i+1 coz the matrix is symmetric
			{
				qNetwork[i][j] = findSimilarityBetweenQueryPair(queryList.get(i), queryList.get(j));
				qNetwork[j][i] = qNetwork[i][j];
			}
		}
	}

	public static double findSimilarityBetweenQueryPair(Query q1, Query q2)
	{
		// we need to find the similarity between two queries as done in Lucchese's WSDM 2011 paper
		// just implementing the Content based similarity, skipping the wiki-related distance (TODO)
		double sim = 0;
		double simL = editDistance(q1.query, q2.query);
		double simJ = jaccardDistance(q1.query, q2.query);
		System.out.println(simL+"-----"+simJ);
		return sim;
	}

	public static void loadQueryList() throws IOException, ClassNotFoundException
	{
		//FileInputStream fis = new FileInputStream("data/sessionTrack2014_queryList");
		FileInputStream fis = new FileInputStream("data/common_AOLST_QueryList");
		ObjectInputStream ois = new ObjectInputStream(fis);
		queryList = (ArrayList<Query>) ois.readObject();
		ois.close();
		System.out.println("Loaded queryList with: "+queryList.size()+" queries");
	}
	
	public static double jaccardDistance(String a, String b)
	{
		double sim = 0;
		/*AbstractStringMetric metric = new JaccardSimilarity();
		sim = (double) metric.getSimilarity(a,b);*/
		HashMap<String, String> trigrams1 = getTriGramsMapForString(a);
		HashMap<String, String> trigrams2 = getTriGramsMapForString(b);
		double common = commonElementsBetweenHashMap(trigrams1, trigrams2);
		double union = unionSizeBetweenHashMaps(trigrams1, trigrams2);
		sim = (double)1 - (double)(common/union);
		//System.out.println(sim+"__"+common+"--"+union);
		return sim;
	}
	
	public static int unionSizeBetweenHashMaps(HashMap<String, String> h1, HashMap<String, String> h2)
	{
		int unionSize = 0;
		Iterator<String> itr = h1.keySet().iterator();
		while(itr.hasNext())
		{
			String s = itr.next();
			if(!h2.containsKey(s)) h2.put(s, s);
		}
		unionSize = h2.size();
		return unionSize;
	}
	
	public static int commonElementsBetweenHashMap(HashMap<String, String> h1, HashMap<String, String> h2)
	{
		int common = 0;
		Iterator<String> itr = h1.keySet().iterator();
		while(itr.hasNext())
		{
			String s = itr.next();
			if(h2.containsKey(s)) common++;
		}
		return common;
	}
	
	public static HashMap<String, String>getTriGramsMapForString(String s)
	{
		HashMap<String, String> trigrams = new HashMap<String, String>();
		int length = s.length();
		int c=0;
		while(c < (length-2))
		{
			String temp = ""+s.charAt(c)+s.charAt(c+1)+s.charAt(c+2);
			//System.out.println(temp);
			c++;
			if(!trigrams.containsKey(temp)) trigrams.put(temp, temp);
		}
		//System.out.println(trigrams.size());
		return trigrams;
	}

	// code for Levenshtein distance -- from Wiki
	public static int editDistance(String a, String b)
	{
		a = a.toLowerCase();
		b = b.toLowerCase();
		// i == 0
		int [] costs = new int [b.length() + 1];
		for (int j = 0; j < costs.length; j++)
			costs[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			// j == 0; nw = lev(i - 1, j)
			costs[0] = i;
			int nw = i - 1;
			for (int j = 1; j <= b.length(); j++) {
				int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
				nw = costs[j];
				costs[j] = cj;
			}
		}
		return costs[b.length()];
	}
}
