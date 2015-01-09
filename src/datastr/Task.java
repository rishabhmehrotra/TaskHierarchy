package datastr;
import java.io.Serializable;
import java.util.*;

// as of now, only used in the Extract Gold Task class
public class Task implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public ArrayList<Query> queryList;
	
	public Task(){
		this.queryList = new ArrayList<Query>();
	}
	
	public void addQuery(Query q)
	{
		this.queryList.add(q);
	}
}
