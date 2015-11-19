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
curl http://apache.mirror.triple-it.nl/lucene/solr/5.3.1/solr-5.3.1.zip > solr-5.3.1.zip
unzip solr-5.3.1.zip
cd solr-5.3.1
./bin/solr start
```

### Checkout/Build Apache manifoldCF
Taken from [ManifoldCF docs](https://manifoldcf.apache.org/release/trunk/en_US/how-to-build-and-deploy.html)

```
git clone git@github.com:apache/manifoldcf.git
cd manifoldcf
ant make-deps
ant build
mvn install -DskipTests -Dmaven.test.skip=true

# Copy connectors
cp connectors/solr/target/mcf-solr-connector-2.3-SNAPSHOT.jar dist/connector-lib
cp connectors/alfresco-webscript/target/mcf-alfresco-webscript-connector-2.3-SNAPSHOT.jar dist/connector-lib
cp ~/alfresco-indexer/alfresco-indexer-client/target/alfresco-indexer-client.jar dist/connector-lib

cd connectors/alfresco-webscripts
mvn package
```

### Define properties.xml and connectors.xml

Edit `manifoldcf/dist/example/connectors.xml` and paste the following content

```
<connectors>
  <!-- Add your output connectors here -->
  <outputconnector name="Solr" class="org.apache.manifoldcf.agents.output.solr.SolrConnector"/>
    <!-- Add your authority connectors here -->
  <authorityconnector name="AlfrescoAuthority" class="org.apache.manifoldcf.authorities.authorities.alfrescowebscript.AlfrescoAuthorityConnector"/>
    <!-- Add your repository connectors here -->
  <repositoryconnector name="AlfrescoCrawl" class="org.apache.manifoldcf.crawler.connectors.alfrescowebscript.AlfrescoConnector"/>
</connectors>
```

Edit `manifoldcf/dist/example/properties.xml` and paste the following content

```
<configuration>
  <property name="org.apache.manifoldcf.crawleruiwarpath" value="../web/war/mcf-crawler-ui.war"/>
  <property name="org.apache.manifoldcf.authorityservicewarpath" value="../web/war/mcf-authority-service.war"/>
  <property name="org.apache.manifoldcf.apiservicewarpath" value="../web/war/mcf-api-service.war"/>
  <property name="org.apache.manifoldcf.usejettyparentclassloader" value="true"/>
  <property name="org.apache.manifoldcf.jettyconfigfile" value="./jetty.xml"/>
  <property name="org.apache.manifoldcf.combinedwarpath" value="../web/war/mcf-combined-service.war"/>
  <property name="org.apache.manifoldcf.databaseimplementationclass" value="org.apache.manifoldcf.core.database.DBInterfaceHSQLDB"/>
  <property name="org.apache.manifoldcf.hsqldbdatabasepath" value="."/>
  <property name="org.apache.manifoldcf.database.maxhandles" value="100"/>
  <property name="org.apache.manifoldcf.crawler.threads" value="50"/>
  <property name="org.apache.manifoldcf.crawler.historycleanupinterval" value="2592000000"/>
  <property name="org.apache.manifoldcf.logconfigfile" value="./logging.ini"/>
  <property name="org.apache.manifoldcf.connectorsconfigurationfile" value="connectors.xml"/>
  <libdir path="../connector-lib"/>
  <libdir path="../connector-common-lib"/>
  <libdir path="../connector-lib-proprietary"/>
</configuration>
```

### Run Apache manifoldCF
```
cd manifoldcf/dist/example
./start.sh
```

### Access Apache manifoldCF
Login with admin/admin on http://localhost:8345/mcf-crawler-ui/login.jsp
