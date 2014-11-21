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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AlfrescoResponse {
  private final long lastTransactionId;
  private final long lastAclChangesetId;
  private final String storeId;
  private final String storeProtocol;
  private final Iterable<Map<String, Object>> documents;

  public AlfrescoResponse(long lastTransactionId, long lastAclChangesetId, String storeId,
                          String storeProtocol, Iterable<Map<String, Object>> documents) {
    this.lastTransactionId = lastTransactionId;
    this.lastAclChangesetId = lastAclChangesetId;
    this.storeId = storeId;
    this.storeProtocol = storeProtocol;
    this.documents = documents;
  }

  public AlfrescoResponse(long lastTransactionId, long lastAclChangesetId) {
    this(lastTransactionId, lastAclChangesetId, "", "", Collections.<Map<String, Object>>emptyList());
  }

  public long getLastTransactionId() {
    return lastTransactionId;
  }

  public long getLastAclChangesetId() {
    return lastAclChangesetId;
  }

  public String getStoreId() {
    return storeId;
  }

  public String getStoreProtocol() {
    return storeProtocol;
  }

  public Iterable<Map<String,Object>> getDocuments() {
    return documents;
  }

  public List<Map<String, Object>> getDocumentList() {
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    for (Map<String, Object> m : documents) {
      list.add(m);
    }
    return list;
  }
}