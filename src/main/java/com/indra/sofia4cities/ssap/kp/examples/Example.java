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

package com.indra.sofia4cities.ssap.kp.examples;

import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

import org.fusesource.mqtt.client.QoS;

import com.indra.sofia2.ssap.kp.SSAPMessageGenerator;
import com.indra.sofia2.ssap.kp.exceptions.ConnectionToSIBException;
import com.indra.sofia2.ssap.ssap.SSAPMessage;
import com.indra.sofia2.ssap.ssap.SSAPQueryType;
import com.indra.sofia2.ssap.ssap.body.SSAPBodyReturnMessage;
import com.indra.sofia2.ssap.ssap.exceptions.SQLSentenceNotAllowedForThisOperationException;
import com.indra.sofia4cites.ssap.kp.config.MQTTConnectionConfig;
import com.indra.sofia4cities.ssap.kp.implementations.mqtt.KpMQTTClient;

public class Example {
	
	KpMQTTClient kp;
	boolean isJoined = false;
	String sessionKey = null;
	Properties properties = new Properties();
	
	public String HOST;
	public String PORT;
	public String KP;
	public String KP_INSTANCE;
	public String TOKEN;
	public String ONTOLOGY;
	
	Scanner in = new Scanner(System.in);
	
	
	public static void main(String [] args) {
		
		Example example = new Example();
		example.loadProperties();
		example.initializeKp();
		example.userAction();
	}
	
	public void loadProperties() {		
    	try {
    		
    		System.out.println("Loading file exampe.properties ...... ");
    		InputStream stream = getClass().getClassLoader().getResourceAsStream("example.properties");
			this.properties.load(stream);
			HOST = this.properties.getProperty("example.mqtt.url", "localhost");
			PORT = this.properties.getProperty("example.mqtt.port", "1880");
			KP = this.properties.getProperty("example.kp", "KP_APITesting");
			KP_INSTANCE = this.properties.getProperty("example.kp_instance", "KP_APITesting:KPTestTemperatura01");
			TOKEN = this.properties.getProperty("example.token", "49622ff059364fcea6ce78e8b0fa8b4b");
			ONTOLOGY = this.properties.getProperty("example.ontology_name", "TestSensorTemp");
			
			System.out.println("Properties Loaded ");
			System.out.println("\t example.mqtt.url: " + HOST);
			System.out.println("\t example.mqtt.port: " + PORT);
			System.out.println("\t example.kp: " + KP);
			System.out.println("\t example.kp_instance: " + KP_INSTANCE);
			System.out.println("\t example.token: " + TOKEN);
			System.out.println("\t example.ontology_name: " + ONTOLOGY);
			
			
		} catch (Exception e) {
			System.err.println("ERROR: Properties file could not be loaded");
			this.exit();
		}
	}
	
	public void initializeKp()  {
		
		MQTTConnectionConfig config = new MQTTConnectionConfig();
		config.setSibHost(HOST);
		config.setSibPort(Integer.valueOf(PORT));
		config.setKeepAliveInSeconds(5);
		config.setQualityOfService(QoS.AT_LEAST_ONCE);
		config.setSibConnectionTimeout(6000);
		config.setSsapResponseTimeout(6000);
	
		kp = new KpMQTTClient(config, KP, KP_INSTANCE, TOKEN);
		try {
			System.out.println("Connecting with platform ...... ");
			kp.connect();
			this.doJoin();
		} catch (ConnectionToSIBException e) {
			System.err.println("Error connecting with platform");
			e.printStackTrace();
			this.exit();
		}
		
		System.out.println("ThinkKp joined and connected with platform succesfully ");	
	}
	
	
	public boolean doJoin() {
		SSAPMessage request = SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(kp.getToken(), kp.getKpInstance());
		SSAPMessage response;
		System.out.println("Joining with platform with message: " + request.getBody());
		try {
			response = kp.send(request);
			this.sessionKey = response.getSessionKey();
			if( sessionKey!= null && sessionKey!= "")
				System.out.println("ThinkKp joined with sessionKey: " + sessionKey);
			else {
				System.err.println("Error joining with platform: " + response.getBody());
				this.exit();
			}
				
		} catch (ConnectionToSIBException e) {
			System.err.println("Error joining with platform");
			e.printStackTrace();
			this.exit();
		}
		
		return (this.sessionKey != null);
	}
	
	public boolean doLeave() {
		
		boolean ret = true;
		
		if(this.sessionKey != null) {
			SSAPMessage request = SSAPMessageGenerator.getInstance().generateLeaveMessage(this.sessionKey);
			SSAPMessage response;
			SSAPBodyReturnMessage responsebody = null;
			try {
				response = kp.send(request);
				responsebody = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
			} catch (ConnectionToSIBException e) {
				//e.printStackTrace();
			}
			
			ret = responsebody.isOk();
		}
		
		return ret;
	}

