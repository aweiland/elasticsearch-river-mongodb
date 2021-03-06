/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.river.mongodb.simple;

import static org.elasticsearch.client.Requests.countRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.river.mongodb.RiverMongoDBTestAbstract;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

public class RiverMongoStoreStatisticsTest extends RiverMongoDBTestAbstract {

    private DB mongoDB;
    private DBCollection mongoCollection;
    private final String storeStatsIndex = "stats-index-" + System.currentTimeMillis();
    private final String storeStatsType = "stats" + System.currentTimeMillis();

    @Test
    public void testStoreStatistics() throws Throwable {
        logger.debug("Start testStoreStatistics");
        try {

            DBObject dbObject1 = new BasicDBObject(ImmutableMap.of("name", "Richard"));
            WriteResult result1 = mongoCollection.insert(dbObject1);
            logger.info("WriteResult: {}", result1.toString());
            Thread.sleep(wait);

            ActionFuture<IndicesExistsResponse> response = getNode().client().admin().indices()
                    .exists(new IndicesExistsRequest(getIndex()));
            assertThat(response.actionGet().isExists(), equalTo(true));
            refreshIndex();
            assertThat(getNode().client().count(countRequest(getIndex())).actionGet().getCount(), equalTo(1l));

            assertThat(getNode().client().admin().indices().prepareExists(storeStatsIndex).get().isExists(), equalTo(true));

            assertThat(getNode().client().admin().indices().prepareTypesExists(storeStatsIndex).setTypes(storeStatsType).get().isExists(),
                    equalTo(true));

            deleteRiver();
            createRiver();

            Thread.sleep(wait);
        } catch (Throwable t) {
            logger.error("testStoreStatistics failed.", t);
            t.printStackTrace();
            throw t;
        }
    }

    @BeforeTest
    void setUp() {
        createDatabase();
        createRiver();
    }

    private void createDatabase() {
        logger.debug("createDatabase {}", getDatabase());
        try {
            mongoDB = getMongo().getDB(getDatabase());
            mongoDB.setWriteConcern(WriteConcern.REPLICAS_SAFE);
            logger.info("Start createCollection");
            mongoCollection = mongoDB.createCollection(getCollection(), null);
            Assert.assertNotNull(mongoCollection);
        } catch (Throwable t) {
            logger.error("createDatabase failed.", t);
        }
    }

    private void createRiver() {
        try {
            super.createRiver(TEST_MONGODB_RIVER_STORE_STATISTICS_JSON, getRiver(), (Object) String.valueOf(getMongoPort1()),
                    (Object) storeStatsIndex, (Object) storeStatsType, (Object) getDatabase(), (Object) getCollection(),
                    (Object) getIndex(), (Object) getDatabase());

        } catch (Exception ex) {
        }
    }

    @AfterTest
    void cleanUp() {
        super.deleteRiver();
        Assert.assertTrue(getNode().client().admin().indices().prepareDelete(storeStatsIndex).get().isAcknowledged());
        logger.info("Drop database " + mongoDB.getName());
        mongoDB.dropDatabase();
    }

}
