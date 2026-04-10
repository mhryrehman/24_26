<#ftl output_format="HTML" encoding="UTF-8">
<#import "template.ftl" as layout>

<@layout.emailLayout>

  <#-- Custom subject for password reset -->
  <#global subject = "Reset your Leap password">

  <p>Hi ${user.firstName?has_content?then(user.firstName, user.username)},</p>
  <p>We got a request to reset your Leap password.</p>

  <p><strong><a href="${link}">Reset Password</a></strong></p>

  <p>For your security, this link will only work for the next ${linkExpirationFormatter(linkExpiration)}.</p>
  <p>If you didn’t request a password reset, don’t worry — you can ignore this email and your password will stay the same.</p>

  <p>Thanks for being part of Leap,<br/><strong>The Leap Team</strong></p>

</@layout.emailLayout>
