/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.maoo.indexer.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class WebScriptsAlfrescoClient implements AlfrescoClient {
  private static final String FIELD_PROPERTIES = "properties";
  
  private static final String LAST_TXN_ID = "last_txn_id";
  private static final String DOCS = "docs";
  private static final String LAST_ACL_CS_ID = "last_acl_changeset_id";
  
  private static final String URL_PARAM_LAST_TXN_ID = "lastTxnId";
  private static final String URL_PARAM_LAST_ACL_CS_ID = "lastAclChangesetId";
  private static final String URL_PARAM_INDEXING_FILTERS = "indexingFilters";
  
  private static final String STORE_ID = "store_id";
  private static final String STORE_PROTOCOL = "store_protocol";
  private static final String USERNAME = "username";
  private static final String AUTHORITIES = "authorities";
//  private static final String UUIDS = "uuids";
  private final Gson gson = new Gson();
  private final String changesUrl;
//  private final String uuidsUrl;
  private final String actionsUrl;
  private final String metadataUrl;
  private final String authoritiesUrl;
  private final String username;
  private final String password;

  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  private final Logger logger = LoggerFactory.getLogger(WebScriptsAlfrescoClient.class);
  private int connectTimeout = 60000;
  private int socketTimeout = 300000;
  private int requestTimeout = 60000;

  public WebScriptsAlfrescoClient(String protocol, String hostname,
                                  String endpoint, String storeProtocol, String storeId) {
    this(protocol, hostname, endpoint, storeProtocol, storeId, null, null);
  }

  public WebScriptsAlfrescoClient(String protocol, String hostname,
                                  String endpoint, String storeProtocol, String storeId, String username,
                                  String password) {
    changesUrl = String.format("%s://%s%s/node/changes/%s/%s", protocol, hostname, endpoint, storeProtocol, storeId);
//    uuidsUrl = String.format("%s://%s%s/node/uuids/%s/%s", protocol, hostname, endpoint, storeProtocol, storeId);
    actionsUrl = String.format("%s://%s%s/node/actions/%s/%s", protocol, hostname, endpoint, storeProtocol, storeId);
    metadataUrl = String.format("%s://%s%s/node/details/%s/%s", protocol, hostname, endpoint, storeProtocol, storeId);
    authoritiesUrl = String.format("%s://%s%s/auth/resolve/", protocol, hostname, endpoint);
    this.username = username;
    this.password = password;
  }

  @Override
  public AlfrescoResponse fetchNodes(long lastTransactionId,
		  long lastAclChangesetId,
		  AlfrescoFilters filters) {

	  String urlWithParameter = String.format("%s?%s",
			  changesUrl,
			  urlParameters(lastTransactionId, lastAclChangesetId, filters));
	  return getDocumentsActions(urlWithParameter);
  }

  @Override
  public AlfrescoResponse fetchNode(String nodeUuid) throws AlfrescoDownException {
	  String urlWithParameter = String.format("%s/%s", actionsUrl, nodeUuid);
	  return getDocumentsActions(urlWithParameter);
  }
  
  private AlfrescoResponse getDocumentsActions(String url){

	  logger.debug("Hitting url: {}", url);

	  try{
	    CloseableHttpClient httpClient = getHttpClient();
		  HttpGet httpGet = createGetRequest(url);
		  CloseableHttpResponse response = httpClient.execute(httpGet);
		  HttpEntity entity = response.getEntity();
		  AlfrescoResponse afResponse = fromHttpEntity(entity);
		  EntityUtils.consume(entity);
		  response.close();
		  httpClient.close();
		  return afResponse;
	  } catch (IOException e) {
		  logger.warn("Failed to fetch nodes.", e);
		  throw new AlfrescoDownException("Alfresco appears to be down", e);
	  }
  }

private HttpGet createGetRequest(String url) {
    HttpGet httpGet = new HttpGet(url);
    httpGet.addHeader("Accept", "application/json");
    if (useBasicAuthentication()) {
      httpGet.addHeader("Authorization", "Basic " + Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes(Charset.forName("UTF-8"))));
    }
    return httpGet;
  }

  private boolean useBasicAuthentication() {
    return username != null && !"".equals(username) && password != null;
  }

  private String urlParameters(long lastTransactionId, long lastAclChangesetId, AlfrescoFilters filters) {
    
	  String urlParameters = null;
	  if(filters != null && !filters.isEmpty()){
		  String indexingFilters=null;
		  try
		  {
			  indexingFilters = URLEncoder.encode(filters.toJSONString(),"UTF-8");
		  }
		  catch (UnsupportedEncodingException e)
		  {
			  indexingFilters= filters.toJSONString();
		  }

		  urlParameters = String.format("%s=%d&%s=%d&%s=%s",
				  URL_PARAM_LAST_TXN_ID, lastTransactionId,
				  URL_PARAM_LAST_ACL_CS_ID, lastAclChangesetId,
				  URL_PARAM_INDEXING_FILTERS, indexingFilters);
	  }else
		  urlParameters = String.format("%s=%d&%s=%d",
				  URL_PARAM_LAST_TXN_ID, lastTransactionId,
				  URL_PARAM_LAST_ACL_CS_ID, lastAclChangesetId);

	  return urlParameters;
  }

  private AlfrescoResponse fromHttpEntity(HttpEntity entity) throws IOException {
    Reader entityReader = new InputStreamReader(entity.getContent());
    JsonObject responseObject = gson.fromJson(entityReader, JsonObject.class);
    ArrayList<Map<String, Object>> documents = new ArrayList<Map<String, Object>>();

    long lastTransactionId = getStringAsLong(responseObject, LAST_TXN_ID, 0L);
    long lastAclChangesetId = getStringAsLong(responseObject, LAST_ACL_CS_ID, 0L);
    String storeId = getString(responseObject, STORE_ID);
    String storeProtocol = getString(responseObject, STORE_PROTOCOL);

    if (responseObject.has(DOCS) && responseObject.get(DOCS).isJsonArray()) {
      JsonArray docsArray = responseObject.get(DOCS).getAsJsonArray();
      for (JsonElement documentElement : docsArray) {
        Map<String, Object> document = createDocument(documentElement);
        document.put(STORE_ID, storeId);
        document.put(STORE_PROTOCOL, storeProtocol);
        documents.add(document);
      }
    } else {
      logger.warn("No documents found in response!");
    }

    return new AlfrescoResponse(lastTransactionId, lastAclChangesetId, storeId, storeProtocol, documents);
  }
  
