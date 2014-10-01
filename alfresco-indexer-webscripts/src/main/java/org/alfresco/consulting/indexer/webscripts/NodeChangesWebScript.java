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
package org.apache.manifoldcf.integration.alfresco.indexer.webscripts;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.manifoldcf.integration.alfresco.indexer.dao.IndexingDaoImpl;
import org.apache.manifoldcf.integration.alfresco.indexer.entities.NodeEntity;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateHashModel;

/**
 * Renders out a list of nodes (UUIDs) that have been changed in Alfresco; the changes can affect:
 * - A node metadata
 * - Node content
 * - Node ACLs
 *
 * Please check src/main/amp/config/alfresco/extension/templates/webscripts/org/alfresco/consulting/indexer/webscripts/changes.get.desc.xml
 * to know more about the RestFul interface to invoke the WebScript
 *
 * List of pending activities (or TODOs)
 * - Move private/static logic into the IndexingService
 * - Using JSON libraries (or StringBuffer), render out the payload without passing through FreeMarker template
 * - Wrap (or Proxy) IndexingDaoImpl into an IndexingService, which (optionally) performs any object manipulation
 */
public class NodeChangesWebScript extends DeclarativeWebScript {

  protected static final Log logger = LogFactory.getLog(NodeChangesWebScript.class);

  @Override
  protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

    //start time  
    long startTime = System.currentTimeMillis();  
      
    //Fetching request params
    Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
    String storeId = templateArgs.get("storeId");
    String storeProtocol = templateArgs.get("storeProtocol");
    String lastTxnIdString = req.getParameter("lastTxnId");
    String lastAclChangesetIdString = req.getParameter("lastAclChangesetId");
    String maxTxnsString = req.getParameter("maxTxns");
    String maxAclChangesetsString = req.getParameter("maxAclChangesets");

    //Parsing parameters passed from the WebScript invocation
    Long lastTxnId = (lastTxnIdString == null ? null : Long.valueOf(lastTxnIdString));
    Long lastAclChangesetId = (lastAclChangesetIdString == null ? null : Long.valueOf(lastAclChangesetIdString));
    Integer maxTxns = (maxTxnsString == null ? maxNodesPerTxns : Integer.valueOf(maxTxnsString));
    Integer maxAclChangesets = (maxAclChangesetsString == null ? maxNodesPerAcl : Integer.valueOf(maxAclChangesetsString));
    
    JSONObject indexingFilters=null;
    try
    {
        indexingFilters = req.getParameter("indexingFilters")!=null ? 
               (JSONObject) JSONValue.parse(URLDecoder.decode(req.getParameter("indexingFilters"),"UTF-8")): null;
    }
    catch (UnsupportedEncodingException e)
    {
        throw new WebScriptException(e.getMessage(),e);
    }
    
    logger.debug(String.format("Invoking Changes Webscript, using the following params\n" +
        "lastTxnId: %s\n" +
        "lastAclChangesetId: %s\n" +
        "storeId: %s\n" +
        "storeProtocol: %s\n" +
        "indexingFilters: %s\n", lastTxnId, lastAclChangesetId, storeId, storeProtocol, indexingFilters));

    //Indexing filters
    if(indexingFilters!=null){
        setIndexingFilters(indexingFilters);
    }
    
    //Getting the Store ID on which the changes are requested
    Pair<Long,StoreRef> store = nodeDao.getStore(new StoreRef(storeProtocol, storeId));
    if(store == null)
    {
        throw new IllegalArgumentException("Invalid store reference: " + storeProtocol + "://" + storeId);
    }

    Set<NodeEntity> nodes = new HashSet<NodeEntity>();
    //Updating the last IDs being processed
    //Depending on params passed to the request, results will be rendered out
    if (lastTxnId == null) {
      lastTxnId = new Long(0);
    }
    List<NodeEntity> nodesFromTxns = indexingService.getNodesByTransactionId(store, lastTxnId, maxTxns);
    if (nodesFromTxns != null && nodesFromTxns.size() > 0) {
      nodes.addAll(nodesFromTxns);
    }
    
    //Set the last database transaction ID or increment it by maxTxns
    Long lastTxnIdDB= indexingService.getLastTransactionID();

    if((lastTxnId+maxTxns) > lastTxnIdDB){
        lastTxnId=lastTxnIdDB;
    }else{
        lastTxnId+=maxTxns;
    }
    
    
    
    if (lastAclChangesetId == null) {
      lastAclChangesetId = new Long(0);
    }
    List<NodeEntity> nodesFromAcls = indexingService.getNodesByAclChangesetId(store, lastAclChangesetId, maxAclChangesets);
    if (nodesFromAcls != null && nodesFromAcls.size() > 0) {
      nodes.addAll(nodesFromAcls);
    }
    
