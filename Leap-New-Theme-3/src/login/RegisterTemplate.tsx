import { useEffect } from "react";
import { clsx } from "keycloakify/tools/clsx";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { TemplateProps } from "keycloakify/login/TemplateProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import { useSetClassName } from "keycloakify/tools/useSetClassName";
import { useInitialize } from "keycloakify/login/Template.useInitialize";
import type { I18n } from "./i18n";
import type { KcContext } from "./KcContext";

export default function RegisterTemplate(props: TemplateProps<KcContext, I18n>) {
    const {
        displayInfo = false,
        displayMessage = true,
        displayRequiredFields = false,
        socialProvidersNode = null,
        infoNode = null,
        documentTitle,
        bodyClassName,
        kcContext,
        i18n,
        doUseDefaultCss,
        classes,
        children
    } = props;

    const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });

    const { msg, msgStr } = i18n;

    const { auth, url, message, isAppInitiatedAction } = kcContext;

    useEffect(() => {
        document.title = documentTitle ?? msgStr("loginTitle", kcContext.realm.displayName);
    }, []);

    useSetClassName({
        qualifiedName: "html",
        className: kcClsx("kcHtmlClass")
    });

    useSetClassName({
        qualifiedName: "body",
        className: bodyClassName ?? kcClsx("kcBodyClass")
    });

    const { isReadyToRender } = useInitialize({ kcContext, doUseDefaultCss });

    if (!isReadyToRender) {
        return null;
    }

    return (
        <>


            <div className="signup-auth-container auth-container">
                {/* <div className="leap-header">
                    <div className="leap-img">
                        <img src="data:image/png;base64,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" />
                    </div>                    
                    <div className="sub">Your Health Companion</div>
                </div> */}
                
                {/* <h3 className="text-center">Create an account</h3>
                <h6 className="text-center mb-3">Already have an account? <a href={url.loginUrl}>Login</a></h6> */}
                <div className={kcClsx("kcFormCardClass")}>
                    {false ? (
                        <header className={kcClsx("kcFormHeaderClass")}>
                            {(() => {
                                const node = !(auth !== undefined && auth?.showUsername && !auth?.showResetCredentials) ? (
                                    <></>
                                ) : (
                                    <div id="kc-username" className={kcClsx("kcFormGroupClass")}>
                                        <label id="kc-attempted-username">{auth?.attemptedUsername}</label>
                                        <a id="reset-login" href={url.loginRestartFlowUrl} aria-label={msgStr("restartLoginTooltip")}>
                                            <div className="kc-login-tooltip">
                                                <i className={kcClsx("kcResetFlowIcon")}></i>
                                                <span className="kc-tooltip-text">{msg("restartLoginTooltip")}</span>
                                            </div>
                                        </a>
                                    </div>
                                );

                                if (displayRequiredFields) {
                                    return (
                                        <div className={kcClsx("kcContentWrapperClass")}>
                                            <div className={clsx(kcClsx("kcLabelWrapperClass"), "subtitle")}>
                                                <span className="subtitle">
                                                    <span className="required">*</span>
                                                    {msg("requiredFields")}
                                                </span>
                                            </div>
                                            <div className="col-md-10">{node}</div>
                                        </div>
                                    );
                                }

                                return node;
                            })()}
                        </header>
                    ) : (
                        <></>
                    )}
                </div>
                <div>
                    <div>
                        {/* App-initiated actions should not see warning messages about the need to complete the action during login. */}
                        {displayMessage && message !== undefined && (message.type !== "warning" || !isAppInitiatedAction) && (
                            <div
                                className={clsx(
                                    `alert-${message.type}`,
                                    kcClsx("kcAlertClass"),
                                    `pf-m-${message?.type === "error" ? "danger" : message.type}`
                                )}
                            >
                                <div className="pf-c-alert__icon">
                                    {message.type === "success" && <span className={kcClsx("kcFeedbackSuccessIcon")}></span>}
                                    {message.type === "warning" && <span className={kcClsx("kcFeedbackWarningIcon")}></span>}
                                    {message.type === "error" && <span className={kcClsx("kcFeedbackErrorIcon")}></span>}
                                    {message.type === "info" && <span className={kcClsx("kcFeedbackInfoIcon")}></span>}
                                </div>
                                <span
                                    className={kcClsx("kcAlertTitleClass")}
                                    dangerouslySetInnerHTML={{
                                        __html: kcSanitize(message.summary)
                                    }}
                                />
                            </div>
                        )}
                        {children}
                        {auth !== undefined && auth.showTryAnotherWayLink && (
                            <form action={url.loginAction} method="post">
                                <div>
                                    <input type="hidden" name="tryAnotherWay" value="on" />
                                    <a
                                        href="#"
                                        id="try-another-way"
                                        onClick={() => {
                                            document.forms["kc-select-try-another-way-form" as never].submit();
                                            return false;
                                        }}
                                    >
                                        {msg("doTryAnotherWay")}
                                    </a>
                                </div>
                            </form>
                        )}
                        {socialProvidersNode}
                        {displayInfo && (
                            <div>
                                <div>{infoNode}</div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
            <div className="auth-chat">
                <span className="icon-help"></span>
            </div>
            {/* <div className="signup-auth-footer">


                <a href="https://leap.health/providers/privacy-policy/" target="_blank">Privacy Policy</a>
                <a href="https://leap.health/providers/terms-of-use/" target="_blank">Terms of Service</a>
                <span>&#169; 2025 Leap Health</span>
            </div> */}
        </>
    );
}
