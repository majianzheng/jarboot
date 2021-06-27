import {memo} from "react";
import {Result, Button} from "antd";
import {DownloadOutlined} from '@ant-design/icons';

const HeapDumpView = memo((props: any) => {
    return <>
        <Result
            status="success"
            title="Heap dump success!"
            subTitle={props?.data?.dumpFile}
            extra={[
                <Button icon={<DownloadOutlined/>}
                        type="primary"
                        key="Download"
                        href={`/jarboot-service/downloadFile/${props?.data?.encodedName}`}>
                    Download
                </Button>
            ]}/>
    </>
});

export default HeapDumpView;
