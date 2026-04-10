import { useState, useEffect } from "react";
import { clsx } from "keycloakify/tools/clsx";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { TemplateProps } from "keycloakify/login/TemplateProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import { useSetClassName } from "keycloakify/tools/useSetClassName";
import { useInitialize } from "keycloakify/login/Template.useInitialize";
import type { I18n } from "./i18n";
import type { KcContext } from "./KcContext";
import appStore from "../../public/app-store.svg";
import careAlerts from "../../public/care-alerts.svg";
import manageAppointments from "../../public/manage-appointments.svg";
import digitalCheckin from "../../public/digital-checkin.svg";
import healthRecord from "../../public/health-record.svg";
import careTeamManagement from "../../public/care-team-manage.svg";
import playStore from "../../public/play-store.svg";
import testResults from "../../public/test-result.svg";
declare global {
    interface Window {
        Intercom?: any;
        intercomSettings?: Record<string, any>;
    }
}

export default function Template(props: TemplateProps<KcContext, I18n>) {
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
    const [isMobile, setIsMobile] = useState(false);
    // const intercomBootedRef = useRef(false);

    const isMobileDevice = (): boolean => {
        if (typeof navigator === "undefined") return false;

        return /Mobi|Android|iPhone|iPad|BlackBerry|IEMobile|Opera Mini/i.test(
            navigator.userAgent
        );
    };

    // const { pageId } = kcContext;
    // const pageClass = (kcContext.pageId ?? "").replace(/\.ftl$/, "");
    const [pageClass, setPageClass] = useState(
        (kcContext.pageId ?? "").replace(/\.ftl$/, "")
    );
    // console.log("pageclass",pageClass); 
    useEffect(() => {
        const urlParams = new URLSearchParams(window.location.search);
        const redirectUri = urlParams.get("redirect_uri");
        if (redirectUri) {
            // sessionStorage.setItem("redirectUri", redirectUri);
            try {
                const parsedRedirectUri = new URL(redirectUri);
                sessionStorage.setItem("redirectUri", parsedRedirectUri.toString());
            } catch (e) {
                // console.log("Invalid RedirectUri");
            }
        }
        // console.log("redirect uri",redirectUri);
        if (pageClass === "register") {
            // const urlParams = new URLSearchParams(window.location.search);
            const modeParam = urlParams.get("mode");
            if (modeParam || sessionStorage.getItem("mode")) {
                setPageClass(prev => prev + " activation-code"); // append class
            }
        }
    }, []);
    useEffect(() => {
        setIsMobile(isMobileDevice());
    }, []);

    useEffect(() => {
        const settings = {
            app_id: "byxrgcmv",
            ...(isMobile
                ? { hide_default_launcher: true }
                : { alignment: "right", horizontal_padding: 20, vertical_padding: 20 }),
        };

        if (typeof window.Intercom === "function") {
            window.Intercom("boot", settings);
        } else {
            const s = document.createElement("script");
            s.src = "https://widget.intercom.io/widget/byxrgcmv";
            s.async = true;
            s.onload = () => window.Intercom("boot", settings);
            document.head.appendChild(s);
        }
    }, [isMobile]);

    useEffect(() => {
        document.title = documentTitle ?? msgStr("loginTitle", kcContext.realm.displayName);
        // document.title = "storybook"

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


    const images = [
        {
            id: 1,
            src: manageAppointments,
            alt: "manage appointments",
            title: "Manage Appointments",
            subtitle: "View, schedule, and prepare for your upcoming appointments with just a tap."
        },
        {
            id: 2,
            src: digitalCheckin,
            alt: "digital check-in",
            title: "Digital Check-in",
            subtitle: "Complete your intake in minutes with our secure, step-by-step digital check-in."
        },
        {
            id: 3,
            src: careTeamManagement,
            alt: "Care Team Messaging",
            title: "Care Team Messaging",
            subtitle: "Stay connected with your care team—send and receive secure messages anytime, anywhere."
        },
        {
            id: 4,
            src: testResults,
            alt: "test result",
            title: "Test Results",
            subtitle: "Securely view all your lab work, radiology reports, and procedure results in one place."
        },
        {
            id: 5,
            src: healthRecord,
            alt: "health record",
            title: "Health Record",
            subtitle: "Access your complete health record—view notes, vitals, history, and more."
        },
        {
            id: 6,
            src: careAlerts,
            alt: "care-alerts",
            title: "Care Alerts",
            subtitle: "Stay on track with real-time care alerts for appointments, medications, test results, and messages."
        },
        {
            id: 7,
            type: "app-download",
            alt: "Download App",
            title: "Try the Leap App",
            subtitle: "Scan the QR Code to get the app"
        }
    ];
    const [currentIndex, setCurrentIndex] = useState(0);
    const [isAutoPlaying, setIsAutoPlaying] = useState(true);
    // const [isManuallyPaused, setIsManuallyPaused] = useState(false);
    // Auto-play functionality
    useEffect(() => {
        if (!isAutoPlaying) return;

        const interval = setInterval(() => {
            setCurrentIndex((prevIndex) =>
                prevIndex === images.length - 1 ? 0 : prevIndex + 1
            );
        }, 8000);

        return () => clearInterval(interval);
    }, [isAutoPlaying, images.length]);

    // Resume auto-play after user interaction
    useEffect(() => {
        // if (!isAutoPlaying && !isManuallyPaused) {
        if (!isAutoPlaying) {
            const resumeTimer = setTimeout(() => {
                setIsAutoPlaying(true);
            }, 8000);

            return () => clearTimeout(resumeTimer);
        }
        // }, [isAutoPlaying, isManuallyPaused]);
    }, [isAutoPlaying]);



    const goToPrevious = () => {
        setCurrentIndex(currentIndex === 0 ? images.length - 1 : currentIndex - 1);
        setIsAutoPlaying(false);
    };

    const goToNext = () => {
        setCurrentIndex(currentIndex === images.length - 1 ? 0 : currentIndex + 1);
        setIsAutoPlaying(false);
    };

    // const toggleAutoPlay = () => {
    //     setIsAutoPlaying(prev => !prev);
    //     setIsManuallyPaused(prev => !prev);
    // };


    // Check if current slide is the last one (app download)
    const isLastSlide = currentIndex === images.length - 1;
    if (!isReadyToRender) {
        return null;
    }

    const handleLogoClick = () => {
        const params = new URLSearchParams(window.location.search);
        const redirectUrl = params.get("redirect_uri");
        let target;

        if (redirectUrl) {
            try {
                target = new URL(redirectUrl).origin; // strip everything except origin
            } catch {
                // ignore
            }
        }
        if (!target) target = sessionStorage.getItem("redirectUri");

        if (target) {
            window.location.href = target;
        } else {
            // console.warn("No redirect origin found.");
        }
    };

    return (
        <>
            {isMobile && (
                <button
                    id="mobile-help"
                    className="top-help-btn"
                    onClick={() => window.Intercom?.("show")}
                >
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M9 14H11V16H9V14ZM10 0C4.48 0 0 4.48 0 10C0 15.52 4.48 20 10 20C15.52 20 20 15.52 20 10C20 4.48 15.52 0 10 0ZM10 18C5.59 18 2 14.41 2 10C2 5.59 5.59 2 10 2C14.41 2 18 5.59 18 10C18 14.41 14.41 18 10 18ZM10 4C7.79 4 6 5.79 6 8H8C8 6.9 8.9 6 10 6C11.1 6 12 6.9 12 8C12 10 9 9.75 9 13H11C11 10.75 14 10.5 14 8C14 5.79 12.21 4 10 4Z" fill="white" />
                    </svg>
                </button>


            )}

            {/* <a
                href="https://staging.leaphealth.ai:8087/"
                onClick={(e) => {
                    e.preventDefault();              // ensure we clear before leaving
                    sessionStorage.clear();          // <-- clears all sessionStorage keys
                    window.location.assign("https://staging.leaphealth.ai:8087/");
                }}
                rel="noopener noreferrer"
            >
                <img src={appStore} alt="Get the app" />
            </a> */}

            {/* <div className="signup-auth-container auth-container"> */}
            <div className={`signup-auth-container auth-container ${pageClass}`}>
                <div className="header-wrapper">
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                        <div className="leap-logo-header" onClick={handleLogoClick}>
                            <svg width="75" height="41" viewBox="0 0 134 73" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M107.083 51.2941C107.083 56.8746 107.083 62.4552 107.083 68.0357C107.159 69.0638 106.888 70.0883 106.312 70.9475C105.736 71.8067 104.886 72.4523 103.898 72.7832C103.172 73.0133 102.399 73.0618 101.649 72.9248C100.899 72.7877 100.196 72.4695 99.6008 71.9979C99.0059 71.5263 98.5382 70.9163 98.2401 70.2226C97.9421 69.5289 97.8226 68.773 97.8925 68.0225C97.872 63.4617 97.8925 58.9033 97.8925 54.3448C97.8925 48.1521 97.8659 41.9591 97.9069 35.7665C97.8525 33.3095 98.285 30.8658 99.182 28.5736C100.637 25.106 103.151 22.1727 106.372 20.1817C109.593 18.1908 113.362 17.241 117.154 17.4647C121.743 17.8018 126.033 19.8443 129.159 23.1808C132.286 26.5173 134.016 30.9001 134.003 35.4471C133.987 38.1285 133.35 40.7707 132.14 43.1711C130.931 45.5716 129.182 47.667 127.027 49.296C126.101 50.1053 124.958 50.6326 123.735 50.8145C122.513 50.9964 121.265 50.8251 120.139 50.3211C119.021 49.8972 118.041 49.1794 117.306 48.2457C116.571 47.3119 116.11 46.1974 115.969 45.0227C115.86 44.3349 115.99 44.0516 116.768 43.9932C118.686 43.8256 120.497 43.0481 121.93 41.7772C123.364 40.5063 124.342 38.8105 124.717 36.9441C125.071 35.0908 124.822 33.1749 124.004 31.4706C123.187 29.7664 121.843 28.3615 120.168 27.4585C118.486 26.5667 116.557 26.238 114.67 26.5218C112.784 26.8055 111.04 27.6861 109.703 29.0319C108.851 29.8282 108.178 30.7926 107.726 31.8619C107.274 32.9313 107.055 34.0823 107.081 35.2407C107.122 40.6107 107.083 45.9511 107.083 51.2941Z" fill="white" />
                                <path d="M44.9796 29.6033C44.093 28.4184 42.8821 27.5083 41.4896 26.9807C40.0972 26.453 38.5821 26.3299 37.121 26.625C35.5695 26.8186 34.1016 27.4315 32.8801 28.3965C31.6587 29.3615 30.7302 30.6407 30.1976 32.0936C29.4863 33.8626 29.3773 35.8125 29.8866 37.6483C30.3959 39.4841 31.4964 41.1063 33.0212 42.2696C34.4866 43.4003 36.275 44.0463 38.1329 44.1157C39.9909 44.1851 41.8236 43.6741 43.3713 42.6559C44.6105 41.8998 45.6168 40.8222 46.2792 39.5405C46.5727 38.9673 46.8253 38.7967 47.4577 39.1564C48.4357 39.7194 49.2551 40.516 49.8403 41.4733C50.4256 42.4306 50.7573 43.518 50.8067 44.6354C50.856 45.7527 50.6211 46.8642 50.1224 47.8682C49.6238 48.8723 48.8777 49.7365 47.9531 50.3816C45.8959 51.7435 43.56 52.6409 41.112 53.0092C38.664 53.3775 36.164 53.2076 33.7898 52.5125C29.929 51.5316 26.5129 49.2986 24.0896 46.1709C21.6663 43.0431 20.376 39.2021 20.4252 35.2634C20.408 31.3285 21.7222 27.5008 24.1584 24.3882C26.5947 21.2756 30.0135 19.0564 33.872 18.0833C38.3841 16.9077 43.1812 17.4994 47.2622 19.7351C51.3433 21.9709 54.3927 25.6776 55.774 30.0818C56.0764 30.8433 56.177 31.6689 56.065 32.4794C55.953 33.29 55.6321 34.0588 55.1342 34.7124C54.6363 35.366 53.9777 35.8827 53.2204 36.2133C52.463 36.5438 51.6321 36.6772 50.8078 36.6007C46.9133 36.6682 43.0194 36.6344 39.1226 36.6007C37.5302 36.6007 36.9433 35.9103 37.0388 34.3661C37.1755 33.086 37.7763 31.8982 38.7305 31.0219C39.6847 30.1455 40.9273 29.6401 42.2294 29.5989C43.1462 29.5831 44.0606 29.6033 44.9796 29.6033Z" fill="white" />
                                <path d="M85.2447 43.129C85.2447 40.5096 85.2447 37.8872 85.2447 35.2617C85.2464 33.2551 84.5564 31.3076 83.2887 29.7399C82.021 28.1723 80.2513 27.0783 78.2703 26.6372C76.2979 26.2146 74.2386 26.4622 72.4266 27.3401C70.6145 28.2181 69.1554 29.6746 68.2857 31.4741C67.3965 33.3019 67.1795 35.3787 67.6714 37.3477C68.1633 39.3166 69.3336 41.0547 70.9815 42.2636C72.2828 43.2888 73.8704 43.8957 75.5311 44.0032C76.2477 44.0437 76.3795 44.3137 76.3431 44.9499C76.2223 46.0104 75.8451 47.0261 75.2423 47.9116C74.6396 48.7972 73.8294 49.5261 72.8809 50.0365C71.9324 50.5469 70.8728 50.8238 69.793 50.8437C68.7131 50.8637 67.6444 50.6262 66.6773 50.1512C63.5939 48.4736 61.1902 45.7975 59.8694 42.5737C58.757 40.1709 58.2022 37.553 58.2466 34.9111C58.2911 32.2692 58.9334 29.6705 60.126 27.3056C61.3187 24.9407 63.0322 22.8691 65.14 21.2432C67.2478 19.6173 69.6965 18.4781 72.3077 17.9088C74.8397 17.3104 77.4739 17.2651 80.0253 17.7766C82.5766 18.2881 84.984 19.3439 87.0797 20.8701C89.1754 22.3963 90.9084 24.3564 92.1581 26.613C93.4078 28.8697 94.1431 31.3685 94.3141 33.9354C94.5871 39.7796 94.3901 45.6354 94.4174 51.4842C94.4783 52.3962 94.252 53.3043 93.7698 54.0843C93.2876 54.8642 92.5728 55.4777 91.7238 55.8404C90.9443 56.2222 90.067 56.3657 89.2046 56.2519C88.3422 56.1382 87.534 55.7726 86.8831 55.2021C86.3294 54.7366 85.893 54.1502 85.608 53.4894C85.3229 52.8286 85.1967 52.1112 85.2403 51.3942C85.2403 48.6414 85.2417 45.8863 85.2447 43.129Z" fill="white" />
                                <path d="M0.0119613 20.5591C0.0119613 15.2161 -0.0130618 9.87289 0.0119613 4.52989C-0.00171914 3.77626 0.17897 3.03147 0.53624 2.36551C0.89351 1.69954 1.41575 1.13428 2.05465 0.722469C2.69355 0.310659 3.42761 0.0659524 4.18842 0.0112531C4.94922 -0.0434463 5.71188 0.0935797 6.40438 0.409666C7.19093 0.735367 7.86384 1.28181 8.33932 1.98136C8.81481 2.68092 9.07262 3.50284 9.0802 4.3455C9.14162 5.56605 9.10464 6.79337 9.10464 8.02066V35.2774C9.054 37.1129 9.58375 38.9186 10.6197 40.443C11.6557 41.9674 13.1464 43.1346 14.885 43.7829C15.5188 43.9823 16.1013 44.3153 16.5923 44.7586C17.0833 45.2019 17.4706 45.7454 17.7286 46.3511C17.9865 46.9569 18.1087 47.6106 18.0862 48.2675C18.0638 48.9243 17.8974 49.5687 17.5986 50.1558C17.0881 51.1154 16.2489 51.8637 15.2305 52.267C14.2121 52.6702 13.0814 52.7021 12.0415 52.3564C8.6759 51.1817 5.73969 49.0455 3.60416 46.2194C1.46863 43.3934 0.230089 40.0047 0.0463949 36.4825C-0.0491482 31.1777 0.0264012 25.8662 0.0264012 20.5591H0.0119613Z" fill="white" />
                            </svg>
                        </div>
                        {/* <div className="intercom">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M9 14H11V16H9V14ZM10 0C4.48 0 0 4.48 0 10C0 15.52 4.48 20 10 20C15.52 20 20 15.52 20 10C20 4.48 15.52 0 10 0ZM10 18C5.59 18 2 14.41 2 10C2 5.59 5.59 2 10 2C14.41 2 18 5.59 18 10C18 14.41 14.41 18 10 18ZM10 4C7.79 4 6 5.79 6 8H8C8 6.9 8.9 6 10 6C11.1 6 12 6.9 12 8C12 10 9 9.75 9 13H11C11 10.75 14 10.5 14 8C14 5.79 12.21 4 10 4Z" fill="white"/>
                    </svg>
                </div> */}
                    </div>
                </div>
                <div className="signup-wrapper">
                    <div className="carosuel-wrapper">
                        <div className="carousel-main-wrapper">
                            {!isLastSlide && (
                                <div className="leap-logo">
                                    <svg width="134" height="73" viewBox="0 0 134 73" fill="none" xmlns="http://www.w3.org/2000/svg">
                                        <path d="M107.083 51.2941C107.083 56.8746 107.083 62.4552 107.083 68.0357C107.159 69.0638 106.888 70.0883 106.312 70.9475C105.736 71.8067 104.886 72.4523 103.898 72.7832C103.172 73.0133 102.399 73.0618 101.649 72.9248C100.899 72.7877 100.196 72.4695 99.6008 71.9979C99.0059 71.5263 98.5382 70.9163 98.2401 70.2226C97.9421 69.5289 97.8226 68.773 97.8925 68.0225C97.872 63.4617 97.8925 58.9033 97.8925 54.3448C97.8925 48.1521 97.8659 41.9591 97.9069 35.7665C97.8525 33.3095 98.285 30.8658 99.182 28.5736C100.637 25.106 103.151 22.1727 106.372 20.1817C109.593 18.1908 113.362 17.241 117.154 17.4647C121.743 17.8018 126.033 19.8443 129.159 23.1808C132.286 26.5173 134.016 30.9001 134.003 35.4471C133.987 38.1285 133.35 40.7707 132.14 43.1711C130.931 45.5716 129.182 47.667 127.027 49.296C126.101 50.1053 124.958 50.6326 123.735 50.8145C122.513 50.9964 121.265 50.8251 120.139 50.3211C119.021 49.8972 118.041 49.1794 117.306 48.2457C116.571 47.3119 116.11 46.1974 115.969 45.0227C115.86 44.3349 115.99 44.0516 116.768 43.9932C118.686 43.8256 120.497 43.0481 121.93 41.7772C123.364 40.5063 124.342 38.8105 124.717 36.9441C125.071 35.0908 124.822 33.1749 124.004 31.4706C123.187 29.7664 121.843 28.3615 120.168 27.4585C118.486 26.5667 116.557 26.238 114.67 26.5218C112.784 26.8055 111.04 27.6861 109.703 29.0319C108.851 29.8282 108.178 30.7926 107.726 31.8619C107.274 32.9313 107.055 34.0823 107.081 35.2407C107.122 40.6107 107.083 45.9511 107.083 51.2941Z" fill="white" />
                                        <path d="M44.9796 29.6033C44.093 28.4184 42.8821 27.5083 41.4896 26.9807C40.0972 26.453 38.5821 26.3299 37.121 26.625C35.5695 26.8186 34.1016 27.4315 32.8801 28.3965C31.6587 29.3615 30.7302 30.6407 30.1976 32.0936C29.4863 33.8626 29.3773 35.8125 29.8866 37.6483C30.3959 39.4841 31.4964 41.1063 33.0212 42.2696C34.4866 43.4003 36.275 44.0463 38.1329 44.1157C39.9909 44.1851 41.8236 43.6741 43.3713 42.6559C44.6105 41.8998 45.6168 40.8222 46.2792 39.5405C46.5727 38.9673 46.8253 38.7967 47.4577 39.1564C48.4357 39.7194 49.2551 40.516 49.8403 41.4733C50.4256 42.4306 50.7573 43.518 50.8067 44.6354C50.856 45.7527 50.6211 46.8642 50.1224 47.8682C49.6238 48.8723 48.8777 49.7365 47.9531 50.3816C45.8959 51.7435 43.56 52.6409 41.112 53.0092C38.664 53.3775 36.164 53.2076 33.7898 52.5125C29.929 51.5316 26.5129 49.2986 24.0896 46.1709C21.6663 43.0431 20.376 39.2021 20.4252 35.2634C20.408 31.3285 21.7222 27.5008 24.1584 24.3882C26.5947 21.2756 30.0135 19.0564 33.872 18.0833C38.3841 16.9077 43.1812 17.4994 47.2622 19.7351C51.3433 21.9709 54.3927 25.6776 55.774 30.0818C56.0764 30.8433 56.177 31.6689 56.065 32.4794C55.953 33.29 55.6321 34.0588 55.1342 34.7124C54.6363 35.366 53.9777 35.8827 53.2204 36.2133C52.463 36.5438 51.6321 36.6772 50.8078 36.6007C46.9133 36.6682 43.0194 36.6344 39.1226 36.6007C37.5302 36.6007 36.9433 35.9103 37.0388 34.3661C37.1755 33.086 37.7763 31.8982 38.7305 31.0219C39.6847 30.1455 40.9273 29.6401 42.2294 29.5989C43.1462 29.5831 44.0606 29.6033 44.9796 29.6033Z" fill="white" />
                                        <path d="M85.2447 43.129C85.2447 40.5096 85.2447 37.8872 85.2447 35.2617C85.2464 33.2551 84.5564 31.3076 83.2887 29.7399C82.021 28.1723 80.2513 27.0783 78.2703 26.6372C76.2979 26.2146 74.2386 26.4622 72.4266 27.3401C70.6145 28.2181 69.1554 29.6746 68.2857 31.4741C67.3965 33.3019 67.1795 35.3787 67.6714 37.3477C68.1633 39.3166 69.3336 41.0547 70.9815 42.2636C72.2828 43.2888 73.8704 43.8957 75.5311 44.0032C76.2477 44.0437 76.3795 44.3137 76.3431 44.9499C76.2223 46.0104 75.8451 47.0261 75.2423 47.9116C74.6396 48.7972 73.8294 49.5261 72.8809 50.0365C71.9324 50.5469 70.8728 50.8238 69.793 50.8437C68.7131 50.8637 67.6444 50.6262 66.6773 50.1512C63.5939 48.4736 61.1902 45.7975 59.8694 42.5737C58.757 40.1709 58.2022 37.553 58.2466 34.9111C58.2911 32.2692 58.9334 29.6705 60.126 27.3056C61.3187 24.9407 63.0322 22.8691 65.14 21.2432C67.2478 19.6173 69.6965 18.4781 72.3077 17.9088C74.8397 17.3104 77.4739 17.2651 80.0253 17.7766C82.5766 18.2881 84.984 19.3439 87.0797 20.8701C89.1754 22.3963 90.9084 24.3564 92.1581 26.613C93.4078 28.8697 94.1431 31.3685 94.3141 33.9354C94.5871 39.7796 94.3901 45.6354 94.4174 51.4842C94.4783 52.3962 94.252 53.3043 93.7698 54.0843C93.2876 54.8642 92.5728 55.4777 91.7238 55.8404C90.9443 56.2222 90.067 56.3657 89.2046 56.2519C88.3422 56.1382 87.534 55.7726 86.8831 55.2021C86.3294 54.7366 85.893 54.1502 85.608 53.4894C85.3229 52.8286 85.1967 52.1112 85.2403 51.3942C85.2403 48.6414 85.2417 45.8863 85.2447 43.129Z" fill="white" />
                                        <path d="M0.0119613 20.5591C0.0119613 15.2161 -0.0130618 9.87289 0.0119613 4.52989C-0.00171914 3.77626 0.17897 3.03147 0.53624 2.36551C0.89351 1.69954 1.41575 1.13428 2.05465 0.722469C2.69355 0.310659 3.42761 0.0659524 4.18842 0.0112531C4.94922 -0.0434463 5.71188 0.0935797 6.40438 0.409666C7.19093 0.735367 7.86384 1.28181 8.33932 1.98136C8.81481 2.68092 9.07262 3.50284 9.0802 4.3455C9.14162 5.56605 9.10464 6.79337 9.10464 8.02066V35.2774C9.054 37.1129 9.58375 38.9186 10.6197 40.443C11.6557 41.9674 13.1464 43.1346 14.885 43.7829C15.5188 43.9823 16.1013 44.3153 16.5923 44.7586C17.0833 45.2019 17.4706 45.7454 17.7286 46.3511C17.9865 46.9569 18.1087 47.6106 18.0862 48.2675C18.0638 48.9243 17.8974 49.5687 17.5986 50.1558C17.0881 51.1154 16.2489 51.8637 15.2305 52.267C14.2121 52.6702 13.0814 52.7021 12.0415 52.3564C8.6759 51.1817 5.73969 49.0455 3.60416 46.2194C1.46863 43.3934 0.230089 40.0047 0.0463949 36.4825C-0.0491482 31.1777 0.0264012 25.8662 0.0264012 20.5591H0.0119613Z" fill="white" />
                                    </svg>
                                </div>
                            )}
                            <div className="carousel-container">

                                {/* Images */}
                                {images.map((image, index) => (
                                    <div
                                        key={image.id}
                                        className={`slide-wrapper ${index === currentIndex ? 'slide-active' : 'slide-inactive'}`}
                                    >
                                        {image.type === 'app-download' ? (
                                            // App Download Slide
                                            <div className="app-download-slide">
                                                <div className="mobile-phones-container">

                                                </div>
                                                <h2 className="app-download-title">{image.title}</h2>
                                                <p className="app-download-subtitle">{image.subtitle}</p>
                                                <div className="qr-code">
                                                    <div className="qr-pattern"></div>
                                                </div>
                                                <div className="app-store-buttons">
                                                    <div className='play-google'>
                                                        <a href="https://play.google.com/store/apps/details?id=com.curemd.leap" target="_blank" rel="noopener noreferrer">
                                                            <img src={playStore} alt="Download on Play Store" />
                                                        </a>
                                                    </div>
                                                    <div className='app-store'>
                                                        <a href="https://apps.apple.com/us/app/leap-your-health-companion/id1659618898" target="_blank" rel="noopener noreferrer">
                                                            <img src={appStore} alt="Download on Play Store" />
                                                        </a>
                                                    </div>

                                                </div>
                                            </div>
                                        ) : (
                                            // Regular Image Slide
                                            <>
                                                <div className='images-wrapper'>
                                                    <img
                                                        src={image.src}
                                                        alt={image.alt}
                                                        className="slide-image"
                                                    />
                                                </div>
                                                <div className="gradient-overlay" />
                                                <div className="text-content-overlay">
                                                    <h2 className="slide-main-title">
                                                        {image.title}
                                                    </h2>
                                                    <p className="slide-description">
                                                        {image.subtitle}
                                                    </p>
                                                </div>
                                            </>
                                        )}
                                    </div>
                                ))}
                                {/* Navigation Dots */}
                                {/* <button
                                    onClick={toggleAutoPlay}
                                    className="navigation-button pause-button"
                                >
                                    {!isManuallyPaused ? (
                                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                            <rect x="3" y="2" width="3" height="12" fill="white" />
                                            <rect x="10" y="2" width="3" height="12" fill="white" />
                                        </svg>
                                    ) : (
                                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                            <path d="M3 2L13 8L3 14V2Z" fill="white" />
                                        </svg>
                                    )}
                                </button> */}

                                <div className="navigation-dots-container">
                                    {images.map((_, index) => (
                                        <button
                                            key={index}
                                            onClick={() => {
                                                setCurrentIndex(index);
                                                setIsAutoPlaying(false);
                                            }}
                                            className={`navigation-dot ${index === currentIndex ? 'dot-active' : 'dot-inactive'}`}
                                        />
                                    ))}
                                </div>

                                <div className='arrow-button'>
                                    {/* Previous Button - Hidden on last slide */}
                                    <button
                                        onClick={goToPrevious}
                                        className={`navigation-button prev-navigation-button ${isLastSlide ? 'hidden' : ''}`}
                                    >
                                        <svg width="16" height="14" viewBox="0 0 16 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                                            <path d="M7.15898 13.8666C7.36292 14.0528 7.67918 14.0384 7.86536 13.8345C8.05154 13.6305 8.03714 13.3143 7.8332 13.1281L1.66535 7.49736H15.4961C15.7722 7.49736 15.9961 7.2735 15.9961 6.99736C15.9961 6.72122 15.7722 6.49736 15.4961 6.49736H1.66824L7.8332 0.86927C8.03714 0.68309 8.05154 0.366835 7.86536 0.162895C7.67918 -0.0410457 7.36292 -0.0554433 7.15898 0.130737L0.242631 6.44478C0.102683 6.57254 0.0228539 6.74008 0.00314236 6.91323C-0.0014925 6.94058 -0.00390625 6.96869 -0.00390625 6.99736C-0.00390625 7.02423 -0.00178671 7.05061 0.00229454 7.07633C0.0204692 7.25224 0.100582 7.4229 0.242631 7.55258L7.15898 13.8666Z" fill="white" />
                                        </svg>

                                    </button>

                                    {/* Next Button - Hidden on last slide */}
                                    <button
                                        onClick={goToNext}
                                        className={`navigation-button next-navigation-button ${isLastSlide ? 'hidden' : ''}`}
                                    >
                                        <svg width="16" height="14" viewBox="0 0 16 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                                            <path d="M8.83711 0.130737C8.63317 -0.0554432 8.31692 -0.0410458 8.13074 0.162894C7.94456 0.366834 7.95895 0.683089 8.16289 0.869269L14.3307 6.5H0.5C0.223858 6.5 0 6.72386 0 7C0 7.27614 0.223858 7.5 0.5 7.5H14.3279L8.16289 13.1281C7.95895 13.3143 7.94456 13.6305 8.13074 13.8345C8.31692 14.0384 8.63317 14.0528 8.83711 13.8666L15.7535 7.55258C15.8934 7.42482 15.9732 7.25728 15.993 7.08414C15.9976 7.05678 16 7.02867 16 7C16 6.97313 15.9979 6.94675 15.9938 6.92103C15.9756 6.74512 15.8955 6.57446 15.7535 6.44478L8.83711 0.130737Z" fill="white" />
                                        </svg>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="form-wrapper">
                        {/* <div className="leap-header">
                    <div className="leap-img">
                        <img src="data:image/png;base64,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" />
                    </div>                    
                    <div className="sub">Your Health Companion</div>
                </div> */}

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
                        <div className="form-basic-render">
                            <div id="kc-form-main" className="form-basic-render-main">
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
                        {/* <div className="auth-chat">
                    <span className="icon-help">
                        <svg width="29" height="29" viewBox="0 0 29 29" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M13.3477 20.296H16.1255V23.0738H13.3477V20.296ZM14.7366 0.851562C7.06989 0.851562 0.847656 7.0738 0.847656 14.7405C0.847656 22.4072 7.06989 28.6294 14.7366 28.6294C22.4033 28.6294 28.6255 22.4072 28.6255 14.7405C28.6255 7.0738 22.4033 0.851562 14.7366 0.851562ZM14.7366 25.8516C8.61156 25.8516 3.62544 20.8655 3.62544 14.7405C3.62544 8.61547 8.61156 3.62935 14.7366 3.62935C20.8616 3.62935 25.8477 8.61547 25.8477 14.7405C25.8477 20.8655 20.8616 25.8516 14.7366 25.8516ZM14.7366 6.40713C11.6671 6.40713 9.18101 8.89325 9.18101 11.9627H11.9588C11.9588 10.4349 13.2088 9.18491 14.7366 9.18491C16.2644 9.18491 17.5144 10.4349 17.5144 11.9627C17.5144 14.7405 13.3477 14.3933 13.3477 18.9072H16.1255C16.1255 15.7822 20.2921 15.4349 20.2921 11.9627C20.2921 8.89325 17.806 6.40713 14.7366 6.40713Z" fill="white"/>
                        </svg>
                    </span>
                </div> */}
                        <div className="auth-footer">
                            <a href="https://leaphealth.ai/privacy-policy/" target="_blank">Privacy Policy</a>
                            <a href="https://leaphealth.ai/terms-of-use/" target="_blank">Terms of Service</a>
                            <span>&#169; 2025 Leap Health</span>
                        </div>
                    </div>
                </div>
            </div>

        </>
    );
}