//  private Collection<String> extractIDs(HttpEntity entity) throws IOException {
//	  Collection<String> uuids = Sets.newHashSet();
//	  Reader entityReader = new InputStreamReader(entity.getContent());
//	  JsonObject responseObject = gson.fromJson(entityReader, JsonObject.class);
//	  JsonArray array = responseObject.getAsJsonArray(UUIDS);
//	  for(JsonElement nextId:array)
//		  uuids.add(nextId.getAsString());
//	  return uuids;
//  }

  private long getStringAsLong(JsonObject responseObject, String key, long defaultValue) {
    String string = getString(responseObject, key);
    if (Strings.isNullOrEmpty(string)) {
      return defaultValue;
    }
    return Long.parseLong(string);
  }

  private String getString(JsonObject responseObject, String key) {
    if (responseObject.has(key)) {
      JsonElement element = responseObject.get(key);
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
        return element.getAsString();
      } else {
        logger.warn("The {} property (={}) is not a string in document: {}", new Object[]{key, element, responseObject});
      }
    } else {
      logger.warn("The key {} is missing from document: {}", key, responseObject);
    }
    return "";
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> createDocument(JsonElement documentElement) {
    if (documentElement.isJsonObject()) {
      JsonObject documentObject = documentElement.getAsJsonObject();
      return (Map<String, Object>) gson.fromJson(documentObject, Map.class);
    }
    return new HashMap<String, Object>();
  }
 
  @Override
  public Map<String, Object> fetchMetadata(String nodeUuid)
          throws AlfrescoDownException {
	String json = fetchMetadataJson(nodeUuid);

    @SuppressWarnings("unchecked")
    Map<String, Object> map = gson.fromJson(json, Map.class);

    List<Map<String, String>> properties = extractPropertiesFieldFromMap(nodeUuid, map);

    for (Map<String, String> e : properties) {
      map.put(e.get("name"), this.parseValue(e.get("type"), e.get("value")));
    }
    return map;
  }

  private String fetchMetadataJson(String nodeUuid) {
    String fullUrl = String.format("%s/%s", metadataUrl, nodeUuid);
    logger.debug("url: {}", fullUrl);
    try {
      CloseableHttpClient httpClient = getHttpClient();
      HttpGet httpGet = createGetRequest(fullUrl);
      CloseableHttpResponse response = httpClient.execute(httpGet);
      HttpEntity entity = response.getEntity();
      String result = CharStreams.toString(new InputStreamReader(entity.getContent(),
              "UTF-8"));
      response.close();
      httpClient.close();
      return result;
    } catch (IOException e) {
      throw new AlfrescoDownException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, String>> extractPropertiesFieldFromMap(String nodeUuid,
          Map<String, Object> map) {
    Object properties = map.remove(FIELD_PROPERTIES);
    if(properties == null){
    	throw new AlfrescoDownException("No Properties Fetched for the Node " + nodeUuid);
    }

    if (!(properties instanceof List)) {
      throw new AlfrescoDownException(FIELD_PROPERTIES
              + " is not of type List, it is of type " + properties.getClass());
    }
    return (List<Map<String, String>>) properties;
  }

  private AlfrescoUser userFromHttpEntity(HttpEntity entity) throws IOException {
    Reader entityReader = new InputStreamReader(entity.getContent());
    JsonElement responseObject = gson.fromJson(entityReader, JsonElement.class);
    if (responseObject instanceof JsonObject) {
      return getUser((JsonObject)responseObject);
    } else if (responseObject instanceof JsonArray) {
      return getUser(((JsonArray)responseObject).get(0).getAsJsonObject());
    } else {
      return getUser(responseObject.getAsJsonObject());
    }
  }

  private AlfrescoUser getUser(JsonObject responseObject) {
    String username = getUsername(responseObject);
    List<String> authorities = getAuthorities(responseObject);
    return new AlfrescoUser(username, authorities);
  }

  private String getUsername(JsonObject userObject) {
    if (!userObject.has(USERNAME)) {
      throw new AlfrescoParseException("Json response is missing username.");
    }
    JsonElement usernameElement = userObject.get(USERNAME);
    if (!usernameElement.isJsonPrimitive() || !usernameElement.getAsJsonPrimitive().isString()) {
      throw new AlfrescoParseException("Username must be a string. It was: " + usernameElement.toString());
    }
    return usernameElement.getAsString();
  }

  private List<String> getAuthorities(JsonObject userObject) {
    List<String> authorities = new ArrayList<String>();
    if (!userObject.has(AUTHORITIES)) {
      throw new AlfrescoParseException("Json response is authorities.");
    }
    JsonElement authoritiesElement = userObject.get(AUTHORITIES);
    if (!authoritiesElement.isJsonArray()) {
      throw new AlfrescoParseException("Authorities must be a json array. It was: " + authoritiesElement.toString());
    }
    JsonArray authoritiesArray = authoritiesElement.getAsJsonArray();
    for (JsonElement authorityElement : authoritiesArray) {
      if (!authorityElement.isJsonPrimitive()) {
        throw new AlfrescoParseException("Authority entry must be a string. It was: " + authoritiesElement.toString());
      }
      JsonPrimitive authorityPrimitive = authorityElement.getAsJsonPrimitive();
      if (!authorityPrimitive.isString()) {
        throw new AlfrescoParseException("Authority entry must be a string. It was: " + authoritiesElement.toString());
      }
      authorities.add(authorityPrimitive.getAsString());
    }
    return authorities;
  }

  @Override
  public AlfrescoUser fetchUserAuthorities(String username)
		  throws AlfrescoDownException {
	  CloseableHttpResponse response=null;
    CloseableHttpClient httpClient = getHttpClient();
	  try {
		  String url = String.format("%s%s", authoritiesUrl, username);

		  if (logger.isDebugEnabled()) {
			  logger.debug("Hitting url: " + url);
		  }

		  HttpGet httpGet = createGetRequest(url);
		  response = httpClient.execute(httpGet);
		  HttpEntity entity = response.getEntity();
		  AlfrescoUser afResponse = userFromHttpEntity(entity);
		  EntityUtils.consume(entity);
		  return afResponse;
	  } catch (IOException e) {
		  if (logger.isDebugEnabled()) {
			  logger.warn("Failed to fetch nodes.", e);
		  }
		  throw new AlfrescoDownException("Alfresco appears to be down", e);
	  }finally{

		  try {
			  if(response!=null)
				  response.close();
			  httpClient.close();
		  } catch (IOException e) {
			  // Nothing to do
		  }
	  }
  }

  @Override
  public List<AlfrescoUser> fetchAllUsersAuthorities()
          throws AlfrescoDownException {
    HttpResponse response;
    try {
      CloseableHttpClient httpClient = getHttpClient();

      if (logger.isDebugEnabled()) {
        logger.debug("Hitting url: " + authoritiesUrl);
      }

      HttpGet httpGet = createGetRequest(authoritiesUrl);
      response = httpClient.execute(httpGet);
      HttpEntity entity = response.getEntity();
      List<AlfrescoUser> users = usersFromHttpEntity(entity);
      EntityUtils.consume(entity);
      return users;
    } catch (IOException e) {
      if (logger.isDebugEnabled()) {
        logger.warn("Failed to fetch nodes.", e);
      }
      throw new AlfrescoDownException("Alfresco appears to be down", e);
    }
  }

  private List<AlfrescoUser> usersFromHttpEntity(HttpEntity entity) throws IOException {
    Reader entityReader = new InputStreamReader(entity.getContent());
    JsonElement responseObject = gson.fromJson(entityReader, JsonElement.class);
    if (!responseObject.isJsonArray()) {
      throw new AlfrescoParseException("Users must be a json array.");
    }
    List<AlfrescoUser> users = new ArrayList<AlfrescoUser>();
    JsonArray usersArray = responseObject.getAsJsonArray();
    for (JsonElement userElement : usersArray) {
      if (!userElement.isJsonObject()) {
        throw new AlfrescoParseException("User must be a json object.");
      }
      AlfrescoUser user = getUser(userElement.getAsJsonObject());
      users.add(user);
    }
    return users;
  }

  @Override
  public InputStream fetchContent(String contentUrlPath) {
	  HttpGet httpGet = new HttpGet(contentUrlPath);
	  httpGet.addHeader("Accept", MediaType.APPLICATION_BINARY.toString());
	  if (useBasicAuthentication()) {
		  httpGet.addHeader("Authorization", "Basic " + Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes(Charset.forName("UTF-8"))));
	  }

	  CloseableHttpClient httpClient = getHttpClient();
	  try {
		HttpResponse response = httpClient.execute(httpGet);
		InputStream stream = response.getEntity().getContent(); 
		return stream;
	} catch (Exception e) {
		throw new AlfrescoDownException("Alfresco appears to be down", e);
	}
  }
  
  private Object parseValue(String type, String value) {
	  if (type == null)
		  return value;
	  
	  try {
		  Class<?> typeClass = Class.forName(type);
		  if (String.class.isAssignableFrom(typeClass)) {
		  } else if (Boolean.class.isAssignableFrom(typeClass)) {
			  return new Boolean(value);
		  } else if (Number.class.isAssignableFrom(typeClass)) {
			  Constructor<?> constructor = typeClass.getConstructor(String.class);
			  return constructor.newInstance(value);
		  } else if (java.util.Date.class.isAssignableFrom(typeClass)) {
			  synchronized (this.sdf) {
				  return this.sdf.parse(value);
			  }
		  }
	  } catch (ClassNotFoundException cnfe) {
		  this.logger.warn("Class " + type + " not found; skipping conversion from java.util.String");
	  } catch (NoSuchMethodException nsme) {
		  this.logger.error("Class " + type + " does not have a java.util.String constructor, as expected; skipping conversion from java.util.String");
	  } catch (InvocationTargetException ite) {
		  this.logger.error(ite.getMessage() + "; skipping conversion from java.util.String", ite.getTargetException());
	  } catch (IllegalAccessException iae) {
		  this.logger.error("The java.util.String constructor is not public; skipping conversion from java.util.String");
	  } catch (InstantiationException ie) {
		  this.logger.error("Class " + type + " cannot be instantiated; skipping conversion from java.util.String");
	  } catch (ParseException pe) {
		  this.logger.error("The " + value + " date could not be parsed; skipping conversion from java.util.String");
	  }

	  return value;
  }

  public CloseableHttpClient getHttpClient() {
    RequestConfig config = RequestConfig.copy(RequestConfig.DEFAULT)
        .setConnectTimeout(this.connectTimeout)
        .setSocketTimeout(this.socketTimeout)
        .setConnectionRequestTimeout(this.requestTimeout)
        .build();
    CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build();

    return httpClient;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  public void setRequestTimeout(int requestTimeout) {
    this.requestTimeout = requestTimeout;
  }
}
