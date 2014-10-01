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
package org.apache.manifoldcf.integration.alfresco.indexer.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.RowBounds;
import org.apache.manifoldcf.integration.alfresco.indexer.entities.NodeBatchLoadEntity;
import org.apache.manifoldcf.integration.alfresco.indexer.entities.NodeEntity;
import org.apache.manifoldcf.integration.alfresco.indexer.utils.Utils;
import org.mybatis.spring.SqlSessionTemplate;

public class IndexingDaoImpl
{

    private static final String SELECT_NODES_BY_ACLS = "alfresco.index.select_NodeIndexesByAclChangesetId";
    private static final String SELECT_NODES_BY_TXNS = "alfresco.index.select_NodeIndexesByTransactionId";
    private static final String SELECT_NODES_BY_UUID = "alfresco.index.select_NodeIndexesByUuid";
    private static final String SELECT_LAST_TRANSACTION_ID = "select_LastTransactionID";
    private static final String SELECT_LAST_ACL_CHANGE_SET_ID = "select_LastAclChangeSetID";
    
    private static final String SITES_FILTER="SITES";
    private static final String PROPERTIES_FILTER="PROPERTIES";

    protected static final Log logger = LogFactory.getLog(IndexingDaoImpl.class);

    private SqlSessionTemplate template;
    private NodeService nodeService;
    
    private Set<String> allowedTypes;
    private Set<String> excludedNameExtension;
    private Set<String> properties;
    private Set<String> aspects;
    private Set<String> mimeTypes;
    private Set<String> sites;

    @SuppressWarnings("unchecked")
	public List<NodeEntity> getNodesByAclChangesetId(Pair<Long, StoreRef> store, Long lastAclChangesetId, int maxResults)
    {
        StoreRef storeRef = store.getSecond();
        if (maxResults <= 0 || maxResults == Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("Maximum results must be a reasonable number.");
        }

        logger.debug("[getNodesByAclChangesetId] On Store " + storeRef.getProtocol() + "://" + storeRef.getIdentifier());

        NodeBatchLoadEntity nodeLoadEntity = new NodeBatchLoadEntity();
        nodeLoadEntity.setStoreId(store.getFirst());
        nodeLoadEntity.setStoreProtocol(storeRef.getProtocol());
        nodeLoadEntity.setStoreIdentifier(storeRef.getIdentifier());
        nodeLoadEntity.setMinId(lastAclChangesetId);
        nodeLoadEntity.setMaxId(lastAclChangesetId + maxResults);
        nodeLoadEntity.setAllowedTypes(this.allowedTypes);
        nodeLoadEntity.setExcludedNameExtension(this.excludedNameExtension);
//        nodeLoadEntity.setProperties(this.properties);
        nodeLoadEntity.setAspects(this.aspects);
        nodeLoadEntity.setMimeTypes(this.mimeTypes);

        return filterNodes((List<NodeEntity>) template.selectList(SELECT_NODES_BY_ACLS, nodeLoadEntity, new RowBounds(0,
                Integer.MAX_VALUE)));
    }

    @SuppressWarnings("unchecked")
	public List<NodeEntity> getNodesByTransactionId(Pair<Long, StoreRef> store, Long lastTransactionId, int maxResults)
    {
        StoreRef storeRef = store.getSecond();
        if (maxResults <= 0 || maxResults == Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("Maximum results must be a reasonable number.");
        }

        logger.debug("[getNodesByTransactionId] On Store " + storeRef.getProtocol() + "://" + storeRef.getIdentifier());

        NodeBatchLoadEntity nodeLoadEntity = new NodeBatchLoadEntity();
        nodeLoadEntity.setStoreId(store.getFirst());
        nodeLoadEntity.setStoreProtocol(storeRef.getProtocol());
        nodeLoadEntity.setStoreIdentifier(storeRef.getIdentifier());
        nodeLoadEntity.setMinId(lastTransactionId);
        nodeLoadEntity.setMaxId(lastTransactionId + maxResults);
        nodeLoadEntity.setAllowedTypes(this.allowedTypes);
        nodeLoadEntity.setExcludedNameExtension(this.excludedNameExtension);
//        nodeLoadEntity.setProperties(this.properties);
        nodeLoadEntity.setAspects(this.aspects);
        nodeLoadEntity.setMimeTypes(this.mimeTypes);

        return filterNodes((List<NodeEntity>) template.selectList(SELECT_NODES_BY_TXNS, nodeLoadEntity, new RowBounds(0,
                Integer.MAX_VALUE)));
    }

