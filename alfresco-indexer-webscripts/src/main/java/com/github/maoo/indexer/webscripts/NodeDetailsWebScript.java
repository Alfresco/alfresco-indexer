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
package com.github.maoo.indexer.webscripts;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.InputStream;

import com.github.maoo.indexer.utils.Utils;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.permissions.Acl;
import org.alfresco.repo.domain.permissions.AclDAO;
import org.alfresco.repo.security.permissions.AccessControlEntry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.thumbnail.script.ScriptThumbnailService;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.repo.thumbnail.ThumbnailRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.codec.binary.Base64;

import org.alfresco.service.cmr.repository.ContentService;

import com.google.gdata.util.common.base.StringUtil;

/**
 * Given a nodeRef, renders out all data about a node (except binary content): -
 * Node metadata - Node ACLs
 *
 * Please check
 * src/main/amp/config/alfresco/extension/templates/webscripts/com/github/maoo/
 * indexer/webscripts/details.get.desc.xml to know more about the RestFul
 * interface to invoke the WebScript
 *
 * List of pending activities (or TODOs) - Refactor recursive getAllAcls (direct
 * recursion) . Evaluate the possibility to write a SQL statement for that -
 * Move private/static logic into the IndexingService (see notes on
 * NodeChangesWebScript) - Move the following methods (and related SQL
 * statements) into IndexingDaoImpl -- nodeService.getProperties --
 * nodeService.getAspects -- nodeDao.getNodeAclId --
 * solrDao.getNodesByAclChangesetId -- nodeService.getType and
 * dictionaryService.isSubClass (should be merged into one) - Using JSON
 * libraries (or StringBuffer), render out the payload without passing through
 * FreeMarker template
 */
public class NodeDetailsWebScript extends DeclarativeWebScript implements InitializingBean, ResourceLoaderAware {

	protected static final Log logger = LogFactory.getLog(NodeDetailsWebScript.class);
	protected static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private static final String BASE_READ_PERMISSIONS = "ReadPermissions";

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

		final String serviceContextPath = req.getServerPath() + req.getServiceContextPath();

		final List<String> readableAuthorities = new ArrayList<String>();

		// Parsing parameters passed from the WebScript invocation
		Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
		String storeId = templateArgs.get("storeId");
		String storeProtocol = templateArgs.get("storeProtocol");
		String uuid = templateArgs.get("uuid");
		NodeRef nodeRef = new NodeRef(storeProtocol, storeId, uuid);
		logger.debug(String.format("Invoking ACLs Webscript, using the following params\n" + "nodeRef: %s\n", nodeRef));

		// Processing properties
		Map<QName, Serializable> propertyMap = nodeService.getProperties(nodeRef);
		Map<String, Pair<String, String>> properties = toStringMap(propertyMap);

		// Processing aspects
		Set<QName> aspectsSet = nodeService.getAspects(nodeRef);
		Set<String> aspects = toStringSet(aspectsSet);

		// Processing content data
		ContentData contentData = (ContentData) propertyMap.get(ContentModel.PROP_CONTENT);

		// Get the node ACL Id
		Long dbId = (Long) propertyMap.get(ContentModel.PROP_NODE_DBID);
		Long nodeAclId = nodeDao.getNodeAclId(dbId);

		// Get also the inherited ones
		List<Acl> acls = getAllAcls(nodeAclId);
		// @TODO - avoid reverse by implementing direct recursion
		Collections.reverse(acls);

		// Getting path and siteName
		Path pathObj = nodeService.getPath(nodeRef);
		String path = pathObj.toPrefixString(namespaceService);
		String siteName = Utils.getSiteName(pathObj);

		// Walk through ACLs and related ACEs, rendering out authority names
		// having a granted permission on the node
		for (Acl acl : acls) {
			List<AccessControlEntry> aces = aclDao.getAccessControlList(acl.getId()).getEntries();
			for (AccessControlEntry ace : aces) {
				if ((!ace.getPermission().getName().equals(BASE_READ_PERMISSIONS))
						&& ace.getAccessStatus().equals(AccessStatus.ALLOWED)) {
					if (!readableAuthorities.contains(ace.getAuthority())) {
						readableAuthorities.add(ace.getAuthority());
					}
				}
			}
		}

		Map<String, Object> model = new HashMap<String, Object>(1, 1.0f);
		if (contentData != null) {
			model.put("mimetype", contentData.getMimetype());
			model.put("size", contentData.getSize());
		}

		model.put("nsResolver", namespaceService);
		model.put("readableAuthorities", readableAuthorities);
		model.put("properties", properties);
		model.put("aspects", aspects);
		model.put("path", path);
		model.put("serviceContextPath", serviceContextPath);
		model.put("documentUrlPrefix", documentUrlPrefix);
		model.put("shareUrl", shareUrl);
		model.put("shareUrlPrefix", shareUrlPrefix);
		model.put("thumbnailUrlPrefix", thumbnailUrlPrefix);
		model.put("previewUrlPrefix", previewUrlPrefix);

