import React, { useRef, useState, useEffect, useMemo } from "react";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import getStarted from "../../../public/get-started.svg";

export default function OtpForm(
  props: PageProps<Extract<KcContext, { pageId: "otp-form.ftl" }>, I18n>
) {
  const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
  const { url } = kcContext;
  const { msg, msgStr } = i18n;
  const maskedEmail = kcContext.attributes?.userEmail ?? "";
  const maskedMobile = kcContext.attributes?.userMobileNumber ?? "";
  const expirySeconds = kcContext.attributes?.expirySeconds ?? 60;
  let otpLength = kcContext.attributes?.length ?? 4; // make this configurable
  const resendTimer = kcContext.attributes?.resendTime ?? 60;
  // otpLength = 6;
  const { kcClsx } = getKcClsx({
    doUseDefaultCss,
    classes,
  });

  const inputRefs = Array.from({ length: otpLength }, () => useRef<HTMLInputElement>(null));
  const [otp, setOtp] = useState(Array(otpLength).fill(""));
  const [hasError, setHasError] = useState(false);

  const [remainingTime, setRemainingTime] = useState(expirySeconds);
  const [resendRemaining, setResendRemaining] = useState(resendTimer);
  const otpFormSubmitted = typeof window !== "undefined" && sessionStorage.getItem("otpFormSubmitted") === "true";
  const customError = kcContext.attributes?.customOtpError;
  const sticky = typeof window !== "undefined" ? sessionStorage.getItem("stickyOtpError") : null;

  const message = useMemo(() => {
    return kcContext.message
      ?? (customError ? { type: "error", summary: customError } : undefined)
      ?? (sticky ? { type: "error", summary: sticky } : undefined);
  }, [kcContext.message, customError, sticky]);
  const isMaxAttemptsReached = message?.type === "error" && message?.summary && message.summary === "Maximum attempts reached";


  useEffect(() => {
    // --- OTP countdown ---
    const maxReached = sessionStorage.getItem("maxAttemptsReached") === "true";
    let otpStart = Number(sessionStorage.getItem("otpStartTime"));
    if (!otpStart) {
      otpStart = Date.now();
      sessionStorage.setItem("otpStartTime", otpStart.toString());
    }

    // --- Resend countdown ---
    let resendStart = Number(sessionStorage.getItem("resendStartTime"));
    if (!resendStart) {
      resendStart = Date.now();
      sessionStorage.setItem("resendStartTime", resendStart.toString());
    }

    const tick = () => {
      if (maxReached) {
        setRemainingTime(0);
      } else {
        const otpElapsed = Math.floor((Date.now() - otpStart) / 1000);
        setRemainingTime(Math.max(expirySeconds - otpElapsed, 0));
      }

      const resendElapsed = Math.floor((Date.now() - resendStart) / 1000);
      setResendRemaining(Math.max(resendTimer - resendElapsed, 0));
    };

    tick();
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, []);



  useEffect(() => {
    if (isMaxAttemptsReached) {
      sessionStorage.setItem("maxAttemptsReached", "true"); // block future attempts
      setRemainingTime(0);
    }
  }, [isMaxAttemptsReached]);


  useEffect(() => {
    const allFilled = otp.every((digit) => digit !== "");
    if (allFilled && !otpFormSubmitted && !hasError) {
      sessionStorage.setItem("otpFormSubmitted", "true");
      const form = document.getElementById("kc-otp-code-form") as HTMLFormElement;
      form?.submit();
    }
  }, [otp, otpFormSubmitted, isMaxAttemptsReached]);


  useEffect(() => {

    if (message?.type === "error" && message.summary !== "Maximum attempts reached") {
      sessionStorage.removeItem("otpFormSubmitted");
      setOtp(Array(otpLength).fill(""));
      inputRefs[0].current?.focus();
      setHasError(true);

    }
  }, [message]);

  useEffect(() => {
    const savedOtp = sessionStorage.getItem("otpValues");
    if (savedOtp) {
      setOtp(JSON.parse(savedOtp));
    }
  }, []);

  useEffect(() => {
    sessionStorage.setItem("otpValues", JSON.stringify(otp));
  }, [otp]);




  const handleChange = (index: number, value: string) => {
    if (!/^\d?$/.test(value)) return;
    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);
    sessionStorage.removeItem("otpFormSubmitted");
    sessionStorage.removeItem("stickyOtpError");
    if (value && index < inputRefs.length - 1) {
      inputRefs[index + 1].current?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key !== "Backspace" && e.key !== "Delete") return;

    e.preventDefault();
    const next = [...otp];

    if (next[index]) {
      next[index] = "";
      setOtp(next);
      return;
    }
    if (index > 0) {
      next[index - 1] = "";
      setOtp(next);
      inputRefs[index - 1].current?.focus();
    }
  };


  if (typeof window !== "undefined") {
    try {
      const target = (sessionStorage.getItem("flowEntrypoint") as string) || (kcContext.url.loginRestartFlowUrl as string) || "/";
      const nav = (performance.getEntriesByType?.("navigation")[0] || {}) as any;
      const cameFromBack = nav?.type === "back_forward";
      const fromOtp = document.referrer && /\/otp/i.test(document.referrer);

      if (cameFromBack || fromOtp) {
        const loc = window.top && window.top !== window ? window.top.location : window.location;
        loc.replace(target);
        return null as any;
      }
      window.addEventListener("unload", () => { });
    } catch {
      /* ignore */
    }
  }


  return (
    <Template
      // kcContext={kcContext}
      kcContext={{ ...kcContext, message: undefined }} // suppress top-level display
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      displayInfo={false}
      headerNode={
        <div id="kc-username" className={kcClsx("kcFormGroupClass")} style={{ fontSize: "16px" }}>
          <a id="reset-login" href={url.loginRestartFlowUrl} aria-label={msgStr("restartLoginTooltip")}>
            <div className="kc-login-tooltip">
              <i className={kcClsx("kcResetFlowIcon")} />
              <span className="kc-tooltip-text">{msg("restartLoginTooltip")}</span>
            </div>
          </a>
        </div>
      }
    >
      <div className="auth-section form">
        <div className="auth-wrapper">
          <div className="mobile-screen-icon">
            <img src={getStarted} alt="" />
          </div>
          <div className="auth-header with-subtitle text-center">Enter the 4-digit code</div>
          <div className="auth-subheader text-center">
            {/* <span>We sent a code to {maskedEmail}</span> */}
            <span>
              {maskedMobile && maskedEmail ? (
                <>
                  We sent a code to your cell ending in <strong>{maskedMobile}</strong> <br/>and{" "}
                <strong>  {maskedEmail}</strong>
                </>
              ) : maskedMobile ? (
                <>
                  We sent a code to your cell ending in <strong>{maskedMobile}</strong>
                </>
              ) : (
                <>We sent a code to <strong>{maskedEmail}</strong></>
              )}
            </span>
            <br />
            <span>To keep your account safe, do not share</span>
            <br />
            <span>this code with anyone</span>
          </div>



          <form id="kc-otp-code-form" action={url.loginAction} method="post">
            <div className="code-input-container ">
              {otp.map((value, index) => (
                <input
                  key={index}
                  ref={inputRefs[index]}
                  id={`digit${index + 1}`}
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  className={`code-input mt-0 ${hasError ? "invalid" : ""}`}
                  style={{
                    color: hasError ? "#000" : undefined,
                    caretColor: hasError ? "#000" : undefined,
                  }}
                  maxLength={1}
                  autoComplete="off"
                  value={value}
                  onChange={(e) => {
                    handleChange(index, e.target.value);
                    if (hasError)
                      setHasError(false);
                  }}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                />

              ))}
            </div>
            <input type="hidden" name="otp" value={otp.join("")} />
          </form>
          {message?.type === "error" &&
            message?.summary && (
              <div className="otp-warning">
                <svg xmlns="http://www.w3.org/2000/svg" width="17" height="18" viewBox="0 0 17 18" fill="none">
                  <path d="M7.66797 11.5003H9.33464V13.167H7.66797V11.5003ZM7.66797 4.83366H9.33464V9.83366H7.66797V4.83366ZM8.49297 0.666992C3.89297 0.666992 0.167969 4.40033 0.167969 9.00033C0.167969 13.6003 3.89297 17.3337 8.49297 17.3337C13.1013 17.3337 16.8346 13.6003 16.8346 9.00033C16.8346 4.40033 13.1013 0.666992 8.49297 0.666992ZM8.5013 15.667C4.81797 15.667 1.83464 12.6837 1.83464 9.00033C1.83464 5.31699 4.81797 2.33366 8.5013 2.33366C12.1846 2.33366 15.168 5.31699 15.168 9.00033C15.168 12.6837 12.1846 15.667 8.5013 15.667Z" fill="#FF0000" />
                </svg>

                <span className="ms-2">{message.summary}</span>
              </div>
            )}

          <div className="text-center">
            {!isMaxAttemptsReached && resendRemaining <= 0 && (

              <div className="resend-code mb-3">
                <form id="resend-form" method="post" action={kcContext.url.loginAction} style={{ display: "none" }}>
                  <input type="hidden" name="resend" value="true" />
                </form>

                <span>
                  Didn't get the code?{" "}
                  <a
                    href="#"
                    id="resend"
                    className="ms-1"
                    onClick={(e) => {
                      e.preventDefault();
                      // if (remainingTime > 0) return; //uncomment later
                      if (remainingTime > 0) {
                        //do nothing , just to avoid unused variable error
                      }
                      if (resendRemaining > 0) return;
                      // Only way to reset maxAttemptsReached flag:
                      sessionStorage.removeItem("maxAttemptsReached");
                      const prevCount = parseInt(sessionStorage.getItem("otpResendCount") || "0");
                      sessionStorage.setItem("otpResendCount", (prevCount + 1).toString());
                      // sessionStorage.removeItem("otpFormSubmitted"); // reset form flag
                      sessionStorage.setItem("otpFormSubmitted", "true"); // block auto-submit until user edits

                      sessionStorage.setItem("otpStartTime", Date.now().toString()); // Restart timer
                      sessionStorage.setItem("resendStartTime", Date.now().toString());
                      if (message?.type === "error" && message.summary && message.summary !== "Maximum attempts reached") {
                        sessionStorage.setItem("stickyOtpError", message.summary);
                      }
                      // setResendRemaining(resendTimer); // immediate UI feedback
                      (document.getElementById("resend-form") as HTMLFormElement)?.requestSubmit();
                    }}
                    style={{
                      // pointerEvents: remainingTime === 0 || otpFormSubmitted ? "auto" : "none",
                      pointerEvents: resendRemaining === 0 ? "auto" : "none",
                      opacity: resendRemaining === 0 ? 1 : 0.5,
                      textDecoration: resendRemaining === 0 ? "underline" : "none",
                      cursor: resendRemaining === 0 ? "pointer" : "default",
                      color: resendRemaining === 0 ? "#7F65ED" : "#AAA",
                    }}
                  >
                    Resend Code
                  </a>
                </span>
              </div>
            )}
            {!isMaxAttemptsReached && resendRemaining > 0 && (
              <div className="resend-code-timer">
                Resend Available in: <span className="leap-primary">{resendRemaining} seconds</span>
                {/* OTP expires in: <span className="leap-primary">{formatTime(remainingTime)}</span> */}
              </div>
            )}
          </div>
        </div>
      </div>
    </Template>
  );
}
