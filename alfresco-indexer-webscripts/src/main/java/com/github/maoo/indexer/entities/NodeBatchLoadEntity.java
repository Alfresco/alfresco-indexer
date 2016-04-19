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
package com.github.maoo.indexer.entities;

import java.util.Collection;
import java.util.Set;

public class NodeBatchLoadEntity extends org.alfresco.repo.domain.node.ibatis.NodeBatchLoadEntity {
  private Long minId;
  private Long maxId;
  private String uuid;
  private Collection<String> allowedTypes;
  private Collection<String> excludedNameExtension;
//  private Collection<String> properties;
//  private Collection<String> sites;
  private Collection<String> aspects;
  private Collection<String> mimeTypes;

  //These input values will be set on all returned NodeEntity objects returned by iBatis mappers
  private String storeProtocol;
  private String storeIdentifier;
  
  public Collection<String> getAllowedTypes() {
    return allowedTypes;
  }

  public Long getMinId() {
    return minId;
  }

  public void setMinId(Long minId) {
    this.minId = minId;
  }

  public Long getMaxId() {
    return maxId;
  }

  public void setMaxId(Long maxId) {
    this.maxId = maxId;
  }

  public void setAllowedTypes(Collection<String> allowedTypes) {
    this.allowedTypes = allowedTypes;
  }

  public String getStoreProtocol() {
    return storeProtocol;
  }

  public void setStoreProtocol(String storeProtocol) {
    this.storeProtocol = storeProtocol;
  }

  public String getStoreIdentifier() {
    return storeIdentifier;
  }

  public void setStoreIdentifier(String storeIdentifier) {
    this.storeIdentifier = storeIdentifier;
  }
  
  public void setUuid(String uuid){
      this.uuid=uuid;
  }
  
  public String getUuid(){
      return this.uuid;
  }
  
  public void setExcludedNameExtension(Set<String> excludedNameExtension){
      this.excludedNameExtension= excludedNameExtension;
  }
  
  public Collection<String> getExcludedNameExtension(){
      return this.excludedNameExtension;
  }
  
//  public void setProperties(Collection<String> properties){
//      this.properties=properties;
//  }
//  
//  public Collection<String> getProperties(){
//      return this.properties;
//  }
  
  public void setAspects(Collection<String> aspects){
      this.aspects=aspects;
  }
  
  public Collection<String> getAspects(){
      return this.aspects;
  }
  
  public void setMimeTypes(Collection<String> mimeTypes){
      this.mimeTypes=mimeTypes;
  }
  
  public Collection<String> getMimeTypes(){
      return this.mimeTypes;
  }
}
