package initialSetup;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import datastr.*;

public class GetAOLUsersForAMT {

	public static HashMap<String, User> users;
	public static HashMap<String, String> queries;
	public static HashMap<String, String> queriesinfo;
	public static HashMap<String,Query> queryMap;
	public static ArrayList<Query> queryList;

	public static void main(String[] args) throws IOException, ParseException {
		//System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("output1.txt"))));
		queries = new HashMap<String, String>();
		queriesinfo = new HashMap<String, String>();
		queryMap = new HashMap<String,Query>();
		queryList = new ArrayList<Query>();
		String filename = "/Users/rishabhmehrotra/dev/workspace/TaskBasedUserModeling/src/data/AOL/AOL1.txt";
		BufferedReader br;
		String line = "";
		int start = 1;
		String prevUserID = "";
		int c=0, count=1;
		users = new HashMap<String, User>();
		while(count>0)
		{
			filename = "/Users/rishabhmehrotra/dev/workspace/TaskBasedUserModeling/src/data/AOL/AOL"+count+".txt";
			
			br = new BufferedReader(new FileReader(filename));
			line = br.readLine();line = br.readLine();
			while(line!=null)
			{
				try{
					c++;
					//if(c==100) break;
					String parts[] = line.split("\t");
					String userID = "";


					if(line.length()<1 || parts.length<1) {line = br.readLine();continue;}
					userID = parts[0];
					String time="", rank="", curl="";
					if(parts.length>4)
					{
						time = parts[2];
						rank = parts[3];
						curl = parts[4];
					}
					//if(line.contains("2317930")) System.out.println("-----------------------"+userID);

					if(userID.compareTo(prevUserID) == 0)
					{

						HashMap<String, String> queriesMap = users.get(userID).queriesMap;
						if(queriesMap == null) queriesMap = new HashMap<String,String>();
						queriesMap.put(parts[1], "");
						User u = users.get(userID);
						u.setQueriesMap(queriesMap);
						u.setNumQ(queriesMap.size());
						users.put(userID, u);

					}
					else
					{

						HashMap<String, String> queriesMap = new HashMap<String,String>();
						queriesMap.put(parts[1], "");
						User u = new User(userID);
						u.setQueriesMap(queriesMap);
						u.setNumQ(queriesMap.size());
						users.put(userID, u);
					}
					prevUserID = userID;
					if(!queriesinfo.containsKey(parts[1]))
						queriesinfo.put(parts[1], time+"\t"+rank+"\t"+curl);
					line = br.readLine();
				} catch(Exception e) {e.printStackTrace();}
				queriesinfo.clear();
			}
			int cc = 0;
			System.out.println("Done with AOL"+(count+1)+".txt & no of users right now: "+c+"_"+users.size());
			Iterator<Map.Entry<String,User>> iter = users.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String,User> entry = iter.next();
				if(entry.getValue().numQ<1000 || entry.getValue().numQ>20000){
					iter.remove();
					cc++;
				}
			}
			System.out.println("Done with AOL"+(count+1)+".txt & no of users right now: "+c+"_"+users.size());
			System.out.println("Removed users: "+cc);
			br.close();
			br = new BufferedReader(new FileReader(filename));
			line = br.readLine();line = br.readLine();
			while(line!=null)
			{
				String parts1[] = line.split("\t");
				if(parts1.length<3) continue;
				String userID = parts1[0];
				if(!users.containsKey(userID)) {line = br.readLine();continue;}
				String qText = parts1[1];//.toLowerCase();
				if(queryMap.containsKey(qText))
				{
					Query q = queryMap.get(qText);
					if(parts1.length>4) q.addURL(parts1[4]);
					queryMap.put(qText, q);
				}
				else
				{
					Query q = new Query(qText);
					if(parts1.length>4) q.addURL(parts1[4]);
					q.setUserID(userID);
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date qTime = df.parse(parts1[2]);
					q.setqTime(qTime);
					queryMap.put(qText, q);
				}
				
				line = br.readLine();
			}
			br.close();
			System.out.println("Done with "+count);
			count--;
		}
		int cc=0;
		System.out.println("Printing details of valid users now...");
		Iterator<User> itr = users.values().iterator();
		while(itr.hasNext())
		{
			User u = itr.next();
			cc += u.numQ;
			System.out.println(u.numQ+"\t\t"+cc);
			Iterator<String> itr1 = u.queriesMap.keySet().iterator();
			while(itr1.hasNext())
			{
				String s = itr1.next();
				//System.out.println(s);
				if(!queries.containsKey(s)) queries.put(s, s);
			}
			System.out.println("-----------");
		}
		System.out.println("=====\nNo of queries:"+queries.size()+"\n=====\n");
		System.out.println("=====\nNo of queries:"+queryMap.size()+"\n=====\n");
		Iterator<Query> itr3 = queryMap.values().iterator();
		while(itr3.hasNext())
		{
			Query q = itr3.next();
			queryList.add(q);
		}
		FileOutputStream fos = new FileOutputStream("data/UserStudy_AOLQueryList");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(queryList);

		fos.close();
		System.exit(0);
		
		filename = "/Users/rishabhmehrotra/dev/workspace/TaskBasedUserModeling/src/data/AOL/AOL"+count+".txt";
		
		
		
		
		Iterator<String> itr2 = queriesinfo.keySet().iterator();
		while(itr2.hasNext())
		{
			String q = itr2.next();
			if(!queries.containsKey(q)) continue;
			String info = queriesinfo.get(q);
			System.out.println(q+"\t"+info);
		}

	}
}

class User
{
	public String userid;
	public HashMap<String, String> queriesMap;
	public int numQ;
	public int toRemove;

	public User(String userid)
	{
		this.userid = userid;
		this.queriesMap = new HashMap<String, String>();
		this.toRemove = 0;
	}

	public void setNumQ(int size) {
		this.numQ = size;
	}

	public void setQueriesMap(HashMap<String, String> queries) {
		this.queriesMap = queries;
	}
}
