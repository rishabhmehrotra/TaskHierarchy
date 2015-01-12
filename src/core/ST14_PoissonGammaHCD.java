package core;

import java.io.*;
import java.util.*;

import datastr.Document;
import datastr.Node;
import datastr.Query;
import datastr.Tree;
import experiments.ComputePairwiseFRPScoresOnAOL;

// this class modified the existing BHCD algo & incorporates the Poisson-Gamma conjugate distribution model
public class ST14_PoissonGammaHCD {
	
	public static int sizeNetwork;
	public static String networkDataFile = "data/qNetwork";
	//public static String networkDataFile = "data/network";
	public static double network[][];// = new double[sizeNetwork][sizeNetwork];
	public static ArrayList<Tree> forrest;
	public static HashMap<Integer, Tree> forrestMap;
	public static double gamma = 0.4, alpha1 =9, alpha2=9, alpha3=9, beta1=0.33, beta2=0.33, beta3=0.33;
	public static int tID;
	public static PriorityQueue<Tree> heap;
	public static Tree finalTree;
	public static ArrayList<Query> queryList;
	public static HashMap<String, String> stopWords;
	
	public static int pruning = 2;// 0 no pruning, 1- all pruning, 2- just the tree merge based on size pruning

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		// the stages decide whether we want to run the first part of the code or just the second part of the code
		int stage = 1;
		
