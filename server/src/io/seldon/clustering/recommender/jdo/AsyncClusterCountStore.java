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

package io.seldon.clustering.recommender.jdo;

import io.seldon.db.jdbc.JDBCConnectionFactory;
import io.seldon.general.Action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class AsyncClusterCountStore implements Runnable {

	public static class ClusterCount {
		public int clusterId;
		public long itemId;
		public double weight;
		public ClusterCount(int clusterId, long itemId,
				double weight) {
			super();
			this.clusterId = clusterId;
			this.itemId = itemId;
			this.weight = weight;
		}
		
		
	}
	
	private static Logger logger = Logger.getLogger(AsyncClusterCountStore.class.getName());
    private String client;
    private int timeout; 
    private LinkedBlockingQueue<ClusterCount> queue;
    private int batchSize; // batch size for sql statements
    private int maxDBRetries = 1; // max # of times to try sql statement on exception
    boolean keepRunning;
    double decay = 3600;
    Connection connection = null;
    
    PreparedStatement countPreparedStatement;
    private int countsAdded = 0; // actions added so far to sql statement
    private int countsAddedTotal = 0; // total actions added including counts for same cluster and item (will be different than counts added for cached version that uses db time)
     
    long lastSqlRunTime = 0;
    int badActions = 0;
    boolean useDBTime = true;
    TreeMap<Integer,TreeMap<Long,Double>> clusterCounts;
    
    public AsyncClusterCountStore(String client, int qTimeoutSecs, int batchSize, int maxQSize,int maxDBRetries,double decay,boolean useDBTime) {
        this.client = client;
        this.batchSize = batchSize;
        this.maxDBRetries = maxDBRetries;
        this.queue = new LinkedBlockingQueue<>(maxQSize);
        this.timeout = qTimeoutSecs;
        this.decay = decay;
        clusterCounts = new TreeMap<>();
        this.useDBTime = useDBTime;
        logger.info("Async cluster count created for client "+client+" qTimeout:"+qTimeoutSecs+" batchSize:"+batchSize+" maxQSize:"+maxQSize+" maxDBRetries:"+maxDBRetries+" decay:"+decay+" use DB Time:"+useDBTime);
    }

    public void run() {
    	keepRunning = true;
    	this.lastSqlRunTime = System.currentTimeMillis();
        while (true) 
        {
            try 
            {
                ClusterCount count = queue.poll(timeout, TimeUnit.SECONDS);
                if (count != null)
                {
                	if (useDBTime)
                		addCount(count);
                	else
                		addSQL(count);
                }
                long timeSinceLastSQLRun = (System.currentTimeMillis() - this.lastSqlRunTime)/1000;
                boolean runSQL = false;
                if ((count == null && countsAdded > 0))
                {
                	runSQL = true;
                	logger.info("Run sql as timeout on poll and actionsAdded > 0");
                }
                else if (countsAdded >= batchSize)
                {
                	runSQL = true;
                	logger.info("Run sql as batch size exceeded");
                }
                else if (timeSinceLastSQLRun > timeout && countsAdded > 0)
                {
                	runSQL = true;
                	logger.info("Run sql as time between sql runs exceeded");
                }
                if (runSQL)
                    runSQL();
                if (!keepRunning && count == null)
                {
                	logger.warn("Asked to stop as keepRunning is false");
                	return;
                }

            } 
            catch (InterruptedException e) {
            	logger.error("Received interrupted exception - will stop",e);
                return;
            }
            catch (Exception e)
            {
            	logger.error("Caught exception while running ", e);
                resetState();
                logger.warn("\\-> Reset buffers.");
            }
            catch (Throwable t)
            {
                logger.error("Caught throwable while running ", t);
                resetState();
                logger.warn("\\-> Reset buffers.");
            }
        }
    }

    private void resetState() {
    	clusterCounts = new TreeMap<>();
        clearSQLState();
        countsAdded = 0;
        countsAddedTotal = 0;
        this.lastSqlRunTime = System.currentTimeMillis();
    }
    
    private void clearSQLState()
    {
    	try
		{
    		if (connection != null)
    		{
    			try{connection.close();}
    			catch( SQLException exception )
    			{
    				logger.error("Unable to close connection",exception);
    			}
    		}
    		if (countPreparedStatement != null)
    		{
    			try{countPreparedStatement.close();}
    			catch( SQLException exception )
    			{
    				logger.error("Unable to close action perpared statment",exception);
    			}
    		}

		}
		finally
		{
			connection = null;
			countPreparedStatement = null;
		}
		
    }
    
    private void executeBatch() throws SQLException
    {
    	if (countsAdded > 0)
    	{
    		countPreparedStatement.executeBatch();
    		countPreparedStatement.close();
			countsAdded = 0;

    		connection.commit();
    	}
    }
    
    private void rollBack()
    {
    	try
		{
			connection.rollback();
		}
		catch( SQLException re )
		{
			logger.error("Can't roll back transaction",re);
		}
    }
   
    private void runSQL() throws SQLException 
    {
    	int sqlAdded = 0;
    	int localActionsAdded = this.countsAdded;
    	if (useDBTime)
    	{
    		addSQLs();
    		sqlAdded = this.countsAdded; 
        	localActionsAdded = this.countsAddedTotal;
    	}
    	else
    	{
    		sqlAdded = this.countsAdded;
        	localActionsAdded = this.countsAdded;
    	}
    	long t1 = System.currentTimeMillis();

    	boolean success = false;
        for (int i = 0; i < this.maxDBRetries; i++)
        {
        	try
    		{
    			executeBatch();
    			success = true;
    			break;
            }
    		catch (SQLException e) {
                logger.error("Failed to run update ",e);
			}
    	}
        if (!success)
        {
        	rollBack();
        	localActionsAdded = 0;
        }
        resetState();
        long t2 = System.currentTimeMillis();
        //log q size
        float compression = localActionsAdded > 0 ? (1.0f-(sqlAdded/(float)localActionsAdded)) : 0.0f;
        logger.info("Asyn count for "+client+" at size:"+queue.size()+" actions added "+localActionsAdded+" unique sql inserts "+sqlAdded + " compression " + compression +" time to process:"+(t2-t1));
    }
    
    /**
     * Allowed operations to fill in nulls in Action
     * @param action
     */
    private void repairAction(Action action)
    {
        if (action.getTimes() == null)
                action.setTimes(1);
    }

    
    private void getConnectionIfNeeded() throws SQLException
    {
    	if (connection == null)
    	{
    		connection = JDBCConnectionFactory.get().getConnection(client);
    		connection.setAutoCommit( false );
    	}
    }
    
   

    private void addCount(ClusterCount count)
    {
    	TreeMap<Long,Double> clusterMap = clusterCounts.get(count.clusterId);
    	if (clusterMap == null)
    	{
    		clusterMap = new TreeMap<>();
    		clusterMap.put(count.itemId, count.weight);
    		clusterCounts.put(count.clusterId, clusterMap);
    		countsAdded++;
    	}
    	else
    	{
    		Double presValue = clusterMap.get(count.itemId);
    		if (presValue == null)
    		{
    			clusterMap.put(count.itemId, count.weight);
    			countsAdded++;
    		}
    		else
    			clusterMap.put(count.itemId, presValue + count.weight);
    	}
    	this.countsAddedTotal++;
    }
    
    private synchronized int addSQLs() throws SQLException
    {
    	getConnectionIfNeeded();
		
		// Add action batch
		if (countPreparedStatement == null)
			countPreparedStatement = connection.prepareStatement("insert into cluster_counts values (?,?,?,unix_timestamp()) on duplicate key update count=?+exp(-(greatest(unix_timestamp()-t,0)/?))*count,t=unix_timestamp()");
		int added = 0;
    	for(Map.Entry<Integer,TreeMap<Long,Double>> m : clusterCounts.entrySet())
    	{
    		for(Map.Entry<Long, Double> e : m.getValue().entrySet())
    		{
    			countPreparedStatement.setInt(1, m.getKey());
    			countPreparedStatement.setLong(2, e.getKey());
    			countPreparedStatement.setDouble(3, e.getValue());
    			countPreparedStatement.setDouble(4, e.getValue());
    			countPreparedStatement.setDouble(5, decay);
    			
    			countPreparedStatement.addBatch();
    			added++;
    		}
    	}
    	logger.info("Added "+added+" sql inserts to run ");
    	clusterCounts = new TreeMap<>();
    	return added;
    }
    
    
    
    private synchronized void addActionBatch(ClusterCount count) throws SQLException
    {
    	long time = System.currentTimeMillis();
    	countPreparedStatement.setInt(1, count.clusterId);
		countPreparedStatement.setLong(2, count.itemId);
		countPreparedStatement.setDouble(3, count.weight);
		countPreparedStatement.setLong(4,  time);
		countPreparedStatement.setDouble(5, count.weight);
		countPreparedStatement.setLong(6, time);
		countPreparedStatement.setDouble(7, decay);
		countPreparedStatement.setLong(8,  time);
    }

    private void addSQL(ClusterCount count) throws SQLException {
    	
    		getConnectionIfNeeded();
    		
    		// Add action batch
    		if (countPreparedStatement == null)
    			countPreparedStatement = connection.prepareStatement("insert into cluster_counts values (?,?,?,?) on duplicate key update count=?+exp(-(greatest(?-t,0)/?))*count,t=?");
    		addActionBatch(count);
    		countPreparedStatement.addBatch();
    		countsAdded++;
    		
    		
       
    }
    

    public void put(ClusterCount count) {
        queue.add(count);
    }
    
    public int getQSize()
    {
    	return queue.size();
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getActionsAdded() {
        return countsAdded;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

	public int getMaxDBRetries() {
		return maxDBRetries;
	}

	public void setMaxDBRetries(int maxDBRetries) {
		this.maxDBRetries = maxDBRetries;
	}

	public boolean isKeepRunning() {
		return keepRunning;
	}

	public void setKeepRunning(boolean keepRunning) {
		this.keepRunning = keepRunning;
	}

	public int getBadActions() {
		return badActions;
	}

	public double getDecay() {
		return decay;
	}

	public synchronized void setDecay(double decay) {
		this.decay = decay;
	}
    
	

}
