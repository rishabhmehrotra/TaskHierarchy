package experiments;

import java.io.*;
import java.util.*;

import javax.xml.soap.Node;

import datastr.Tree;

//this is the class for evaluation metric: pairwise F/R/P scores on the AOL logs
public class ComputePairwiseFRPScoresOnAOL {

	public ArrayList<Tree> clusters;

	public Tree finalTree;

	public ComputePairwiseFRPScoresOnAOL(Tree finalTree) throws ClassNotFoundException, IOException
	{
		this.finalTree = finalTree;
		System.out.println("Inside ComputePairwiseFRPScoresOnAOL ---  Loaded the tree with "+finalTree.nChildren+" children");
		flattenHierarchy();
	}

	public void flattenHierarchy()
	{
		int tID = 1000;
		clusters = new ArrayList<Tree>();
		Queue<Tree> q = new LinkedList<Tree>();
		q.add(finalTree);
		while(q.size()<50)
		{
			Tree t = q.remove();
			
			Tree temp = new Tree(tID++);
			Iterator<Tree> itr = t.childTrees.iterator();
			while(itr.hasNext())
			{
				Tree tt = itr.next();
				if(tt.nodeList.size() < 4) {temp.addChildTree(tt);}
				else q.add(tt);
				System.out.println("Added tree with "+tt.nodeList.size()+" nodes");
			}
			if(temp.nodeList.size()>1) q.add(temp);
			System.out.println("Size of the queue:"+q.size());

		}
		System.out.println("Size of the queue:"+q.size()+"\n\n===========\n\n");
		//System.exit(0);
		Iterator<Tree> itr = q.iterator();
		while(itr.hasNext())
		{
			Tree t = itr.next();
			System.out.println(t.nodeList.size()+"__"+t.nChildren);
			Iterator<datastr.Node> it = t.nodeList.iterator();
			while(it.hasNext())
			{
				datastr.Node n = it.next();
				System.out.print(n.q.query+"\n");
			}
			System.out.println("\n\n\n========\n\n\n");
		}
	}
}
