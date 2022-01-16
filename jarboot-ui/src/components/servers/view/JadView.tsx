import React, {memo} from "react";
import {CommonConst} from "@/common/CommonConst";
import CodeEditor from "@/components/code";

const JadView = memo((props: any) => {

    return <>
        <CodeEditor height={CommonConst.PANEL_HEIGHT - 25}
                    readOnly={true}
                    source={props?.data?.source}/>
    </>
});

export default JadView;
