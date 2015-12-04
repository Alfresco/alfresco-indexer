{
  "docs" : [
    <#list nodes as node>
      {
        <#assign qname=QName.createQName(node.getTypeNamespace(),node.getTypeName()) >
        <#assign type=qname.toPrefixString(nsResolver) >
        <#assign deleted=(node.getDeleted(qnameDao) || type == "sys:deleted") >

        <#assign suffix="/"+storeProtocol+"/"+storeId+"/"+node.uuid >
        <#assign nodeRef=storeProtocol+"://"+storeId+"/"+node.uuid >
         <#if node.name??>
        	"name" : "${node.name}",
        </#if>
        "propertiesUrl" : "${propertiesUrlPrefix + suffix}",
        "uuid" : "${node.uuid}",
        "nodeRef": "${nodeRef}",
        "type" : "${type}",
        "deleted" : ${deleted?string}
      }
      <#if node_has_next>,</#if>
    </#list>
  ],
  "store_id" : "${storeId}",
  "store_protocol" : "${storeProtocol}"
}
