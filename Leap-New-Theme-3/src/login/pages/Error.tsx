import type { PageProps } from "keycloakify/login/pages/PageProps";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import { useEffect, useState } from "react";



export default function Error(props: PageProps<Extract<KcContext, { pageId: "error.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { message, client, skipLink } = kcContext;
    const { msg } = i18n;
    const [isCookieError, setCookieError] = useState(false);
    const [isSessionExistsError, setSessionExistsError] = useState(false);
    const [kcFlow, setKcFlow] = useState("");

    useEffect(() => {
        if (message.summary.includes("Cookie not found")) {
            setCookieError(true);
            const flowEndpoint = sessionStorage.getItem("flowEntrypoint");
            const keycloakFlow = sessionStorage.getItem("kc_flow");
            if (keycloakFlow) {
                setKcFlow(keycloakFlow);
            }
            if (flowEndpoint) {
                const target = new URL(flowEndpoint);
                const url = target.toString();
                if (window.top && window.top !== window) {
                    window.top.location.replace(url);
                } else {
                    window.location.replace(url);
                }
            }
        }
        else if (message.summary.includes("You are already authenticated as different user")) {
            setSessionExistsError(true);
        }
    }, [message]);

    const onSilentLogout = async() => {
        let clientUrl = client.baseUrl;
        if (!clientUrl) {
            if (sessionStorage.getItem("redirectUrl") != null)
                clientUrl = sessionStorage.getItem("redirectUrl") ?? undefined;
            if (!clientUrl)
                return;
        }

        const target = new URL(clientUrl);
        const redirectUrl = target.toString();

        const basePath = window.location.href.split("/login-actions")[0];
        const logoutUrl = basePath + "/force-logout?redirect_uri=" + encodeURIComponent(redirectUrl);
        return logoutUrl;
    }

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            displayMessage={false}
            headerNode={msg("errorTitle")}
        >
            {!isCookieError ? (
                <>
                    <div id="kc-error-message">
                        <p className="instruction" dangerouslySetInnerHTML={{ __html: kcSanitize(message.summary) }} />
                        {isSessionExistsError && (
                            <div style={{ marginTop: "1rem" }}>
                                <button
                                    type="button"
                                    className="btn leap-button mb-lg-4 mb-3 w-100"
                                    onClick={async () => {
                                        const logoutUrl = await onSilentLogout();
                                        if (logoutUrl) {
                                            window.location.href = logoutUrl;
                                        }
                                    }}
                                >
                                    Logout
                                </button>

                            </div>
                        )}
                        {!isSessionExistsError && !skipLink && client !== undefined && client.baseUrl !== undefined && (
                            <p>

                                <a id="backToApplication" href={client.baseUrl}>
                                    {msg("backToApplication")}
                                </a>
                            </p>
                        )}
                    </div>
                </>
            ) : (
                <>
                    <span>Redirecting to {kcFlow}</span>
                </>
            )
            }
        </Template >
    );
}
