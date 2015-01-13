package experiments;
import java.io.*;
import java.util.*;

import datastr.*;

public class TermPredictionOnAOLUserSessions {
	
	public static HashMap<String, UserSession> userSessionsMap;
	public static ArrayList<Tree> taskList;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		loadPrunedUserSessionsFromDisk();
		System.out.println("Loaded AOL/ST user sessions from disk: "+userSessionsMap.size());
		// since we are assuming that each user will have one session, userID will uniquely identify each session we have
		populateTrainTestForUserSessions();

		loadSTTaskHierarchyTree();
		// now convert the hierarchy to a flat list of tasks/trees wherein each tree will have a list of nodes
		convertHierarchyToFlatTaskList();
		// now we have the userSessions as well as the tasks, we need to evaluate the term prediction
		// Step 1: for each session, we use its train part to find the closest task 
		// Step 2: then use that task to predict terms and see how many can be retrieved
		termPredictionForEachSession();
	}
	
	public static void termPredictionForEachSession()
	{
		// Step 1: for each session, we use its train part to find the closest task
		// ASSUMPTION: Just retrieving ONE TASK per session, we can also try getting top-k tasks and get terms etc
		Iterator<UserSession> itr = userSessionsMap.values().iterator();
		while(itr.hasNext())
		{
			UserSession us = itr.next();
			Tree closest = findClosestTreeForThisSession(us);
			// Step 2: then use that task to predict terms and see how many can be retrieved
			int matches = findCommonTermsForSessionAndTask(us, closest);
		}
	}
	
	public static int findCommonTermsForSessionAndTask(UserSession us, Tree closest)
	{
		int matches = 0;
		// we have the task from which we need to do term prediction
		return matches;
	}
	
	public static Tree findClosestTreeForThisSession(UserSession us)
	{
		/* To match the userSession with a particular tree, we could do one of the following things:
		 * 1) find the task which contains at least one common query (or maximum common no of queries) with this session
		 * 2) find cosine similarity between session BoW and Task BoW and find the closest task
		 * For now, implementing the logic 1 - finding task with maximum no of query overlaps with the session in consideration
		 */
		
		Tree closest = null;
		
		// traverse through the entire task list and find no of overlapping queries
		int max = 0;
		Iterator<Tree> itr = taskList.iterator();
		while(itr.hasNext())
		{
			Tree t = itr.next();
			// now t is the current task in consideration
			int common = 0;
			Iterator<Query> itr1 = us.queries.iterator();
			while(itr1.hasNext())
			{
				Query q = itr1.next();
				// now for this query, scan through the entire list of nodes in the task t to find if its present
				Iterator<Node> itr2 = t.nodeList.iterator();
				while(itr2.hasNext())
				{
					Node n = itr2.next();
					if(n.q.query.compareToIgnoreCase(q.query) == 0) common++;
				}
			}
			if(common>max)
			{
				max = common;
				closest = t;
			}
		}
		System.out.println("Found the closest tree, it has no of common queries = "+max);
		return closest;
	}
	
	public static void convertHierarchyToFlatTaskList()
	{
		
	}
	
	public static void populateTrainTestForUserSessions()
	{
		// for each user session, we divide the queries into 2 maps, training & test - we'll use the training part to get the task
		// and suggest queries from that particular task to predict the queries/terms in th etest map
		Iterator<UserSession> itr = userSessionsMap.values().iterator();
		while(itr.hasNext())
		{
			UserSession us = itr.next();
			us.populateSelfTrainTestQueriesForTermPrediction();
			us.populateSelfTrainTestBoW();
		}
	}
	
	public static void loadSTTaskHierarchyTree() throws IOException, FileNotFoundException
	{
		// load the already constructed tree/hierarchy here
	}
	
	public static void loadPrunedUserSessionsFromDisk() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream("data/prunedUserSessionsMap-AOL-SessionTrack");
		ObjectInputStream ois = new ObjectInputStream(fis);
		userSessionsMap = (HashMap<String, UserSession>) ois.readObject();
		ois.close();
	}
}
