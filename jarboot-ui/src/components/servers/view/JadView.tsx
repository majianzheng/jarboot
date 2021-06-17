import {memo} from "react";
import {JarBootConst} from "@/common/JarBootConst";
import {CodeEditor} from "@/components";

const JadView = memo((props: any) => {

    return <>
        <CodeEditor height={JarBootConst.PANEL_HEIGHT}
                    readOnly={true}
                    source={props?.data?.source}/>
    </>
});

export default JadView;
