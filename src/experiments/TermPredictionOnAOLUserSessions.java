package experiments;
import java.io.*;
import java.util.*;

import datastr.*;

public class TermPredictionOnAOLUserSessions {
	
	public static HashMap<String, UserSession> userSessionsMap;
	public static ArrayList<Tree> taskList = new ArrayList<Tree>();// populated when we flatten the hierarchy
	public static Tree STHierarchyTree;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		loadPrunedUserSessionsFromDisk();
		System.out.println("Loaded AOL/ST user sessions from disk: "+userSessionsMap.size()+"\nGoing to populate train/test for user sessions");
		// since we are assuming that each user will have one session, userID will uniquely identify each session we have
		populateTrainTestForUserSessions();
		System.out.println("Done with populating train/test for user sessions...\nGoing to load tree now...");
		loadSTTaskHierarchyTree();
		System.out.println("Loaded the tree, now going to flatten it...");
		// now convert the hierarchy to a flat list of tasks/trees wherein each tree will have a list of nodes
		flattenHierarchy();
		System.out.println("Done with the flatening of the hierarchy...we have the taskList with "+taskList+" trees...\nGoing to populate BoW in taskList");
		// for each task in the taskList, we populate the BoW from queries in the tasks
		populateBoWForEachTaskInTaskList();
		System.out.println("Done with populating BoW in taskList...\nGoing to do term Prediction");
		// now we have the userSessions as well as the tasks, we need to evaluate the term prediction
		// Step 1: for each session, we use its train part to find the closest task 
		// Step 2: then use that task to predict terms and see how many can be retrieved
		termPredictionForEachSession();
	}
	
	public static void populateBoWForEachTaskInTaskList()
	{
		Iterator<Tree> itr = taskList.iterator();
		while(itr.hasNext())
		{
			Tree t = itr.next();
			// for this tree, we create a HashMap of BoW which contains all the terms present in all the queries in this tree
			t.populateQueryWords();
			System.out.println("For this task, no of Query Words populated: "+t.querywords.size()+" while no of queries: "+t.nodeList.size());
		}
	}
	
	public static void termPredictionForEachSession()
	{
		// Step 1: for each session, we use its train part to find the closest task
		// ASSUMPTION: Just retrieving ONE TASK per session, we can also try getting top-k tasks and get terms etc
		Iterator<UserSession> itr = userSessionsMap.values().iterator();
		double avgMatches = 0, skips=0;
		while(itr.hasNext())
		{
			UserSession us = itr.next();
			Tree closestTree = findClosestTreeForThisSession(us);
			if(closestTree == null)
			{
				// TODO
				System.out.println("No closest Tree found ... ");
				skips++;
				continue;
			}
			System.out.println("The closest tree found has "+closestTree.nodeList.size()+" queries");
			// Step 2: then use that task to predict terms and see how many can be retrieved
			int matches = findCommonTermsForSessionAndTask(us, closestTree);
			System.out.println("Matches found for this session: "+matches+"\n-----------");
			avgMatches += matches;
		}
		avgMatches = avgMatches/userSessionsMap.size();
		System.out.println("Avg no of term matches obtained: "+avgMatches);
		System.out.println("Skips: "+skips+" / "+userSessionsMap.size());
	}
	
	public static int findCommonTermsForSessionAndTask(UserSession us, Tree closestTree)
	{
		int matches = 0;
		/* we have the task from which we need to do term prediction
		 * We have the following possibilities for term prediction:
		 * 1) We consider all the words from task and see how many overlap
		 * 2) We do term ranking and vary the rank based term recommendation (top-10, top-20, etc)
		 * For now, we'll go with the 1st option - just find common words in session-testBoW & tree
		 */
		Iterator<String> itr1 = us.selfTestBoW.keySet().iterator();
		while(itr1.hasNext())
		{
			String term = itr1.next();
			if(closestTree.querywords.containsKey(term)) matches++;
		}
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
		//if(max == 0)//then no tree has any query overlap, for now skipping this, TODO
		return closest;
	}
	
	public static void flattenHierarchy()
	{
		int tID = 2000;
		Queue<Tree> q = new LinkedList<Tree>();
		q.add(STHierarchyTree);
		while(q.size()<50)
		{
			Tree t = q.remove();
			
			Tree temp = new Tree(tID++);
			Iterator<Tree> itr = t.childTrees.iterator();
			while(itr.hasNext())
			{
				Tree tt = itr.next();
				if(tt.nodeList.size() < 4) {temp.addChildTree(tt);}
				else q.add(tt);
				//System.out.println("Added tree with "+tt.nodeList.size()+" nodes");
			}
			if(temp.nodeList.size()>1) q.add(temp);
			//System.out.println("Size of the queue:"+q.size());
		}
		System.out.println("Size of the queue:"+q.size()+"\n\n===========\n\n");
		Iterator<Tree> itr = q.iterator();
		while(itr.hasNext())
		{
			Tree t = itr.next();
			taskList.add(t);
		}
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
			System.out.println("For this user session, train: "+us.selfTrainBoW.size()+" test: "+us.selfTestBoW.size());
		}
	}
	
	public static void loadSTTaskHierarchyTree() throws IOException, ClassNotFoundException
	{
		// load the already constructed tree/hierarchy here
		FileInputStream fis = new FileInputStream("data/finalTreeObtainedbyBHCD_SessionTrack2014_full");
        ObjectInputStream ois = new ObjectInputStream(fis);
        STHierarchyTree = (Tree) ois.readObject();
        ois.close();
	}
	
	public static void loadPrunedUserSessionsFromDisk() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream("data/prunedUserSessionsMap-AOL-SessionTrack");
		ObjectInputStream ois = new ObjectInputStream(fis);
		userSessionsMap = (HashMap<String, UserSession>) ois.readObject();
		ois.close();
	}
}
