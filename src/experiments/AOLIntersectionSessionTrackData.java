package experiments;

import java.io.*;
import java.util.*;

import datastr.*;

public class AOLIntersectionSessionTrackData {

	public static ArrayList<Query> queryList;
	public static String sessionTrackQueryListFile = "data/sessionTrack2014_queryList";
	public static HashMap<String, Integer> queryMap = new HashMap<String, Integer>(); // the integer is for the count of the query in the AOL logs
	//public static ArrayList<String> relevantUsersFromAOL = new ArrayList<String>();
	public static HashMap<String, Integer> relevantUsersMap = new HashMap<String, Integer>();
	public static HashMap<String, ArrayList<String>> userQueryMapForAOL = new HashMap<String, ArrayList<String>>();
	public static HashMap<String, UserSession> userSessionsMap = new HashMap<String, UserSession>();
	public static HashMap<String, String> commonQueries = new HashMap<String, String>();
	public static HashMap<String, UserSession> prunedUserSessionsMap = new HashMap<String, UserSession>();
	public static HashMap<Integer, Integer> commonSessionID = new HashMap<Integer, Integer>();
	public static ArrayList<Query> common_AOLST_QueryList = new ArrayList<Query>();

	public static void main(String[] args) throws IOException, ClassNotFoundException{
		int stage = 4;
		if(stage == 1)
		{
			// load session track, find common queries between session track & AOL & populate user-query map of common users-queries between AOL & ST(Session Track)
			loadSessionTrackQueryList();
			populateQueryMapFromQueryList();
			System.out.println("Total no of queries: "+queryList.size()+"_"+queryMap.size());
			traverseAOLLogsAndFindIntersectionWithSessionTrack();
			System.out.println("Total number of users in AOL who's query match with session Log data: "+userQueryMapForAOL.size());
			saveUserQueryMapForAOLToDisk();
			saveCommonQueriesMap();
		}
		else if(stage == 2)
		{
			// now we know which users to look for in the AOL logs -- now we need to extract the next-k queries from the user's sessions
			// we traverse the AOL logs, if user is present then build a session for that user
			loadUserQueryMapForAOLToDisk();
			populateUserSessionsFromAOLLogTraversal();
			saveUserSessionsToDisk();
		}
		else if(stage == 3)
		{
			// in this stage, we will prune the original session track to contain just the sessions which are relevant to the queries 
			// we obtained by AOL-ST common query finding...
			// NOTE: each Query in the session track will have a corresponding sessionID
			loadCommonQueries();
			loadUserSessionsFromDisk();
			pruneUserSessions();
			savePrunedUserSessionsToDisk();
		}
		else if(stage == 4)
		{
			// extra stage: find relevant sessions from the session track which have queries in the AOL as well
			loadSessionTrackQueryList();
			loadCommonQueries();
			findSessionIDsFromCommonQueries();
			populateQueryListFromCommonQueriesForTreeBuilding();
			saveCommon_AOLST_QueryListToDisk();
		}
	}
	
	public static void saveCommon_AOLST_QueryListToDisk() throws IOException
	{
		FileOutputStream fos = new FileOutputStream("data/common_AOLST_QueryList");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(common_AOLST_QueryList);
		fos.close();
		System.out.println("Saved the common_AOLST_QueryList to disk");
	}
	
	public static void populateQueryListFromCommonQueriesForTreeBuilding()
	{
		// traverse through the queryList and only put those queries whose sessionID is there in the common sessionID hashmap
		Iterator<Query> itr = queryList.iterator();
		while(itr.hasNext())
		{
			Query q = itr.next();
			if(commonSessionID.containsKey(q.sessionID))
			{
				common_AOLST_QueryList.add(q);
				//System.out.println(q.query+"_____"+q.documents.size());
			}
		}
		System.out.println("Total queries via common session IDs: "+common_AOLST_QueryList.size());
	}
	
