package com.rapidminer.pagerank.operator;

import java.io.File;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.pagerank.hadoop.PageRankDriver;
import com.rapidminer.pagerank.hadoop.utilities.HadoopUtilities;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;

/**
 * 
 * @author Khanh Duy Pham
 *
 */
public class PageRankHadoopOperator extends AbstractExampleSetProcessing {

	public static final String PARAMETER_DAMPING = "Damping factor";
	public static final String PARAMETER_INTERATION = "Iteration factor";
	public static final Boolean PARAMETER_CLUSTER = false;

	public PageRankHadoopOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		metaData.addAttribute(new AttributeMetaData("PageRank", Ontology.NUMERICAL));
		return super.modifyMetaData(metaData);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// Logger logger = LogService.getRoot();
		// get value parameter
		Double damping = getParameterAsDouble(PARAMETER_DAMPING);
		int interaions = getParameterAsInt(PARAMETER_INTERATION);
		PageRankDriver.DAMPING = damping;
		PageRankDriver.INTERATIONS = interaions;
		ExampleSet exampleSetResult = null;
		try {

			// Using on Hadoop
			HadoopUtilities.saveExampleSetToFile(exampleSet);
			if (PARAMETER_CLUSTER) {
				if (!HadoopUtilities.writeHadoopCluster(PageRankDriver.INPUT_LOCAL, PageRankDriver.INPUT_CLUSTER)) {
					throw new UserError(this, "301", "Update file to HDFS failed");
				}
				if (HadoopUtilities.runHadoopCluster()) {
					if (!HadoopUtilities.checkFileExistHadoopCluster(PageRankDriver.RESULT_CLUSTER)) {
						throw new UserError(this, "301", PageRankDriver.RESULT_CLUSTER);
					}
					exampleSetResult = HadoopUtilities.getDataHadoopCluster(PageRankDriver.RESULT_CLUSTER);
				} else {
					throw new UserError(this, "301", "Run Hadoop Cluster Failed");
				}

			} else {
				if (PageRankDriver.runPageRankHadoopLocal()) {
					if (!new File(PageRankDriver.RESULT_LOCAL).exists()) {
						throw new UserError(this, "301", PageRankDriver.RESULT_LOCAL);
					}
					exampleSetResult = HadoopUtilities.getDataHadoopLocal(PageRankDriver.RESULT_LOCAL);
				} else {
					throw new UserError(this, "301", "Run Hadoop Failed:" + PageRankDriver.RESULT_LOCAL);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}

		return exampleSetResult;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_DAMPING,
				I18N.getGUIMessage("operator.parameter.pagerank_damping_factor.description"), 0.0, 1.0, 0.85);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_INTERATION,
				I18N.getGUIMessage("operator.parameter.pagerank_iteration_factor.description"), 1, Integer.MAX_VALUE,
				2);
		types.add(type);
		return types;
	}

}
