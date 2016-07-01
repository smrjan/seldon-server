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

package io.seldon.recommendation;

import java.io.Serializable;
import java.util.List;

public class RecommendationResult implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	List<Recommendation> recs;
	String uuid;
	String cohort;
	public RecommendationResult(List<Recommendation> recs, String uuid, String cohort) {
		super();
		this.recs = recs;
		this.uuid = uuid;
		this.cohort = cohort;
	}
	public List<Recommendation> getRecs() {
		return recs;
	}
	public void setRecs(List<Recommendation> recs) {
		this.recs = recs;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getCohort() {
		return cohort;
	}

	public void setCohort(String cohort) {
		this.cohort = cohort;
	}
}
