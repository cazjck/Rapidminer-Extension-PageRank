package com.rapidminer.pagerank.hadoop.utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.ExampleSetFactory;

/**
 * 
 * @author Khanh Duy Kham
 *
 */
public class ReadFileHadoopClusterCallable implements Callable<ExampleSet> {
	BufferedReader br;
	FileSystem fs;
	Path path;
	ArrayList<String[]> arrayList;
	String line;

	/**
	 * Read File Hadoop Cluster by Future task
	 * 
	 * @param url
	 * @throws Exception
	 */
	public ReadFileHadoopClusterCallable(String url) throws Exception {
		arrayList = new ArrayList<>();
		fs = FileSystem.get(HadoopCluster.getConf());
		path = new Path(url);

	}

	@Override
	public ExampleSet call() throws Exception {
		if (fs.exists(path)) {
			br = new BufferedReader(new InputStreamReader(fs.open(path)));

			while ((line = br.readLine()) != null) {
				String[] s = line.split("\t");
				arrayList.add(s);
			}
			br.close();
			fs.close();
		}

		String[][] array2D = MaxtriHelper.convert2DString(arrayList);
		return ExampleSetFactory.createExampleSet(array2D);
	}

}
