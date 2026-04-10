export function setFlowIndexOnce(isEntryPoint: boolean) {
    try {
        if (!isEntryPoint) {
            if (sessionStorage.getItem("pageIndex") != null) {
                const lastIndex = Number(sessionStorage.getItem("pageIndex"));
                if (lastIndex != null && lastIndex != undefined) {
                    const nextIndex = lastIndex + 1;
                    sessionStorage.setItem("pageIndex", nextIndex.toString());
                }
            }
            else {
                if (sessionStorage.getItem("kc_flow") != "register")
                    sessionStorage.setItem("pageIndex", "0");
                else
                    sessionStorage.setItem("pageIndex", "1");
            }
        }
        else {
            if (sessionStorage.getItem("kc_flow") != "register")
                sessionStorage.setItem("pageIndex", "0");
            else
                sessionStorage.setItem("pageIndex", "1");
        }
    } catch {
        //do nothing
    }
}

export function getFlowIndex() {
    if(!sessionStorage.getItem("pageIndex"))
        return;
    const flowIndex = Number(sessionStorage.getItem("pageIndex"));
    return flowIndex;
}