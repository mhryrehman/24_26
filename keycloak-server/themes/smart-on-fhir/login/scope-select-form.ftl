<#import "template.ftl" as layout>
<#import "components/atoms/button.ftl" as button>
<#import "components/atoms/button-group.ftl" as buttonGroup>

<img class="logo" src="https://www.curemd.com/cmd/cmd-assets/images/curemd-vector.svg" alt="CureMD Logo" />

<@layout.registrationLayout displayInfo=true; section>
    <#if section == "title">
        Scope Selection
    <#elseif section == "header">
        Select Permissions
    <#elseif section == "form">
        <div class="form-container scope-main-wrap">
            <p class="form-description scope-descp">
                Please select the scopes you want to grant access to:
            </p>

            <form id="scope-selection" action="${url.loginAction}" method="post">
                <div class="scope-list">
                    <#-- scopesMap: Map where key = scope name, value = consent/friendly text -->
                    <#list scopesMap?keys as scopeKey>
                        <#assign scopeText = scopesMap[scopeKey]!scopeKey>
                        <div class="scope-item">
                            <label class="scope-checkbox">
                                <span class="scope-label">${scopeText}</span>
                                <input type="checkbox"
                                       checked="checked"
                                       name="selectedScopes"
                                       id="${scopeKey}"
                                       value="${scopeKey}">
                                <span class="scope-checkmark"></span>
                            </label>
                        </div>
                    </#list>
                </div>

                <@buttonGroup.kw>
                    <@button.kw color="primary" name="login" type="submit">
                        Submit
                    </@button.kw>
                </@buttonGroup.kw>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>
