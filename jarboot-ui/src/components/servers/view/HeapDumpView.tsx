import React, {memo} from "react";
import {Result, Button} from "antd";
import {DownloadOutlined} from '@ant-design/icons';
import CommonUtils from "@/common/CommonUtils";
import StringUtil from "@/common/StringUtil";

const HeapDumpView = memo((props: any) => {
    const token = CommonUtils.getRawToken();
    const {data, remote} = props;
    let subTitle = data?.dumpFile;
    const isRemote = (StringUtil.isNotEmpty(remote) && 'localhost' !== remote && '127.0.0.1' !== remote);
    if (isRemote) {
        subTitle = `Dump file is stored in remote server ${remote}, can't download directly.`;
    }
    return <>
        <Result
            status="success"
            title="Heap dump success!"
            subTitle={subTitle}
            extra={[
                <Button icon={<DownloadOutlined/>}
                        type="primary"
                        disabled={isRemote}
                        key="Download"
                        href={`/api/jarboot/cloud/download/${data?.encodedName}?token=${token}`}>
                    Download
                </Button>
            ]}/>
    </>;
});

export default HeapDumpView;
