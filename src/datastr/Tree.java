package datastr;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;


public class Tree implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int treeID;
	public ArrayList<Node> nodeList; // we assume that the nodeList is always updated with nodes from all the children
	public ArrayList<Tree> childTrees;
	public int nChildren;
	public double likelihood = 0.0;
	public boolean isLeaf;
	public int n1;
	public int n0;
	public int n1CH, n0CH;
	public double gXY;
	public double affScore;
	
	public boolean isMerge;
	public boolean isInitialMerge;
	
	// X & Y -- these are when the initial merge trees have just two children, we denote the trees with X & Y
	// also, for merging cosideration, we keep a track of potential merging candidates to calculate bayes factor score
	public Tree X,Y;
	
	public double bayesFactorScore;
	
	public HashMap<String, Integer> querywords;
	
	public Tree(int tID)
	{
		this.treeID = tID;
		this.nodeList = new ArrayList<Node>();
		this.childTrees = new ArrayList<Tree>();
		this.nChildren = 0;
		this.isLeaf = false;
		this.isMerge = false;
		this.isInitialMerge = false;
		this.bayesFactorScore = 0.0;
		this.n1 = 0; this.n0 = 0; this.n1CH = 0; this.n0CH = 0;
		this.querywords = new HashMap<String, Integer>();
	}
	
	public void populateQueryWords()
	{
		this.querywords = new HashMap<String, Integer>();
		Iterator<Node> itr = this.nodeList.iterator();
		//System.out.println("--------------------------------");
		while(itr.hasNext())
		{
			Node n = itr.next();
			String query = n.q.query.toLowerCase();
			//System.out.print(query+"___");
			String parts[] = query.split(" ");
			for(int i=0;i<parts.length;i++)
			{
				if(this.querywords.containsKey(parts[i]))
				{
					int temp = this.querywords.get(parts[i]);
					this.querywords.put(parts[i], new Integer(temp+1));
				}
				else
				{
					this.querywords.put(parts[i], new Integer(1));
				}
			}
		}
		/*System.out.println();
		Iterator<String> itr1 = this.querywords.keySet().iterator();
		while(itr1.hasNext())
		{
			System.out.print(itr1.next()+"==");
		}
		System.out.println("Populated tree with "+this.querywords.size()+" query words");
		*/
	}
	
	
	public static <K,V extends Comparable<? super V>> 
	List<Entry<K, V>> entriesSortedByValues(Map<K,V> map) {

		List<Entry<K,V>> sortedEntries = new ArrayList<Entry<K,V>>(map.entrySet());

		Collections.sort(sortedEntries, 
				new Comparator<Entry<K,V>>() {
			@Override
			public int compare(Entry<K,V> e1, Entry<K,V> e2) {
				return e2.getValue().compareTo(e1.getValue());
			}
		}
				);

		return sortedEntries;
	}

	public void addNode(Node n)
	{
		this.nodeList.add(n);
		//this.nChildren++; TODO: check, it wasnt commented before,shud we comment it?
		// we are incrementingnChildren only when we have merged t rees, not with initialTrees
	}
	
	public void addChildTree(Tree t)
	{
		this.childTrees.add(t);
		this.nChildren++;
		// now that a new child is added to this tree, we update the nodeList accordingly
		updateNodeList(t);
	}
	
	public void updateNodeList(Tree t)
	{
		Iterator<Node> itr = t.nodeList.iterator();
		while(itr.hasNext())
		{
			Node n = itr.next();
			this.nodeList.add(n);
		}
	}
	
	public Tree getX() {
		return X;
	}

	public void setX(Tree x) {
		X = x;
	}

	public Tree getY() {
		return Y;
	}

	public void setY(Tree y) {
		Y = y;
	}
}
