package org.bigbluebutton
/* 
    BigBlueButton open source conferencing system - http://www.bigbluebutton.org/

    Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).

    This program is free software; you can redistribute it and/or modify it under the
    terms of the GNU Lesser General Public License as published by the Free Software
    Foundation; either version 3.0 of the License, or (at your option) any later
    version.

    BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
    PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License along
    with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
*/

import java.util.List
import java.util.Map

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

import org.apache.commons.codec.binary.Base64
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.Document

class LtiService {

    boolean transactional = false

    def endPoint = "localhost"
    def consumers = "demo:welcome"
    def mode = "simple"

	def ssl_enabled

    Map<String, String> consumerMap
    
    def retrieveIconEndpoint() {
        return endPoint.replaceFirst("tool", "images/icon.ico")
    }

    def retrieveBasicLtiEndpoint() {
        return endPoint
    }
    
    private Map<String, String> getConsumer(consumerId) {
        Map<String, String> consumer = null
        
        if( this.consumerMap.containsKey(consumerId) ){
            consumer = new HashMap<String, String>()
            consumer.put("key", consumerId);
            consumer.put("secret",  this.consumerMap.get(consumerId))
        }
        
        return consumer
    }

    private void initConsumerMap(){
        this.consumerMap = new HashMap<String, String>()
        String[] consumers = this.consumers.split(",")
        //for( int i=0; i < consumers.length; i++){
        if ( consumers.length > 0 ){
            int i = 0;
            String[] consumer = consumers[i].split(":")
            if( consumer.length == 2 ){
                this.consumerMap.put(consumer[0], consumer[1])
            }
        }
        
    }
    
    public String sign(String sharedSecret, String data) throws Exception
    {
        Mac mac = setKey(sharedSecret)
        
        // Signed String must be BASE64 encoded.
        byte[] signBytes = mac.doFinal(data.getBytes("UTF8"));
        String signature = encodeBase64(signBytes);
        return signature;
    }
    
    private Mac setKey(String sharedSecret) throws Exception
    {
        Mac mac = Mac.getInstance("HmacSHA1");
        byte[] keyBytes = sharedSecret.getBytes("UTF8");
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
        mac.init(signingKey);
        return mac
    }

    private String encodeBase64(byte[] signBytes) {
        return Base64.encodeBase64URLSafeString(signBytes)
    }

    def logParameters(Object params) {
        log.debug "----------------------------------"
        for( param in params ) log.debug "${param.getKey()}=${param.getValue()}"
        log.debug "----------------------------------"
    }

    def boolean isSSLEnabled(String query) {
        if ( ssl_enabled == null ) {
			ssl_enabled = false
			log.debug("Pinging SSL connection")

			try {
				// open connection
				StringBuilder urlStr = new StringBuilder(query)
				URL url = new URL(urlStr.toString())
				HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection()
				httpConnection.setUseCaches(false)
				httpConnection.setDoOutput(true)
				httpConnection.setRequestMethod("HEAD")
				httpConnection.setConnectTimeout(5000)
				httpConnection.connect()

				int responseCode = httpConnection.getResponseCode()
				if (responseCode == HttpURLConnection.HTTP_OK) {
					ssl_enabled = true
				} else {
					log.debug("HTTPERROR: Message=" + "BBB server responded with HTTP status code " + responseCode)
				}

			} catch(IOException e) {
				log.debug("IOException: Message=" + e.getMessage())
			} catch(IllegalArgumentException e) {
				log.debug("IllegalArgumentException: Message=" + e.getMessage())
			} catch(Exception e) {
				log.debug("Exception: Message=" + e.getMessage())
			}
		}

		return ssl_enabled
    }

    public getToolConsumerProfile(String query) {
        JSONObject toolConsumerProfile = ltiProxyRequest(query)
        log.debug toolConsumerProfile
    }
    
    /** Make an API call */
    private JSONObject ltiProxyRequest(String query) {
        StringBuilder urlStr = new StringBuilder(query);

        try {
            // open connection
            //log.debug("doAPICall.call: " + query );

            URL url = new URL(urlStr.toString());
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setUseCaches(false);
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("GET");
            httpConnection.connect();

            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // read response
                InputStreamReader isr = null;
                BufferedReader reader = null;
                StringBuilder json = new StringBuilder();
                try {
                    isr = new InputStreamReader(httpConnection.getInputStream(), "UTF-8");
                    reader = new BufferedReader(isr);
                    String line;
                    while ( line = reader.readLine() ) {
                        json.append(line);
                    }
                } finally {
                    if (reader != null)
                        reader.close();
                    if (isr != null)
                        isr.close();
                }
                httpConnection.disconnect();

                String jsonString = json.toString()
                JSONObject ltiProxyResponse = new JSONObject(jsonString) 
                return ltiProxyResponse
            } else {
                log.debug("ltiProxyRequest.HTTPERROR: Message=" + "BBB server responded with HTTP status code " + responseCode);
            }
        } catch(IOException e) {
            log.debug("ltiProxyRequest.IOException: Message=" + e.getMessage());
        } catch(IllegalArgumentException e) {
            log.debug("ltiProxyRequest.IllegalArgumentException: Message=" + e.getMessage());
        } catch(Exception e) {
            log.debug("ltiProxyRequest.Exception: Message=" + e.getMessage());
        }
    }

    public Map jsonToMap(JSONObject json) throws JSONException
    {
        Map<String, Object> retMap = new HashMap<String, Object>();

        if(json != JSONObject.NULL)
        {
            retMap = toMap(json);
        }
        return retMap;
    }

    public Map toMap(JSONObject object) throws JSONException
    {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext())
        {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray)
            {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject)
            {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public List toList(JSONArray array) throws JSONException
    {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++)
        {
            Object value = array.get(i);
            if(value instanceof JSONArray)
            {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject)
            {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

}
