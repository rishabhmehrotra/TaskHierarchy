package core;

import java.io.*;
import java.util.*;

import datastr.Node;
import datastr.Query;
import datastr.Tree;

// this class implements the code for the Bayesian Hierarchical Community Detection algorithm as proposed in NIPS 2013.
// this takes in a 0/1 NxN matrix with N = no of nodes and outputs a single tree with all the N nodes as its nodes.
// we now have to modify it to incorporate additional information: for each pair of nodes, we will have some three or so types of information.
// we then need to build our tree so that we incorporate this information as well via Dirichlet-Multinomial prior
public class BHCD {
	
	public static int sizeNetwork;
	public static String networkDataFile = "data/qNetwork";
	//public static String networkDataFile = "data/network";
	public static double network[][];// = new double[sizeNetwork][sizeNetwork];
	public static ArrayList<Tree> forrest;
	public static HashMap<Integer, Tree> forrestMap;
	public static double gamma = 0.4, alpha = 1.0, beta = 0.6, delta = 0.2, lambda = 0.4;
	public static int tID;
	public static PriorityQueue<Tree> heap;
	public static Tree finalTree;
	public static ArrayList<Query> queryList;

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		int stage = 2;// the stages decide whether we want to run the first part of the code or just the second part of the code
		
		if(stage == 1)
		{
			tID = 1;
			forrest = new ArrayList<Tree>();
			forrestMap = new HashMap<Integer, Tree>();
			Comparator<Tree> comparator = new NodeComparator();
			loadQueryList();
			heap = new PriorityQueue<Tree>(sizeNetwork, comparator);
			importNetworkData();//this also populates the forrest
			initializeForrest();
			populateInitialHeap();
			findHierarchicalCommunities();
			System.out.println("Done with FindHierComm... heap size: "+heap.size());
			saveFinalTree(finalTree);
		}
		else
		{
			loadFinalTree();
			printFirstLevelOfFinalTree(finalTree, 0);
		}
		
		
		//printHierarchicalTree(finalTree);
	}
	
	public static void loadFinalTree() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream("data/finalTreeObtainedbyBHCD");
        ObjectInputStream ois = new ObjectInputStream(fis);
        finalTree = (Tree) ois.readObject();
        ois.close();
	}
	
	public static void saveFinalTree(Tree t) throws IOException
	{
		FileOutputStream fos = new FileOutputStream("data/finalTreeObtainedbyBHCD");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(t);
		fos.close();
	}
	
	public static void printFirstLevelOfFinalTree(Tree t, int depth)
	{
		if(depth == 4) return;
		//System.out.println("inside print1stlevel: t: "+t.nodeList.size()+"_"+t.nChildren);
		//System.out.println("Inside printFirstLevel: childTree size= "+t.childTrees.size());
		Iterator<Tree> itr = t.childTrees.iterator();
		Queue<Tree> q = new LinkedList<Tree>();
		int maxDepth = 4;
		//System.out.println("----------------------------");
		System.out.println();
		while(itr.hasNext())
		{
			Tree tt = itr.next();
			tt.populateQueryWords();
			for(int j =0;j<depth;j++) System.out.print("\t");
			System.out.println("Tree: "+tt.treeID+"; n1:"+tt.n1+" n0:"+tt.n0+" "+tt.nChildren+" with no of nodes "+tt.nodeList.size()+"__"+tt.entriesSortedByValues(tt.querywords));
			printFirstLevelOfFinalTree(tt, depth+1);
			/*Iterator<Tree> itt = tt.childTrees.iterator();
			while(itt.hasNext())
			{
				Tree tt1 = itt.next();
				System.out.println("\t"+tt1.nChildren+" with no of nodes "+tt1.nodeList.size());
				Iterator<Tree> itt2 = tt1.childTrees.iterator();
				while(itt2.hasNext())
				{
					Tree tt2 = itt2.next();
					System.out.println("\t\t"+tt2.nChildren+" with no of nodes "+tt2.nodeList.size());
				}
			}*/
			q.add(tt);
		}
		System.out.println();
	}
	
	@SuppressWarnings({ "unchecked", "unchecked" })
	public static void loadQueryList() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream("data/queriesLISTFromAOL_Gold");
        ObjectInputStream ois = new ObjectInputStream(fis);
        queryList = (ArrayList<Query>) ois.readObject();
        ois.close();
        System.out.println("Loaded queryList with: "+queryList.size()+" queries");
        sizeNetwork = queryList.size();
        network = new double[sizeNetwork][sizeNetwork];
	}
	
	
	public static void printHierarchicalTree(Tree t)
	{
		Iterator<Tree> itr = t.childTrees.iterator();
		while(itr.hasNext())
		{
			Tree tt = itr.next();
			Iterator<Node> it = tt.nodeList.iterator();
			System.out.print("Tree: "+tt.treeID+"___ ");
			while(it.hasNext())
			{
				Node n = it.next();
				System.out.print((n.nodeID+1)+" ");
			}
			System.out.println();
			printHierarchicalTree(tt);
		}
		System.out.println();
	}
	
	public static void findHierarchicalCommunities()
	{
		while(heap.size() > 0)
		{
			Tree I = heap.poll();
			Tree X = I.getX();
			Tree Y = I.getY();
			if(forrestMap.containsKey(new Integer(X.treeID)) && forrestMap.containsKey(new Integer(Y.treeID)))
			{
				forrest.remove(X);forrest.remove(Y);// TODO: checkthe validity, if incorrect, iterate & remove
				forrestMap.remove(X.treeID);forrestMap.remove(Y.treeID);
				forrest.add(I);
				forrestMap.put(new Integer(I.treeID), I);
				
				// now for each tree in the forrest, we have to find potential merges with the current tree t
				Iterator<Tree> itr = forrestMap.values().iterator();
				while(itr.hasNext())
				{
					Tree J = itr.next();
					if(J.treeID == I.treeID) continue;
					// compute sigmaMM, where M is the possible merges of I & J
					// now there are 3 possibilities to merge I & J, we calculate bayesFactorScore for each
					// possibility and chose the one which has the maximum score
					Tree M = mergeTrees(I, J);
					heap.add(M);
					System.out.println("Tree added to the heap: L="+M.likelihood+" S="+M.bayesFactorScore+" X="+M.getX().treeID+" Y="+M.getY().treeID+" noNodes: "+M.nodeList.size());
					System.out.println("Current Heap Size: "+heap.size()+" no of nodes in M ryt now: "+M.nodeList.size()+"__nChildren: "+M.nChildren+" sizeNetwork: "+sizeNetwork);
					if(M.nodeList.size() == sizeNetwork || M.nodeList.size() == 866)
					{
						finalTree = M;
						System.out.println("Final tree -- "+M.childTrees.size()+"_"+M.nChildren);
					}
				}
			}
			//else do nothing, the element has already been popped out from the PriorityQueue
		}
		System.out.println("Inside FindHierCom function, heap size: "+heap.size());
	}
	
	public static Tree mergeTrees(Tree I, Tree J)
	{
		Tree M = new Tree(tID++);
		M.isMerge = true;
		M.isInitialMerge = false;
		//sigmaIJ
		int n1IJ = getN1ij(I, J);
		int n0IJ = getN0ij(I, J);
		//sigmaMM
		int n1MM = I.n1 + J.n1 + n1IJ;
		int n0MM = I.n0 + J.n0 + n0IJ;
		// sigmaCH
		int n1CH1 = n1IJ;
		int n0CH1 = n0IJ;
		int n1CH2 = I.n1CH + n1IJ;
		int n0CH2 = I.n0CH + n0IJ;
		int n1CH3 = J.n1CH + n1IJ;
		int n0CH3 = J.n0CH + n0IJ;

		// now calculate the numerator of the likelihood & see which of the 3 give max
		// even in the numerator just the 2nd part will matter: g(sigma-CH)*likelihood_of_each_child
		
		// case 1:
		double num1 = 0.0;
		num1 = getGValue(n1CH1, n0CH1)*I.likelihood*J.likelihood;
		
		// case 2:
		double num2 = 0.0;
		double g2 = getGValue(n1CH2, n0CH2);
		double prodChildren2 = 1;
		Iterator<Tree> itr2 = I.childTrees.iterator();
		while(itr2.hasNext())
		{
			Tree tt = itr2.next();
			prodChildren2 *= tt.likelihood;
		}
		// but what if I was just a initialTree? it wont have any children trees...
		if(I.nChildren != I.childTrees.size()) System.out.println("ERRROOORRRRRRRR here");
		if(I.nChildren == 0) prodChildren2 = I.likelihood;
		num2 = g2 * prodChildren2 * J.likelihood;
		
		// case 3:
		double num3 = 0.0;
		double g3 = getGValue(n1CH3, n0CH3);
		double prodChildren3 = 1;
		Iterator<Tree> itr3 = J.childTrees.iterator();
		while(itr3.hasNext())
		{
			Tree tt = itr3.next();
			prodChildren3 *= tt.likelihood;
		}
		// but what if J was just a initialTree? it wont have any children trees...
		if(J.nChildren != J.childTrees.size()) System.out.println("ERRROOORRRRRRRR here");
		if(J.nChildren == 0) prodChildren3 = J.likelihood;
		num3 = g3 * prodChildren3 * I.likelihood;
		
		// now we have all three possibilities, we see which yields the max num
		System.out.println("num1: "+num1+" num2: "+num2+" num3: "+num3);
		if(num1 >= num2 &&  num1 >= num3)
		{
			M.setX(I); M.setY(J);
			M.n1CH = n1CH1; M.n0CH = n0CH1;
			M.n1 = n1MM; M.n0 = n0MM;
			M.addChildTree(I); M.addChildTree(J);// this should update the nodeList of M
			double pi = 1-((1-gamma)*(1-gamma));
			double f = getFValue(M.n1,M.n0);
			M.gXY = getGValue(n1IJ, n0IJ);
			double likelihood = (pi*f) + ((1-pi)*num1);
			M.likelihood = likelihood;
			M.bayesFactorScore = getBayesFactorScoreForTree(M);
		}
		else if(num2 >= num1 &&  num2 >= num3)
		{
			//J is absorebed into I, so we can neglect M and instead add J as a child of I
			M.setX(I); M.setY(J);
			M.n1CH = n1CH2; M.n0CH = n0CH2;
			M.n1 = n1MM; M.n0 = n0MM;
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			//make changes in the children: 1) add all of Is children as M's and 2) add J as M's child
			Iterator<Tree> itr4 = I.childTrees.iterator();
			while(itr4.hasNext())
			{
				Tree t4 = itr4.next();
				M.addChildTree(t4);
			}
			M.addChildTree(J);
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			double pi = 1-((1-gamma)*(1-gamma));
			double f = getFValue(M.n1,M.n0);
			M.gXY = getGValue(n1IJ, n0IJ);
			double likelihood = (pi*f) + ((1-pi)*num2);
			M.likelihood = likelihood;
			M.bayesFactorScore = getBayesFactorScoreForTree(M);
		}
		else// if (num3 >= num1 &&  num3 >= num2)
		{
			M.setX(I); M.setY(J);
			M.n1CH = n1CH3; M.n0CH = n0CH3;
			M.n1 = n1MM; M.n0 = n0MM;
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			//make changes in the children: 1) add all of Is children as M's and 2) add J as M's child
			Iterator<Tree> itr4 = J.childTrees.iterator();
			while(itr4.hasNext())
			{
				Tree t4 = itr4.next();
				M.addChildTree(t4);
			}
			M.addChildTree(I);
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			double pi = 1-((1-gamma)*(1-gamma));
			double f = getFValue(M.n1,M.n0);
			M.gXY = getGValue(n1IJ, n0IJ);
			double likelihood = (pi*f) + ((1-pi)*num3);
			M.likelihood = likelihood;
			M.bayesFactorScore = getBayesFactorScoreForTree(M);
		}
		return M;
	}
	
	public static void populateInitialHeap()
	{
		System.out.println("Starting the population of the initial heap...");
		for(int i=0;i<forrest.size();i++)
		{
			Tree ti = forrest.get(i);
			for(int j=i+1;j<forrest.size();j++)
			{
				Tree tj = forrest.get(j);
				if(ti.treeID == tj.treeID) continue;
				// now that we have the two tree candidates, we Merge (JOIN) them, coz other 2 merges are same
				Tree m = new Tree(tID++);
				m.isMerge = true;
				m.isInitialMerge = true;
				m.addChildTree(ti);
				m.addChildTree(tj);
				m.setX(ti);
				m.setY(tj);
				//get sigmaMM (= sigmaSelf) now
				m.likelihood = getTreeLikelihood(m);
				// now we have the Pm (likelihood) for this merged tree...
				// next we need to compute the score for this tree
				m.bayesFactorScore = getBayesFactorScoreForTree(m);
				// now add this tree to the PriorityQueue
				heap.add(m);
				//System.out.println("Tree added to the heap: L="+m.likelihood+" S="+m.bayesFactorScore+" X="+m.getX().treeID+" Y="+m.getY().treeID);
			}
		}
		System.out.println("Done with Heap Initialization; "+heap.size()+" trees added to the heap.\n");
		//System.exit(0);
	}
	
	public static double getBayesFactorScoreForTree(Tree t)
	{
		double bfs = 0.0, num=0.0, den=1; // TODO: den was initially initialized to 0, mae it to 1 on 9th Sept, see if its wrong
		num = t.likelihood;
		den = t.getX().likelihood * t.getY().likelihood * t.gXY;
		if(den == 0) den = 1;
		bfs = num/den;
		return bfs;
	}
	
	public static void initializeForrest()
	{
		System.out.println("Initializing forrest now...");
		Iterator<Tree> itr = forrest.iterator();
		while(itr.hasNext())
		{
			Tree t = itr.next();
			double likelihood = getTreeLikelihood(t);
			t.likelihood = likelihood;
			//System.out.println("Tree "+t.treeID+" initialzed with likelihood: "+t.likelihood);
		}
		System.out.println("Done with Forest Initialization");
	}
	
	public static double getTreeLikelihood(Tree t)
	{
		double likelihood = 0.0;
		if(t.isLeaf)
		{
			int n1 = getN1self(t);
			int n0 = getN0self(t);
			double f = getFValue(n1, n0);
			double pi = 1.0;
			likelihood = pi*f;
			return likelihood;
		}
		else // if its not a leaf, then its a merge tree,
		{
			/*
			** the selfN1 and selfN0 values would have already been updated for this tree
			*/
			t.n1 = getN1self(t);// we can use these coz the nodeList of m is updated when we addChildTree 
			t.n0 = getN0self(t);
			if(t.isInitialMerge)
			{
				// get sigmaIJ now
				int n1ij = getN1ij(t.childTrees.get(0), t.childTrees.get(1));
				int n0ij = getN0ij(t.childTrees.get(0), t.childTrees.get(1));
				// since this is the 1st kind of merge(=JOIN), sigma-CH = sigmaIJ as each child is just 1 node
				//by first pat of Eq 13
				t.n1CH = n1ij;
				t.n0CH = n0ij;
				
				// calculating the likelihood now, eq 10
				double pi = 1-((1-gamma)*(1-gamma));
				double f = getFValue(t.n1, t.n0);
				double g = getGValue(n1ij, n0ij);
				
				t.gXY = g;
				//System.out.println(pi+" x "+f+"    +  ("+1+"-"+pi+" )* "+g+" * "+t.childTrees.get(0).likelihood+" * "+t.childTrees.get(1).likelihood);
				likelihood = (pi*f)  +  ((1-pi)*g*t.childTrees.get(0).likelihood*t.childTrees.get(1).likelihood);
				return likelihood;
			}
			else
			{
				// this part is handled inside the merge function...
			}
		}
		return likelihood;
	}
	
	public static int getN0ij(Tree ti, Tree tj)
	{
		int n0 = 0;
		ArrayList<Node> nodesI = ti.nodeList;
		ArrayList<Node> nodesJ = tj.nodeList;
		for(int i =0;i<nodesI.size();i++)
		{
			Node ni = nodesI.get(i);
			for(int j=0;j<nodesJ.size();j++)
			{
				Node nj =nodesJ.get(j);
				if(ni.nodeID == nj.nodeID) System.out.println("~~~~~~~~~~~~~ERROR~~~~~~~~~~~~~~~");
				if(network[ni.nodeID][nj.nodeID] == 0) n0++;
			}
		}
		return n0;
	}
		
	public static int getN1ij(Tree ti, Tree tj)
	{
		int n1 = 0;
		ArrayList<Node> nodesI = ti.nodeList;
		ArrayList<Node> nodesJ = tj.nodeList;
		for(int i =0;i<nodesI.size();i++)
		{
			Node ni = nodesI.get(i);
			for(int j=0;j<nodesJ.size();j++)
			{
				Node nj =nodesJ.get(j);
				if(ni.nodeID == nj.nodeID) System.out.println("~~~~~~~~~~~~~ERROR~~~~~~~~~~~~~~~~~~");
				n1 += network[ni.nodeID][nj.nodeID];
			}
		}
		//System.out.println("N1ij for trees "+ti.treeID+" & "+tj.treeID+" = "+n1);
		return n1;
	}
	
	public static int getN1self(Tree t)
	{
		int n1 = 0;
		ArrayList<Node> nodes = t.nodeList;
		for(int i =0;i<nodes.size();i++)
		{
			Node ni = nodes.get(i);
			for(int j=i+1;j<nodes.size();j++)
			{
				Node nj =nodes.get(j);
				n1 += network[ni.nodeID][nj.nodeID];
			}
		}
		//System.out.println("N1self for tree "+t.treeID+" = "+n1);
		return n1;
	}
	
	public static int getN0self(Tree t)
	{
		int n0 = 0;
		ArrayList<Node> nodes = t.nodeList;
		for(int i =0;i<nodes.size();i++)
		{
			Node ni = nodes.get(i);
			for(int j=i+1;j<nodes.size();j++)
			{
				Node nj =nodes.get(j);
				if(network[ni.nodeID][nj.nodeID] == 0) n0++;
			}
		}
		return n0;
	}
	
	public static double getFValue(int n1, int n0)
	{
		n1+=n1;
		double f = 0.0, num=0.0, den=0.0;
		num = StatUtility.beta((double)(alpha+n1), (double)(beta+n0));
		den = StatUtility.beta(alpha, beta);
		f = (double) (num/den);
		//System.out.println("F function value for "+n1+" & "+n0+" = "+f);
		return f;
	}
	
	public static double getGValue(int n1, int n0)
	{
		double g = 0.0, num=0.0, den=0.0;
		num = StatUtility.beta((double)(delta+n1), (double)(lambda+n0));
		den = StatUtility.beta(delta, lambda);
		g = (double) (num/den);
		//System.out.println("G function value for "+n1+" & "+n0+" = "+g);
		return g;
	}
	
	public static void importNetworkData() throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(networkDataFile));
		String line = br.readLine();
		int count = 0;
		while(line!=null)
		{
			String[] split = line.split("\t");
			if(split.length != sizeNetwork) System.out.println("Errorrr in here, size of network mismatch with network file "+split.length+"_"+sizeNetwork+"__at__"+count);
			for(int i=0;i<sizeNetwork;i++)
			{
				int n = Integer.parseInt(split[i]);
				network[count][i] = n;
			}
			Node n = new Node(count);
			n.setQ(queryList.get(count));
			count++;
			Tree t = new Tree(tID++);
			t.addNode(n);
			t.isLeaf = true;
			forrest.add(t);
			forrestMap.put(new Integer(t.treeID), t);
			line= br.readLine();
		}
		System.out.println("Forrest created with "+forrest.size()+" trees");
		System.out.println("Checking if network built is correct:");
		printNetwork();
	}
	
	public static void printNetwork()
	{
		for(int i=0;i<sizeNetwork;i++)
		{
			for(int j=0;j<sizeNetwork;j++)
			{
				System.out.print(network[i][j]+"\t");
			}
			System.out.println();
		}
	}
}