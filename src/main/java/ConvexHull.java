
import java.util.*;
import java.util.Iterator;
import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.SparkConf;
import java.util.Set;
import scala.Tuple2;
import java.util.Collection;
import java.io.*;
import com.google.common.collect.Lists;


// OUR POINT OBJECT CLASS
class Point2D implements Comparable<Point2D>, java.io.Serializable {
	double x,y;
	
	Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}
 
	// For sorting: sort first on x, break tie on y
	public int compareTo(Point2D other)     
	{
		// break tie with y coordinate
		if( x == other.x) {
			if (y < other.y) return -1;
			if (y > other.y) return 1;
			return 0;
		}
		// otherwise just compare x coordinate
		if (x < other.x) return -1;
		return 1;	
	}
 
    // cross product of two vectors
	public double cross(Point2D p) {
		return x * p.y - y * p.x;
	}
 
    // subtraction of two points
	public Point2D sub(Point2D p) {
		return new Point2D(x - p.x, y - p.y);
	}
 
	public String toString() {
		return x + "," + y;
	}

}


// MAIN 
public class ConvexHull {
	public static void main (String[] args) {
		// Set up the spark environment
		SparkConf conf = new SparkConf().setAppName("myapp").setMaster("spark://192.168.77.147:7077");     
        JavaSparkContext sc = new JavaSparkContext(conf);
        sc.addJar("/home/andrew/Desktop/CH.jar");
        
        // get the string lines from the text file  
        JavaRDD<String> lines = sc.textFile("hdfs://master:54310/inputfiles/convexhull/input.txt");
        
        JavaRDD<String> finalresult = lines.mapPartitions(new FlatMapFunction<Iterator<String>,String>()
        {
        	public Iterable<String> call(Iterator<String> s)
        	{		
        		// Convex Hull algorithm implemented with guidance of the following link:
        		// www.algorithmist.com/index.php/Monotone_Chain_Convex_Hull
        		List<String> mylist = Lists.newArrayList(s);
        		ArrayList<Point2D>  pointList = new ArrayList<Point2D>();
        		for (String str : mylist) {
        			Point2D p = new Point2D(0,0);
        			String[] split = str.split(",");
        			p.x = Double.parseDouble(split[0].trim());
        			p.y = Double.parseDouble(split[1].trim());
        			pointList.add(p);
        		}
        		Point2D[] points = pointList.toArray(new Point2D[pointList.size()]);
        		Arrays.sort(points);
        		
        		int n = points.length;
    			Arrays.sort(points);
    			// In between we may have a 2n points
    			Point2D[] ans = new Point2D[2*n];
    			int k = 0;
    			// start is the first insertion point
    			int start = 0;
    		 
    			// Find the lower layer of the hull
    			for(int i = 0; i < n; i++) {
    				Point2D p = points[i];
    				while (k - start >= 2 && p.sub(ans[k-1]).cross(p.sub(ans[k-2])) > 0)
    					k--;
    				ans[k++] = p; 
    			}
    			
    			// truncate the last point from the lower layer
    			k--;
    			start = k;						
    		 
    			// Find the upper layer of the hull
    			for (int i = n-1; i >= 0; i--) {
    				Point2D p = points[i];
    				while (k - start >= 2 && p.sub(ans[k-1]).cross(p.sub(ans[k-2])) > 0)
    					k--;
    				ans[k++] = p; 
    			}
    			
    			// truncate the last point from the top layer
    			k--;
    		 
    			Point2D[] pointArray = Arrays.copyOf(ans, k);
    			String[] stringPoints = new String[pointArray.length];
    			for (int i=0; i < pointArray.length; i++)
    				stringPoints[i] = pointArray[i].toString();
    			
    			Iterable<String> anything = Arrays.asList(stringPoints);
    			return anything;   	
        	}
        });
        
        // collect the individual convex hulls from the partitions, omitting all points
        // that are not in the hulls.
        List<String> list = finalresult.collect();

        // convert the strings into Point2D objects
        ArrayList<Point2D>  pointList = new ArrayList<Point2D>();
		for (String str : list) {
			Point2D p = new Point2D(0,0);
			String[] split = str.split(",");
			p.x = Double.parseDouble(split[0].trim());
			p.y = Double.parseDouble(split[1].trim());
			pointList.add(p);
		}
		
		// find the convex hull of the union of the convex hulls of the partitions.
		Point2D[] points = pointList.toArray(new Point2D[pointList.size()]);
		Arrays.sort(points);
		
		int n = points.length;
		Arrays.sort(points);
		// In between we may have a 2n points
		Point2D[] ans = new Point2D[2*n];
		int k = 0;
		// start is the first insertion point
		int start = 0;
	 
		// Find the lower layer of the hull
		for(int i = 0; i < n; i++) {
			Point2D p = points[i];
			while (k - start >= 2 && p.sub(ans[k-1]).cross(p.sub(ans[k-2])) > 0)
				k--;
			ans[k++] = p; 
		}
		
		// truncate the last point from the lower layer
		k--;
		start = k;						
	 
		// Find the upper layer of the hull
		for (int i = n-1; i >= 0; i--) {
			Point2D p = points[i];
			while (k - start >= 2 && p.sub(ans[k-1]).cross(p.sub(ans[k-2])) > 0)
				k--;
			ans[k++] = p; 
		}
		
		// truncate the last point from the top layer
		k--;
	 
	
		Point2D[] pointArray = Arrays.copyOf(ans, k);
		List<Point2D> list_values = Arrays.asList(pointArray);
		JavaRDD<Point2D> toFile = sc.parallelize(list_values);
		
		// write results to file
		toFile.repartition(1).saveAsTextFile("hdfs://master:54310/outputfiles/convexhull/result");    
	}
}