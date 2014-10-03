<#escape x as jsonUtils.encodeJSONString(x)>
{
  "readableAuthorities" : [
    <#list readableAuthorities as readableAuthority>
      "${readableAuthority}"
      <#if readableAuthority_has_next>,</#if>
    </#list>
  ],
  "path" : "${path}",
  <#if mimetype??>
  	"mimetype" : "${mimetype}",
  </#if>
  <#if size??>
  	"size" : "${size}",
  </#if>
  <#if shareUrlPath??>
    "shareUrlPath" : "${shareUrlPrefix + shareUrlPath}",
  </#if>
  <#if contentUrlPath??>
    "contentUrlPath" : "${contentUrlPrefix + contentUrlPath}",
  </#if>
  <#if thumbnailUrlPath??>
    "thumbnailUrlPath" : "${thumbnailUrlPrefix + thumbnailUrlPath}",
  </#if>
  <#if previewUrlPath??>
    "previewUrlPath" : "${previewUrlPrefix + previewUrlPath}",
  </#if>

  <#assign propNames = properties?keys>
  "aspects" : [
    <#list aspects as aspect>
    "${aspect}"
    <#if aspect_has_next>,</#if>
  </#list>
  ],
  "properties" : [
    <#list propNames as propName>
      {
        <#assign propPair=properties[propName] >
        "name" : "${propName}",
        "type" : "${propPair.first}",
        "value" : "${propPair.second}"
      }
      <#if propName_has_next>,</#if>
    </#list>
  ]
}
</#escape>