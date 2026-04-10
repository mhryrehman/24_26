import type { JSX } from "keycloakify/tools/JSX";

import { useEffect, useState, useRef, useMemo } from "react";
import type { LazyOrNot } from "keycloakify/tools/LazyOrNot";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import { getKcClsx, type KcClsx } from "keycloakify/login/lib/kcClsx";
import { clsx } from "keycloakify/tools/clsx";
import type { UserProfileFormFieldsProps } from "keycloakify/login/UserProfileFormFieldsProps";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import Template from "../Template";

type RegisterProps = PageProps<Extract<KcContext, { pageId: "register.ftl" }>, I18n> & {
    UserProfileFormFields: LazyOrNot<(props: UserProfileFormFieldsProps) => JSX.Element>;
    doMakeUserConfirmPassword: boolean;
};

const getMonthNumber = (monthName: string) => {
    const monthsArray = [
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ];
    return monthsArray.indexOf(monthName) + 1;
};

const onlyLetters = (s: string) => {
    return s
        .replace(/[^\p{L}' ]/gu, "")      // allow only letters, apostrophes, and spaces
        .replace(/^\s+/, "")              // remove leading spaces
        .replace(/(?<=^'?)\p{L}/u, ch => ch.toLocaleUpperCase()); // capitalize first letter (even after an apostrophe)
};

const safeGet = (k: string) => { try { return sessionStorage.getItem(k); } catch { return null; } };
const safeSet = (k: string, v: string) => { try { sessionStorage.setItem(k, v); } catch { } };

export default function Register(props: RegisterProps) {
    const { kcContext, i18n, doUseDefaultCss, classes } = props;
    const { kcClsx } = getKcClsx({
        doUseDefaultCss,
        classes
    });
    const { messageHeader, url, messagesPerField, recaptchaRequired, recaptchaVisible, recaptchaSiteKey, recaptchaAction, termsAcceptanceRequired } =
        kcContext;

    const { msg, msgStr: _msgStr, advancedMsg } = i18n;

    const [areTermsAccepted, setAreTermsAccepted] = useState(false);
    // const [dob, setDob] = useState({ month: "", day: "", year: "" });
    const dobFromKc = safeGet("dob") ?? "";
    const [dob, setDob] = useState(() => {
        if (!dobFromKc) return { year: "", month: "", day: "" };
        const [y = "", m = "", d = ""] = dobFromKc.split("-");
        return { year: y, month: m, day: d };
    });
    const [email, setEmail] = useState("");

    const [customErrors, setCustomErrors] = useState<{ firstName?: string; lastName?: string; dob?: string; month?: string; day?: string; year?: string; gender?: string; language?: string; terms?: boolean; termsAndPolicies?: string; password?: string; "password-confirm"?: string; cell?: string; }>({});
    const genderFromKc = safeGet("gender");
    const [gender, setGender] = useState(genderFromKc ?? "Male");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [emailError, setEmailError] = useState<string | undefined>(undefined);
    const [emailTouched, setEmailTouched] = useState(false);
    const [showProfileContext, setShowProfileContext] = useState(false);
    const [invitationId, setInvitationId] = useState("");
    const cellFromKc = safeGet("cell");
    const [cell, setCell] = useState(cellFromKc ?? "");
    const serverEmailError = messagesPerField.existsError("email");
    const [hideServerError, setHideServerError] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const hasServerError = kcContext.messagesPerField.exists("global") || kcContext.messagesPerField.existsError("email");


    type ActivationCodeProfile = {
        email: string;
        firstName: string;
        lastName: string;
        gender: string;
        dob: string;
        invitationId: string,
    };

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

    const [activationCodeProfile, setActivationCodeProfile] = useState<ActivationCodeProfile>({
        email: "PatrickJones@gmail.com",
        firstName: "Patrick",
        lastName: "Jones Smith",
        gender: "Male",
        dob: "1996-12-12",
        invitationId: "",
    });

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

    let loginUrl = url.loginUrl;
    // --- state & url helpers ---
    const SS_KEY = "RegisterState";
    const SS_KEY_SIGNUPURL = "SignupUrl";
    const normalizeB64 = (s: string) => {
        const n = s.replace(/-/g, "+").replace(/_/g, "/");
        return n + "=".repeat((4 - (n.length % 4)) % 4);
    };

    const decodeState = (s: string) => {
        try {
            const b64 = normalizeB64(s);
            const bin = atob(b64);
            let txt = bin;
            try { txt = decodeURIComponent(bin); } catch { }
            return JSON.parse(txt);
        } catch {
            return null;
        }
    };

    const withParam = (urlStr: string, key: string, val: string) => {
        if (!val) return urlStr;
        const u = new URL(urlStr, window.location.origin);
        u.searchParams.set(key, val);
        return u.pathname + "?" + u.searchParams.toString();
    };
    const syncConfirmError = (pw: string, cpw: string) => {
        setCustomErrors(prev => {
            if (cpw && pw !== cpw) {
                return { ...prev, ["password-confirm"]: "Passwords do not match" };
            }
            const { ["password-confirm"]: prevMsg, ...rest } = prev;
            if (prevMsg === "Passwords do not match") return rest;
            return prev;
        });
    };


    const TemplateComponent = Template;
    const rawStateRef = useRef<string | null>(null);
    const [isLeapHealth, setIsLeapHealth] = useState(false);
    // const [isLeapWeb, setIsLeapWeb] = useState(false);
    const validateEmail = (value: string, _onBlur: Boolean) => {
        if (!_onBlur) return undefined;
        if (!value.trim()) return "Required";
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
            return "Please provide a valid email address.";
        }
        return undefined;
    };


    const [isPasswordVisible, setIsPasswordVisible] = useState(false);
    const [isConfirmVisible, setIsConfirmVisible] = useState(false);
    const todayStr = new Date().toISOString().split("T")[0];

    const passwordInputRef = useRef<HTMLInputElement>(null);


    const orderedFieldNames = [
        "email",
        "firstName",
        "lastName",
        "username",
        "password",
        "password-confirm"
    ];

    const orderedAttributesByName = orderedFieldNames.reduce((acc, name) => {
        if (kcContext.profile.attributesByName[name]) {
            acc[name] = kcContext.profile.attributesByName[name];
        }
        return acc;
    }, {} as typeof kcContext.profile.attributesByName);

    const [firstName, setFirstName] = useState(orderedAttributesByName.firstName?.value ?? "");
    const [lastName, setLastName] = useState(orderedAttributesByName.lastName?.value ?? "");
    const [hasPasswordFocused, setHasPasswordFocused] = useState(false);
    const [isPasswordStrong, setIsPasswordStrong] = useState(false);
    const [passwordCriteriaState, setPasswordCriteriaState] = useState({
        minLength: false,
        upperCase: false,
        lowerCase: false,
        number: false,
        symbol: false,
    });
    const criteria = {
        minLength: (pw: string) => pw.length >= 8,
        upperCase: (pw: string) => /[A-Z]/.test(pw),
        lowerCase: (pw: string) => /[a-z]/.test(pw),
        number: (pw: string) => /\d/.test(pw),
        symbol: (pw: string) => /[^A-Za-z0-9]/.test(pw),
    };


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
                        fill={isValid ? "#1ab34dff" : "#555555"}  // Change color based on validity
                    />
                </svg>
            </span>
            {label}
        </div>
    );

    const handleCellChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        let digits = e.target.value.replace(/\D/g, "");

        if (digits.length > 10) {
            digits = digits.slice(0, 10);
        }

        let formatted = digits;
        if (digits.length > 3 && digits.length <= 6) {
            formatted = `(${digits.slice(0, 3)}) ${digits.slice(3)}`;
        } else if (digits.length > 6) {
            formatted = `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6)}`;
        }

        setCell(formatted);
    };

    const isAtLeast18 = (y: string, m: string, d: string) => {
        const dob = new Date(Number(y), Number(m) - 1, Number(d));
        if (isNaN(dob.getTime())) return false;
        const today = new Date();
        let age = today.getFullYear() - dob.getFullYear();
        const mdiff = today.getMonth() - dob.getMonth();
        if (mdiff < 0 || (mdiff === 0 && today.getDate() < dob.getDate())) age--;
        return age >= 18;
    };

    const sanitizeName = (s: string) =>
        s.replace(/[^\p{L}' ]/gu, "") // keep letters, spaces, apostrophes
            .replace(/\s+/g, " ")        // collapse multiple spaces
            .trim();                     // trim ends

    useEffect(() => {
        const emailInput = document.querySelector<HTMLInputElement>('input[name="email"]');
        if (!emailInput) return;

        const handleEmailChange = () => {
            setEmail(emailInput.value);
        };

        emailInput.addEventListener("input", handleEmailChange);
        return () => emailInput.removeEventListener("input", handleEmailChange);
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
        const updated = {
            minLength: criteria.minLength(password),
            upperCase: criteria.upperCase(password),
            lowerCase: criteria.lowerCase(password),
            number: criteria.number(password),
            symbol: criteria.symbol(password),
        };

        setPasswordCriteriaState(updated);

        const allValid = Object.values(updated).every(Boolean);
        setIsPasswordStrong(allValid);
    }, [password]);

    useEffect(() => {
        if (!hasServerError) {
            sessionStorage.removeItem("cell");
            sessionStorage.removeItem("dob");
            sessionStorage.removeItem("gender");
            setCell("");
            setDob({ year: "", month: "", day: "" });
            setGender("Male");
        }
    }, []);


    useEffect(() => {
        let emailFromContext = kcContext.profile.attributesByName.email?.value ?? "";

        const urlParams = new URLSearchParams(window.location.search);
        const fromUrl = urlParams.get("state");
        const fromStore = safeGet(SS_KEY);
        const rawState = fromUrl ?? fromStore ?? null;
        const signup_url = urlParams.get("signup_url");
        const signupUrlStore = safeGet(SS_KEY_SIGNUPURL);

        if (!sessionStorage.getItem("registerObject")) {
            const registerObject = {
                registerUrl: window.location.href,
                isRegisterFlow: true

            }
            sessionStorage.setItem("registerObject", JSON.stringify(registerObject));
        }

        if (fromUrl && fromUrl !== fromStore) safeSet(SS_KEY, fromUrl);
        if (signup_url && signup_url !== signupUrlStore) safeSet(SS_KEY_SIGNUPURL, signup_url);
        rawStateRef.current = rawState;
        const base = url.loginUrl;
        if (rawState != null) {
            const newQueryUrl = new URL(base, window.location.origin);
            newQueryUrl.searchParams.append("state", rawState);
            newQueryUrl.searchParams.append("signup_url", signup_url ?? "");
            loginUrl = newQueryUrl.toString();
        }

        let gotProfileFromState = false;
        const parsed = rawState ? decodeState(rawState) : null;
        if (parsed?.profileMode) setProfileMode(parsed.profileMode);
        if (parsed?.profileMode === "activationCode" && parsed.activationCodeProfile) {
            const p = parsed.activationCodeProfile as ActivationCodeProfile;
            sessionStorage.setItem("mode", "activationCode");

            setActivationCodeProfile(p);
            if (p.email) { setEmail(p.email); emailFromContext = p.email; }

            if (p.dob) {
                const [year, monthRaw, day] = p.dob.split("-");
                const month = isNaN(Number(monthRaw))
                    ? getMonthNumber(monthRaw).toString().padStart(2, "0")
                    : monthRaw.padStart(2, "0");
                setDob({ year, month, day });
            }
            if (p.firstName) setFirstName(p.firstName);
            if (p.lastName) setLastName(p.lastName);
            if (p.invitationId) setInvitationId(p.invitationId);
            if (p.gender) setGender(p.gender);
            gotProfileFromState = true;
        } else if (parsed?.profileMode === "appointment" && parsed.appointmentProfile) {
            setAppointmentProfile(parsed.appointmentProfile as AppointmentProfile);
            gotProfileFromState = true;
            setIsLeapHealth(true);
            sessionStorage.removeItem("mode");
        }
        else {
            sessionStorage.removeItem("mode");
        }
        setShowProfileContext(gotProfileFromState);
        if (!rawState) setEmail(emailFromContext);
    }, []);

    useEffect(() => {
        const urlParams = new URLSearchParams(window.location.search);
        const signup_url = urlParams.get("initiate_url");
        if (signup_url) {
            sessionStorage.setItem("kc_flow", "register");
            sessionStorage.setItem("flowEntrypoint", signup_url);
        }
    }, []);

    useEffect(() => {
        if (cell)
            safeSet("cell", cell);
        else
            sessionStorage.removeItem("cell");
    }, [cell]);

    useEffect(() => {
        if (dob.year && dob.month && dob.day)
            safeSet("dob", `${dob.year}-${dob.month}-${dob.day}`);
        else
            sessionStorage.removeItem("dob");
    }, [dob]);

    useEffect(() => {
        if (gender)
            safeSet("gender", gender);
    }, [gender]);


    const isActivation = profileMode === "activationCode";

    const isEmailValidNow = useMemo(() => {
        if (isActivation) return true;                           // email is readonly in activation flow
        if (!email.trim()) return false;
        if (messagesPerField.existsError("email") && !hideServerError) return false; // backend says "Email already exists"
        return validateEmail(email, true) === undefined;               // your validator returns undefined when OK
    }, [isActivation, email, messagesPerField]);

    const isNameValid = isActivation ? true : !!firstName.trim() && !!lastName.trim();
    const isGenderValid = isActivation ? true : !!gender;
    const isDobValid = useMemo(() => {
        if (isActivation) return true;
        if (!dob.year || !dob.month || !dob.day) return false;

        const value = `${dob.year.padStart(4, "0")}-${dob.month.padStart(2, "0")}-${dob.day.padStart(2, "0")}`;
        const min = "1900-01-01";
        const today = new Date();
        const max = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
        if (customErrors.dob) return false;
        return value >= min && value <= max;
    }, [isActivation, dob, customErrors.dob]);
    const arePasswordsValid = !!password && !!confirmPassword && password === confirmPassword && isPasswordStrong;
    const canContinue = isEmailValidNow && isNameValid && isGenderValid && isDobValid && arePasswordsValid;

    if (typeof window !== "undefined") {
        try {
            const target = (sessionStorage.getItem("flowEntrypoint") as string) || (kcContext.url.loginRestartFlowUrl as string) || "/";
            const nav = (performance.getEntriesByType?.("navigation")[0] || {}) as any;
            const cameFromBack = nav?.type === "back_forward";
            const fromRegister = document.referrer && /\/register/i.test(document.referrer);

            if (cameFromBack || fromRegister) {
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
            <TemplateComponent
                kcContext={kcContext}
                i18n={i18n}
                doUseDefaultCss={doUseDefaultCss}
                classes={classes}
                headerNode={messageHeader !== undefined ? advancedMsg(messageHeader) : msg("registerTitle")}
                displayMessage={messagesPerField.exists("global")}
                displayRequiredFields
            >
                {showProfileContext ? <ProfileContext /> : null}
                <form
                    id="kc-register-form"
                    className={kcClsx("kcFormClass")}
                    action={url.registrationAction}
                    method="post"
                    noValidate
                    onSubmit={(e) => {
                        e.preventDefault();
                        if (!canContinue || submitting) return;
                        setSubmitting(true);
                        if (firstName) {
                            const trimmedFirstName = firstName.trim();
                            setFirstName(trimmedFirstName);
                        }
                        if (lastName) {
                            const trimmedLastName = lastName.trim();
                            setLastName(trimmedLastName);
                        }

                        const raw = rawStateRef.current ?? safeGet(SS_KEY);
                        if (raw) {
                            const form = e.currentTarget as HTMLFormElement;
                            form.action = withParam(url.registrationAction, "state", raw);
                        }

                        e.currentTarget.submit();
                    }}
                >


                    <>
                        <div className="basic-form-reder">
                            {(() => {
                                if (profileMode == "activationCode") {
                                    return (
                                        <>
                                            <div className="title-form with-subtitle text-center">Set Password</div>
                                            <p className="form-detail mb-lg-4 ">Set and confirm your password <br className="d-lg-block d-none"></br> to complete account setup.</p>
                                        </>
                                    );
                                } else if (profileMode == "appointment") {
                                    return (
                                        <>
                                            <div className="register-form-basic">
                                                <div className=" already-have-account"><span className="">Already have an account? </span> <a href={loginUrl} >Log in</a></div>
                                                <div className="title-form with-subtitle ">Tell us a bit about yourself</div>
                                                <p className="form-detail  ">To book your appointment, we need a few things for <span className="d-lg-block"> {appointmentProfile.firstName} {appointmentProfile.lastName}'s office </span> </p>
                                            </div>
                                        </>
                                    );
                                } else {
                                    return <>
                                        <div className="title-form text-center">Create an account</div>
                                    </>;
                                }
                            })()}
                            {EmailField()}
                            {/* Render Name + Gender + Language + DOB only when DOB is NOT provided */}
                            {profileMode != "activationCode" ? (
                                <>
                                    <div className={kcClsx("kcFormGroupClass")}>
                                        <div className={kcClsx("kcLabelWrapperClass")}>
                                            <label
                                                htmlFor="firstName"
                                                className={kcClsx("kcLabelClass")}
                                            >
                                                Legal First Name{<span style={{ color: "#EA445A" }}>*</span>}
                                            </label>
                                            <div className="icon icon-info ms-1">
                                                <svg style={{ marginTop: -4 }} width="16" height="16" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                                                    <path d="M8.16602 4.83317H9.83268V6.49984H8.16602V4.83317ZM8.16602 8.1665H9.83268V13.1665H8.16602V8.1665ZM8.99935 0.666504C4.39935 0.666504 0.666016 4.39984 0.666016 8.99984C0.666016 13.5998 4.39935 17.3332 8.99935 17.3332C13.5993 17.3332 17.3327 13.5998 17.3327 8.99984C17.3327 4.39984 13.5993 0.666504 8.99935 0.666504ZM8.99935 15.6665C5.32435 15.6665 2.33268 12.6748 2.33268 8.99984C2.33268 5.32484 5.32435 2.33317 8.99935 2.33317C12.6743 2.33317 15.666 5.32484 15.666 8.99984C15.666 12.6748 12.6743 15.6665 8.99935 15.6665Z" fill="#1B2228" />
                                                </svg>
                                                <div className="tooltip-text">
                                                    Please make sure that name matches your government issued ID.
                                                </div>
                                            </div>
                                        </div>

                                        <div className={kcClsx("kcInputWrapperClass")}>
                                            {/* First Name */}
                                            <input
                                                style={{
                                                    width: "100%",
                                                }}
                                                type="text"
                                                id="firstName"
                                                name="firstName"
                                                pattern="[A-Za-z' ]*"
                                                maxLength={35}
                                                className={kcClsx("kcInputClass")}
                                                aria-invalid={!!customErrors.firstName}
                                                value={firstName}
                                                onChange={(e) => {
                                                    const value = onlyLetters(e.target.value);
                                                    setFirstName(value);
                                                    setCustomErrors((prev) => {
                                                        const { firstName, ...rest } = prev;
                                                        return rest;
                                                    });
                                                }}
                                                onBlur={() => {
                                                    const trimmed = sanitizeName(firstName);
                                                    setFirstName(trimmed);

                                                    if (!trimmed) {
                                                        setCustomErrors((prev) => ({ ...prev, firstName: "Required" }));
                                                    }
                                                }}
                                            />
                                            {customErrors.firstName && (
                                                <span className={kcClsx("kcInputErrorMessageClass")} aria-live="polite" >
                                                    <ErrorIcon />
                                                    <span className="ml-4">

                                                        {customErrors.firstName}
                                                    </span>
                                                </span>
                                            )}



                                        </div>
                                    </div>
                                    {/* Last Name */}
                                    <div className={kcClsx("kcFormGroupClass")}>
                                        <div className={kcClsx("kcLabelWrapperClass")}>
                                            <label
                                                htmlFor="lastName"
                                                className="label-name "
                                            >
                                                Legal Last Name{<span style={{ color: "#EA445A" }}>*</span>}
                                            </label>
                                            <div className="icon icon-info ms-1">
                                                <svg width="16" height="16" style={{ marginTop: -4 }} viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                                                    <path d="M8.16602 4.83317H9.83268V6.49984H8.16602V4.83317ZM8.16602 8.1665H9.83268V13.1665H8.16602V8.1665ZM8.99935 0.666504C4.39935 0.666504 0.666016 4.39984 0.666016 8.99984C0.666016 13.5998 4.39935 17.3332 8.99935 17.3332C13.5993 17.3332 17.3327 13.5998 17.3327 8.99984C17.3327 4.39984 13.5993 0.666504 8.99935 0.666504ZM8.99935 15.6665C5.32435 15.6665 2.33268 12.6748 2.33268 8.99984C2.33268 5.32484 5.32435 2.33317 8.99935 2.33317C12.6743 2.33317 15.666 5.32484 15.666 8.99984C15.666 12.6748 12.6743 15.6665 8.99935 15.6665Z" fill="#1B2228" />
                                                </svg>
                                                <div className="tooltip-text">
                                                    Please make sure that name matches your government issued ID.
                                                </div>
                                            </div>
                                        </div>

                                        <div className={kcClsx("kcInputWrapperClass")}>
                                            <input
                                                style={{
                                                    width: "100%",
                                                }}
                                                type="text"
                                                id="lastName"
                                                name="lastName"
                                                maxLength={35}
                                                pattern="[A-Za-z]*"
                                                className={kcClsx("kcInputClass")}
                                                aria-invalid={!!customErrors.lastName}
                                                value={lastName}
                                                onChange={(e) => {
                                                    const value = onlyLetters(e.target.value);
                                                    setLastName(value);
                                                    setCustomErrors((prev) => {
                                                        const { lastName, ...rest } = prev;
                                                        return rest;
                                                    });
                                                }}
                                                onBlur={() => {
                                                    const trimmed = sanitizeName(lastName);
                                                    setLastName(trimmed);

                                                    if (!trimmed) {
                                                        setCustomErrors((prev) => ({ ...prev, lastName: "Required" }));
                                                    }
                                                }}
                                            />
                                            {customErrors.lastName && (
                                                <span className={kcClsx("kcInputErrorMessageClass")} aria-live="polite" >
                                                    <ErrorIcon />
                                                    <span className="ml-4">
                                                        {customErrors.lastName}</span>
                                                </span>
                                            )}
                                        </div>
                                    </div>

                                    <div className={kcClsx("kcFormGroupClass")}>
                                        <div className={kcClsx("kcLabelWrapperClass")}>
                                            <label className={kcClsx("kcLabelClass")}>Date of Birth{<span style={{ color: "#EA445A" }}>*</span>}</label>
                                        </div>


                                        <div className={kcClsx("kcInputWrapperClass")}>
                                            <input
                                                type="date"
                                                min="1900-01-01"
                                                max={todayStr}
                                                className={kcClsx("kcInputClass")}
                                                value={
                                                    dob.year && dob.month && dob.day
                                                        ? `${dob.year}-${dob.month.padStart(2, "0")}-${dob.day.padStart(2, "0")}`
                                                        : ""
                                                }
                                                onChange={(e) => {
                                                    // let user type freely
                                                    const value = e.target.value;
                                                    if (!value) {
                                                        setDob({ year: "", month: "", day: "" });
                                                        return;
                                                    }

                                                    // Valid date string -> parse it
                                                    const [y, m, d] = value.split("-");
                                                    // If appointment flow, enforce 18+
                                                    if (profileMode === "appointment" && !isAtLeast18(y, m, d)) {
                                                        setDob({ year: y, month: m, day: d }); // keep what user picked
                                                        setCustomErrors(prev => ({
                                                            ...prev,
                                                            dob: "You must be 18 or older to book an appointment on Leap. If you are a parent or guardian booking for an individual under 18, enter your own information first. You'll be prompted to enter the patient's information at a later step."
                                                        }));
                                                        return;
                                                    }


                                                    const [year, month, day] = value.split("-");
                                                    setDob({ year, month, day });
                                                    setCustomErrors(prev => {
                                                        const { dob, ...rest } = prev;
                                                        return rest;
                                                    });
                                                }}
                                                onBlur={(e) => {
                                                    const value = e.target.value.trim();
                                                    const input = e.target as HTMLInputElement;

                                                    if (!value) {
                                                        setCustomErrors(prev => ({ ...prev, dob: "Required" }));
                                                        setDob({ year: "", month: "", day: "" });
                                                        return;
                                                    }
                                                    const v = input.validity;
                                                    if (!input.checkValidity()) {
                                                        setCustomErrors(prev => ({
                                                            ...prev,
                                                            dob: v.rangeOverflow
                                                                ? "Future date cannot be selected"
                                                                : v.rangeUnderflow
                                                                    ? "Invalid date"
                                                                    : "Invalid date"
                                                        }));
                                                        return;
                                                    }

                                                    const [y, m, d] = value.split("-");
                                                    // If appointment flow, enforce 18+
                                                    if (profileMode === "appointment" && !isAtLeast18(y, m, d)) {
                                                        setDob({ year: y, month: m, day: d });
                                                        setCustomErrors(prev => ({
                                                            ...prev,
                                                            dob: "You must be 18 or older to book an appointment on Leap. If you are a parent or guardian booking for an individual under 18, enter your own information first. You'll be prompted to enter the patient's information at a later step."
                                                        }));
                                                        return;
                                                    }
                                                    setDob({ year: y, month: m, day: d });
                                                    setCustomErrors(prev => {
                                                        const { dob, ...rest } = prev;
                                                        return rest;
                                                    });
                                                }}
                                                aria-invalid={!!(customErrors.dob || customErrors.year || customErrors.month || customErrors.day)}
                                                style={{ padding: "0.45rem 0.75rem", height: "auto", textTransform: "uppercase" }}
                                            />



                                            {customErrors.year && <span className={kcClsx("kcInputErrorMessageClass")} aria-live="polite">
                                                <ErrorIcon />
                                                <span className="ml-4">
                                                    {customErrors.year}</span></span>}
                                            {customErrors.month && <span className={kcClsx("kcInputErrorMessageClass")} aria-live="polite">
                                                <ErrorIcon />
                                                <span className="ml-4">
                                                    {customErrors.month}</span></span>}
                                            {customErrors.day && <span className={kcClsx("kcInputErrorMessageClass")} aria-live="polite">
                                                <ErrorIcon />
                                                <span className="ml-4">
                                                    {customErrors.day}</span></span>}
                                            {/* {customErrors.dob && (
                                                <span id="input-error-dob" className={kcClsx("kcInputErrorMessageClass")} aria-live="polite">
                                                    <ErrorIcon />
                                                    <span className="ml-4">
                                                        {customErrors.dob}</span>
                                                </span>
                                            )} */}
                                            {customErrors.dob && (
                                                <>
                                                    {customErrors.dob.includes("You must be 18 or older to book an appointment") ? (
                                                        // Special rendering for the 18+ error
                                                        <div
                                                            id="input-error-dob"
                                                            // className={kcClsx("kcInputErrorMessageClass")}
                                                            className={`${kcClsx("kcInputErrorMessageClass")} dob-appointment-error`}
                                                            aria-live="polite"
                                                            
                                                        >
                                                            <ErrorIcon />
                                                            <span className="ml-4">
                                                                {customErrors.dob}
                                                            </span>
                                                        </div>
                                                    ) : (
                                                        // Default rendering for all other dob errors
                                                        <span
                                                            id="input-error-dob"
                                                            className={kcClsx("kcInputErrorMessageClass")}
                                                            aria-live="polite"
                                                        >
                                                            <ErrorIcon />
                                                            <span className="ml-4">{customErrors.dob}</span>
                                                        </span>
                                                    )}
                                                </>
                                            )}

                                        </div>

                                        {/* Hidden field to send combined dob */}
                                        <input
                                            type="hidden"
                                            name="user.attributes.dob"
                                            value={
                                                dob.year && dob.month && dob.day
                                                    ? `${dob.year}-${dob.month.padStart(2, "0")}-${dob.day.padStart(2, "0")}`
                                                    : ""
                                            }
                                        />
                                    </div>



                                    {/* Gender */}
                                    <div className="basic-form-gender">
                                        <div className={kcClsx("kcFormGroupClass")}>
                                            <div className={kcClsx("kcLabelWrapperClass")}>
                                                <label className={kcClsx("kcLabelClass")} htmlFor="gender">Sex{<span style={{ color: "#EA445A" }}>*</span>}</label>
                                            </div>
                                            <div className={kcClsx("kcInputWrapperClass")}>
                                                <label className={gender === "Male" ? "selected" : undefined}>
                                                    <input
                                                        type="radio"
                                                        name="user.attributes.gender"
                                                        value="Male"
                                                        checked={gender === "Male"}
                                                        onChange={(e) => setGender(e.target.value)}
                                                        aria-invalid={!!customErrors.gender}
                                                    />
                                                    Male
                                                </label>

                                                <label className={gender === "Female" ? "selected" : undefined}>
                                                    <input
                                                        type="radio"
                                                        name="user.attributes.gender"
                                                        value="Female"
                                                        checked={gender === "Female"}
                                                        onChange={(e) => setGender(e.target.value)}
                                                        aria-invalid={!!customErrors.gender}
                                                    />
                                                    Female
                                                </label>

                                                <label className={gender === "Other" ? "selected" : undefined}>
                                                    <input
                                                        type="radio"
                                                        name="user.attributes.gender"
                                                        value="Other"
                                                        checked={gender === "Other"}
                                                        onChange={(e) => setGender(e.target.value)}
                                                        aria-invalid={!!customErrors.gender}
                                                    />
                                                    Other
                                                </label>
                                            </div>
                                            {customErrors.gender && (
                                                <div className={kcClsx("kcInputWrapperClass")}>
                                                    <span id="input-error-gender" className={kcClsx("kcInputErrorMessageClass")} aria-live="polite">
                                                        <ErrorIcon />
                                                        <span className="ml-4">
                                                            {customErrors.gender}</span>
                                                    </span>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                    <div className={kcClsx("kcFormGroupClass")}>
                                        <div className={kcClsx("kcLabelWrapperClass")}>
                                            <label
                                                className={kcClsx("kcLabelClass")}
                                                htmlFor="cell"
                                            >
                                                Cell
                                            </label>
                                        </div>
                                        <div className={kcClsx("kcInputWrapperClass")}>
                                            <input
                                                type="text"
                                                name="user.attributes.cell"
                                                id="cell"
                                                className="kcInputClass pf-c-form-control"
                                                inputMode="numeric"   // mobile numeric keypad
                                                autoComplete="tel"    // better autofill
                                                value={cell}
                                                // onChange={handleCellChange}
                                                onChange={(e) => {
                                                    handleCellChange(e); // handle formatting logic
                                                    setCustomErrors((prev) => {
                                                        const { cell, ...rest } = prev;
                                                        return rest;
                                                    });
                                                }}
                                                onBlur={() => {
                                                    const digitsOnly = cell.replace(/\D/g, ""); // remove formatting chars
                                                    if (digitsOnly.length < 1)
                                                        return;
                                                    // if (cell)
                                                    //     safeSet("cell", cell);
                                                    if (digitsOnly.length < 10) {
                                                        setCustomErrors(prev => ({
                                                            ...prev,
                                                            cell: "Please enter a valid cell number (10 digits)"
                                                        }));
                                                    } else {
                                                        // clear the error if valid
                                                        setCustomErrors(prev => {
                                                            const { cell, ...rest } = prev;
                                                            return rest;
                                                        });
                                                    }
                                                }}

                                            />
                                            {customErrors.cell && (
                                                <span className={kcClsx("kcInputErrorMessageClass")} aria-live="polite" >
                                                    <ErrorIcon />
                                                    <span className="ml-4">
                                                        {customErrors.cell}</span>
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                </>
                            ) : (
                                <>

                                    <input type="hidden" name="firstName" value={firstName} />
                                    <input type="hidden" name="lastName" value={lastName} />
                                    <input type="hidden" name="user.attributes.invitation_id" value={invitationId} />
                                    <input
                                        type="hidden"
                                        name="user.attributes.dob"
                                        value={
                                            dob.year && dob.month && dob.day
                                                ? `${dob.year}-${dob.month.padStart(2, "0")}-${dob.day.padStart(2, "0")}`
                                                : ""
                                        }
                                    />
                                </>
                            )}

                            {/* Passwords */}
                            {PasswordFields()}

                        </div>
                    </>

                    {termsAcceptanceRequired && (
                        <TermsAcceptance
                            i18n={i18n}
                            kcClsx={kcClsx}
                            messagesPerField={messagesPerField}
                            areTermsAccepted={areTermsAccepted}
                            onAreTermsAcceptedValueChange={setAreTermsAccepted}
                        />
                    )}
                    {recaptchaRequired && (recaptchaVisible || recaptchaAction === undefined) && (
                        <div className="form-group">
                            <div className={kcClsx("kcInputWrapperClass")}>
                                <div className="g-recaptcha" data-size="compact" data-sitekey={recaptchaSiteKey} data-action={recaptchaAction}></div>
                            </div>
                        </div>
                    )}
                    <div className={kcClsx("kcFormGroupClass")}>


                        {recaptchaRequired && !recaptchaVisible && recaptchaAction !== undefined ? (
                            <div id="kc-form-buttons" className={kcClsx("kcFormButtonsClass")}>
                                <button
                                    className={clsx(
                                        kcClsx("kcButtonClass", "kcButtonPrimaryClass", "kcButtonBlockClass", "kcButtonLargeClass"),
                                        "g-recaptcha"
                                    )}
                                    data-sitekey={recaptchaSiteKey}
                                    data-callback={() => {
                                        (document.getElementById("kc-register-form") as HTMLFormElement).submit();
                                    }}
                                    data-action={recaptchaAction}
                                    disabled={!canContinue || submitting}
                                    // aria-disabled={!canContinue}
                                    type="submit"
                                >
                                    {msg("doRegister")}
                                    {/* Save & Continue */}
                                </button>
                            </div>
                        ) : (
                            <div id="kc-form-buttons" className={kcClsx("kcFormButtonsClass")}>
                                <input
                                    className={isLeapHealth ? kcClsx("kcButtonClass", "kcButtonPrimaryClass", "kcButtonBlockClass", "kcButtonLargeClass") : kcClsx("kcButtonClass", "kcButtonPrimaryClass", "kcButtonBlockClass", "kcButtonLargeClass")}
                                    type="submit"
                                    // value={msgStr("doRegister")}
                                    value={"Continue"}
                                    disabled={!canContinue || submitting}
                                />
                            </div>
                        )}
                    </div>
                    {profileMode == "basic" ? (
                        <div className="register-form-basic">
                            <div className="text-center already-have-account"><span className="">Already have an account? </span> <a href={loginUrl} >Log in</a></div>

                        </div>
                    ) : (undefined)}
                </form>
            </TemplateComponent>
        </>
    );

    function EmailField() {
        const isLeapWeb = profileMode === "activationCode";
        return (
            <>
                <div className={kcClsx("kcFormGroupClass")}>
                    <div className={kcClsx("kcLabelWrapperClass")}>
                        <label htmlFor="email" className={kcClsx("kcLabelClass")}>
                            Email{!isLeapWeb && <span style={{ color: "#EA445A" }}>*</span>}
                            {isLeapWeb && <span style={{ color: "#555555", fontSize: 18, fontWeight: 400 }}> (Username)</span>}
                        </label>
                    </div>
                    <div className={kcClsx("kcInputWrapperClass")}>
                        <input
                            type="text"
                            id="email"
                            name="email"
                            maxLength={75}
                            className={kcClsx("kcInputClass")}
                            value={email}
                            readOnly={isLeapWeb}
                            autoComplete="email"
                            tabIndex={isLeapWeb ? -1 : 0}                          // 1) remove from tab order
                            onMouseDown={(e) => { if (isLeapWeb) e.preventDefault(); }} // 2) block mouse focus
                            style={isLeapWeb ? { backgroundColor: "#f5f5f5", cursor: "default" } : undefined}
                            onChange={(e) => {
                                if (!isLeapWeb) {
                                    // setEmail(e.target.value);
                                    const sanitizedValue = e.target.value.replace(/[^a-zA-Z0-9._@]/g, '');
                                    setEmail(sanitizedValue);
                                    if (emailTouched) {
                                        setEmailError(validateEmail(e.target.value, false));
                                    }
                                    setHideServerError(true);
                                }
                            }}
                            onBlur={() => {
                                if (!isLeapWeb) {
                                    setEmailTouched(true);
                                    setEmailError(validateEmail(email, true));
                                }
                            }}
                            onFocus={(e) => {
                                if (!isLeapWeb) {
                                    if (!emailTouched) setEmailTouched(true);
                                }
                                else {
                                    e.currentTarget.blur(); return;
                                }
                            }}
                            aria-invalid={(serverEmailError && !hideServerError) || (!!emailError && emailTouched)}
                        />
                        {serverEmailError && !hideServerError ? (
                            <span className={kcClsx("kcInputErrorMessageClass")}>
                                <ErrorIcon />
                                <span className="ml-4">
                                    Email already exists</span></span>
                            // This email is already in use</span><a href={loginUrl}>Log in</a></span>

                        ) : emailTouched && emailError ? (
                            <span className={kcClsx("kcInputErrorMessageClass")}>
                                <ErrorIcon />
                                <span className="ml-4">
                                    {emailError}</span></span>
                        ) : null}

                    </div>
                </div>
                <input type="hidden" name="username" value={email} />
            </>
        )
    }

    function PasswordFields() {
        return <>
            {/* Password Field */}
            <div className={kcClsx("kcFormGroupClass")}>
                <div className={kcClsx("kcLabelWrapperClass")}>
                    <label htmlFor="password" className={kcClsx("kcLabelClass")}>Password{<span style={{ color: "#EA445A" }}>*</span>}</label>
                </div>
                <div className={kcClsx("kcInputWrapperClass")}>
                    <div className="kcInputGroup pf-c-input-group" style={{ display: "flex", flexDirection: "column" }}>
                        {/* password input */}
                        <div style={{ display: "flex" }}>

                            <input
                                ref={passwordInputRef}
                                type={isPasswordVisible ? "text" : "password"}
                                name="password"
                                id="password"
                                className={`${kcClsx("kcInputClass")} pf-c-form-control`}
                                value={password}
                                aria-invalid={!!customErrors.password}
                                autoComplete="new-password"
                                maxLength={20}
                                onChange={(e) => {
                                    const next = e.target.value;
                                    setPassword(next);
                                    syncConfirmError(next, confirmPassword);
                                }}
                                onBlur={(e) => {
                                    const v = e.currentTarget.value.trim();
                                    setCustomErrors(prev =>
                                        v ? { ...prev, password: undefined } : { ...prev, password: "Required" }
                                    );
                                }}
                                onFocus={() => setHasPasswordFocused(true)}
                            />
                            <button
                                type="button"
                                className="kcFormPasswordVisibilityButtonClass pf-c-button pf-m-control"
                                aria-label={isPasswordVisible ? "Hide password" : "Show password"}
                                onClick={() => {
                                    setIsPasswordVisible(prev => !prev);
                                    setTimeout(() => {
                                        if (passwordInputRef.current) {
                                            passwordInputRef.current.focus();
                                            const len = passwordInputRef.current.value.length;
                                            passwordInputRef.current.setSelectionRange(len, len);
                                        }
                                    }, 0);
                                }}
                            >
                                <span aria-hidden="true">
                                    {isPasswordVisible ? (
                                        <svg style={{ width: "100%", height: "100%" }}
                                            width="20"
                                            height="17"
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
                        {customErrors.password && (
                            <span className={kcClsx("kcInputErrorMessageClass")} aria-live="polite" style={{
                                display: "flex",
                                alignItems: "center",
                                lineHeight: "normal",
                                marginTop: "3px"
                            }}>
                                <ErrorIcon />
                                <span className="ml-4">
                                    {customErrors.password}</span>
                            </span>
                        )}
                    </div>
                </div>
            </div>
            {/* Confirm Password */}
            <div className={kcClsx("kcFormGroupClass")}>
                <div className={kcClsx("kcLabelWrapperClass")}>
                    <label htmlFor="password-confirm" className={kcClsx("kcLabelClass")}>Confirm Password{<span style={{ color: "#EA445A" }}>*</span>}</label>
                </div>

                <div className={kcClsx("kcInputWrapperClass")}>
                    <div className="kcInputGroup pf-c-input-group" style={{ display: "flex", flexDirection: "column" }}>
                        <div style={{ display: "flex" }}>
                            <input
                                id="password-confirm"
                                name="password-confirm"
                                // placeholder="Confirm Password"
                                type={isConfirmVisible ? "text" : "password"}
                                className={`${kcClsx("kcInputClass")} pf-c-form-control`}
                                value={confirmPassword}
                                maxLength={20}
                                onChange={(e) => {
                                    const value = e.target.value;
                                    setConfirmPassword(value);
                                    syncConfirmError(password, value);
                                }}
                                onBlur={() => {
                                    if (!confirmPassword) {
                                        setCustomErrors((prev) => ({ ...prev, ["password-confirm"]: "Required" }));
                                    }
                                }}
                                onCopy={(e) => e.preventDefault()}
                                onCut={(e) => e.preventDefault()}
                                onPaste={(e) => e.preventDefault()}
                                onDragStart={(e) => e.preventDefault()}
                                aria-invalid={!!customErrors["password-confirm"]}
                            />

                            <button
                                type="button"
                                className="kcFormPasswordVisibilityButtonClass pf-c-button pf-m-control"
                                aria-label={isConfirmVisible ? "Hide password" : "Show password"}
                                onClick={() => {
                                    setIsConfirmVisible(v => !v);
                                    requestAnimationFrame(() => {
                                        const el = document.getElementById("password-confirm-visible") as HTMLInputElement | null;
                                        if (el) {
                                            el.focus();
                                            const len = el.value.length;
                                            try { el.setSelectionRange(len, len); } catch { }
                                        }
                                    });
                                }}
                            >
                                <span aria-hidden="true" className="password-reset-icon">
                                    {isConfirmVisible ? (
                                        <svg style={{ width: "100%", height: "100%" }}
                                            width="20"
                                            height="17"
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
                        {customErrors["password-confirm"] && (
                            <span className={kcClsx("kcInputErrorMessageClass")} aria-live="polite" style={{
                                display: "flex",
                                alignItems: "center",
                                lineHeight: "normal",
                                marginTop: "3px"
                            }}>
                                <ErrorIcon />
                                <span className="ml-4">
                                    {customErrors["password-confirm"]}
                                </span>
                            </span>
                        )}
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
            </div>
        </>
    }



    function ProfileContext() {
        // helpers
        const fullName = (f?: string, l?: string) =>
            [f, l].filter(Boolean).join(" ").trim() || "â€”";

        const initials = (f?: string, l?: string) =>
            [f?.[0], l?.[0]].filter(Boolean).join("").toUpperCase() || "â€¢";

        const prettyDob = (dob?: string) => {
            if (!dob) return "â€”";
            const [year, month, day] = dob.split("-");
            if (!year || !month || !day) return "â€”";

            const monthNames = [
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            ];

            const mi = Number(month) - 1;
            if (isNaN(mi) || mi < 0 || mi > 11) return "â€”";

            return `${monthNames[mi]} ${day.padStart(2, "0")}, ${year}`;
        };


        const formatWhen = (iso?: string, tzLabel?: string) => {
            if (!iso) return "â€”";
            const dt = new Date(iso);
            if (isNaN(dt.getTime())) return "â€”";
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
                    className="profile-context "
                    style={{
                        display: "flex", alignItems: "start", gap: 20,
                        padding: "12px", border: "1px solid #EAEBEF",
                        borderRadius: 10, background: "#F9FAFB",
                    }}
                >
                    {/* Avatar (photo or initials) */}
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
                                    {String(a.visitType).trim().toLowerCase() === "video visit" ? (
                                        // video icon
                                        <svg width="21" height="21" viewBox="0 0 21 21" fill="none" xmlns="http://www.w3.org/2000/svg" aria-label="Video visit">
                                            <path d="M13.125 8.75L17.1089 6.7585C17.2422 6.69186 17.3904 6.66039 17.5394 6.6671C17.6883 6.67381 17.8331 6.71847 17.9599 6.79683C18.0867 6.87519 18.1915 6.98467 18.2641 7.11486C18.3367 7.24506 18.3749 7.39166 18.375 7.54075V13.4592C18.3749 13.6083 18.3367 13.7549 18.2641 13.8851C18.1915 14.0153 18.0867 14.1248 17.9599 14.2032C17.8331 14.2815 17.6883 14.3262 17.5394 14.3329C17.3904 14.3396 17.2422 14.3081 17.1089 14.2415L13.125 12.25V8.75ZM2.625 7C2.625 6.53587 2.80937 6.09075 3.13756 5.76256C3.46575 5.43437 3.91087 5.25 4.375 5.25H11.375C11.8391 5.25 12.2842 5.43437 12.6124 5.76256C12.9406 6.09075 13.125 6.53587 13.125 7V14C13.125 14.4641 12.9406 14.9092 12.6124 15.2374C12.2842 15.5656 11.8391 15.75 11.375 15.75H4.375C3.91087 15.75 3.46575 15.5656 3.13756 15.2374C2.80937 14.9092 2.625 14.4641 2.625 14V7Z"
                                                stroke="#765AEB" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                                        </svg>
                                    ) : (
                                        // location icon
                                        <svg width="13" height="20" viewBox="0 0 13 20" fill="none" xmlns="http://www.w3.org/2000/svg" aria-label="In-person visit">
                                            <path d="M6.50139 1.66699C3.28714 1.66699 0.6875 4.27533 0.6875 7.50033C0.6875 11.8753 6.50139 18.3337 6.50139 18.3337C6.50139 18.3337 12.3153 11.8753 12.3153 7.50033C12.3153 4.27533 9.71564 1.66699 6.50139 1.66699ZM2.34861 7.50033C2.34861 5.20033 4.20906 3.33366 6.50139 3.33366C8.79372 3.33366 10.6542 5.20033 10.6542 7.50033C10.6542 9.90033 8.26217 13.492 6.50139 15.7337C4.77383 13.5087 2.34861 9.87533 2.34861 7.50033Z" fill="#7F65ED" />
                                            <path d="M6.50139 9.58366C7.64815 9.58366 8.57778 8.65092 8.57778 7.50033C8.57778 6.34973 7.64815 5.41699 6.50139 5.41699C5.35463 5.41699 4.425 6.34973 4.425 7.50033C4.425 8.65092 5.35463 9.58366 6.50139 9.58366Z" fill="#7F65ED" />
                                        </svg>
                                    )}
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

        // ----- activationCode card (name + DOB, no picture) -----
        const b = activationCodeProfile;

        return (
            <div
                className="profile-context mb-3"
                style={{
                    display: "flex", alignItems: "center", gap: 12,
                    padding: "12px 16px", border: "1px solid #E8E8F2",
                    borderRadius: 12, background: "#fff",
                }}
            >
                <div className="d-flex justify-content-center" style={{ lineHeight: 1.2, gap: 16 }}>
                    <div className="user-initial" style={{
                        width: 44,
                        height: 44,
                        borderRadius: 12,
                        background: "#F4F5FA",
                        border: "1px solid #E8E8F2",
                        display: "grid",
                        placeItems: "center",
                        lineHeight: 0,     // prevent baseline gap
                        flexShrink: 0,     // avoid shrinking in flex
                    }}>
                        {/* <img src="https://www.curemd.com/cmd/cmd-assets/images/people/dr-lilly-rose.jpg" alt="" /> */}
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            width="24"
                            height="24"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="#6B7280"
                            strokeWidth="1.8"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            role="img"
                            aria-label="User"
                            style={{ display: "block" }}
                        >
                            <circle cx="12" cy="8" r="4" />
                            <path d="M4.5 20a8.5 8.5 0 0 1 15 0" />
                        </svg>
                    </div>
                    <div className="user-detail">
                        <span style={{ fontWeight: 700, fontSize: 18, }}>{fullName(b.firstName, b.lastName)}</span>
                        <span className="d-block" style={{ color: "#6B7280", fontSize: 14, marginTop: 4 }}>{prettyDob(b.dob)}</span>
                    </div>
                </div>
            </div>
        );
    }

    function ErrorIcon() {
        return (
            <div className="align-self-center error-icon-style">

                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 17 18" fill="none">
                    <path d="M7.66797 11.5003H9.33464V13.167H7.66797V11.5003ZM7.66797 4.83366H9.33464V9.83366H7.66797V4.83366ZM8.49297 0.666992C3.89297 0.666992 0.167969 4.40033 0.167969 9.00033C0.167969 13.6003 3.89297 17.3337 8.49297 17.3337C13.1013 17.3337 16.8346 13.6003 16.8346 9.00033C16.8346 4.40033 13.1013 0.666992 8.49297 0.666992ZM8.5013 15.667C4.81797 15.667 1.83464 12.6837 1.83464 9.00033C1.83464 5.31699 4.81797 2.33366 8.5013 2.33366C12.1846 2.33366 15.168 5.31699 15.168 9.00033C15.168 12.6837 12.1846 15.667 8.5013 15.667Z" fill="#FF0000" />
                </svg>
            </div>

        )
    }
}

function TermsAcceptance(props: {
    i18n: I18n;
    kcClsx: KcClsx;
    messagesPerField: Pick<KcContext["messagesPerField"], "existsError" | "get">;
    areTermsAccepted: boolean;
    onAreTermsAcceptedValueChange: (areTermsAccepted: boolean) => void;
}) {
    const { i18n, kcClsx, messagesPerField, areTermsAccepted, onAreTermsAcceptedValueChange } = props;

    const { msg } = i18n;

    return (
        <>
            <div className="form-group">
                <div className={kcClsx("kcInputWrapperClass")}>
                    {msg("termsTitle")}
                    <div id="kc-registration-terms-text">{msg("termsText")}</div>
                </div>
            </div>
            <div className="form-group">
                <div className={kcClsx("kcLabelWrapperClass")}>
                    <input
                        type="checkbox"
                        id="termsAccepted"
                        name="termsAccepted"
                        className={kcClsx("kcCheckboxInputClass")}
                        checked={areTermsAccepted}
                        onChange={e => onAreTermsAcceptedValueChange(e.target.checked)}
                        aria-invalid={messagesPerField.existsError("termsAccepted")}
                    />
                    <label htmlFor="termsAccepted" className={kcClsx("kcLabelClass")}>
                        {msg("acceptTerms")}
                    </label>
                </div>
                {messagesPerField.existsError("termsAccepted") && (
                    <div className={kcClsx("kcLabelWrapperClass")}>
                        <span
                            id="input-error-terms-accepted"
                            className={kcClsx("kcInputErrorMessageClass")}
                            aria-live="polite"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 17 18" fill="none">
                                <path d="M7.66797 11.5003H9.33464V13.167H7.66797V11.5003ZM7.66797 4.83366H9.33464V9.83366H7.66797V4.83366ZM8.49297 0.666992C3.89297 0.666992 0.167969 4.40033 0.167969 9.00033C0.167969 13.6003 3.89297 17.3337 8.49297 17.3337C13.1013 17.3337 16.8346 13.6003 16.8346 9.00033C16.8346 4.40033 13.1013 0.666992 8.49297 0.666992ZM8.5013 15.667C4.81797 15.667 1.83464 12.6837 1.83464 9.00033C1.83464 5.31699 4.81797 2.33366 8.5013 2.33366C12.1846 2.33366 15.168 5.31699 15.168 9.00033C15.168 12.6837 12.1846 15.667 8.5013 15.667Z" fill="#FF0000" />
                            </svg>
                            <span className="ml-4">
                                {kcSanitize(messagesPerField.get("termsAccepted"))}
                            </span>
                        </span>
                    </div>
                )}
            </div>
        </>
    );
}


