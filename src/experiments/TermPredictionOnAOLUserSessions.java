package experiments;
import java.io.*;
import java.util.*;

import datastr.UserSession;

public class TermPredictionOnAOLUserSessions {
	
	public static HashMap<String, UserSession> userSessionsMap;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		loadPrunedUserSessionsFromDisk();
		System.out.println("Loaded AOL/ST user sessions from disk: "+userSessionsMap.size());
		populateTrainTestForUserSessions();
		// since we are assuming that each user will have one session, userID will uniquely identify each session we have
		loadSTTaskHierarchyTree();
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
