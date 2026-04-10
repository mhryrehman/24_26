import { useState, useRef, useEffect } from "react";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";


export default function Terms(
  props: PageProps<Extract<KcContext, { pageId: "terms.ftl" }>, I18n>
) {
  const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
  const { url } = kcContext;
  const { msg, msgStr:_msgStr } = i18n;
  const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });

  const [accepted, setAccepted] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const termsRef = useRef<HTMLDivElement | null>(null);
  const reachedEnd = () => {
    const el = termsRef.current;
    if (!el) return false;
    const overflow = el.scrollHeight - el.clientHeight;
    if (overflow > 1) {
      return el.scrollTop + el.clientHeight >= el.scrollHeight - 1; 
    }
    const rect = el.getBoundingClientRect();
    const vpH = window.innerHeight || document.documentElement.clientHeight;
    return rect.bottom <= vpH + 2; 
  };

  const checkAndAccept = () => {
    if (!accepted && reachedEnd()) setAccepted(true);
  };

  useEffect(() => {
    const el = termsRef.current;
    if (!el) return;
    if (el && el.scrollHeight <= el.clientHeight + 1) setAccepted(true);

    termsRef.current?.focus({ preventScroll: true });
    checkAndAccept();
  }, []);


  const onTermsScroll: React.UIEventHandler<HTMLDivElement> = (e) => {
    const el = e.target as HTMLElement;
    const atEnd = el.scrollTop + el.clientHeight >= el.scrollHeight - 2;
    if (atEnd) setAccepted(true);
  };

  if (typeof window !== "undefined") {
    try {
      const target =
        (sessionStorage.getItem("flowEntrypoint") as string) ||
        (kcContext.url.loginRestartFlowUrl as string) ||
        "/";

      const nav = (performance.getEntriesByType?.("navigation")[0] || {}) as any;
      const cameFromBack = nav?.type === "back_forward";
      const fromterms = document.referrer && /\/terms/i.test(document.referrer);
      if (cameFromBack || fromterms) {
        const loc =
          window.top && window.top !== window ? window.top.location : window.location;
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
      headerNode={msg("termsTitle")}
    >
      <div className="oha-container ">
        <div
          className="oha-section"
          id="kc-terms-text"
          ref={termsRef}
        tabIndex={-1}
        >
          <div className="oha-title"><span className="d-block">End User License Agreement</span>
            Leap Health Mobile Software Product ("Product") </div>
          <div className="oha-wrapper">
            <p className="term-condition" onScroll={onTermsScroll} onKeyDown={onTermsScroll} tabIndex={0}>
              <p>This Leap Health Mobile Software Product ("Product") distributed via the Apple App Store and Google Play Store, is licensed, to the
                end user by Leap Health and CureMD.com, Inc. including its subsidiaries and affiliates (“Leap”, “we” or “us”). By installing or using the
                Product, you ("you", “your”) agree to be bound by the terms of this End User License Agreement ("EULA"), and all other terms and
                policies that appear on the Product, including the <a href="https://leaphealth.ai/end-eser-license-agreement/" style={{ color: "#765AEB" }} target="_blank" rel="noopener noreferrer"> EULA</a>, <a href="https://leaphealth.ai/services-agreement/" style={{ color: "#765AEB" }} target="_blank" rel="noopener noreferrer">Services Agreement</a>, <a href="https://leaphealth.ai/terms-of-use/" style={{ color: "#765AEB" }} target="_blank" rel="noopener noreferrer">Terms of Use</a> and <a href="https://leaphealth.ai/privacy-policy/" style={{ color: "#765AEB" }} target="_blank" rel="noopener noreferrer">Privacy Policy</a>, which govern your use
                of the Product. All rights, title, and interest in and to the Product not expressly granted to you under this EULA are reserved by us.</p>
              <p><strong><span className="me-2">1.</span>License Grant and Restrictions.</strong>
                We grant you a revocable, non-exclusive, non-transferable, limited license to download, install,
                and use the Product on devices you own or control, in accordance with the <a href="https://apps.apple.com/us/app/leap-your-health-companion/id1659618898" style={{ color: "#765AEB" }} target="_blank" rel="noopener noreferrer"> Apple App Store </a> and <a href="https://play.google.com/intl/en-US_us/about/play-terms/index.html" style={{ color: "#765AEB" }} target="_blank" rel="noopener noreferrer"> Google Play Store Terms of Service</a>, as third party beneficiary to enforce this EULA . This License prohibits the use of the Product on any device that you do
                not own or control. You are not allowed to distribute or make the Product accessible over a network where it could be used by
                multiple devices at the same time. You may not rent, lease, lend, sell, redistribute, or sublicense the Product. Additionally, you
                may not copy, decompile, reverse engineer, disassemble, attempt to derive the source code of, modify, or create derivative works
                of the Product or any updates or parts of it.</p>
              <p><strong><span className="me-2">2.</span>Services and Functionalities.</strong>
                The Product is an all-in-one patient engagement platform that facilitates various services including
                telehealth consultations, digital check-in systems, a Find-a-Doc marketplace, reputation management for healthcare providers,
                online patient scheduling, and a patient chat system. The Product enables you to access, review, and interact with specific data,
                and perform certain actions with this data, through a connection to your healthcare provider(s) (“Provider”) with our Leap
                application. The specifics of these functionalities are subject to change and may be updated or modified by us without prior
                notice to ensure continuous improvement and compliance with health regulations and user needs.</p>
              <p><strong><span className="me-2">3.</span> Account and Your Responsibilities.</strong> Use of the Product requires an active account with healthcare facilities that have partnered
                with us. You are responsible for all activities that occur under your account and must promptly notify us of any unauthorized use
                of your account or any other breaches of security. The management of your credentials and the confidentiality of your account
                information are your sole responsibility.</p>
              <p><strong><span className="me-2">4.</span> Data Privacy and Security.</strong> The Product involves processing, collecting, storing, maintaining, uploading, syncing, transmitting,
                sharing, disclosing and using personal and health-related information. Our Privacy Policy, outlines our data handling practices.
                The Product employs commercially reasonable security measures to protect your data, but you acknowledge that no internet
                transmission is completely secure or error-free and that the responsibility for personal data uploaded to the Product ultimately
                lies with you.</p>
              <p>By installing or using the Product, or pressing the ‘I AGREE’ button, YOU EXPRESSLY CONSENT TO THE COLLECTION, STORAGE,
                PROCESSING, MAINTENANCE, UPLOADING, SYNCING, TRANSMITTING, SHARING, AND DISCLOSURE OF YOUR DATA,
                INCLUDING BUT NOT LIMITED TO AUTHENTICATION, PERFORMANCE OPTIMIZATION, SOFTWARE UPDATES, AND PRODUCT
                SUPPORT. By continuing to use the Product, you indicate your ongoing consent to these activities and to any collection, storage,
                transmission, and use of data in accordance with the Privacy Policy.
              </p>
              <p> You acknowledge that: (1) information regarding the software, hardware model and iOS version of the device on which you are
                running the Product may be collected, transmitted to, and stored on a database server designated by your Provider, may be
                transmitted to us, and may be used to make changes, updates, or improvements to, or to optimize the performance of the
                Product or to inform future development; and (2) audit logs reflecting your logins, logouts, and the activities you have accessed
                through your use of the Product may be generated, collected, transmitted, and stored on a database server designated by your
                Provider, may be made available to us, and may also be made available to us for troubleshooting. Additionally, this information
                may be used for providing, customizing, and improving the services; marketing the services; corresponding with you; complying
                with legal requirements; health information exchanges; onboarding verification; maintaining or servicing accounts; providing
                customer service; processing or fulfilling orders and transactions; verifying customer information; processing payments; providing
                financing; providing advertising or marketing services; providing analytic services or similar services on behalf of the business or
                service provider; undertaking internal research for technological development and demonstration; undertaking activities to verify
                or maintain the quality or safety of our services; advertisement and marketing; auditing; sending personalized messages and
                surveys; detecting security incidents; resisting malicious, deceptive, or illegal actions; ensuring the physical safety of individuals;
                for short-term, transient use, including non-personalized advertising; performing or providing internal business functions; and
                compliance with relevant standards and rules.
              </p>
              <p><strong><span className="me-2">5.</span> Compliance with Applicable Laws.</strong> You agree to use the Product in compliance with all applicable laws and regulations,
                including but not limited to laws related to the protection of personal information, healthcare regulations, and any applicable
                international laws. This includes adhering to laws governing the use of telehealth services and data protection.
                The terms of the License will govern any upgrades provided by us that replace or enhance the original Product, unless a separate
                license accompanies the upgrade. In such cases, the terms of the new license will apply. You agree to promptly install any such
                upgrades and discontinue use of the prior version. This paragraph does not obligate us to create or supply any upgrades to the
                Product.
              </p>
              <p><strong><span className="me-2">6.</span> Termination.</strong> The License is effective unless terminated by us at any time for convenience. Your rights under this License will
                automatically terminate for cause without notice from us effective immediately if you fail to adhere to any of its terms. Upon
                termination, you are required to cease all use of the Product and destroy all copies, whether in whole or in part.
              </p>
              <p><strong><span className="me-2">7.</span> DISCLAIMER OF WARRANTIES.</strong> YOU EXPRESSLY ACKNOWLEDGE AND AGREE THAT YOUR USE OF THE PRODUCT AND YOUR
                RELIANCE ON ITS OPERATION, OUTPUT, OR RESULTS IS AT YOUR SOLE RISK. THE ENTIRE RISK AS TO SATISFACTORY QUALITY,
                PERFORMANCE, ACCURACY, AND EFFORT IS WITH YOU. TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, THE
                PRODUCT AND ANY SERVICES PERFORMED OR PROVIDED BY THE PRODUCT ("APPLICATION SERVICES") ARE PROVIDED "AS IS"
                AND "AS AVAILABLE", WITH ALL FAULTS AND WITHOUT WARRANTY OF ANY KIND. WE HEREBY DISCLAIM ALL WARRANTIES
                AND CONDITIONS WITH RESPECT TO THE PRODUCT AND ANY APPLICATION SERVICES, EITHER EXPRESS, IMPLIED, OR
                STATUTORY, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES AND/OR CONDITIONS OF MERCHANTABILITY, OF
                SATISFACTORY QUALITY, OF FITNESS FOR A PARTICULAR PURPOSE, OF ACCURACY, OF QUIET ENJOYMENT, AND NON-
                INFRINGEMENT OF THIRD-PARTY RIGHTS. WE DO NOT WARRANT THAT THE FUNCTIONS CONTAINED IN, OR SERVICES
                PERFORMED OR PROVIDED BY, THE PRODUCT WILL MEET YOUR REQUIREMENTS, THAT THE OPERATION OF THE PRODUCT OR
                APPLICATION SERVICES WILL BE UNINTERRUPTED OR ERROR-FREE, THAT ANY SERVICE INTERRUPTIONS WILL BE CORRECTED,
                OR THAT THE DATA OR INFORMATION PROVIDED THROUGH THE PRODUCT WILL BE ACCURATE. NO ORAL OR WRITTEN
                INFORMATION OR ADVICE GIVEN BY US OR ITS AUTHORIZED REPRESENTATIVE SHALL CREATE A WARRANTY. SHOULD THE
                PRODUCT OR APPLICATION SERVICES PROVE DEFECTIVE, YOU ASSUME THE ENTIRE COST OF ALL NECESSARY SERVICING,
                REPAIR, OR CORRECTION. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OF IMPLIED WARRANTIES OR LIMITATIONS
                ON APPLICABLE STATUTORY RIGHTS OF A CONSUMER, SO THE ABOVE EXCLUSIONS AND LIMITATIONS MAY NOT APPLY TO
                YOU. IN JURISDICTIONS WHERE EXCLUSIONS ARE ALLOWED, THESE EXCLUSIONS SHALL APPLY TO THE FULLEST EXTENT
                PERMITTED BY LAW.
              </p>
              <p><strong><span className="me-2">8.</span> Optional Health Information Authorization.</strong> By selecting the 'Optional Opt-In Authorization' checkbox during your acceptance
                of this EULA, you authorize us and your healthcare provider to provide you with access to personalized health-related content
                via the Leap platform and to share your protected health information ("Data") with third parties as necessary. This authorization
                also includes your consent for the use of your Data to conduct data analysis aimed at improving the effectiveness of the services
                outlined below.</p>
              <p>These services may include, without limitation, insights into treatment options, therapies, suitable insurance plans, pharmacies,
                programs from pharmaceutical companies, educational materials, information on financial aid programs, opportunities for clinical
                trials, special offers, coupons, advertisements for third-party services, and other health-related services and supplies.
                Communications under this authorization may include alerts, reminders, emails, advertisements, and other messages.</p>
              <p>
                YOU ACKNOWLEDGE AND AGREE THAT WE MAY RECEIVE FINANCIAL BENEFITS OR PAYMENTS FOR PROVIDING YOU WITH THIS
                INFORMATION. The Data we share may include your name, email, address, phone number, and other health-related information
                necessary to facilitate the services described above. This authorization also covers the use and sharing of any additional
                information that you voluntarily provide.
              </p>
              <p><span className="me-2" style={{ fontSize: "14 !important" }}>a.</span><span style={{ fontSize: "12 !important", textDecoration: "underline" }}>Privacy and Security Commitments. </span>
                We are committed to protecting your Data in accordance with applicable federal
                guidelines and our Privacy Policy. We implement strict security measures to safeguard your Data, use it solely for the
                purposes described in this clause, conduct anonymous analyses to improve content, and comply with legal requirements.
                YOU UNDERSTAND AND AGREE THAT DATA SHARED WITH THIRD PARTIES MAY NO LONGER BE PROTECTED BY PRIVACY
                LAWS. WE DO NOT SHARE YOUR DATA WITH ANY ENTITY APART FROM YOUR HEALTHCARE OR SERVICE PROVIDER
                UNLESS EXPRESSLY AUTHORIZED BY THIS AUTHORIZATION.
              </p>
              <p><span className="me-2" style={{ fontSize: "14 !important" }}>b.</span><span style={{ fontSize: "12 !important", textDecoration: "underline" }}>Opt-Out Option. </span>
                You may withdraw this authorization at any time through our settings page.
              </p>
              <p><strong><span className="me-2">9.</span> Limitation of Liability.</strong> To the extent permitted by applicable law, Leap and its affiliates, subsidiaries, officers, directors,
                employees, agents, partners, and licensors shall not be liable for any direct, indirect, incidental, special, consequential, commercial,
                punitive, or exemplary damages, including but not limited to, damages for loss of profits, revenue, goodwill, use, data,
                electronically transmitted orders, business interruption, or other economic advantage (even if we have been advised of the
                possibility of such damages), however caused and regardless of the theory of liability. This includes damages arising out of or
                related to your use of or inability to use the Product, whether based on warranty, contract, tort (including negligence), or any
                other legal theory, and even if a remedy set forth herein is found to have failed of its essential purpose. In no event shall Leap's
                total liability to you for all damages exceed the amount of fifty dollars ($50.00). Some jurisdictions do not allow the limitation of
                liability for personal injury, or of incidental or consequential damages, so this limitation may not apply to you. The foregoing
                limitations will apply even if the above stated remedy fails of its essential purpose.
              </p>
              <p><strong><span className="me-2">10.</span> Export Compliance.</strong> You may not use, export, import, or transfer the Product except as authorized by U.S. law and the laws of
                the jurisdiction where the Product was obtained. Specifically, the Product must not be without limitation exported or re-exported
                to any U.S.-embargoed countries or to anyone on U.S. government lists of prohibited parties, including but not limited to the
                Specially Designated Nationals, Denied Persons, and Entity Lists. You represent and warrant that you are not located in such a
                country or on any such list. You also agree not to use the Product for any purposes prohibited by U.S. law.
              </p>
              <p><strong><span className="me-2">11.</span>Third Party Beneficiaries.</strong> Apple Inc., Google Inc., and their respective subsidiaries are third-party beneficiaries of this Agreement. Upon your acceptance of this EULA, Apple and Google will have the right (and will be deemed to have accepted the right) to enforce this Agreement against you as a third-party beneficiary thereof. This third-party beneficiary status is limited to claims that these entities may have against you in relation to this Agreement.</p>
              <p><strong><span className="me-2">12.</span>Governing Law and Jurisdiction.</strong> This Agreement shall be governed by and construed in accordance with the laws of the State of New York, without regard to its conflict of law provisions. You agree to submit to the personal jurisdiction of the courts located within the county of New York, New York, for the resolution of all disputes arising from or related to this Agreement and your use of the Product. In addition to the foregoing, your use of the Product may also be subject to other local, state, national or international laws.</p>
              <p><strong><span className="me-2">13.</span>Order of Precedence.</strong> In the event of any discrepancies, conflicts, or contradictions between the provisions of the following documents, the order of precedence shall be as follows: (1) EULA; (2) the Services Agreement, (3) the Terms of Use, (4) the Privacy Policy. The provisions in a document higher in this order of precedence shall supersede and prevail over any conflicting or contradictory provisions in a document lower in this order.</p>
              <p><strong>Acknowledgment & Acceptance. </strong>
                By clicking "I Agree", you acknowledge that you have read, understood, and agree to be bound by
                the terms of this End User License Agreement (EULA), including the Privacy Policy and Terms of Use. If you do not agree to these
                terms, do not proceed with the use of the LEAP application.
              </p>
              <p><strong>Optional Opt-In Authorization (Section 8). </strong>
                By selecting the checkbox below, you authorize us and your healthcare provider to
                provide you with access to personalized health-related content via the Leap platform and to share your protected health
                information as described in Section 8 of this EULA. This authorization is optional and not required to use the application
              </p>
            </p>
          </div>
        </div>
        <form
          className="form-actions"
          action={url.loginAction}
          method="POST"
          onSubmit={(e) => {
            if (!accepted || submitting){ 
              e.preventDefault();
              return;
            }
            setSubmitting(true);
          }}
        >
          <div className="oha-button" style={{ display: "flex", justifyContent: "end", alignItems: "center", gap: "10px" }}>
            {accepted && (
              <input
                type="hidden"
                name="user.attributes.eula_accepted"
                value="true"
              />
            )}
            <input
              className={kcClsx(
                "kcButtonClass",
                "kcButtonPrimaryClass",
                "kcButtonLargeClass"
              )}
              id="kc-accept"
              type="submit"
              value={"I Agree"}
              disabled={!accepted || submitting}
            />
          </div>
        </form>
      </div>
    </Template>
  );
}
