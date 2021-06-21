import {memo} from "react";

const ClassLoaderView = memo((props: any) => {

    return <>
        {JSON.stringify(props?.data)}
    </>
});

export default ClassLoaderView;
