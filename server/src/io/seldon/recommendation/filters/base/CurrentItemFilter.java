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

package io.seldon.recommendation.filters.base;

import io.seldon.clustering.recommender.RecommendationContext;
import io.seldon.recommendation.ItemFilter;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Filter for the item that the user is currently interacting with.
 * @author firemanphil
 *         Date: 05/12/14
 *         Time: 15:31
 */
@Component
public class CurrentItemFilter implements ItemFilter {
    @Override
    public List<Long> produceExcludedItems(String client, Long user, String clientUserId, RecommendationContext.OptionsHolder optsHolder,
                                           Long currentItem,String lastRecListUUID, int numRecommendations) {
    	if (currentItem != null)
    		return Collections.singletonList(currentItem);
    	else
    		return Collections.emptyList();
    }
}

