import {useEffect, useState} from "react";
import SettingService from "@/services/SettingService";
import CommonNotice from "@/common/CommonNotice";

const JarbootVersion = () => {
    let [version, setVersion] = useState();
    useEffect(() => {
        SettingService.getVersion().then(resp => {
            if (resp.resultCode !== 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            setVersion(resp.result);
        }).catch(CommonNotice.errorFormatted);
    }, []);
    return <span style={{position: "relative", top: '-5px'}}>{version}</span>;
};
export default JarbootVersion;
