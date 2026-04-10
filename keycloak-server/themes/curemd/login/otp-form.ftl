<#import "template.ftl" as layout>
<#import "components/atoms/button.ftl" as button>
<#import "components/atoms/button-group.ftl" as buttonGroup>
<#import "components/atoms/form.ftl" as form>
<#import "components/atoms/input.ftl" as input>
<#import "components/atoms/radio.ftl" as radio>
<#import "features/labels/totp.ftl" as totpLabel>

<img class="logo" src="https://www.curemd.com/cmd/cmd-assets/images/curemd-vector.svg" alt="CureMD Logo" />
  
<@layout.registrationLayout displayMessage=true; section>
  <#if section == "header">
    ${msg("doLogIn")}

  <#elseif section == "form">
    <@form.kw action=url.loginAction method="post">

<#if attributes?exists && attributes.showOtpSentMessage?? && attributes.showOtpSentMessage == true>
  <div id="otp-info-banner" style="
      background:#e8f5e9;
      color:#2e7d32;
      padding:12px;
      border-radius:6px;
      margin-bottom:1rem;
      font-size:14px;
      transition: opacity .5s ease;">
    ${msg("otpSent")}
  </div>

  <script>
    setTimeout(function () {
      var el = document.getElementById("otp-info-banner");
      if (el) {
        el.style.opacity = "0";
        setTimeout(function () { el.remove(); }, 500);
      }
    }, 6000);
  </script>
</#if>


      <!-- Expiry / countdown: prefer expiryEpochMillis (ms), fallback to attributes.expirySeconds -->
      <#if expiryEpochMillis?? || (attributes?exists && attributes.expiryEpochMillis??) || attributes.expirySeconds??>
        <div id="otp-expiry-container" style="margin-bottom:1rem; font-size:13px; color:#555;">
          OTP will expire in <span id="otp-countdown">--:--</span>
        </div>

        <script>
          (function () {
            // Read epoch as string (safe). Prefer top-level then attributes.
            <#if expiryEpochMillis??>
              var expiryEpochRaw = "${expiryEpochMillis!''}";
            <#elseif attributes?exists && attributes.expiryEpochMillis??>
              var expiryEpochRaw = "${attributes.expiryEpochMillis!''}";
            <#else>
              var expiryEpochRaw = "";
            </#if>

            // fallback TTL (seconds) if epoch not available
            var fallbackTtlRaw = "${attributes.expirySeconds! '0'}";

            // parse numeric values in JS
            var expiryEpoch = null;
            if (expiryEpochRaw !== null && expiryEpochRaw !== "") {
              expiryEpoch = parseInt(expiryEpochRaw, 10);
              if (isNaN(expiryEpoch)) { expiryEpoch = null; }
            }

            var remaining;
            if (expiryEpoch !== null) {
              remaining = Math.floor((expiryEpoch - Date.now()) / 1000);
            } else {
              var raw = fallbackTtlRaw;
              remaining = parseInt(raw, 10) || 0;
            }

            // UI elements
            var countdownEl = document.getElementById("otp-countdown");
            var containerEl = document.getElementById("otp-expiry-container");
            var resendBtn = document.querySelector('button[name="resend"], input[name="resend"]');

            function pad(n) { return n < 10 ? "0" + n : n; }

            function tick() {
              if (!countdownEl || !containerEl) return;

              if (remaining <= 0) {
  // Hide the timer silently
  containerEl.style.display = "none";

  // Enable resend button
  if (resendBtn) {
    resendBtn.disabled = false;
    resendBtn.classList.remove('disabled');
  }

  clearInterval(timerId);
  return;
}


              var mins = Math.floor(remaining / 60);
              var secs = remaining % 60;
              countdownEl.textContent = pad(mins) + ":" + pad(secs);

              // disable resend while OTP is valid (tweak if you want separate cooldown)
              if (resendBtn) { resendBtn.disabled = true; resendBtn.classList.add('disabled'); }

              remaining--;
            }

            // initial tick + interval
            tick();
            var timerId = setInterval(tick, 1000);
          })();
        </script>
      </#if>

      <!-- OTP input -->
      <@input.kw
        name="otp"
        type="text"
        autocomplete="off"
        autofocus=true
        invalid=messagesPerField.existsError("totp")
        label=msg("One-time code*")
        message=kcSanitize(messagesPerField.get("totp"))
      />

      <!-- Buttons: Sign In and Resend -->
      <div style="display:flex; gap:12px; margin-top:1.5rem;">
        <div style="flex:1;">
          <@button.kw
            color="primary"
            name="submitAction"
            value="login"
            type="submit"
            style="width:100%; display:block;">
            ${msg("doLogIn")}
          </@button.kw>
        </div>

        <div style="flex:1;">
          <@button.kw
            color="primary"
            name="resend"
            value="resend"
            type="submit"
            formnovalidate="formnovalidate"
            style="width:100%; display:block;">
            ${msg("Resend")}
          </@button.kw>
        </div>
      </div>

    </@form.kw>
  </#if>
</@layout.registrationLayout>
