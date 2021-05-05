package graph;
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import ij.measure.*;
import ij.util.Tools;
import java.util.*;

/***
   * Use this class to hold the "closed" list of nodes during the 
   * connectivity search.
   * 
   * 2010-04-30 Ben Tupper, written
   */
   
public class Subgraph extends Vector<Node>{


   public Subgraph(){ super(); }

   /***
      * Returns the label of the first item stored or 0 if none.
      */ 
   public int getLabel(){
      int label = 0;
      if (size() > 0){
         Node node = (Node) get(0);
         label = node.label;
      }
      return label;
   }//getLabel

   /***
      * Returns the adjency list or null if none
      */
   public int[] getAdjacencyList(){
         int sz = size();
         if (sz == 0){ return null;}
         int[] aL = new int[sz];
         
         for (int i = 0; i < sz; i++){
            aL[i] = ((Node) get(i)).label;
         }
         return aL;
   }//getAdjacencyList
   
/***
   * For debugging use really
   */
   public void log(){
      int label = getLabel();
      IJ.log(label + ": " + toString());
   }
   
   public String toString(){
      int[] aL = getAdjacencyList();
      String s = "";
      for (int i = 0; i < size(); i++){
         s = s + ((Node) get(i)).label + " ";
      }
      return s;
   }
   
}