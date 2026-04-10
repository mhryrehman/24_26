<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Enter OTP</title>
    <style>
        body {
            background: #f6f3ff;
            min-height: 100vh;
            margin: 0;
            font-family: 'Inter', Arial, sans-serif;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: flex-start;
        }
        .leap-logo {
            display: flex;
            flex-direction: column;
            align-items: center;
            margin-top: 48px;
            margin-bottom: 12px;
        }
        .leap-logo img {
            height: 70px;
        }
        .leap-subtitle {
            color: #8d6ee8;
            font-size: 1.35rem;
            font-weight: 500;
            margin-top: 0.5rem;
            margin-bottom: 2.5rem;
            text-align: center;
        }
        .container {
            background: #fff;
            border-radius: 18px;
            box-shadow: 0 4px 24px 0 rgba(60,106,243,0.08);
            padding: 2.5rem 2.5rem 2rem 2.5rem;
            width: 100%;
            max-width: 420px;
            margin: 0 auto 2rem auto;
            display: flex;
            flex-direction: column;
            align-items: center;
            position: relative;
            z-index: 2;
        }
        .otp-title {
            font-size: 2rem;
            font-weight: bold;
            color: #2d2d2d;
            margin-bottom: 0.5rem;
            text-align: center;
        }
        .otp-desc {
            font-size: 1.1rem;
            color: #7a7a7a;
            margin-bottom: 1.5rem;
            text-align: center;
        }
        .otp-label {
            font-weight: 500;
            color: #2d2d2d;
            margin-bottom: 0.5rem;
            display: block;
        }
        .otp-input {
            padding: 12px;
            width: 100%;
            border-radius: 6px;
            border: 2px solid #d1d5db;
            font-size: 1rem;
            margin-bottom: 0.5rem;
            outline: none;
            transition: border 0.2s;
        }
        .otp-input:focus {
            border: 2px solid #3c6af3;
        }
        .otp-input.error {
            border: 2px solid red;
        }
        .otp-error {
            color: red;
            font-size: 0.95rem;
            margin-bottom: 0.5rem;
            width: 100%;
            text-align: left;
        }
        .otp-timer {
            margin-bottom: 1.5rem;
            text-align: center;
            font-weight: 500;
            color: #8d6ee8;
        }
        .otp-buttons {
            display: flex;
            gap: 10px;
            width: 100%;
            margin-bottom: 1.2rem;
        }
        .otp-btn {
            flex: 1;
            padding: 12px 0;
            border: none;
            border-radius: 6px;
            font-weight: 600;
            font-size: 1rem;
            cursor: pointer;
            transition: background 0.2s, color 0.2s;
        }
        .otp-btn.primary {
            background-color: #8d6ee8;
            color: #fff;
        }
        .otp-btn.primary:hover:not(:disabled),
        .otp-btn.primary:focus:not(:disabled) {
            background-color: #7a5fd6;
        }
        .otp-btn.primary:disabled {
            background-color: #b3c6fa;
            color: #fff;
            cursor: not-allowed;
        }
        .otp-btn.resend {
            background-color: #ffc107;
            color: #2d2d2d;
        }
        .otp-btn.resend:disabled {
            background-color: #e0e0e0;
            color: #2d2d2d;
            cursor: not-allowed;
        }
        .otp-btn.cancel {
            width: 100%;
            background-color: #f5f5f5;
            color: #2d2d2d;
            border: 1px solid #ccc;
            margin-top: 0.5rem;
        }
        .otp-global-error {
            color: red;
            font-weight: bold;
            text-align: center;
            margin-top: 1.5rem;
        }

        .otp-input-box:focus {
        border-color: #8d6ee8;
        outline: none;
        }
    </style>
