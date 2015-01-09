package experiments;

import core.*;

import java.io.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import datastr.Query;

public class ReadSessionTrack {

	public static ArrayList<Query> queryList = new ArrayList<Query>();

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		File file = new File("/Users/rishabhmehrotra/dev/UCL/sessionTrack/sessiontrack2014.xml");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		doc.getDocumentElement().normalize();
		System.out.println("Root element " + doc.getDocumentElement().getNodeName());
		NodeList sessionList = doc.getElementsByTagName("session");
		System.out.println("Total no of sessions:"+sessionList.getLength());
		//System.exit(0);
		int c=0, sessionID=0, nQ=0;
		for (int s = 0; s < sessionList.getLength(); s++)
		{
			//if(nQ>100) break;
			Node node1 = sessionList.item(s);
			sessionID++;
			if (node1.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element1 = (Element) node1;
				NodeList interactionsList = element1.getElementsByTagName("interaction");

				for(int i=0;i<interactionsList.getLength();i++)
				{
					Node node2 = interactionsList.item(i);
					Element interaction = (Element) node2;
					NodeList queryLst = interaction.getElementsByTagName("query");
					Element query = (Element) queryLst.item(0);
					NodeList fstNm = query.getChildNodes();
					String qString = ((Node) fstNm.item(0)).getNodeValue();
					//System.out.println("Query: "  + qString+"  c="+c++);
					Query q = new Query(qString);
					q.sessionID = sessionID;
					nQ++;
					// now get results/documents for this query
					NodeList resultList = interaction.getElementsByTagName("result");
					for(int j=0;j<resultList.getLength();j++)
					{
						Node node3 = resultList.item(j);
						Element result = (Element) node3;
						String url, clueweb12id, title, snippet;
						url = getValue(result, "url");
						clueweb12id = getValue(result, "clueweb12id");
						title = getValue(result, "title");
						snippet = getValue(result, "snippet");
						datastr.Document d = new datastr.Document(url, clueweb12id, title, snippet);
						q.addDoc(d);
						c++;
					}
					queryList.add(q);
				}
			}
		}
		System.out.println("total no of queries added to queryList: "+queryList.size());
		System.out.println("total no of docs added overall: "+c);
		saveQueryList();
		System.out.println("QueryList from Session Track saved to file");
	}
	
	public static void saveQueryList() throws IOException
	{
		FileOutputStream fos = new FileOutputStream("data/sessionTrack2014_queryList");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(queryList);
		fos.close();
	}
	
	public static String getValue(Element e, String field)
	{
		NodeList nList = e.getElementsByTagName(field);
		System.out.println("=="+nList.getLength());
		Element e1 = (Element) nList.item(0);
		//System.out.println(e1.getChildNodes().toString());
		NodeList fstNm = e1.getChildNodes();
		
		if(fstNm.getLength() == 0){System.out.println("~~~~~~~~~~~~~~~~~~~error!!!");return "";}
		String result = ((Node) fstNm.item(0)).getNodeValue();
		return result;
	}

}
