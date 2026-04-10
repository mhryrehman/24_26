import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import { useEffect, useState } from "react";

export default function LoginPageExpired(props: PageProps<Extract<KcContext, { pageId: "login-page-expired.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;

    // const { url } = kcContext;

    const { msg } = i18n;
    const [kcFlow, setKcFlow] = useState("");

    useEffect(() => {

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
    }, []);



    return (
        <Template kcContext={kcContext} i18n={i18n} doUseDefaultCss={doUseDefaultCss} classes={classes} headerNode={msg("pageExpiredTitle")}>
            <span>Redirecting to {kcFlow}</span>

            {/* <p id="instruction1" className="instruction">
                {msg("pageExpiredMsg1")}
                <a id="loginRestartLink" href={url.loginRestartFlowUrl}>
                    {msg("doClickHere")}
                </a>{" "}
                .<br />
                {msg("pageExpiredMsg2")}{" "}
                <a id="loginContinueLink" href={url.loginAction}>
                    {msg("doClickHere")}
                </a>{" "}
                .
            </p> */}
        </Template>
    );
}
