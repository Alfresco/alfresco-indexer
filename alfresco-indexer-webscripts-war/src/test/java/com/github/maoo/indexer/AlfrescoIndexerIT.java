package com.github.maoo.indexer;

import com.github.maoo.indexer.client.AlfrescoClient;
import com.github.maoo.indexer.client.AlfrescoFilters;
import com.github.maoo.indexer.client.AlfrescoResponse;
import com.github.maoo.indexer.client.WebScriptsAlfrescoClient;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AlfrescoIndexerIT {

  private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHss");

  @Test
  public void fetchNodes() {
    AlfrescoClient client = new WebScriptsAlfrescoClient(
        "http", "localhost:8080",
        "/alfresco/service",
        "workspace",
        "SpacesStore",
        "admin", "admin");
    assertNotNull(client);

    //Fetching all Alfresco nodes that have (ever) changed, expecting 65 results
    AlfrescoResponse response = client.fetchNodes(0, 0, new AlfrescoFilters());
    List<Map<String, Object>> list = response.getDocumentList();
    assertEquals(65, list.size());

    //Checking results
    assertEquals("SpacesStore", response.getStoreId());
    assertEquals("workspace", response.getStoreProtocol());

    //Iterate on all changed nodes
    for (Map<String, Object> d : list) {
      String uuid = (String) d.get("uuid");

      //Fetching metadata of the changed node
      Map<String, Object> metadata = client.fetchMetadata(uuid);
      String path = (String) metadata.get("path");

      //Checking metadata of one specific node, given its path
      if ("/app:company_home/st:sites/cm:swsdp/cm:documentLibrary/cm:Agency_x0020_Files/cm:Logo_x0020_Files/cm:logo.png".equals(path)) {
        assertMetadata(uuid, metadata);
      }
    }

    //Adding a document in Alfresco via CMIS
    CMISUtils cdc = new CMISUtils();
    cdc.setUser("admin");
    cdc.setPassword("admin");
    cdc.createDocument("test" + "." + sdf.format(new Date()), "cmis:document");

    //Fetching nodes again, starting from latest transaction and ACL changeset IDs
    response = client.fetchNodes(response.getLastTransactionId(), response.getLastAclChangesetId(), new AlfrescoFilters());
    list = response.getDocumentList();
    assertEquals(1, list.size());

    //Checking metadata
    String uuid = (String) list.get(0).get("uuid");
    Map<String, Object> metadata = client.fetchMetadata(uuid);
    assertTrue(((String) metadata.get("cm:name")).startsWith("test"));
    assertEquals("admin", metadata.get("cm:modifier"));
  }

  private void assertMetadata(String uuid, Map<String, Object> metadata) {
    List<String> aspects = (List<String>) metadata.get("aspects");
    List<String> readableAuthorities = (List<String>) metadata.get("readableAuthorities");

    assertTrue(aspects.contains("cm:titled"));
    assertTrue(aspects.contains("exif:exif"));
    assertTrue(aspects.contains("cm:rateable"));

    assertTrue(readableAuthorities.contains("GROUP_EVERYONE"));
    assertTrue(readableAuthorities.contains("GROUP_site_swsdp_SiteConsumer"));
    assertTrue(readableAuthorities.contains("GROUP_site_swsdp_SiteContributor"));

    assertEquals("logo.png", metadata.get("cm:title"));
    assertEquals("abeecher", metadata.get("cm:modifier"));

    assertTrue(((String) metadata.get("documentUrl")).contains(uuid));
    assertTrue(((String) metadata.get("contentUrlPath")).contains(uuid));
    assertTrue(((String) metadata.get("shareUrlPath")).contains(uuid));
    assertTrue(((String) metadata.get("thumbnailUrlPath")).contains(uuid));
    assertTrue(((String) metadata.get("previewUrlPath")).contains(uuid));
  }
}