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


package com.indra.sofia4cities.ssap.kp.implementations.oficials.abstracts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.indra.sofia2.ssap.kp.Listener4SIBIndicationNotifications;
import com.indra.sofia2.ssap.kp.SSAPMessageGenerator;
import com.indra.sofia2.ssap.ssap.SSAPBulkMessage;
import com.indra.sofia2.ssap.ssap.SSAPMessage;
import com.indra.sofia2.ssap.ssap.SSAPQueryType;
import com.indra.sofia2.ssap.ssap.body.SSAPBodyOperationMessage;
import com.indra.sofia2.ssap.ssap.body.SSAPBodyReturnMessage;
import com.indra.sofia2.ssap.ssap.body.bulk.message.SSAPBodyBulkReturnMessage;
import com.indra.sofia4cities.ssap.kp.KpToExtendApi;
import com.indra.sofia4cities.ssap.kp.logging.LogMessages;
import com.indra.sofia4cities.ssap.testutils.FixtureLoader;
import com.indra.sofia4cities.ssap.testutils.KpApiUtils;
import com.indra.sofia4cities.ssap.testutils.TestProperties;

public abstract class KpFunctionalAbstract {

	protected static Logger log;
	protected static KpApiUtils utils;
	
	protected final static String TOKEN = TestProperties.getInstance().get("test.officials.token");
	protected final static String KP = TestProperties.getInstance().get("test.officials.kp");
	protected final static String KP_INSTANCE = TestProperties.getInstance().get("test.officials.kp_instance") + System.currentTimeMillis();
	
	private final static String ONTOLOGY_NAME = TestProperties.getInstance().get("test.officials.ontology_name");
	
	private static JsonNode ONTOLOGY_INSTANCE;
	private static JsonNode COMMAND_REQ_INSTANCE;
	private static JsonNode ONTOLOGY_UPDATE;
	private static JsonNode ONTOLOGY_QUERY_NATIVE_CRITERIA;
	private static JsonNode ONTOLOGY_DELETE;
	
	private final static String ONTOLOGY_QUERY_NATIVE_STATEMENT = "db.TestSensorTemp.find({'Sensor.assetId': 'S_Temperatura_00066'})";
	private final static String ONTOLOGY_QUERY_SQLLIKE = "select * from TestSensorTemp where Sensor.assetId = 'S_Temperatura_00066'";
	private final static String ONTOLOGY_INSERT_SQLLIKE = "insert into TestSensorTemp(geometry, assetId, measure, timestamp) values (\"{ 'coordinates': [ 40.512967, -3.67495 ], 'type': 'Point' }\", \"S_Temperatura_00067\", 15, \"{ '$date': '2014-04-29T08:24:54.005Z'}\")";
	
	private final static String ONTOLOGY_UPDATE_WHERE = "{Sensor.assetId:\"S_Temp_00067\"}";
	private final static String ONTOLOGY_QUERY_NATIVE = "{Sensor.assetId:\"S_Temp_00067\"}";
	
	private final static String ONTOLOGY_UPDATE_SQLLIKE = "update TestSensorTemp set measure = 15 where Sensor.assetId = \"S_Temperatura_00067\"";
	
	private static KpToExtendApi kp;
	
	private boolean indicationReceived;
	private String sessionKey;
	
	public abstract KpToExtendApi getImplementation();
	public abstract Logger getLog();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		FixtureLoader loader = new FixtureLoader();
		
