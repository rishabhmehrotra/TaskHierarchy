package datastr;

import java.io.Serializable;

public class Document implements Serializable{
	
	public String url;
	public String clueweb12id;
	public String title;
	public String snippet;
	
	public Document(String url, String clueweb12id, String title, String snippet)
	{
		this.url = url;
		this.clueweb12id = clueweb12id;
		this.title = title;
		this.snippet = snippet;
		//System.out.println("---\nDocument created: \nURL:_"+this.url+"\nclueweb12id:_"+this.clueweb12id+"\ntitle:_"+this.title);
	}
}