		if(stage == 1)
		{
			populateStopWordsHashMap();
			System.out.println("Starting BHCD via Poisson_Gamma Method...");
			tID = 1;
			forrest = new ArrayList<Tree>();
			forrestMap = new HashMap<Integer, Tree>();
			Comparator<Tree> comparator = new NodeComparator();
			loadQueryList();
			heap = new PriorityQueue<Tree>(sizeNetwork, comparator);
			importNetworkData(); //this also populates the forest
			initializeForrest();
			populateInitialHeap();// NOTE: all the pairs of tree merges so far are just in the heap, not added to forest, coz not all will be added to the forrest
			findHierarchicalCommunities();
			System.out.println("Done with FindHierComm... heap size: "+heap.size());
			saveFinalTree(finalTree);
		}
		else
		{
			loadFinalTree();
			System.out.println("Loaded tree: "+finalTree.nChildren);
			new ComputePairwiseFRPScoresOnAOL(finalTree);
			printTree();
		}
		
		
		//printHierarchicalTree(finalTree);
	}
	
	public static void printTree() throws IOException
	{
		FileWriter fstream = new FileWriter("data/hierarchyDEMO_demoST");
		BufferedWriter out = new BufferedWriter(fstream);
		printFirstLevelOfFinalTree(finalTree, 0, out);
		out.close();
	}
	
	@SuppressWarnings({ "unchecked", "unchecked" })
	public static void loadQueryList() throws IOException, ClassNotFoundException
	{
		//FileInputStream fis = new FileInputStream("data/sessionTrack2014_queryList");
		FileInputStream fis = new FileInputStream("data/common_AOLST_QueryList");
        ObjectInputStream ois = new ObjectInputStream(fis);
        queryList = (ArrayList<Query>) ois.readObject();
        ois.close();
        System.out.println("Loaded queryList with: "+queryList.size()+" queries");
        sizeNetwork = queryList.size();
        network = new double[sizeNetwork][sizeNetwork];
	}
	
	public static void importNetworkData() throws IOException
	{
		/*BufferedReader br = new BufferedReader(new FileReader(networkDataFile));
		String line = br.readLine();*/
		int count = 0;
		while(count < sizeNetwork)
		{
			/*String[] split = line.split("\t");
			if(split.length != sizeNetwork) System.out.println("Errorrr in here, size of network mismatch with network file "+split.length+"_"+sizeNetwork+"__at__"+count);
			for(int i=0;i<sizeNetwork;i++)
			{
				int n = Integer.parseInt(split[i]);
				network[count][i] = n;
			}*/
			Node n = new Node(count);
			n.setQ(queryList.get(count));
			count++;
			Tree t = new Tree(tID++);
			t.addNode(n);
			t.isLeaf = true;
			forrest.add(t);
			forrestMap.put(new Integer(t.treeID), t);
			//line= br.readLine();
		}
		System.out.println("Forest created with "+forrest.size()+" trees");
		System.out.println("Checking if network built is correct:");
		//printNetwork();
	}
	
	public static void initializeForrest()//as of now, the trees in this forrest are just single node trees, this case is handled in the getlikelihood function
	{
		System.out.println("Initializing forrest now...");
		Iterator<Tree> itr = forrest.iterator();
		while(itr.hasNext())
		{
			Tree t = itr.next();
			double likelihood = getTreeLikelihood(t);
			t.likelihood = likelihood;
			System.out.println("Tree "+t.treeID+" initialzed with likelihood: "+t.likelihood);
		}
		System.out.println("\n\nDone with Forest Initialization\n\n");
		//System.exit(0);
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
				m.likelihood = getTreeLikelihood(m);// as of now, the tree m is a dual-child tree, the likelihood calculation of this case is handled in the getlikelihood function 
				if(pruning == 2)
					if(m.likelihood == -1)
					{
						//System.out.println("Skipped inside initial heap population");
						continue;
					}
				// now we have the Pm (likelihood) for this merged tree...
				// next we need to compute the score for this tree
				m.bayesFactorScore = getBayesFactorScoreForTree(m);
				// now add this tree to the PriorityQueue
				heap.add(m);
				//System.out.println("Tree added to the heap: L="+m.likelihood+" S="+m.bayesFactorScore+" X="+m.getX().treeID+" Y="+m.getY().treeID);
			}
			System.out.println("Populating Initial heap...current i:"+i+" / "+forrest.size()+"___heapSize: "+heap.size());
			//if(i==100) break;
		}
		System.out.println("Done with Heap Initialization; "+heap.size()+" trees added to the heap.\n");
		//System.exit(0);
	}
	
	public static void findHierarchicalCommunities()
	{
		// as of now we have populated the forrest and then for all pairs of trees in the forrest we have populated the corresponding merged trees in the heap/priorityQueue
		int temp1=0;
		int maxTreeSize = 0;
		while(heap.size() > 0)
		{
			Tree I = heap.poll();
			Tree X = I.getX();
			Tree Y = I.getY();
			// if both of these trees X & Y arent there in forrest then that means they're already merged with some tree and we just remove this tree from the heap and proceed
			if(forrestMap.containsKey(new Integer(X.treeID)) && forrestMap.containsKey(new Integer(Y.treeID)))
			{
				System.out.println("Popped from HEAP: nChildren= "+I.nChildren+"\tno of nodes: "+I.nodeList.size()+"\tlikelihood: "+I.likelihood);
				forrest.remove(X);forrest.remove(Y);// TODO: checkthe validity, if incorrect, iterate & remove
				forrestMap.remove(X.treeID);forrestMap.remove(Y.treeID);
				forrest.add(I);
				forrestMap.put(new Integer(I.treeID), I);
				
				// now for each tree in the forrest, we have to find potential merges with the current tree t
				Iterator<Tree> itr = forrestMap.values().iterator();
				int temp=0;
				while(itr.hasNext())
				{
					Tree J = itr.next();
					if(J.treeID == I.treeID) continue;
					// compute sigmaMM, where M is the possible merges of I & J
					// now there are 3 possibilities to merge I & J, we calculate bayesFactorScore for each
					// possibility and chose the one which has the maximum score -- all this is done inside mergeTrees
					if(pruning == 2)
					{
						//int n1 = I.nChildren;
						//int n2 = J.nChildren;
						int n1 = I.nodeList.size();
						int n2 = J.nodeList.size();
						//if(n1>n2 && n1-n2>30) {System.out.println("----------------------SKIPPED -2");continue;}
						//if(n2>n1 && n2-n1>30) {System.out.println("----------------------SKIPPED -2");continue;}
						if(n1 > n2 && n1>20)
						{
							if(n2 < 0.25*n1) {System.out.println("----------------------SKIPPED -2");continue;}
							if(n1 > 1.75*n2) {System.out.println("----------------------SKIPPED -2");continue;}
						}
						else if(n2 > n1 && n2>20)
						{
							if(n1 < 0.25*n2) {System.out.println("----------------------SKIPPED -2");continue;}
							if(n2 > 1.75*n1) {System.out.println("----------------------SKIPPED -2");continue;}
						}
					}
					Tree M = mergeTrees(I, J);
					heap.add(M);
					if(M.nodeList.size() > maxTreeSize) {maxTreeSize = M.nodeList.size();finalTree = M;}
					//System.out.println("Tree added to the heap: L="+M.likelihood+" S="+M.bayesFactorScore+" X="+M.getX().treeID+" Y="+M.getY().treeID+" noNodes: "+M.nodeList.size());
					//System.out.println("Current Heap Size: "+heap.size()+" no of nodes in M ryt now: "+M.nodeList.size()+"__nChildren: "+M.nChildren+" sizeNetwork: "+sizeNetwork);
					temp = M.nodeList.size();
					if(M.nodeList.size() == sizeNetwork || M.nodeList.size() == 866)
					{
						finalTree = M;
						System.out.println("Final tree -- "+M.childTrees.size()+"_"+M.nChildren);
					}
				}
				//System.out.println("Current Heap Size: "+heap.size()+" no of nodes in M ryt now: "+temp+" sizeNetwork: "+sizeNetwork);
				System.out.println("Current Heap Size: "+heap.size()+"\t Forrest Size:"+forrest.size()+"\tno of nodes in I ryt now: "+I.nodeList.size()+"__nChildren: "+I.nChildren+" sizeNetwork: "+sizeNetwork);
			}
			//else do nothing, the element has already been popped out from the PriorityQueue
		}
		temp1++;
		if(temp1%1000 == 0) System.out.println("Inside FindHierCom function, heap size: "+heap.size());
		System.out.println("The maximum tree size seen during the hierarchy building: "+maxTreeSize);
	}
	
	public static Tree mergeTrees(Tree I, Tree J)
	{
		// now to merge two trees, we have three cases of which we will merge in the way which gives us the maximum bayes factor score
		// so we'll calculate the bayes factor score for al three merge tree possibilities and chose the one which has max score
		Tree M = new Tree(tID++);
		M.isMerge = true;
		M.isInitialMerge = false;

		// now calculate the numerator of the likelihood & see which of the 3 give max
		// even in the numerator just the 2nd part will matter: g(sigma-CH)*likelihood_of_each_child
		
		// case 1: JOIN I & J
		double num1 = 0.0;
		num1 = I.likelihood*J.likelihood;
		
		// case 2: J gets ABSORBED into I
		double num2 = 0.0;
		//double g2 = getGValue(n1CH2, n0CH2);
		double prodChildren2 = 1d;
		Iterator<Tree> itr2 = I.childTrees.iterator();
		while(itr2.hasNext())
		{
			Tree tt = itr2.next();
			prodChildren2 *= tt.likelihood;
		}
		// but what if I was just a initialTree? it wont have any children trees...
		if(I.nChildren != I.childTrees.size()) System.out.println("ERRROOORRRRRRRR here");
		if(I.nChildren == 0) prodChildren2 = I.likelihood;
		num2 = prodChildren2 * J.likelihood;
		
		// case 3: I gets ABSORBED into J
		double num3 = 0.0;
		//double g3 = getGValue(n1CH3, n0CH3);
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
		num3 = prodChildren3 * I.likelihood;
	
		
		
		
		// now we have all three possibilities, we see which yields the max num
		//System.out.println("num1: "+num1+" num2: "+num2+" num3: "+num3);
		if(num1 >= num2 &&  num1 >= num3)
		{
			// case: JOIN I & J
			M.setX(I); M.setY(J);
			M.addChildTree(I); M.addChildTree(J);// this should update the nodeList of M
			// so now we have M with its updates list of nodes/queries/nodeList
			double pi = 1-((1-gamma)*(1-gamma));
			double fDm = get_fDmValue(M);
			double likelihood = (pi*fDm) + ((1-pi)*num1);// coz num1 is as it is the multiplication of the likelihood of the two child trees
			M.likelihood = likelihood;
			M.bayesFactorScore = getBayesFactorScoreForTree(M);
		}
		else if(num2 >= num1 &&  num2 >= num3)
		{
			//J is absorebed into I, so we can neglect M and instead add J as a child of I --Update-- NOT SURE IF ITS RIGHT!
			M.setX(I); M.setY(J);
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			//make changes in the children: 1) add all of Is children as M's and 2) add J as M's child
			Iterator<Tree> itr4 = I.childTrees.iterator();
			while(itr4.hasNext())
			{
				Tree t4 = itr4.next();
				M.addChildTree(t4);
			}
			M.addChildTree(J); // NOTE: addChildren function will update the nodeList accordingly
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			double pi = 1-((1-gamma)*(1-gamma));
			double fDm = get_fDmValue(M);
			double likelihood = (pi*fDm) + ((1-pi)*num2);
			M.likelihood = likelihood;
			M.bayesFactorScore = getBayesFactorScoreForTree(M);
		}
		else// if (num3 >= num1 &&  num3 >= num2)
		{
			M.setX(I); M.setY(J);
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
			double fDm = get_fDmValue(M);
			double likelihood = (pi*fDm) + ((1-pi)*num3);
			M.likelihood = likelihood;
			M.bayesFactorScore = getBayesFactorScoreForTree(M);
		}
		return M;
	}
	
	public static double get_fDmValue(Tree t)
	{
		// this function assumes the tree we are getting has updated nodeList
		// we will need to iterate over all query pairs and calculate the probability of the q-q pairs based on all the 3 relations
		// and product it over all the query pairs
		double result = 1d;//1 coz we have to multiply
		int size = t.nodeList.size();
		int count=0, nn=0;
		int R1=0, R2=0, R3=0;
		for(int i=0;i<size;i++)
		{
			Node n1 = t.nodeList.get(i);
			Query q1 = n1.q;
			
			for(int j=i+1;j<size;j++)
			{
				Node n2 = t.nodeList.get(j);
				Query q2 = n2.q;
				// now we have two queries, we need to calculate the probability of a link between these queries based on the multi-relational Poisson-Gamma model
				int r1 = getR1(q1, q2);
				int r2 = getR2(q1, q2);
				int r3 = getR3(q1, q2);
				R1+=r1;
				R2+=r2;
				R3+=r3;
				nn++;
				count += (r1+r2+r3);
				//System.out.println("r1/2/3="+r1+"_"+r2+"_"+r3);
				// now we have the r values, we 
				//double affinity1 = getGammaPosterior(alpha1, beta1, r1);
				//double affinity2 = getGammaPosterior(alpha2, beta2, r2);
				//double affinity3 = getGammaPosterior(alpha3, beta3, r3);
				// now we have the affinities from each of the 3 relations, we need to calculate the likelihood of this tree (= with just two nodes)
				//double temp = affinity1*affinity2*affinity3;
				//count++;
				//if(temp>0) result*=temp;
			}
		}
		
		R1 = R1*5/nn;
		R2 = R2*5/nn;
		R3 = R3*5/nn;
		//System.out.println("R1/2/3: "+R1+"_"+R2+"_"+R3+" Size: "+size+" nn: "+nn);
		double affinity1 = getGammaPosterior(alpha1, beta1, R1);
		double affinity2 = getGammaPosterior(alpha2, beta2, R2);
		double affinity3 = getGammaPosterior(alpha3, beta3, R3);
		
		result = affinity1*affinity2*affinity3;
		//if(pruning==1) if(count==0) return -1;
		
		//return (result/count);//returning the average instead of just the result
		return result;
	}
	
	public static double getTreeLikelihood(Tree t)
	{
		double likelihood = 0.0;
		if(t.isLeaf)
		{
			int r1 = 0, r2=0, r3=0;// coz this is the leaf otherwise getRValue(query1, query2)
			double affinity1 = getGammaPosterior(alpha1, beta1, r1);
			double affinity2 = getGammaPosterior(alpha2, beta2, r2);
			double affinity3 = getGammaPosterior(alpha3, beta3, r3);
			likelihood = affinity1*affinity2*affinity3;
			//System.out.println("Inside getTreeLikelihood for a leaf: "+likelihood+" "+affinity1+" "+affinity2+" "+affinity3);
			//return likelihood;//we donot assign this likelihood to the tree here coz the function calling it would be doing it anyway
			return 1;
		}
		else // if its not a leaf, then its a merge tree,
		{
			if(t.isInitialMerge)
			{
				// since this is the initial merge, this tree would have just two children as X & Y
				Tree X = t.getX();
				Tree Y = t.getY();
				// now each of these trees would have just one child - one node
				ArrayList<Node> nlistX = X.nodeList;
				ArrayList<Node> nlistY = Y.nodeList;
				if(!(nlistX.size() == 1 && nlistY.size() == 1)) System.out.println("Something wrong...initial merge and size of each nodelist shoul be 1");
				Node nX = X.nodeList.get(0);
				Node nY = Y.nodeList.get(0);
				// now we have both the nodes, we need the r1, r2, r3 values form these two queries
				int r1 = getR1(nX.q, nY.q);
				int r3 = getR3(nX.q, nY.q);
				if(pruning == 2) if(r1+r3 == 0) return -1;
				int r2 = getR2(nX.q, nY.q);
				//System.out.println("r1/2/3="+r1+"_"+r2+"_"+r3);
				
				// now we have the r values, we 
				double affinity1 = getGammaPosterior(alpha1, beta1, r1);
				double affinity2 = getGammaPosterior(alpha2, beta2, r2);
				double affinity3 = getGammaPosterior(alpha3, beta3, r3);
				// now we have the affinities from each of the 3 relations, we need to calculate the likelihood of this tree (= with just two nodes)
				double fDm = affinity1*affinity2*affinity3;
				
				// calculating the likelihood now, eq 10
				double pi = 1-((1-gamma)*(1-gamma));//coz we have fixed no of 2 children in this case
				//System.out.println(pi+" x "+f+"    +  ("+1+"-"+pi+" )* "+g+" * "+t.childTrees.get(0).likelihood+" * "+t.childTrees.get(1).likelihood);
				likelihood = (pi*fDm)  +  ((1-pi)*t.childTrees.get(0).likelihood*t.childTrees.get(1).likelihood);//coz we have fixed no of 2 children in this case
				if(likelihood > 1000000) {System.out.println("--~~~~~"+likelihood+" r1/2/3="+r1+"_"+r2+"_"+r3);System.exit(0);}
				return likelihood;//we donot assign this likelihood to the tree here coz the function calling it would be doing it anyway
			}
			else
			{
				// this part is handled inside the merge function...
			}
		}
		return likelihood;
	}
	
	public static double getBayesFactorScoreForTree(Tree t)
	{
		// this function just calculates the bayesfactorscore when two node trees are merged not for other cases
		// the pther cases are hendled inside the mergeTrees function
		// NOTE: if making changes in BayesFactorScore calculation, then change this as well as the logic insid ethe mergeTrees function
		double bfs = 0.0, num=0.0, den=1; // TODO: den was initially initialized to 0, made it to 1 on 9th Sept, see if its wrong
		num = t.likelihood;
		//den = t.getX().likelihood * t.getY().likelihood * t.gXY;
		den = t.getX().likelihood * t.getY().likelihood;
		if(den == 0) den = 1;
		bfs = num/den;
		return bfs;
	}
	
	public static double getGammaPosterior(double alpha, double beta, int r)
	{
		// this function will implement the code for the part wherein we use the Poisson-Gamma conjugate model to calculate the affinity scores
		// for this particular value of r observed based on the priors alpha & beta
		double result = 0.0;
		double factor1, factor2, factor3;
		
		// factor 1:
		double num1 = StatUtility.gamma(alpha+r);
		double den1 = StatUtility.gamma(alpha)*StatUtility.factorial(r);
		factor1 = num1/den1;
		
		
		// factor 2:
		double temp = beta/(beta+1);
		factor2 = Math.pow(temp, alpha);
		
		// factor 3:
		temp = 1/(beta+1);
		factor3 = Math.pow(temp,r);
		
		result = factor1*factor2*factor3;
		//System.out.println("num1: "+num1+"__den1: +den1");
		//System.out.println("=================="+factor1+"__"+factor2+"__"+factor3);
		return result;
	}
	
	public static int getR1(Query q1, Query q2)
	{
		// QUERY based r
		// this function will implement the part where we have 2 queries and we need to calculate the R1 relational score between these queries
		// (1-editDistance)%; similarity score; Jaccard similarity score; topical similarity score; inverse of temporal distance (same user), 
		int result = 0;
		String s1 = q1.query, s2 = q2.query;
		
		// editDistance - re-scaled to 0-10
		int ed = editDistance(s1,s2);
		int length = Math.max(s1.length(), s2.length());
		int temp1 = (1 - ed/length)*10;
		
		// Similarity score - 2 X no of common words
		int temp2 = 3*findNumberOfCommonWords(s1,s2);
		
		// Jaccard Similarity
		int temp3 = 0;
		
		// topical Similarity
		int temp4 = 0;
		
		// inverse of temporal Distance - IFF SAME USER
		int temp5 = 0;
		/*long secs = (q1.getqTime().getTime() - q2.getqTime().getTime()) / 1000;
		int hours = (int) (secs / 3600);
		if(q1.userID.compareTo(q2.userID)==0)
		{
			if(hours<2) temp5 = 10; else if(hours>2 && hours<4) temp5 = 8; else if(hours>4 && hours<6) temp5 = 6; else if(hours>6 && hours<8) temp5 = 4; else if(hours>8 && hours<10) temp5 = 2; else temp5 = 0;
		}*/
		
		// final result = sum over all tempi's
		
		//result = temp1 + temp2 + temp3+ temp4 + temp5;
		result = temp2;
		if(result>=3) result = 1; else result = 0;
		return result;
	}
	
	public static int getR2(Query q1, Query q2)
	{
		// URL based r
		// this function will implement the part where we have 2 queries and we need to calculate the R2 relational score between these queries
		// common ODP categories, common URLS, max/min of edit distance between URLs, avg of edit distance between URLs, Jaccard index between URLs
		
		int result = 0;
		
		// common ODP categories
		int temp1 = 0;
		
		// common URLs +++ editDistance: min & avg
		int temp2 = 0, temp3 = 0, temp4 = 0;
		/* ignoring the URLs as of now, instead just focusing on the documents
		Iterator<String> itr = q1.urls.values().iterator();
		int min = 1000, minSize = 0, avg=0, count=0;
		while(itr.hasNext())
		{
			String url1 = itr.next();
			if(url1.length()<1) continue;
			if(q2.urls.containsKey(url1)) temp2++;
			
			Iterator<String> itr2 = q2.urls.values().iterator();
			while(itr2.hasNext())
			{
				String url2 = itr2.next();
				if(url2.length()<1) continue;
				int ed = editDistance(url1, url2);
				if(min>=ed)
				{
					min = ed;
					minSize = Math.max(url1.length(), url2.length());
					//System.out.println(min+"_"+minSize);
				}
				avg+= ed; count++;
			}
		}
		if(count == 0) {temp4 = 0;temp3=0;}
		else
		{
			//System.out.println("min:"+min+"_minsize:_"+minSize+" count:_"+count);
			avg = avg/count;
			temp4 = 10-avg;
			if(temp4<0) temp4=0;
			temp3 = (1 - min/minSize)*10;
		}
		*/
		
		
		// jaccard index between URLs
		int temp5 = 0;
		
		// snippets common words & title common words
		int temp6 = 0;
		/*
		int nSnippet = 0, nTitle = 0;
		Iterator<Document> itr1 = q1.documents.iterator();
		while(itr1.hasNext())
		{
			Document d1 = itr1.next();
			Iterator<Document> itr2 = q2.documents.iterator();
			while(itr2.hasNext())
			{
				Document d2 = itr2.next();
				// now find the common words between these two documents
				nSnippet += findNumberOfCommonWords(d1.snippet, d2.snippet);
				nTitle += findNumberOfCommonWords(d1.title, d2.title);
			}
		}
		int nSAvg = nSnippet/100;
		int nTAvg = nTitle/100;
		
		if(nSAvg+nTAvg > 20) temp6 = 10;
		else if(nSAvg+nTAvg > 10) temp6 = 7;
		else temp6 = nSAvg+nTAvg;
		*/
		Iterator<String> itr = q1.wordsInDocumentsSnippets.keySet().iterator();
		while(itr.hasNext())
		{
			String word = itr.next();
			if(stopWords.containsKey(word)) continue;
			if(q2.wordsInDocumentsSnippets.containsKey(word)) temp6++;
		}
		Iterator<String> itr1 = q1.wordsInDocumentsTitle.keySet().iterator();
		while(itr1.hasNext())
		{
			String word = itr1.next();
			if(stopWords.containsKey(word)) continue;
			if(q2.wordsInDocumentsTitle.containsKey(word)) temp6++;
		}
		//result = temp1 + temp2 + temp3 + temp4 + temp5;
		result = temp6/50;
		//System.out.println("--------------------------------------------------------"+result);
		if(result>3) result = 1; else result = 0;
		return result;
	}
	
	public static int getR3(Query q1, Query q2)
	{
		// SESSION based r
		// this function will implement the part where we have 2 queries and we need to calculate the R3 relational score between these queries
		int result = 0;
		/*
		// same user
		if(q1.userID.compareTo(q2.userID) == 0) result++;
		
		long secs = (q1.getqTime().getTime() - q2.getqTime().getTime()) / 1000;
		int hours = (int) (secs / 3600);
		// same user as well as same session
		if(hours<4 && (q1.userID.compareTo(q2.userID) == 0)) result++;
		*/
		if(q1.sessionID == q2.sessionID) result = 3;
		return result;
	}
	
	
	
	
	public static int findNumberOfCommonWords(String s1, String s2)
	{
		int result1 = 0;
		String parts1[] = s1.split(" ");
		for(int i=0;i<parts1.length;i++)
		{
			if(s2.contains(parts1[i])) result1++;
		}
		
		/*int result2 = 0;
		String parts2[] = s2.split(" ");
		for(int i=0;i<parts2.length;i++)
		{
			if(s1.contains(parts2[i])) result2++;
		}
		if(result1>result2)
			return result1;
		else
			return result2;
			*/
		return result1;
	}
	
	// code for Levenshtein distance -- from Wiki
	public static int editDistance(String a, String b)
	{
		a = a.toLowerCase();
		b = b.toLowerCase();
		// i == 0
		int [] costs = new int [b.length() + 1];
		for (int j = 0; j < costs.length; j++)
			costs[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			// j == 0; nw = lev(i - 1, j)
			costs[0] = i;
			int nw = i - 1;
			for (int j = 1; j <= b.length(); j++) {
				int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
				nw = costs[j];
				costs[j] = cj;
			}
		}
		return costs[b.length()];
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
	
	public static void printFirstLevelOfFinalTree(Tree t, int depth, BufferedWriter out) throws IOException
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
			System.out.println("Tree: "+tt.treeID+"; with no of children "+tt.nChildren+" with no of nodes "+tt.nodeList.size()+"__"+tt.entriesSortedByValues(tt.querywords));
			//out.write("\n");
			String temp = "";
			for(int i=0;i<depth;i++) temp+= "\t"; 
			for(int i=0;i<tt.nodeList.size();i++)
			{
				out.write(temp+tt.nodeList.get(i).q.query);
				out.write("\n");
			}
			out.write("\n====================\n");
			printFirstLevelOfFinalTree(tt, depth+1, out);
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
	
	public static void loadFinalTree() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream("data/finalTreeObtainedbyBHCD_SessionTrack2014_full");
        ObjectInputStream ois = new ObjectInputStream(fis);
        finalTree = (Tree) ois.readObject();
        ois.close();
	}
	
	public static void saveFinalTree(Tree t) throws IOException
	{
		FileOutputStream fos = new FileOutputStream("data/finalTreeObtainedbyBHCD_SessionTrack2014_full");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(t);
		fos.close();
	}
	
	public static void populateStopWordsHashMap() throws IOException
	{
		stopWords = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader("data/stoplists/en.txt"));
		String line = br.readLine();
		while(line!=null)
		{
			if(!stopWords.containsKey(line)) stopWords.put(line, line);
			line = br.readLine();
		}
		System.out.println("populated stopwords hashmap with:"+stopWords.size()+"English stopwords");
	}
}