		String documentUrlPath = String.format("/%s/%s/%s", storeProtocol, storeId, uuid);
		model.put("documentUrlPath", documentUrlPath);
		

		// Calculating the contentUrlPath and adding it only if the contentType
		// is child of cm:content
		boolean isContentAware = isContentAware(nodeRef);
		if (isContentAware) {
			String contentUrlPath = String.format("/api/node/%s/%s/%s/content", storeProtocol, storeId, uuid);
			model.put("contentUrlPath", contentUrlPath);

			// Rendering out the (relative) URL path to Alfresco Share
			String shareUrlPath = null;

			if (!StringUtil.isEmpty(siteName)) {
				shareUrlPath = String.format("/page/site/%s/document-details?nodeRef=%s", siteName, nodeRef.toString());

			} else {
				shareUrlPath = String.format("/page/document-details?nodeRef=%s", nodeRef.toString());
			}

			if (shareUrlPath != null) {
				model.put("shareUrlPath", shareUrlPath);
			}
		}

		String previewUrlPath = String.format("/api/node/%s/%s/%s/content/thumbnails/webpreview", storeProtocol,
				storeId, uuid);
		model.put("previewUrlPath", previewUrlPath);

		String thumbnailUrlPath = String.format(
				"/api/node/%s/%s/%s/content/thumbnails/doclib?c=queue&ph=true&lastModified=1", storeProtocol, storeId,
				uuid);
		model.put("thumbnailUrlPath", thumbnailUrlPath);

		ThumbnailRegistry registry = thumbnailService.getThumbnailRegistry();
		ThumbnailDefinition details = registry.getThumbnailDefinition("doclib");
		try {
			NodeRef thumbRef = thumbnailService.getThumbnailByName(nodeRef, ContentModel.PROP_CONTENT, "doclib");
			if (thumbRef == null) {
				thumbRef = thumbnailService.createThumbnail(nodeRef, ContentModel.PROP_CONTENT, details.getMimetype(),
						details.getTransformationOptions(), details.getName());
			}

			ContentReader thumbReader = contentService.getReader(thumbRef, ContentModel.PROP_CONTENT);
			InputStream thumbsIs = thumbReader.getContentInputStream();
			String thumbBase64 = Base64.encodeBase64String(IOUtils.toByteArray(thumbsIs));
			model.put("thumbnailBase64", thumbBase64);
			IOUtils.closeQuietly(thumbsIs);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("It was not possible to get/build thumbnail doclib for nodeRef: " + nodeRef + ". Message: "
						+ e.getMessage());
			}

			// thumbnail placeholder
			String phPath = scriptThumbnailService.getMimeAwarePlaceHolderResourcePath("doclib", details.getMimetype());

			StringBuilder sb = new StringBuilder("classpath:").append(phPath);
			final String classpathResource = sb.toString();

