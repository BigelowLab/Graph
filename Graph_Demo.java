import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import java.util.*;
import graph.*;

/**
  * This class demonstrates the use of a breadth-first-search(BFS) to 
  * associate particles located within a specified distance of each other.
  * The distance between particles is determined using the distance between
  * "Edges", "Centroids" or "Centers of Mass". From these computations a distance matrix is 
  * prepared - just like that on a road map showing the distances between 
  * cities.  Using the user specifed "connection distance" an adjacency 
  * matrix is prepared. The BFS algorithm then scans the adjacency matrix
  * to convert to adjacency lists. Note that the Results Table must be 
  * populated with either (XStart, YStart), (X, Y) or (XM, YM) depending upon the
  * users choice of "Edges", "Centroids" or "Centers of Mass"
  *
  * Suggested separation distances
  *   Edges = 10
  *   Centroids and Centers of Mass = 35
  *
  * 2010-05-01 btupper@bigelow.org
  * 2010-08-30 BT publish distance matrix and a few typos fixed
  * 2010-08-31 BT simplified to call Graph_ using IJ.run()
  */ 


public class Graph_Demo implements PlugIn {
   
   ImagePlus imp, origimp, maskimp;
	ImageProcessor ip, origip, maskip;

		
	public void run(String arg) {

   if (IJ.versionLessThan("1.43")) {return;}

   
   //open the demo image, threshold, binarize and analyze it
   //make sure the XStart and YStart are included in the output
   ImagePlus origimp = IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
   origimp.show();
   ImagePlus maskimp = new Duplicator().run(origimp);
   maskimp.setTitle("Mask");
   IJ.setAutoThreshold(maskimp, "Default");
   IJ.run(maskimp, "Convert to Mask", "");
   maskimp.show();
   IJ.run("Set Measurements...", "  centroid center display redirect=blobs.gif decimal=3");
   IJ.run(maskimp, "Analyze Particles...", "size=0-Infinity circularity=0.00-1.00 show=Nothing display clear record");
   //IJ.run("Graph ", "mask=blobs.gif distance=Edges neighbors=10.000 lines label matrix");
   IJ.run("Graph ","");
   }
  
}
