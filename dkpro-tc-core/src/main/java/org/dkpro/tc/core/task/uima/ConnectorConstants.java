/*******************************************************************************
 * Copyright 2018
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
package org.dkpro.tc.core.task.uima;

/**
 * Defines names of parameters.
 * Names that are defined in more than one task should be defined here
 */
public interface ConnectorConstants
{
    /**
     * Array of feature extractors to be used
     */
    static final String PARAM_FEATURE_EXTRACTORS = "featureExtractors";

    /**
     * Array of outcomes
     */
    static final String PARAM_OUTCOMES = "occurringOutcomes";
    
    /**
     * Array of feature filters to be used
     */
    static final String PARAM_FEATURE_FILTERS = "featureFilters";
    
    /**
     * The data writer class to be used for writing features
     */
    static final String PARAM_DATA_WRITER_CLASS = "dataWriterClass";

    /**
     * The learning mode, e.g. single-label, multi-label or regression
     */
    static final String PARAM_LEARNING_MODE = "learningMode";

    /**
     * The feature mode, e.g. document, pair, unit, or sequence
     */
    static final String PARAM_FEATURE_MODE = "featureMode";
    
    /**
     * In case of multi-labeling, the threshold used to create bipartitions from rankings
     */
    static final String PARAM_BIPARTITION_THRESHOLD = "bipartitionThreshold";

    /**
     * Whether we are extracting for training or testing. May e.g. have consequences when applying filters.
     */
    static final String PARAM_IS_TESTING = "isTesting";
    /**
     * Whether to turn on instance weighting.  If true, user should override addweight 
     * in the reader.
     */
    static final String PARAM_APPLY_WEIGHTING = "applyWeights";
    
    /**
     * If a sparse feature representation shall be used
     */
    static final String PARAM_USE_SPARSE_FEATURES= "useSparseFeatures";
    
    /**
     * If a sparse feature representation shall be used
     */
    static final String PARAM_REQUIRED_TYPES = "requiredTypes";


}