import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import ij.util.*;
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
  * 2010-08-31 btupper@bigelow.org
  */ 


public class Graph_ implements PlugIn {
   
   String maskName = "";
   ImagePlus imp, maskimp;  
	ImageProcessor ip, maskip;
	Calibration cal;

	Wand wand;
   private Vector <PolygonRoi> vPoly = new Vector<PolygonRoi>();
   private Vector <graph.Point> vPoints = new Vector<graph.Point>(); // if we are operating on points we don't need a Vector
   
   private double[][] dist; //distance matrix
   private int[][] adj; //adjacency matrix
   private Line[][] line;//array of lines (Roi)
   ResultsTable rt;
   private int[] colIdx = new int[2];
   private ResultsTable dt;
   private ResultsTable mt;
   private double combineIfCloserThan = 10.0;
   private int nItems;
   
   public String whichDist = "Edges"; // Edges, Centroids, Centers of Mass
   
   private boolean symmetricTable = true;
   private boolean showLines = false;
   private boolean showDist = false;
   private boolean showDistTable = false;
	private String distFormat = "%4.1f";
	
	public void run(String arg) {

   if (IJ.versionLessThan("1.43")) {return;}

   //are there options?  If so, parse them.  If not, then show the dialog.
   String opt = Macro.getOptions();
   if ((opt != null) && (opt.length() != 0)){
      String[] s;
      String[] opts = opt.split(" ");
      for (int i = 0; i < opts.length; i++){
        s = opts[i].split("=");
        if (s[0].equalsIgnoreCase("mask")){
          maskName = s[1];
        } else if (s[0].equalsIgnoreCase("neighbors")){
          combineIfCloserThan = Tools.parseDouble(s[1]);
        } else if (s[0].equalsIgnoreCase("labels")){
          showDist = true;
        } else if (s[0].equalsIgnoreCase("lines")) {
          showLines = true;
        } else if (s[0].equalsIgnoreCase("matrix")) {
          showDistTable = true;
        } else if (s[0].equalsIgnoreCase("distance")) {
          whichDist = s[1];
        } else {
          IJ.log("oops! unrecognized argument: " + opts[i]);
        }
      }//i-loop
   } else {
      if (showDialog() == false){return;}
   }
   
   // here is where it really happens
   process();
   
   }
   
/** The primary entry function*/
   private void process(){
  
   // double check the image, is it really binary (0-255) 
   maskimp = WindowManager.getImage(maskName);
   maskip = maskimp.getProcessor();
   if (!maskip.isBinary()){
      IJ.showMessage("Graph_: mask image must be binary");
      return;
   }
   
   //for efficiency, predetermine column indices for (XStart, YStart), (X,Y), or (XM, YM)
   rt = Analyzer.getResultsTable();  
   nItems = rt.getCounter();
   if (nItems == 0){
      IJ.showMessage("Graph_: Results table must be populated");
      return;
   }
   
   if (whichDist.equalsIgnoreCase("Edges")){
      colIdx[0] = rt.getColumnIndex("XStart");
      colIdx[1] = rt.getColumnIndex("YStart");
      if ((colIdx[0] == rt.COLUMN_NOT_FOUND) || (colIdx[1] == rt.COLUMN_NOT_FOUND)) {
         IJ.showMessage("Graph_: Results table must have XStart and YStart columns");
         return;
      }      
   } else if (whichDist.equalsIgnoreCase("Centroids")) {
      colIdx[0] = rt.getColumnIndex("X");
      colIdx[1] = rt.getColumnIndex("Y");
      if ((colIdx[0] == rt.COLUMN_NOT_FOUND) || (colIdx[1] == rt.COLUMN_NOT_FOUND)) {
         IJ.showMessage("Graph_: Results table must have X and Y");
         return;
      }
   } else if (whichDist.equalsIgnoreCase("Centers of Mass")){   // "centers of mass"
      colIdx[0] = rt.getColumnIndex("XM");
      colIdx[1] = rt.getColumnIndex("YM");
      if ((colIdx[0] == rt.COLUMN_NOT_FOUND) || (colIdx[1] == rt.COLUMN_NOT_FOUND)) {
         IJ.showMessage("Graph_: Results table must have XM and YM");
         return;
      }   
   } else {
      IJ.showMessage("Distance is not known: " + whichDist);
      return;  
   }
   
   //duplicate the mask for the purpose of creating the blob-colored adjacencies
   //this will have problems when we get more than 254 blobs, so convert to Short 
   ip = maskip.duplicate();
   ip = ip.convertToShort(true);
   imp = new ImagePlus("Adjacencies", ip);
   
   //calibration is required for distance computation
   cal = maskimp.getCalibration();

   //create the distance, adjacency and Line (roi) matrices
   dist = new double[nItems][nItems];
   adj = new int[nItems][nItems];
   line = new Line[nItems][nItems];

  
   
   if (whichDist.equalsIgnoreCase("edges")){
      wand = new Wand(maskip);
      wand.setAllPoints(true);
      //for each blob, get the perimter coords - these are saved as Polygon Rois
      captureBoundaries();
      //now compute the distances between the polygons (and create the Line Rois)
      computeDistEdges();
   } else {
      capturePoints();
      computeDistPoints();   
   }  
      
      
   //now build the adjacency matrix - note the diagonal is left as 0
	for (int y = 0; y < nItems; y++){
	  for (int x = y; x < nItems; x++){
	     if ((dist[y][x] > 0) && (dist[y][x] < combineIfCloserThan)){
	        adj[y][x] = x+1;
	        adj[x][y] = y+1;
	     }
	  }
	}

   //create and instance of the Graph maker
	Graph	g = new graph.Graph();
	//extract the sub graphs
	int n = g.extractSubgraphs(adj);
	
	//
	//the remainder if this method is simply show and tell
	//
	// show the connectivity lists (sub graphs)
   IJ.log("Separation distances computed on " + whichDist);
   IJ.log("Number of connected components = " + n);		
   String[] aL = g.toStrings();
   for (int i = 0; i < g.size(); i++){
      IJ.log("CC-" + (i+1) + ": " + aL[i]);
   }	
   	
   //now we can have adj as a ragged array of CC lists
   //the adjacency list look just like what was printed in the log window
   //less the "CC-n:" part.  Each element of adjList contains one subgraph
   // and each subgraph contains one or more nodes.
   int[][] adjList;
   adjList = g.getAdjacencyList();
   
   // label the mask  
   FloodFiller ff = new FloodFiller(ip);
   int x0 = 0;
   int y0 = 0;
   for (int y = 0; y < adjList.length; y++){
      ip.setValue(y+1);
      for (int x = 0; x < adjList[y].length; x++){
          x0 = (int) rt.getValueAsDouble(colIdx[0], adjList[y][x] -1);
          y0 = (int) rt.getValueAsDouble(colIdx[1], adjList[y][x] -1);
          ff.fill8(x0,y0);
      }
   }
   
   //if the user asks for lines and distance labels
   if (showLines || showDist){
      Overlay overlay;
      overlay = imp.getOverlay();
      if (overlay == null) { overlay = new Overlay();}
      int xc = 0;
      int yc = 0;
      for (int y = 0; y < adj.length; y++){
         for (int x = y; x < adj[y].length; x++){
            if ((adj[y][x] != 0) && (line[y][x] != null)){
                  xc = (int) Math.round((line[y][x].x2d - line[y][x].x1d)/4.0 + line[y][x].x1d);
                  yc = (int) Math.round((line[y][x].y2d - line[y][x].y1d)/4.0 + line[y][x].y1d);
                  TextRoi text = new TextRoi(xc, yc, 
                     String.format(distFormat, dist[y][x]), 
                     new Font("SansSerif", Font.PLAIN, 10));
                  if (showLines) {overlay.add(line[y][x]);}
                  if (showDist) {overlay.add(text);}
               }
         }
      }
      imp.setOverlay(overlay); 
   }//show something?
   
   // show the distance table?
   if (showDistTable){
      dt = new ResultsTable();
      for (int y = 0; y < nItems; y++){
         dt.incrementCounter();
         for (int x = 0; x < nItems; x++){
            dt.setValue(""+(x+1),y,dist[y][x]);
         }
      }
      dt.show("Distance Matrix");
   } //showDistTable
   
   //this is to exxagerate the contrast without losing the labels
   IJ.run(imp, "Enhance Contrast", "saturated=0.4");
   imp.show();
   

}  //run

/**
   * Computes the shortest distance between each object and all other objects listed 
   * in the Results Table
   */
   private void computeDistPoints(){
      graph.Point A;
      graph.Point B;
      for (int y = 0; y < nItems; y++){
         A = vPoints.get(y);
         for (int x = (y+1); x<nItems; x++){
            B = vPoints.get(x);
            dist[y][x] = minimumDistanceTo(A, B, y, x);
            if (symmetricTable) {dist[x][y] = dist[y][x];}
         } // j-loop
      
      }//i-loop
   
   }


/**
   * Computes the shortest distance between each object and all other objects listed 
   * in the Results Table
   */
   private void computeDistEdges(){
      PolygonRoi polyA;
      PolygonRoi polyB;
      for (int y = 0; y < nItems; y++){
         polyA = vPoly.get(y);
         FloatPolygon fpA = polyA.getFloatPolygon();
         
         for (int x = (y+1); x<nItems; x++){
            polyB = vPoly.get(x);
            dist[y][x] = minimumDistanceTo(fpA, polyB.getFloatPolygon(), y, x);
            if (symmetricTable) {
               dist[x][y] = dist[y][x];
            }
         } // j-loop
      
      }//i-loop
   
   }

/**
  * Computes the minumum distance between points
  */   
   private double minimumDistanceTo(graph.Point A, graph.Point B, int y, int x){
   
    double d = A.distanceTo(B); 
    Line thisLine = new Line(A.x, A.y, B.x, B.y);
    if (thisLine.getLength() > 0.0) {
       line[y][x] = thisLine;
       line[x][y] = thisLine;
    }
    
    return d;
  }// minimumDistanceBetween
     
/**
  * Computes the minumum distance between the boundaries defined by each polygon
  */   
   private double minimumDistanceTo(FloatPolygon A, FloatPolygon B, int y, int x){
      double d = Double.MAX_VALUE;
      double temp, tempx, tempy;
      int ipA = 0;
      int ipB = 0;
      
      for (int i = 0; i < A.npoints; i++){
        for (int j = 0; j < B.npoints; j++){
          tempx = cal.getX(A.xpoints[i]) - cal.getX( B.xpoints[j]);
          tempy = cal.getY(A.ypoints[i]) - cal.getY( B.ypoints[j]);
          temp = tempx*tempx + tempy*tempy;
          if (temp < d) { 
            d = temp;
            ipA = i;
            ipB = j;
         }
        } //j-loop
      } // i-loop
    
    
    Line thisLine = new Line(A.xpoints[ipA], A.ypoints[ipA], B.xpoints[ipB], B.ypoints[ipB]);
    if (thisLine.getLength() > 0.0) {
       line[y][x] = thisLine;
       line[x][y] = thisLine;
    }
    
    return Math.sqrt(d);
  }// minimumDistanceBetween


/***
   Loops through each row of the results table and creates a Point per row
   Will be either Centers of Mass or Centroid depending upon user choice  
  */
   private void capturePoints(){
   
      double x0 = 0;
      double y0 = 0;
      for (int i = 0; i < nItems; i++){
         //x0 = cal.getX( rt.getValueAsDouble(colIdx[0], i));
         //y0 = cal.getY( rt.getValueAsDouble(colIdx[1], i));
         x0 = rt.getValueAsDouble(colIdx[0], i);
         y0 = rt.getValueAsDouble(colIdx[1], i);
         graph.Point P = new graph.Point(x0, y0);
         vPoints.add(P);
      }
   }

/**
   Loops through each row of the Results Table, uses Wand to caputure the "allPoints"
   boundary and stores them as PolygonRoi objects in a Vector.
   */
   private void captureBoundaries(){
   
      int x0 = 0;
      int y0 = 0;
   
      for (int i = 0; i < nItems; i++){
         x0 = (int) rt.getValueAsDouble(colIdx[0], i);
         y0 = (int) rt.getValueAsDouble(colIdx[1], i);
         wand.autoOutline(x0, y0, 0.0, Wand.EIGHT_CONNECTED);
         PolygonRoi poly = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON);
         vPoly.add(poly);
      }
   
   }//captureBoundaries

  /**
    * This returns a list of the names of the image windows plus "None"
    */
  private String[] getImageList(){
    int n = WindowManager.getImageCount();
    if (n == 0){ return null;}
    int[] ids = WindowManager.getIDList();
    Vector<String>out = new Vector<String>();
    ImagePlus theImp = null; 
    ImageProcessor theIp = null;
    for (int i = 0; i < n; i++){
      theImp = WindowManager.getImage(ids[i]);
      theIp = theImp.getProcessor();
      if (theIp.isBinary()){out.add(theImp.getTitle());}
    }//i-loop
    if (out.size()==0){ return null;}
    
    return out.toArray(new String[out.size()]); 
  }//getImageList
   
 
 
 private boolean showDialog(){
 
 
   String[] imageList = getImageList();
   if (imageList == null){
      IJ.showMessage("Graph_", "There are no binary images to select from");
      return false;
   }
   
   String[] wdist = {"Edges", "Centroids", "Centers of Mass"};
   GenericDialog gd = new GenericDialog("Graph_ setup");
   gd.addChoice("Mask image", imageList, imageList[0]);
   gd.addChoice("Distance between", wdist, "Edges");
   gd.addNumericField("Neighbors closer than", combineIfCloserThan, 3);
   gd.addCheckbox("Lines shown?", showLines);
   gd.addCheckbox("Label distances?", showDist);
   gd.addCheckbox("Matrix shown?", showDistTable);
   gd.showDialog();
   
   if (gd.wasCanceled()) {return false;}
   maskName = gd.getNextChoice();
   whichDist = gd.getNextChoice();
   combineIfCloserThan = gd.getNextNumber();
   showLines = gd.getNextBoolean();
   showDist = gd.getNextBoolean();
   showDistTable = gd.getNextBoolean();
   return true;
 }
 
}
