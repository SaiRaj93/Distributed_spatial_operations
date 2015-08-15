import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.SparkConf;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.*;

public class RangeQuery implements Serializable {

	public static String query = new String();
	public static void main(String args[]) throws FileNotFoundException, IOException
	{
		SparkConf conf = new SparkConf().setAppName("RangeQuery").setMaster("spark://192.168.77.147:7077");
		JavaSparkContext sc = new JavaSparkContext(conf);
	
		sc.addJar("/home/andrew/Desktop/range.jar");
		JavaRDD<String> datalines = sc.textFile("hdfs://master:54310/inputfiles/rangequery/RangeQueryTestData.csv");
		
		JavaRDD<String> query_rdd=sc.textFile("hdfs://master:54310/inputfiles/rangequery/queryrectangle.csv");
		List<String> query_value =query_rdd.collect();
		String query= query_value.get(0);
		Broadcast<String> br=sc.broadcast(query); // Broadcasting the file as a string array
			final String query_string;
			query_string=br.value();
	
		JavaRDD<String> ids = datalines.map(new Function<String,String>()
		{
			public String call(String data)
			{
				
				List<String> value_data = new ArrayList<String>();
				List<String> value_query = new ArrayList<String>();
		
				value_data.addAll((List<String>)Arrays.asList(data.split(",")));
				value_query.addAll((List<String>)Arrays.asList(query_string.split(",")));
				
				Double x1 = Double.parseDouble(value_data.get(0));
				Double y1 = Double.parseDouble(value_data.get(1));
				Double x2 = Double.parseDouble(value_data.get(2));
				Double y2 = Double.parseDouble(value_data.get(3));
							
				Double xa = Double.parseDouble(value_query.get(0));
				Double ya = Double.parseDouble(value_query.get(1));
				Double xb = Double.parseDouble(value_query.get(2));
				Double yb = Double.parseDouble(value_query.get(3));
				
				if(((x1 >= xa && x1<= xb) && (y1 >=ya && y1 <= yb)) && ((x2 >= xa && x2<= xb) && (y2 >=ya && y2 <= yb)))
				{			
						return value_data.toString();
				}
				else
					return "gamma";
			}		
		});
		
		List<String> temp = new ArrayList<String>();
		temp = ids.collect();
		
		List<String> finallist = new ArrayList<String>();
		
		for(String s : temp)
		{
			if(s.equals("gamma") == false)
			{
				finallist.add(s);
			}
		}
		
		JavaRDD<String> finalRdd = sc.parallelize(finallist);
		finalRdd.repartition(1).saveAsTextFile("hdfs://master:54310/outputfiles/rangequery/result");
		
	}
}
