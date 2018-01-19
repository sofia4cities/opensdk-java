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

import org.atmosphere.wasync.Request.METHOD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.indra.sofia4cites.ssap.kp.config.WebSocketConnectionConfig;
import com.indra.sofia4cities.ssap.kp.KpToExtendApi;
import com.indra.sofia4cities.ssap.kp.implementations.oficials.abstracts.KpFunctionalAbstract;
import com.indra.sofia4cities.ssap.kp.implementations.websockets.KpWebSocketClient;
import com.indra.sofia4cities.ssap.testutils.TestProperties;

public class TestKpFunctionalWebSocket extends KpFunctionalAbstract {
	
	@Override
	public KpToExtendApi getImplementation() {
		
		WebSocketConnectionConfig config=new WebSocketConnectionConfig();
		config.setEndpointUri(TestProperties.getInstance().get("test.officials.websockets.url"));
		config.setMethod(METHOD.GET);
		config.setSibConnectionTimeout(Integer.valueOf(TestProperties.getInstance().get("test.officials.websockets.connection_timeout")));
				
		return new KpWebSocketClient(config, KP, KP_INSTANCE, TOKEN);
	}

	@Override
	public Logger getLog() {
		return  LoggerFactory.getLogger(TestKpFunctionalWebSocket.class);
	}


}
