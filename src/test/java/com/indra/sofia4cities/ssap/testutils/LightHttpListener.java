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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

public class LightHttpListener implements Callable<String> {
	private int port = 3003;
	
	public LightHttpListener(int port) {
		this.port = port;
	}
	public String call() throws Exception {
		String retStr = null;
		try (ServerSocket server = new ServerSocket(port)) {
			server.setSoTimeout(0);

			//Waiting for client (sofia2) connection
			Socket client = server.accept();
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
			
			//Reading http header
			String s;
			int contentLength = -1;
            while (!(s = in.readLine()).isEmpty()) {
                String [] header = s.split(":");
                if(header[0].equalsIgnoreCase("Content-Length")) {
                	contentLength = Integer.valueOf(header[1].trim());
                }
            }
            
            //Reading http content
            char [] buffer = new char[contentLength];
            in.read(buffer, 0, contentLength);

            //Sending response to client
           	out.write("HTTP/1.1 200 OK\r\n");
            out.write("Content-Length: 0\r\n");
            out.write("Connection: close\r\n");
            out.write("\r\n");
            out.flush();
            retStr = String.valueOf(buffer);
            client.close();
	  	} catch (IOException e) {
			e.printStackTrace();
		}
		
		return retStr;
		
	}

	
	

}