		ONTOLOGY_INSTANCE = loader.load("ontology_instance");
		COMMAND_REQ_INSTANCE = loader.load("command_req_instance");
		ONTOLOGY_UPDATE = loader.load("ontology_update_no_id");
		ONTOLOGY_QUERY_NATIVE_CRITERIA = loader.load("ontology_quey_native_criteria");
		ONTOLOGY_DELETE = loader.load("ontology_delete");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
	
	}	

	@Before
	public void setUp() throws Exception {
		log = this.getLog();
		utils = new KpApiUtils(getClass());
			kp= this.getImplementation();
			kp.connect();
		sessionKey = utils.doJoin(kp, TOKEN, KP_INSTANCE);
	}

	@After
	public void tearDown() throws Exception {
		utils.doLeave(kp, sessionKey);
		kp.disconnect();
	}

	
	
	@Test
	public void testJoinByTokenLeave( ) throws Exception {	
		
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		SSAPMessage<SSAPBodyReturnMessage> response= kp.send(msgJoin);
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, response.toJson()));
		
		assertNotSame(response.getSessionKey(), null);
		
		String sessionKey=response.getSessionKey();
		
		SSAPBodyReturnMessage bodyReturn=response.getBodyAsObject();
		assertEquals(bodyReturn.getData(), sessionKey);
		assertTrue(bodyReturn.isOk());
		assertSame(bodyReturn.getError(), null);
		
		JsonNode jBody = response.getBodyAsJsonObject();
		assertEquals(jBody.path("data").asText(), sessionKey);
		assertTrue(jBody.path("ok").asBoolean());
		assertTrue(jBody.path("error").isNull());
		
	}
	
	@Test
	public void testInsertNative() throws Exception {
		
		SSAPMessage msgInsert=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSTANCE.toString());
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, msgInsert.toJson()));
		SSAPMessage<SSAPBodyReturnMessage> response = kp.send(msgInsert);
		
		SSAPBodyReturnMessage returned=response.getBodyAsObject();
		assertTrue(returned.isOk());
		
		JsonNode jBody = response.getBodyAsJsonObject();
		assertTrue(jBody.path("ok").asBoolean());
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, response.toJson()));
	}
	
	@Test
	public void testUpdateNative() throws Exception {
	
		SSAPMessage msgInsert=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSTANCE.toString());
		SSAPMessage<SSAPBodyReturnMessage> response=kp.send(msgInsert);
				
		SSAPMessage msgUpate=SSAPMessageGenerator.getInstance().generateUpdateMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_UPDATE.toString(), ONTOLOGY_UPDATE_WHERE);
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, msgUpate.toJson()));
		SSAPMessage<SSAPBodyReturnMessage> responseUpdate=kp.send(msgUpate);
		
		SSAPBodyReturnMessage returned=response.getBodyAsObject();
		assertTrue(returned.isOk());
		
		JsonNode jBody = response.getBodyAsJsonObject();
		assertTrue(jBody.path("ok").asBoolean());
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, response.toJson()));
		
	}
	
	@Test
	public void testQueryNative() throws Exception {
		SSAPMessage msgInsert=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSERT_SQLLIKE, SSAPQueryType.SQLLIKE);
		SSAPBodyOperationMessage messageRequest = SSAPBodyOperationMessage.fromJsonToSSAPBodyOperationMessage(msgInsert.getBody());
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, msgInsert.toJson()));
		SSAPMessage<SSAPBodyReturnMessage> responseInsert=kp.send(msgInsert);
		
		//SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
		SSAPBodyReturnMessage returnedInsert=responseInsert.getBodyAsObject();
		assertTrue(returnedInsert.isOk());
		
		
		SSAPMessage msgQuery=SSAPMessageGenerator.getInstance().generateQueryMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_QUERY_NATIVE, SSAPQueryType.NATIVE);
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, msgQuery.toJson()));
		SSAPMessage<SSAPBodyReturnMessage> response=kp.send(msgQuery);
				
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
		assertTrue(returned.isOk());

		JsonNode jBody = response.getBodyAsJsonObject();
		JsonNode jReturned = returned.getDataAsJsonObject();
		assertTrue(jBody.path("ok").asBoolean());
		Iterator<JsonNode> iterator = jReturned.iterator();
					
		while(iterator.hasNext()) {
			JsonNode item = iterator.next();
			assertTrue(!item.path("_id").isNull());
		}
		
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, response.toJson()));
	}
	
	@Test
	public void testInsertSqlLike() throws Exception {
		
		SSAPMessage msgInsert=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSERT_SQLLIKE, SSAPQueryType.SQLLIKE);
		SSAPBodyOperationMessage messageRequest = SSAPBodyOperationMessage.fromJsonToSSAPBodyOperationMessage(msgInsert.getBody());
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, msgInsert.toJson()));
		SSAPMessage<SSAPBodyReturnMessage> response=kp.send(msgInsert);
		
		//SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
		SSAPBodyReturnMessage returned=response.getBodyAsObject();
		assertTrue(returned.isOk());
		
		JsonNode jBody = response.getBodyAsJsonObject();
		JsonNode jReturned = returned.getDataAsJsonObject();
		assertTrue(jBody.path("ok").asBoolean());
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, response.toJson()));
				
	}
	
	@Test
	public void testUpdateSqlLike() throws Exception {
		
		SSAPMessage msgUpate=SSAPMessageGenerator.getInstance().generateUpdateMessage(sessionKey, ONTOLOGY_NAME, null, ONTOLOGY_UPDATE_SQLLIKE, SSAPQueryType.SQLLIKE);
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, msgUpate.toJson()));
		SSAPMessage<SSAPBodyReturnMessage> response=kp.send(msgUpate);

		//SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
		SSAPBodyReturnMessage returned=response.getBodyAsObject();
		assertTrue(returned.isOk());
		
		JsonNode jBody = response.getBodyAsJsonObject();
		JsonNode jReturned = returned.getDataAsJsonObject();
		assertTrue(jBody.path("ok").asBoolean());
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, response.toJson()));
	}
	
	@Test
	public void testQuerySql() throws Exception {
	
		SSAPMessage msgQuery=SSAPMessageGenerator.getInstance().generateQueryMessage(sessionKey, ONTOLOGY_NAME, "select * from " + ONTOLOGY_NAME , SSAPQueryType.SQLLIKE);
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, msgQuery.toJson()));
		SSAPMessage<SSAPBodyReturnMessage> response=kp.send(msgQuery);
		
		//SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
		SSAPBodyReturnMessage returned=response.getBodyAsObject();
		assertTrue(returned.isOk());
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, response.toJson()));
	}
	
	@Test
	public void testQuerySqlBDC() throws Exception {

		SSAPMessage msgQuery=SSAPMessageGenerator.getInstance().generateQueryMessage(sessionKey, null, "select * from Asset where identificacion='tweets_sofia'", SSAPQueryType.BDC);
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, msgQuery.toJson()));
		SSAPMessage<SSAPBodyReturnMessage> response=kp.send(msgQuery);
				
		//SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
		SSAPBodyReturnMessage returned=response.getBodyAsObject();
		assertTrue(returned.isOk());
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, response.toJson()));
	}
	
	@Test
	public void testQueryBDC() throws Exception {
	
		SSAPMessage msgQuery=SSAPMessageGenerator.getInstance().generateQueryMessage(sessionKey, null, "select * from Asset", SSAPQueryType.BDC);
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, msgQuery.toJson()));
		SSAPMessage<SSAPBodyReturnMessage> response=kp.send(msgQuery);
	
		//SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
		SSAPBodyReturnMessage returned=response.getBodyAsObject();
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, response.toJson()));
		
		assertTrue(returned.isOk());
	}
	
	@Test
	public void testSubscribeUnsubscribe() throws Exception{
		
		kp.addListener4SIBNotifications(new Listener4SIBIndicationNotifications() {
			
			@Override
			public void onIndication(String messageId, SSAPMessage ssapMessage) {
				
				log.info(String.format(LogMessages.LOG_NOTIFICATION, messageId, ssapMessage.toJson()));
				
				indicationReceived=true;
			
				SSAPBodyReturnMessage indicationMessage=SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(ssapMessage.getBody());
				
				assertNotSame(indicationMessage.getData(), null);
				assertTrue(indicationMessage.isOk());
				assertSame(indicationMessage.getError(), null);
				log.info(String.format(LogMessages.LOG_RESPONSE_DATA, ssapMessage.toJson()));
				
			}
		});
				
		SSAPMessage msg=SSAPMessageGenerator.getInstance().generateSubscribeMessage(sessionKey, ONTOLOGY_NAME, 0, "", SSAPQueryType.SQLLIKE);
		
		SSAPMessage<SSAPBodyReturnMessage> msgSubscribe = kp.send(msg);
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, msgSubscribe.toJson()));
		//SSAPBodyReturnMessage responseSubscribeBody = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(msgSubscribe.getBody());
		SSAPBodyReturnMessage responseSubscribeBody=msgSubscribe.getBodyAsObject();
		assertNotSame(responseSubscribeBody.getData(), null);
		assertTrue(responseSubscribeBody.isOk());
		assertSame(responseSubscribeBody.getError(), null);
		
		String subscriptionId=responseSubscribeBody.getData();
		
		SSAPMessage msgInsert=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSERT_SQLLIKE, SSAPQueryType.SQLLIKE);
		
		
		SSAPMessage<SSAPBodyReturnMessage> responseInsert=kp.send(msgInsert);
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, responseInsert.toJson()));
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseInsert.getBody());
		assertTrue(returned.isOk());
				
		Thread.sleep(5000);
		assertTrue(indicationReceived);
		
		SSAPMessage msgUnsubscribe=SSAPMessageGenerator.getInstance().generateUnsubscribeMessage(sessionKey, ONTOLOGY_NAME, subscriptionId);
		
		SSAPMessage<SSAPBodyReturnMessage> responseUnsubscribe=kp.send(msgUnsubscribe);
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, responseUnsubscribe.toJson()));
		//SSAPBodyReturnMessage responseUnSubscribeBody = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseUnsubscribe.getBody());
		SSAPBodyReturnMessage responseUnSubscribeBody=responseUnsubscribe.getBodyAsObject();
		 
		assertEquals(responseUnSubscribeBody.getData(), "");
		assertTrue(responseUnSubscribeBody.isOk());
		assertSame(responseUnSubscribeBody.getError(), null);
	
	}
	
	@Test
	public void testBulk() throws Exception {
		
		SSAPMessage msgInsert1=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSTANCE.toString());
		SSAPMessage msgInsert2=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSTANCE.toString());
		SSAPMessage msgInsert3=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSERT_SQLLIKE, SSAPQueryType.SQLLIKE);
		SSAPMessage msgUpate1=SSAPMessageGenerator.getInstance().generateUpdateMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_UPDATE.toString(), ONTOLOGY_UPDATE_WHERE);
		SSAPMessage msgUpate2=SSAPMessageGenerator.getInstance().generateUpdateMessage(sessionKey, ONTOLOGY_NAME, null, ONTOLOGY_UPDATE_SQLLIKE, SSAPQueryType.SQLLIKE);
		
		SSAPBulkMessage request = SSAPMessageGenerator.getInstance().generateBulkMessage(sessionKey);
		
		request.addMessage(msgInsert1);
		request.addMessage(msgInsert2);
		request.addMessage(msgInsert3);
		request.addMessage(msgUpate1);
		request.addMessage(msgUpate2);
			
		log.info(String.format(LogMessages.LOG_REQUEST_DATA, request.toJson()));
		SSAPMessage<SSAPBodyReturnMessage> response=kp.send(request);
		JsonNode jBody = response.getBodyAsJsonObject();
		if(jBody.isArray()) {
			 for (final JsonNode objNode : jBody) {
			        System.out.println(objNode.toString());
			    }
		}
		
		SSAPBodyReturnMessage bodyBulkReturn=SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
		System.out.println(bodyBulkReturn.getData());
		SSAPBodyBulkReturnMessage summary=SSAPBodyBulkReturnMessage.fromJsonToSSAPBodyBulkReturnMessage(bodyBulkReturn.getData());
		
		assertEquals(3, summary.getInsertSummary().getObjectIds().size());
		System.out.println(summary.getUpdateSummary().getObjectIds().size());
		StringBuilder sb = new StringBuilder();
		for(String oid:summary.getInsertSummary().getObjectIds()) 
			sb.append(oid);
		
		log.info(String.format(LogMessages.LOG_RESPONSE_DATA, response.toJson()));
	}
	
}
