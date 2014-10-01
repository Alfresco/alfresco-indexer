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
package org.alfresco.consulting.indexer.client;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AlfrescoFilters {

	private Collection<String> siteFilters;
	
	private Collection<String> typeFilters;
	
	private Collection<String> mimetypeFilters;
	
	private Collection<String> aspectFilters;
	
	private Map<String, String> metadataFilters;
	
	public AlfrescoFilters(){
		siteFilters = Sets.newHashSet();
		typeFilters = Sets.newHashSet();
		mimetypeFilters = Sets.newHashSet();
		aspectFilters = Sets.newHashSet();
		metadataFilters = Maps.newHashMap();
	}

	public Collection<String> getSiteFilters() {
		return siteFilters;
	}
	
	public Collection<String> getTypeFilters(){
	    return typeFilters;
	}

	public Collection<String> getMimetypeFilters() {
		return mimetypeFilters;
	}

	public Collection<String> getAspectFilters() {
		return aspectFilters;
	}

	public Map<String, String> getMetadataFilters() {
		return metadataFilters;
	}
	
	public void addSiteFilter(String site){
		siteFilters.add(site);
	}
	
	public void addTypeFilter(String type){
	    typeFilters.add(type);
	}
	
	public void addMimetypeFilter(String mimetype){
		mimetypeFilters.add(mimetype);
	}
	
	public void addAspectFilter(String aspect){
		aspectFilters.add(aspect);
	}
	
	public void addMetadataFilter(String metadata, String value){
		metadataFilters.put(metadata, value);
	}
	
	public boolean isEmpty(){
		return siteFilters.isEmpty() &&
				typeFilters.isEmpty() &&
				mimetypeFilters.isEmpty() &&
				aspectFilters.isEmpty() &&
				metadataFilters.isEmpty();
				
	}
	
	public  String toJSONString(){
	    
	    Gson gson= new GsonBuilder().create();
	    return gson.toJson(this);
	    
	}
}
