package experiments;
import java.io.*;
import java.util.*;

import datastr.UserSession;

public class TermPredictionOnAOLUserSessions {
	
	public static HashMap<String, UserSession> userSessionsMap;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		loadUserSessionsFromDisk();
		System.out.println("Loaded AOL/ST user sessions from disk: "+userSessionsMap.size());
		// since we are assuming that each user will have one session, userID will uniquely identify each session we have
		loadSTTaskHierarchyTree();
	}
	
	public static void loadSTTaskHierarchyTree() throws IOException, FileNotFoundException
	{
		// load the already constructed tree/hierarchy here
	}
	
	public static void loadUserSessionsFromDisk() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream("data/userSessionsMap-AOL-SessionTrack");
		ObjectInputStream ois = new ObjectInputStream(fis);
		userSessionsMap = (HashMap<String, UserSession>) ois.readObject();
		ois.close();
	}
}
