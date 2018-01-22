/*******************************************************************************
* Copyright Indra Sistemas, S.A.
* 2013-2018 SPAIN
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*      http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************************/


package com.indra.sofia4cities.ssap.kp.implementations.oficials;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.indra.sofia2.ssap.json.JSON;
import com.indra.sofia2.ssap.kp.implementations.rest.SSAPResourceAPI;
import com.indra.sofia2.ssap.kp.implementations.rest.resource.SSAPResource;
import com.indra.sofia4cities.ssap.kp.logging.LogMessages;
import com.indra.sofia4cities.ssap.testutils.FixtureLoader;
import com.indra.sofia4cities.ssap.testutils.RestApiUtils;
import com.indra.sofia4cities.ssap.testutils.TestProperties;

public class TestRestApiFunctional {

	private static Logger log =  LoggerFactory.getLogger(TestRestApiFunctional.class);
	private static RestApiUtils utils = new RestApiUtils(TestRestApiFunctional.class);

	private final static String TOKEN = TestProperties.getInstance().get("test.officials.token");
	private final static String KP = TestProperties.getInstance().get("test.officials.kp");
	private final static String KP_INSTANCE = TestProperties.getInstance().get("test.officials.kp_instance") + System.currentTimeMillis();
	
	private final static String ONTOLOGY_NAME = TestProperties.getInstance().get("test.officials.ontology_name");
	
	private final static String SERVICE_URL=TestProperties.getInstance().get("test.officials.rest.url");

	
	private static JsonNode ONTOLOGY_INSTANCE;
	private static JsonNode COMMAND_REQ_INSTANCE;
	private static JsonNode ONTOLOGY_UPDATE;
	private static JsonNode ONTOLOGY_QUERY_NATIVE_CRITERIA;
	private static JsonNode ONTOLOGY_DELETE;
	
	private final static String ONTOLOGY_QUERY_NATIVE_STATEMENT = "db.TestSensorTemp.find({\"Sensor.assetId\": \"S_Temperatura_00066\"})";
	private final static String ONTOLOGY_QUERY_SQLLIKE = "select * from TestSensorTemp where Sensor.assetId = \"S_Temperatura_00066\"";
	private final static String ONTOLOGY_INSERT_SQLLIKE = "insert into TestSensorTemp(geometry, assetId, measure, timestamp) values (\"{ 'coordinates': [ 40.512967, -3.67495 ], 'type': 'Point' }\", \"S_Temperatura_00066\", 15, \"{ '$date': '2014-04-29T08:24:54.005Z'}\")";
						
	
	private SSAPResourceAPI api;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		FixtureLoader loader = new FixtureLoader();
		
