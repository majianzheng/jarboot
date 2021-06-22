import {memo, useState} from "react";
import { Modal, Upload, Form, Radio, Input } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import CommonNotice from "@/common/CommonNotice";
import StringUtil from "@/common/StringUtil";
import {useIntl} from "umi";

interface UploadFileModal {
    server?: string;
    onClose: () => void;
}
enum UploadFileStage {
    SERVER_CONFIRM,
    UPLOAD,
    SUBMIT,
    CLEAR
}
const UploadFileModal = memo((props: UploadFileModal) => {
    //阶段，1：确定服务的名称；2：开始选择并上传文件；3：提交或清理
    const [stage, setStage] = useState(UploadFileStage.SERVER_CONFIRM);
    const [name, setName] = useState('');
    const [fileList, setFileList] = useState(new Array<any>());
    const [form] = Form.useForm();
    const intl = useIntl();
    const onOk = () => {
        switch (stage) {
            case UploadFileStage.SERVER_CONFIRM:
                onConfirm();
                break;
            case UploadFileStage.UPLOAD:
                onUpload();
                break;
            case UploadFileStage.SUBMIT:
                //提交后天更新
                break;
            case UploadFileStage.CLEAR:
                //清理临时目录
                break;
            default:
                break;
        }
        //props.onClose && props.onClose();
    };
    const onConfirm = () => {
        const server = form.getFieldValue("server");
        if (StringUtil.isEmpty(server)) {
            CommonNotice.info(intl.formatMessage({id: 'SELECT_UPLOAD_SERVER_TITLE'}));
            return;
        }
        setName(server);
        setStage(UploadFileStage.UPLOAD);
    };
    const onUpload = () => {
        const list = fileList.filter(value => 'done' === value.status);
        if (list.length <= 0) {
            CommonNotice.info(intl.formatMessage({id: 'UPLOAD_FILE_EMPTY'}));
            return;
        }
        setStage(UploadFileStage.SUBMIT);
    };

    const onCancel = () => {
        props.onClose && props.onClose();
    };

    const uploadProps = {
        name: 'file',
        multiple: true,
        action: `/jarboot-service/upload`,
        fileList: fileList,
        data: () => {
            return {server: props.server}
        },
        beforeUpload(file: any) {
            const isJarOrZip = file.type === "application/java-archive" || file.type === "application/zip";
            console.log("upload file type:", file.type);
            const isLt60M = file.size / 1024 / 1024 < 60;
            if (!isLt60M) {
                CommonNotice.error('文件大小必须小于60MB！');
            }
            const notUploaded = -1 === fileList.findIndex((value: any) => value.uid === file.file.uid);

            if (notUploaded && isJarOrZip && isLt60M) {
                setFileList((prevState => prevState.concat([file])));
                return true;
            }
            return false;
        },
        onChange(info: any) {
            const { status } = info.file;
            if (status !== 'uploading') {
                console.log(info.file, info.fileList);
            }
            if (status === 'done') {
                CommonNotice.info(`${info.file.name} file uploaded successfully.`);
            } else if (status === 'error') {
                CommonNotice.error(`${info.file.name} file upload failed.`);
            }
            setFileList(info.fileList);
        },
        onRemove(file: any) {
            console.log("remove file.", file);
        },
    };
    const layout = {
        labelCol: { span: 4 },
        wrapperCol: { span: 20 },
    };

    const getStageTitle = () => {
        let title = '';
        switch (stage) {
            case UploadFileStage.SERVER_CONFIRM:
                title = intl.formatMessage({id: 'SELECT_UPLOAD_SERVER_TITLE'});
                break;
            case UploadFileStage.UPLOAD:
            case UploadFileStage.SUBMIT:
            case UploadFileStage.CLEAR:
                title = intl.formatMessage({id: 'UPLOAD_STAGE_TITLE'}, {server: name});
                break;
            default:
                break;
        }
        return title;
    };

    return <Modal title={getStageTitle()}
                  visible={true}
                  destroyOnClose={true}
                  onOk={onOk} onCancel={onCancel}>
        {UploadFileStage.SERVER_CONFIRM === stage && <Form
            {...layout}
            form={form}
            initialValues={{server: props.server}}
        >
            <Form.Item label={intl.formatMessage({id: 'NAME'})} name={"server"}>
                <Input autoComplete="off"
                       autoCorrect="off"
                       autoCapitalize="off"
                       spellCheck="false"
                       placeholder="input update or new server name" />
            </Form.Item>
        </Form>}
        {UploadFileStage.UPLOAD === stage && <Upload.Dragger {...uploadProps}>
            <p className="ant-upload-drag-icon">
                <InboxOutlined/>
            </p>
            <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
            <p className="ant-upload-hint">
                Support for a single or bulk upload. Strictly prohibit from uploading company data or other
                band files
            </p>
        </Upload.Dragger>}
        {UploadFileStage.SUBMIT === stage && <div>
            显示成功上传即将更新的文件列表，功能开发中...
        </div>}
        {UploadFileStage.CLEAR === stage && <div>

        </div>}
    </Modal>
});

export default UploadFileModal;
