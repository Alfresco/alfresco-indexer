alfresco-indexer
================

What is it?
---
Alfresco Indexer is an API that allows to index content stored in Alfresco, when you want, how you want, selecting the content you're interested to.

Compatibility Matrix
---

| Alfresco Indexer version | (shipped with) ManifoldCF version | (tested wth) Alfresco edition/version |
| ------------- |:-------------:| -----:|
| 0.7.x | 1.8.0 to 2.2.0-RC0 | Community 5.0.[a,b,c,d], Enterprise 4.2.x |
| 0.8.x | trunk (master) - WIP | Community 5.0.d, Enterprise 5.0.x |

Community 5.1.[a,b,c]-EA is work in progress (add issue link)
There may be other permutations that work but haven't been tested.

Run Tests
---
```
git clone git@github.com:maoo/alfresco-indexer.git
mvn clean install -DskipTests
cd alfresco-indexer-webscripts-war
mvn clean integration-test
```

To know how to build the master and test it against ManifoldCF, follow [these instructions](MANIFOLD.md)

Project Structure
---
- *Alfresco Indexer Webscripts* - A server-side component (an [AMP](http://docs.alfresco.com/4.2/tasks/amp-install.html) that needs to be installed in Alfresco) that exposes a set of Webscripts on Alfresco Repository
- *Alfresco Indexer Client* - A Java API that wraps HTTP invocations to Alfresco Indexer Webscripts  and publishes a [simple client interface](https://github.com/maoo/alfresco-indexer/blob/master/alfresco-indexer-client/src/main/java/com/github/maoo/indexer/client/AlfrescoClient.java) to interact with Alfresco contents; hereby the most important methods you get access to:

```
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
```

Differences with Alfresco-Solr integration
---

The software architecture of Alfresco Indexer is the same delivered by Alfresco-Solr integration:
- A collection of webscripts (accessible via `/alfresco/api/solr/*` endpoints) that allow to track transactions and acl change events on Alfresco side
- A Java client that interacts with webscripts and updates Apache Solr indexes

Nevertheless, the following differences can be noted:
- Alfresco Indexer Webscripts are delivered by an AMP, they're not part of the core Alfresco code, as opposed to Alfresco Solr Integration
- Alfresco Indexer is an unsupported, community, experimental effort; Alfresco Solr integration is stable and supported by Alfresco
- Alfresco Indexer is agnostic to the Search Engine to adopt, as opposed to [Alfresco-Solr integration](https://svn.alfresco.com/repos/alfresco-open-mirror/alfresco/HEAD/root/projects/solr-client/source/java/org/alfresco/solr/client/SOLRAPIClient.java); [Alfresco ManifoldCF Connector](http://svn.apache.org/repos/asf/manifoldcf/trunk/connectors/alfresco-webscript/) is a great example on how to use Alfresco with other Search Engines (i.e. Elasticsearch)
- Alfresco-Solr integration maintains 2 isolated index structures for transactions and changesets; Alfresco-Indexer maintains 1 index structure with one index entry per Alfresco node, containing a list of readable authorities (`readablaAuthorities`); as a result:
  1. Alfresco-Solr integration is slower at query time, since document index entries must be cross-referenced with ACL index entries to understand which documents are accessible from the current user
  2. Alfresco Indexer triggers a reindexing of all nodes whose ACL change; a change to `/app:Company_Home` would trigger a full re-indexing; on the other hand, it doesn't need complex  query logic to implement authorisation query parsers for the Search Engine of your choice
- Alfresco Indexer *does not* provide any Search Engine query parser, as opposed to Alfresco Solr integration, that delivers CMISQL, FTS and Lucene Query query predicates to implement advanced query capabilities; this makes Alfresco Indexer not suitable for any integration with Alfresco clients that rely on these search capabilities, such as Alfresco Share

To summarise, advantages of using Alfresco Indexer:
- Simplified Search Index structure, it improves integration of Alfresco indexing with existing Search engines and index data structures
- The authorization checks are implemented by query parsers by adding security constraints to a given query; there is no post-processing or data-joining activity involved during a query execution

Disadvantages of using Alfresco Indexer:
- If an ACL changes on a node, also all other nodes that inherit from it will be re-indexed, including node properties and content
- Alfresco query parsers cannot be used with this solution, therefore Alfresco Share won't work out of the box

Configuration
---
Alfresco Indexer Webscripts can be configured to tweak the indexing process; in `alfresco-global.properties` you can override the following default parameters.

### Url Prefixes
```
indexer.properties.url.prefix = http://localhost:8080/alfresco/service/node/details
indexer.document.url.prefix = http://localhost:8080/alfresco/service/slingshot/node
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
Alfresco Indexer binaries can be found in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Calfresco-indexer); you can use Alfresco Indexer using Apache Maven, simply adding the following dependency in your pom.xml file:

```
  <dependency>
      <groupId>com.github.maoo.indexer</groupId>
      <artifactId>alfresco-indexer-client</artifactId>
      <version>0.8.0</version>
  </dependency>
```

Release
---
Before releasing, make sure you can upload artifacts to Maven Central:
```
mvn deploy -Pgpg
```
If everything goes fine, make sure you're up-to-date with git master and run the release command:
```
git status
netstat -anl | grep 8080 #make sure local port 8080 is free
mvn clean -Ppurge
mvn release:prepare release:perform
```
Follow [sonatype docs](http://central.sonatype.org/pages/apache-maven.html) for setting up your environment.

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
