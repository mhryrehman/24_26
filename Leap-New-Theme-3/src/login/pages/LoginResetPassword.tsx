import React, { useState } from "react";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import getStarted from "../../../public/get-started.svg";
export default function LoginResetPassword(props: PageProps<Extract<KcContext, { pageId: "login-reset-password.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;

    const { kcClsx } = getKcClsx({
        doUseDefaultCss,
        classes
    });

    const { url, realm: _realm, auth, messagesPerField } = kcContext;

    const { msg, msgStr } = i18n;
    const [emailError, setEmailError] = useState("");
    const [hideServerError, setHideServerError] = useState(false);
    const [emailInput, setEmailInput] = useState(auth.attemptedUsername ?? "");
    const [submitting, setSubmitting] = useState(false);
    // const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailInput.trim());
    // const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailInput);
    // const canContinue = emailOk;

    const validateEmail = (raw: string) => {
        // const email = raw.trim();
        const email = raw;
        if (!email) return "Required";
        const re = /^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\.)+[A-Za-z]{2,}$/;
        return re.test(email)
            ? ""
            : "Please enter a valid email address.";
    };


    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        setEmailInput(value);
        setHideServerError(true);

        const error = validateEmail(value);
        setEmailError(error);
    };


    const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (submitting) return;
        setSubmitting(true);

        const form = e.currentTarget;
        const email = form.username.value;
        const error = validateEmail(email);

        if (error) {
            setEmailError(error);
            setSubmitting(false);
            return;
        }

        setEmailError("");
        form.submit();
    };
    const canContinue = validateEmail(emailInput) === "";


    if (typeof window !== "undefined") {
        try {
            const target =
                (sessionStorage.getItem("flowEntrypoint") as string) ||
                (kcContext.url.loginRestartFlowUrl as string) ||
                "/";

            const nav = (performance.getEntriesByType?.("navigation")[0] || {}) as any;
            const cameFromBack = nav?.type === "back_forward";
            const fromloginReset = document.referrer && /\/login-reset/i.test(document.referrer);

            if (cameFromBack || fromloginReset) {
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
            displayInfo
            displayMessage={false}
            headerNode={msg("emailForgotTitle")}
        >
            <div className="auth-section form">
                <div className="auth-wrapper">
                    <div className="mobile-screen-icon">
                        <img src={getStarted} alt="" />
                    </div>
                    <div className="auth-header with-subtitle">Forgot Password</div>
                    <div className="auth-subheader">
                        Enter the email linked to your Leap account, and we’ll send you a link to reset your password.
                    </div>
                    <form id="kc-reset-password-form" action={url.loginAction} method="post" onSubmit={handleSubmit}>
                        <div className="space-input-bottom position-relative">
                            <label htmlFor="username" className="col-form-label">
                                Email
                            </label>
                            <input
                                type="text"
                                id="username"
                                name="username"
                                className={emailError || messagesPerField.existsError("username") ? "auth-input input-error mb-0" : "auth-input mb-0"}
                                maxLength={75}
                                autoFocus
                                onChange={handleChange}

                                value={emailInput}
                                defaultValue={auth.attemptedUsername ?? ""}
                                aria-invalid={!!(emailError || messagesPerField.existsError("username"))}
                            />

                            {/* {!hideServerError && (emailError || messagesPerField.existsError("username")) && ( */}
                            {(emailError || (!hideServerError && messagesPerField.existsError("username"))) && (

                                <span
                                    id="input-error-username"
                                    className={kcClsx("kcInputErrorMessageClass")}
                                    aria-live="polite"
                                >
                                    <div className="align-self-center error-icon-style">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 17 18" fill="none">
                                            <path d="M7.66797 11.5003H9.33464V13.167H7.66797V11.5003ZM7.66797 4.83366H9.33464V9.83366H7.66797V4.83366ZM8.49297 0.666992C3.89297 0.666992 0.167969 4.40033 0.167969 9.00033C0.167969 13.6003 3.89297 17.3337 8.49297 17.3337C13.1013 17.3337 16.8346 13.6003 16.8346 9.00033C16.8346 4.40033 13.1013 0.666992 8.49297 0.666992ZM8.5013 15.667C4.81797 15.667 1.83464 12.6837 1.83464 9.00033C1.83464 5.31699 4.81797 2.33366 8.5013 2.33366C12.1846 2.33366 15.168 5.31699 15.168 9.00033C15.168 12.6837 12.1846 15.667 8.5013 15.667Z" fill="#FF0000" />
                                        </svg>
                                    </div>
                                    <span className="ml-4">
                                        {kcSanitize(emailError || messagesPerField.get("username"))}
                                    </span>
                                </span>
                            )}
                        </div>
                        <div>
                            <div id="kc-form-buttons">
                                <button tabIndex={7} className="btn leap-button w-100 mb-3" name="Next" type="submit" value={msgStr("doSubmit")} disabled={!canContinue}>
                                    Continue
                                </button>
                                <button
                                    type="button"
                                    className="btn leap-button-outline w-100 mb-3"
                                    name="backToLogin"
                                    onClick={() => {
                                        // if(submitting)
                                        //     return;
                                        // setSubmitting(true); //new line to block the submission
                                        window.location.href = url.loginUrl
                                    }}
                                >
                                    Back to Login
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </Template>
    );
}
