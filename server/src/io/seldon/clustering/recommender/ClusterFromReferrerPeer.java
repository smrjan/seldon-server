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

package io.seldon.clustering.recommender;

import io.seldon.api.state.NewClientListener;
import io.seldon.api.state.options.DefaultOptions;
import io.seldon.api.state.zk.ZkClientConfigHandler;
import io.seldon.clustering.recommender.jdo.JdoClusterFromReferrer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import io.seldon.db.jdo.DbConfigHandler;
import io.seldon.db.jdo.DbConfigListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClusterFromReferrerPeer implements DbConfigListener {

	private static Logger logger = Logger.getLogger(ClusterFromReferrerPeer.class.getName());
	public static final String PROP = "io.seldon.clusters.referrer.clients";
	
	Map<String,IClusterFromReferrer> referrerHandlerMap = new ConcurrentHashMap<>();
	DefaultOptions options;
	
	@Autowired
	public ClusterFromReferrerPeer(DefaultOptions options, DbConfigHandler dbConfigHandler)
	{
		this.options = options;
		dbConfigHandler.addDbConfigListener(this);
	}
	
	private void addClient(String client)
	{
		referrerHandlerMap.put(client, new JdoClusterFromReferrer(client));
	}

	public void clientDeleted(String client) {
		logger.info("Removing client:"+client);
		IClusterFromReferrer cfr = referrerHandlerMap.get(client);
		if (cfr != null)
		{
			cfr.shutdown();
			referrerHandlerMap.remove(client);
		}
		else
			logger.warn("Unknown client - can't remove "+client);

	}
	

	public void shutdown()
	{
		for(Map.Entry<String, IClusterFromReferrer> e : referrerHandlerMap.entrySet())
			e.getValue().shutdown();
	}
	
	public IClusterFromReferrer get(String client)
	{
		return referrerHandlerMap.get(client);
	}

	@Override
	public void dbConfigInitialised(String client) {
		logger.info("Adding client: "+client);
		addClient(client);
	}
}
