/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.dkpro.tc.features.length;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.dkpro.tc.api.exception.TextClassificationException;
import org.dkpro.tc.api.features.ClassificationUnitFeatureExtractor;
import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.features.FeatureExtractorResource_ImplBase;
import org.dkpro.tc.api.features.MissingValue;
import org.dkpro.tc.api.features.MissingValue.MissingValueNonNominalType;
import org.dkpro.tc.api.type.TextClassificationUnit;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Extracts the number of characters in the document, per sentence, and per token
 */
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token" })
public class NrOfChars
    extends FeatureExtractorResource_ImplBase
    implements ClassificationUnitFeatureExtractor
{

    /**
     * Public name of the feature "number of characters"
     */
    public static final String FN_NR_OF_CHARS = "NrofChars";
    /**
     * Public name of the feature "number of characters per sentence"
     */
    public static final String FN_NR_OF_CHARS_PER_SENTENCE = "NrofCharsPerSentence";
    /**
     * Public name of the feature "number of characters per token"
     */
    public static final String FN_NR_OF_CHARS_PER_TOKEN = "NrofCharsPerToken";

    @Override
    public Set<Feature> extract(JCas jcas, TextClassificationUnit u)
        throws TextClassificationException
    {
        double nrOfChars = jcas.getDocumentText().length();
        double nrOfSentences = JCasUtil.selectCovered(jcas, Sentence.class, u).size();
        double nrOfTokens = JCasUtil.selectCovered(jcas, Token.class, u).size();

        Set<Feature> features = new HashSet<Feature>();
        features.add(new Feature(FN_NR_OF_CHARS, nrOfChars));

        if (nrOfSentences == 0) {
            features.add(new Feature(FN_NR_OF_CHARS_PER_SENTENCE,
                    new MissingValue(MissingValueNonNominalType.NUMERIC)));
        }
        else {
            features.add(new Feature(FN_NR_OF_CHARS_PER_SENTENCE, nrOfChars / nrOfSentences));
        }

        if (nrOfTokens == 0) {
            features.add(new Feature(FN_NR_OF_CHARS_PER_TOKEN,
                    new MissingValue(MissingValueNonNominalType.NUMERIC)));
        }
        else {
            features.add(new Feature(FN_NR_OF_CHARS_PER_TOKEN, nrOfChars / nrOfTokens));
        }
        return features;
    }
}