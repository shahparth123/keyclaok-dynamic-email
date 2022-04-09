<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>
    <#if section = "header">
    <#--        ${msg("updatePasswordTitle")} MAVQ CHANGE-->
        <div class="mavq-title">Create New Password</div>
        <div class="mavq-p-b-15 mavq-sub-title">
            Please enter a new password for your account
        </div>
    <#elseif section = "form">
        <form id="kc-passwd-update-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <#--     MAVQ CHANGE       <input type="text" id="username" name="username" value="${username}" autocomplete="username"-->
            <#--                   readonly="readonly" style="display:none;"/>-->
            <#--            <input type="password" id="password" name="password" autocomplete="current-password" style="display:none;"/>-->

            <#--            <div class="${properties.kcFormGroupClass!}">-->
            <div class="mavq-field mavq-p-b-24">
                <#--                MAVQ CHANGE <div class="${properties.kcLabelWrapperClass!}">-->
                <#--                    <label for="password-new" class="${properties.kcLabelClass!}">${msg("passwordNew")}</label>-->
                <#--                </div>-->
                <label for="email">New Password
                    <span class="mavq-required">*</span>
                </label>
                <#--                <div class="${properties.kcInputWrapperClass!}">-->
                <div class="mavq-input-box">
                    <img src="${url.resourcesPath}/assets/eye.svg" class="mavq-password-eye mavq-new-password"
                         onclick="togglePassword(this)"/>
                    <input type="password" id="password-new" name="password-new" class="${properties.kcInputClass!}"
                           autofocus autocomplete="new-password"
                           aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
                    />

                    <#if messagesPerField.existsError('password')>
                        <span id="input-error-password" class="${properties.kcInputErrorMessageClass!}"
                              aria-live="polite">
                            ${kcSanitize(messagesPerField.get('password'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>

            <#--            <div class="${properties.kcFormGroupClass!}">-->
            <div class="mavq-field mavq-p-b-34">
                <#--                <div class="${properties.kcLabelWrapperClass!}">-->
                <#--                    <label for="password-confirm" class="${properties.kcLabelClass!}">${msg("passwordConfirm")}</label>-->
                <#--                </div>-->
                <label for="password">Confirm New Password
                    <span class="mavq-required">*</span>
                </label>
                <#--                <div class="${properties.kcInputWrapperClass!}">-->
                <div class="mavq-input-box">
                    <img src="${url.resourcesPath}/assets/eye.svg" class="mavq-password-eye mavq-confirm-password"
                         onclick="togglePassword(this)"/>
                    <input type="password" id="password-confirm" name="password-confirm"
                           class="${properties.kcInputClass!}"
                           autocomplete="new-password"
                           aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
                    />

                    <#if messagesPerField.existsError('password-confirm')>
                        <span id="input-error-password-confirm" class="${properties.kcInputErrorMessageClass!}"
                              aria-live="polite">
                            ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                        </span>
                    </#if>

                </div>
            </div>

            <#--            <div class="${properties.kcFormGroupClass!}">-->
            <div class="mavq-field mavq-p-b-24">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <#if isAppInitiatedAction??>
                            <div class="checkbox">
                                <label><input type="checkbox" id="logout-sessions" name="logout-sessions" value="on"
                                              checked> ${msg("logoutOtherSessions")}</label>
                            </div>
                        </#if>
                    </div>
                </div>

                <#if isAppInitiatedAction??>
                    <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                               type="submit" value="${msg("doSubmit")}"/>
                        <button class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                                type="submit" name="cancel-aia" value="true"/>
                        ${msg("doCancel")}</button>
                    </div>
                <#else>
                    <div class="mavq-submit-btn">
                        <button class="mavq-primary-action-btn ${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                type="submit">Save
                        </button>
                    </div>
                </#if>
            </div>
            <#--            <#if !isAppInitiatedAction??>-->
            <#--            <span class="mavq-remember-password">Remember Password?-->
            <#--                <a id="remember_password_redirection" href="#">Login</a>-->
            <#--            </span>-->
            <#--            </#if>-->
        </form>
    </#if>
</@layout.registrationLayout>