		ONTOLOGY_INSTANCE = loader.load("ontology_instance");
		COMMAND_REQ_INSTANCE = loader.load("command_req_instance");
		ONTOLOGY_UPDATE = loader.load("ontology_update");
		ONTOLOGY_QUERY_NATIVE_CRITERIA = loader.load("ontology_quey_native_criteria");
		ONTOLOGY_DELETE = loader.load("ontology_delete");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		this.api = new SSAPResourceAPI(SERVICE_URL);
		RestApiUtils.disableSSLVerification();
		
		
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testQueryBDC() {

		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		Response respQuery=this.api.query(sessionkey, null, "select identificacion from asset;", null, "BDC");		
		assertEquals(200, respQuery.getStatus());	
		
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respQuery.getStatus(), "QUERY"));
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, utils.getSSAPResource(api, respQuery).getData()));
			
		utils.leave(api, sessionkey);	
	}
	
	
	@Test
	public void testJoinByTokenLeave() {
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		Response respLeave = utils.leave(api, sessionkey);
		assertEquals(respLeave.getStatus(), 200);
	}
	
	@Test
	public void testSubscribeUnsubscribe2() {
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		Response respSubscribe = this.api.subscribe(sessionkey, ONTOLOGY_NAME, "select * from " + ONTOLOGY_NAME, 0, null, "SQLLIKE", "http://localhost:10080/ReceptorSuscripcionesRest/SubscriptionReceiver");
		String subscriptionId = utils.getSSAPResource(api, respSubscribe).getData();
		assertEquals(200, respSubscribe.getStatus());
		
		Response respUnsubscribe = this.api.unsubscribe(sessionkey, subscriptionId);
		assertEquals(200, respUnsubscribe.getStatus());
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respUnsubscribe.getStatus(), "UNSUBSCRIBE"));
		
		utils.leave(api, sessionkey);		
	}
	
	
	@Test
	public void testInsert() {
		
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		SSAPResource ssapInsert=new SSAPResource();
		ssapInsert.setData(ONTOLOGY_INSTANCE.toString());
		ssapInsert.setSessionKey(sessionkey);
		ssapInsert.setOntology(ONTOLOGY_NAME);
		
		Response respInsert=this.api.insert(ssapInsert);
		assertEquals(200, respInsert.getStatus());
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respInsert.getStatus(), "INSERT"));
		
		String data = utils.getSSAPResource(api, respInsert).getData();
		
		assertEquals(true, data.contains("$oid"));	
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, data));
		
		utils.leave(api, sessionkey);		
	}
	
	@Test
	public void testUpdate() throws JsonProcessingException, IOException {
		
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		SSAPResource ssapInsert=new SSAPResource();
		ssapInsert.setData(ONTOLOGY_INSTANCE.toString());
		ssapInsert.setSessionKey(sessionkey);
		ssapInsert.setOntology(ONTOLOGY_NAME);
		
		Response respInsert=this.api.insert(ssapInsert);		
		String data = utils.getSSAPResource(api, respInsert).getData();
		JsonNode ObjectId = JSON.getObjectMapper().readTree(data);
			
		((ObjectNode)ONTOLOGY_UPDATE).replace("_id", ObjectId.at("/_id"));
			
		SSAPResource ssapUpdate=new SSAPResource();
		ssapUpdate.setSessionKey(sessionkey);
		ssapUpdate.setOntology(ONTOLOGY_NAME);
		ssapUpdate.setData(ONTOLOGY_UPDATE.toString());
		
		Response respUpdate=this.api.update(ssapUpdate);
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respUpdate.getStatus(), "UPDATE"));
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, utils.getSSAPResource(api, respUpdate).getData()));
		assertEquals(respUpdate.getStatus(), 200);

		utils.leave(api, sessionkey);	
	}
	
	@Test
	public void testQueryByObjectId() throws JsonProcessingException, IOException {
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);

		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		SSAPResource ssapInsert=new SSAPResource();
		ssapInsert.setData(ONTOLOGY_INSTANCE.toString());
		ssapInsert.setSessionKey(sessionkey);
		ssapInsert.setOntology(ONTOLOGY_NAME);
		Response respInsert=this.api.insert(ssapInsert);		
		String data = utils.getSSAPResource(api, respInsert).getData();				
		JsonNode ObjectId = JSON.getObjectMapper().readTree(data);
		
		Response respQuery=this.api.query(ObjectId.at("/_id/$oid").asText(), sessionkey, ONTOLOGY_NAME);
		assertEquals(200, respQuery.getStatus());
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respQuery.getStatus(), "QUERY_BY_ID"));
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, utils.getSSAPResource(api, respQuery).getData()));
		
		utils.leave(api, sessionkey);	
	}
	
	@Test
	public void testQueryNativeCriteria() {
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		SSAPResource ssapInsert=new SSAPResource();
		ssapInsert.setData(ONTOLOGY_INSTANCE.toString());
		ssapInsert.setSessionKey(sessionkey);
		ssapInsert.setOntology(ONTOLOGY_NAME);
		Response respInsert=this.api.insert(ssapInsert);		
		
		Response respQuery=this.api.query(sessionkey, ONTOLOGY_NAME, ONTOLOGY_QUERY_NATIVE_CRITERIA.toString(), null, "NATIVE");
		assertEquals(respQuery.getStatus(), 200);
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respQuery.getStatus(), "QUERY_NATIVE_CRITEREIA"));
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, utils.getSSAPResource(api, respQuery).getData()));
		
		utils.leave(api, sessionkey);
	}
	
	@Test
	public void testQueryNativeStatement() {
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		SSAPResource ssapInsert=new SSAPResource();
		ssapInsert.setData(ONTOLOGY_INSTANCE.toString());
		ssapInsert.setSessionKey(sessionkey);
		ssapInsert.setOntology(ONTOLOGY_NAME);
		Response respInsert=this.api.insert(ssapInsert);		
		
		Response respQuery=this.api.query(sessionkey, ONTOLOGY_NAME, ONTOLOGY_QUERY_NATIVE_STATEMENT, null, "NATIVE");
		assertEquals(respQuery.getStatus(), 200);
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respQuery.getStatus(), "QUERY_NATIVE_STATEMENT"));
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, utils.getSSAPResource(api, respQuery).getData()));
		
		utils.leave(api, sessionkey);
	}
	
	@Test
	public void testQuerySQLLIKEStatement() {
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		SSAPResource ssapInsert=new SSAPResource();
		ssapInsert.setData(ONTOLOGY_INSTANCE.toString());
		ssapInsert.setSessionKey(sessionkey);
		ssapInsert.setOntology(ONTOLOGY_NAME);
		Response respInsert=this.api.insert(ssapInsert);
				
		Response respQuery=this.api.query(sessionkey, ONTOLOGY_NAME, ONTOLOGY_QUERY_SQLLIKE, null, "SQLLIKE");
		
		assertEquals(respQuery.getStatus(), 200);
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respQuery.getStatus(), "QUERY_SQL_LIKE"));
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, utils.getSSAPResource(api,respQuery).getData()));
		
		utils.leave(api, sessionkey);
	}
	
	@Test
	public void testInsertSQLLIKEStatement() {
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
			
		Response respQuery=this.api.query(sessionkey, ONTOLOGY_NAME, ONTOLOGY_INSERT_SQLLIKE, null, "SQLLIKE");
		assertEquals(respQuery.getStatus(), 200);
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respQuery.getStatus(), "INSERT_SQL_LIKE"));
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, utils.getSSAPResource(api, respQuery).getData()));
		
		utils.leave(api, sessionkey);
	}

	@Test
	public void testDeleteByObjectId() throws JsonProcessingException, IOException {
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		SSAPResource ssapInsert=new SSAPResource();
		ssapInsert.setData(ONTOLOGY_INSTANCE.toString());
		ssapInsert.setSessionKey(sessionkey);
		ssapInsert.setOntology(ONTOLOGY_NAME);
		Response respInsert=this.api.insert(ssapInsert);
		String data = utils.getSSAPResource(api, respInsert).getData();	
		JsonNode ObjectId = JSON.getObjectMapper().readTree(data);
		
		Response respQuery=this.api.deleteOid(ObjectId.at("/_id/$oid").asText(), sessionkey, ONTOLOGY_NAME);
		assertEquals(respQuery.getStatus(), 200);
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respQuery.getStatus(), "DELETE_BY_OBJECT_ID"));
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, respQuery.getStatus()));
		//log.info(String.format(LogMessages.LOG_RESPONSE_DATA, utils.getSSAPResource(api, respQuery).getData()));
		
		utils.leave(api, sessionkey);
	}
	
	@Test
	public void testDelete() throws  IOException {
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		SSAPResource ssapInsert=new SSAPResource();
		ssapInsert.setData(ONTOLOGY_INSTANCE.toString());
		ssapInsert.setSessionKey(sessionkey);
		ssapInsert.setOntology(ONTOLOGY_NAME);
		Response respInsert=this.api.insert(ssapInsert);		
		String data = utils.getSSAPResource(api, respInsert).getData();				
		
		
		SSAPResource ssapDelete=new SSAPResource();
		ssapDelete.setData(data);
		ssapDelete.setSessionKey(sessionkey);
		ssapDelete.setOntology(ONTOLOGY_NAME);
		
		Response respQuery=this.api.delete(ssapDelete);
		
		assertEquals(respQuery.getStatus(), 200);
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respQuery.getStatus(), "DELETE"));
		
		utils.leave(api, sessionkey);
	}
	
	@Test
	public void testBulk() {
		Response respJoin = utils.join(this.api, KP_INSTANCE, TOKEN);	
		String sessionkey = utils.getSSAPResource(api, respJoin).getSessionKey();
		
		List<String> msgBulk = new ArrayList<String>();
		String insert_instance = ONTOLOGY_INSTANCE.toString();
		msgBulk.add(insert_instance);
		msgBulk.add(insert_instance);
		msgBulk.add(insert_instance);
		
		SSAPResource ssapInsert=new SSAPResource();
		ssapInsert.setData(msgBulk.toString());
		ssapInsert.setSessionKey(sessionkey);
		ssapInsert.setOntology(ONTOLOGY_NAME);
				
		Response respInsert=this.api.insert(ssapInsert);
		assertEquals(200, respInsert.getStatus());
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respInsert.getStatus(), "BULK_INSERT"));
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, utils.getSSAPResource(api,respInsert).getData()));
		
		utils.leave(api, sessionkey);
		
	}
}
