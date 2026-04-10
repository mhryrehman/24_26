<#ftl output_format="HTML" encoding="UTF-8">
<#import "template.ftl" as layout>

<@layout.emailLayout>

  <#-- Normalize actions list (Keycloak may expose requiredActions or actions) -->
  <#assign ra = (requiredActions!actions)![]>
  <#assign isOnlyPasswordReset = (ra?size == 1 && ra?seq_contains("UPDATE_PASSWORD"))>

  <#if isOnlyPasswordReset>

    <p>Hi ${user.firstName?has_content?then(user.firstName, user.username)},</p>
    <p>We got a request to reset your Leap password.</p>

    <p><strong><a href="${link}">Reset Password</a></strong></p>

    <p>For your security, this link will only work for the next ${linkExpirationFormatter(linkExpiration)}.</p>
    <p>If you didn’t request a password reset, don’t worry — you can ignore this email and your password will stay the same.</p>

    <p>Thanks for being part of Leap,<br/><strong>The Leap Team</strong></p>

  <#else>

    <#-- Your default template body (unchanged) -->
    <#outputformat "plainText">
      <#assign requiredActionsText>
        <#if requiredActions??>
          <#list requiredActions>
            <#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#sep>
            </#items>
          </#list>
        </#if>
      </#assign>
    </#outputformat>

    ${kcSanitize(msg("executeActionsBodyHtml", link, linkExpiration, realmName, requiredActionsText, linkExpirationFormatter(linkExpiration)))?no_esc}

  </#if>

</@layout.emailLayout>
