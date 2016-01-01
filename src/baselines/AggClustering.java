package baselines;
import datastr.*;
import hc.*;
import hc.visualization.*;

import java.io.*;
import java.util.*;
// this class will take as input an arraylist of queries and run agglomerative clustering algo
// HC source: https://github.com/lbehnke/hierarchical-clustering-java
public class AggClustering {

	public static ArrayList<Query> queryList;
	public static int sizeNetwork;
	public static double distances[][];
	
	public static void main(String[] args) throws IOException, ClassNotFoundException{
		loadQueryList();
		populatePairwiseDistances();
		buildHC();
	}
	
	public static void buildHC()
	{
		ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
		String[] names = new String[sizeNetwork];
		for(int i=0;i<sizeNetwork;i++)
			names[i] = i+"";
		Cluster cluster = alg.performClustering(distances, names, new AverageLinkageStrategy());
		DendrogramPanel dp = new DendrogramPanel();
		dp.setModel(cluster);
	}
	
	public static void populatePairwiseDistances()
	{
		sizeNetwork = queryList.size();
        distances = new double[sizeNetwork][sizeNetwork];
        for(int i=0;i<sizeNetwork;i++)
        {
        	for(int j=0;j<sizeNetwork;j++)
        	{
        		double dist = getDistanceBetweenQueries(queryList.get(i),queryList.get(j));
        		distances[i][j] = dist;
        		distances[j][i] = dist;
        	}
        }
	}
	
	public static double getDistanceBetweenQueries(Query q1, Query q2)
	{
		double dist = 5;
		int r1 = getR1(q1, q2);
		int r2 = getR2(q1, q2);
		int r3 = getR3(q1, q2);
		dist = dist - (r1+r2+r3);
		return dist;
	}
	
