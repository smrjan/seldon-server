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

package io.seldon.api.service.async;

import io.seldon.api.state.NewClientListener;
import io.seldon.api.state.options.DefaultOptions;
import io.seldon.api.state.zk.ZkClientConfigHandler;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import io.seldon.db.jdo.DbConfigHandler;
import io.seldon.db.jdo.DbConfigListener;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JdoAsyncActionFactory implements DbConfigListener{

	
	private static final String DEF_HOSTNAME = "TEST";
	private static Logger logger = Logger.getLogger(JdoAsyncActionFactory.class.getName());
	private static boolean active = false;
	
	
	private ConcurrentHashMap<String,AsyncActionQueue> queues = new ConcurrentHashMap<>();
	
	private static int DEF_QTIMEOUT_SECS = 5;
	private static int DEF_MAXQSIZE = 100000;
	private static int DEF_BATCH_SIZE = 2000;
	private static int DEF_DB_RETRIES = 3;
	private static boolean DEF_RUN_USERITEM_UPDATES = true;
	private static boolean DEF_UPDATE_IDS_ACTION_TABLE = true;
	private static boolean DEF_INSERT_ACTIONS = true;
	
	public static final String ASYNC_PROP_PREFIX = "io.seldon.asyncactions";
	private static Properties props;
	
	private static boolean asyncUserWrites = true;
	private static boolean asyncItemWrites = true;
	
	public static boolean isActive() {
		return active;
	}

	DefaultOptions options;
	
	@Autowired
	public JdoAsyncActionFactory(DefaultOptions options,DbConfigHandler dbConfigHandler)
	{
		this.options = options;
		dbConfigHandler.addDbConfigListener(this);
	}
	
	public void clientDeleted(String client) {
		logger.info("Removing client:"+client);
		AsyncActionQueue q = queues.get(client);
		if (q != null)
		{
			q.setKeepRunning(false);
			queues.remove(client);
		}
		else
			logger.warn("Unknown client - can't remove "+client);
	}
	
	
	
	
	private AsyncActionQueue create(String client)
	{
		int qTimeout = DEF_QTIMEOUT_SECS;
		int maxqSize = DEF_MAXQSIZE;
		int batchSize = DEF_BATCH_SIZE;
		int dbRetries = DEF_DB_RETRIES;
		boolean runUserItemUpdates = DEF_RUN_USERITEM_UPDATES;
		boolean runUpdateIdsActionTable = DEF_UPDATE_IDS_ACTION_TABLE;
		boolean insertActions = DEF_INSERT_ACTIONS;
		
		return create(client,qTimeout,batchSize,maxqSize,dbRetries,runUserItemUpdates,runUpdateIdsActionTable,insertActions);
	}
	
	private static AsyncActionQueue create(String client,int qtimeoutSecs,int batchSize,int qSize,int dbRetries,boolean runUserItemUpdates,boolean runUpdateIdsActionTable,boolean insertActions)
	{
		JdoAsyncActionQueue q = new JdoAsyncActionQueue(client,qtimeoutSecs,batchSize,qSize,dbRetries,runUserItemUpdates,runUpdateIdsActionTable,insertActions);
		Thread t = new Thread(q);
		t.start();
		return q;
	}
	
	public AsyncActionQueue get(String client)
	{
		AsyncActionQueue queue = queues.get(client);
		if (queue == null)
		{
			queues.putIfAbsent(client, create(client));
			queue = get(client);
		}
		return queue;
	}

	public static boolean isAsyncUserWrites() {
		return asyncUserWrites;
	}

	public static void setAsyncUserWrites(boolean asyncUserWrites) {
		JdoAsyncActionFactory.asyncUserWrites = asyncUserWrites;
	}

	public static boolean isAsyncItemWrites() {
		return asyncItemWrites;
	}

	public static void setAsyncItemWrites(boolean asyncItemWrites) {
		JdoAsyncActionFactory.asyncItemWrites = asyncItemWrites;
	}
	
	private void shutdownQueues()
	{
		for(Map.Entry<String,AsyncActionQueue> e : queues.entrySet())
		{
			e.getValue().setKeepRunning(false);
		}
	}


	@Override
	public void dbConfigInitialised(String client) {
		logger.info("Adding client: "+client);
		get(client);
	}
}
