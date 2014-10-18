package org.alfresco.consulting.indexer;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.alfresco.consulting.indexer.client.*;

import java.util.List;
import java.util.Map;

public class AlfrescoIndexerIT {

  @Test
  public void fetchNodes() {
    AlfrescoClient client = new WebScriptsAlfrescoClient(
        "http","localhost:8080",
        "/alfresco/service",
        "workspace",
        "SpacesStore",
        "admin","admin");
    assertNotNull(client);

    AlfrescoResponse response = client.fetchNodes(0, 0, new AlfrescoFilters());
    List<Map<String, Object>> list = response.getDocumentList();

    assertEquals(65, list.size());

    Map<String, Object> doc = list.get(0);
    assertEquals("SpacesStore", response.getStoreId());
    assertEquals("workspace", response.getStoreProtocol());

    for(Map<String, Object> d : list) {
      String type = (String)d.get("type");
      String uuid = (String)d.get("uuid");
      Boolean deleted = (Boolean)d.get("deleted");
      if ("cm:content".equals(type) && deleted == false) {
        Map<String, Object> metadata = client.fetchMetadata(uuid);
        String path = (String)metadata.get("path");
        if ("/app:company_home/st:sites/cm:swsdp/cm:documentLibrary/cm:Agency_x0020_Files/cm:Logo_x0020_Files/cm:logo.png".equals(path)) {
          List<String> aspects = (List<String>)metadata.get("aspects");
          List<String> readableAuthorities = (List<String>)metadata.get("readableAuthorities");

          assertTrue(aspects.contains("cm:titled"));
          assertTrue(aspects.contains("exif:exif"));
          assertTrue(aspects.contains("cm:rateable"));

          assertTrue(readableAuthorities.contains("GROUP_EVERYONE"));
          assertTrue(readableAuthorities.contains("GROUP_site_swsdp_SiteConsumer"));
          assertTrue(readableAuthorities.contains("GROUP_site_swsdp_SiteContributor"));

          assertEquals("logo.png",metadata.get("cm:title"));
          assertEquals("abeecher",metadata.get("cm:modifier"));

          assertTrue(((String)metadata.get("documentUrl")).contains(uuid));
          assertTrue(((String)metadata.get("contentUrlPath")).contains(uuid));
          assertTrue(((String)metadata.get("shareUrlPath")).contains(uuid));
          assertTrue(((String)metadata.get("thumbnailUrlPath")).contains(uuid));
          assertTrue(((String)metadata.get("previewUrlPath")).contains(uuid));

          //System.out.println(metadata);

        }
      }
    }
  }
}