import type { JSX } from "keycloakify/tools/JSX";
import { useEffect, useRef, useState } from "react";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import { useIsPasswordRevealed } from "keycloakify/tools/useIsPasswordRevealed";
import { clsx } from "keycloakify/tools/clsx";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import { getKcClsx, type KcClsx } from "keycloakify/login/lib/kcClsx";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function Login(props: PageProps<Extract<KcContext, { pageId: "login.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;

    const { kcClsx } = getKcClsx({
        doUseDefaultCss,
        classes
    });

    const { social, realm, url, usernameHidden, login, auth, registrationDisabled, messagesPerField, message } = kcContext;

    const { msg, msgStr } = i18n;

    const [isLoginButtonDisabled, setIsLoginButtonDisabled] = useState(false);
    const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
    const [showProfileContext, setShowProfileContext] = useState(false);
    const [showToast, setShowToast] = useState(false);
    const [toasterMessage, setToasterMessage] = useState<string | null>(null);
    const emailRef = useRef<HTMLInputElement>(null);
    const passRef = useRef<HTMLInputElement>(null);
    type AppointmentProfile = {
        firstName: string;
        lastName: string;
        suffix?: string;
        designation?: string;
        whenISO?: string;
        tzLabel?: string;
        visitType?: string;
        reason?: string;
        photoUrl?: string;
    };

    const [profileMode, setProfileMode] =
        useState<"activationCode" | "appointment" | "basic">("basic");
    const [appointmentProfile, setAppointmentProfile] = useState<AppointmentProfile>({
        firstName: "Naz",
        lastName: "Kleiman",
        suffix: "MD",
        designation: "Primary Care Doctor",
        whenISO: "2025-08-08T20:00:00-05:00",
        tzLabel: "EST",
        visitType: "Video Visit",
        reason: "Illness",
        photoUrl:
            "",
    });

    useEffect(() => {
        //To handle appointment case, such that if user loads appointment case then reloads the link the appointment case should not be there anymore if user initites from the beginning
        var path = window.location.pathname.replace(/\/+$/, "").toLowerCase();
        if (path === "/auth" || path.endsWith("/auth")) {
            const val = sessionStorage.getItem("RegisterState");
            if (val !== null) {
                sessionStorage.removeItem("RegisterState");
            }
        }

        const urlParams = new URLSearchParams(window.location.search);
        const fromUrl = urlParams.get("state");
        const fromStore = sessionStorage.getItem("RegisterState");
        const rawState = fromUrl ?? fromStore ?? null;
        if (fromUrl && fromUrl !== fromStore) {
            sessionStorage.setItem("RegisterState", fromUrl);
        }
        const signup_url = urlParams.get("signup_url");
        const signupUrlStore = sessionStorage.getItem("SignupUrl");
        if (signup_url && signup_url !== signupUrlStore) sessionStorage.setItem("SignupUrl", signup_url);

        let gotProfileFromState = false;

        if (rawState) {
            try {
                const decoded = decodeURIComponent(atob(rawState));
                const parsed = JSON.parse(decoded);

                if (parsed.profileMode) {
                    setProfileMode(parsed.profileMode);
                }

                if (parsed.profileMode === "appointment" && parsed.appointmentProfile) {
                    setAppointmentProfile(parsed.appointmentProfile as AppointmentProfile);
                    gotProfileFromState = true;
                }

            } catch {
                // ignore parse errors
            }
        }
        setShowProfileContext(gotProfileFromState);

    }, []);

    useEffect(() => {
        if (typeof window === "undefined") return;
        [
            "otpValues",
            "otpStartTime",
            "otpFormSubmitted",
            "otpErrorOccurred",
            "maxAttemptsReached",
            "otpResendCount",
            "otpFlowId",
            "resendStartTime",
            "stickyOtpError",
        ].forEach(k => sessionStorage.removeItem(k));
    }, []);

    useEffect(() => {
        var path = window.location.pathname.replace(/\/+$/, "").toLowerCase();
        if (path === "/auth" || path.endsWith("/auth")) {
            if (sessionStorage.getItem("kc_flow") != "login") {
                sessionStorage.setItem("kc_flow", "login");
                if (sessionStorage.getItem("flowEntrypointLogin") == null)
                    sessionStorage.setItem("flowEntrypointLogin", window.location.href);
                const loginEntryPoint = sessionStorage.getItem("flowEntrypointLogin");
                if (loginEntryPoint)
                    sessionStorage.setItem("flowEntrypoint", loginEntryPoint);
                else {
                    sessionStorage.setItem("flowEntrypoint", window.location.href);
                }
            }
        }
    }, []);


    useEffect(() => {
        if (!message) {
            setToasterMessage(null);
            setShowToast(false)
            return;
        }

        // Build the text you want to show
        let text = message.summary ?? null;
        if (message.type !== "success") {
            const raw = sessionStorage.getItem("registerObject");
            if (raw) {
                try {
                    const { isRegisterFlow } = JSON.parse(raw);
                    if (isRegisterFlow && (text ?? "").toLowerCase().includes("login attempt timed out")) {
                        text = "Your signup attempt timed out. Signup will start from the beginning";
                        sessionStorage.removeItem("registerObject");
                    }
                } catch { /* ignore parse errors */ }
            }
        }

        setToasterMessage(text);
        setShowToast(Boolean(text));
    }, [message]);


    function ProfileContext() {
        // helpers
        const fullName = (f?: string, l?: string) =>
            [f, l].filter(Boolean).join(" ").trim() || "—";

        const initials = (f?: string, l?: string) =>
            [f?.[0], l?.[0]].filter(Boolean).join("").toUpperCase() || "•";

        const formatWhen = (iso?: string, tzLabel?: string) => {
            if (!iso) return "—";
            const dt = new Date(iso);
            if (isNaN(dt.getTime())) return "—";
            const datePart = dt.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });
            const timePart = dt.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
            return `${datePart} at ${timePart}${tzLabel ? ` ${tzLabel}` : ""}`;
        };


        // ----- Appointment card -----
        if (profileMode === "appointment") {
            const a = appointmentProfile;
            const nameWithSuffix = [fullName(a.firstName, a.lastName), a.suffix].filter(Boolean).join(", ");

            return (
                <div
                    className="profile-context mb-3"
                    style={{
                        display: "flex", alignItems: "start", gap: 20,
                        padding: "12px", border: "1px solid #EAEBEF",
                        borderRadius: 10, background: "#F9FAFB",
                    }}
                >
                    {a.photoUrl ? (
                        <img
                            src={a.photoUrl}
                            alt={nameWithSuffix || "Avatar"}
                            style={{ minWidth: 60, maxWidth: 60, maxHeight: 60, minHeight: 60, borderRadius: 50, objectFit: "cover" }}
                        />
                    ) : (
                        <div className="user-profile-pic"
                            aria-hidden
                            style={{
                                minWidth: 60, maxWidth: 60, maxHeight: 60, minHeight: 60, borderRadius: 50, background: "#F3F2FF",
                                display: "flex", alignItems: "center", justifyContent: "center",
                                fontWeight: 700,
                            }}
                        >
                            {initials(a.firstName, a.lastName)}
                        </div>
                    )}

                    <div style={{ display: "flex", flexDirection: "column", lineHeight: 1.25 }}>
                        <div className="user-name" style={{ fontWeight: 700, fontSize: 18, marginBottom: 4, color: "#1B2228" }}>{nameWithSuffix}</div>
                        {a.designation && (
                            <div className="other-info" style={{ fontSize: 16, fontWeight: 500, color: "#666666", marginBottom: 4 }}>{a.designation}</div>
                        )}

                        <div className="other-info" style={{ marginBottom: 4, fontSize: 16, fontWeight: 400, color: "#1B2228" }}>
                            {formatWhen(a.whenISO, a.tzLabel)}
                        </div>

                        {a.visitType && (
                            <div style={{ marginBottom: 4, fontSize: 16, fontWeight: 600, color: "#1B2228", display: "flex", alignItems: "center" }}>
                                <span className="other-info" style={{ marginRight: 6 }}>
                                    <svg width="21" height="21" viewBox="0 0 21 21" fill="none" xmlns="http://www.w3.org/2000/svg">
                                        <path d="M13.125 8.75L17.1089 6.7585C17.2422 6.69186 17.3904 6.66039 17.5394 6.6671C17.6883 6.67381 17.8331 6.71847 17.9599 6.79683C18.0867 6.87519 18.1915 6.98467 18.2641 7.11486C18.3367 7.24506 18.3749 7.39166 18.375 7.54075V13.4592C18.3749 13.6083 18.3367 13.7549 18.2641 13.8851C18.1915 14.0153 18.0867 14.1248 17.9599 14.2032C17.8331 14.2815 17.6883 14.3262 17.5394 14.3329C17.3904 14.3396 17.2422 14.3081 17.1089 14.2415L13.125 12.25V8.75ZM2.625 7C2.625 6.53587 2.80937 6.09075 3.13756 5.76256C3.46575 5.43437 3.91087 5.25 4.375 5.25H11.375C11.8391 5.25 12.2842 5.43437 12.6124 5.76256C12.9406 6.09075 13.125 6.53587 13.125 7V14C13.125 14.4641 12.9406 14.9092 12.6124 15.2374C12.2842 15.5656 11.8391 15.75 11.375 15.75H4.375C3.91087 15.75 3.46575 15.5656 3.13756 15.2374C2.80937 14.9092 2.625 14.4641 2.625 14V7Z" stroke="#765AEB" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
                                    </svg>
                                </span>
                                <span className="other-info">{a.visitType}</span>
                            </div>
                        )}

                        {a.reason && (
                            <div style={{ fontSize: 14, color: "#374151" }}>
                                {a.reason}
                            </div>
                        )}
                    </div>
                </div>
            );
        }
    }
    //Profile Context

    const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const form = e.currentTarget;
        const email = (form.username?.value || "").trim();
        const pwd = (form.password?.value || "").trim();
        // const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        const emailPattern = /^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\.)+[A-Za-z]{2,}$/;


        const newErrors: { email?: string; password?: string } = {};

        if (!email) {
            newErrors.email = "Required";
        } else if (!emailPattern.test(email)) {
            newErrors.email = "Please enter a valid email address.";
        }

        if (!pwd) {
            newErrors.password = "Required";
        }

        setErrors(newErrors);

        if (Object.keys(newErrors).length > 0) {
            setIsLoginButtonDisabled(false);
            return; // don’t submit
        }

        setIsLoginButtonDisabled(true);
        form.submit();
    };

    const clearFieldError = (field: "email" | "password") =>
        setErrors(prev => ({ ...prev, [field]: undefined }));

    const validateField = (field: "email" | "password", value: string) => {
        const newErrors: typeof errors = {};
        if (field === "email") {
            if (!value.trim()) newErrors.email = "Required";
            else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()))
                newErrors.email = "Please enter a valid email address.";
        }
        if (field === "password") {
            if (!value.trim()) newErrors.password = "Required";
        }
        setErrors(prev => ({ ...prev, ...newErrors }));
    };


    const [email, setEmail] = useState(kcContext.login.username ?? "");
    const [password, setPassword] = useState("");
    const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
    let canContinue = emailOk && (password.trim().length > 0);
    canContinue = true;

    if (typeof window !== "undefined") {
        try {
            const target =
                (sessionStorage.getItem("flowEntrypoint") as string) ||
                (kcContext.url.loginRestartFlowUrl as string) ||
                "/";

            const nav = (performance.getEntriesByType?.("navigation")[0] || {}) as any;
            const cameFromBack = nav?.type === "back_forward";
            if (cameFromBack) {
                const loc = window.top && window.top !== window ? window.top.location : window.location;
                loc.replace(target);
                return null as any;
            }

            window.addEventListener("pageshow", (e) => {
                if ((e as PageTransitionEvent).persisted) {
                    const loc = window.top && window.top !== window ? window.top.location : window.location;
                    loc.replace(target);
                }
            })
        } catch {
            /* ignore */
        }

    }

    return (
        <>
            {showToast && toasterMessage && !messagesPerField.existsError("username", "password") ? (

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
                    <div className={`notification-text ${message?.type !== "success" ? "" : ""}`}>{toasterMessage}</div>
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
                headerNode={msg("loginAccountTitle")}
                displayInfo={realm.password && realm.registrationAllowed && !registrationDisabled}
                infoNode={
                    false ? (
                        <div id="kc-registration-container">
                            <div id="kc-registration">
                                <span>
                                    {"Don't have an account?"}{" "}
                                    <a href={url.registrationUrl}>
                                        {msg("doRegister")}
                                    </a>
                                </span>
                            </div>
                        </div>
                    ) : (
                        <></>
                    )
                }
                socialProvidersNode={
                    false ? (
                        <>
                            {realm.password && social?.providers !== undefined && (social?.providers?.length ?? 0) !== 0 && (
                                <div id="kc-social-providers" className={kcClsx("kcFormSocialAccountSectionClass")}>
                                    <hr />
                                    <h2>{msg("identity-provider-login-label")}</h2>
                                    <ul
                                        className={kcClsx(
                                            "kcFormSocialAccountListClass",
                                            (social?.providers?.length ?? 0) > 3 && "kcFormSocialAccountListGridClass"
                                        )}
                                    >
                                        {social?.providers?.map((...[p, , providers]) => (
                                            <li key={p.alias}>
                                                <a
                                                    id={`social-${p.alias}`}
                                                    className={kcClsx(
                                                        "kcFormSocialAccountListButtonClass",
                                                        providers.length > 3 && "kcFormSocialAccountGridItem"
                                                    )}
                                                    type="button"
                                                    href={p.loginUrl}
                                                >
                                                    {p.iconClasses && (
                                                        <i className={clsx(kcClsx("kcCommonLogoIdP"), p.iconClasses)} aria-hidden="true"></i>
                                                    )}
                                                    <span
                                                        className={clsx(kcClsx("kcFormSocialAccountNameClass"), p.iconClasses && "kc-social-icon-text")}
                                                        dangerouslySetInnerHTML={{ __html: kcSanitize(p.displayName) }}
                                                    ></span>
                                                </a>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </>
                    ) : (
                        <></>
                    )
                }
            >
                <div className="auth-section login form">
                    <div className="auth-wrapper">
                        {showProfileContext ? <ProfileContext /> : null}
                        <div className="d-flex">
                            <div className="auth-log">
                                <div className="auth-header " style={{ fontSize: "30 !important" }}>Log in</div>
                                {messagesPerField.existsError("username", "password") && (
                                    <div className="invalid-credentials">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 17 18" fill="none">
                                            <path d="M7.66797 11.5003H9.33464V13.167H7.66797V11.5003ZM7.66797 4.83366H9.33464V9.83366H7.66797V4.83366ZM8.49297 0.666992C3.89297 0.666992 0.167969 4.40033 0.167969 9.00033C0.167969 13.6003 3.89297 17.3337 8.49297 17.3337C13.1013 17.3337 16.8346 13.6003 16.8346 9.00033C16.8346 4.40033 13.1013 0.666992 8.49297 0.666992ZM8.5013 15.667C4.81797 15.667 1.83464 12.6837 1.83464 9.00033C1.83464 5.31699 4.81797 2.33366 8.5013 2.33366C12.1846 2.33366 15.168 5.31699 15.168 9.00033C15.168 12.6837 12.1846 15.667 8.5013 15.667Z" fill="#FF0000" />
                                        </svg>
                                        <div className="invalid-text">Invalid username or password</div>
                                    </div>
                                )}

                                <form
                                    id="kc-form-login"
                                    onSubmit={handleSubmit}
                                    action={url.loginAction}
                                    method="post"
                                >
                                    <div className="space-input">
                                        <label className="col-form-label">Email</label>
                                        <input
                                            id="username"
                                            aria-label="username"
                                            ref={emailRef}
                                            className={(errors.email || messagesPerField.existsError("username", "password")) ? "auth-input input-error" : "auth-input"}
                                            name="username"
                                            type="text"
                                            maxLength={75}
                                            onInput={(e) => setEmail(e.currentTarget.value)}
                                            onChange={(e) => {
                                                setEmail(e.target.value);
                                                if (errors.email) clearFieldError("email");
                                            }}
                                            onBlur={(e) => validateField("email", e.currentTarget.value)}
                                            autoFocus
                                            aria-invalid={messagesPerField.existsError("username", "password")}
                                        />
                                        {errors.email && (
                                            // <span id="input-error-email-required" className={kcClsx("kcInputErrorMessageClass")} aria-live="polite" >{errors.email}</span>
                                              <span
                                                id="input-error-password-required"
                                                className={kcClsx("kcInputErrorMessageClass")}
                                                aria-live="polite"
                                            >
                                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 17 18" fill="none">
                                                    <path d="M7.66797 11.5003H9.33464V13.167H7.66797V11.5003ZM7.66797 4.83366H9.33464V9.83366H7.66797V4.83366ZM8.49297 0.666992C3.89297 0.666992 0.167969 4.40033 0.167969 9.00033C0.167969 13.6003 3.89297 17.3337 8.49297 17.3337C13.1013 17.3337 16.8346 13.6003 16.8346 9.00033C16.8346 4.40033 13.1013 0.666992 8.49297 0.666992ZM8.5013 15.667C4.81797 15.667 1.83464 12.6837 1.83464 9.00033C1.83464 5.31699 4.81797 2.33366 8.5013 2.33366C12.1846 2.33366 15.168 5.31699 15.168 9.00033C15.168 12.6837 12.1846 15.667 8.5013 15.667Z" fill="#FF0000" />
                                                </svg>
                                                <span className="ml-4">{errors.email}</span>
                                            </span>
                                        )}
                                    </div>

                                    <div className="space-input">
                                        <label className="col-form-label">Password</label>

                                        <PasswordWrapper kcClsx={kcClsx} i18n={i18n} passwordInputId="password">
                                            <input
                                                id="password"
                                                aria-label="password"
                                                name="password"
                                                type="password"
                                                autoComplete="current-password"
                                                ref={passRef}
                                                maxLength={20}
                                                onInput={(e) => setPassword(e.currentTarget.value)}
                                                onChange={(e) => {
                                                    setPassword(e.target.value);
                                                    if (errors.password) clearFieldError("password");
                                                }}
                                                onBlur={(e) => validateField("password", e.currentTarget.value)}
                                                className={(errors.password || messagesPerField.existsError("password")) ? "auth-input input-error" : "auth-input"}
                                                aria-invalid={
                                                    Boolean(errors.password) || messagesPerField.existsError("password")
                                                }
                                            />
                                        </PasswordWrapper>

                                        {errors.password && (
                                            <span
                                                id="input-error-password-required"
                                                className={kcClsx("kcInputErrorMessageClass")}
                                                aria-live="polite"
                                            >
                                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 17 18" fill="none">
                                                    <path d="M7.66797 11.5003H9.33464V13.167H7.66797V11.5003ZM7.66797 4.83366H9.33464V9.83366H7.66797V4.83366ZM8.49297 0.666992C3.89297 0.666992 0.167969 4.40033 0.167969 9.00033C0.167969 13.6003 3.89297 17.3337 8.49297 17.3337C13.1013 17.3337 16.8346 13.6003 16.8346 9.00033C16.8346 4.40033 13.1013 0.666992 8.49297 0.666992ZM8.5013 15.667C4.81797 15.667 1.83464 12.6837 1.83464 9.00033C1.83464 5.31699 4.81797 2.33366 8.5013 2.33366C12.1846 2.33366 15.168 5.31699 15.168 9.00033C15.168 12.6837 12.1846 15.667 8.5013 15.667Z" fill="#FF0000" />
                                                </svg>
                                                <span className="ml-4">{errors.password}</span>
                                            </span>
                                        )}
                                        {usernameHidden && messagesPerField.existsError("password") && (
                                            <span
                                                id="input-error-password"
                                                className={kcClsx("kcInputErrorMessageClass")}
                                                aria-live="polite"
                                            >
                                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 17 18" fill="none">
                                                    <path d="M7.66797 11.5003H9.33464V13.167H7.66797V11.5003ZM7.66797 4.83366H9.33464V9.83366H7.66797V4.83366ZM8.49297 0.666992C3.89297 0.666992 0.167969 4.40033 0.167969 9.00033C0.167969 13.6003 3.89297 17.3337 8.49297 17.3337C13.1013 17.3337 16.8346 13.6003 16.8346 9.00033C16.8346 4.40033 13.1013 0.666992 8.49297 0.666992ZM8.5013 15.667C4.81797 15.667 1.83464 12.6837 1.83464 9.00033C1.83464 5.31699 4.81797 2.33366 8.5013 2.33366C12.1846 2.33366 15.168 5.31699 15.168 9.00033C15.168 12.6837 12.1846 15.667 8.5013 15.667Z" fill="#FF0000" />
                                                </svg>
                                                <span className="ml-4">
                                                    {kcSanitize(messagesPerField.getFirstError("password"))}
                                                </span>
                                            </span>
                                        )}
                                    </div>


                                    <div className="d-flex align-items-center justify-content-between flex-wrap space-input-bottom">
                                        {/* {realm.rememberMe && !usernameHidden && (   //remember me condition */}

                                        <div className="d-flex align-items-center">
                                            <label htmlFor="rememberMe" className="d-flex align-items-center cursor-pointer">
                                                <div className="checkbox-round pt-sm-0">
                                                    <input
                                                        // tabIndex={5}
                                                        id="rememberMe"
                                                        aria-label="rememberMe"
                                                        name="rememberMe"
                                                        type="checkbox"
                                                        defaultChecked={!!login.rememberMe}
                                                        className="custom-checkbox"
                                                    />
                                                    <label htmlFor="rememberMe"></label>
                                                </div>
                                                <div className="ms-2 login-remember-me">Remember me</div>
                                            </label>
                                        </div>
                                        {/* )} */}
                                        <a href={url.loginResetCredentialsUrl} className="forgot-password pt-sm-0">Forgot Password?</a>
                                    </div>
                                    <input type="hidden" id="id-hidden-input" name="credentialId" value={auth.selectedCredential} />
                                    <button
                                        disabled={isLoginButtonDisabled || !canContinue}
                                        className="btn leap-button mb-lg-4 mb-3 w-100"
                                        name="login"
                                        id="kc-login"
                                        type="submit"

                                    >
                                        Login
                                    </button>
                                    <div className="text-center fw-500 account-confirmation">
                                        Don’t have an account?
                                        {/* <a href={(new URLSearchParams(window.location.search).get("signup_url") ?? sessionStorage.getItem("SignupUrl")) ?? url.registrationUrl}> Signup </a> */}
                                        <a href={(new URLSearchParams(window.location.search).get("signup_url") ??   (profileMode !== "appointment" ? sessionStorage.getItem("SignupUrl") : null)) ?? url.registrationUrl}> Signup </a>
                                    </div>
                                </form>
                            </div>

                            <div className="auth-adverb d-none">
                                <div>
                                    <div className="adverb-header mb-2">Try the Leap App</div>
                                    <div className="adverb-sub-header">Scan the QR Code to get the app</div>
                                </div>
                                <div className="adverb-qr m-auto">
                                    <img alt="adverb-qr" src="data:image/png;base64,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" />
                                </div>

                                <div>
                                    <div className="adverb-img mb-1 mt-3">
                                        <img alt="adverb-img-1" src="data:image/png;base64,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" />
                                    </div>
                                    <div className="adverb-img">
                                        <img alt="adverb-img-2" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAzUAAAEQCAYAAACJElpAAAAACXBIWXMAACxLAAAsSwGlPZapAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAJjkSURBVHgB7f0LvF1HeeYJv7XPkWTZ1hXZQkbB8uAEkEnIIMIM3UlQMCHdk+5APjDpcElCko8QOgM96SQmbYOsYCcYcpmQ/kgg6SZg54bsfCE9ExonNk6mCWmC+H30YIN/nxoLEBhZsnQk2ZZ1Obumntr11npX7bX2Xvuco6NzpOdvb617rVqX2qee/V5KhBBCCCGEEEIIIYQQQgghhBBCCCGEEEIIuWhw5pPXee/jp9hm19nlWjm6zpQhxdTZqT1PmJfyWFO+pO15fVEfsccXZdpp7Rpk+B64Yj/XUp69D7nODdcoDWVJcXxTPcr7KuUzsecojynvlQzff3vPavep3K98FsW1SjpGGq7BNZzTNdTRbhv1nJ05V7lf0/tSXkNZ/tB+Rd3ttHYfy+en9dJ5s2/TPXNN5ynujz1e2t4nabh/tpyWNtb0/ktxPa3PsliulcP2z/YvbP9236Z7xvYvbP8N93XoWYiw/V+g7X/J05MlTMPNtPP6BPKDcs7FD7jlllvysWZdDw9r9+7dDvO23LRPLgfb0zqd6k4uoefO9UK5WF/UuRfW91AfGdxvLMfjTB2crpeyQs7psXqeXjqnfWO1vFwxrY+5plw2zpvKjPvoR+poWc6U27iuqEc+v57DnLpWlTSTnwOuQTfaZ6/XYe8t7gv2w73V+2Tu2dD+Wl9bPo5N5dgvjV799uc62vr3pP5FavfVa+8V9yceh/217iKNf6R66T12qY72eeZ6pHLEvrf2Olu+lKQ4r963fE6p3kW9r6J1QNn6TIv65PcP57HXntqa3uvaF7NpE/m8YtqgHmvu7dD7JoP2wPbP9s/2z/bP9u/Y/tn+593+nW3/imlTueyyPFKnVMCtn0LsNH0xjvtMpU9el75gpuyyfnR9mo+fG264Ycpst/vm7fiU59Av/WL/sj5TbfXGMabcqfK8ZVm2fuUxxXFa7lRx3WVZU233xB5j62jLLO9LeR5zfW33YKjeDdc/tK18Fvb5lcfrtjHnaq1jyz3JdWg6d/H+ldOevX9lnUbdg7a6lO+ULds+53Hnamo39j7ad0qa22HtHZER7355L6TlmY9Y1/rs2P7Z/u17y/bP9s/2z/Z/MbV//aS+da+hn53735Lww9YpuejRG2QecJN6zcKl4SE1vcz2JZoyjWToY794x63buXPndHH8tN2vOGbaHtNUh6bz2EZdTO0XynR5fNt5WuqdP3Zbsd80lpuur9zPbiu+lMpraK2LbfBtdW26d6Pq0nKO1utvKr/pHWpaZ+5VWYfW5zKqXk3r7fOw9xnz9jqa3qumujfVsemdLd+DEXWe7vD8ppvKwTlGvadN97Fsb237sv2z/betZ/uv15Htn+1f68/2f9G0/3jvrXCSQhCWQqpJ7IgMuSRefPh2P9tscSmUca/8om16Kc0XJD4r0jQvY9uOHTtWmJcw76fHYruWUZTX6WPKrp2/LMtcw9hPU32KxtSpvuW2ccuj6qH3G/erqQ66vqmeDedaUda9eM5jr6Xp/ozbp1xvGnzbNQ9Nm87bVD97LTq179q4a2yp94quz86WUZZb7tdQr1x3e39GvW/lc2hb7nDtK5ruWfneNdSb7b/9nnZaHlUPYftvPW9T/dj+d7bWYcy1s/2z/Y+9lqb7M26fcv1F3P5bRZQY4SOVqKlZIKVd5Cwq51NN4dxeKn9Gb3wNYbmJHynq+NnPfnbb6tWrXzI1NfX88Lk6rPrOuJNzVwshhBBCCCFkToTu+FfCZyb0q2dmZ2c/j8/Zs2f/f29729v+2/3336/99uhrFvrpPvXVzeFeqvAlb+Oqzrl/2mKLGpeusDx3TeDYbX/8x3+84fnPf/4PrVy58iXhuB8Kq9YLIYQQQgghZLGYCV34vz19+vRffv7zn//L1772tUftxmDZkeuuuy6KHK8BNybZgOn/nzMWS9Q4VW5Gwem5o5gJN8OlmxHNVuGGvWTVqlU/ND09/WNCIUMIIYQQQsiSoN/vf/jEiRO/s2PHjs9LssJA2OzZs0ctMrF/r2LG9P9124JzzkVNJdZqas0KG5duQlwBMXPppZe+I2x/iRBCCCGEEEKWJLDenDp16ld+7ud+7u+wnFzUslsaXNTMvuj/11zYFpJzKWoay04XhNiZeFE7d+504QbI3r17n7927drfGCdmer2eXHLJJRIsOLJy5cr4mZqaisuEEEIIIYSQuXH27Fk5c+YMLDFy+vRpeeqpp+IUy6MI/fuPHThw4Be+7/u+b7+ukiq0xJsAG5/2V4GzYPTk3KBjzkR3Mjv+zGte85p4ziBmkIbOvelNb1r/0EMPvWPdunWfbRM0EDLr16+Xpz/96XL11VfL5s2b5WlPe5qsWbNGVq1aRUFDCCGEEELIPEGfevXq1XLZZZfJhg0bZMuWLbHvjT54MD609rlDH/4V3/It3/L//9KXvvROLO/YsQMZi7OBQ/v/qgt0gNSUCVkWggW31DTEzDhjgoquZgDuZp/85Ce3PeMZz7griJbnN5UFiwzEDG4uIYQQQggh5Pxy8uRJmZmZiVacFvZ/7Wtf+/7du3fvxwJc0mwigUSpZOatbBZM1KhVCcIF/nNJ3DibmtnEzrig5N4Q1N5vSEMSAIoZQgghhBBCli5wVXvsscfkySefbNqMlNC/8uxnP/vf79y506dYG1BOLfMSNgsiakwyAJuWWctXMZOXg6B5RxA07yjLgUlr06ZNFDOEEEIIIYQsA06cOBEtNxA5JWHdu57znOe8S6oMaV4zpCXDx4KJmwWJqXED4iz+0bTMuv3QoUMxIYCMEDTw03vGM55BQUMIIYQQQsgyATHuiL25/PLLh7ahz4++/44dO6IOgCaQNJSLenLJAjHfgtyYdTWXswcffPCdK1euvNnurEkA1q1bJ4QQQgghhJDlybFjx+TIkSND62Gxed3rXveuIID8FVdc4ZMHF8juaHYsm7kM1jlfS03NPITK2BgaWGdCpXGOaKFpEjRQdhQ0hBBCCCGELG/Qp4fnVZklDRabP/qjP3rHiRMn3AMPPDCVPLgsOTZflyfNijYfUQOzUa+oDNZh3JkoZFDx7du3uy9+8YtvLV3OcLG4aIwzQwghhBBCCFn+oG8Po0WTsLnjjjt+7PTp01krYIgXbFODSEuMTSfmZakpT6z5qGFWwnTv3r3ufe9737YVK1bULDS4yKaLJYQQQgghhCxvtK8PryxLEDy//v73v/+ZO3bssBrCpezJcd5XJhrMd/ZBm1NMjZ6gGI9G5/Pn2muvdR//+McfCvtdbY+nhYYQQgghhJALm2CVkUceeUT6/X5eNzs7+9/uu+++7//Zn/3Zo0HcwAiiG63QyfMp1masBWdiSw0K3r17t0sjgcZ1JntBTST95V/+5c2loNm4cSMFDSGEEEIIIRc46PMjIZhlamrqO17ykpf8r8H40QuCRkx8TdQSZoDOwcqOCQOmZEIgaNIAOjGGJszD3QyZzWKl9u/fHysULDTXXHnllR+1xyLVG0QNIYQQQggh5MLnkksuiZaaU6dO5XVB2HzvS1/60v/0X//rfz2Ibc9+9rNl27Zt0BECbWEox8BsZdKglpqS0nkdWFMTAwSB4571rGe9t3ai6WnZsGGDEEIIIYQQQi4eoAGefPLJ2gCdT3/6098bNMMPhNma+5lJGjBYOXA/czJmUM5JY2o0eCePtikmhib5xfX+/u//fmew0vxne2Cw5jQOykMIIYQQQgi5sHnqqadifI3l8OHDP/D617/+b/ft29dPOgI6I49dA2Gza9cuHbNmpKiZOKbGOatnBoNr6jyyncF09LSnPe0me8yll15KQUMIIYQQQshFCtzQ8LFs3LjxpiBoICwQXwMDSRYZyKoMUWN1x6jyO4sazXiWkgPEk0DQHDp0KLuiIdvZ7//+73/n1NTU99hjg8gRQgghhBBCyMVLGYrS6/W+9x/+4R++t9gt6ooU3pKXZYEsNSpocmEPPPAAYmliogDE0WAfKK1nPOMZ/9oeCEXG8WgIIYQQQgi5uGmy1lx++eU/tGXLFiQvi15fUsTuw5ACCQIDy6hxazrF1KAANf1gcBwz1U8cFfQDH/jAhuuvv/4b9tinP/3psnr1aiGEEEIIIYRc3JSxNUFnHPvoRz/63Jtuuunotdde69etW9e3sTVhe79I69xoseliqamNSQMgaJC+Ofm95bO88IUv/Jf2QFhoKGgIIYQQQgghAJaaXq+SIEFnrPtn/+yfQUPA60tMbI1L2/O8JixrKnesX5hPZhoVNlqw5pDevn27IIUz1l166aU1UYMEAYQQQgghhBCirF27VmZmZvJy0Azfu3Xr1jsOHDhg42eG0jiPGohzrKXGpm7GwJsm21lcp4IGnxUrVtQSBFDUEEIIIYQQQiylJ9f09PS/UEGDTMoptgZAe+R59Rxriq1pFTVpZ5vxzKfkALqLQ7aztI/7+Mc//vwwXZcLDmYlup4RQgghhBBCLE0uaB/96Ee3YXb//v1xlW6zmZaDgaWX9h+Kq2kVNdhZ1VCy0MR9YamBnxsGyDl79izGpYm7b9q06Tvs8StXrhRCCCGEEEIIKSmzoF1zzTXfs2XLlixgNLty2lyKGGfj/UW6ZT+L+0DUBCsNju6JyXq2detWF8xFvS984QvvCZV7ix60fv36oVzUhBBCCCGEEPLYY4/J8ePH8/LJkyff/7KXveyXDh48CL3RTx+dF6mEjVUzeb41UYD6qsFKg+ktt9wSA3Z27twpjz76qMbSyOzsrNu8ebOsWLHi2+3xq1atEkIIIYQQQggpKb26pqamrg6CRoK1RlM+51iaNM0CxtvxZhIj3c8whZjBoDcpSCdmPQuCJu+Hk/b7SB/t1hXHCyGEEEIIIYSUlKJmenoaBhIHXSHGmwwGlYQVF0OZ0UZmP4MwSYNs+uuuu04HwcmqCa5nmKYAnvX22GC5EUIIIYQQQggpCZaZptUO1hqpQl3ciRMnnBE2cVsyntQsKI3mFDMejd2nl1zPeslS0wvmoR7U1JkzZ6Y+85nPHLNlXHPNNUIIIYQQQgghTTz88MO15WuvvXbdFVdc0Q8GkxhTE5b9vn37NLYmx9Qk9zOfNEtc2Wip0YE27SoUYFzPopXmkUcegZpyR44cEUIIIYQQQgiZD0HQ6KwLgsa6ouXxMpNWqbmpjYqpqQmbYKXJBwbVpKt9UFPIcsYAGkIIIYQQQsh8wDAxNbGi6/FPysSsXmXexvA3ihooHzXnIEmADKw0WeFgfJo06mcs6ejRo0IIIYQQQggh8+Hw4cOCzMoYswbjYQZjik0KkLVLstRkfdJqYTGZ0lzx6TVMe8E8NGOPZ0wNIYQQQgghpI2GmBokHstj1ARR09+/f38cq2bnzp3wEPPJWuNNWucobNosNRp045KlpgZUE3JIy3D+aEIIIYQQQgiZmPXr17uNGzdmb7AgaOJ0+/btGFbGBUET90tapaZjhkRNEj064KYYURPXBUHjwgl0UJy4PlRACCGEEEIIIWSuzMzMyJEjR6Lm2Lx5czaeIFHZjh078n6qVSBukhvasKhJCQJ04M24Kn08VNL09LSXujsaIYQQQgghhMyLZCiJWsOMVxPdzfbu3RuXb7jhBtUfMVEAEgZgocn9LJpq0qCbgtRpacCbqJL27duXd4RfGzKfBVVFcUMIIYQQQgiZM0lTWF0RBQ4MK2le1AUt75AsNdMN5bkUeBPncSBEDUw+J0+ezOPU4J9Dhw5RzBBCCCGEEELmzbp166TX69UyKyOW3+gPj2FmkJW5GFOz2VIjgwQBWbBg0E0QCkRqtVggUq3BUqMVIIQQQgghhJC5olaXlCxAtm7dimQBmI3LMLToMDM6Ro1Omyw1utFD2GiigGCliWJGsxAYPzd37NgxIYQQQgghhJC5cvz4cY8MaMFSg5TOOi6mv/baa2XlypW1cWlMtuZIY/YzPQCCBjE1YPXq1T4NfhMHw5HBiJ9CCCGEEEIIIQvBzMyMh6Xmiiuu0FVOY/qR1hlTDZWxLmhDlppiwM0cjLN3714VOy6lc/aHDx/WdT0hhBBCCCGEkLkTNciRI0dqATOw1MDAEmY9DC6a0hmZz9Ri0ypGsIe6nqVEAdlSs3XrVk3rHAfJEUIIWQJ84xvfkPvuu0/+4i/+In/+8R//UU6cOCFkMr70pS/Jq1/9anne854Xp1gmhBBCziVr167N88ZS42GpSQaWTMoUoJ/GmBqvA3CqqEkBOT2oJGQgQFzN5s2b/ezsrDt79qwQQsj5AqLlYx/7mNx7770jxctznvOc+HnLW94iV111lZB2IA7f+MY35vsJQYPle+65R9asWSOEEELIueD48eNxrBoMwlmQLTd79uyJxpUiUYBvtNRgIwSNihpJVpmgkmKBW7ZskX6/7w4fPhz93oQQQhYZiBl0tPGBRWacNQYdc+z38pe/XG6++ebYcSfN4F6V9xPLsIIRQggh5wpYalTQHDp0SIIRBZmX4zK8xqQKkckWGo2racx+psE3NvsZgPsZzD8QNeFECOLpoSCbS5oQQs41t99+u9xxxx0yV9Q1DVYbfJY6sER9/etfz8uveMUr5BnPeIacK9qsMeMsXBBDVvjAMvbSl75UCCGEkC4g+1mYQGPIkSNH4rqzZ89G40pyP+vrODUyGFuzr8c2Dr6p5hwraCQlCQjCJvq1XXHFFQ4KihBCFgtYC9761rdGK00T6Ix/13d9V80nF2KgyfIA3v/+98fpUhc2Ghuk4BrPpahB+RBOEFPKG97whrh+FLjPek/BK1/5SooaQgghk+A2bNgQBQ1ianq9nj9w4IAaVjDwJmL9+xhDU+NldBTOIVFjU6PZ3M9SuaDllUlFMVEAIWRRuOmmmxoFDTrbECajOt0QBuhwl25ny0XYLDa33XabXH/99VGo4L6OEzSEEELIApCFiDGeeJPSWdM6exNTE5eHYmqwXj82+xnQApP7WTYL2V9FCSHkXADxUcZ0wDLzoQ99KH7GdbphNUCge5N4aSqbSLSyjBOLhBBCyEIRNIUrw1rgJSZG7ADrTaYGmaZEAdGKg8+uXbtiIfBbS8E5kTROTdwXGQqQqYAQQs4VsK5YtyaA+I677rpr4g43Oum33nrr0HpYgZj6mRBCCDl/GE2hugNuZ73t27cLPk2oxWZI1CT3tLjDa17zmt4NN9wQswycPHmy5maWckfHdbTUEELOJRAcFrXQzDWuBFabG2+8sbYOgmY+yQcIIYQQMj/WrVtnM5wh+xkm/vTp0+7BBx9sO6w5pgbxNloQ2LNnT5yWBamfG9KuUdQQQs4VDz300FAcDawt8w2UR+A7XM5s2RA1WD+XsVggihB/YpMSoBwdH2eSMselm8b2pn0WcvwdXIO1XKH+5TWU+5SWLvzidq7rOQ59JrYeeB6oA6ZzobzuputpOy8sixzrhxBCmjl27FiMj0Hcfq/Xc+HTR0pnDYFRimRmgxibsjC4nbkqQ4BOYfbpJWHT0w9SOqdEAVPhZI/Zcq655hohhJD5gjFlEOSvoAOJ2JiFQMe6sXSJz7Gg0woxNG6snC7JDBTUqS3DWxsQeZ/4xCdkoYB1zGY/a0p/jWvG85mUSe/xpKjVDZ9RzwT3DNeETG+TUN4bez0QynCVhKBpA5ZCDgJLCCEiDz/8cG352muv3RgmSNPs09R+xGwTsw40JwooV+GfJGjyADjBHOSDoOHAm4SQc8q9995bW17ILGVNv5pPkjAAnWYM5jmu8wxUQEEEMHbn3KHPBMJi3H1Gum8IlB/4gR8YKUK6gvGTkHJ8XFkQg6961auYnIIQQgrWrVtXroo6BCmdbXx/sNTk+dbBN5OlJhd0ww03RFez8MfB7927V/bv3x83Hjx4MPq7IZc0B98khJwLIATKjinSDC8kn/70p2UulL/Wd0XHnMGv+/ylfmGBkCkTSnQB4ubVr351TGM9qdXGnnuSmCy813iH8B7M1Q2OEEIuNI4dOybr1693MzMzmvEsKpbkfubEZEEzmqU1piYrnt27d/sHHngAo3bmzXbfTZs2+cOHD3OcGkLIOaH8xXupxCPgF/lS0KBeOkClxvvowJ/o7NrYCqzHL/rI3tYEkhhYMYfz2XuB7WVH+HzcF1wrOuXKZz7zGfnd3/3d2vYmy9q56MQ3CRrcE7h6oR56To3zgQWwfIYQGRCak7rG2UFHJ3kP8IxhuWt7Dwgh5GIE8fpGY9S8wtIwM0PrwXRDWU6FjR2nJg10U6Pf70drDRMFEELOBZ/97Gdry0thvBR0hMtf5NGJRee9FBbaQcb2stOtHeEunf6yXA04P9+g024TNqDzXm5fjHq2pfxuypCnCQIwBg8ED4SMFRoqMiYRiRCdAOXinG3vAc75tre9rSZQMQ/LHccCIoSQTDaabN682a1YscIdOHAgiphkaPE6hiZoHXxTkvIxyQK0APiz1XZETA3GqSFkPnztzFH5k+N75fbH/jp+/uqJB+SrZ+nSSGRoDKxnP/vZcr4pO88QJbCcjOsENwXad4nFIeOB1cvSJmhK1NJknx2E2VxSe+s5R70HqE/TPnNxYySEkAuUGNqS8AcPHvRB0MQFjFOjYgYxNca7LP7TJGpUzFizTh58UxMFKPB54+CbZC78lye+Ia/42gfkf3z4dvm5b35U3vPY38TPG77+kcG6R/fI1yhuLmrKX/7Pt1UYnU9bJ3RkJ0lcUGY/g6Cxmd3I5MDKUbopTjKGkWZAs8xFbCIep4t1R93TLJNmuiOEkAuZFKvvzZiYHoaVYniZqFds2EyrpcYSVJHbu3cvRvREogDZunWrNwUypoZMzM8/LPLSL1wqf/fE0fDCmVcuvlEDV0lYb75z/0Dc0HJDwPmOpyl/wUdHdlLKDjQzYM2P0sqBQP9JxzCCyLBJGyYVm7AgTuI+ViYjKMU7IYRcxMRO4caNG10aE7M2fmaBS4Imbm+MqdECEFODj7qfBbOPf/LJJx2ETXlyQrryK8GK+L5vQk9vlMePvVkuX/d70puaQRoL/C9x/Fcobxlktfjj45+V//LUl+VHL98hN258mZCLl0ktNfgFf1LR0PRLOkDchbUITNqRVXQgTrUEaIY3Dsg4N0orB+Jk5gKOs66FeG+a3oMmXvSiF8kkQHTZdwDg/WI2PEIIibg0bIxqDA/DCtzPVJMkjWJd0BpFTSxMD9DC0icKmi1btrhHHnkkrt+wYYPr9/sUNqQTHwmi+10HetEYg3fQ9zfKE0HYXLb+98K6o/HNG3hH+sF8fOucfPXMUXn30b+WP3l8r9y44WXyo2t2CCHjsFmpuoIOZ1NntnRxmrQjq6Azi3PY8ihq5sZDDz1Us3LgHs414B6pwstEDl2ZixgpRQ0hhJABiNfv9XqI3Y+GFoS+BP3hrfsZsjNLYVhpEjVtZh4XCouDb546dSqu2LRpk6TsBHRBI51419ddtgW6JGD6KmzWBovN9MxAcQ88Jc2RwWoT/vvK2aPyrw/tiQLnVzf9S/nBS68TcvGADuz5+jW77OSiU7pQsRBf/OIX+Sv9HCjdtuaTKhqWNys0MO1qPWEGUEIIWTjMGDWR5CGWRQzCYvbs2eN1nBrNbdZmqSmFTU20JCtNFDTBUuNpqSFd+PwTIl855RqTi8+eDcLmeBA264LFpof4GZcsNb7a2Q3mPSw3Qdy87psfkdeueaG8PVhunjm9QciFBzqLNt2unV9syg70XAZ5bOPxxx8XMjllkppJY2lKSusJyqfYJISQxSP83Xfpu71xLJo0zExN9Ki4GRI1aUN2PzMuaCJ1sRMLQ4YCVEAIGcPfoa+Q3pQcuJU0S7AySn9WXdE+AG9KGYTWDA7w+dWr3uMYb/P43vD57EDcrKe4udDAr+fWQjJpQHU5OGQTKF/HGSHLi1LkzteFD6KoHByTEELI4mEETV/XJfezoX21jzjOUhNJgsbfcMMN8uUvf9kfO3ash0AdLauoACEjmZlVK81AoMR5VzcDRmEz8zNyKWJspmaMRHfGJS35rkUGZQ3Ezd4obF57+Q6KmwsEuBPZ7FaTunuVg0N24fLLLxdCCCGELD7w0EA8zczMjCCl8/R0liqtXmGtlppyhyBo4LeGVa4YfFN/Mu8JIV1R04xqExkImxxj4wfC5smZN2dhUxcxkjKj2TKr9/XXZv5a/iiIm19O4oYsb8oYCVhVFjqovoyV6RofgWQCGCF+IZhPLMjFTOkaNl/LCi0zhBBy/kkxNbF3qCEvWEYWZs1+BlLfL3uRDYma0pSjq/FPsNIgA4FDooCzZ88if3SMqUmD5BAyEn2l0jA0Qcz4JGxcu7DZ8AFxvSOmEJ0ZvMPqwmbf26/OHpWffWyP/NqxvxmIm8sobpYrZfpjHT+ka6rdLnz2s58dOmcTpdhBh3qumbbIwlA+k/mO91IeP98YHUIIIZNx/Phxv379egdhg3FqtmzZIlNTUy583/eLwTdl9+7dOgBn/K17yMoSVE8OzIH7WbLSiBTJA4JpyG/atMmnmBohpAsu/zvIZqZKRyNn4nx6y6KwOfozMe2zpO0pFXlyXktixuhva8FBMoE3H/6o/OCjH+TgncuUphS9CzlYJeIn7r333tq6F77whY37Ir7HgnTC5PxSPpNJ0jCX4HlaSw3ePSYJIISQRcclS010PwuWGnfgwAGkcx6K3zdx/3Fbk+vY4Mfv0HvUnZFpIB3gbaBOv99nOmcyGU4H15RoYqkyN/uadXBY2GwYrHRZ1eTjasu+kjU+bcPAnc/7xu3ys0f2RCsOWV6UVhnE1dxxxx2yEDSNGt9mqSnF1UKlcyZzRwexVCBK5vpcPvOZz9SW6RJICCHnhex6liw1cZwaJWmSSDGeZns8jOlgWv+1OE5NUE1I45xH+2SiANKNWth/mvFmPukW46aGTxQ2R34mTDfIwOXM10qKy96n8W2cKctV8Tdh9o+e2DsQN0cpbpYTEBOloEA65fmmd8bxpTh6xSte0epyhPX2l3u4Ks21A83YjYWjjGuyiSUm4c4776wt410ghBCyuKxbty5ON27cGDt0iKkpM58hgRkoMjQPixp0BL0vw7AjcaUOgAP1BDZsYJYp0p2slWteY75YrtB3EcLmZBA2PgmboUKtGpLqHLVpAuLmfzn0Qfm1438jZHnwlre8pbYMUfDGN75xzsJGjy/FRXmekle+8pW15bmMVYM6v/jFL47nh6iai8vU+RyvZ6lRPhNY3ya9pxBCZTwN46UIIWTxOXbsWJwmw0mN7du3Y4yaWmhM/lFbmi01OkxNduiBqWfHjh0oLJuA4OcGmCSATEplmXEDHaJWleiCJmZqYmaSxWbl8SBs5JJclq/F06SEfGmdb0j+p6sQY/OrQdQ875u3yx89uVfI0gYdzNINDZ1QCINJrSUQBK9+9auHOrEof1xgOPax7k4496Rj3KDO9tgux5dxi/OJHVlMFsNFr8mS97a3va2zNQzvQylOR1nsCCGEnDvUUiOmu6jaY/Xq1d66n8WdYl/StcbURCB9glknJwfYu3ev2KwDwVLj1TRESBeyPslGFas6fJEeTVJaMxM+E/45emqjPPlYEDb+ksHRpgibeEBdzuoVSNnVzGFfmT0qbz66R647eLv8X6e/LGTpcuONNw51XlXY3HzzzWM70OjkovP6qle9akjQwK1snJUGQNCU+8HagvOPs56odag8dxc3pzIgHtaIpWitee5zn1tbxrXOxZo1KeUzwXkhXMfdI7wz5TPp+i4QQgg5dwSNoQYUh4zLGFYmaJGhnp1PYKFpnJooZJAmbdeuXRA2UfjAUhNMQs4OvplMQxQ2pDuusqBoaudq3UDYOJgSMRVXxcnomDb4nHlGEDZvltVP+z1xvafycWp+dPaVdMV5nQxvk4G4+eeHPyivv3SH/Ls1L5NnTtGtcinyvve9L3ZCS0sFOvn44Nd1CAAb5I2OLfZvs26gE/uhD32o89g3sNagM2yzsOHcWAfRVf7Kjw4z9sU+pfUA+5buU02UYg7lvPzlL4/xJHouXPP5jgPRe2/vNUQN7o0+E9wPPMeFBPcHQsQKKJwH9wj3t3wmqB+eR1Mmvdtuu41WGkIIOU8Y97M4RaIAZD9Lm2Ocf4qpwTiafVhpRokaVwxmgwLyj+UwAZmAHZdySVPYkLmhCc107BojbGpRN2m7jmPjz1wlJyFsNgVh455K21yywrRo7ULQ1E6R5v/o5OfkzpN75fWrKW6WIhAeECDovDZlQENHVkVEF9AZvvXWWyfuxKLjW4orPXdTRrUm0Ml/+9vf3mlfdb8rr9leJwTFUghuh0VNXewUiBprSYPQXOh0yWpdKS1DKni7gOfKWBpCCDmvoGfWh6UGwsYMvml6i4K4muyrk9zPfNM4NV6TBRRZBXKiAAyEo6Rc0l4I6YB1PXMt7mG1NM2iy1WsjB4Xhc3hQYyNd0VutRaLzFBdpJ5fwA+UldwRhM32Q++RX338b+SYf0rI0gHCBh1niJG5doy1DAikufwqj+PvuuuuObspQaBMYh0CqO+oQUfLcVbOFyoUR3GuYm3wPHCfJrmvQK11zHhGCCHnnTj4JmY0ft9SxtToMfinKfuZC2Imd/M0bRo2wZ8NQDXpiZj9jHSlnqp5sKCxMnYMzYHQMIkDnAaCSSFsfHRFO3nozTHGpjp4MGvjahrz+cnAAhQ93Hp1l7V49rDhtiBqXnz4t6P1hiwt4FZ0zz33xA50mda3DXS40enFcaMEQlfQif7EJz4RO8PjOtLYjnqi8zyXjjdQMdc2hkoZr3O+wLPR+9LEuUx0gOcKwYlzjxO9GiN1991300JDCCFLgLVr10aPMs1+BkNKShQQO2rJe0wZ9Aa9d3nBMjDU1LbFbuKOHTvc3r17e2m5lz6Yn8L8vn37HrPlXHPNNUKI5Ve/KXLbIymQf+DiKP1+5QPmTdxMMpgMSOqktt6oFBzXW/kNWXVFckWzlKKmdEGTEcvF/ldPb5R/d9n10TWNLE1gAYC1Ap17tVqgYwtrDITAXITEpOdHh70cmR7nXujz4xrhxoUpsqOh/IV26VoIcC9wT7SeqONiDmzZ9Ey0DhxgkxBCzi8PP/xwbTkYUDaGid+0adPs4cOHfRA1/WBM6Yf1PmgNZD/rB2Gj3bU+/lEvsxbHnNy3jMIF1po9e/ZMhQJjBoL9+/erqOkFE1FvZmaGooaMBaLmV7+pAf2VkLFTnc9JArKYSfPFfnGa/nFB2FxihI1aagaxNj6vyy+9K8SOH1hu4mxN/Ljakd+z8hr5wNob5GrG2xBCCCGEzJlS1LzgBS942vHjxyFWZjdv3tzv9Xp+amoKyQJmpUpgGzM0I6FZygEQ6Y04j9OYmiBochdPY2qS+5kPgkYI6U5OYJGSAlRxNtk06CRvq+JpXLXOFTE2WuLpq+Sp5IrmnYm98X4oTMe3WGWcVqChznr+vzvzZXnu4dvlTSf2xKxphBBCCCFk/kCkBINJnJ+dne0h5CVlP8sBBhoag0zNMkhwFtc3Dr6pMzZRAEbx1HTOOMGhQ4ckmIZET0zIWHJ8jAwynElhKnTVG5tFThY7KV2zMwXVBukcTCths7puhXEN84111Fif+k5ZivlKHd351OfkuUcgbu6Sr/QpbgghhBBC5gOGj0mzDu5nbftZjaLWmlGWmtoBDz74YC44ZT+Ly8h+Vo52TUgbaglxhbhwTaLD1RMJ9AbR+5VlxxWmmmSBhLA59ejPhJnVtZga+xmuVNrmveh/uq0tyYAec8dTn5UfOPb78u9PfkoIIYQQQsjcgKawXmCbN2+OPcNgXMndsT179sQpXNDssY0pnXUeokZNPKGwMvsZFFQ80fHjx4WQrqRUFUnY1GVGY5rn8mA/OKZmyXEmQ5oki00QNr6/ujlJQELzZUS7ZuEDZ2vmrOlIj5XKGgo3tF98/D/Jc46+R+44xUxphBBCCCGTEjRF7HphnBpw8OBBv3XrVhhXBi46BSljc7P7mTMDhUDUqBoKhUX3s1Cws+PUYF9aakgXrBxwxtWsCsSvixtnBIYWUNMVpUWntrsfWGwOwmJziTRhxY5NJlDVMbmi5W2mPTnjPKdNJqzbP3tE3vT4XfLsIG7+7uyXhRBCCCGEdGPdunUxtAUpnRG/D0sNYmpgqYGBJe2W4/7DtK/HDv12nVLtOp2mfWD26SWVhE8PJzlz5swUgnOOHj06xexnZBy/dnDwsSmZqzTNrkjTXGQ7G9pfagPRVOu9ml9qWdFWbv49cb2U7tm89c2pnvX4gbDx3tctOCkdta9lHzCSzQ3qjmJev/IFcvOlL5Ore8yURgghhBBiKbOf7dixY+OxY8fQkeoHUdM/dOgQRAs++uu3nddP/NW5cfBN7bRZVq9e7a0/WzAHQUVJEDSOlhrSGWttMTnF665oPrt7ZYuNkwZXM5diXjQbmq/pCy0bFpvTB98svn9Jx0qmpARlQTk5gSvE0GB9lbGtElV3nP6cPHvmPfLTT+xhMgFCCCGEkBEgUQAsNXA/C4ImdrfgJaYaZOfOnTk0Bq5nmvkMNMbUqKCxiQL27t3r4IIGkvtZ9hliTA3pSt1zzNdWJp1T0ZDzwts5pwLICJCynDTtn9qShM3qqgQjlqoDq3mfBZM9u8sJnmvZDpLI8n44HUEUN6f2yrcde4/c+tS9MuOLAUIJIYQQQkjEJAqInazp6WmfvMXk/vvvj6Ex0CgYpybt15z9TC01UD9J1OQeGtI6l7tLvStISDuF0MhWl7jk6/tFa0wlemzqZufKArUcV9t3sOhzprSBxaZIHjCqgnakz1Sw1wQFZZ11ydWbRC3RQCjvXSf/Wl504n3BgsNkAoQQQgghBbXOFQwpGCOzidKrrNe2k7HSuGDqiUepSkL2s82bN2exs3bt2uY+IiGGmogp3cnyDlJLIGAGiq1ZcZwVLHGNup9VrmFahhdfy4p2+ps/E8exUTQ0x2dHtsKVDGU4n5MF+GwKsvKmaAKu+vFgMJhodQzc0H76ybvk246/Rz5CcUMIIYQQEkGiAEndpk2bNjloDrifFbuZ34yjl0zc3iRqcm8MwsbkgK71FRFTg/kNGzZQ0JDJcFIbY0Y1wmCaYmWMRaR0S8sCpVZetS0KIeerY8vjgrA588jP5Bgbl/6tD7ipUsmMWaOVLVzMfFZk3iqyPK27zOU52Q9xc/Iu+f4nfp/xNoQQQgi56EmDbzrE7adBNWP2M0mdLsTUYJ16k9mwmSZR43SsGhzwwAMPxD01jdq2bdviNoxTg+nRo0eFMTWkK9Yxy/b/KzToX6R0bIwDcPaq/Wppob3kcP1BuIsbCBu7Q2mxeeTN0WLjXV28uJxieiCwKrFTZjtzlXbRCkpltUkJ3OoXIEURgb89+2X51sffEwUOxQ0hhBBCLlZS8jEfrDSlUSWGwdx///3Rg+yGG25wNmMzaLPUxO4WDkAwThAwcW8dfBOkjAQ+WGqE2c9IN3xhOqnP16wxMohBqQbUrBsEraUnHtsz8TTJmuLMvi4H45g6QNh842dEUvKAKpuaqV95vrzB5319zRbjc2xPXazV7oJmnRYz1q185Mxe+dYn3iv/9qn/g+KGEEIIIRcdGHwT2uLw4cPQGrpaw2BgqfFB2PigT/zu3btTpIFrThSQx+AIUx14M0yR+cxj8E0FlpqNGzdijBohpBPWUlFYYLLHVq8SMS6Jk8E+lUmk5momg32t6LHuatVpqnKstUiyK9ogeUC20qRYnOxo5qVuiXE201ntRNX+do2rajmkj2rHeXnfmU/Jy07+vvxOmBJCCCGEXExguBhMMfhmyriMYWXUUpNTOscNvvKLafotuexy2U/PTHtmmYNvkrG8Owju2w8NOvxJOkjyFqumxbrBxGVfLisW7MCcOYLGZyeygegwZpf6sWG/KrW5rL/0G7Liqt+TY/6UWFGTKQfk1BOnrGZVZjSXEwrULFJ63rLFmRibcl+AQTvfuepl8mPTLxBCCCGEkAuJcvDNF7zgBU8L1prZMOtHDb4JYQNrTYqpabbUSBY99XFqxPRDgWY/g4mIkC5Yq4lv2GgtNvVN1q3SmxIqK0m2vOi8NwNxauKBOK/HuFp66Jknr5Kzh15TN/OIcUkzFqPhulfqJWdGc8NXOXRc09Zip/3+qPzkU3tkx+O/InLy74QQQggh5GIA7mdBb8SelQ2BAddddx30Sr8aI7AlpbP6qJlxanSMmtzbO3jwoLqfOSGkK87GyUjlUlbuZuJkJMfESOWapkH8ppzsVmZti5JEkTlOauKpSh5w9Njz5Oyjr6nFufjC/W1g/dFkBmoZSvJlSIxlqSXeCi4tuzZ1UsYYqVVnfbAefeix35Pe179P3KM/JXL2K0IIIYQQcqGB5GMwmASNEZeD3ojdKITA7NixQxOZ+aRRagaX6bKwgRVnkAAtBUfn4BwF/m2PPPKIR7o1GfcDNCGKkypDmUiO3eoF8dLvVy5bOZTGxHelw03cS91FrVQLxissr9OVLllUYLTxrq5E+sd3xJ2nrtyTLTzS5FJmAny0lfjqRIM5tT755EIn1tZkjxlGPUQhaD555E/l+WceHRxy/A/Fnfiw+LU/Fsyku0ILvloIIYQQQi4E1q1bFz3KgtFkqIu0d+9gbL8gaJwOOaO6BfNDlhrdYLI9WUsNUjrHwTdl+Md1QjrhCvcu7wvrjRSWlDwvNReySl9YyVCUUZv3Q5YhXW+NJP3jL5TZR28QK1CyeNE9i8Ir6051goE4G7biFB5utcO8OQUEzX2P/Zk8//RByQFB6UAIm96BIMBmfkUIIYQQQi4EIFJmZmbiPBIFJFzQIblThngaCJu4wXSoGsep0Rlj2snn2r9/v9NMBELIhFSZy5IJQ3whcnSmbhSpCRtxUsXGJJeyXI6vYmXsAJw1xVS5o9VtKJV7WxQ2B38kn19aXMyytWVIkRmrUnGMd0XjKbeHz7bZY3Lf4T+V7zx9sNhHbT1hRf+o9I7+irgD14o8/hEhhBBCCFnOIKWzpM6OxtRs3bpVnnzyyahZMPgmsjND2Ji+XZzptZQZe01W1KxevVoVkk+WGvV3cxynhnRDhYPP1pUhIWL2axU2TlJMjVQWnuwqZtFtMnSOfEhaWU8ZPZiBK9psjrEx5Tsbc1M7XXatq8fLVPNt2c/yvg6C5ngQNH8m33nmoBS+bdUNMAe7M/uld/gng7h5VhA3HxZCCCGEkGVKrZNz8OBBOXDgAIwqMa3ziRMn4noddibR7H4mlc9NDMIJH6ROUz+23LXCODWYbtiwgRYb0gk35LZlLSwVvZ4bbbHJofc+q4q8u3FjqwklZ/exgfv5B4GqSjqFMeTYDulHi42XxqwE2eJjr6naTWqlS3lDalrFuYGF5pPBQoOpDJmFpFBRRXGzX5HeYz8l7uD1TCZACCGEkOVK+Ut1zH6G+P41a9YUPydX9GQM8FnbsWNHPEjjakAwCcVEAcx+RjqTY1+k5vIVl11lxakyjlU7D4kTVwmP7GbW5MZml500bleL0aAF+dr5MIXFBsJG65VyGBh9ka7GZmUecf6arktTHHs1LDSH/kyuPjtjTiINBSlWLlVZ2dyp+6X3jWeJO/JTIrMUN4QQQghZNuTOzaZNmxDykrOfAQy+WeyXe0ojRQ0sNfBZC1aaeEBQSLlHpZYagPRrhHRGXciyVaXurFWKk5r1JM24elF1gWPczWrHNVSjOnhwrBU3VkjBYuNTjI2rK7N6RXS798P75Kv0QzE12/rBQvPon8i2s8lCY+tVP7jpKqoKZB+3UP8nPjwQN8d+nuKGEEIIIcsFFwSNHD58OPZ6tm7dWnaIass+jaY+MlEAwGidCMoJ1hqxA98geCec0K9fv14I6YwzL5i1yNSyn6X4l8YCvLjsOmYFkGYwc1IbrHPIItPgRebMvO5YO99gGl3RvplibApBY/KkDSUOGM50Vl0r9o0Wmkf/NEyToMkWmkHsUVV67eZJ5frmq32dMWXp/T3x2+IOXS/yJJMJEEIIIWTpsm7dulpoS7/fd4ipgQbBODXJe0zHqQGDX3Klud8YB6lxrvrtu/j0gimo98gjj/Q2btzYO3LkCIRRL5iFHrOFXHPNNUKI5T2PeXnvEeOmVQSbWHeuap9KLnjraWWnDcdLzT2s4VgpwlN8GotmqGxXryPWrN0bGsFHReNschCaOb3Uq9Lc4twgKcC9B/8kTgfr1MLj0rxVW1Jfl8vzNWtTTZk5sw1XuGKb+LW7RFb/mBBCCCGEnE8efvjh2nIQL08LE8Tzz0r1y22cR1rnK6+8Msb633///Vinw4K0JgqQNPpmnIf7mVT9NWdNQIipoaWGdMUNIvTr/fHSvSy7jvl6djPR9eYYJzWLTn3qU5Y1U7aez+yXi3aVu5jTjGjihr3AIHZgsXnkNXmDK4SK2bXuWFdsu/qsCppjxurSUlmxATvGOpNHGDXbNcVa+ZMFijv7Fekd/UlxB58lcvpvhRBCCCFkCRE7NMFwEjs3KaYmg5ia8MkuLOPGqYkUGamQLCCeCCYgu2FmZsYLIR3IblvGQ6oSFbVJnqsLmgZRZOddJWiGy/M1e6OrzbuhfV125yrql46NwuYbr8nRMY2pmkeInWuCZeaTB/9UtgVhk5MC5Eq1maSsFcZcoComra8zgsc6xRlBNMiU9lJxx5hMgBBCCCFLhthxgeEE82kYmdjbefDBB2NITDK4xHWJeODYRAHIAw3/tZMnT+aelI5ToyfnODWkC64QMa6t42+sFlUmtNR7N+KiJo6cK2JsqhM6s926kjVagIq6ZKuPLpg6DoTNjzS6nQ3kRLFCKkFzX7DQxCxn8biePaiBQoXZi6idI2330rxvLcNBquHJD0vvEJIJ/CTFDSGEEEKWArnzEiw1cfnaa6+NnZhgpXFpjBqfXM+ytWbU4JuiQTjICQ11JPXeVEwUgHXMfkY6E4WDH15XWG80K5orlEjdmOHz/j4LHl+32KSpz5nVkluaLW+oLmolcpItREbw1MQZhM3XX1M7m270hXUIXBNjaP40CJpjUrmNmTgZb+Yz1spiTlA/q9Td0cxuWmbrGDc+ihs3c73IU0wmQAghhJDzBpKQuY0bNyLTckwUgJWa0jlYarJGQf8s/fgd92kUNWFHXR/3hP/a9u3b4wHbtm1DajUrcJwQMgnmjenVxEK9M+4Kly9rWbHiQ8WMFUX4p+dSJjS10rjKxateXiWksmUmCyQrPAZuadYQEjcfe0F0RdPz+vJ6EkgGcN83TdpmsQcUgqPmOlZZVmqD4QyZutI642Ym1mJjEgZITSwmEXf2Yekdf6PIk+8TQgghhJDFBtnPZmZmpNfreWRaPnjwYOyspAzMscMC9zOImmSpkVHuZ37Xrl3RpIOBN3UlRvHUwTdnZ2drvSm6n5GuDISDywKibmfwNYuN2KlaTKQQI9my42rHqDgprTd5vdhybQVlqL6S6+yyQLINIK6fGQibXISrXOdAzHL2yJ/G5ADFGcSYpqQmYIYwIsu5QgcV8TN23jWU12i0GZTZe/znRc5+XgghhBBCFhMdcwYWGlhqJHVg7OCbcD9TSw0O0exn0w3lDfpkqZMFNQSl9Oijj5aDb8b1QvczMinqZgWB4AehIHmVVF3yuD5ZJLzN/+xczbNKjRbqeZWNHOmQJIVCeT7pgSRtvB/eX6TIduajoMGxg/olW40daEYvamaHwEjqrvpodZ0BJAO495t/OshyJmWFxaxLB8V5a2Xpy7Dy6hfLIkMua/jNwlVWphpNfnfmulwQNn79vUIIIYQQsliopkiJAhBT4xHLD0uNChvDoBeV0joPiRozRk3cOQXjZOB+tn///txD2rBhgxw9elQIGcfA0JHEgQqVLCacna3EiDMROLnP7QeOYEmNuGQV0WE3a/YKNWjk7cblKgmLShANjvaFc2UtWYEZeDOWphnZ9DxHdwzO+4yBsIGr2X2P/JlcPTsj9YAgKzKcDJtO9Cr61cX07IwfPjaqwH5xrKtuvi1WC/XWhFXdOXfm/nCpqDNTthNCCCFkUcmdIpv9zK5P3mSxk9Q6To2OUZM6cvGfYK3xxUlcstJEQUP3MzIJ9eD/gVtXdvMq+uBVLEvdxcyOYaPz+Xgtq6c2GnNO06l3ZQKC+kbj2iZFIgMbq+NreiHOQ9h8/TUxGUAUNJrlzFtTkLnY0t3MVdddrfBSU3e6fsgFTYx1xpihVKnZmBx7roY4HHf6L4UQQgghZBHJnSLjfuZhpdH4fqBpna2rf2NMzaCz6KK/WvggUAfj1BQ9rwq6n5GuOBPc3pgAQIzVwxoXVAdIta8dXLN2jrxLlapZrTa2rNhx77khIdUaY+N8berMzuUx1zy8Ve59/z6T5cyiAkPnXXVgTVy5ogL2mDR1vjhW6jevLCvH5KR1Q9nWXFX+7H4hhBBCCFksgqEk90hgQNm8eXP0EgsgE3Peb8+ePbEz5cyv0mPHqXnggQfijnv37o3T/fv355Ol0T6dENKZZHkpLDGDdZV10ek2K2yM6KlmdJ0xWQyJFytYNNA/CQLv6+fJUz8sdsTVhZGtp5lec/Sg/PX7f0Gu/nhP/Ee2D3uW1awkrnJp837YgiKFdUeMSmtreua+DAkh698no+ZF2lNAE0IIIYQsPMZQEjshyH4WtIfoODWKZj+z+44cpwZcd911HpkGpOi5wSSUgnjY8yGdaLKIlPNqJcxuXlaYiAx5aWWXLynSNDf191UoOKnF5Yu6k+X9dOvozGiDE3pjBHGyLQmabUcOxl38p7eIv2N7gwhxUgveUVOSFFeXrS+2+VUuYrWKtY1HU6u3WolcfZtvED3T3ymEEEIIIYsIxqmJM8n9LFpqkvtZXNaUznHnQchMq6Wm1rNSS02xXjOfETJv7Hgu1cvmTQIAqYubJHpsFjKrB4aFUl3sWDkwoG4BymLK7OWsZUbLlMoNDfNXR0Hzi3J1EDRROiSh4v8ewuY6c64GQZLjXZyt0lBNawE8Nde1cltZfmGFqRcqTYkEPEUNIYQQQhaRNE5N7OAkreHUS0zdz2wSM9uHHBI1VvFgEQfu3LnT7uKEkLlirBp5WosRsdPBAJpZA5hQkGq1dV+Thvn6qXXG9Uw5ah0SGQ5/kQZRNGSxEXlmEDL3/PtflGc+9s18KdU01PLTV1WuaDaQP27umQrWXdmGXdd8MRWzf1FubV9fr7Ceq0wgoMeu2CkydbUQQgghhCwWx45h+Iuqy4aUzlL0hoIuGXQHc9+tPfuZd65Rt+TeVjhBnNm4cWNM6UxIV6p+u6+bBLOw8JWVpBhjpRIXvtkYIZITEegos7XjnJQOYLX4mDIRQK/XLHKqMgfxOVdHQfNLcZpMSDWDi0+iIVpsIGxqCs1YaCqvN6nX1EtDgI+pqx5fiBlXHGstRL7FypNETn/Nf5SLgRMnTsg3vvGNOCWEEELI+QWWGjGhLoncYYGh5f777y9+iR1MmwbfrHUIQYqpiZu2bdsGM1Bc6PV6fnZ2tqXbR0hBISwG2sYEydtddcwYGfTbbaiHLU4H6PS5DDfkihbj702Wr6wlsrYYjDfjIDyQICAVqrJLy3CFZpDkcvaff+fGJGgG61ItZMg6gjXBYhO3/sSDYse8GbpPNYVjfcK8DLuZDZ+nmm8oY0jomPW4G2t+84Kz0kC43HvvvfLQQw/Jl770Jfn617/eKGTWrFkjz3nOc/Lnu77ru+Sqq64SQhaLf/zHf4zv5yie8YxnxHeTDPMXf/EXstBg2Ap8N+C+n6vvgy71vv7662M9CLnQGTUGptEkoObe0iRqXGGpyb0fBOjAnw2WmrNnz9qsA4SMx1dCxI67qX1zr2t8eZCrCRtX7mLLMANl+gaRU/Oysn19PS7Wb6B4tK6qAeqDfYo8Mwiaj7/vl+SZjx3MY1hW56msS0NCA8kDsP+PP5BWqWXFGfe0dO3mNgySEpiKa/21Qr3CilOWURVUF0e6urchCJrfEL/6x+VCAKLljjvuiJ0FiJqux6BTiY8CcfOGN7yBAocsCnhfP/axj43c55WvfCVFTQs333yznEv0h4+XvvSlUWQs1HdCl3pTzJKLhRQGU+vAXHvttTFZgAUDcGLoGUndt16XsnUmCBqHDATg0KFD/vDhw46Db5Ku1DOZWRepRLIQ2nTPVQplPxTXYj/VOYwdKFs8fK0OtUQCYqqR6+ayW1dVtK+VjRiav/rtX5KrH3s0lzCwCOUzDZ3eV5cp/lNB2PzhdilqINIW/F/bx9XVWW7OxYnyIUYktbix+cveKv0r9l0QggbC5Pbbb5eXv/zl8v73v7+zoGkDlp2bbroploeOx3zLI4QsX/SHD/2OeeMb31j7EYQQMj8QU6MxMkgU0O/3oT28zX6m7Nq1qyZ8miw1PqieLHY0ZZpuQwaCFFPjhJAJyS+NcxpaU7h3JfetbGkR61+WynBSavi4f4pPqRk8YtmVdcXWw6d6DNzOxOgJPzhHLs+JNymQnxmEzP+ZBI3XOmVXMiuDKpHlU70qc1OY/furBud/44P1m2Rdz7I1plhvhF/ND8+WUUvb7Kv7la81/LNyp/TX/ccLxt0MlhkImXMVI4Nf0dGBef3rXx+tN4SQixt8H0DYwHr2lre8hdZcQuYJBt+EwQRx+xg6BuPUpE21wTehT+p9LNeY/SyacpI5J69O03j0I488Yg9xZqAcQkbixQ9ZYqSwtigajB/npXIrswaLSkf4YWtLLsfXkgyoEMjLrvLiqowxRXlpX8TO/J+/ZSw01jiSijZ2Ic0mWFlEqksfFB6Ejf/QdiktSu3xMoU7mZqH9ALENxxbmrQgZr5X/MZ7pR8+F4KggfUEHQv8enqug/4R74Dz4Hy02hBCAH7wwHfCfffdJ4SQuRM0RfzZFYIGiQI2b94cOy8Ng2/qT745wVlj9jOdT2Ydp6nTcODWrVvjPE60adMmv2HDBk/3M9KVLEqcl6FgeiNw6sLH1UVIdgvz+TCxRgkt34k1nFRlDRkZfVmF6pxS1QUuZ//Hb94YLTWafMDXDx8uWmvqZXh8Sz0QWdE+tF1q6s6X5aliyjWUYdOLPSBZdTQASVM3BwHjN/659DfdJ37VTrkQgHvY+XAB0V9oKWwIIQA/eLz1rW8dGxNFCBkNBt+EpQbuZ2qpKeNpJHbD6qlc28apwaxLGQP6mmkAvmwHDhyI8xx8k8yNyoVsoBtcFih1uVE3FFahIJV6sVYbK2xKqlTRLpdVDy2pBEy1rn7+b3nsm/KffvPtMSmAKTnVwUgsY63xQxWpFWmOU4vNc6Weatm4kBm3NWkM/hepjUuj90bd0tx68Wt2Sf/KveIveYVcKKigGZct6lyB81LYEEIsiMFjnA0hc8ZZAwvQeH5LER4TabLU6KxPB+QeFhIF2H0RvCOEdMVVAmMQ3+5F40BqGffSfi5P63LHDthZWWaqeWfLiTPGEjMkaAbb7Sk0FkYtNt8ShMxf/sbbs4Um7tOmK6T94n1loskCSOOG4uRTyRXtyel8TC6x9M/LIseZfc1FZsNNEDNrg5jZ8t/D9J2hxa+XCwUIibe97W3nfYwZChtCSAksNvxOIGRO+KNHj0b3MwXx/OVOKabGeZOKuXGcmqSQEFsjN9xwQ7TKPProo84G6MD9DBnQsN+6deva+3KEZKz9oj4WkhoXNH7dm/286dfXUiX7yhCR+/i+MoJ4G1efEwloPaz1ZlCPbNgQyXEwzzx8UP7i14OgOfJoVY73Q2PWOFOGmAD/ml1UnEgRX+PyTqmiEDZfXSPulz4nctkZabM+5SwIjVabwXp/6U+IXxeEzPTVcqGhMTRzsdDYsWgwjw+EET6w/OAzqVBSt5O77rpLCCFLG6RFHpcaWb8T0LbxmVSg4FhYbD70oQ8JIWQicucGWmN6ehqx/F5TOmPwTazfs2eP+GJsmem2AlMmARcOckHY+NBAq15m2I6BNyVZeo4dO0aLDRlLzb1LhYu3KytB44xLVzYVeiNWCpexcnDNssuvQ2lm5zcMrplcu7JYyhaOwfm/JQia/+973x4sNY+2Sofy4qqhN5sDbYaO07p5V92Yr60V/54XiLtxr8ilZyVnf1PXs6E0z4U1B0kA1u8Sf8lL5EIFGc4mFTToxCA7kYqZUcB1pMt4IRaIIdQL5yCELF30u2AS8H1z5513xkF8uwocHfOKY8sQMhmI10cGNA11QTx/EDS5YwVBo5aatCpuaxynxiif+NM6hA0WEFMDvzakdD548KBLJ+Y4NWRynIlH0c66JgEwcTPFIXnqnAavVCmgrSeWzao27HJWe71r59Q4nyho3vPLYfpozRhSxaTVZYv3UjNE2dj84iqkCvavXN20DK9rv7YmCJsdIidXGOuSDI7NhZspWPEd4jf/jfSfft8FLWggNCYZNRwpVvFrKT7oXHQZkRv73XbbbfKJT3xiohStCzEuDiFk6YGBL2+88cb4PYKBN7uC7wRCyGTA/UyKHhQsNXZdyn5WM9Y0JgpwTnMECKw00dSzd+/eOPjm/v37NaWzR2YCKCmmdCZdsXEsOZYlJw2QLHCMPqnFu5ShJZLFjBEo9lgzX18QE68juS74IIbmz6OgOZhratHxa3JCACNk6s5mTnytZuZ4adYlzsbefDUIm9tfEITNtFFl1XVnq01vfWiMvyn9qz4XxMxOudCZpJOAzsfdd989519K0ZHB8ZN0YuByQgi5MMF3wvve9z55xSu6JVyBpeZ8x/0RspzQkJagMWKPB4aUAwcO6OCbcR20iWjkwSAsIK5vShRgu42w0rSdV13QCJkIk7RMBiIkB6YMYkxyIoDq9VJR4otyqjIGEsZmOKsJIVcJpCxwnBVWAyBk7n53stAUpqIo+NN/vjp1bbfKqOIqlzdr6UkHeD/cdLJAMokQBsImWGyemBK1TOV7M7VB/Pp3Sv9bkATgrXIxACtNV7czdDrQ+ehimRkFjofVBm5rXUAnhtYaQi5s3v72t3f+bpnEskzIxU4SKDmmxrJ69WqPjMyqTao460FHbLq9TB/1DdQQDt6xYwesNTZUwR8+fLgnhMwFjX3RUBgvtaiXaIFJ26z3VU3ceDHDuqiwsbaatK+v5k3xA41gDB5bDx2Uu1TQuGy1rMp3rpRZ+SK8EWq5LkUoTC3Yx+xtLi+v8k6lk8vCxr19r8hls4Nd1v6vwTqz64LKZtaFrlYauIxBiCwU6Lz89m//trz61a/u9KsrOjHnIrYGYukzn/mMPPTQQzmI2YJfkXHtEGBdXe3OJYgzUpGHupb3DvV99rOfXUvccD7rifsKzwNbT60b7mebK6ImmRjHUo6twDXgHuA69HlZ9N3CNZzPZ7VUwPW/4Q1v6PSdhPdqKWLb57jvE3wmccW1dG0fer6FoEtKbU0aQ5YWyfsL2c9i1wiWGoS+7N+/f9ih36bDlWZR48UYbFQNwf1MUk8snMA98sgjcX79+vWu3+8LIXPBpch/n6YqcAYb08uo4sCIEN1ez62sO5jNWeIYQWJcuFw659ZDj8qeX/tl2RotNKKtwJRrTuvMLmm/5NiZ3ekGeQe0BlbYOGPc8Tn5gdSuwpvcAMZi8+4gbHavkf41H7wgM5qNYxIrzbnIOIQ/uBAqt99++9h9UdeFEjXoENxxxx1RKI2zAJV/yNEBfeUrX9nZVWYhQB0wqjrqO04AlvWFm9/111+/KPXV+4q6jupw2TriXuK5lh0vHS9pFHh/EKO1lMA9wHPCPRjXCVwK79ZSA6IG79Ck7/n5ZD7tEwIAbRTPfRLx0aV9ANxPxC3NF1wfslGOA+eiqFl6wP3s2LFjucuG4WMQ+gKCFnE7d+6EtSZ2sJAsYNeuXfkH6CZLi2tyjcF6BOlALSGmZvPmzdk/iDE1pBNDATEgCRnn89gwdfczn606talxJ9NDBokHfJVEIK+TuvtZNrIM3nMImY/+qhE0UlmC8ks+5GNmL8Grx1m5WlSZ1dzVhtVVLS5HXemgavK5/8fvEP+Lvyv9b/3ERSloQFcXDnQ60YE8F+CPbpc/5nNJAVuCDsfNN98sL37xi+ecgACdEsT4/MAP/MA5H+VcOy74dOnoNYHOyGLUF+eB1Q33tcsvyArewZe//OXx+pY7uAZcC0T6XDrdi/luLVXwS3+X75rzNTiwBc9rvu1TMzzivcF3U9fvpC4ptMFCuemhfXdhklhJsnikjMpu06ZNsQt08ODBOA0axCFhGdzPAARN+HgdagM0iZqBa3/YoRytc+XKlV7VUsp+5oSQCXCu5ZUx/mRRlFj1EQ+sdqwnFSi3ueI86pbms7gZbB9Mka55z6/+u2CpOSRDJ8spngfxMd5WNc9XtqCaeNHtNYtPcazX+Bo9pxsu4OlPF//v/q3433lvEDbPl4sV/PHs0vGC4DjXvxzjV8ouzOfXWXQS0XFYqD/y6FRpB/RcxPugkwSRsFC/SGt9J+k4dQWdePyKO5+OJspYrlmtdIwnXMNCBLDrs7pYB6CF+2QXzte9wTPGs8bzWUiLEb6bUGZXAdFF1KgL5HzpUgbqc65+/CLzZ/369XL48OHceYJBBYkCzHiZsNI4E1MTZ4bczzRAB51CHXwTLmhQRwr82zQDmhZEyFhg0fAtr0uOsdEXVP2v1NJRDZTphw9Vn8nKBSz5ePkU1OKMxWSw3cmak4/Ln/32bfKMRx/V6qXTpTgX72vxMM4E5VRuY5Wg0RNbl7JKz1SJA3KcTyHwavfm8stEXvPD4sJH1lwmFztd/9Atxh+qrtaarp2dEnSWz1WHGR3QV73qVTGBwkLEd+gAg107NpOCjhOePdwJ5+trj7pCzCxUxw7PSOMqlgv4pf1tb3vbObEcqCVgIZ7VcmIpd4zxzuOZTGKNnAQddBjxi+N+TOoafzTfcX1wfJf3+2J2m1wGRG2BDMtHjhyJK86ePYsOUy3WRa00idgBGxI1SaS4dEBch5TOau4BEDTISIBBcVIuaULGU4/nat2lDOzPQf311bWkARqjonjjNOZrQf2DyZonH5c/eddNsvUrD5uy7Kts0xbo4Jj2XHbXQUyQaxA2ao3JsTYpXmaYlIjgsktFfoRipqRrp3kxOpjoyHa11kzKuRQ0inZ0unREupRzrjpMCjooC9FZfve7373gsQ34BXy5DKyo7oHnMr3wQj0rMn8Wq30C/LChSSTawPcmto9rgxjcdD7xiAtpOSLnh7Vr1zo7To2mdIb7GTzGMLyMpHgaGXTJ+mnaOE6NChqnlhoVNCgQo3pit5TOmYKGdMdkExtF5Xmm8TGSg/Fr7mMmVqZ0K+v1KiET43VM2WuefCIKmufu/7JUOdNkIJysokpWIudMMzHBM3GomrxsHNRq498YS44pwsbs6H7+n79M3IffL+6nXk9BU9DllzfN0rNcWQxBY0Enfz4dHnRkFqPDBLSzPFcXHtzXcxX30SVxxPkG9w0WmsUYL0Wf1cUyNktXq9diZ4vDe7lY7RPAVXTcM+8Sw4JMcfNx1esialAPup4tXco4/eQZlt3PkChAt6WRN7MbWpOlJk537doVczqnAW5ygTLoh/WQjUBNQ0FVCSFdaHU/G9rPLGTTjM/WkLQ42Df/I1XGMV+NP6Npo7EPBM0f/woEzcMyGOjSJCIw584WI1e5oVUZzVKlnLUQVUJHJc4gBbSx+PiqftXJwv8v+HbpBSHjXvAdQoa5ENLljgO/Xk4qaCDiXvSiF+XOEjpXmo63C7iv6OjeddddE3e4UNdJXc5s2mZNH4v6du38auzGpJntcD/mIhZRT3zw9w1/ZNvuK57dUhc2EBmTuJxp2ma1tmgb7PrM9VnBzfFCp2t7W0xRAwE/STyePm+4zGp/TtsnnnmXNor9EVs3ysoCCzfa4rjyYK2Zi9UdgqjLe84EAUuekR1F6zmWwg7y/q0pnXUB8TRB2PgUV+OffPJJd/bsWX/q1Ckh5FxRxfkXwmBIqUhNlPiGtqACZe3JJ+TO3TfJ9iBoRKSWkaw2Loxus8LI7JvPl7KeuRFudTYOJx1SubptuVJ6N/88xcwYuv7auJytNPiVsytwGcMf/LbrRScEVpgunS10ANDJmCSN6qQiAZ0c1LetU4fOV9fsbhAQ6DhN0uHBvZgEdPBQ5yaRjHuL85cuNIv5i/ik4N52FTS4r7j2tmeFctBh7pI9S9NEX8huPl0TmMw1vm6udG2fo951Rb8julg6x4kaHRdm3D3DuzMXUYMxvMZxLt2HycIA97PwQ1LfxtRI+r14x44dHmmdpd7p0mFofOPgmSp6NKYmCJqaz9Ds7KxDPE1yQWNKZ3IOUROMFTrJacxVcTOlI6TruSqdswxiaO685Sa5Lgma4dMUcqgW2DOyZrG9eF+W45KbWYPYWXu59P7Nm2Tqz/+QgqYDXX/JX66iZpJOJ+Jg8Bl1rfgV8p577ukcL4OOyCTuHpMMgAor0KhOMkAHA9aXrs+vyy+9io7J0QXUEfXAp62Th3uL7QsxlsZi0FWA6rXjukY9Kx2rCc+1S8zMcs0Q15WuMVqL6eqE971roPyod11B3fGd00VkdMle1iVeBmXMNSX8OGilWfqYwTdr6zGszMmTJ526n6lGUUGDfxrHqXGDmABXpnRGcA5SOqt/2+HDh2XDhg3d/IkIEekYU2P2cfUMYjaWJi9Xi9X2JCkGSQGCheaWm2X7ww9Lk/dbtgCZeBk9ySh3OVcKnpwoQIq0z2l5zWXS++nXydTdfyi9H+EvRV3p2uFfjj7S+MPdNdYDHc5JAvvREen6B7xr5xOd5K5uLZMIFTy7rsHlOmhmF7reW+3Ud7UqoIN3LgZ4XWi6ChqIlEksKvq8xrlUoXO6lAaeXEgmsVguZke6S8cezxrfD5Mw7scJZZzVUl1QxzGpe2tXqxmtNMsHWGqQlCzhUwhMRENjyj5aU6KA2rKNqcHmlChAB9+MiQUYU0M64VynmJqmfVwhVAbTyopjpENcryIHguYj73yHPPfLDxvLSj1Av0pr5kwptj7VWDK+7XitgamS91W1poKImQ6WmakgahyTAExE11/slmO2pUl+VZ2LOwY6Ll06EF1GFwddO3EQYJOKTNSzawxGF1GD6+kqwOYysri67ixVuo75MZdnBfQX/HFcqANzwq2x6w8ui+mCh+cJwXnrrbfG9xOCqhQS2DYp6jo2DsS1jCuni7CYVAx3HceMWc+WPklTeFhqDlVjCGLgTWQ+izE1CImB4cU5O4hHc0xNCrJ2cWCbBx54IK8OBbpQYOy2HTx4EILIzczMCCGdKMSK6yhyhsheZWlcGkydpMB/SeIpfHk+8YR8+B3viBaackyYHA8zWMiCy4Ty5HNoXYcup6pOKquay05oL/gOmf75N4n71v9ByNIFv/J18ceelHF/vLt2uufaeUYHAp2cLjE7qMs44YQA3nGg4zDX1NroNEHAjesIa4d9VAela6cI9Z3rr7d4LrhvS3HQyS6CGfdvPr9co8M8Lk0v3pm5dKKXKpOOzYT3eTGtyJo6ualtaIKOudanS0rmroH6436YmPS96WqhIkuflvEvcyrncr1U3bDG7GdIJBDTo+3evTuqIR2n5vTp09Y3J4JRP/v9vhAyFlcGzXcXNM4OVqNiyCVBIzI09gssNH8IQbN/fy3zRZUVzVXpoqV8rX1KHiBFVjRv4npMS7I5oQc7itvx7TL906+V3g7GzMyXxUpDO0mwfldGdRi7ukvMt1N0/fXXdxo9flxwblc/9/laL1CHLr/uo76jOildO53zra9mdFpqdLn+hbA0oYM66j3uIkCXA+r22CVJgmUpuTt1tbaca/AuaBbENiZ9b7r84LKcBsq9mDl27Bi0BYwmWVzASyxYcPqrV69GooC4DpYaDZOBboF+abLURGsOrDRYgPsZhM2OHTtEC8JAOGfPnkXCACkDeQhpZQ5WGbXmeF93NauyoYnJwTxYhoXmQze9I6Ztti5n+bi4exXcn8NpRGqDdorU0zs715JjzcTSuC1XyvSbXidTP3i9EDKKrhmz5tspUnePcb+ManrlNne1xRrUDp2uLr8Ij7Osdbm/C5EJqetI6YvNuPujv+jPF4iacSmt8SyWkqiZJIU6fnxA/eeS4Q4/SFxI1oGFtEh2+U4a98OF3W8cy30cs4sM6BBkP3NTU1N+enpawie6nknqngWN0jeDb3rtnw2JGh3IRt3PIGgAUqglfzaHRAEI3kH2M1pqSGeMpcbNI75mUIDkIBZb1tonn5T/eNM75TkP7x+EyiRR4ozokZStbJBUwNUtMNY2k/Ivm8PiSV0exKbaHXEyUz/yCpn+0VcwZmaBWexB6xaLLlaahep4ooxxHQh1TWn7w991rKCFcLXpImrguz9KhC3W2Eb66/dSSuus92YUC9XB6/K8x8VZLDaLkcAAneilHHM1CXiXIAInGftmHF1c0HC+LpkGu4gaWmmWFf7o0aPWtcfBUnPttde6lStX+mCtiYaX8IFO0WFqorBpcj8bcjGD+1l4qWHy6elJELyzadOm6K7GlM6kE77uetZF2DTt42pCpBpd5vIgaP7DL0PQPJxPVxuMsypAa5HrlEVOtaW2m5SiJ5Xr1l4exMwPUcwsAfAr4nJKFtClE7xQHc+unXd0Pucjahaqvl3LaRNhix3AjXFIlpKo6Xr9C9lJHcUkA39eKCDpxXLKyAjhoj9s4IPv0/lYqcahA36OKrurC1oXgcpUzsuK/LN1WvYHDhzIG5HSOXycETTSaqmRgbEmW2qSEoKVRk+SM6YdPnw4lsLsZ6QTc4ipadqnts4NglvWBEHzB1bQiCYO8JWwEakSpbmBM5orB8YsDTXJyqMDgNo6TH/f/ywrkQRgy5VCzh3LMatZF7r45S+USIA1YZwPOxjV+exS34V6Vl0HK2wTNV3dZBZShC2lLF9dRMRiplteiokUziXjxpI6n6Adq1DBjxiY4n1ZjNjFEgiNcYKpS0KQLgkxlmPK/4sZeIEhERm8wjQDGsapQVpnxPgHfMp+FrdpX68x+5nN9ARBkxIFREGzbds2wVg16URxHS015HwCl7Pff3sQNF9OgqYhwL9EcwR4VwkbV7iiueS/5tPOKorcC54nq970Wpna8e1Czj1dfzT54he/uKwEUJfvzYV0vcMf9XEdiLbOZ9df2hfTpQk8/vjjshRYai6S56ODSgaiHhaapSZoNL053LSW0rhBiDkaF9s0rr5dXM8mGd+LLAl8EDQe3mAAcfxTU1MC17NxBzYmCrALKVFAXgdBA5JyKk1EhCwqa54IgubGd8qzv7xfNJVyjn9xmmYZDFT8IBzGV+5lUbRozEz9NdaUAS6t7121WVbd8jaKmUWmawd3qf0aPK6j26W+C/nr4nKLTepiWZrvD2oLdX+XmrfCxejudb5Bx/ntb3/7kmpnc83Ytlig/Y2Ln9Osi233tYtIYyrn5Qeyn/X7fXfkyBHbMctj1RRjaObxakaNU2PTpUWLDEw/yHp26tQpQbIAXR++0J0QMkfmOl7NQNDsioLG13zHXD3Hs/F6q52nFjgj0qrN11wWLDM/Kitey197zgddrS/jUhKPAn/0vvCFL3TeH39I3/jGN47ch26586OLqFkqHbULNZkFGY8OwrrUOs74PsR4OkvdajcuJThoG0NLY37GlU/Xs+VF0hRx8E14hSH72apVqzyMKikDWkTTOQPVLb2mAtX9zB4A4MsWCnVJ0MRdURbdz0gnWoTLXAXNB4Og+bb//nAt3bK6jJlhNfNAm9V57LbKcqPrcmKzIGZWvulfyWX/6T9Q0JxHuo6toCmJF4Mu57n88suFzJ0ulqylIiZoGbl40MFakZXr05/+tHzoQx9acoIGLl1vfetbF/T7sG1Az/nSJaV6m4tZl7FpmCBg+aGaYtOmTbE3Bs2hXmLKoUOHnNEo2WOsMVGAFC5oIJh8okLatm0b1FLcvnHjRqRzdkzpTBaaUdYbFTTP/u8P53U57fLg6IHLmNNtrhh809eP00CzvJ+XFf/yeln1b3+aGc2WCPhjOu4XOQ2AXYwORpdsQOMsNegcjeu4L2RneTEE32L/wDVfaxju70LEYS21X8O7/DKtySMWg6UW6wZXsUnHJ8I9Xcx7NlcmGYOnDR3TBdeMpB0YwBfXjXIXOiZH09aPKrdtDK1x8TQLMQ4VOT8gpkZStw4xNUHYxHnVIkgWgE+y0MTkZjiuMaamTJEGMIonVp09ezau1IwEyFBAyEIzUtD80sBCUxv3xhznbGazIYlu8zRXsz7F3EztuE4u+YWflqln/w9Clg5ds2Hhjy5+OT3XdBl3YyGChRcyTqiLQGrrfC524H7XcU3arGFdO9ELdX+XWzwXQEf11ltvlYsRjeW4ELn55psn2h/3QV208J11PgQoROYoUQNBg8Fk8c4qaHPjBBatNMsbGE7ggpaoxe/fcMMNHuNouvogg37I/Szlc64JGoDBNxFTc+DAAR2nJp+A7mfkXGHfQwiaD/zSLfJtSAowZMlxxuqiKf6Mi5nZ06fBNytvNCfTO75dLvvgrXLZ7/8qBc0SRH8pHMdipKnFH9gubg/jhNhzn/tcGcdCWWrQAehiTRglXrp0dhZqPIuuf1Pa6tvVgrNQ9V1KY9QAxvhcnCCteNfvDMQBqfsc4lUgAM6XRa3L9/snP/nJ2nKX73laaZYtsTungsaGvGhMDQRN3DGPNTiYGRI1KUEAfNWq3mE6yb59++q9yMH+kwdEECIyJJzLZaAvbBY0KYYmHZClez8pGO/rsTO5xJqwcaLCHRnNVt/yVrn8D26T6Rcyq9lSpWtcDZiv68U4uqQQBeN+CV5MkdC1nFF16mKtWaj6dr3Hbe8E3pcu97frecax1ERNF8G8lFL7koWhy/uMtnHXXXdFUbNUxC/qMc6qUv6QNO5a0f6Z9Wx5ookC8IFXGNi6dWt2P8MwM5r9LMdTp2ljooBdu3Z5fDDwph6YBt9ETE1cxomQQ/ro0aNCSCcK0VK6OY5KGPD7v/X/kW+1MTS+Ne+AiFRWHC91m+VgPvx7+WWy6mf+laz509+SlT9EE/VyAH+Eu4DO2rkaCBHWji6iCX9Qx4mALi51OqL2fFkIIdZFVGr61fnS5ZoXyhI23/uLd22pxdTg3ozrsJ6vARfJuQMuWuNAgoOlODDoOKtK+V047lopaJYvwVKfAwcQ5oKYmuQlljuRmiig7De2Zj9D1A0OUBNPMPnEI+04NUgSgHmmLiWdmGP2s12//n551v/1GSkzLg+0kBVKgwFnBh5oKmyGrT+XvPlfybq/+qCsDlMmAlg+4I9UV/eId7/73eckzgGCpot7Rxe3B+sfPoqFsCYsxFgOXf3TkX51PnRJ0wpe9KIXjdz+whe+ULowXwE83+s9V3TpuC7VupO5MU6kzjdw/lzGjuH7Z5wQ1+/CLj+ezDW9P1kSuA0bNsQOnFpq1KCi3H///bFHmManQX8vTptiahoiqQcnkaKHqP5ujKkhc6UpfssCQfMv/vp+qdIwV/tW1hrzauo6bzOoDbat+L7/SdYHMXPpm3+UYmaZ0tVagz94GEdmIf8IQ9BgELsudBnBWrP+jAMdz/lcR1c/+3F1Rie5i7vKnXfeKfOhq/vgOJE1iQibq7UG93apunF1uf6FEMx4N9EudLT6pZY04WKhSxuf7w/Q5/pdHye49H0d90OEZm8jyxavWgQGFKBJylLmMwcvshQmYx1xmi01ttQwcfBfsweBzZs3x64jRv0UQubIIGi/2VLzzl//XfnBIGiyy1hK2jy0e22sGVd7wyFspl94naz9D7fKmt/6ZelddaWQ5Qv+6HV1K8AfeQibhei43X777Z072xAHXbOFdel4QqDB8jQX0MHsWu9x97Xrr7y473ONa0J9u1gPuvjLT5LhChmjJu2MY/+5PpfFoMuzQie1q1BvA88a7QPjoqC9vfzlL5fnPe95cYplrGf8zvJnkiQEc2Xc9yHO3yXrGa00yxvE1MzMzORlTRSAZGUJD/czKQQNGClqYNZB2jTkgtZVOqOuZ8glTfcz0gnXXf9GQXPPJ3NAjDOWGDOpz0dhU23pbblS1v7Bu2RdEDQrXvg8IRcGXa01AH8E0amaS6cV4I/nq1/96ok6fpPUDx3PLtYPCLO5dD4xonhXK00XIdbV+oGO7qTB83g+6AR3oes97mIxAyqAu74jeC9e9apXLemYlK6WwLk8KwXHtYlQ7XwuVDIGMpou7XeucVST/DgyH7q4GOP7fNx3GlM5L2+QgCy5n2U087JyxRVXxGVYawZJm12z+5nkQdg9dpakhmTHjh21AmES2rhxY/T7ofsZWUiyoJFhH8jsajaUKCAF00AAXX6ZXPaLPykb//MHZMV3UcxcaOAP3yTCAaDjhV+O8QcRvziO6ryiowYBgU4uPpN0+FCvrlYagI5n118VJ7EWoeMCQdP1F/Ku93OSUcVx77qeXwVN17F0usYFYL+ubig4N94RCOCmemugsr4XyyHIvstzVVfNScWH3otxMAvV4tEl5nBScYL3o4uQWCjGte1x38d41yb5DiZLD4iUo0ePOoxToyCmZt++fUhaFndBvD80ChKa2cHahwbfVLXjqkAHv3PnTqfWGhSMZAE6+Ca201JDJsENjTFT8Y73/q78LzGGpqIpuKs+kxbXXCqrX/cv5NI3/EvGzFzgoLM2lzFp0HHTzpuODo4/gPjDjR9n5hMPMBexBSBqusbNoEOC/WCBaOoo4jpwfV0TGuj5J+kEYMBGWK/Gdeq1s4xOCu5LU4cL+0BA4tNVJEx6j5HtqasFCOBZqPXB1nk5xorgHcG7Mi4GQTuuo56V3XeS+LK5tAkyN5Dxb9x7iueG770uzwXfrxD5iyVoAKws87EKdbXOkqVLyn6GuP3c/dMkZYnoegZRo/MqWaYbyquCF8IUKigF49htOXhHaKkhHdk6NZiOEjQ/eM/9RVYzX/da817Tmw1eUj/Yfskrvk/W3PiTFDMXEe973/uiC9BcO5vonOGzEJ1VdALnOjo7Ohi33XZb5463drp17B4VJPgFc1L3EtR70k4nzodjYDmapL44zqa6Rn0ndXtCh2fS7E3o2EO4zcV9r8u7ofdw0pHcF4u3v/3tnQP49VnpyPLWyoX3Cs9rkh8SJrGqkfmDjH9dBgaGaIDQxXuL9N/6nPU7EWVo4ofFBnVBm53ruWkVXP6sW7cu/vg9MzODbl5/8+bNbvXq1VHY6OCbBj/oB7aImuSblpeNoAHOqqVNmzb5w4cP01JDOrF9Rfu2m2GhCYIGaMYyfQ2RBCMl7RP9dyBoJLqXXf6W18hKuplddKBTj9GwFzrL2aSg44Z6zMflQa08k/xCqe5Qc/3jr/dvLgPwQSSggztJOmQIrvmMC4P7DKvLXMBxk3bIuwJxvdQG37RMKpqBCs75xsPg3pDFAwIS3yFdfthAW4SL6iSgDS7Gdy1+vJhLW8VxdD1b/hw7dswmAXC9Xq8ftEcefDMIGxhc+tgIa40zHcShmBoE6EhOfDZYhX927NgxdOIgaITZz0hXrlvh5MWrhl+Xm98TLDSfuH8QJxPXFGPPiDeDaab0zN+1XTZ8aLds/NCvUNBcxOAP2N13333eAkMXQtAoEDWL6aqDju586g0LwGKlTV2I+4wO9kK/J3heyyF1LETzXC2Jc2WpDvJ4IdPVrWwuzMcaPSlzte4xQcCFAQwlmigAoS4pMZlH9jNYanbu3OkfeOABl2JqnK/G9mjNfuZ2795tY2rk5MmTuqyDgsRSkP2M7mekK/9mTV3U3PSegYWmeiUrKie1NEZNWDF11RWy7tafC2LmXRQzJII/5OiwLrbvPv6AQlAt5C+DiyFs1EIz3w7AQpUzjoUSjgv9niy2CJ0v6Cji+udimZsUCBqm1T0/4L6fi3uPHzEWywrSNXNfeQxdHS8MYFxBogDMI9QlWGrsSOqxa2gSBYy21FiwMwa4QZKA5MfmzSdmPwtqSgjpCiw1P3XZ4N2EoPnBe/42zruUuayy1kjOcBbf5jWXRTezK/78N2T1K79PCClBB/MTn/jEOfepRicbncNz1UHEdeAX0S6ZjCYF9+auu+5asHt0rgXluRKO83lPVGQtxwB43E88/3PxbgG9NxQ055eFFpUob7GtIJOej1aaC4fkfpZJ49RkdzT8kwbftG5lzZaalMo5JggoN8GXbevWrdlio5kJ1q1b1xz5TUgDu9b35M//22eDheZv0xqXxUtelmp5TRAzm//6d2XNv/4RJgIgI0HnF50qdLQXWtyoa8diuLvhF0dcx0Jl8kHd0TFZKFe5EhUKC1Vf7RyfK+Go7wk6+Khzl04+3ifcQzz/5RyMjGu/5557FlQ427bBQO2lAd5VPJP5tJ/zKVK7juFl9ycXBklT1HQFMi+vXLky6pBgaHGw1ABXjH/YltI5ip/du3dHEw/cz1AIgnPEZECTwY6uVFWEjOOF//x/kieeeouc+PBfydmH9uf19uW69JU7g5B5jUw/40ohZBIgOvBBMCyC2RHwPJdgbnVpQFmL3VlD5xNxL+iY3HnnnTEj0aRBuqizZgw7125Htr4IVu6acUvRbG44frHuNc6HOgMdrbxMXwv/btRnMdy2FhO8E/gg2xnayFwCsxfz/SKTg7YE0T5pe9Txs/A5X89Vvw+6vJccC+nCArpCUphLGj5Gk5TFbGiIqcH66667TmNq+nk4mobCorBJ1hoImmjNefTRR3sQNUEtoXCswwfuZ72jR4/29u3bd8SWc8011wghXTj79UflzBf3yxmIm6BqIGIuedl3SY9WGbKAaEpafPDHHcu2A6sWDKQ4xbymtV1KoL4PPfRQ/ENfpm9GJwAdcK3/UuiIa8YxFQtt9cW9xme5dowhDMaldMYzgTVrqVK2j/JHgKX4fpHJQFvEc8V3SCne8Uy1LS4VgYAfozB+0jgg3PTHCbL8ePjhh2vL11577dPCZHbjxo1+amqqH0QNMp3hN29Mo6gJhhafhpyJFp2kXXyThSVbY1TgyEDAqKuazvc2bdrkDh8+jPmpIGoes4VQ1BBCCLkY6CJq0GGEexYhpBuwIHZJOw03UmbaW740iJqNMhAwWcjgE9b7oDX6aTfdbt3UfFOigFLQRPcz+LGJseyYNGt0PSOEEHLRgl++x8Hx3AiZjC7jdpWDxJILgqYkZC4ImjgthphxdkD3JlEz8Eurgm/izIMPPhiPQrAOCOYgr4kCCCGEkOWEulktVFnjYMeLkO6oi+04mGnvwgOJAjSlcwN+7969NeuMTRbQmP1MVQ9iagB818rd8M+mTZvyPCGEELJUQSfpjjvuiD76L37xi+XVr361vPGNb+w0+vooIIy6BDPD/YwQ0g24nnWBqZwvPI4dO4ZJzbUsGFTi4JsBB+8xg1pq4j9N2c+Q+Uzdz/LqFJgjZ8+ejS5nV1xxhUNGAiGEEEKWIOgYId4FlpQm8YJfgm+//fZ5jZTeJZAZMDsTId2A2xna7TjQphZrQFCyqLj169e7Xq8Xh47ZsmVL1B779+/XlM5235phpXHwTYgZfNRSA5DSGQcfOHAgFgD3s40bN8bdhXE1hBBClhiafW2UNQadJwiTSS022B+Wni6uZ5oxjBAyQNumBW0KPzJ0iaUBCzUuFllyRJ0RBE1cwOCb0B5B0PgHH3xQCktNbayaxuxnxkrjzCemcC5SOveCmurNzMz0mP2MEELIUgKdJgiPLkB0YFwPuLOMSlWMjhfc2PDpKoRgCeLggIRUoF2qqNHU+ZPEuOEYDCJLlj8t2c8gbGavuOIKPz093V+1alUflhpJWc/S+Jl9GF927drVV2HTZmEprS9RwIQTIftAFjjhM4Xp2rVrpz73uc9R1BBCCFlS4JdfCJBJgFtLOW6OjrUz6SCVKAujshNCBuDHAMS1zQe0Kbp0Xhi0jFPTLz8ppXMUNjfccIPfs2dPjLuJMTMDVeOnm06gg9ikATijuEkpne2A727Tpk3+8OHDdD0jhBCyJIH15d57753oV2AIl0nFSxP4NXk+8TqEXIjMt20h4xkFzYXNhg0bkAEtDh9z6NAhuym6pgVBo8su5QKIHmaN2c+UBx54oCZYHnzwQddwYjl+/LgQQgghSw1YW/Crrrq4LPZ5GUtDSJ377rtP5gosqDfeeKOQCxekdIbxBfOFoAE1HeIHoiUKGywPiRo3IO4H004w8cT1GKcGQTpaDj7BSiMjckkTQggh5x0Ii8UUNjgPRjmnoCFkmLlaaujKeXFw7NgxNzMzg1mb2Uy1Rm0drDN2KJqm7GeqeuKCMfHI6dOnXRp8M8bccJwaQgghywEIjLvvvvucD9aH8nEeChpChuk6qGYJ3EghaEYl8SAXDH79+vU6FmZel+JpgDWmuJSxOa5rEjV5Z5vSeceOHRIKlP379wtyRgONp1m7dq0QQgghSxl0iOC68olPfGJB08GiXJSHThfKZ8eLkGY+85nPdN4X7Qg/EqC9QtSQi4aoLeANhpgauw5g3Mw0qwN0+uSG1j6+TEomoJnOkCigh5gaTem8efNmd+bMmakjR47EzGhM6UwIIWQ5odnMMEhn2wCdbaDDBXcYfJCumUKGkG6g3T300EOxzWlWQYA2hB/JMa4TYmfKDITkwqQp+9mGDRtmjx496oOo6WNczKA9ZoP2QBY0GFn83r17Y2rnLGYGhhrfKGpSFgFnsp/lsWogak6dOtV75JFHVPBMhZewx5TOhBBCljO2k9UkcBArg04XOluLnXiAEEIuRJrGqQmipg9RI1VK59p80CZR4ISptdpIU0rnHHize/fuuCINcjO0X/p4Zj8jhBCy3NFfhwkhhJw/kIRs06ZNfbigBeAl1p+enkZmNAlWGogZGFX6un/yLmscp6aWKABA0CCm5uTJk/7BBx/siUkOsH79etfv94UQQgghhBBC5gPGqQnaIhpO8Nm/f7/O53V2/9aUzqNI49TYAThlZmaG2c8IIYQQQggh8wLWGAy8WZC1RjCyOB1uBiEydnzN6RHl1uJtYO6xbN682Z85c0aOHDni6H5GCCGEEEIImQ8YpwbToC/iMjIur1q1KmZf3r59O/SIt5pEQ2YwbbPU5IwCCtzPwNatW+PJDh486MIJawE6hBBCCCGEEDJHokfYxo0b48IjjzwSBY0CSw2mGHZGh57RkJk2UeNqQTWGAwcO+KaTCyGEEEIIIYTMHbXUZL2hBhUASw2mSdDUNMmowTdrO4ZC3LXXXivbtm2TYrvj4JuEEEIIIYSQeaKJAJwZfLMGYmqSqBkMUOMHQ9QMiRrrdqZmHWXfvn3ZBLR58+a8njE1hBBCCCGEkAXAeoC5AwcOYPwaJCyLK/bs2aPbamEwQ6ImBdxEhWRFDUbwxFQtNSnVGlI6CyGEEEIIIYTMB2Q/S/hDhw417qPZzxQNmWl1PysTBaj7mVpqwonidqZ0JoQQQgghhMyXY8eOxXFqwqxDpuW0Ok6R/UyB4QVSxcqVxkQBEDxJ9WTzDwqC+xlAejUZZCZw4cTCmBpCCCGEEELIPHEaI4NMy5IEzcqVK32D+5kS92kap8aO2ClS92vz27Ztc6dOnYoLyEywfv36HmNqCCGEEEIIIfMBhpJgV8nmFx2nJggaq00iyessr2u01BjXs7xjUkdu//79DjmjsQ2WmpmZGSGEEEIIIYSQ+XL06NHaUDFnz56tLZvsZyB7lrW6n8kgUYCzwTiIqQn4zZs3xwJ6vZ6mXSOEEEIIIYSQOXP8+HGvSciQ0hmGlOnp6SFji8bUWJrcz3KigCBusuhB9rO9e/fG7QcPHvThRO7QoUNR3KxZs8bNzs5+bWpq6lt0/6CqUAkhhBBCCCGEEAu0gqXf738VU/UCQ/YzuJ8hSRkMKytXrnRXXnllVDLB6OJ2796N2axsWlWH+qnhIATkqKDR7SnNWrTUnDhxYshaEyomhBBCCCGEEFLSIGqOI6UzMqBJ3RPM7du3zyNp2f3336/rfNAncR9YbKBbWkUNXM/SVBMHuFCYT4E6+ST4B0E9oWL/t7XUIJlAUFRCCCGEEEIIIZbSABLESdAzx6AtsCEmBkhx/JI0SJxP2iRqFbihaWKBtkQBdtHt3LkzzmiygJTSWTZt2hR3hP/bmTNnvmYPOn36tBBCCCGEEEJIiWZTVk6ePPkFSVmYN27cmNdv3bo1ThEKA02iYgaEaV9GpXROrmfAFdtUNTkE7+hInzAVPf7441+4/PLL846lSYkQQgghhBBCwFNPPVVbfvLJJ7+wdu1aNzU1pSacKFYOHDhQDjcTEwUYb7J2S40UYub++++3Wc7ifBA0eR183z73uc99alRFCSGEEEIIIQSUXl1f/epX/29Mjx49KinDMqw0tdia5D2mlpqaXmkTNZF0gKogmH366QR5/YYNG+K+N910EzIWHNNj4SdHYUMIIYQQQgixQCPYmBrE07z2ta/9AkJasKxxMsFKI9u2bYu74JMSBfjqMJ91yjhRg3FqVNAgA5pDSjWbLzqoKQ/3M3Dy5MmP2+PDshBCCCGEEEKI8sQTT9SWT5069fdmsRbcn1I616wyOk6NMzEzQ6IGw9OUhcLUk1I6y759+1B4zIa2adOmGMhz7NgxpHWGuchWCAkEhBBCCCGEEEKUJ598srZ85MgRNYxEywvGwty8eXNcEQSNR0pnuz8ML1bQgCZLjZcxaBYCNQ3pyJ8f/vCHPx5EUVYydEEjhBBCCCGEKNAGZUKxD37wgx9fs2aNLkZXs4MHD8YFGFQUzciMJAG+Stc8cFkbcU5npvj0dD6Imt6ZM2eiIAon7AVRMzUzM4PlXrDo/Oq6dev+31rIJZdcIpoCmhBCCCGEEHLxgrFnrNHjiSee+LPv/u7v/rkTJ04gyCZ+MGwMsqCtWLGif+DAgZi2GSmdg87IKZxV1Iwcp0aSoEn7apazWBhWILVaEDP4wP0M1h+PAThx3Fe/+tW/sgWh0rTWEEIIIYQQcnEDC02pC77whS98IAiabGhBaMvhw4f70BopnTPwGgqjiczgfaaCBoxMFICdbbIAFIZEARZNuZYK7f/wD//w358+fboWW4PUbIQQQgghhJCLl8cee6y2DM3wute97oEwi8Rj0Yhy5MiRPP5MynwGtzM7vEw0vFTeZwMa3c8qa47TfcpPb8uWLVEQBRNSdDsLn6k1a9b0gtLq3X333d/9/Oc//89tmXBBgysaIYQQQggh5OICScWCBaa27vOf//z/68d//Mf/y+OPP94PoqYfjCX9YAzpb968GZYadTVTtzRJyzGeBjpFp6DRUlNmE0gjdsa0zlpgEDNlQoGYAe3yyy+XV73qVZ8qrTWHDh2q5aMmhBBCCCGEXPjA7WxmZqa27qmnnvp40AxD3l1XXHFFThIgVRhMHHjzhhtuwCdLFatZ3Jg6NFlpMFaNC5Vzp06d6oVpD2nXwvopGYik+PnDP/zDb//u7/7uv7GFYTwb+MkRQgghhBBCLg5g3AjWmNq6PXv2vOjWW2/96hNPPDEbFvsbNmyYhZUmbcY6a6XJw8zcf//92WqTxqoZmShgiKCKcrKANFaNVjIH8KgvXLDW+J/4iZ/4QjAx/YYt49ixYxy7hhBCCCGEkIsE9P1LQXPkyJFfT4LGp1TOPggaNaBE0RKMKFnMAGgRCJqUKEBdz/L2kZaacFAvuZ4NxdSkXRBb45K1Ziptixabyy67rPfDP/zD62+66aa7V6xYcZ2W2ev1YnzNypUrhRBCCCGEEHJhcvr0afn6179eW9fv97/2bd/2bS+SygJjUzn3kfnMbPPFfC1hgJ0faanZtWvX0MA2xTxySPtkrfFpEM44H5RX/8477zx29913/2Q5ICfyU5eD7hBCCCGEEEIuDNDXN7ExEWiCoA1eZVetXbsWGiLqh2R5yZ/t27f7prLNsDOZLjE1eYrAHMTPPProo70HH3wQq3RAzt4VV1yBbVOhUm52dnYqnKwXTE3RcnPvvff+yNVXX/1btuDp6eloscGUEEIIIYQQcmEAQdNkxAjrbvme7/meD4qJmUHWM4iZmZmZGFsT9AESkvWvvfZahLyopQYeZDrVwTelk/sZ3NTiDlVa53IaEwJs3brVHThwIC83fOK2T3/6078QhM/P23NA0GzevJmuaIQQQgghhFwAwOUMgqbMehyMH7/54he/+Ncvu+yy6NG1Zs2a/okTJ7LrWdAJ/eT91d+2bZvfv39/dk9D1rPrrrsuChh4kkkafNPS6n4G5WN2tm5oMa4mmIPiejPSp2Y28w0fCRfxG8HCU7PWQL3Bz47JAwghhBBCCFneoE+Pvn2ToHnpS1/6m5hHcgBJGkGTjEFDIEMa5oPBBAnJ4Hqmw8m4PXv25LIKg0tmXPazrGU0D7RmQXvwwQfLGBuMABqvAKmbsQwFlrZHpfVP/sk/+Q1cVHkSjC6KC2GcDSGEEEIIIcsLiJigA2KfviRZaH7z5MmTOUuyDMa39MiMDMKxWegEg0nUDQh1CVqiZiSB61n4OOt2poyLqVF/Nc2E5lDYAw88AMUUXcswZs2+ffuymxk+GzZscEePHo3zoTIuxdf0Lr300jh/7733/tsrr7zyfyvPBXe0cGwcwJMQQgghhBCytHnqqadajRPw0rr++ut/I3mA9ZOVpm8/wUoTDSNbtmzpI5ZG6lnPxEyzkCnjacBYUdOwTw9+bUHU5NTOEDbhgnpnzpzpHTx40IXK9ULlyjibuByETe/JJ5/s/e3f/u1PX3XVVf9bqNDa8oRhH3na057GJAKEEEIIIYQsQSBmghEjTkuQ5Wz//v23fP/3f/9HsRj69j70/yFOZmF96fV6/WClwYCbKMMOtOmRIGDlypU+GEAQZ+OD5lArTYyngbEljVFTO2eXwTdNOE0FRvRMFUCgf4ytCYJG3dBqMTWF6ShW+iUveckffOQjH/lns7OzB8qyw0XL1772tRhk1HSjCCGEEEIIIYsP+uboo7f100+fPv0g+vg/9EM/pIEw0UojSTckt7M4HwSNtcxE9u3b11+9ejUG2tRVUb2ooIkr3LBdpouoycD9DCeFYoJy0kqk9M4I7CkHxYkBQLgQFTZBsOR93vWud33tO7/zO//JN7/5zf+96Xx60yBw4KNHgUMIIYQQQsjigj74zMyMfOUrXxlpdECf/h3veMdrfv3Xf/1r6P8HC00ULcbtLE6DhaY2qGbSENEdDckB9u7dG7UEMp4h9EUtLE3j0yhd3M/Kfe3UfmTbtm29s2fPOrih9ft9jFsT3c6CsMFyLyiz7JKG+JogcFxQYr2TJ0+6X/7lX37mj/7oj74zrH/5qEoEc1VMAX3JJZfIqlWr4jLc1OiqRgghhBBCyNxBXAyC/k+dOhVTM2MZAqbMZlYS+vL/8Hd/93e7f+EXfuEBTQgAQYO4F6RvFjMmDdzOUhxNNJKE/vwsPL62b9/eV0PJzp07fXI9w7I9uW+rw0SixiQNQMKAuA7xNV/+8pd7QVFZgdPDwJpByU2FSrtQ6d7atWt7x48f711++eVxHyQOwBSiBtNwAyBUen/wB3/w4uc///n/Jqz/n4UQQgghhBCyJIGY+dznPve///iP//inw6IP/fc++vQQNPDOsmPSQOAELRCtMZs2bfJTU1N9hK7AShM0QB/hLCm78pDnV/ynIY7GMomoKY8ppzYDWhQuQWH1krUGaZ6ngjKrbQ8XGrOhyUAw6XpUuvfe9753+z/9p//0J4MYerUQQgghhBBCzjtIAhAMFnd96UtfuieImX+QIm4+JQWI88hoHAwZmukM62YhaCBwer2eX7FiRX96ejpadRBHk9zOfEpKloVNU7azkklFjZMslFzphgZ6DdNeqLw7fPhwb/369W5mZiaLH1htUFZQcFnohAvKVptg7nKveMUr1v7sz/7sy6+66qqXw3rTlC2NEEIIIYQQcm6AkDlx4sQ93/zmN//hAx/4wD0f+9jHjktdzKjg0ExmcTwaCJpgoICFJsfQBKMHhAzSN+e0zdu3b4eVpo9EZEgQcMstt8T18AwzFpqFEzVJzOi8HZAT49a4HTt2OLihpbFrXDAnuQMHDkQBo8IG8TXJYlOz2hSfXhAwMGm5NM0JDX7nd35n+7Oe9azrnva0pz03qLrtQeWtC+arNeHmbBVCCCGEEELInDh79mzMShwMC188derU1x577LEHP/WpT/3X22677WtYH4wOPiUJgKuZb3A1w3iT/SBmbAyND4YNJBqwFhudFzOFpujDQgMxk0Jdai5oo5iL+1k8TkVNOqFmRstWm6C4XFBcNVc0iCIIG7OftdpIirMBUypq7DnDjYzLsOAU62MQUzkttsebgTLSPn2MraM7mYeky5LOJem8XstGmShP97E0ZIPI5y/qVdYz7ovzpDra+uZ5rYdFrynhtZyW87hU71yOvY50b72ti9m3PDbva+9fOp8r72lRX3uOXDezPj+T8lloY7LX2XAvZdR2rUPbemlmZHmpjigT+/TSfbLPojZf3DN7XVKcI98n8z7ad7p2v9PUFeXU3omGOuj+vuF9KZ+jrnPpWofuWcv98S31kA7nrL0HRV1q7Yvtn+3f3s+m7Wz/Q3Vg+2f7Z/u/uNt/eW1Dz1b3USGDKdalpAA5i1nqz2fREkSNBOPDrBnuxaZw9slCo+uhK/qqLcS4nmEyKp4GzCVlmC3RJzGTl2E2QpazYLGRVMH4xQEzU1B/Pa10Wg+TlINJKgmaPuJsEFCUrEDeng++dEno6M2O5yweqD5g+5LUXirz0Pr6Ejd92ZgvsKwgzfpRX9D2i6D24on5opDiy0fXFefz5th+Qz3jel3WY2x9ii8v31KGpMao9fVlI5Pha83q2ZZpG0DDHyJv/pjMpnsgxblqXzbFl5VtaL7hnEPX2vRFpcvlcel+N36xtXxJ67OuPU/zDGv3r+1ZSvGHRN9J8wfZll3eB3ut5b2U4j3U9iIN5xj6IpWGP6Iy/NxrX8TmnI3H2Pum57N/NNO9a2xbTw13PJrav7D9D98ztn+xdWT7Z/tn+2f7l3L9Rd7+rSAq279twypmastwNcM0CZosXpAMALEzUomZeFzKdKbDwmD8S3f//ff7Bx54wEm97Q5u9hhBAyYap2ZU4cghjQ8qZAbbFK0YclofOnQoqjb40snghe6nLAh6sbhwrJ9Ng/TMSmWe6hdqEC9FX/fDQ4FqTC+lTy9VP+0XszFcMvh1pq/bzH6xjthHBg8xm8dwjJiHZupa26ZfCCgjHD9r15tz9NN2Pc+snhP7pfmh86Vr6ps62uvCteu1aOMa+uA43Ktye7p/uTx7Tp1qfVPdvZ6v6b7Y+pmybN3FPFdf3m9zz4buqzlnvBYs6zq7zdRb35NZW8eWumsZffu8zP75/ui9TNcza8+tz9OWV3x8w9Trs8QxoexZW1YqZ9Y8n1lTtr12+/ybrsGnILt+ec/tsxhR76bz5GvVd90MsFXbX5+H3jf7bpjnOtt23vKcWLbtX8/P9s/2z/bP9i9s/2z/bP+Ttn9fPMPGj6Zr1g9czpK7Wd++U3oOWGgOHz4c59NYNPGZJOuMgliauA/GpUllZ8YlCMj7yRyAG1k6Sc4aYMrKUxtjgxXhhvUQY7N582Z38ODBKKg2bNjQO3r0qB7vgiDKZSELWkokoOXmbZdeeqlgnBtbr5RkAA8lrk83PqtOTUKQ9pWGebt/vlys07JlDLZcW16xvla+rZctJ11D0/pYrq1/074j6lU7b7mtKE+vv+kcLpXTVnY+tiy/7XrtOew6XT/qPooxhxfXUatHcS3OmFLHPjtbpnnPxJbd9I6V1962X9t9aDtHWW5Rx8Z6lPfV7lvSdG57TLp3btyxxTH53SrfffN+A2fqVmubI+4B2//4etXOy/bP9s/2z/bfcm62/4uz/XtzrE/97ThVcYGBNFMSgEaRqPEzmrYZlhokBdi2bZvfv39/FFdmgM1YJrKdffSjH+3binRJDmCZ04iVuCgVNlK/CXEdLDbBfCRpwBykc0aKtuhjiAs6deoUxrGZDRfYw8Wm41zKjmbFy2wSOfFYuKZhPW4mBE0awDOeA/PG587reiynByL2Swn7YD1mU1l6jD5Mp+VgW3kPVFTpNj1f+XJouViv9bBlpGN92i/fUy1HjzHXkMvVS9HrMl+OQ/W19TLnFXPd0nZuGShzl46Lz7msV8P5ckMw1x6/dKyVT7/8TAPMZlZT56F19rxFHfTaffkFlc479MfJPhudpnr1R5xH7Hum5yrrV/4xbHoP7P1oe052v5OVC6ZF/8DY99a+966sg2LeO1+cM9e9rLc+J1N3P6Le+d3WbVqW/fXFviNlfdL2+L2D56LvY1EfEbb/fA0Wtn+2/7IOCts/23/Tvmz/F3X79+ZexfJSAoB4XsTMiGTLT+0TDBVxn2CsiMIF6ZrhqYX5YODwGIcmiBloArFeXcnbq3Y/fOrUT8KcRA3QLyN9U6ygCBVTt7Z4Y1B5BALJwNzUg/lpdnY2+hUGURNFC9Rcv9+3ggZWm96JEydwnIqbGK+Ttte+BNJNdzqvhHVQlFH02AdtvsC87mP315fLbKu9SOnc/eJLOB8D9DhVt2EZY/OocStmiMC+5hx++Da7Wv3s+VO5fd0WlPBQHWw9zLXmeStONWsF9sW8PZ9pBPolHp9tmtbqrOXYlzM9H82g581835zDLtuGZp+zPa/9ktTn4PX69b6kZ6/Pqp+Wa78G6vus7066l7W62uu3+5r7Z981b65H/whom8n3Sp+hVO99PIc+v3Re3/Ae2l+mYkXN+1BrB+ncfXMv9H0Ue++LdtFE/iUMX/j2Oej7gjKLNqH3IrcB+y7psyvfN1On/Dyl3uZr82ives/Y/tn+2f7Z/hueCds/2z/bf0P71/Jb2r+tdzx3ss5I6p9LMEjEQTUhZMJHTB2jdSb0+WO8zr59+6AFcrlXXHFFFjS7du3yJjmAZliWSZn8iOYyvMmG5lC5JHac2ccu94LFRoIJKq6DO1oSNBLETnybjhw5ooN2xrFsjh8/ruVIIXCgGvOXLZbTDXdlHUfVXxeSApXk9pbXpwQGuSHgJdCGEM6X98E6XU4ZIOx5FKt2a8fjHCrOknnPlmH3q5U17njdlpbt/fLm2vL6hutruoflvYsJH9JU7D3FctO1aBkt22rbzTOp1R/TFKBWXlvtXmObqVvjO6H76fVrvWNBlSukt9dk7tFQ3W290hdHvr/2V4+WutU6B/YYe19H0HRvG8/dcD5bhrTctzxv3zPTBlrbXSrPaX3izskCnI7P7ya2ldet91T30Xe1bLdlPdvuUVk22z/bP9t/LoPtn+2f7f/Cb/8iLe0EFhUVMGLEo85v3LgRcTOirmYHDx60+5UfJASI8feYh8sZYmigG7RMV6mZtnY78qIXioGMS+oK4iapLodKA4xlI0bY4J8gbpyKm4aP0oNJKyjAXnG+8gWGdQdiBw8hrsB8QfmFXJYRseVoGXY+LcfjyvOVxzTVoyy/6XwN5HoW1+lV4NlzmvPG49quJc2XZQ/dE1uHhnM01rFlnyGKfZFswpV1NXWubS/LSfvqPaldR9P9LcrXslvP0XStuqzHNp0z1UmXa/fanMs+q8bnXb6DKEvfA3velmdde1ea7kMb5bFt7166Hr1Gb36EaG1n45h0P7Z/sedl+y+2txzD9s/2P1QO2/9wHdj+69eqyxdQ+/fFdUZrTAoPieIk9MmRprl/+PDhvC4YKHwQNDEZwIEDcagbuJwJXM5SUoCaeFKXMxkWLxOLGWXBRI1NHtBSvkvp2qDS5NFHH+2lNG4OwgYz1nIDpRemogkFsD6oQVhwamWmYKS8TlNE67nDssDKo1OzX5zqNp0vKY9rIu3j07m77CtdaKpzWs7nsvu0XWvbefW6waj9bblN+xqG7ns5X5570vPqvUbec33n2p5rU5mS7p3+Ith2LfZe22sq6tF0aO09GPe8Rz2zpn2VUcfY9Yhnw2C3497xtm1d6VJGw/OM97Y8dlSbbXomI5412/+I87L9s/2LsP2z/bP9l1xs7R/3IuxXbvIj5mviBGIGUwiatK5f7p+sM7DMxH0xuCamEDXJu2uw88Awcv5FTVmmWmzULU0KgYNPGqAz+tiFqbPb8NmyZYvALQ2fQ4cOiQzXNx8D1Zh8+cTOy/AvMVFh4qUOYiiuT8LI/oKDMnzaJx6Tkhh4LR/oOXSbOW8+Z7IwSdt9Ssfr+XX/fJw9r6lrPEbN9UXdva2TXVec25vzOy3TlJX3S9fVWI7WV/ez98VitpX317c8r1iv5KdZe4Z6D+w9tmVjOyivyZQnxT2SouyyTrXr0Hrb8uw9wno9v5apjdS8H0o+l07t+2Sfb/Ec8vtcPie9flOHxntsz2vqVqN8j4rn2PTO5udrrkVMXfP5G9pN3ibpXS/e8fLX1XyMfSfMOZt+iWX7Z/uX4h6JsP2z/VfnZ/s3+7H916/jImj/+TknQ0I+h8bApP54nA9WmT6yGgfjRD9lNUPGY8TP5IQAib4p3ydvLm8yKM9ZyFjmPE5NG5qsQF3QkrDRi4gbYanB8uo0GqkxS9kPMibAlNXX8W2CGqzl3ob/ni5PT0/n/PbpBYrz4aForuzZ9OmHhwczWt4/zVuFOQuzGtaHc8Rtdh8cnzI7xP17vV6sV8rk1tdjME3rMI/1ed7WJ9U97p/K1eNi2Wl/W9dZTFEPbIc/o5i84FjWMrEvji/3EZPHXO8FjjH75SnqhLo1nEfv9WxxX/rmevvpnsf7g2PwwbKeF8eZZzqr16DXaM6Zr12vC1Nz3rge90SfnXkvUJ49l32mWna+3/Y6tD76bmm9bXl6bdgf58Q2LRf7py8/ex323sTy03a/YsWKs3rPUA62me2zDWXMmnLiteM4vU8o01VZSmr7m+dWe+Z63ShDr13roNeR1sd3Vd/11N5m9ZpMG5o19zK/2/p+Fdfi032rtdV0nD5nPX/Z/n0q1x7D9s/2z/bP9s/2z/bP9j+6/cf901iS/SBo+knIxGnoi3vtj0PMYP9wvT6IGJ0K1gVB00+CJlposH8KQ/FhWjNyODeHbAAjWHBRozcPJBUmRthEggkq3sBwwXojbUOK81B64ZMbF3z0IHCk+tLTwXziA8LNhujBJzyI/LKah5LPYZbtuvigMEUZoey4H86Rttl98rwMXsJYB1y7HoN6YYpyQnmYx/r40rh6Gjz9Aps1Yky/IH3yWdQ694v7hOCseI16LdgPy+mYuD/mtbxUTn5x07XkOup+WDbXGK8N5RphGfe157ZTvV7sr3XHMvbXL2xzTXmq98CUq9fh7bn1Gek6vRZ771BWehbxk65Jj5lN74v9Yovvjrl3uSytT9P7lK6tr89Kn78pO5aTzqvT/IVjy09lij4nlJGe8Wzx3tau2ZZjv4T0HUvH1/bX567PSK/ZvqNYj/cb21Idcz313uizxnWntqftsJ/aQyxHyzT30n5B19pkUR+v77UM37NYhj4zU0+2f7Z/tn+2f7Z/tn+2/w7t39QXwlDvs1+1atXsli1bsnFB6wkRg3lYZ5DVDJYZiBmsg5CBoIGrGRICQAukATXhdhb3CevitZr6LwgLqpCayoeW2b17t2ZEG0ogoGPZJGoiC+5pmBauabXy8Q/SxaWgpEx4CHFbyo+t62r72G12H6xPUy9j7pGWaY7JUxk8KGf3scfArQ4+iA3HtZ7D1NHWzbddb1leeQ8sLdfQWs6o+ycN9da6pqkr1+Eamo4dVZe2epTbm+pV1m1MWUPvgqlj4zZbVgqcc+W7YcqWEeUPlTnu/rc95/DlGusxqoymc1ma6oDrO3PmTC99Gbbe6y7XkGh8p8fV1a4rj2X7Z/u3ZZd1Y/sfLkfnLWz/zedh+2f7bytrmbR/b+qb+9SYTwImklzMBFYZCBkch0E0QUqeITazGYCgSUkBnHpsmZTNCyZowLkWNZqLvLyAmvlJ543IKbfV6okYnNOnTzuoQ3v8fEgppmvLSkpg4O0+xfa8rNvDixDrFF4Mb8vEy3H27Flnj5Vmv1dLbgDlecrltusydYfvo31BddalkV5tmbVG11BW23wuy5Zt69V0vC4X9SrxKWPe0DU20VRnXa/gmYR3yTXVoem41KjjgLCjzlnO453A+4CpfknouUddQ1lfrVvbObvUxZCfDd5LrVfTO12U6dOxte3Jl9ZpuSgT1zzu+mT4nWmloV7j2k+nMtn+2f7LOrD9s/2z/bP9F+Vd0O1f64S2jPWpn52vNcXARzGDUJIgYPL7C8uMGipM1mONm9FynDn/ggqaxcZJXaDE+ZTBAlabKXxk8LLgg/lp81mBaVCEK9L8yjS/suGzCtNw8+N8mta26Ty2me1xW3iYq/BJy+W6leXxumy2r9Jtuk7rUtbPrM/rymPsee3+Zt9VbedpuJZV5XZdV9Yfy/be6HxxH4bOb+tQ1M+er/X44j4N7WufR1HequKZDD3D8hmUz7ksv+EZrGp6r8q6NL1Ttj4N7015rSvL99aew95n+x4Uy0PvvH2G5X3SdeU71dYeRjzHpmfWeJ9tfZraV8szrD2H4hrLZ8D2z/bf9M6W69j+2f7Z/tn+L9b2n59d8SxWYGr63bEfHsSM9sun7Kfow7uWz4WFpuHDVD9SXWzPTO1NyuJGbyZusn4k3eg0LdevlOphxGV9eEYUZXGEdbrdPlSdL8sq5lfaOjSdvzhvPt+IsofKtNfXcK22zJUNZZUv6IpSHBbLQ9emZds6lOcr7uFKU0/bUMv7Wt6P/Jwa7md5Lfn8thEWDXJlw3MYusdt99Rc+9A9KN8Te2/K8otjm97F2vWU52gR843PsumeFe9/7bpa3tUV9t6W113cr6b9W9tU8d6U115+sefztxy/srifK4Ttf1SbYftn+69dV8u7yvbP9s/2Lxdu+0/z0+b8sY9txEut/5365VHA3HLLLb3Uj7diJk61f1/09y9Y7MW5lEcg3xjcKLsshSJMn+nypjd9GvZZ0bQeD8qu0+W2B2unTfvZ8gpxNmVeitbyG5aHBJ49rz3OrJ8aVX7L/FRRxsi6lfdsVBn2PrTdty7PUstpuPah88uw1W/oOG3UZX3M8SvG1c2WUZy/9RpHXHu+jmKfqVHPVbeV97nlfFNt98ksT404vnWdfQ/KdlI+o2L/cfdletL92P7by2f7H0zZ/uv3ie2f7Z/tn+2/eFdr81Lvq9esM6Y/LyZH2EVFzSWt/OhNK25eXMYHNzpta7rxeV3TenucFOKpVKN2v/JBF9vjCzeqTm3LbfUctX9Rp0YB2PX84+5hyz3ttR0zqryGchr31Wc84tpby2vat2l727Ns2698H7SOxTMYEuRt9WnbXkx7o45tu996bNt5zB/f2vs0qj722qV470e9y6Peg3HvTtMzYPtn+x9VHts/239bXZqeY0td2f6F7X/Ue3OBt/9eSx+8sa9urDNCBjTG3ogxYUndXc025ix0pHgA5iUst1tr0NBxhVmtrXz7Ig+V1/YpRVq53p6rbbuYBlV8wQ8d33Y++9HrxXTc/mZ73tc+i+LjRpXRdJ+73Kvy+m2dmq6r6TmWy3rt5lqcNL9Pneradp4R1+TGPbeGL5feuGfV8j65pnvR9GXW5f0ZV99R7/yo+zFqH7Z/tn+2f+mNe1Zs/2z/4+rE9s/239T+G/reF36szAISFV7hf9dqwbE3WpftA9AvLjWH2WPsfrachvU9u62sx7iHXZZt62PrWF5bUxlNdS8+vS7161rnlv2k6VzlvqOW284/Yp9e8TzH1rnL/RlVR9PYW+vTch4pr6PpGbe8p0PvctP+TeWW7WHUfW4qs+36ZMQz0nfBtruijjKuTuUfoqZymp4h2z/b/7h7NO7+sP2z/Yuw/bdtZ/tn+y+fe0P7l9TOc8w86cbQzSoeht2n6eUSaXmRZMRDa/sCbGvsxUNtOkbMvtLSOGXcl1j5YrWV03Rf2r4wzTaR9pd4VKMYSgBRPqOme9f2BWWPaSqrvK50zNB12nUN5xVzP2yZ5fMbuhfmfNJ2jlH3zdwPKe+lNLyXHddLw7wU96k8Ttq+3Ir3YdTz6Hrd0nAfGt+Npj8WxXWMfB9sXXU92z/bf3Fetn9h+y/3aTqHZ/tvfI/Z/i/c9l++z9L8HMgCYh+eNLzsdp/aw/N1f7/al6u0NFaRxi8KPbdIvWHbfZr2tcvl/rVzFF9+Q3X29aCsoX18XUU3vfDltZX3qVa2LxS5adiNz8M2fPul11C3oT9C5jqkfG6+/Q9G45eQLbPpXSmebeP6hi+jUQy9e0U5tp72mPLaXUMdas+seB6uuPeN72pRfuM1++E/LmUd274ky7rXnq+v/0FwDe+AFGUNXYew/bP9s/1LUTbbP9u/FNfB9s/2L8X1dG7/QgghhBBCCCGEEEIIIYQQQkgn/h9LecCVB4t7oAAAAABJRU5ErkJggg==" />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                {false ? (

                    <div id="kc-form">
                        {showProfileContext ? <ProfileContext /> : null}

                        <div id="kc-form-wrapper">
                            {realm.password && (

                                <form
                                    id="kc-form-login"
                                    onSubmit={handleSubmit}
                                    action={url.loginAction}
                                    method="post"
                                >
                                    {!usernameHidden && (
                                        <div className={kcClsx("kcFormGroupClass")}>
                                            <label htmlFor="username" className={kcClsx("kcLabelClass")}>
                                                {!realm.loginWithEmailAllowed
                                                    ? msg("username")
                                                    : !realm.registrationEmailAsUsername
                                                        ? msg("usernameOrEmail")
                                                        : msg("email")}
                                            </label>
                                            <input
                                                id="username"
                                                aria-label="kc-username"
                                                ref={emailRef}
                                                className={kcClsx("kcInputClass")}
                                                name="username"
                                                maxLength={75}
                                                defaultValue={login.username ?? ""}
                                                type="text"
                                                autoFocus
                                                onInput={(e) => setEmail(e.currentTarget.value)}
                                                onChange={(e) => {
                                                    setEmail(e.target.value);
                                                    if (errors.email) clearFieldError("email");
                                                }}
                                                onBlur={(e) => validateField("email", e.currentTarget.value)}
                                                autoComplete="username"
                                                aria-invalid={messagesPerField.existsError("username", "password")}
                                            />
                                        </div>
                                    )}

                                    <div className={kcClsx("kcFormGroupClass")}>
                                        <label htmlFor="password" className={kcClsx("kcLabelClass")}>
                                            {msg("password")}
                                        </label>
                                        <PasswordWrapper kcClsx={kcClsx} i18n={i18n} passwordInputId="password">
                                            <input
                                                id="password"
                                                aria-label="kc-password"
                                                className={kcClsx("kcInputClass")}
                                                name="password"
                                                type="password"
                                                ref={passRef}
                                                maxLength={20}
                                                autoComplete="current-password"
                                                onInput={(e) => setPassword(e.currentTarget.value)}
                                                onChange={(e) => {
                                                    setPassword(e.target.value);
                                                    if (errors.password) clearFieldError("password");
                                                }}
                                                onBlur={(e) => validateField("password", e.currentTarget.value)}
                                                aria-invalid={messagesPerField.existsError("username", "password")}
                                            />
                                        </PasswordWrapper>
                                        {usernameHidden && messagesPerField.existsError("username", "password") && (
                                            <span
                                                id="input-error"
                                                className={kcClsx("kcInputErrorMessageClass")}
                                                aria-live="polite"
                                            >
                                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 17 18" fill="none">
                                                    <path d="M7.66797 11.5003H9.33464V13.167H7.66797V11.5003ZM7.66797 4.83366H9.33464V9.83366H7.66797V4.83366ZM8.49297 0.666992C3.89297 0.666992 0.167969 4.40033 0.167969 9.00033C0.167969 13.6003 3.89297 17.3337 8.49297 17.3337C13.1013 17.3337 16.8346 13.6003 16.8346 9.00033C16.8346 4.40033 13.1013 0.666992 8.49297 0.666992ZM8.5013 15.667C4.81797 15.667 1.83464 12.6837 1.83464 9.00033C1.83464 5.31699 4.81797 2.33366 8.5013 2.33366C12.1846 2.33366 15.168 5.31699 15.168 9.00033C15.168 12.6837 12.1846 15.667 8.5013 15.667Z" fill="#FF0000" />
                                                </svg>
                                                <span className="ml-4">
                                                    {kcSanitize(messagesPerField.getFirstError("username", "password"))}
                                                </span>
                                            </span>
                                        )}
                                    </div>

                                    <div className={kcClsx("kcFormGroupClass", "kcFormSettingClass")}>
                                        <div id="kc-form-options">
                                            {realm.rememberMe && !usernameHidden && (
                                                <div className="checkbox">
                                                    <input
                                                        id="rememberMe"
                                                        aria-label="kc-rememberMe"
                                                        name="rememberMe"
                                                        type="checkbox"
                                                        defaultChecked={!!login.rememberMe}
                                                        className="custom-checkbox"
                                                    />
                                                    <label className="custom-checkbox-label"> {msg("rememberMe")}</label>
                                                </div>
                                            )}
                                        </div>
                                        <div className={kcClsx("kcFormOptionsWrapperClass")}>
                                            {realm.resetPasswordAllowed && (
                                                <span>
                                                    <a href={url.loginResetCredentialsUrl}>
                                                        {msg("doForgotPassword")}
                                                    </a>
                                                </span>
                                            )}
                                        </div>
                                    </div>

                                    <div id="kc-form-buttons" className={kcClsx("kcFormGroupClass")}>
                                        <input type="hidden" id="id-hidden-input" name="credentialId" value={auth.selectedCredential} />
                                        <input
                                            className="login-custom-button"
                                            name="login"
                                            id="kc-login"
                                            type="submit"
                                            value={msgStr("doLogIn")}
                                        />
                                    </div>
                                </form>
                            )}
                        </div>
                    </div>
                ) : (
                    <></>
                )}
            </Template>
        </>
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
                <span aria-hidden="true" className="password-icon-eye">
                    {isPasswordRevealed ? (
                        <svg style={{ width: "100%", height: "100%" }}
                            width="20"
                            height="13"
                            viewBox="0 0 20 13"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg"
                        >
                            <path d="M9.99967 1.91667C13.158 1.91667 15.9747 3.69167 17.3497 6.5C15.9747 9.30833 13.1663 11.0833 9.99967 11.0833C6.83301 11.0833 4.02467 9.30833 2.64967 6.5C4.02467 3.69167 6.84134 1.91667 9.99967 1.91667ZM9.99967 0.25C5.83301 0.25 2.27467 2.84167 0.833008 6.5C2.27467 10.1583 5.83301 12.75 9.99967 12.75C14.1663 12.75 17.7247 10.1583 19.1663 6.5C17.7247 2.84167 14.1663 0.25 9.99967 0.25ZM9.99967 4.41667C11.1497 4.41667 12.083 5.35 12.083 6.5C12.083 7.65 11.1497 8.58333 9.99967 8.58333C8.84967 8.58333 7.91634 7.65 7.91634 6.5C7.91634 5.35 8.84967 4.41667 9.99967 4.41667ZM9.99967 2.75C7.93301 2.75 6.24967 4.43333 6.24967 6.5C6.24967 8.56667 7.93301 10.25 9.99967 10.25C12.0663 10.25 13.7497 8.56667 13.7497 6.5C13.7497 4.43333 12.0663 2.75 9.99967 2.75Z" fill="#555555" />
                        </svg>
                    ) : (
                        <svg style={{ width: "100%", height: "100%" }}
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
