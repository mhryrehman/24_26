import { Suspense, lazy } from "react";
import type { ClassKey } from "keycloakify/login";
import type { KcContext } from "./KcContext";
import { useI18n } from "./i18n";
import DefaultPage from "keycloakify/login/DefaultPage";
import Template from "./Template";
import OtpForm from "./pages/OtpForm";
import LoginResetPassword from "./pages/LoginResetPassword";
import Login from "./pages/Login";
import Register from "./pages/Register";
import LoginUpdatePassword from "./pages/LoginUpdatePassword";
import Terms from "./pages/Terms";
import Oha from "./pages/Oha";
import Error from "./pages/Error";
import LoginPageExpired from "./pages/LoginPageExpired";


const UserProfileFormFields = lazy(
    () => import("./UserProfileFormFields")
);

const doMakeUserConfirmPassword = true;

export default function KcPage(props: { kcContext: KcContext }) {
    const { kcContext } = props;

    const { i18n } = useI18n({ kcContext });

  return (
    <Suspense>
      {(() => {
        switch (kcContext.pageId) {
          case "otp-form.ftl":
            return (
              <OtpForm
                kcContext={kcContext as Extract<KcContext, { pageId: "otp-form.ftl" }>}
                i18n={i18n}
                doUseDefaultCss={true}
                Template={Template}
                classes={{}}
              />
            );

            case "login-reset-password.ftl":
              return (
                <LoginResetPassword
                kcContext={kcContext as Extract<KcContext, {pageId: "login-reset-password.ftl"}>}
                i18n={i18n}
                doUseDefaultCss={true}
                Template={Template}
                classes={{}}
              />
              );
          case "login-update-password.ftl":
            return (
              <LoginUpdatePassword
                kcContext={kcContext as Extract<KcContext, { pageId: "login-update-password.ftl" }>}
                i18n={i18n}
                doUseDefaultCss={true}
                Template={Template}
                classes={{}}
              />
            );  

            case "login.ftl":
              return (
                <Login
                kcContext={kcContext as Extract<KcContext, {pageId: "login.ftl"}>}
                i18n={i18n}
                doUseDefaultCss={true}
                Template={Template}
                classes={{}}
              />
              );

            case "register.ftl":
              return (
                <Register
                kcContext={kcContext as Extract<KcContext, {pageId: "register.ftl"}>}
                i18n={i18n}
                doUseDefaultCss={true}
                Template={Template}
                classes={{}}
                UserProfileFormFields={UserProfileFormFields}
                doMakeUserConfirmPassword={doMakeUserConfirmPassword}
              />
              );

            case "terms.ftl":
              return (
                <Terms
                kcContext={kcContext as Extract<KcContext, {pageId: "terms.ftl"}>}
                i18n={i18n}
                doUseDefaultCss={true}
                Template={Template}
                classes={{}}
                // UserProfileFormFields={UserProfileFormFields}
                // doMakeUserConfirmPassword={doMakeUserConfirmPassword}
              />
              );
            case "oha.ftl":
              return (
                <Oha
                kcContext={kcContext as Extract<KcContext, {pageId: "oha.ftl"}>}
                i18n={i18n}
                doUseDefaultCss={true}
                Template={Template}
                classes={{}}
                // UserProfileFormFields={UserProfileFormFields}
                // doMakeUserConfirmPassword={doMakeUserConfirmPassword}
              />
              );

            case "error.ftl":
              return (
                <Error
                kcContext={kcContext as Extract<KcContext, {pageId: "error.ftl"}>}
                i18n={i18n}
                doUseDefaultCss={true}
                Template={Template}
                classes={{}}
                // UserProfileFormFields={UserProfileFormFields}
                // doMakeUserConfirmPassword={doMakeUserConfirmPassword}
              />
              );
            case "login-page-expired.ftl":
              return (
                <LoginPageExpired
                kcContext={kcContext as Extract<KcContext, {pageId: "login-page-expired.ftl"}>}
                i18n={i18n}
                doUseDefaultCss={true}
                Template={Template}
                classes={{}}
                // UserProfileFormFields={UserProfileFormFields}
                // doMakeUserConfirmPassword={doMakeUserConfirmPassword}
              />
              );

          default:
            return (
              <DefaultPage
                kcContext={kcContext}
                i18n={i18n}
                classes={classes}
                Template={Template}
                doUseDefaultCss={true}
                UserProfileFormFields={UserProfileFormFields}
                doMakeUserConfirmPassword={doMakeUserConfirmPassword}
              />
            );
        }
      })()}
    </Suspense>
  );
}

const classes = {} satisfies { [key in ClassKey]?: string };
