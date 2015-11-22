{
  <#if nodes??>"totalNodes" : "${nodes?size}",</#if>
  <#if elapsedTime??>"elapsedTime" : "${elapsedTime}",</#if>
  "docs" : [
    <#list nodes as node>
      {
        <#assign qname=QName.createQName(node.getTypeNamespace(),node.getTypeName()) >
        <#assign suffix="/"+storeProtocol+"/"+storeId+"/"+node.uuid >
        <#assign type=qname.toPrefixString(nsResolver) >
        <#assign deleted=(node.getDeleted(qnameDao) || type == "sys:deleted") >
        "propertiesUrl" : "${serviceContextPath + propertiesUrlPrefix + suffix}",
        "uuid" : "${node.uuid}",
        "type" : "${type}",
        "deleted" : ${deleted?string}
      }
      <#if node_has_next>,</#if>
    </#list>
  ],
  <#if lastTxnId??>
    "last_txn_id" : "${lastTxnId?c}",
  </#if>
  <#if lastAclChangesetId??>
    "last_acl_changeset_id" : "${lastAclChangesetId?c}",
  </#if>
  "store_id" : "${storeId}",
  "store_protocol" : "${storeProtocol}"
}
