package baselines;
import datastr.*;
import hc.*;
import hc.visualization.*;

import java.io.*;
import java.util.*;
// this class will take as input an arraylist of queries and run agglomerative clustering algo
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
		double dist = 1;
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

}
