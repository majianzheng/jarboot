import React, {memo} from "react";
import {BackTop, Typography} from "antd";

const {Text} = Typography;

const GoGithubDoc: any = memo(() => {
    return <>
        <Typography>
            <Text>帮助文档编写中，前往Github寻找更多帮助信息</Text>
        </Typography>
        <BackTop/>
    </>;
});

export default GoGithubDoc
