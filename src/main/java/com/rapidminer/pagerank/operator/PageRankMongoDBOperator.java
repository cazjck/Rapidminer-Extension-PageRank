package com.rapidminer.pagerank.operator;

import java.util.List;

import com.mongodb.MapReduceOutput;
import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSONParseException;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.pagerank.hadoop.PageRankDriver;
import com.rapidminer.pagerank.mongodb.MongoDBPageRank;
import com.rapidminer.pagerank.mongodb.config.MongoDBConfigurable;
import com.rapidminer.pagerank.mongodb.config.MongoExceptionWrapper;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.RepositoryAccessor;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

/**
 * 
 * @author Khanh Duy Pham
 *
 */
public class PageRankMongoDBOperator extends AbstractExampleSetProcessing {

	public static final String PARAMETER_DAMPING = "Damping_factor";
	public static final String PARAMETER_INTERATION = "Iteration_factor";
	public static final Boolean PARAMETER_CLUSTER = false;

	public PageRankMongoDBOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		metaData.addAttribute(new AttributeMetaData("PageRank", Ontology.NUMERICAL));
		return super.modifyMetaData(metaData);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = (ParameterType) new ParameterTypeConfigurable("mongodb_pagerank_instance",
				I18N.getGUIMessage("operator.parameter.mongodb_instance.description", new Object[0]),
				"mongodb_pagerank_instance");
		type.setOptional(false);
		types.add(type);
		ParameterType damping = new ParameterTypeDouble(PARAMETER_DAMPING, "This parameter defines damping factory.",
				0.0, 1.0, 0.85);
		ParameterType interations = new ParameterTypeInt(PARAMETER_INTERATION,
				"This parameter defines iteration factory.", 1, 100, 2);
		// type.registerDependencyCondition(new BooleanParameterCondition(this,

		types.add(damping);
		types.add(interations);

		return types;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// Logger logger = LogService.getRoot();

		// get value parameter
		Double damping = getParameterAsDouble(PARAMETER_DAMPING);
		int interaions = getParameterAsInt(PARAMETER_INTERATION);
		PageRankDriver.DAMPING = damping;
		PageRankDriver.INTERATIONS = interaions;
		String mongoDBInstanceName = null;
		ExampleSet exampleSetResult = null;
		try {
			mongoDBInstanceName = this.getParameterAsString("mongodb_pagerank_instance");
			MongoDBConfigurable mongoDBInstanceConfigurable = (MongoDBConfigurable) ConfigurationManager.getInstance()
					.lookup("mongodb_pagerank_instance", mongoDBInstanceName, (RepositoryAccessor) null);
			// Validation attributes
			checkAttribute(exampleSet);
			try {

				// Save Example Set to MongoDB
				if (!MongoDBPageRank.saveCollectionOld(exampleSet, mongoDBInstanceConfigurable)) {
					throw new UserError(this, "301", "error save file");
				}

				MapReduceOutput result;
				if ((result = MongoDBPageRank.runPageRankOld(interaions, damping,mongoDBInstanceConfigurable)) == null) {
					throw new UserError(this, "301", "Page Rank - Map Reduce on MongoDB failed");
				}
				exampleSetResult = MongoDBPageRank.getDataPageRank(result);

			} catch (IllegalArgumentException | JSONParseException ex2) {
				final RuntimeException ex = null;
				final RuntimeException e2 = ex;
				throw new UserError((Operator) this, (Throwable) e2, "pagerank.mongodb.invalid_json_object");
			} catch (MongoException e3) {
				throw new UserError((Operator) this, (Throwable) new MongoExceptionWrapper(e3),
						"pagerank.mongodb.mongo_exception");
			}

		} catch (ConfigurationException e) {
			throw new UserError((Operator) this, (Throwable) e, "pagerank.mongodb.configuration_exception",
					new Object[] { mongoDBInstanceName });
		} finally {

		}
		return exampleSetResult;
	}

	private void checkAttribute(ExampleSet exampleSet) throws OperatorException {
		Attribute URL = exampleSet.getAttributes().get("URL");
		Attribute OutLinks = exampleSet.getAttributes().get("OutLinks");
		if (URL == null) {
			throw new AttributeNotFoundError(this, "URL", "URL");
		}
		if (OutLinks == null) {
			throw new AttributeNotFoundError(this, "OutLinks", "OutLinks");
		}

	}

}