/*
 * Seldon -- open source prediction engine
 * =======================================
 *
 * Copyright 2011-2015 Seldon Technologies Ltd and Rummble Ltd (http://www.seldon.io/)
 *
 * ********************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ********************************************************************************************
 */

package io.seldon.recommendation.combiner;

import io.seldon.recommendation.RecommendationPeer;

import java.util.List;

/**
 * @author firemanphil
 *         Date: 23/02/15
 *         Time: 10:07
 */
public interface AlgorithmResultsCombiner {

    public boolean isEnoughResults(int numRecsRequired, List<RecommendationPeer.RecResultContext> resultsSets);

    public RecommendationPeer.RecResultContext combine(int numRecsRequired, List<RecommendationPeer.RecResultContext> resultsSets);
}
