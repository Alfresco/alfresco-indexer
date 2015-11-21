Testing against Manifold master
---

There are few steps to perform in order to test a local build of alfresco-indexer against Manifold master version.

### Checkout/Build Alfresco indexer
```
git clone git@github.com:maoo/alfresco-indexer.git
cd alfresco-indexer
mvn clean install -DskipTests
```
Tests are currently failing due to a new SDK version being used; will be fixed ASAP

### Run a local Alfresco instance
This local installation will include alfresco-indexer-webscripts AMP
```
cd alfresco-indexer/alfresco-indexer-webscripts
mvn integration-test -Pamp-to-war,purge,enterprise
```

### Download/run a local Apache Solr
```
curl http://archive.apache.org/dist/lucene/solr/4.9.1/solr-4.9.1.zip > solr-4.10.4.zip
unzip solr-4.10.4.zip
cd solr-4.10.4
./bin/solr start
```

### Checkout/Build Apache manifoldCF
Taken from [ManifoldCF docs](https://manifoldcf.apache.org/release/trunk/en_US/how-to-build-and-deploy.html)

```
# git clone git@github.com:apache/manifoldcf.git
curl -L https://github.com/apache/manifoldcf/archive/release-2.2-RC0.zip > manifold.zip
unzip manifold.zip
cd manifoldcf-release-2.2-RC0
ant make-deps
ant make-core-deps
ant build
mvn install -DskipTests -Dmaven.test.skip=true

rm -rf dist/connector-lib/alfresco-indexer*
cp ~/alfresco-indexer/alfresco-indexer-client/target/alfresco-indexer-client.jar dist/connector-lib


cd connectors/alfresco-webscripts
mvn package
```

### Test Alfresco Webscripts Manifold Connector
```
cd manifoldcf/connectors/alfresco-webscripts
# [change pom.xml with <alfresco.indexer.version>0.7.2-SNAPSHOT</alfresco.indexer.version>]
mvn clean integration-test
```

### Run Apache manifoldCF
```
cd manifoldcf/dist/example
./start.sh
```

### Access Apache manifoldCF
Login with admin/admin on http://localhost:8345/mcf-crawler-ui/login.jsp

### Configure Manifold

Create the following items on Manifold UI:
1. Output Connection (via `List Output Connections`); type is `Solr`
2. Authority Group (via `List Authority Groups`)
3. Authority Connection (via `List Authority Connections`); type is `Alfresco Webscript`; set Authority group with value from #2
4. Repository Connection (via `List Repository Connections`); type is `Alfresco`; set username/password to `admin/admin`
5. Job (via `List all Jobs`); set #3 and #4
6. Start Job (via `Status and Job Management`)
