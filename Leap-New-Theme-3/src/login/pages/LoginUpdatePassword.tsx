import type { JSX } from "keycloakify/tools/JSX";
import { useIsPasswordRevealed } from "keycloakify/tools/useIsPasswordRevealed";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import { getKcClsx, type KcClsx } from "keycloakify/login/lib/kcClsx";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import { useEffect, useMemo, useState } from "react";
export default function LoginUpdatePassword(props: PageProps<Extract<KcContext, { pageId: "login-update-password.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;

    const { kcClsx } = getKcClsx({
        doUseDefaultCss,
        classes
    });

    const { msg, msgStr } = i18n;

    const { url, messagesPerField, isAppInitiatedAction } = kcContext;
    const [pwd, setPwd] = useState("");
    const [confirmPwd, setConfirmPwd] = useState("");
    const [triedSubmit, setTriedSubmit] = useState(false);
    const [hasPasswordFocused, setHasPasswordFocused] = useState(false);
    const [showToast, setShowToast] = useState(false);
    const [toasterMessage, setToasterMessage] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);


    const [mismatch, setMismatch] = useState(false);
    const [passwordCriteriaState, setPasswordCriteriaState] = useState({
        minLength: false,
        upperCase: false,
        lowerCase: false,
        number: false,
        symbol: false,
    });
    const message = (kcContext as any)?.message as
        | { type?: "success" | "error"; summary?: string }
        | undefined;



    useEffect(() => {
        if (!message) {
            setShowToast(false);
            setToasterMessage(null);
            return;
        }
        const text = message.summary ?? null;
        setShowToast(Boolean(text));
        setToasterMessage(text);
    }, [message]);

    const strongEnough = useMemo(() => {
        const { minLength, upperCase, lowerCase, number, symbol } = passwordCriteriaState;
        return minLength && upperCase && lowerCase && number && symbol;
    }, [passwordCriteriaState]);
    const passwordsMatch = pwd !== "" && pwd === confirmPwd;
    const showStrengthError = triedSubmit && !strongEnough;
    const isPwdEmpty = triedSubmit && pwd.trim() === "";
    const isConfirmEmpty = triedSubmit && confirmPwd.trim() === "";
    const isFormValid = pwd.trim().length > 0 && confirmPwd.trim().length > 0 && strongEnough && passwordsMatch;

    const PasswordRequirement = ({
        label,
        isValid,
    }: { label: string; isValid: boolean }) => (
        <div style={{ color: isValid ? "#1ab34dff" : "#1B2228", marginBottom: "4px", fontSize: "16px", lineHeight: "22px", fontWeight: "400" }}>
            <span style={{ marginRight: 8 }}>
                <svg
                    width="20"
                    height="21"
                    viewBox="0 0 20 21"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg">
                    <path
                        d="M9.9974 2.16602C5.3974 2.16602 1.66406 5.89935 1.66406 10.4993C1.66406 15.0993 5.3974 18.8327 9.9974 18.8327C14.5974 18.8327 18.3307 15.0993 18.3307 10.4993C18.3307 5.89935 14.5974 2.16602 9.9974 2.16602ZM9.9974 17.166C6.3224 17.166 3.33073 14.1743 3.33073 10.4993C3.33073 6.82435 6.3224 3.83268 9.9974 3.83268C13.6724 3.83268 16.6641 6.82435 16.6641 10.4993C16.6641 14.1743 13.6724 17.166 9.9974 17.166ZM13.2307 7.40768L8.33073 12.3077L6.76406 10.741C6.43906 10.416 5.91406 10.416 5.58906 10.741C5.26406 11.066 5.26406 11.591 5.58906 11.916L7.7474 14.0744C8.0724 14.3994 8.5974 14.3994 8.9224 14.0744L14.4141 8.58268C14.7391 8.25768 14.7391 7.73268 14.4141 7.40768C14.0891 7.08268 13.5557 7.08268 13.2307 7.40768Z"
                        fill={isValid ? "#1ab34dff" : "#555555"}
                    />
                </svg>
            </span>
            {label}
        </div>
    );

    const onSubmit = (e: React.FormEvent<HTMLFormElement>) => {
        const active = document.activeElement as HTMLButtonElement | null;
        if (active?.name === "cancel-aia") return;
        if (submitting) { e.preventDefault(); return; }
        setSubmitting(true);
        setTriedSubmit(true);

        const notMatching = !passwordsMatch;
        const emptyFields = pwd.trim() === "" || confirmPwd.trim() === "";

        setMismatch(notMatching);

        if (emptyFields || notMatching || !strongEnough) {
            e.preventDefault();
            setSubmitting(false);
            return;
        }

        (e.currentTarget as HTMLFormElement).submit();
    };

    if (typeof window !== "undefined") {
        try {
            const target = (sessionStorage.getItem("flowEntrypoint") as string) || (kcContext.url.loginRestartFlowUrl as string) || "/";
            const nav = (performance.getEntriesByType?.("navigation")[0] || {}) as any;
            const cameFromBack = nav?.type === "back_forward";
            const fromloginUpdate = document.referrer && /\/login-update/i.test(document.referrer);
            
            if (cameFromBack || fromloginUpdate) {
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
        <>
            {showToast && toasterMessage ? (

                <div className="notification">
                    <div className="notification-icon">
                        {message?.type === "success" ? (
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
                                <path d="M19.7988 10.1113C19.7988 15.4616 15.4616 19.7988 10.1113 19.7988C4.76105 19.7988 0.423828 15.4616 0.423828 10.1113C0.423828 4.76106 4.76105 0.423828 10.1113 0.423828C15.4616 0.423828 19.7988 4.76106 19.7988 10.1113ZM8.99078 15.2408L16.1783 8.05328C16.4223 7.80922 16.4223 7.41348 16.1783 7.16941L15.2944 6.28555C15.0504 6.04145 14.6546 6.04145 14.4105 6.28555L8.54883 12.1472L5.81215 9.41051C5.56809 9.16645 5.17234 9.16645 4.92824 9.41051L4.04437 10.2944C3.80031 10.5384 3.80031 10.9342 4.04437 11.1782L8.10688 15.2407C8.35098 15.4848 8.74668 15.4848 8.99078 15.2408Z" fill="#14AE5C" />
                            </svg>
                        ) : (
                            <svg width="20" height="20" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M9.0013 0.666992C4.4013 0.666992 0.667969 4.40033 0.667969 9.00033C0.667969 13.6003 4.4013 17.3337 9.0013 17.3337C13.6013 17.3337 17.3346 13.6003 17.3346 9.00033C17.3346 4.40033 13.6013 0.666992 9.0013 0.666992ZM9.83464 13.167H8.16797V11.5003H9.83464V13.167ZM9.83464 9.83366H8.16797V4.83366H9.83464V9.83366Z" fill="#d65745"></path>
                            </svg>
                        )}
                    </div>
                    <div className="notification-text">{toasterMessage}</div>
                    <button className="close-button" onClick={() => {
                        setShowToast(false);
                        setToasterMessage(null);
                    }}>
                        <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M1 1L6 6M11 11L6 6M6 6L11 1L1 11" stroke="#B3B3B3" stroke-width="2" />
                        </svg>
                    </button>
                </div>
            ) : null}
            <Template
                kcContext={kcContext}
                i18n={i18n}
                doUseDefaultCss={doUseDefaultCss}
                classes={classes}
                displayMessage={false}
                headerNode={msg("updatePasswordTitle")}
            >
                <div className="auth-section form">
                    <div className="auth-wrapper">
                        <div className="auth-header with-subtitle">Reset Password</div>
                        <div className="auth-subheader">Enter a new password. We'll ask for this password <br></br>
                            whenever you login</div>
                        <form id="kc-passwd-update-form" action={url.loginAction} method="post" onSubmit={onSubmit}>
                            <div className="space-input">
                                <label htmlFor="password-new" className="col-form-label">
                                    {msg("passwordNew")}
                                </label>
                                <div>
                                    <PasswordWrapper kcClsx={kcClsx} i18n={i18n} passwordInputId="password-new">
                                        <input
                                            type="password"
                                            id="password-new"
                                            name="password-new"
                                            className={
                                                messagesPerField.existsError("password", "password-confirm") || showStrengthError ? "auth-input input-error mb-0" : "auth-input mb-0"
                                            }
                                            autoFocus
                                            autoComplete="new-password"
                                            maxLength={20}
                                            value={pwd}
                                            onChange={e => {
                                                const v = e.target.value;
                                                setPwd(v);
                                                setMismatch(v !== "" && v !== confirmPwd);
                                                setPasswordCriteriaState({
                                                    minLength: v.length >= 8,   
                                                    upperCase: /[A-Z]/.test(v),
                                                    lowerCase: /[a-z]/.test(v),
                                                    number: /\d/.test(v),
                                                    symbol: /[^A-Za-z0-9]/.test(v),
                                                });
                                            }}
                                            onFocus={() => setHasPasswordFocused(true)}
                                            onCopy={(e) => e.preventDefault()}
                                            onCut={(e) => e.preventDefault()}
                                            onPaste={(e) => e.preventDefault()}
                                            onDragStart={(e) => e.preventDefault()}
                                            aria-invalid={
                                                messagesPerField.existsError("password", "password-confirm") || mismatch
                                            }
                                        />
                                    </PasswordWrapper>

                                    {messagesPerField.existsError("password") && (
                                        <span
                                            id="input-error-password"
                                            className={kcClsx("kcInputErrorMessageClass")}
                                            aria-live="polite"
                                        >
                                            <div className="align-self-center error-icon-style">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 17 18" fill="none">
                                                <path d="M7.66797 11.5003H9.33464V13.167H7.66797V11.5003ZM7.66797 4.83366H9.33464V9.83366H7.66797V4.83366ZM8.49297 0.666992C3.89297 0.666992 0.167969 4.40033 0.167969 9.00033C0.167969 13.6003 3.89297 17.3337 8.49297 17.3337C13.1013 17.3337 16.8346 13.6003 16.8346 9.00033C16.8346 4.40033 13.1013 0.666992 8.49297 0.666992ZM8.5013 15.667C4.81797 15.667 1.83464 12.6837 1.83464 9.00033C1.83464 5.31699 4.81797 2.33366 8.5013 2.33366C12.1846 2.33366 15.168 5.31699 15.168 9.00033C15.168 12.6837 12.1846 15.667 8.5013 15.667Z" fill="#FF0000" />
                                            </svg>
                                            </div>
                                            <span className="ml-4">
                                                {kcSanitize(messagesPerField.get("password"))}
                                            </span>
                                        </span>
                                    )}
                                    {isPwdEmpty && (
                                        <span
                                            id="input-error-password-required"
                                            className={kcClsx("kcInputErrorMessageClass")}
                                            aria-live="polite"
                                        >
                                            Required
                                        </span>
                                    )}

                                </div>
                            </div>
                            <div className="space-input-bottom">
                                <label htmlFor="password-confirm" className="col-form-label">
                                    {msg("passwordConfirm")}
                                </label>
                                <div>
                                    <PasswordWrapper kcClsx={kcClsx} i18n={i18n} passwordInputId="password-confirm">
                                        <input
                                            type="password"
                                            id="password-confirm"
                                            name="password-confirm"
                                            className={
                                                messagesPerField.existsError("password", "password-confirm") || showStrengthError ? "auth-input input-error" : "auth-input"
                                            }
                                            autoComplete="new-password"
                                            value={confirmPwd}
                                            maxLength={20}
                                            onChange={e => {
                                                const v = e.target.value;
                                                setConfirmPwd(v);
                                                setMismatch(pwd !== "" && pwd !== v);
                                            }}
                                            onCopy={(e) => e.preventDefault()}
                                            onCut={(e) => e.preventDefault()}
                                            onPaste={(e) => e.preventDefault()}
                                            onDragStart={(e) => e.preventDefault()}
                                            aria-invalid={
                                                messagesPerField.existsError("password", "password-confirm") || mismatch
                                            }
                                        />
                                    </PasswordWrapper>
                                    <span className={kcClsx("kcInputErrorMessageClass")} aria-live="polite">
                                        {isConfirmEmpty
                                            ? "Required"
                                            : (triedSubmit || confirmPwd.length > 0) && !passwordsMatch
                                                ? "Passwords do not match."
                                                : !messagesPerField.existsError("password") && showStrengthError
                                                    ? "Password doesn't meet the complexity requirements."
                                                    : ""}
                                    </span>
                                    {hasPasswordFocused && (
                                        <div className="passHint">
                                            <div className="password-title">Password must meet the following conditions:</div>
                                            <PasswordRequirement
                                                label="Must be 8 or more characters"
                                                isValid={passwordCriteriaState.minLength}

                                            />
                                            <PasswordRequirement
                                                label="Must contain alphabets and numbers"
                                                isValid={(passwordCriteriaState.upperCase || passwordCriteriaState.lowerCase) && passwordCriteriaState.number}
                                            />
                                            <PasswordRequirement
                                                label="Must include uppercase and lowercase letters"
                                                isValid={passwordCriteriaState.lowerCase && passwordCriteriaState.upperCase}
                                            />
                                            <PasswordRequirement
                                                label="Must contain a special character"
                                                isValid={passwordCriteriaState.symbol}
                                            />
                                        </div>
                                    )}

                                </div>
                            </div>
                            <div>
                                <LogoutOtherSessions kcClsx={kcClsx} i18n={i18n} />
                                <div id="kc-form-buttons">
                                    <button className="btn leap-button w-100" type="submit" value={msgStr("doSubmit")} disabled={!isFormValid || submitting}>
                                        Reset Password
                                    </button>
                                    {isAppInitiatedAction && (
                                        <button className="btn leap-button w-100" type="submit" name="cancel-aia" value="true" disabled={submitting}>
                                            {msg("doCancel")}
                                        </button>
                                    )}
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </Template>
        </>
    );
}

function LogoutOtherSessions(props: { kcClsx: KcClsx; i18n: I18n }) {
    const { kcClsx, i18n } = props;

    const { msg } = i18n;

    return true ? (
        <></>
    ) : (
        <div id="kc-form-options" className={kcClsx("kcFormOptionsClass")}>
            <div className={kcClsx("kcFormOptionsWrapperClass")}>
                <div className="checkbox">
                    <label>
                        <input type="checkbox" id="logout-sessions" name="logout-sessions" value="on" defaultChecked={true} />
                        {msg("logoutOtherSessions")}
                    </label>
                </div>
            </div>
        </div>
    );
}

function PasswordWrapper(props: { kcClsx: KcClsx; i18n: I18n; passwordInputId: string; children: JSX.Element }) {
    const { kcClsx: _kcClsx, i18n, passwordInputId, children } = props;

    const { msgStr } = i18n;

    const { isPasswordRevealed, toggleIsPasswordRevealed } = useIsPasswordRevealed({ passwordInputId });


    return (
        <div className="showHidePassword">
            {children}
            <button
                type="button"
                aria-label={msgStr(isPasswordRevealed ? "hidePassword" : "showPassword")}
                aria-controls={passwordInputId}
                onClick={toggleIsPasswordRevealed}
            >
                <span aria-hidden="true" className="login-update-eye-icon">
                    {isPasswordRevealed ? (
                        <svg style={{width:"100%", height:"100%"}}
                            width="20"
                            height="13"
                            viewBox="0 0 20 13"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg"
                        >
                            <path d="M9.99967 1.91667C13.158 1.91667 15.9747 3.69167 17.3497 6.5C15.9747 9.30833 13.1663 11.0833 9.99967 11.0833C6.83301 11.0833 4.02467 9.30833 2.64967 6.5C4.02467 3.69167 6.84134 1.91667 9.99967 1.91667ZM9.99967 0.25C5.83301 0.25 2.27467 2.84167 0.833008 6.5C2.27467 10.1583 5.83301 12.75 9.99967 12.75C14.1663 12.75 17.7247 10.1583 19.1663 6.5C17.7247 2.84167 14.1663 0.25 9.99967 0.25ZM9.99967 4.41667C11.1497 4.41667 12.083 5.35 12.083 6.5C12.083 7.65 11.1497 8.58333 9.99967 8.58333C8.84967 8.58333 7.91634 7.65 7.91634 6.5C7.91634 5.35 8.84967 4.41667 9.99967 4.41667ZM9.99967 2.75C7.93301 2.75 6.24967 4.43333 6.24967 6.5C6.24967 8.56667 7.93301 10.25 9.99967 10.25C12.0663 10.25 13.7497 8.56667 13.7497 6.5C13.7497 4.43333 12.0663 2.75 9.99967 2.75Z" fill="#555555" />
                        </svg>
                    ) : (
                        <svg style={{width:"100%", height:"100%"}}
                            width="20"
                            height="17"
                            viewBox="0 0 20 17"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg"
                        >
                            <path d="M9.99967 3.47915C13.158 3.47915 15.9747 5.25415 17.3497 8.06248C16.858 9.07915 16.1663 9.95415 15.3413 10.6625L16.5163 11.8375C17.6747 10.8125 18.5913 9.52915 19.1663 8.06248C17.7247 4.40415 14.1663 1.81248 9.99967 1.81248C8.94134 1.81248 7.92467 1.97915 6.96634 2.28748L8.34134 3.66248C8.88301 3.55415 9.43301 3.47915 9.99967 3.47915ZM9.10801 4.42915L10.833 6.15415C11.308 6.36248 11.6913 6.74581 11.8997 7.22081L13.6247 8.94581C13.6913 8.66248 13.7413 8.36248 13.7413 8.05415C13.7497 5.98748 12.0663 4.31248 9.99967 4.31248C9.69134 4.31248 9.39967 4.35415 9.10801 4.42915ZM1.67467 1.70415L3.90801 3.93748C2.54967 5.00415 1.47467 6.42081 0.833008 8.06248C2.27467 11.7208 5.83301 14.3125 9.99967 14.3125C11.2663 14.3125 12.483 14.0708 13.5997 13.6291L16.4497 16.4791L17.6247 15.3041L2.84967 0.520813L1.67467 1.70415ZM7.92467 7.95415L10.0997 10.1291C10.0663 10.1375 10.033 10.1458 9.99967 10.1458C8.84967 10.1458 7.91634 9.21248 7.91634 8.06248C7.91634 8.02081 7.92467 7.99581 7.92467 7.95415ZM5.09134 5.12081L6.54967 6.57915C6.35801 7.03748 6.24967 7.53748 6.24967 8.06248C6.24967 10.1291 7.93301 11.8125 9.99967 11.8125C10.5247 11.8125 11.0247 11.7041 11.4747 11.5125L12.2913 12.3291C11.558 12.5291 10.7913 12.6458 9.99967 12.6458C6.84134 12.6458 4.02467 10.8708 2.64967 8.06248C3.23301 6.87081 4.08301 5.88748 5.09134 5.12081Z" fill="#555555" />
                        </svg>
                    )}
                </span>
            </button>
        </div>
    );
}