    public NodeEntity getNodeByUuid(Pair<Long, StoreRef> store, String uuid)
    {
        StoreRef storeRef = store.getSecond();

        logger.debug("[getNodeByUuid] On Store " + storeRef.getProtocol() + "://" + storeRef.getIdentifier());

        NodeBatchLoadEntity nodeLoadEntity = new NodeBatchLoadEntity();
        nodeLoadEntity.setStoreId(store.getFirst());
        nodeLoadEntity.setStoreProtocol(storeRef.getProtocol());
        nodeLoadEntity.setStoreIdentifier(storeRef.getIdentifier());
        nodeLoadEntity.setUuid(uuid);

        return (NodeEntity) template.selectOne(SELECT_NODES_BY_UUID, nodeLoadEntity);
    }
    
    /**
     * Get the last acl change set id from database
     * 
     * @return
     */
    public Long getLastAclChangeSetID(){
        
        if(logger.isDebugEnabled()){
            logger.debug("[getLastAclChangeSetID]");
        }
        
        return (Long) template.selectOne(SELECT_LAST_ACL_CHANGE_SET_ID);
    }
    
    /**
     * Get the last transaction id from database
     * 
     * @return
     */
    public Long getLastTransactionID(){
        
        if(logger.isDebugEnabled()){
            logger.debug("[getLastTransactionID]");
        }
        
        return (Long) template.selectOne(SELECT_LAST_TRANSACTION_ID);
    }
    
    /**
     * Filter the nodes based on some parameters
     * @param nodes
     * @return
     */
    private List<NodeEntity> filterNodes(List<NodeEntity> nodes)
    {
        List<NodeEntity> filteredNodes= null;
        
        //Filter by sites
        Map<String,Boolean> filters=getFilters();
        
        if(filters.values().contains(Boolean.TRUE)){
            
            filteredNodes= new ArrayList<NodeEntity>();
            
            for(NodeEntity node:nodes){
                
               boolean shouldBeAdded=true;
               NodeRef nodeRef= new NodeRef(node.getStore().getStoreRef(),node.getUuid());
                    
               if(nodeService.exists(nodeRef)){
                    
                    //Filter by site
                    if(filters.get(SITES_FILTER)){
                        Path pathObj = nodeService.getPath(nodeRef);
                        String siteName = Utils.getSiteName(pathObj);
                        shouldBeAdded= siteName!=null && this.sites.contains(siteName);
                    }
                    
                    //Filter by properties
                    if(filters.get(PROPERTIES_FILTER) && shouldBeAdded){
                        for(String prop:this.properties){
                            
                            int pos=prop.lastIndexOf(":");
                            String qName=null;
                            String value=null;
                            
                            if(pos!=-1 && (prop.length()-1)>pos){
                                qName=prop.substring(0, pos);
                                value= prop.substring(pos+1,prop.length());
                            }
                            
                            if(StringUtils.isEmpty(qName) || StringUtils.isEmpty(value)){
                                //Invalid property
                                continue;
                            }
                            
                            Serializable rawValue= nodeService.getProperty(nodeRef, QName.createQName(qName));
                            shouldBeAdded=shouldBeAdded && value.equals(rawValue);
                            
                        }
                    }
                    
                    if(shouldBeAdded){
                        filteredNodes.add(node);
                    }
                }
            }
        }else{
            filteredNodes=nodes;
        }
        return filteredNodes;
    }

    /**
     * Get existing filters
     * @return
     */
    private Map<String,Boolean> getFilters()
    {
        Map<String,Boolean> filters= new HashMap<String, Boolean>(2);
        //Site filter
        filters.put(SITES_FILTER, this.sites!=null && this.sites.size() > 0);
        //Properties filter
        filters.put(PROPERTIES_FILTER, this.properties!=null && this.properties.size() > 0);
        
        return filters;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate)
    {
        this.template = sqlSessionTemplate;
    }
    
    public void setServiceRegistry(ServiceRegistry serviceRegistry){
        this.nodeService= serviceRegistry.getNodeService();
    }

    public void setAllowedTypes(Set<String> allowedTypes)
    {
        this.allowedTypes = allowedTypes;
    }

    public Set<String> getAllowedTypes()
    {
        return this.allowedTypes;
    }

    public void setExcludedNameExtension(Set<String> excludedNameExtension)
    {
        this.excludedNameExtension = excludedNameExtension;
    }

    public Set<String> getExcludedNameExtension()
    {
        return this.excludedNameExtension;
    }

    public void setProperties(Set<String> properties)
    {
        this.properties = properties;
    }

    public Set<String> getProperties()
    {
        return this.properties;
    }

    public void setAspects(Set<String> aspects)
    {
        this.aspects = aspects;
    }

    public Set<String> getAspects()
    {

        return this.aspects;
    }

    public void setMimeTypes(Set<String> mimeTypes)
    {
        this.mimeTypes = mimeTypes;
    }

    public Set<String> getMimeTypes()
    {
        return this.mimeTypes;
    }
    
    public void setSites(Set<String> sites)
    {
        this.sites = sites;
    }

    public Set<String> getSites()
    {
        return this.sites;
    }
    
}
