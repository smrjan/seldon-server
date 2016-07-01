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

public class Recommendation implements Comparable<Recommendation>, Serializable{

	long content;
	int type;
	Double prediction;
	
	public Recommendation(long content, int type, Double prediction) {
		this.content = content;
		this.type = type;
		this.prediction = prediction;
	}

	public long getContent() {
		return content;
	}

	public int getType() {
		return type;
	}

	public Double getPrediction() {
		return prediction;
	}

	
	
	public void setContent(long content) {
		this.content = content;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setPrediction(Double prediction) {
		this.prediction = prediction;
	}

	

	@Override
	public int compareTo(Recommendation o) {
		if (this.prediction > o.prediction)
			return -1;
		else if (this.prediction < o.prediction)
			return 1;
		else
			return 0;
	}
	
}
