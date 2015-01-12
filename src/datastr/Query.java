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
	public HashMap<String, Integer> wordsInDocumentsSnippets;
	public HashMap<String, Integer> wordsInDocumentsTitle;

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
	
	public void populateWordsInDocHashMap()
	{
		this.wordsInDocumentsSnippets = new HashMap<String, Integer>();
		this.wordsInDocumentsTitle = new HashMap<String, Integer>();
		
		Iterator<Document> itr = this.documents.iterator();
		while(itr.hasNext())
		{
			Document d = itr.next();
			String snippet = d.snippet;
			String title = d.title;
			String parts1[] = snippet.split(" ");
			for(int i=0;i<parts1.length;i++)
			{
				if(this.wordsInDocumentsSnippets.containsKey(parts1[i]))
				{
					int c = this.wordsInDocumentsSnippets.get(parts1[i]);
					c++;
					this.wordsInDocumentsSnippets.put(parts1[i], new Integer(c));
				}
				else
					this.wordsInDocumentsSnippets.put(parts1[i], new Integer(1));
			}
			String parts2[] = title.split(" ");
			for(int i=0;i<parts2.length;i++)
			{
				if(this.wordsInDocumentsTitle.containsKey(parts2[i]))
				{
					int c = this.wordsInDocumentsTitle.get(parts2[i]);
					c++;
					this.wordsInDocumentsTitle.put(parts2[i], new Integer(c));
				}
				else
					this.wordsInDocumentsTitle.put(parts2[i], new Integer(1));
			}
		}
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
