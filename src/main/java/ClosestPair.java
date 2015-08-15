// Finding The Closest Pair of Points

import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.SparkConf;
import scala.Tuple2;
import java.io.*;
import java.util.*;



public class ClosestPair {
	
	
	public static void main(String []args)
	{	
		//Initializing the spark Contest Object
		SparkConf conf=new SparkConf().setAppName("SPATIAL4").setMaster("spark://192.168.77.147:7077");
		JavaSparkContext sc=new JavaSparkContext(conf);
		
		//Adding the Jar File
		sc.addJar("/home/andrew/Desktop/CP.jar");
		
		//Getting the file from the HDFS
		JavaRDD<String> jr=sc.textFile("hdfs://master:54310/inputfiles/closest/Inputs.csv");
		
		//Broadcasting the list
		List<String> st=jr.collect();
		Broadcast<List<String>> br=sc.broadcast(st); // Broadcasting the file as a string array
		final List<String> broad;
		broad=br.value();
		
		//Finding the closest point for every element in the RDD 
		JavaPairRDD<Double,String> fl=jr.mapToPair(new PairFunction<String,Double,String>()
			{
			double mini=10;
			String point=null;
			public Tuple2<Double,String> call(String data) throws Exception
				{
				String parts[]=data.split(",");
				double x1=Double.parseDouble(parts[0]);
				double y1=Double.parseDouble(parts[1]);
				for(String part: broad)
					{
					String par[]=part.split(",");
					double x2=Double.parseDouble(par[0]);
					double y2=Double.parseDouble(par[1]);
					double dist=Math.sqrt((Math.pow((x2-x1),2))+(Math.pow((y2-y1),2)));
					if(dist<mini && dist>0)
						{
						mini=dist;
						point=part;
						}
					}
							
							
				return new Tuple2<Double,String>(mini,data+"$"+point);
				}
				
			});
			
			//Find the closest pair
			Tuple2<Double,String> result=fl.sortByKey(true).first();
			List<String> finalresult = Arrays.asList(result._2.split("$"));
			
			//Writing the output into HDFS
			JavaRDD<String> finalRdd= sc.parallelize(finalresult);
			finalRdd.repartition(1).saveAsTextFile("hdfs://master:54310/outputfiles/closest/result");		
	}
	
}