			InputStream is = null;
			try {
				is = resourceLoader.getResource(classpathResource).getInputStream();
				String thumbBase64 = Base64.encodeBase64String(IOUtils.toByteArray(is));
				model.put("thumbnailBase64", thumbBase64);
			} catch (Exception e2) {
				if (logger.isWarnEnabled()) {
					logger.warn("It was not possible to get/build placeholder thumbnail doclib for nodeRef: " + nodeRef
							+ ". Message: " + e2.getMessage());
				}
			} finally {
				IOUtils.closeQuietly(is);
			}

		}

		model.put("contentUrlPrefix", contentUrlPrefix);

		return model;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		boolean alfrescoWithPort = true;
		if ((sysAdminParams.getAlfrescoProtocol() == "https" && sysAdminParams.getAlfrescoPort() == 443)
				|| (sysAdminParams.getAlfrescoProtocol() == "http" && sysAdminParams.getAlfrescoPort() == 80)) {
			alfrescoWithPort = false;
		}
		String alfrescoPrefix = sysAdminParams.getAlfrescoProtocol() + "://" + sysAdminParams.getAlfrescoHost()
				+ (alfrescoWithPort ? ":" + sysAdminParams.getAlfrescoPort() : "") + "/"
				+ sysAdminParams.getAlfrescoContext();
		contentUrlPrefix = alfrescoPrefix + "/service";
		previewUrlPrefix = alfrescoPrefix + "/service";
		thumbnailUrlPrefix = alfrescoPrefix + "/service";

		boolean shareWithPort = true;
		if ((sysAdminParams.getShareProtocol() == "https" && sysAdminParams.getSharePort() == 443)
				|| (sysAdminParams.getShareProtocol() == "http" && sysAdminParams.getSharePort() == 80)) {
			shareWithPort = false;
		}
		shareUrlPrefix = sysAdminParams.getShareProtocol() + "://" + sysAdminParams.getShareHost()
				+ (shareWithPort ? ":" + sysAdminParams.getSharePort() : "") + "/" + sysAdminParams.getShareContext();

	}

	private boolean isContentAware(NodeRef nodeRef) {
		QName contentType = nodeService.getType(nodeRef);
		return dictionaryService.isSubClass(contentType, ContentModel.TYPE_CONTENT);
	}

	private Set<String> toStringSet(Set<QName> aspectsSet) {
		Set<String> ret = new HashSet<String>();
		for (QName aspect : aspectsSet) {
			ret.add(aspect.toPrefixString(namespaceService));
		}
		return ret;
	}

	private Map<String, Pair<String, String>> toStringMap(Map<QName, Serializable> propertyMap) {
		Map<String, Pair<String, String>> ret = new HashMap<String, Pair<String, String>>(1, 1.0f);
		for (QName propertyName : propertyMap.keySet()) {
			Serializable propertyValue = propertyMap.get(propertyName);
			if (propertyValue != null) {
				String propertyType = propertyValue.getClass().getName();
				String stringValue = propertyValue.toString();
				if (propertyType.equals("java.util.Date")) {
					stringValue = sdf.format(propertyValue);
				}
				ret.put(propertyName.toPrefixString(namespaceService),
						new Pair<String, String>(propertyType, stringValue));
			}
		}
		return ret;
	}

	private List<Acl> getAllAcls(Long nodeAclId) {
		logger.debug("getAllAcls from " + nodeAclId);
		Acl acl = aclDao.getAcl(nodeAclId);
		Long parentNodeAclId = acl.getInheritsFrom();
		logger.debug("parent acl is  " + parentNodeAclId);
		if (parentNodeAclId == null || !acl.getInherits()) {
			List<Acl> ret = new ArrayList<Acl>();
			ret.add(acl);
			return ret;
		} else {
			List<Acl> inheritedAcls = getAllAcls(parentNodeAclId);
			logger.debug("Current acl with id " + nodeAclId + " is " + acl);
			inheritedAcls.add(acl);
			return inheritedAcls;
		}
	}

	public List<String> getThumbnailDefinitions(NodeRef nodeRef) {
		List<String> result = new ArrayList<String>(7);

		ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
		if (contentReader != null) {
			String mimetype = contentReader.getMimetype();
			List<ThumbnailDefinition> thumbnailDefinitions = thumbnailService.getThumbnailRegistry()
					.getThumbnailDefinitions(mimetype, contentReader.getSize());
			for (ThumbnailDefinition thumbnailDefinition : thumbnailDefinitions) {
				result.add(thumbnailDefinition.getName());
			}
		}

		return result;
	}

	private DictionaryService dictionaryService;
	private NamespaceService namespaceService;
	private NodeService nodeService;
	private NodeDAO nodeDao;
	private AclDAO aclDao;
	private String documentUrlPrefix;
	private String shareUrl;
	private String shareUrlPrefix;
	private String thumbnailUrlPrefix;
	private String previewUrlPrefix;
	private SysAdminParams sysAdminParams;
	private ResourceLoader resourceLoader;
	private ScriptThumbnailService scriptThumbnailService;
	private ThumbnailService thumbnailService;
	private ContentService contentService;
	private String contentUrlPrefix;

	public void setDictionaryService(DictionaryService dictionaryService) {
		this.dictionaryService = dictionaryService;
	}

	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setNodeDao(NodeDAO nodeDao) {
		this.nodeDao = nodeDao;
	}

	public void setAclDao(AclDAO aclDao) {
		this.aclDao = aclDao;
	}

	public void setDocumentUrlPrefix(String documentUrlPrefix) {
		this.documentUrlPrefix = documentUrlPrefix;
	}

	public void setShareUrl(String shareUrl) {
		this.shareUrl = shareUrl;
	}

	public void setThumbnailUrlPrefix(String thumbnailUrlPrefix) {
		this.thumbnailUrlPrefix = thumbnailUrlPrefix;
	}

	public void setPreviewUrlPrefix(String previewUrlPrefix) {
		this.previewUrlPrefix = previewUrlPrefix;
	}

	public void setShareUrlPrefix(String shareUrlPrefix) {
		this.shareUrlPrefix = shareUrlPrefix;
	}

	public void setSysAdminParams(SysAdminParams sysAdminParams) {
		this.sysAdminParams = sysAdminParams;
	}

	public void setThumbnailService(ThumbnailService thumbnailService) {
		this.thumbnailService = thumbnailService;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void setScriptThumbnailService(ScriptThumbnailService scriptThumbnailService) {
		this.scriptThumbnailService = scriptThumbnailService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setContentUrlPrefix(String contentUrlPrefix) {
		this.contentUrlPrefix = contentUrlPrefix;
	}

}