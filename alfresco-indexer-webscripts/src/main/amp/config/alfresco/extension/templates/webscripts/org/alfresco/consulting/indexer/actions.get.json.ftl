{
  "docs" : [
    <#list nodes as node>
      {
        <#assign qname=QName.createQName(node.getTypeNamespace(),node.getTypeName()) >
        
        <#assign suffix="/"+storeProtocol+"/"+storeId+"/"+node.uuid >
        <#assign nodeRef=storeProtocol+"://"+storeId+"/"+node.uuid >
        <#if node.name??>
        	"name" : "${node.name}",
        	"propertiesUrl" : "${propertiesUrlTemplate + suffix}",
        </#if>
        "uuid" : "${node.uuid}",
        "nodeRef": "${nodeRef}",
        "type" : "${qname.toPrefixString(nsResolver)}",
        "deleted" : ${node.getDeleted(qnameDao)?string}
      }
      <#if node_has_next>,</#if>
    </#list>
  ],
  "store_id" : "${storeId}",
  "store_protocol" : "${storeProtocol}"
}