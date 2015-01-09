package datastr;

import java.io.Serializable;

public class Node implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int nodeID;
	public Query q;

	public Node(int nodeID)
	{
		this.nodeID = nodeID;
	}
	
	public Query getQ() {
		return q;
	}

	public void setQ(Query q) {
		this.q = q;
	}
}
