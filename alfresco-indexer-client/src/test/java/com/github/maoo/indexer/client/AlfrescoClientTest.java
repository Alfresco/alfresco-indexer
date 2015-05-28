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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.github.maoo.indexer.client.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.io.CharStreams;

/**
 * @author martin.nycander
 */
public class AlfrescoClientTest {
  private static final String STORE_PROTOCOL = "workspace";
  private static final String STORE_ID = "spacesStore";
  private final String lastTransactionParam = "lastTxnId";
  private final String lastAclChangesetParam = "lastAclChangesetId";
  private final String changesEndpoint = "/alfresco/service/node/changes/" + STORE_PROTOCOL + "/" +
      STORE_ID + "\\?" +
      lastTransactionParam + "=[0-9]+&" +
      lastAclChangesetParam + "=[0-9]+";
  private final String metadataEndpoint = "/alfresco/service/node/details/" + STORE_PROTOCOL + "/" +
      STORE_ID + "/";
  private final String authoritiesEndpoint = "/alfresco/service/auth/resolve/";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8089);

  private AlfrescoClient client;

  @Before
  public void setup() {
    client = new WebScriptsAlfrescoClient("http", "localhost:8089", "/alfresco/service",
        STORE_PROTOCOL, STORE_ID);
  }

  private void stubResult(String body) {
    stubFor(get(urlMatching(changesEndpoint))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(body)));
  }

  @Test
  public void whenASingleDocumentIsReturnedItIsIndexed() throws Exception {
    String noderef = "abc123";
    String type = "cm:content";
    boolean deleted = false;

    String storeId = STORE_ID;
    String storeProtocol = STORE_PROTOCOL;

    stubResult("{ \"docs\": [ { \"uuid\": \"" + noderef + "\", " +
        "\"type\": \"" + type + "\", " +
        "\"deleted\": " + deleted + "} ], " +
        "\"store_id\": \"" + storeId + "\", " +
        "\"store_protocol\": \"" + storeProtocol + "\", " +
        "\"last_txn_id\": 2," +
        "\"last_acl_changeset_id\": 2  } ");

    AlfrescoResponse response = client.fetchNodes(0, 0, new AlfrescoFilters());
    List<Map<String, Object>> list = response.getDocumentList();

    Assert.assertEquals(1, list.size());

    Map<String, Object> doc = list.get(0);

    assertEquals(noderef, doc.get("uuid"));
    assertEquals(type, doc.get("type"));
    assertEquals(deleted, doc.get("deleted"));
    assertEquals(STORE_ID, response.getStoreId());
    assertEquals(STORE_PROTOCOL, response.getStoreProtocol());
  }

  @Test
  public void whenADocumentIsFetchedItShouldBeEnrichedWithStoreIdAndStoreProtocol() throws Exception {
    String noderef = "abc123";
    String type = "cm:content";
    boolean deleted = false;

    String storeId = STORE_ID;
    String storeProtocol = STORE_PROTOCOL;

    stubResult("{ \"docs\": [ { \"uuid\": \"" + noderef + "\", " +
        "\"type\": \"" + type + "\", " +
        "\"deleted\": " + deleted + "} ], " +
        "\"store_id\": \"" + storeId + "\", " +
        "\"store_protocol\": \"" + storeProtocol + "\", " +
        "\"last_txn_id\": 2," +
        "\"last_acl_changeset_id\": 2  } ");

    AlfrescoResponse response = client.fetchNodes(0, 0, new AlfrescoFilters());
    List<Map<String, Object>> list = response.getDocumentList();

    Assert.assertEquals(1, list.size());

    Map<String, Object> doc = list.get(0);

    assertEquals(STORE_ID, doc.get("store_id"));
    assertEquals(STORE_PROTOCOL, doc.get("store_protocol"));
    assertEquals(STORE_ID, response.getStoreId());
    assertEquals(STORE_PROTOCOL, response.getStoreProtocol());
  }

  @Test
  public void whenEmptyListIsReturnedItIsHandled() throws Exception {
    stubResult("{ \"docs\": [ ], " +
        "\"store_id\": \"\", " +
        "\"store_protocol\": \"\", " +
        "\"last_txn_id\": 0," +
        "\"last_acl_changeset_id\": 0  } ");

    AlfrescoResponse response = client.fetchNodes(0, 0, new AlfrescoFilters());
    assertTrue(response.getDocumentList().isEmpty());
  }

  @Test
  public void whenLastTransactionIdIsReturnedItShouldBeUsedOnTheNextRequest() throws
      Exception {
    stubResult("{ }");

    long lastTransactionId = 0;
    long lastAclChangesetId = 0;
    client.fetchNodes(lastTransactionId, 0, new AlfrescoFilters());
    List<LoggedRequest> requests = WireMock.findAll(
        getRequestedFor(urlMatching(changesEndpoint)));

    assertEquals(1, requests.size());
    assertTrue(requests.get(0).getUrl().contains(this.lastTransactionParam+"="+lastTransactionId));
    assertTrue(requests.get(0).getUrl().contains(this.lastAclChangesetParam+"="+lastAclChangesetId));
  }

  @Test(expected = AlfrescoDownException.class)
  public void whenAlfrescoIsDownAnExceptionShouldBeThrown() throws Exception {
    stubFor(get(urlMatching(changesEndpoint)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

    AlfrescoResponse alfrescoResponse = client.fetchNodes(0, 0, new AlfrescoFilters());

    assertNotNull("Response should never be null", alfrescoResponse);
  }

  @Test
  public void whenAProperNodeIsGivenThenTheMetadataEndpointIsQueried()
      throws Exception {
    String testFile = CharStreams.toString(new InputStreamReader(getClass()
        .getResourceAsStream("/metadata.sample.json"), "UTF-8"));

    String uuid = "abc123";

    stubFor(get(
        urlEqualTo(metadataEndpoint + uuid)).willReturn(
        aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json").withBody(testFile)));

    Map<String, Object> metadata = client.fetchMetadata(uuid);

    Assert.assertEquals(Arrays.asList("1", "2", "3"),
        metadata.get("readableAuthorities"));
    Assert.assertEquals(Arrays.asList("a", "b", "c"),
        metadata.get("aspects"));
    Assert.assertEquals("A/B/C", metadata.get("path"));
    Assert.assertEquals("pluto", metadata.get("pippo"));
    Assert.assertEquals("5", metadata.get("foo"));
  }

  @Test
  public void whenFetchUserAuthoritiesIsCalledTheCorrectUserAndAuthoritiesShouldBeReturned()
      throws Exception {

    String testFile = CharStreams.toString(
        new InputStreamReader(
                getClass().getResourceAsStream("/authorities.sample.json"),
                "UTF-8"));

    stubFor(get(
        urlEqualTo(authoritiesEndpoint + "admin")).willReturn(
        aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json").withBody(testFile)));

    AlfrescoUser user = client.fetchUserAuthorities("admin");

    List<String> expectedGroups = Arrays.asList(
        "GROUP_ALFRESCO_ADMINISTRATORS", "GROUP_EMAIL_CONTRIBUTORS",
        "GROUP_EVERYONE", "GROUP_site_konner", "GROUP_site_konner_SiteManager",
        "GROUP_site_swsdp", "GROUP_site_swsdp_SiteManager",
        "ROLE_ADMINISTRATOR");

    assertEquals("admin", user.getUsername());
    assertEquals(expectedGroups, user.getAuthorities());
  }

  @Test
  public void whenFetchAllUsersAuthoritiesIsCalledTheCorrectUserAndAuthoritiesShouldBeReturned()
      throws Exception {
    String testFile = CharStreams.toString(
        new InputStreamReader(
                getClass().getResourceAsStream("/multi.authorities.sample.json"),
                "UTF-8"));
    stubFor(get(
        urlEqualTo(authoritiesEndpoint)).willReturn(
        aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json").withBody(testFile)));
    List<AlfrescoUser> users = client.fetchAllUsersAuthorities();

    assertEquals(3, users.size());

    AlfrescoUser guest = users.get(0);
    assertEquals("guest", guest.getUsername());
    assertEquals(Arrays.asList("ROLE_GUEST"), guest.getAuthorities());

    AlfrescoUser abeecher = users.get(1);
    assertEquals("abeecher", abeecher.getUsername());
    assertEquals(Arrays.asList("GROUP_EVERYONE", "GROUP_site_swsdp",
        "GROUP_site_swsdp_SiteCollaborator"), abeecher.getAuthorities());

    AlfrescoUser mjackson = users.get(2);
    assertEquals("mjackson", mjackson.getUsername());
    assertEquals(Arrays.asList("GROUP_EVERYONE", "GROUP_site_swsdp",
        "GROUP_site_swsdp_SiteManager"), mjackson.getAuthorities());
  }

  @Test
  public void whenUsernameAndPasswordAreConfiguredBasicAuthenticationShouldBeUsed() throws Exception {
    String noderef = "abc123";
    String type = "cm:content";
    boolean deleted = false;

    String storeId = STORE_ID;
    String storeProtocol = STORE_PROTOCOL;

    stubFor(get(urlMatching(changesEndpoint))
        .withHeader("Authorization", equalTo("Basic dXNlcm5hbWU6cGFzc3dvcmQ="))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{ \"docs\": [ { \"uuid\": \"" + noderef + "\", " +
                "\"type\": \"" + type + "\", " +
                "\"deleted\": " + deleted + "} ], " +
                "\"store_id\": \"" + storeId + "\", " +
                "\"store_protocol\": \"" + storeProtocol + "\", " +
                "\"last_txn_id\": 2, "+
                "\"last_acl_changeset_id\": 2 } ")));

    client = new WebScriptsAlfrescoClient("http", "localhost:8089", "/alfresco/service", STORE_PROTOCOL, STORE_ID, "username", "password");
    AlfrescoResponse response = client.fetchNodes(0, 0, new AlfrescoFilters());
    List<Map<String, Object>> list = response.getDocumentList();

    Assert.assertEquals(1, list.size());

    Map<String, Object> doc = list.get(0);

    assertEquals(noderef, doc.get("uuid"));
    assertEquals(type, doc.get("type"));
    assertEquals(deleted, doc.get("deleted"));
    assertEquals(STORE_ID, response.getStoreId());
    assertEquals(STORE_PROTOCOL, response.getStoreProtocol());
  }
}
