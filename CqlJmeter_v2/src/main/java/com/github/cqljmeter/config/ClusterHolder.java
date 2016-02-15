package com.github.cqljmeter.config;

/*
 * #%L
 * CqlJmeter
 * %%
 * Copyright (C) 2014 Mikhail Stepura
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import static com.google.common.collect.Maps.newConcurrentMap;

import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.Validate;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Session;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class ClusterHolder {
	private static final Logger log = LoggingManager.getLoggerForClass();
	
	private final Cluster cluster;
	
	private final static ConcurrentMap<String, ClusterHolder> CLUSTERS = newConcurrentMap();
	
	private final LoadingCache<String, Session> sessions = 
			CacheBuilder.newBuilder().build(new CacheLoader<String, Session>() {
				@Override
				public Session load(String key) throws Exception {
					log.info("New session for " + key + " in :" + Thread.currentThread());
					return cluster.connect(key);
				}}); 

	public ClusterHolder(Cluster cluster) {
		this.cluster = cluster;
	}
	
	public void shutdown() {
		this.cluster.close();
	}

	public Cluster getCluster(){
		return this.cluster;
	}
	
	public Session getSession(String keyspace) {
		return sessions.getUnchecked(keyspace);
	}

	public static void putBuilder(String clusterId, Builder builder) {
		CLUSTERS.putIfAbsent(clusterId, new ClusterHolder(builder.build()));
	}

	public static Session getSession(String clusterId, String keyspace) {
		ClusterHolder holder = CLUSTERS.get(clusterId);
		Validate.notNull(holder, "Can't obtain a cluster config. Did you forget to add C* Cluster Configuration Element?");
		return holder.getSession(keyspace);
	}

	public static void shutdownAll() {
		for (ClusterHolder cluster: CLUSTERS.values()) {
			cluster.shutdown();
		}
		CLUSTERS.clear();
	}
}