	public static void findSessionIDsFromCommonQueries() throws IOException, ClassNotFoundException
	{
		Iterator<Query> itr = queryList.iterator();
		while(itr.hasNext())
		{
			Query q = itr.next();
			if(commonQueries.containsKey(q.query))
			{
				if(commonSessionID.containsKey(q.sessionID));
				else
				{
					commonSessionID.put(new Integer(q.sessionID), 1);
					//System.out.println(q.sessionID);
				}
			}
		}
		System.out.println("Sessions in ST via common queries in AOL & ST: "+commonSessionID.size());
	}
	
	public static void loadUserSessionsFromDisk() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream("data/userSessionsMap-AOL-SessionTrack");
		ObjectInputStream ois = new ObjectInputStream(fis);
		userSessionsMap = (HashMap<String, UserSession>) ois.readObject();
		ois.close();
	}
	
	public static void savePrunedUserSessionsToDisk() throws IOException
	{
		FileOutputStream fos = new FileOutputStream("data/prunedUserSessionsMap-AOL-SessionTrack");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(prunedUserSessionsMap);
		fos.close();
	}
	
	public static void pruneUserSessions()
	{
		// now we go through all the user sessions which have all the queries issued by that particular user
		// and prune it - if a query was common between AOL & ST we take the next 10 or so queries from this user and add it to our user session
		Iterator<String> itr = userSessionsMap.keySet().iterator();
		while(itr.hasNext())
		{
			String userID = itr.next();
			UserSession us = userSessionsMap.get(userID);
			int sz = us.queries.size();
			if(sz<50)
			{
				if(sz<10) continue;
				prunedUserSessionsMap.put(userID, us);
				continue;
			}
			Iterator<Query> itr1 = us.queries.iterator();
			while(itr1.hasNext())
			{
				Query q = itr1.next();
				if(commonQueries.containsKey(q.query))
				{
					// take the next N queries from this user session & add it to the pruned user session
					int N = 20; // no of follow-up queries to be used to populate the pruned user sessions
					if(prunedUserSessionsMap.containsKey(userID))
					{
						// the pruned map already contains the userID, that means we don't need to create a new userSession for this user
						UserSession us1 = prunedUserSessionsMap.get(userID);
						int temp = 0, flag = 0;
						// this way has one issue though - some common queries might get missed this way if they fall within 20 query range of the current query
						while(temp<N)
						{
							us1.addQueryToUserSession(q);
							//us1.queries.add(q);
							if(itr1.hasNext()) q = itr1.next();
							else {flag=1;break;}// set flag to note that we werent able to find N future queries
							temp++;
						}
						// now we have updated the user session with the new queries, so put it back
						if(flag == 0) prunedUserSessionsMap.put(userID, us1); //  add to map if we were able to find atleast N queries in the future
					}
					else
					{
						// this user isnt there in the pruned user session map, so we need to create a new user Session, add queries and populate the prunedUserSession map accordingly
						UserSession newUS = new UserSession(userID);
						int temp = 0, flag = 0;
						// REPEAT: this way has one issue though - some common queries might get missed this way if they fall within 20 query range of the current query
						while(temp<N)
						{
							newUS.addQueryToUserSession(q);
							//newUS.queries.add(q);
							if(itr1.hasNext()) q = itr1.next();
							else {flag=1;break;}// set flag to note that we werent able to find N future queries
							temp++;
						}
						// now we have updated the user session with the new queries, so put it back
						if(flag==0) prunedUserSessionsMap.put(userID, newUS); //  add to map if we were able to find atleast N queries in the future
					}
				}
			}
		}
		
		Iterator<String> itt = prunedUserSessionsMap.keySet().iterator();
		while(itt.hasNext())
		{
			String userID = itt.next();
			System.out.println(userID+" -- "+prunedUserSessionsMap.get(userID).queries.size()+" / "+userSessionsMap.get(userID).queries.size());
		}
		System.out.println("Populated the pruned User Session map with "+prunedUserSessionsMap.size()+" user sessions");
	}
	
	public static void loadCommonQueries() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream("data/commonQueriesAOL-STMap");
		ObjectInputStream ois = new ObjectInputStream(fis);
		commonQueries = (HashMap<String, String>) ois.readObject();
		ois.close();
	}
	
	public static void saveUserSessionsToDisk() throws IOException
	{
		FileOutputStream fos = new FileOutputStream("data/userSessionsMap-AOL-SessionTrack");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(userSessionsMap);
		fos.close();
	}

	public static void populateUserSessionsFromAOLLogTraversal() throws IOException
	{
		String filename = "/Users/rishabhmehrotra/dev/workspace/TaskBasedUserModeling/src/data/AOL/AOL1.txt";
		BufferedReader br;
		br = new BufferedReader(new FileReader(filename));
		String line = br.readLine();
		line = br.readLine();
		int start = 1;
		String prevUserID = "";
		int c=0, cc=0, count=10;
		while(count>0)
		{
			filename = "/Users/rishabhmehrotra/dev/workspace/TaskBasedUserModeling/src/data/AOL/AOL"+count+".txt";
			count--;
			br = new BufferedReader(new FileReader(filename));
			line = br.readLine();line = br.readLine();
			while(line!=null)
			{
				try{
					c++;
					String parts[] = line.split("\t");
					if(line.length()<1 || parts.length<1) {line = br.readLine();continue;}
					
					String userID = parts[0];
					if(userID.compareTo(prevUserID) == 0)
					{
						if(userQueryMapForAOL.containsKey(userID))
						{
							UserSession us = userSessionsMap.get(userID);
							Query q = new Query(parts[1]);
							us.addQueryToUserSession(q);
							userSessionsMap.put(userID, us);
							cc++;
						}
					}
					else
					{
						// check if this user is in the map of intersection
						if(userQueryMapForAOL.containsKey(userID))
						{
							// this user is present, so we create a new usersession
							UserSession us = new UserSession(userID);
							Query q = new Query(parts[1]);
							us.addQueryToUserSession(q);
							userSessionsMap.put(userID, us);
							cc++;
						}
					}
					prevUserID = userID;
					line = br.readLine();
				} catch(Exception e) {e.printStackTrace();}
			}
			System.out.println("Done with AOL"+(count+1)+".txt");
		}
		System.out.println("Total no of queries scanned from the entire AOL logs: "+c);
		System.out.println("Total no of userSessions populated: "+userSessionsMap.size()+" with a total of "+cc+" queries");
	}

	public static void loadUserQueryMapForAOLToDisk() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream("data/userQueryMapToConstructAOLSessions");
		ObjectInputStream ois = new ObjectInputStream(fis);
		userQueryMapForAOL = (HashMap<String, ArrayList<String>>) ois.readObject();
		ois.close();
	}
	
	public static void saveCommonQueriesMap() throws IOException
	{
		FileOutputStream fos = new FileOutputStream("data/commonQueriesAOL-STMap");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(commonQueries);
		fos.close();
	}

	public static void saveUserQueryMapForAOLToDisk() throws IOException
	{
		FileOutputStream fos = new FileOutputStream("data/userQueryMapToConstructAOLSessions");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(userQueryMapForAOL);
		fos.close();
	}

	public static void traverseAOLLogsAndFindIntersectionWithSessionTrack() throws IOException
	{
		String filename = "/Users/rishabhmehrotra/dev/workspace/TaskBasedUserModeling/src/data/AOL/AOL1.txt";
		BufferedReader br;
		br = new BufferedReader(new FileReader(filename));
		String line = br.readLine();
		line = br.readLine();
		int start = 1;
		int c=0, count=10, temp=0;
		while(count>0)
		{
			filename = "/Users/rishabhmehrotra/dev/workspace/TaskBasedUserModeling/src/data/AOL/AOL"+count+".txt";
			count--;
			br = new BufferedReader(new FileReader(filename));
			line = br.readLine();line = br.readLine();
			while(line!=null)
			{
				try{
					c++;
					String parts[] = line.split("\t");
					String userID = "";


					if(line.length()<1 || parts.length<1) {line = br.readLine();continue;}
					userID = parts[0];
					String queryStr = parts[1];
					queryStr = queryStr.trim();
					if(queryMap.containsKey(queryStr))
					{
						if(!commonQueries.containsKey(queryStr)) commonQueries.put(queryStr, queryStr);
						int cQuery = queryMap.get(queryStr);
						cQuery++;
						if(cQuery == 1) temp++;
						queryMap.put(queryStr, new Integer(cQuery));
						//relevantUsersFromAOL.add(userID);
						if(!relevantUsersMap.containsKey(userID))
						{
							relevantUsersMap.put(userID, new Integer(1));
							ArrayList<String> qList = new ArrayList<String>();
							qList.add(queryStr);
							userQueryMapForAOL.put(userID, qList);
						}
						else
						{
							int uCount = relevantUsersMap.get(userID);
							uCount++;
							relevantUsersMap.put(userID, new Integer(uCount));
							ArrayList<String> qList = userQueryMapForAOL.get(userID);
							qList.add(queryStr);
							userQueryMapForAOL.put(userID, qList);
						}
					}
					line = br.readLine();
				} catch(Exception e) {e.printStackTrace();}
			}
			System.out.println("Done with AOL"+(count+1));
		}

		System.out.println("Total no of queries scanned: "+c);


		System.out.println("Analyzing the queryMap to see how many of Session Track queries are in AOL..\n\n\n\n==============\n\n");
		Iterator<String> itr = queryMap.keySet().iterator();
		int common = 0, totalCommonFromAOL=0;
		while(itr.hasNext())
		{
			String query = itr.next();
			int cQ = queryMap.get(query);
			if(cQ>0)
			{
				common++;
				System.out.println(query+"_"+cQ);
				totalCommonFromAOL+=cQ;
			}
		}
		System.out.println("\n\n=========\n\nCommon Queries: "+common+"/"+queryMap.size()+"  -- VERFIY->  temp:"+temp+"__"+totalCommonFromAOL);

		System.out.println("Printing details about the relevant users--> RelevantUsersMaps size: "+relevantUsersMap.size());
		/*
		System.out.println("Printing details about the individual users -->");
		Iterator<String> itr1 = relevantUsersMap.keySet().iterator();
		int mismatch = 0;
		while(itr1.hasNext())
		{
			String userID = itr1.next();
			int uC = relevantUsersMap.get(userID);
			int uQ = userQueryMapForAOL.get(userID).size();
			if(uQ!=uC) mismatch++;
			System.out.println("UserID: "+userID+" Count:"+uC+" uQ:"+uQ);
		}
		 */
		// LOGIC:
		// now whatr ewe could do is that we can store these userIDs in a file and populate/create a dataset which has the relevant sessions fomr the AOL logs
		// the thing to deicde will be wherther to include all the queries for the user or just the relevant parts - some k-queries in the future per user...
		// we could create a new data structure which just has the userID and the correspondong list of queries for the user we found in the session track
		//System.out.println(userQueryMapForAOL.size()+" mismatches:"+mismatch);
	}

	public static void populateQueryMapFromQueryList()
	{
		Iterator<Query> itr = queryList.iterator();
		while(itr.hasNext())
		{
			Query q = itr.next();
			queryMap.put(q.query, new Integer(0));
			//System.out.println(q.query+"_"+q.sessionID);
		}
	}

	public static void loadSessionTrackQueryList() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream(sessionTrackQueryListFile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		queryList = (ArrayList<Query>) ois.readObject();
		ois.close();
	}
}