import React, {useEffect, useState} from "react";
import CommonNotice from "@/common/CommonNotice";
import CloudService from "@/services/CloudService";
import StringUtil from "@/common/StringUtil";

const JarbootVersion = () => {
    let [version, setVersion] = useState();
    useEffect(() => {
        CloudService.getVersion().then(resp => {
            if (StringUtil.isString(resp)) {
                setVersion(resp);
                return;
            }
            if (resp.resultCode !== 0) {
                CommonNotice.errorFormatted(resp);
            }
        }).catch(CommonNotice.errorFormatted);
    }, []);
    return <span style={{position: "relative", top: '-5px'}}>{version}</span>;
};
export default JarbootVersion;
