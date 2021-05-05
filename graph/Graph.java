package graph;
import ij.*;
import java.util.*;

/**
  * Given a symmetric adjacency matrix, this class will convert the matrix
  * into a set of one or more adjacency lists using a Breadth First Search 
  * (BFS) and Depth First Search (DFS) algorithms.  I also found the 
  * FloodFill description (Chapter 11) in Burger and Burge very helpful.
  * I used many online sources for learning how to construct these. 
  * A very good open-list/closed-list explanation can be found here...
  * http://www.kirupa.com/developer/actionscript/depth_breadth_search.htm
  *
  * An example 13x13 adjacency matrix is included which get converted into 
  * 4 adjacency lists as shown here...
  * Number of connected components = 4
  * 0: 1 3 5 6 8 9 11 13 
  * 1: 2 4 
  * 2: 7 
  * 3: 10 12 
  *
  * Note that the nodes are labeled 1,2,3,...
  * Also note that the diagonal is 0 but it could be populated with 1,2,3,...  
  * 
  * 2010-04-30 Ben Tupper, written
  **/
  
public class Graph extends Vector<Subgraph>{
   //an example that I used to for development
   // public int[][] exampleMap = {
   // {0,   0,   3,   0,   5,   0,   0,   0,   0,   0,   0,   0,   0}, 
   // {0,   0,   0,   4,   0,   0,   0,   0,   0,   0,   0,   0,   0}, 
   // {1,   0,   0,   0,   5,   0,   0,   0,   0,   0,   0,   0,   0}, 
   // {0,   2,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0}, 
   // {1,   0,   3,   0,   0,   6,   0,   0,   0,   0,   0,   0,   0}, 
   // {0,   0,   0,   0,   5,   0,   0,   8,   0,   0,   0,   0,   0}, 
   // {0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0}, 
   // {0,   0,   0,   0,   0,   6,   0,   0,   9,   0,  11,   0,   0}, 
   // {0,   0,   0,   0,   0,   0,   0,   8,   0,   0,  11,   0,   0}, 
   // {0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,  12,   0}, 
   // {0,   0,   0,   0,   0,   0,   0,   8,   9,   0,   0,   0,  13}, 
   // {0,   0,   0,   0,   0,   0,   0,   0,   0,  10,   0,   0,   0}, 
   // {0,   0,   0,   0,   0,   0,   0,   0,   0,   0,  11,   0,   0}};
   private int[][] map;
   private Node[] nodeArr;
   private LinkedList<Node> open;
   private Subgraph closed;
   
   public Graph(){ super(); }
   
   public Graph(int[][] theMap){
      super();
      this.map = map;
      int n = extractSubgraphs(theMap);
   }

/***
   * Converts an adjacency matrix to adjacency lists by extracting one or
   * more adjacency subgraphs.  Each subgraph is added to this object and the
   * the number of subgraphs is returned.
   */
   public int extractSubgraphs(int[][] theMap){
   
      this.map = theMap;    
      clear();

      //create the list of unique nodes
      int ny = map.length;      
      nodeArr = new Node[ny];
      for (int y = 0; y < ny; y++){ nodeArr[y] = new Node(y+1);}   

      //traverse the list of possible nodes
      for (int i = 0; i < ny; i++){
         if (nodeArr[i].visited == false){
            //if node not visted then the this is a new root node
            //create a new open and closed list
            //closed nodes have been investigated
            closed = new Subgraph(); 
            //open nodes are neighbors of waiting to be investigated  
            open = new LinkedList<Node>(); 
            //mark any addtions to open as "visited"
            nodeArr[i].visited = true;
            open.addLast(nodeArr[i]);

            while (open.size() != 0){
               Node node = open.pollFirst();
               //if pollFirst returns null then there are no
               //more nodes to visit
               if (node == null) {break;}
                  closed.add(node);
                  traceNeighbors(node);
               //}
            }//while
            
            //we have arrived at the end of this list - add it to the 
            //collection of lists
            add(closed);
         }
      }
      
      return size();
   }

/***
   * This method investigates the direct neighbors of the provided node
   * any unvisted neighbors are marked visited and added to the open list
   */ 
   private void traceNeighbors(Node node){
      int y = node.label - 1;
      for (int x = 0; x < map[y].length; x++){
         if ((map[y][x] !=0) && (nodeArr[x].visited == false)){ 
            nodeArr[x].visited = true;
            open.addLast(nodeArr[x]);
         }//if
      }//for
   }
   
   
/***
   * Returns the ragged array of the adjacency lists
   * the values are the node labels.  
   */
   public int[][] getAdjacencyList(){
      if (size() == 0){ return null; }
      int[][] aL = new int[size()][];
      for (int i = 0; i < size(); i++){
         aL[i] = ((Subgraph) get(i)).getAdjacencyList();
      }
      return aL;
   
   } //getAdjacencyList
   
/***
   * Returns the adjacency lists as a string array
   */
   public String[] toStrings(){
      String[] s = new String[size()];
      for (int i = 0; i < size(); i++){
         s[i] = ((Subgraph) get(i)).toString();
      }
      return s;
   }
}