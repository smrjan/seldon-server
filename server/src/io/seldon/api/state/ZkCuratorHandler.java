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

package io.seldon.api.state;

import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class ZkCuratorHandler implements ConnectionStateListener {

    private static Logger logger = Logger.getLogger(ZkCuratorHandler.class.getName());
    private static ZkCuratorHandler peer;
    final private static String ENV_VAR_SELDON_ZKSERVERS = "SELDON_ZKSERVERS";

    public ZkCuratorHandler() {
        String servers = null;
        servers = System.getenv(ENV_VAR_SELDON_ZKSERVERS);
        logger.info(String.format("using %s[%s]", ENV_VAR_SELDON_ZKSERVERS, servers));
        if (servers == null) {
            logger.warn("*WARNING* SELDON_ZKSERVERS environment variable not set!");
            servers = "localhost";
        }
        this.zkServers = servers;
        startClient();
        peer = this;
    }

    public static void shutdown() {
        if (peer != null)
            peer.stopClient();
    }

    public static ZkCuratorHandler getPeer() {
        return peer;
    }

    CuratorFramework curator;
    String zkServers;

    private void stopClient() {
        curator.close();
    }

    private void startClient() {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
        logger.info("Trying to connect to servers at " + zkServers);
        curator = builder.connectString(zkServers).retryPolicy(new ExponentialBackoffRetry(1000, 100)).build();
        curator.getConnectionStateListenable().addListener(this);
        curator.start();
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState state) {
        switch (state) {
        case RECONNECTED: {
            logger.warn("Reconnection to zookeeper " + zkServers);
        }
            break;
        case LOST: {
            logger.error("Connection lost to zookeeper " + zkServers);
        }
            break;
        case CONNECTED: {
            logger.info("Connection to zookeeper " + zkServers);
        }
            break;
        case SUSPENDED: {
            logger.error("Connection suspended to zookeeper " + zkServers);
        }
            break;
        }

    }

    public static void dump_env() {
        Map<String, String> env = System.getenv();
        logger.info("*** ENV ***");
        StringBuilder lines = new StringBuilder();
        for (String envName : env.keySet()) {
            lines.append(String.format("%s=%s%n", envName, env.get(envName)));
        }
        logger.info(lines.toString());
    }
}
