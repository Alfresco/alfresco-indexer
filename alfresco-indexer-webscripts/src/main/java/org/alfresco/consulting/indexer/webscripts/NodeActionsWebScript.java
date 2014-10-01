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

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateHashModel;

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
import org.springframework.extensions.webscripts.*;

import java.util.*;

/**
 * 
 * @author iarroyo
 *
 */
public class NodeActionsWebScript extends DeclarativeWebScript
{

    protected static final Log logger = LogFactory.getLog(NodeActionsWebScript.class);

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {

        // Fetching request params
        Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
        String storeId = templateArgs.get("storeId");
        String storeProtocol = templateArgs.get("storeProtocol");
        String uuid= templateArgs.get("uuid");
        

        // Getting the Store ID on which the changes are requested
        Pair<Long, StoreRef> store = nodeDao.getStore(new StoreRef(storeProtocol, storeId));
        if (store == null)
        {
            throw new IllegalArgumentException("Invalid store reference: " + storeProtocol + "://" + storeId);
        }

        Set<NodeEntity> nodes = new HashSet<NodeEntity>();
        
        NodeEntity node= indexingService.getNodeByUuid(store, uuid);
        if(node!=null){
            nodes.add(node); 
        }

        // Render them out
        Map<String, Object> model = new HashMap<String, Object>(1, 1.0f);
        model.put("qnameDao", qnameDao);
        model.put("nsResolver", namespaceService);
        model.put("nodes", nodes);
        model.put("storeId", storeId);
        model.put("storeProtocol", storeProtocol);
        model.put("propertiesUrlTemplate", propertiesUrlTemplate);

        // This allows to call the static method QName.createQName from the FTL template
        try
        {
            BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
            TemplateHashModel staticModels = wrapper.getStaticModels();
            TemplateHashModel qnameStatics = (TemplateHashModel) staticModels.get("org.alfresco.service.namespace.QName");
            model.put("QName", qnameStatics);
        }
        catch (Exception e)
        {
            throw new AlfrescoRuntimeException(
                    "Cannot add BeansWrapper for static QName.createQName method to be used from a Freemarker template",
                    e);
        }

        logger.debug(String.format("Attaching %s nodes to the WebScript template", nodes.size()));

        return model;
    }

    private NamespaceService namespaceService;
    private QNameDAO qnameDao;
    private IndexingDaoImpl indexingService;
    private NodeDAO nodeDao;

    private String propertiesUrlTemplate;

    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    public void setQnameDao(QNameDAO qnameDao)
    {
        this.qnameDao = qnameDao;
    }

    public void setIndexingService(IndexingDaoImpl indexingService)
    {
        this.indexingService = indexingService;
    }

    public void setNodeDao(NodeDAO nodeDao)
    {
        this.nodeDao = nodeDao;
    }

    public void setPropertiesUrlTemplate(String propertiesUrlTemplate)
    {
        this.propertiesUrlTemplate = propertiesUrlTemplate;
    }

//    public void setMaxNodesPerAcl(int maxNodesPerAcl)
//    {
//        this.maxNodesPerAcl = maxNodesPerAcl;
//    }
//
//    public void setMaxNodesPerTxns(int maxNodesPerTxns)
//    {
//        this.maxNodesPerTxns = maxNodesPerTxns;
//    }
}