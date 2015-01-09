package experiments;

import core.*;

import java.io.*;
import java.util.*;

import datastr.Query;
import datastr.Task;

public class ExtractGoldTaskAOL {
	
	public static String taskFile = "data/AOLtasks/all-tasks.txt";
	public static String outputTaskListFile = "data/AOL_GOLD_TaskList_Lucchese";
	public static ArrayList<Task> taskList;
	
	public static void main(String[] args) throws IOException{
		
		
		taskList = new ArrayList<Task>();
		
		populateTaskListWithTask();
		saveTaskListToDisk();
		
	}
	
	public static void populateTaskListWithTask() throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(taskFile));
		String line = br.readLine();
		int tPrev=0, sPrev = 0, flagForFirstTime = 1;
		Task t = null;
		while(line!=null)
		{
			String parts[] = line.split("\t");
			String userID = parts[0];
			int sNow = Integer.parseInt(parts[1]);
			int tNow = Integer.parseInt(parts[2]);
			String query = parts[4];
			Query q = new Query(query);
			q.userID = userID;
			
			if(tNow != tPrev || sNow != sPrev)
			{
				if(flagForFirstTime != 1) taskList.add(t);
				else flagForFirstTime = 0;
				System.out.println("------------------------");
				t = new Task();
				t.addQuery(q);
			}
			else
			{
				t.addQuery(q);
			}
			System.out.println(line);
			tPrev = tNow;
			sPrev = sNow;
			line = br.readLine();
		}
		taskList.add(t);
		System.out.println("Total tasks populated in the taskList: "+taskList.size());
		br.close();
	}
	
	public static void saveTaskListToDisk() throws IOException
	{
		FileOutputStream fos = new FileOutputStream(outputTaskListFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(taskList);
		fos.close();
	}
}
