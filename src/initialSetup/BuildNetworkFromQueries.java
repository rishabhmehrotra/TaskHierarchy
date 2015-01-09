package initialSetup;

import java.io.*;
import java.util.*;

import datastr.Query;

public class BuildNetworkFromQueries {
	
	public HashMap<String, Query> queries;
	public ArrayList<Query> queryList;
	
	@SuppressWarnings("unchecked")
	public BuildNetworkFromQueries() throws IOException, ClassNotFoundException
	{
		//FileInputStream fis = new FileInputStream("data/AOLtasks/queriesMapFromAOL_Gold_demo");
		FileInputStream fis = new FileInputStream("data/AOLtasks/queriesMapFromAOL_Gold");
        ObjectInputStream ois = new ObjectInputStream(fis);
        queries = (HashMap<String, Query>) ois.readObject();
        ois.close();

        System.out.println("Loaded queries hashmap with no of queries: "+queries.size());
        // now we need to construct the query network & populate the data/network file with the network matrix

        queryList = new ArrayList<Query>(queries.values());
        
        int N = queryList.size();
        int network[][] = new int[N][N];
        for(int i=0;i<N;i++)
        {
        	for(int j=0;j<N;j++)
        	{
        		if(i==j) network[i][j] = 0;
        		else
        		{
        			Query qi = queryList.get(i);
        			Query qj = queryList.get(j);
        			
        			// just session based network as of now
        			long difference = qi.getqTime().getTime() - qj.getqTime().getTime();
        			long diffInSeconds = difference/1000;
        			int diffInMints = (int) diffInSeconds/60;
        			
        			if(diffInMints <= 20 && qi.userID.equals(qj.userID)) network[i][j]=1;
        			else network[i][j] = 0;
        		}
        	}
        }
        //save the network obtained to file data/network
        FileWriter fstream = new FileWriter("data/qNetwork");
		BufferedWriter out = new BufferedWriter(fstream);
		for(int i =0;i<N;i++)
		{
			for(int j=0;j<N;j++)
			{
				if(j==N-1) out.write(network[i][j]+"\n");
				else out.write(network[i][j]+"\t");
			}
			//out.write("\n");
		}
		// now save the ArrayList of queries for later re-use
		//FileOutputStream fos = new FileOutputStream("data/queriesLISTFromAOL_Gold_demo");
		FileOutputStream fos = new FileOutputStream("data/queriesLISTFromAOL_Gold");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(queryList);
		fos.close();
	}
	
	public static void main(String args[]) throws IOException, ClassNotFoundException
	{
		new BuildNetworkFromQueries();
	}
}
