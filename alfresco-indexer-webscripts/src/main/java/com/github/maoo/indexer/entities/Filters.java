package com.github.maoo.indexer.entities;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Filters {
	
	private Set<String> types = new HashSet<String>(3);
	private Set<String> aspects = new HashSet<String>(3);
	private Set<String> mimeTypes = new HashSet<String>(3);
	private Set<String> sites = new HashSet<String>(3);
	private Map<String, String> metadata = new HashMap<String, String>(3);
	
	public Set<String> getTypes() {
		return this.types;
	}
	
	public void addType(String... values) {
		for (String value : values)
			this.types.add(value);
	}
	
	public void addTypes(Collection<String> values) {
		this.types.addAll(values);
	}
	
	public Set<String> getAspects() {
		return this.aspects;
	}
	
	public void addAspect(String... values) {
		for (String value : values)
			this.aspects.add(value);
	}
	
	public void addAspects(Collection<String> values) {
		this.aspects.addAll(values);
	}
	
	public Set<String> getMimeTypes() {
		return this.mimeTypes;
	}
	
	public void addMimeType(String... values) {
		for (String value : values)
			this.mimeTypes.add(value);
	}
	
	public void addMimeTypes(Collection<String> values) {
		this.mimeTypes.addAll(values);
	}
	
	public Set<String> getSites() {
		return this.sites;
	}
	
	public void addSite(String... values) {
		for (String value : values)
			this.sites.add(value);
	}
	
	public void addSites(Collection<String> values) {
		this.sites.addAll(values);
	}
	
	public Map<String, String> getMetadata() {
		return this.metadata;
	}
	
	public void addMetadata(String key, String value) {
		this.metadata.put(key, value);
	}
	
	public void addMetadata(Map<String, String> values) {
		this.metadata.putAll(values);
	} 

}
