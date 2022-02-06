import React, {useEffect, useState} from "react";
import CommonNotice from "@/common/CommonNotice";
import CloudService from "@/services/CloudService";
import StringUtil from "@/common/StringUtil";

const JarbootVersion = () => {
    let [version, setVersion] = useState();
    let [inDocker, setInDocker] = useState(false);
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
        //检查是否在docker中
        CloudService.checkInDocker().then(resp => {
            if (StringUtil.isString(resp)) {
                setInDocker(StringUtil.toBoolean(resp));
                return;
            }
            if (true === resp || false === resp) {
                setInDocker(resp);
                return;
            }
            if (resp.resultCode !== 0) {
                CommonNotice.errorFormatted(resp);
            }
        }).catch(CommonNotice.errorFormatted);
    }, []);
    return <span style={{position: "relative", top: '-5px'}}>v{version}{inDocker && "(Docker)"}</span>;
};
export default JarbootVersion;
