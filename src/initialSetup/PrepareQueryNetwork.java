package initialSetup;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import datastr.Query;

public class PrepareQueryNetwork {
	
	public static ArrayList<Query> queryList;
	public static int qID;
	
	public static void main(String[] args) throws IOException, ParseException
	{
		qID = 0;
		queryList = new ArrayList<Query>();
		importQueryLog();
		populateQueryNetwork();
	}
	
	public static void importQueryLog() throws IOException, ParseException
	{
		BufferedReader br = new BufferedReader(new FileReader("data/searchLog"));
		String line = br.readLine();
		String prevQ="";
		while(line!=null)
		{
			String parts[] = line.split("\t");
			String userID = parts[0];
			String query = parts[1];
			String qTimeString = parts[2];
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date qTime = df.parse(qTimeString);
			
			if(query.compareTo(prevQ)==0){line = br.readLine();continue;}
			prevQ = query;
			
			Query q = new Query(qID++);
			q.query = query; q.userID = userID; q.qTime = qTime;
			
			queryList.add(q);
			line = br.readLine();
		}
	}
	
	public static void populateQueryNetwork() throws IOException
	{
		FileWriter fstream = new FileWriter("data/queryNetwork");
		BufferedWriter out = new BufferedWriter(fstream);
		
	}
}
