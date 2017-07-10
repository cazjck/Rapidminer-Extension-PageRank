package com.rapidminer.extension.pagerank;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.concurrent.FutureTask;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.rapidminer.extension.utilities.HadoopHelper;



public class PageRankDriver extends Configured implements Tool {
	public static String input;
	public static String LINKS_SEPARATOR = "|";
	private static NumberFormat nf = new DecimalFormat("00");
	public static String INPUT = "D:/pageRank/input/input.txt";
	public static final String OUTPUT = "D:/pageRank/ranking/iter";
	public static final String RESULT = "D:/pageRank/result";
	public static Double DAMPING = 0.85d;
	public static int INTERATIONS = 1;
	public static void main(String[] args) throws Exception {
		System.exit(ToolRunner.run(new Configuration(), new PageRankDriver(), args));

	}

	public PageRankDriver() {
	}

	public PageRankDriver(String inputPath, String outputPath, int interation, float damping) {
		
	}

	@Override
	public int run(String[] args) throws Exception {
		// nlwiki-latest-pages-articles
		// boolean isCompleted = runXmlParsing("wiki/in",
		// "wiki/ranking/iter00");
		boolean isCompleted = false;

		String lastResultPath = null;
		String inPath=null;
		for (int runs = 0; runs < INTERATIONS; runs++) {
			if(runs>0){
				 inPath = OUTPUT + nf.format(runs);
			}
			else {
				inPath=INPUT;
			}
			
			lastResultPath = OUTPUT+ nf.format(runs + 1);

			isCompleted = runRankCalculation(inPath, lastResultPath);

			if (!isCompleted)
				return 1;
		}

		isCompleted = runRankOrdering(lastResultPath,RESULT);
		if (!isCompleted)
			return 1;
		return 0;
	}

	public static boolean runRankCalculation(String inputPath, String outputPath) throws Exception {

		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "DBLP Input Calculate");
		job.setJarByClass(PageRankDriver.class);
		job.setMapperClass(RankCalculateMapper.class);

		job.setReducerClass(RankCalculateReduce.class);
		// job.setCombinerClass(DBLPCalculateReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		HadoopHelper.deleteFolderHadoopCluster(conf,outputPath);
		FileInputFormat.addInputPath(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		return job.waitForCompletion(true) ? true : false;
	}

	private boolean runRankOrdering(String inputPath, String outputPath)
			throws Exception {
		Configuration conf = new Configuration();

		Job rankOrdering = Job.getInstance(conf, "rankOrdering");
		rankOrdering.setJarByClass(PageRankDriver.class);

		rankOrdering.setOutputKeyClass(FloatWritable.class);
		rankOrdering.setOutputValueClass(Text.class);

		rankOrdering.setMapperClass(RankingMapper.class);
		rankOrdering.setSortComparatorClass(DescendingKeyComparator.class);
		HadoopHelper.deleteFolderHadoopCluster(conf,outputPath);
		FileInputFormat.setInputPaths(rankOrdering, new Path(inputPath));
		FileOutputFormat.setOutputPath(rankOrdering, new Path(outputPath));

		rankOrdering.setInputFormatClass(TextInputFormat.class);
		rankOrdering.setOutputFormatClass(TextOutputFormat.class);

		return rankOrdering.waitForCompletion(true);
	}


}
