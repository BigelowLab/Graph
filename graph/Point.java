package graph;
import java.util.*;

/***
   * Use this class for managing the Centroid and Center Mass distances.
   * 
   * 2010-04-30 Ben Tupper, written
   */
   
public class Point {

   public double x = 0.0;
   public double y = 0.0;
   
   public Point(int x, int y){
      this.x = (double) x;
      this.y = (double) y;
   }
   
   public Point(double x, double y){
      this.x = x;
      this.y = y;
   }
   
/***
   * Compute the straight line distance between this and the specified point
   */
   public double distanceTo(Point p){   
      return distanceTo(p.x, p.y);
   }

/***
   * Compute the straight line distance between this and the specified point
   */
   public double distanceTo(double x1, double y1){   
      double dx = x1 - this.x;
      double dy = y1 - this.y;
      return Math.sqrt(dx*dx + dy*dy);
   } 
      
/***
   * Compute the straight line distance between this and the specified point
   */
   public double distanceTo(int x1, int y1){   
      return distanceTo((double) x1, (double) y1);
   }   
   

}