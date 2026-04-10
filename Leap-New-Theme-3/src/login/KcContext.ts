/* eslint-disable @typescript-eslint/no-empty-object-type */
import type { ExtendKcContext } from "keycloakify/login";
import type { KcEnvName, ThemeName } from "../kc.gen";

export type KcContextExtension = {
    themeName: ThemeName;
    properties: Record<KcEnvName, string> & {};
    // NOTE: Here you can declare more properties to extend the KcContext
    // See: https://docs.keycloakify.dev/faq-and-help/some-values-you-need-are-missing-from-in-kccontext
};

export type KcContextExtensionPerPage = {
        "otp-form.ftl": {
        auth: {
            attemptedUsername: string;
        };
        url: {
            loginRestartFlowUrl: string;
            loginAction: string;
        };
        attributes?: {
            userEmail?: string;
            userMobileNumber?: string;
            expirySeconds?:number;
            customOtpError?:string;
            length?:number;
            resendTime?: number;
        };
    };
        "oha.ftl": {
        auth: {
            attemptedUsername: string;
        };
        url: {
            loginRestartFlowUrl: string;
            loginAction: string;
        };
    };

};

export type KcContext = ExtendKcContext<KcContextExtension, KcContextExtensionPerPage>;
