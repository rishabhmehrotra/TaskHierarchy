package datastr;
import java.io.Serializable;
import java.util.*;

public class UserSession implements Serializable{
	
	private static final long serialVersionUID = 1L;
	public String userID;
	public ArrayList<Query> queries;
	
	public UserSession(String userID)
	{
		this.userID = userID;
		this.queries = new ArrayList<Query>();
	}
	
	public void addQueryToUserSession(Query q)
	{
		this.queries.add(q);
	}
}
