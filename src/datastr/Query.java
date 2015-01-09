package datastr;
import java.io.Serializable;
import java.util.*;

public class Query implements Serializable{

	private static final long serialVersionUID = 1L;
	public String query;
	public int qID;
	public double score;
	public String userID;
	public Date qTime;
	public HashMap<String, String> urls;
	public ArrayList<Document> documents;// only valid for session track data
	public int sessionID;//only valid for Session Track data

	public Query(int ID)
	{
		this.qID = ID;
		documents = new ArrayList<Document>();
	}
	
	public Query(String query)
	{
		this.query = query;
		this.urls = new HashMap<String, String>();
		documents = new ArrayList<Document>();
	}
	
	public void addDoc(Document d)
	{
		this.documents.add(d);
	}
	
	public void addURL(String url)
	{
		if(this.urls.containsKey(url));
		else this.urls.put(url, url);
	}
	
	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public Date getqTime() {
		return qTime;
	}

	public void setqTime(Date qTime) {
		this.qTime = qTime;
	}
}