	public static void loadQueryList() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream("data/UserStudy_AOLQueryList");
        ObjectInputStream ois = new ObjectInputStream(fis);
        queryList = (ArrayList<Query>) ois.readObject();
        ois.close();
        System.out.println("Loaded queryList with: "+queryList.size()+" queries");
	}
	
	public static int getR1(Query q1, Query q2)
	{
		// QUERY based r
		// this function will implement the part where we have 2 queries and we need to calculate the R1 relational score between these queries
		// (1-editDistance)%; similarity score; Jaccard similarity score; topical similarity score; inverse of temporal distance (same user), 
		int result = 0;
		String s1 = q1.query, s2 = q2.query;
		
		// editDistance - re-scaled to 0-10
		int ed = editDistance(s1,s2);
		int length = Math.max(s1.length(), s2.length());
		int temp1 = (1 - ed/length)*10;
		
		// Similarity score - 2 X no of common words
		int temp2 = 3*findNumberOfCommonWords(s1,s2);
		
		// Jaccard Similarity
		int temp3 = 0;
		
		// topical Similarity
		int temp4 = 0;
		
		// inverse of temporal Distance - IFF SAME USER
		int temp5 = 0;
		/*long secs = (q1.getqTime().getTime() - q2.getqTime().getTime()) / 1000;
		int hours = (int) (secs / 3600);
		if(q1.userID.compareTo(q2.userID)==0)
		{
			if(hours<2) temp5 = 10; else if(hours>2 && hours<4) temp5 = 8; else if(hours>4 && hours<6) temp5 = 6; else if(hours>6 && hours<8) temp5 = 4; else if(hours>8 && hours<10) temp5 = 2; else temp5 = 0;
		}*/
		
		// final result = sum over all tempi's
		
		//result = temp1 + temp2 + temp3+ temp4 + temp5;
		result = temp2;
		if(result>=3) result = 1; else result = 0;
		return result;
	}
	
	public static int getR2(Query q1, Query q2)
	{
		// URL based r
		// this function will implement the part where we have 2 queries and we need to calculate the R2 relational score between these queries
		// common ODP categories, common URLS, max/min of edit distance between URLs, avg of edit distance between URLs, Jaccard index between URLs
		
		int result = 0;
		
		// common ODP categories
		int temp1 = 0;
		
		// common URLs +++ editDistance: min & avg
		int temp2 = 0, temp3 = 0, temp4 = 0;
		// ignoring the URLs as of now, instead just focusing on the documents
		Iterator<String> itr = q1.urls.values().iterator();
		int min = 1000, minSize = 0, avg=0, count=0;
		while(itr.hasNext())
		{
			String url1 = itr.next();
			if(url1.length()<1) continue;
			if(q2.urls.containsKey(url1)) temp2++;
			
			Iterator<String> itr2 = q2.urls.values().iterator();
			while(itr2.hasNext())
			{
				String url2 = itr2.next();
				if(url2.length()<1) continue;
				int ed = editDistance(url1, url2);
				if(min>=ed)
				{
					min = ed;
					minSize = Math.max(url1.length(), url2.length());
					//System.out.println(min+"_"+minSize);
				}
				avg+= ed; count++;
			}
		}
		if(count == 0) {temp4 = 0;temp3=0;}
		else
		{
			//System.out.println("min:"+min+"_minsize:_"+minSize+" count:_"+count);
			avg = avg/count;
			temp4 = 10-avg;
			if(temp4<0) temp4=0;
			temp3 = (1 - min/minSize)*10;
		}
		//*/
		
		
		// jaccard index between URLs
		int temp5 = 0;
		
		// snippets common words & title common words
		int temp6 = 0;
		/*
		int nSnippet = 0, nTitle = 0;
		Iterator<Document> itr1 = q1.documents.iterator();
		while(itr1.hasNext())
		{
			Document d1 = itr1.next();
			Iterator<Document> itr2 = q2.documents.iterator();
			while(itr2.hasNext())
			{
				Document d2 = itr2.next();
				// now find the common words between these two documents
				nSnippet += findNumberOfCommonWords(d1.snippet, d2.snippet);
				nTitle += findNumberOfCommonWords(d1.title, d2.title);
			}
		}
		int nSAvg = nSnippet/100;
		int nTAvg = nTitle/100;
		
		if(nSAvg+nTAvg > 20) temp6 = 10;
		else if(nSAvg+nTAvg > 10) temp6 = 7;
		else temp6 = nSAvg+nTAvg;
		*/
		/*
		Iterator<String> itr = q1.wordsInDocumentsSnippets.keySet().iterator();
		while(itr.hasNext())
		{
			String word = itr.next();
			if(stopWords.containsKey(word)) continue;
			if(q2.wordsInDocumentsSnippets.containsKey(word)) temp6++;
		}
		Iterator<String> itr1 = q1.wordsInDocumentsTitle.keySet().iterator();
		while(itr1.hasNext())
		{
			String word = itr1.next();
			if(stopWords.containsKey(word)) continue;
			if(q2.wordsInDocumentsTitle.containsKey(word)) temp6++;
		}
		*/
		result = temp1 + temp2 + temp3 + temp4 + temp5;
		//result = temp6/50;
		//System.out.println("--------------------------------------------------------"+result);
		if(result>3) result = 1; else result = 0;
		return result;
	}
	
	public static int getR3(Query q1, Query q2)
	{
		// SESSION based r
		// this function will implement the part where we have 2 queries and we need to calculate the R3 relational score between these queries
		int result = 0;
		/*
		// same user
		if(q1.userID.compareTo(q2.userID) == 0) result++;
		
		long secs = (q1.getqTime().getTime() - q2.getqTime().getTime()) / 1000;
		int hours = (int) (secs / 3600);
		// same user as well as same session
		if(hours<4 && (q1.userID.compareTo(q2.userID) == 0)) result++;
		*/
		if(q1.sessionID == q2.sessionID) result = 3;
		return result;
	}
	
	public static int findNumberOfCommonWords(String s1, String s2)
	{
		int result1 = 0;
		String parts1[] = s1.split(" ");
		for(int i=0;i<parts1.length;i++)
		{
			if(s2.contains(parts1[i])) result1++;
		}
		
		/*int result2 = 0;
		String parts2[] = s2.split(" ");
		for(int i=0;i<parts2.length;i++)
		{
			if(s1.contains(parts2[i])) result2++;
		}
		if(result1>result2)
			return result1;
		else
			return result2;
			*/
		return result1;
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
