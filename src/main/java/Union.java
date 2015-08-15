// CSE 512 Distributed Database Systems - Project
// Operation 1: Geometric Union
// Charan Uppuluri
// This operation performs a Geometric Union on the given set of polygons. 
// The classes provided by Java Topology Suite are used to perform the union operation.


import scala.Tuple2;
import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.SparkConf;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;


public class Union
{
    public static void main( String[] args ) throws IOException
    {
    	SparkConf conf = new SparkConf().setAppName("SPATIAL3").setMaster("spark://192.168.77.147:7077");
    	JavaSparkContext sc = new JavaSparkContext(conf); 
    	sc.addJar("/home/andrew/Desktop/GU.jar");
    	
    	// Reading the diagonal coordinates of the input polygons
    	JavaRDD<String> points_rdd = sc.textFile("hdfs://master:54310/inputfiles/union/UnionInput.csv");
    	    	
    	// Finding out all the points that represent each polygon and creating an RDD.
    	JavaRDD<String> polygons_rdd = points_rdd.map(new Function<String,String>()
    			{
		    	public String call(String s)
			    	{
			    		String cvsSplitBy = ",";
			    		String[] points = s.split(cvsSplitBy);
			    		double[] temp = new double[points.length];
			    		for(int i = 0; i<points.length;i++)
			    		{
			    			temp[i] = java.lang.Double.parseDouble(points[i]);	
			    		}
			    			
			    		double[] coordinates = new double[8];
			    		coordinates[0] = temp[0];
			    		coordinates[1] = temp[1];
			    		coordinates[2] = temp[2];
			    		coordinates[3] = temp[1];
			    		coordinates[4] = temp[2];
			    		coordinates[5] = temp[3];
			    		coordinates[6] = temp[0];
			    		coordinates[7] = temp[3];
			    		String line = "" + coordinates[0] + ","+ coordinates[1]+ ","+ coordinates[2]+ ","+ coordinates[3]+ ","+ coordinates[4]+ ","+ coordinates[5]+ ","+ coordinates[6]+ ","+ coordinates[7];
						return line;    		
			    	}		
    			});
    	
    	// Adding same key to each value to perform aggregate union on all polygons
    	JavaPairRDD<String,String> map_values = polygons_rdd.mapToPair(new PairFunction<String,String,String>()
    	{
    		public Tuple2<String, String> call(String s)
    		{
    			return new Tuple2<String,String> ("a",s);
    		}   		
    	});
    	
    	// Implementation of Aggregate Union function of the set of polygons with same key using reduceByKey()
    	
    	JavaPairRDD<String, String> answer = map_values.reduceByKey(new Function2<String,String,String>()
    	{
    		public String call(String d,String s)
    		{
    			List<String> polygon1 = new ArrayList<String>();
    			List<String> polygon2 = new ArrayList<String>();
    			
    			polygon1.addAll((List<String>)Arrays.asList(d.split(",")));
    			polygon2.addAll((List<String>)Arrays.asList(s.split(",")));
    			
    			List<Double> poly1 = new ArrayList<Double>();
    			List<Double> poly2 = new ArrayList<Double>();
    			
    			for(String point: polygon1)
    			{
    				poly1.add(Double.parseDouble(point));
    			}
    			
    			for(String point: polygon2)
    			{	
    				poly2.add(Double.parseDouble(point));	
    			}
    			
    			// Here, the java topology suite classes are used to perform the Union of two polygons. 
    			//The resultant polygon is returned, to which the union is calculated recursively with each polygon in the RDD.
    			Coordinate[] first = new Coordinate[((poly1.size())/2)+1];
    			Coordinate[] second = new Coordinate[((poly2.size())/2)+1];
    			int j = 0;
  
    			// The "double" points are converted into "coordinates"  			
    			for(int i = 0; i<poly1.size();i=i+2)
    			{	
    				first[j] = new Coordinate(poly1.get(i),poly1.get(i+1));	
    				j = j+1;
    			}
    			first[j] = (new Coordinate(poly1.get(0),poly1.get(1)));		
    			
    			int k = 0;
    			for(int i = 0; i<poly2.size();i=i+2)
    			{	
    				second[k] = new Coordinate(poly2.get(i),poly2.get(i+1));	
    				k = k+1;
    			}
    			
    			second[k] = (new Coordinate(poly2.get(0),poly2.get(1)));
    			GeometryFactory fact1 = new GeometryFactory();
    			LinearRing linear1 = new GeometryFactory().createLinearRing(first);
    			Geometry g1 = new Polygon(linear1, null, fact1);
    			LinearRing linear2 = new GeometryFactory().createLinearRing(second);
    			Geometry g2 = new Polygon(linear2, null, fact1);
    			Geometry G = g2.union(g1);		// Union of current polygon with previous result.
    			Coordinate[] n = G.getCoordinates();	
    			String returnval = "";
    			
    			for(int i = 0; i<n.length-1; i++)
    			{	
    				returnval = returnval + n[i].x +"," +n[i].y+",";	//Coordinates are converted back to "double"
    			}
    			returnval = returnval.substring(0, returnval.length()-1);
    			return returnval;		// new Union is returned
    		}
    	});
		
		// collecting the final Union of all polygons
    	List<Tuple2<String,String>> mylist = answer.collect();
    	List<String> output = new ArrayList<String>();
        for(Tuple2 tuple : mylist)
        {
        	int i = 0;
        	String[] values = tuple._2.toString().split(",");
        	for(i=0;i<values.length-1;i=i+2)
        	{	
        		output.add(values[i]+","+values[i+1]);
        	}
       }
        
        // Save the values in the "output" into a file
       JavaRDD<String> finalrdd= sc.parallelize(output);
       finalrdd.repartition(1).saveAsTextFile("hdfs://master:54310/outputfiles/union/result");
    }
}
