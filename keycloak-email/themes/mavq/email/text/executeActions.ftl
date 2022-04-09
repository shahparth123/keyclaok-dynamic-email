<#ftl output_format="plainText">
<#assign requiredActionsText><#if requiredActions??><#list requiredActions><#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#items></#list><#else></#if></#assign>
${msg("mavqForgotPasswordResetEmailBodyCode",user.attributes.htmlBody,link, linkExpirationFormatter(linkExpiration), "'"
)}


