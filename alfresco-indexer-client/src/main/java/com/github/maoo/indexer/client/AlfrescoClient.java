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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface AlfrescoClient {
  /**
   * Fetches nodes from Alfresco which has changed since the provided timestamp.
   *
   * @param lastAclChangesetId
   *         the id of the last ACL changeset already being indexed; it can be considered a "startFrom" param
   * @param lastTransactionId
   *         the id of the last transaction already being indexed; it can be considered a "startFrom" param
   * @return an {@link AlfrescoResponse}
   */
  AlfrescoResponse fetchNodes(long lastTransactionId, long lastAclChangesetId, AlfrescoFilters filters) throws
      AlfrescoDownException;
  
  /**
   * Fetches Node Info from Alfresco for a given node.
   * @param nodeUuid the UUID for the node
   * @return an {@link AlfrescoResponse}
   * @throws AlfrescoDownException
   */
  AlfrescoResponse fetchNode(String nodeUuid) throws AlfrescoDownException;
  
  /**
   * Fetches metadata from Alfresco for a given node.
   * @param nodeUuid
   *        the UUID for the node
   * @return a map with metadata created from a json object
   */
  Map<String, Object> fetchMetadata(String nodeUuid) throws AlfrescoDownException;

  /**
   * Fetches authorities for the provided username.
   * @param username
   * @return an {@link AlfrescoUser}
   */
  AlfrescoUser fetchUserAuthorities(String username) throws AlfrescoDownException;

  /**
   * Fetches authorities for all users.
   * @return a list of {@link AlfrescoUser}
   */
  List<AlfrescoUser> fetchAllUsersAuthorities() throws AlfrescoDownException;
  
  /**
   * Fetches Document Binary Content
   * 
   * @param contentUrlPath URL of the content
   * @return Document Binary Content
   */
  InputStream fetchContent(String contentUrlPath);
}
