[<#list authoritiesPerUser?keys as username>
  {
    <#assign authorities=authoritiesPerUser[username]>
    "username" : "${username}",
    "authorities" : [
      <#list authorities as authority>
        "${authority}"
        <#if authority_has_next>,</#if>
      </#list>
    ]
  }
  <#if username_has_next>,</#if>
</#list>]