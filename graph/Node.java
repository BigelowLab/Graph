package graph;
import java.util.*;

/***
   * Use this class the label and visited status flag of each node.
   * 
   * 2010-04-30 Ben Tupper, written
   */
   
public class Node {

   public final int label;
   public boolean visited = false;
   
   public Node(int theLabel){
      label = theLabel;
   }

}