//Finding Farthest Pair of Points


// Importing packages
import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.SparkConf;
import scala.Tuple2;
import java.io.*;
import java.util.*;




public class FarthestPair {
	
	
	public static void main(String []args)
	{
		//Create Spark Context Object
		SparkConf conf=new SparkConf().setAppName("SPATIAL3").setMaster("spark://192.168.77.147:7077");
		JavaSparkContext sc=new JavaSparkContext(conf);
		
		//Accessing the Jar file
		sc.addJar("/home/andrew/Desktop/FP.jar");
		
		//Accessing the File from HDFS
		JavaRDD<String> jr=sc.textFile("hdfs://master:54310/inputfiles/farthest/Inputs.csv");
		List<String> st=jr.collect();
		
		// Broadcasting the file as a string array
		Broadcast<List<String>> br=sc.broadcast(st);
		final List<String> broad;
		broad=br.value();
		
		//Calculating the farthest point for each point in RDD
		JavaPairRDD<Double,String> fl=jr.mapToPair(new PairFunction<String,Double,String>()
		{
			double max=-1;
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
					if(dist>max )
					{
						max=dist;
						point=part;
					}
				}
							
				return new Tuple2<Double,String>(max,data+"$"+point);
			}
		});
			
		//Finding the farthest points
		Tuple2<Double,String> result=fl.sortByKey(false).first();
		List<String> finalresult = Arrays.asList(result._2.split("$"));
		
		//Writing file to HDFS
		JavaRDD finalRdd = sc.parallelize(finalresult);
		finalRdd.repartition(1).saveAsTextFile("hdfs://master:54310/outputfiles/farthest/result");
	}
}