</head>
<body>
    <div class="leap-logo">
        <img src="data:image/png;base64,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" alt="Leap Logo" />
        <div class="leap-subtitle">Your Personal Care Companion</div>
    </div>
    <div class="container">
        <div class="otp-title">Enter OTP</div>
    <#if userEmail?? && userEmail?has_content>
        <p class="otp-desc">
            A code has been sent to your email address: <strong>${userEmail}</strong>
        </p>
    </#if>

        <form id="kc-otp-login-form" action="${url.loginAction}" method="post" style="width: 100%;">
        <div style="display: flex; justify-content: center; gap: 10px;  margin: 0 auto 1.5rem auto;
    width: fit-content;">
        <#list 1..4 as i>
        <input id="otp${i}" name="otp${i}" maxlength="1" pattern="[0-9]*" inputmode="numeric" required
            class="otp-input-box"
            style="width: 50px; height: 50px; text-align: center; font-size: 24px; border: 1px solid #ccc; border-radius: 8px;" />
        </#list>
        <input type="hidden" id="emailCode" name="emailCode" />
        </div>

            <#if messagesPerField.existsError('emailCode')>
                <div class="otp-error">
                    ${kcSanitize(messagesPerField.get('emailCode'))?no_esc}
                </div>
            </#if>
       <#if expirySeconds?? && expirySeconds?number != 0>
         <#assign totalSeconds = expirySeconds?number>
         <#assign minutes = (totalSeconds / 60)?floor>
         <#assign seconds = totalSeconds % 60>
         <#assign mm = minutes?string("00")>
         <#assign ss = seconds?string("00")>

            <div class="otp-timer">
             OTP expires in <span id="countdown">${mm}:${ss}</span>
            </div>
     </#if>


<#if message?has_content && message.summary == "Maximum attempts reached">
    <div id="maxAttemptsError" style="color:red; font-weight:bold; text-align:center; background:#ffe6e6; border:1px solid red; padding:10px; border-radius:5px; margin-bottom: 1rem;">
        Maximum attempts reached
    </div>
    <script>
        setTimeout(function () {
            var errorBox = document.getElementById("maxAttemptsError");
            if (errorBox) errorBox.style.display = "none";
        }, 3000);
    </script>
</#if>


            <div class="otp-buttons">
               
                <button id="resendBtn" name="resend" type="submit" class="otp-btn resend" disabled formnovalidate>
                    Resend Code
                </button>
            </div>
            <#if message?has_content && message.summary == "Invalid access code.">
                <script>
                    // Enable resend button if present
                    window.addEventListener("load", function () {
                    var resendBtn = document.getElementById("resendBtn");
                    if (resendBtn) {
                    resendBtn.disabled = false;
                    resendBtn.style.cursor = "pointer";
                }
            });
                </script>
            </#if>

            <button name="cancel" type="submit" class="otp-btn cancel" formnovalidate>
                Cancel
            </button>
        </form>
        
    </div>
    <script>
        var seconds = ${expirySeconds!60};
        var countdownElem = document.getElementById("countdown");
        var resendBtn = document.getElementById("resendBtn");
         window.onload = function () {
        var countdownInterval = setInterval(function () {
            var minutes = Math.floor(seconds / 60);
            var secs = seconds % 60;
            countdownElem.textContent =
                (minutes < 10 ? "0" + minutes : minutes) + ":" +
                (secs < 10 ? "0" + secs : secs);

            if (seconds <= 0) {
                clearInterval(countdownInterval);
                countdownElem.textContent = "00:00";
                resendBtn.disabled = false;
                resendBtn.style.cursor = "pointer";
            }

            seconds--;
        }, 1000);
    };

    const otpInputs = Array.from(document.querySelectorAll(".otp-input-box"));
    const emailCodeInput = document.getElementById("emailCode");
    const form = document.getElementById("kc-otp-login-form");
    otpInputs.forEach((input, index) => {
        input.addEventListener("input", () => {
            // Move to next input
            if (input.value.length === 1 && index < otpInputs.length - 1) {
                otpInputs[index + 1].focus();
            }

            // Update hidden field
            emailCodeInput.value = otpInputs.map(i => i.value).join('');

            // Auto-submit after 4th digit
            if (emailCodeInput.value.length === 4 && index === otpInputs.length - 1) {
                form.submit();
            }
        });

        input.addEventListener("keydown", (e) => {
            if (e.key === "Backspace" && input.value === "" && index > 0) {
                otpInputs[index - 1].focus();
            }
        });
    });
    </script>
</body>
</html>
