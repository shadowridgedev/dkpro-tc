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
package org.dkpro.tc.examples.shallow.serialization.libsvm;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.Lab;
import org.dkpro.lab.task.BatchTask.ExecutionPolicy;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.api.features.TcFeatureFactory;
import org.dkpro.tc.api.features.TcFeatureSet;
import org.dkpro.tc.api.type.TextClassificationOutcome;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.TestCaseSuperClass;
import org.dkpro.tc.examples.shallow.annotators.UnitOutcomeAnnotator;
import org.dkpro.tc.features.maxnormalization.TokenRatioPerDocument;
import org.dkpro.tc.features.ngram.CharacterNGram;
import org.dkpro.tc.features.ngram.WordNGram;
import org.dkpro.tc.io.FolderwiseDataReader;
import org.dkpro.tc.ml.ExperimentSaveModel;
import org.dkpro.tc.ml.libsvm.LibsvmAdapter;
import org.dkpro.tc.ml.uima.TcAnnotator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.tei.TeiReader;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class LibsvmSaveAndLoadModelDocumentSingleLabelTest
    extends TestCaseSuperClass
    implements Constants
{
    static String documentTrainFolder = "src/main/resources/data/twitter/train";
    static String documentTestFolder = "src/main/resources/data/twitter/test";
    static String unitTrainFolder = "src/main/resources/data/brown_tei/";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private ParameterSpace documentGetParameterSpaceSingleLabel(boolean useClassificationArguments)
        throws ResourceInitializationException
    {
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                FolderwiseDataReader.class, FolderwiseDataReader.PARAM_SOURCE_LOCATION,
                documentTrainFolder, FolderwiseDataReader.PARAM_LANGUAGE, "en",
                FolderwiseDataReader.PARAM_PATTERNS, "*/*.txt");
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        Map<String, Object> config = new HashMap<>();
        config.put(DIM_CLASSIFICATION_ARGS, new Object[] { new LibsvmAdapter(), "-c", "100" });
        config.put(DIM_DATA_WRITER, new LibsvmAdapter().getDataWriterClass());
        config.put(DIM_FEATURE_USE_SPARSE, new LibsvmAdapter().useSparseFeatures());
        Dimension<Map<String, Object>> mlas = Dimension.createBundle("config", config);

        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(DIM_FEATURE_SET,
                new TcFeatureSet(TcFeatureFactory.create(TokenRatioPerDocument.class),
                        TcFeatureFactory.create(WordNGram.class, WordNGram.PARAM_NGRAM_USE_TOP_K,
                                50, WordNGram.PARAM_NGRAM_MIN_N, 1, WordNGram.PARAM_NGRAM_MAX_N,
                                3)));

        ParameterSpace pSpace;
        if (useClassificationArguments) {
            pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                    Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                    Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), mlas, dimFeatureSets);
        }
        else {

            config = new HashMap<>();
            config.put(DIM_CLASSIFICATION_ARGS, new Object[] { new LibsvmAdapter() });
            config.put(DIM_DATA_WRITER, new LibsvmAdapter().getDataWriterClass());
            config.put(DIM_FEATURE_USE_SPARSE, new LibsvmAdapter().useSparseFeatures());
            mlas = Dimension.createBundle("config", config);

            pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                    Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                    Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), dimFeatureSets, mlas);
        }
        return pSpace;
    }

    @Test
    public void documentRoundTripTest() throws Exception
    {

        File modelFolder = folder.newFolder();

        ParameterSpace docParamSpace = documentGetParameterSpaceSingleLabel(false);
        documentTrainAndStoreModel(docParamSpace, modelFolder);
        documentLoadAndUseModel(modelFolder, false);
        documentVerifyCreatedModelFiles(modelFolder);

        docParamSpace = documentGetParameterSpaceSingleLabel(true);
        documentTrainAndStoreModel(docParamSpace, modelFolder);
        documentLoadAndUseModel(modelFolder, true);

        modelFolder.deleteOnExit();
    }

    private void documentVerifyCreatedModelFiles(File modelFolder)
    {
        File classifierFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_CLASSIFIER);
        assertTrue(classifierFile.exists());

        File metaOverride = new File(modelFolder.getAbsolutePath() + "/" + META_COLLECTOR_OVERRIDE);
        assertTrue(metaOverride.exists());

        File extractorOverride = new File(
                modelFolder.getAbsolutePath() + "/" + META_EXTRACTOR_OVERRIDE);
        assertTrue(extractorOverride.exists());

        File modelMetaFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_META);
        assertTrue(modelMetaFile.exists());

        File featureMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_MODE);
        assertTrue(featureMode.exists());

        File learningMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_LEARNING_MODE);
        assertTrue(learningMode.exists());

        File id2outcomeMapping = new File(
                modelFolder.getAbsolutePath() + "/" + LibsvmAdapter.getOutcomeMappingFilename());
        assertTrue(id2outcomeMapping.exists());
    }

    private static void documentTrainAndStoreModel(ParameterSpace paramSpace, File modelFolder)
        throws Exception
    {
        ExperimentSaveModel batch = new ExperimentSaveModel("TestSaveModel", modelFolder);
        batch.setPreprocessing(
                createEngineDescription(createEngineDescription(BreakIteratorSegmenter.class)));
        batch.setParameterSpace(paramSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        Lab.getInstance().run(batch);
    }

    private static void documentLoadAndUseModel(File modelFolder,
            boolean evaluateWithClassificationArgs)
        throws Exception
    {
        AnalysisEngine tokenizer = AnalysisEngineFactory.createEngine(BreakIteratorSegmenter.class);

        AnalysisEngine tcAnno = AnalysisEngineFactory.createEngine(TcAnnotator.class,
                TcAnnotator.PARAM_TC_MODEL_LOCATION, modelFolder.getAbsolutePath());

        CollectionReader reader = CollectionReaderFactory.createReader(TextReader.class,
                TextReader.PARAM_SOURCE_LOCATION, documentTestFolder, TextReader.PARAM_LANGUAGE,
                "en", TextReader.PARAM_PATTERNS,
                Arrays.asList(TextReader.INCLUDE_PREFIX + "*/*.txt"));

        List<TextClassificationOutcome> outcomes = new ArrayList<>();
        while (reader.hasNext()) {
            JCas jcas = JCasFactory.createJCas();
            reader.getNext(jcas.getCas());
            jcas.setDocumentLanguage("en");

            tokenizer.process(jcas);
            tcAnno.process(jcas);

            outcomes.add(JCasUtil.selectSingle(jcas, TextClassificationOutcome.class));
        }

        assertEquals(4, outcomes.size());

        if (evaluateWithClassificationArgs) {
            assertEquals(4, outcomes.size());
            assertEquals("neutral", outcomes.get(0).getOutcome());
            assertEquals("neutral", outcomes.get(1).getOutcome());
            assertEquals("neutral", outcomes.get(2).getOutcome());
            assertEquals("neutral", outcomes.get(3).getOutcome());
        }
        else {
            assertEquals(4, outcomes.size());
            assertEquals("emotional", outcomes.get(0).getOutcome());
            assertEquals("emotional", outcomes.get(1).getOutcome());
            assertEquals("emotional", outcomes.get(2).getOutcome());
            assertEquals("emotional", outcomes.get(3).getOutcome());
        }
    }

    @Test
    public void unitRoundTripTest() throws Exception
    {

        File modelFolder = folder.newFolder();

        ParameterSpace unitParamSpace = unitGetParameterSpaceSingleLabel();
        unitTrainAndStoreModel(unitParamSpace, modelFolder);
        unitLoadAndUseModel(modelFolder);
        unitVerifyCreatedModelFiles(modelFolder);

        modelFolder.deleteOnExit();
    }

    public static ParameterSpace unitGetParameterSpaceSingleLabel()
        throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                TeiReader.class, TeiReader.PARAM_LANGUAGE, "en",
                TeiReader.PARAM_SOURCE_LOCATION, unitTrainFolder,
                TeiReader.PARAM_PATTERNS, "a01.xml" );

        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        Map<String, Object> config = new HashMap<>();
        config.put(DIM_CLASSIFICATION_ARGS, new Object[] { new LibsvmAdapter(), "-c", "1000" });
        config.put(DIM_DATA_WRITER, new LibsvmAdapter().getDataWriterClass());
        config.put(DIM_FEATURE_USE_SPARSE, new LibsvmAdapter().useSparseFeatures());
        Dimension<Map<String, Object>> mlas = Dimension.createBundle("config", config);

        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(DIM_FEATURE_SET,
                new TcFeatureSet(TcFeatureFactory.create(TokenRatioPerDocument.class),
                        TcFeatureFactory.create(CharacterNGram.class)));

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_UNIT), dimFeatureSets, mlas);

        return pSpace;
    }

    private static void unitLoadAndUseModel(File modelFolder) throws Exception
    {
        AnalysisEngine tcAnno = AnalysisEngineFactory.createEngine(TcAnnotator.class,
                TcAnnotator.PARAM_TC_MODEL_LOCATION, modelFolder.getAbsolutePath(),
                TcAnnotator.PARAM_NAME_UNIT_ANNOTATION, Token.class.getName());

        CollectionReader reader = CollectionReaderFactory.createReader(TeiReader.class,
                TeiReader.PARAM_SOURCE_LOCATION, unitTrainFolder, TeiReader.PARAM_LANGUAGE, "en",
                TeiReader.PARAM_PATTERNS, Arrays.asList(TeiReader.INCLUDE_PREFIX + "a02.xml"));

        List<TextClassificationOutcome> outcomes = new ArrayList<>();
        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentLanguage("en");
        reader.getNext(jcas.getCas());

        tcAnno.process(jcas);

        outcomes.addAll(JCasUtil.select(jcas, TextClassificationOutcome.class));

        // int i=0;
        // for(TextClassificationOutcome o: outcomes){
        // System.out.println("assertEquals(\"" + o.getOutcome() + "\",
        // outcomes.get("+(i++)+").getOutcome());");
        // }

        assertEquals(31, outcomes.size());
        for(TextClassificationOutcome o : outcomes) {
            assertTrue(o.getOutcome().matches("[A-Za-z]+"));
        }
    }

    private static void unitTrainAndStoreModel(ParameterSpace paramSpace, File modelFolder)
        throws Exception
    {
        ExperimentSaveModel experiment = new ExperimentSaveModel("UnitLibsvmTestSaveModel", modelFolder);
        experiment.setParameterSpace(paramSpace);
        experiment.setPreprocessing(AnalysisEngineFactory.createEngineDescription(UnitOutcomeAnnotator.class));
        experiment.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        Lab.getInstance().run(experiment);
    }

    private void unitVerifyCreatedModelFiles(File modelFolder)
    {
        File classifierFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_CLASSIFIER);
        assertTrue(classifierFile.exists());

        File metaOverride = new File(modelFolder.getAbsolutePath() + "/" + META_COLLECTOR_OVERRIDE);
        assertTrue(metaOverride.exists());

        File extractorOverride = new File(
                modelFolder.getAbsolutePath() + "/" + META_EXTRACTOR_OVERRIDE);
        assertTrue(extractorOverride.exists());

        File modelMetaFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_META);
        assertTrue(modelMetaFile.exists());

        File featureMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_MODE);
        assertTrue(featureMode.exists());

        File learningMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_LEARNING_MODE);
        assertTrue(learningMode.exists());

        File id2outcomeMapping = new File(
                modelFolder.getAbsolutePath() + "/" + LibsvmAdapter.getOutcomeMappingFilename());
        assertTrue(id2outcomeMapping.exists());

        File featureNameMapping = new File(modelFolder.getAbsolutePath() + "/"
                + LibsvmAdapter.getFeatureNameMappingFilename());
        assertTrue(featureNameMapping.exists());
    }
}