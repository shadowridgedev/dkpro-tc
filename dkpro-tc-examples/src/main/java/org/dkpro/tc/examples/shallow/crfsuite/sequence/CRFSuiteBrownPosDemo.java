/**
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package org.dkpro.tc.examples.shallow.crfsuite.sequence;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.Lab;
import org.dkpro.lab.task.BatchTask.ExecutionPolicy;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.api.features.TcFeatureFactory;
import org.dkpro.tc.api.features.TcFeatureSet;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.shallow.util.anno.SequenceOutcomeAnnotator;
import org.dkpro.tc.examples.util.ContextMemoryReport;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.maxnormalization.TokenLengthRatio;
import org.dkpro.tc.features.ngram.CharacterNGram;
import org.dkpro.tc.ml.ExperimentCrossValidation;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.builder.ExperimentBuilder;
import org.dkpro.tc.ml.builder.FeatureMode;
import org.dkpro.tc.ml.builder.LearningMode;
import org.dkpro.tc.ml.crfsuite.CrfSuiteAdapter;
import org.dkpro.tc.ml.report.BatchCrossValidationReport;
import org.dkpro.tc.ml.report.BatchTrainTestReport;

import de.tudarmstadt.ukp.dkpro.core.io.tei.TeiReader;

/**
 * This a pure Java-based experiment setup of POS tagging as sequence tagging.
 */
public class CRFSuiteBrownPosDemo
    implements Constants
{
    public static final String LANGUAGE_CODE = "en";

    public static final int NUM_FOLDS = 2;

    public static final String corpusFilePathTrain = "src/main/resources/data/brown_tei/";

    public static void main(String[] args) throws Exception
    {

        // This is used to ensure that the required DKPRO_HOME environment
        // variable is set.
        // Ensures that people can run the experiments even if they haven't read
        // the setup
        // instructions first :)
        DemoUtils.setDkproHome(CRFSuiteBrownPosDemo.class.getSimpleName());

        Map<String, Object> config = new HashMap<>();
        config.put(DIM_CLASSIFICATION_ARGS, new Object[] { new CrfSuiteAdapter(), CrfSuiteAdapter.ALGORITHM_ADAPTIVE_REGULARIZATION_OF_WEIGHT_VECTOR });
        config.put(DIM_DATA_WRITER, new CrfSuiteAdapter().getDataWriterClass());
        config.put(DIM_FEATURE_USE_SPARSE, new CrfSuiteAdapter().useSparseFeatures());
        Dimension<Map<String, Object>> mlas = Dimension.createBundle("config", config);

        ParameterSpace pSpace = getParameterSpace(Constants.FM_SEQUENCE, Constants.LM_SINGLE_LABEL,
                mlas, null);

        CRFSuiteBrownPosDemo experiment = new CRFSuiteBrownPosDemo();
        experiment.runTrainTest(pSpace);
    }

    public static ParameterSpace getParameterSpace(String featureMode, String learningMode,
            Dimension<Map<String, Object>> config, Dimension<List<String>> dimFilters)
        throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription train = CollectionReaderFactory.createReaderDescription(
                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en", TeiReader.PARAM_SOURCE_LOCATION,
                corpusFilePathTrain, TeiReader.PARAM_PATTERNS, "a01.xml");
        dimReaders.put(DIM_READER_TRAIN, train);

        CollectionReaderDescription test = CollectionReaderFactory.createReaderDescription(
                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en", TeiReader.PARAM_SOURCE_LOCATION,
                corpusFilePathTrain, TeiReader.PARAM_PATTERNS, "a02.xml");
        dimReaders.put(DIM_READER_TEST, test);

        TcFeatureSet tcFeatureSet = new TcFeatureSet(
                TcFeatureFactory.create(TokenLengthRatio.class),
                TcFeatureFactory.create(CharacterNGram.class, CharacterNGram.PARAM_NGRAM_MIN_N, 2,
                        CharacterNGram.PARAM_NGRAM_MAX_N, 4, CharacterNGram.PARAM_NGRAM_USE_TOP_K,
                        50));

        ExperimentBuilder builder = new ExperimentBuilder();
        builder.addFeatureSet(tcFeatureSet);
        builder.setLearningMode(LearningMode.SINGLE_LABEL);
        builder.setFeatureMode(FeatureMode.SEQUENCE);
        builder.addAdapterConfiguration(new CrfSuiteAdapter(), CrfSuiteAdapter.ALGORITHM_L2_STOCHASTIC_GRADIENT_DESCENT);
        builder.setReaders(dimReaders);
        ParameterSpace pSpace = builder.buildParameterSpace();
        
        return pSpace;
    }

    // ##### CV #####
    public void runCrossValidation(ParameterSpace pSpace) throws Exception
    {

        ExperimentCrossValidation experiment = new ExperimentCrossValidation(
                "BrownPosDemoCV_CRFSuite", NUM_FOLDS);
        experiment.setPreprocessing(getPreprocessing());
        experiment.setParameterSpace(pSpace);
        experiment.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        experiment.addReport(BatchCrossValidationReport.class);

        // Run
        Lab.getInstance().run(experiment);
    }

    protected AnalysisEngineDescription getPreprocessing() throws ResourceInitializationException
    {
        return createEngineDescription(SequenceOutcomeAnnotator.class);
    }

    public void runTrainTest(ParameterSpace pSpace) throws Exception
    {
        ExperimentTrainTest experiment = new ExperimentTrainTest("BrownTrainTest");
        experiment.setParameterSpace(pSpace);
        experiment.setPreprocessing(getPreprocessing());
        experiment.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        experiment.addReport(BatchTrainTestReport.class);
        experiment.addReport(ContextMemoryReport.class);

        Lab.getInstance().run(experiment);
    }
}