	public void userAction() {
	
		this.printOperations();
    	
		while(true) {
    		SSAPMessage message = this.askOperation();
    		if(message != null)
    			this.processOperation(message);
    		else
    			System.err.println("");
    		
    		printOperations();
    		
    	}
	}
	
	public SSAPMessage askOperation() {
		String operation;
		SSAPMessage message = null;
		SSAPQueryType queryType; 	
		String stmt;
		
		operation = Example.normalizeInut(in.nextLine());
		
		if(Commands.QUIT.compareToIgnoreCase(operation) == 0) 
			this.exit();
		
		try {
		if(Commands.QUERY.compareToIgnoreCase(operation) == 0) {
			queryType = this.askQueryType(); 	
			System.out.println("insert query statement");
			stmt = Example.normalizeInut(in.nextLine());
			message = SSAPMessageGenerator.getInstance().generateQueryMessage(sessionKey, ONTOLOGY, stmt, queryType);
		}
		else if(Commands.INSERT.compareToIgnoreCase(operation) == 0) {
			queryType = this.askQueryType(); 	
			System.out.println("insert insert statement");
			stmt = Example.normalizeInut(in.nextLine()); 
			if(queryType == SSAPQueryType.SQLLIKE)
				message = SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY, stmt, queryType);
			else
				message = SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY, stmt);
		}
		else if(Commands.UPDATE.compareToIgnoreCase(operation) == 0) {
			queryType = this.askQueryType(); 	
			System.out.println("insert update statement");
			stmt = Example.normalizeInut(in.nextLine()); 
			message = SSAPMessageGenerator.getInstance().generateUpdateMessage(sessionKey, ONTOLOGY, "", stmt, queryType);
		}
		else if(Commands.DELETE.compareToIgnoreCase(operation) == 0) {
			queryType = this.askQueryType(); 	
			System.out.println("insert delete statement");
			stmt = Example.normalizeInut(in.nextLine()); 
			message = SSAPMessageGenerator.getInstance().generateDeleteMessage(sessionKey, ONTOLOGY, stmt, queryType);
		}
		else {
			System.err.println(
					String.format("Please insert a valid operation [%s | %s | %s | %s | %s]", 
							Commands.QUIT, Commands.QUERY, Commands.INSERT, Commands.UPDATE, Commands.DELETE));
			this.askOperation();
		}
		} catch(SQLSentenceNotAllowedForThisOperationException e) {
			System.err.println("ERROR: The sentence has a invalid format or is not a valid Sql operation");
			//this.askOperation();
		}

		return message;
	}
	
	public SSAPQueryType askQueryType() {
		
		SSAPQueryType queryType = null;
		System.out.println("insert type of query [native | sqllike]:");
		String sQueryType = Example.normalizeInut(in.nextLine());
		
		if(sQueryType.compareToIgnoreCase("native") == 0)
			queryType = SSAPQueryType.NATIVE;
		else if(sQueryType.compareToIgnoreCase("sqllike") == 0)
			queryType = SSAPQueryType.SQLLIKE;
		else {
			System.err.println("Query type not recognized");
			this.askQueryType();
		}
		
		return queryType;
	}
	
	public void processOperation(SSAPMessage message) {
		
		try {
			SSAPMessage response = kp.send(message);
			SSAPBodyReturnMessage retResponse = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
	
			if(retResponse.isOk()) {
				System.out.println("Operation success with response: " + retResponse.getData());
				
			}
			else {
				System.err.println("Operation error with message :" + retResponse.getError());
			}
			
		} catch (ConnectionToSIBException e) {
			System.err.println("ERROR: Could not connect with platform");
			e.printStackTrace();
		} 
	}
	
	public static String normalizeInut(String input) {
		return input.trim().replaceAll(" +", " ");
		
	}
	
	public void exit() {
		
		System.out.println("Disconnecting form platform ....");
		this.doLeave();
		this.disconnect();
		System.out.println("Disconnection complete");
		System.out.println("Bye :) ");
		System.exit(-1);
	}
	
	public void disconnect() {
		if(this.kp.isPhysicalConnectionEstablished())
			this.kp.disconnect();
	}
	
	public void printOperations() {
		System.out.println("");
		System.out.println("List of kp actions to perform (Type de option and pulse enter):");
		System.out.println("");
				
		System.out.println(String.format("\tquery <enter>"));
		System.out.println(String.format("\tinsert <enter>"));
		System.out.println(String.format("\tupdate <enter>"));
		System.out.println(String.format("\tdelete <enter>"));
		

		System.out.println("\tquit <enter>");
		
	}
	
	public static class Commands {
		
		public static final String QUERY = "query";
		public static final String INSERT = "insert";
		public static final String UPDATE = "update";
		public static final String DELETE = "delete";
		public static final String QUIT = "quit";
	}
	
}
