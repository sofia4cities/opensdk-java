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

package com.indra.sofia4cities.ssap.testutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.indra.sofia2.ssap.kp.implementations.rest.SSAPResourceAPI;
import com.indra.sofia2.ssap.kp.implementations.rest.exception.ResponseMapperException;
import com.indra.sofia2.ssap.kp.implementations.rest.resource.SSAPResource;
import com.indra.sofia2.ssap.ssap.SSAPVersion;
import com.indra.sofia4cities.ssap.kp.logging.LogMessages;

public class RestApiUtils {
	private static Logger log;
	
	public <T> RestApiUtils(Class<T> clazz) {
		this.log = LoggerFactory.getLogger(clazz);
	}
	
	public Response join(SSAPResourceAPI api, String kp_instance, String token){
		
		SSAPResource ssapJoin=new SSAPResource();		
		ssapJoin.setJoin(true);
		ssapJoin.setInstanceKP(kp_instance);
		ssapJoin.setToken(token);
		ssapJoin.setVersion(SSAPVersion.ONE);

		
		Response respJoin=api.insert(ssapJoin);
		//getSSAPResource2(api, respJoin);
		log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, respJoin.getStatus(), "JOIN"));
		
		return respJoin;

	}
	
	public Response leave(SSAPResourceAPI api, String session_key) {
		if(session_key!=null){
			SSAPResource ssapLeave = new SSAPResource();
			ssapLeave.setLeave(true);
			ssapLeave.setSessionKey(session_key);
			
			Response resp = api.insert(ssapLeave);
			log.info(String.format(LogMessages.LOG_HHTP_RESPONSE_CODE, resp.getStatus(), "LEAVE"));
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return resp;
		}
		else
			return Response.status(Response.Status.GONE).build();
	}
	
	public SSAPResource getSSAPResource(SSAPResourceAPI api, Response response) {
		SSAPResource resp = new SSAPResource();
		try {
			resp = api.responseAsSsap(response);
		} catch (ResponseMapperException e) {
			e.printStackTrace();
		}
		
		
		

		return resp;
	}
	
	//TODO: Borrar este metodo
	public SSAPResource getSSAPResource2(SSAPResourceAPI api, Response response) {
	InputStream is = (InputStream)response.getEntity();
	
	BufferedReader br = new BufferedReader(new InputStreamReader(is));
	String line;
	StringBuilder responseData = new StringBuilder();
	try {
		while((line = br.readLine()) != null) {
		    responseData.append(line);
		}
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	
	System.out.println(responseData.toString());
	return null;
	}
}
