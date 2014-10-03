alfresco-indexer
================

What is it?
---
Alfresco Indexer is an Alfresco AMP that provides an HTTP endpoint to track Alfresco transactions and ACL changes; it aims to deliver a different (custom) way to index content hosted in Alfresco.
Alfresco Indexer Client (sub-module) provides a Java-based client to reach the Alfresco endpoint.

Alfresco Indexer WebScripts mimics the same behaviour of the built-in Solr API Webscripts (and Alfresco Solr CoreTracker), with one fundamental difference: this implementation delivers one single endpoint that returns one single list of nodeRefs, joining nodes that have been altered by transactions OR by ACL changesets; for each nodeRef returned, node properties, aspects and ACLs are indexed into one single (index) document

- Pro: Simplified Search Index structure, it improves integration of Alfresco indexing with existing Search engines and index data structures
- Pro: The authorization checks are implemented by query parsers by adding security constraints to a given query; there is no post-processing or data-joining activity involved during a query execution
- Cons: If an ACL changes on a node, also all other nodes that inherit from it will be re-indexed, including node properties and content
- Cons: Alfresco query parsers cannot be used with this solution, therefore Alfresco Share won't work out of the box

Project Structure
---

- *Alfresco Indexer Webscripts* - An Alfresco Module Package (AMP) that exposes the set of Webscripts on Alfresco Repository (similar to Solr API Webscripts)
- *Alfresco Indexer Client* - A Java API that wraps HTTP invocations to Alfresco Indexer Webscripts (similar to Alfresco Solr CoreTracker, the Alfresco API deployed into Apache Solr that invokes Alfresco Solr API against the repo and commits documents into Solr)

Configuration
---
In `alfresco-global.properties` you can tweak the following default parameters.

### Url Prefixes
```
indexer.properties.url.template = http://localhost:8080/alfresco/service/node/details
indexer.content.url.prefix = http://localhost:8080/alfresco/service
indexer.share.url.prefix = http://localhost:8888/share
indexer.preview.url.prefix = http://localhost:8080/alfresco/service
indexer.thumbnail.url.prefix = http://localhost:8080/alfresco/service
```

### Node Changes paging parameters
```
indexer.changes.nodesperacl=10
indexer.changes.nodespertxn=10
```

### Node Changes allowed Node Types (whitelist)
```
indexer.changes.allowedTypes={http://www.alfresco.org/model/content/1.0}content,{http://www.alfresco.org/model/content/1.0}folder
```

Other examples of allowed types:

```
{http://www.alfresco.org/model/forum/1.0}topic
{http://www.alfresco.org/model/forum/1.0}post
{http://www.alfresco.org/model/content/1.0}person
{http://www.alfresco.org/model/content/1.0}link
{http://www.alfresco.org/model/calendar}calendar
{http://www.alfresco.org/model/calendar}calendarEvent
{http://www.alfresco.org/model/datalist/1.0}dataList
{http://www.alfresco.org/model/datalist/1.0}dataListItem (includes all sub-types, such as dl:task, dl:event and dl:issue)
{http://www.alfresco.org/model/blogintegration/1.0}blogDetails
{http://www.alfresco.org/model/blogintegration/1.0}blogPost
```

Binaries
---
Project binaries can be found at http://repository-maoo.forge.cloudbees.com/release/org/alfresco/consulting/indexer

You can add dependency with the following Maven build items:

Dependency
```
    <dependency>
        <groupId>org.alfresco.consulting.indexer</groupId>
        <artifactId>alfresco-indexer-client</artifactId>
        <version>0.6.1</version>
    </dependency>
```

Repository
```
    <repository>
        <id>alfresco-indexer-release</id>
        <url>dav:https://repository-maoo.forge.cloudbees.com/release/</url>
    </repository>

```

Disclaimer
---
* This project is NOT supported by Alfresco Support
* This project is experimental, only few customers are starting to use it (mostly using the Alfresco ManifoldCF Connector)

Credits
---
This project was have been developed by
* Alfresco Consultant [Maurizio Pillitu](http://session.it)
* [Findwise](http://www.findwise.com/) ([Martin Nycander](https://github.com/Nycander) and [Andreas Salomonsson](https://github.com/andreassalomonsson))
* [Zaizi](http://www.zaizi.com) ([Rafa Haro](https://github.com/rafaharo) and [Ivan Arroyo](https://github.com/iarroyo))

License
---

Please see the file [LICENSE.md](LICENSE.md) for the copyright licensing conditions attached to
this codebase
