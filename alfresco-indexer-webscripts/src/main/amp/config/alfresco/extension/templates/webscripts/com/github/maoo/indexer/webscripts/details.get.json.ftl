<#escape x as jsonUtils.encodeJSONString(x)>
{
  "readableAuthorities" : [
    <#list readableAuthorities as readableAuthority>
      "${readableAuthority}"
      <#if readableAuthority_has_next>,</#if>
    </#list>
  ],
  "path" : "${path}",
  "documentUrl" : "${serviceContextPath + documentUrlPrefix + documentUrlPath}",

  <#if mimetype??>
    "mimetype" : "${mimetype}",
  </#if>
  <#if size??>
    "size" : "${size?c}",
  </#if>

  <#if contentUrlPath??>
    "contentUrlPath" : "${serviceContextPath + contentUrlPath}",
  </#if>
  <#if thumbnailUrlPath??>
    "thumbnailUrlPath" : "${thumbnailUrlPrefix + thumbnailUrlPath}",
  </#if>
  <#if previewUrlPath??>
    "previewUrlPath" : "${previewUrlPrefix + previewUrlPath}",
  </#if>
  <#if shareUrlPath??>
    "shareUrlPath" : "${shareUrl + shareUrlPath}",
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