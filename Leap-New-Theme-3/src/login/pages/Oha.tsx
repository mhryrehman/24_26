import { useRef, useState, useEffect } from "react";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function Oha(
  props: PageProps<Extract<KcContext, { pageId: "oha.ftl" }>, I18n>
) {
  const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
  const { url } = kcContext;
  const { msg: _msg, msgStr } = i18n;
  const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });

  const formRef = useRef<HTMLFormElement | null>(null);
  const hiddenRef = useRef<HTMLInputElement | null>(null);
  const textRef = useRef<HTMLDivElement | null>(null);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  const [actionsEnabled, setActionsEnabled] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submitWith = (val: "true" | "false") => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    if (hiddenRef.current) hiddenRef.current.value = val;
    formRef.current?.requestSubmit();
  };

  const reachedEnd = () => {
    const el = textRef.current;
    if (!el) return false;
    const { scrollTop, clientHeight, scrollHeight } = el;
    return scrollTop + clientHeight >= scrollHeight - 4;
  };

  const checkAndEnable = () => {
    if (!actionsEnabled && reachedEnd()) setActionsEnabled(true);
  };

  const setScrollRef = (node: HTMLDivElement | null) => {
    scrollRef.current = node;
    if (!node) return;
    requestAnimationFrame(() => node.focus({ preventScroll: true }));
  };

  useEffect(() => {
    checkAndEnable();

    const el = scrollRef.current;
    if (!el) return;
    const id = setTimeout(() => el.focus({ preventScroll: true }), 0);
    return () => clearTimeout(id);
  }, []);

  const handleScroll = () => checkAndEnable();

  const handleKeyDown: React.KeyboardEventHandler<HTMLDivElement> = (e) => {
    if (["PageDown", "End", "ArrowDown", " "].includes(e.key)) {
      requestAnimationFrame(checkAndEnable);
    }
  };

  const handleClick: React.MouseEventHandler<HTMLDivElement> = () => {
    requestAnimationFrame(checkAndEnable);
  };

  if (typeof window !== "undefined") {
    try {
      const target =
        (sessionStorage.getItem("flowEntrypoint") as string) ||
        (kcContext.url.loginRestartFlowUrl as string) ||
        "/";

      const nav = (performance.getEntriesByType?.("navigation")[0] || {}) as any;
      const cameFromBack = nav?.type === "back_forward";
      const fromoha = document.referrer && /\/oha/i.test(document.referrer);
      if (cameFromBack || fromoha) {
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
      kcContext={kcContext}
      i18n={i18n}
      doUseDefaultCss={doUseDefaultCss}
      classes={classes}
      displayMessage={true}
      headerNode="OHA Consent"
    >
      <div className="oha-container">
        <div
          id="kc-oha-text"
          ref={textRef}
          tabIndex={-1}
        >
          <div className="oha-title" >Optional Health Information Authorization</div>
          <div className="oha-wrapper">
            <div
              className="term-condition"
              ref={setScrollRef}
              tabIndex={0}
              role="region"
              aria-label="Optional Health Information Authorization text"
              onScroll={handleScroll}
              onKeyDown={handleKeyDown}
              onClick={handleClick}
              autoFocus
            >
              <p>By selecting the "Agree" option, you authorize Leap Health, CureMD.com, Inc., and your healthcare provider to provide
                you with access to personalized health-related content via the Leap platform and to share your protected health
                information (“Data”) with third parties as necessary. You also consent to us using your Data to conduct data analysis to
                enhance the effectiveness of the below-mentioned service</p>
              <p>We aim to enhance your healthcare experience by offering information tailored to your health needs, including, without
                limitation, INSIGHTS INTO TREATMENT OPTIONS, THERAPIES, SUITABLE INSURANCE PLANS, PHARMACIES,
                PROGRAMS FROM PHARMACEUTICAL COMPANIES, EDUCATIONAL MATERIALS, INFORMATION ON FINANCIAL AID
                PROGRAMS, OPPORTUNITIES FOR CLINICAL TRIALS, SPECIAL OFFERS, COUPONS, AND ADVERTISEMENTS FOR THIRD-PARTY SERVICES, AND OTHER HEALTH-RELATED SERVICES AND SUPPLIES. We may communicate with you through
                alerts, reminders, emails, advertisements, and other communications. We may receive financial benefits/payments for
                providing you with this information</p>
              {/* <p>
                We aim to enhance your healthcare experience by offering information tailored to your health needs, including, without
                limitation, INSIGHTS INTO TREATMENT OPTIONS, THERAPIES, SUITABLE INSURANCE PLANS, PHARMACIES,
                PROGRAMS FROM PHARMACEUTICAL COMPANIES, EDUCATIONAL MATERIALS, INFORMATION ON FINANCIAL AID
                PROGRAMS, OPPORTUNITIES FOR CLINICAL TRIALS, SPECIAL OFFERS, COUPONS, AND ADVERTISEMENTS FOR THIRD-PARTY SERVICES, AND OTHER HEALTH-RELATED SERVICES AND SUPPLIES. We may communicate with you through
                alerts, reminders, emails, advertisements, and other communications. We may receive financial benefits/payments for
                providing you with this information.
              </p> */}
              <p>The Data we share may include your name, email, address, or phone number to facilitate the above services and other
                health related information. In addition, your approval specifically covers the use and sharing of any additional
                information that you provide to us.</p>
              <p>
                <strong style={{ textDecoration: "underline", display: "block", marginBottom: "8px" }}>Privacy and Security Commitments</strong>
                We are committed to protecting your Data in accordance with federal guidelines and our <a href="https://leaphealth.ai/privacy-policy/" style={{ color: "#765AEB" }} target="_blank" rel="noopener noreferrer">Privacy Policy</a>. We implement
                strict security measures to safeguard your Data, using it only to deliver services mentioned hereunder, conduct
                anonymous analyses to improve content, and comply with legal requirements. It is important to note that information
                shared with third parties may no longer be protected by privacy laws. WE DO NOT SHARE DATA WITH ANY ENTITY
                APART FROM YOUR HEALTHCARE OR SERVICE PROVIDER UNLESS EXPRESSLY AUTHORIZED BY THIS AUTHORIZATION.
              </p>
              <p>
                <strong style={{ textDecoration: "underline", display: "block", marginBottom: "8px" }}>Opt-Out</strong>
                You have the flexibility to withdraw this consent at any time via our settings page
              </p>
            </div>
          </div>
        </div>

        <form ref={formRef} className="form-actions" action={url.loginAction} method="POST">
          <div className="oha-button oha-two-button" style={{ display: "flex", justifyContent: "end", alignItems: "center", gap: "10px" }}>
            <input ref={hiddenRef} type="hidden" name="user.attributes.oha_accepted" value="" />
            <span className="agree  d-none">
              <button type="button"
                id="kc-accept"
                className={kcClsx("kcButtonClass", "kcButtonPrimaryClass", "kcButtonLargeClass")}> I Agree
              </button>
            </span>
            <span className="decline">
              <button
                type="button"
                id="kc-decline"
                className={kcClsx("kcButtonClass", "kcButtonDefaultClass", "kcButtonLargeClass")}
                onClick={() => submitWith("false")}
                disabled={isSubmitting}
              >
                {msgStr("doDecline") ?? "Decline"}
              </button>

            </span>
            <span className="">
              <button
                type="button"
                id="kc-accept"
                className={kcClsx("kcButtonClass", "kcButtonPrimaryClass", "kcButtonLargeClass")}
                onClick={() => submitWith("true")}
                disabled={isSubmitting}
              >
                {msgStr("doAccept") ?? "Accept"}
              </button>
            </span>
          </div>
        </form>
      </div>
    </Template>
  );
}
