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

import org.fusesource.mqtt.client.QoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.indra.sofia4cites.ssap.kp.config.MQTTConnectionConfig;
import com.indra.sofia4cities.ssap.kp.KpToExtendApi;
import com.indra.sofia4cities.ssap.kp.implementations.mqtt.KpMQTTClient;
import com.indra.sofia4cities.ssap.kp.implementations.oficials.abstracts.KpFunctionalAbstract;
import com.indra.sofia4cities.ssap.testutils.TestProperties;

public class TestKpFunctionalMqtt extends KpFunctionalAbstract{
	
	private final static String HOST = TestProperties.getInstance().get("test.officials.mqtt.url");
	private final static int PORT = Integer.parseInt(TestProperties.getInstance().get("test.officials.mqtt.port"));
		
	private final static String MQTT_USERNAME = TestProperties.getInstance().get("test.officials.mqtt.username");
	private final static String MQTT_PASSWORD = TestProperties.getInstance().get("test.officials.mqtt.password");
	private final static boolean ENABLE_MQTT_AUTHENTICATION = Boolean.valueOf(TestProperties.getInstance().get("test.officials.mqtt.enable_athentication"));

	@Override
	public KpToExtendApi getImplementation() {
		
		MQTTConnectionConfig config = new MQTTConnectionConfig();
		config.setSibHost(HOST);
		config.setSibPort(PORT);
		config.setKeepAliveInSeconds(5);
		config.setQualityOfService(QoS.AT_LEAST_ONCE);
		config.setSibConnectionTimeout(Integer.valueOf(TestProperties.getInstance().get("test.officials.mqtt.connection_timeout")));
		config.setSsapResponseTimeout(Integer.valueOf(TestProperties.getInstance().get("test.officials.mqtt.response_timeout")));
		if (ENABLE_MQTT_AUTHENTICATION) {
			config.setUser(MQTT_USERNAME);
			config.setPassword(MQTT_PASSWORD);
		}
		
		return (new KpMQTTClient(config, KP, KP_INSTANCE, TOKEN));
	}

	@Override
	public Logger  getLog() {
		return LoggerFactory.getLogger(TestKpFunctionalMqtt.class);
	}	

}
