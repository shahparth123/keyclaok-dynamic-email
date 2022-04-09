<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeader??>
            ${messageHeader}
        <#else>
            <#if pageRedirectUri?has_content>
                <div class="mavq-text-center">
                    <img src="${url.resourcesPath}/assets/reset-password.svg"/>
                    <div class="mavq-title mavq-p-b-24">You have changed your password successfully!</div>
                    <#--                    <div class="mavq-field">-->
                    <#--                        <a href="${pageRedirectUri}" class="mavq-primary-action-btn mavq-decoration-none">Click Here to Login</a>-->
                    <#--                    </div>-->
                </div>
            <#elseif actionUri?has_content>
                <div class="mavq-text-center">
                    <div class="mavq-title">Update Your Password</div>
                    <div class="mavq-p-b-15 mavq-sub-title">
                        Click below to update your password
                    </div>
                    <img src="${url.resourcesPath}/assets/confirmation.svg"/>
                    <div class="mavq-field mavq-m-t-24">
                        <a href="${actionUri}" class="mavq-primary-action-btn mavq-decoration-none">Click Here to
                            Proceed</a>
                    </div>
                </div>
            <#else>
                ${message.summary}
            </#if>
        </#if>
    <#elseif section = "form">
        <div id="kc-info-message">
            <#if pageRedirectUri?has_content>
            <#elseif actionUri?has_content>
            <#else>
                <p class="instruction">${message.summary}<#if requiredActions??><#list requiredActions>:
                        <b><#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#items></b></#list><#else></#if>
                </p>
            </#if>
            <#if skipLink??>
            <#else>
            <#--                     <#if pageRedirectUri?has_content>-->
            <#--                         <p><a href="${pageRedirectUri}">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>-->
                <#if actionUri?has_content>
                <#--                     <p><a href="${actionUri}">${kcSanitize(msg("proceedWithAction"))?no_esc}</a></p>-->
                <#elseif (client.baseUrl)?has_content>
                    <p><a href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
                </#if>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>