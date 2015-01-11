package datastr;
import java.io.Serializable;
import java.util.*;

public class UserSession implements Serializable{
	
	private static final long serialVersionUID = 1L;
	public String userID;
	public ArrayList<Query> queries;
	public HashMap<String, Query> selfTrain;
	public HashMap<String, Query> selfTest;
	public HashMap<String, Integer> selfTrainBoW;
	public HashMap<String, Integer> selfTestBoW;
	
	public UserSession(String userID)
	{
		this.userID = userID;
		this.queries = new ArrayList<Query>();
	}
	
	public void addQueryToUserSession(Query q)
	{
		this.queries.add(q);
	}
	
	public void populateSelfTrainTestQueriesForTermPrediction()
	{
		Iterator<Query> itr = this.queries.iterator();
		int count = 0, size = this.queries.size();
		while(itr.hasNext())
		{
			Query q = itr.next();
			if(count<(size/5))
				this.selfTrain.put(q.query,q);
			else
				this.selfTest.put(q.query,q);
		}
	}
	
	public void populateSelfTrainTestBoW()
	{
		// TRAIN part
		Iterator<String> itr = this.selfTrain.keySet().iterator();
		while(itr.hasNext())
		{
			String query = itr.next();
			String parts[] = query.split(" ");
			for(int i=0;i<parts.length;i++)
			{
				String term = parts[i].toLowerCase().trim();
				if(this.selfTrainBoW.containsKey(term))
				{
					int c = this.selfTrainBoW.get(term);
					c++;
					this.selfTrainBoW.put(term, new Integer(c));
				}
				else
					this.selfTrainBoW.put(term, new Integer(1));
			}
		}
		
		// test part
				Iterator<String> itr1 = this.selfTest.keySet().iterator();
				while(itr1.hasNext())
				{
					String query = itr1.next();
					String parts[] = query.split(" ");
					for(int i=0;i<parts.length;i++)
					{
						String term = parts[i].toLowerCase().trim();
						if(this.selfTestBoW.containsKey(term))
						{
							int c = this.selfTestBoW.get(term);
							c++;
							this.selfTestBoW.put(term, new Integer(c));
						}
						else
							this.selfTestBoW.put(term, new Integer(1));
					}
				}
	}
}
