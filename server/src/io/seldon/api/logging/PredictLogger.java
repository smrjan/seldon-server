/*
 * Seldon -- open source prediction engine
 * =======================================
 * Copyright 2011-2015 Seldon Technologies Ltd and Rummble Ltd (http://www.seldon.io/)
 *
 **********************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at       
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ********************************************************************************************** 
*/
package io.seldon.api.logging;

import io.seldon.prediction.PredictionsResult;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;


public class PredictLogger {

	private static Logger predictLogger = Logger.getLogger( "PredictLogger" );
	
	public static void log(String client,String algKey,JsonNode input,PredictionsResult response)
	{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode prediction = mapper.valueToTree(response);
		ObjectNode topNode = mapper.createObjectNode();
		topNode.put("consumer", client);
		topNode.put("input", input);
		topNode.put("prediction", prediction);
		topNode.put("algorithm", algKey);
		predictLogger.info(topNode.toString());
	}
}