    //Set the last database aclChangeSet ID or increment it by maxAclChangesets
    Long lastAclChangesetIdDB= indexingService.getLastAclChangeSetID();

    if((lastAclChangesetId+maxAclChangesets) > lastAclChangesetIdDB){
        lastAclChangesetId=lastAclChangesetIdDB;
    }else{
        lastAclChangesetId+=maxAclChangesets;
    }
    
    //elapsed time
    long elapsedTime = System.currentTimeMillis() - startTime;

    //Render them out
    Map<String, Object> model = new HashMap<String, Object>(1, 1.0f);
    model.put("qnameDao", qnameDao);
    model.put("nsResolver", namespaceService);
    model.put("nodes", nodes);
    model.put("lastTxnId", lastTxnId);
    model.put("lastAclChangesetId", lastAclChangesetId);
    model.put("storeId", storeId);
    model.put("storeProtocol", storeProtocol);
    model.put("propertiesUrlTemplate", propertiesUrlTemplate);
    model.put("elapsedTime", elapsedTime);

    //This allows to call the static method QName.createQName from the FTL template
    try {
      BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
      TemplateHashModel staticModels = wrapper.getStaticModels();
      TemplateHashModel qnameStatics = (TemplateHashModel) staticModels.get("org.alfresco.service.namespace.QName");
      model.put("QName",qnameStatics);
    } catch (Exception e) {
      throw new AlfrescoRuntimeException(
          "Cannot add BeansWrapper for static QName.createQName method to be used from a Freemarker template", e);
    }

    logger.debug(String.format("Attaching %s nodes to the WebScript template", nodes.size()));

    return model;
  }

  @SuppressWarnings("unchecked")
private void setIndexingFilters(JSONObject indexingParams)
  {
      
      //Reset filters
      this.indexingService.setSites(Collections.<String> emptySet());
      this.indexingService.setMimeTypes(Collections.<String> emptySet());
      this.indexingService.setAspects(Collections.<String> emptySet());
      this.indexingService.setProperties(Collections.<String> emptySet());
      
      //Types filter
      List<String> types= (List<String>) indexingParams.get("typeFilters");
      
      if(types!=null && types.size()>0){
          this.indexingService.setAllowedTypes(new HashSet<String>(types));
      }
      
       //Site filter
       List<String> sites= (List<String>) indexingParams.get("siteFilters");
      
       if(sites!=null && sites.size()>0){
           this.indexingService.setSites(new HashSet<String>(sites));
       }
          
       //Mymetype filter
       List<String> mimetypes= (List<String>) indexingParams.get("mimetypeFilters");
       
       if(mimetypes!=null && mimetypes.size()>0){
           this.indexingService.setMimeTypes(new HashSet<String>(mimetypes));
       }
          
       //Aspect filter
       List<String> aspects= (List<String>) indexingParams.get("aspectFilters");
       
       if(aspects!=null && aspects.size()>0){
           this.indexingService.setAspects(new HashSet<String>(aspects));
       }
          
       //Metadata filter
       Map<String,String> auxMap= (Map<String, String>) indexingParams.get("metadataFilters");
       
       if(auxMap!=null && auxMap.size()>0){
           
           Set<String> metadataParams= new HashSet<String>(auxMap.size());
           Set<String> keys= auxMap.keySet();
           StringBuilder sb= new StringBuilder();
           
           for(String key:keys){
               sb.append(key).append(":").append(auxMap.get(key));
               metadataParams.add(sb.toString());
               //reset StringBuilder
               sb.setLength(0);
           }
           this.indexingService.setProperties(metadataParams);
       }
      

  }

  private NamespaceService namespaceService;
  private QNameDAO qnameDao;
  private IndexingDaoImpl indexingService;
  private NodeDAO nodeDao;

  private String propertiesUrlTemplate;
  private int maxNodesPerAcl = 1000;
  private int maxNodesPerTxns = 1000;


  public void setNamespaceService(NamespaceService namespaceService) {
    this.namespaceService = namespaceService;
  }
  public void setQnameDao(QNameDAO qnameDao) {
    this.qnameDao = qnameDao;
  }
  public void setIndexingService(IndexingDaoImpl indexingService) {
    this.indexingService = indexingService;
  }
  public void setNodeDao(NodeDAO nodeDao) {
    this.nodeDao = nodeDao;
  }

  public void setPropertiesUrlTemplate(String propertiesUrlTemplate) {
    this.propertiesUrlTemplate = propertiesUrlTemplate;
  }

  public void setMaxNodesPerAcl(int maxNodesPerAcl) {
    this.maxNodesPerAcl = maxNodesPerAcl;
  }

  public void setMaxNodesPerTxns(int maxNodesPerTxns) {
    this.maxNodesPerTxns = maxNodesPerTxns;
  }
}