package initialSetup;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import datastr.Query;

public class ExtractRelevantQueriesFromAOL {
	// the purpose of this class is to extract the queries present in Lucchese's work with their entire data
	// OUTPUT: queriesMapFromAOL_Gold   ...  HashMap<String, Query>, all sets of queries which are in the gold set
	
	public static HashMap<String, Query> queries;
	
	public static void main(String args[]) throws IOException, ParseException
	{
		queries = new HashMap<String, Query>();
		BufferedReader br1 = new BufferedReader(new FileReader("data/AOLtasks/merged"));
		String line1 = br1.readLine();
		BufferedReader br2 = new BufferedReader(new FileReader("data/AOLtasks/all-tasks.txt"));
		String line2 = br2.readLine();
		//117514	1	1	1	garden botanika.com
		//user-ct-test-collection-06.txt:117514	garden botanika.com	2006-03-01 14:23:17	1	http://www.redtagdeals.com
		int c1=0,c2=0;
		while(line2!=null)
		{
			c1++;
			String parts2[] = line2.split("\t");
			String queryText = parts2[4];//.toLowerCase();
			br1 = new BufferedReader(new FileReader("data/AOLtasks/merged"));
			line1 = br1.readLine();
			c2=0;
			while(line1!=null)
			{
				String parts1[] = line1.split("\t");
				String qText = parts1[1];//.toLowerCase();
				if(queryText.equalsIgnoreCase(qText))
				{
					//System.out.println(queryText+"__"+qText);

					if(queries.containsKey(qText))
					{
						if(parts1.length>4)
						{
							Query q = queries.get(qText);
							q.addURL(parts1[4]);
							queries.put(qText, q);
						}
					}
					else
					{
						Query q = new Query(qText);
						if(parts1.length>4) q.addURL(parts1[4]);
						String userID = parts2[0];
						q.setUserID(userID);
						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date qTime = df.parse(parts1[2]);
						q.setqTime(qTime);
						
						queries.put(qText, q);
					}
					c2++;
				}
				line1 = br1.readLine();
			}
			//System.out.println("------- For line "+c1+" in all-tasks.txt obtained "+c2+" no of matches");
			line2 = br2.readLine();
		}
		//System.out.println("Populated hashmap of queries with "+queries.size()+" no of queries");
		Iterator<Query> itr = queries.values().iterator();
		System.out.println("-------------------------------------------------------------------\n----------\n----------------------------");
		while(itr.hasNext())
		{
			Query q = itr.next();
			System.out.println(q.urls.size()+"______"+q.query+"_"+q.userID+"_"+"_"+q.getqTime());
		}
		
		//FileOutputStream fos = new FileOutputStream("data/AOLtasks/queriesMapFromAOL_Gold_demo");
		FileOutputStream fos = new FileOutputStream("data/AOLtasks/queriesMapFromAOL_Gold");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(queries);

		fos.close();
		System.out.println("Written queriesMapFromAOL_Gold to disk");
	}

}
