import React, {memo, useEffect, useReducer, useRef} from "react";
import { Modal, Upload, Form, Input, Result, Alert } from 'antd';
import {InboxOutlined, LoadingOutlined, BulbOutlined} from '@ant-design/icons';
import CommonNotice from "@/common/CommonNotice";
import StringUtil from "@/common/StringUtil";
import {useIntl} from "umi";
import UploadFileService from "@/services/UploadFileService";
import UploadHeartbeat from "@/components/servers/UploadHeartbeat";
import CommonUtils from "@/common/CommonUtils";

interface UploadFileModalProp {
    server?: string;
    onOk: () => void;
    onCancel: () => void;
}
enum UploadFileStage {
    SERVER_CONFIRM,
    UPLOAD,
    SUBMITTING,
    SUBMIT,
    FAILED
}

interface UploadFileModalState {
    stage: UploadFileStage;
    name: string;
    fileList: any[];
    exist: boolean;
}

const UploadFileModal = memo((props: UploadFileModalProp) => {
    const inputRef = useRef<any>();
    //阶段，1：确定服务的名称；2：开始选择并上传文件；3：提交或清理
    const initArg: UploadFileModalState = {
        stage: UploadFileStage.SERVER_CONFIRM,
        name: '',
        fileList: [] as any[],
        exist: true
    };
    const [state, dispatch] = useReducer((state: UploadFileModalState, action: any) => {
        if ('function' === typeof action) {
            return {...state, ...action(state)} as UploadFileModalState;
        }
        return {...state, ...action} as UploadFileModalState;
    }, initArg, arg => ({...arg} as UploadFileModalState));
    const [form] = Form.useForm();
    const intl = useIntl();

    useEffect(() => {
        inputRef?.current?.focus();
        const value = inputRef?.current?.state?.value;
        if (value && value?.length > 0) {
            inputRef.current.setSelectionRange(0, value.length);
        }
    }, []);

    const onOk = () => {
        switch (state.stage) {
            case UploadFileStage.SERVER_CONFIRM:
                onConfirm();
                break;
            case UploadFileStage.UPLOAD:
                onUpload();
                break;
            case UploadFileStage.SUBMIT:
            case UploadFileStage.FAILED:
                props.onOk && props.onOk();
                break;
            default:
                break;
        }
    };
    const onConfirm = () => {
        const name = form.getFieldValue("name");
        if (StringUtil.isEmpty(name)) {
            CommonNotice.info(intl.formatMessage({id: 'SELECT_UPLOAD_SERVER_TITLE'}));
            return;
        }
        UploadFileService.startUploadFile(name).then(resp => {
            if (resp.resultCode === 0) {
                dispatch({name: name, stage: UploadFileStage.UPLOAD, exist: resp.result});
                UploadHeartbeat.getInstance().start(name);
            } else {
                CommonNotice.errorFormatted(resp);
            }
        }).catch(CommonNotice.errorFormatted)
    };

    const onUpload = () => {
        const list = state.fileList.filter((value: any) => 'done' === value.status);
        if (list.length <= 0 && state.exist) {
            CommonNotice.info(intl.formatMessage({id: 'UPLOAD_FILE_EMPTY'}));
            return;
        }
        UploadHeartbeat.getInstance().stop();
        dispatch({stage: UploadFileStage.SUBMITTING});
        UploadFileService.submitUploadFile(form.getFieldsValue())
            .then(resp => {
                if (resp.resultCode === 0) {
                    dispatch({stage: UploadFileStage.SUBMIT});
                } else {
                    dispatch({stage: UploadFileStage.FAILED});
                    CommonNotice.errorFormatted(resp);
                }
            }).catch(error => {
                dispatch({stage: UploadFileStage.FAILED});
                CommonNotice.errorFormatted(error);
            });
    };

    const onCancel = () => {
        UploadHeartbeat.getInstance().stop();
        if (StringUtil.isNotEmpty(state.name)) {
            UploadFileService.clearUploadFileInCache(state.name);
        }
        props.onCancel && props.onCancel();
    };

    const checkFile = (file: any, show: boolean = true) => {
        const isLt = file.size / 1024 / 1024 < 500;
        if (!isLt) {
            show && CommonNotice.error(intl.formatMessage({id: 'FILE_SIZE_OVER_TIPS'}, {size: '500MB'}));
            return false;
        }
        const notUploaded = -1 === state.fileList.findIndex((value: any) => value.name === file.name);
        if (!notUploaded) {
            show && CommonNotice.error(intl.formatMessage({id: 'SUCCESS'}));
            return false;
        }
        return true;
    };

    const uploadProps = {
        name: 'file',
        multiple: true,
        action: `/api/jarboot/upload`,
        headers: {Authorization: CommonUtils.getToken()},
        fileList: state.fileList,
        data: () => ({server: form.getFieldValue("name")}),
        beforeUpload(file: any) {
            return checkFile(file) ? Promise.resolve(file) : Promise.reject();
        },
        onChange(info: any) {
            const { status } = info.file;
            if (status !== 'uploading') {
                console.log(info.file, info.fileList);
            }
            if ('done' === status || 'removed' === status) {
                dispatch({fileList: info.fileList});
            } else if (status === 'error') {
                CommonNotice.error(`${info.file.name} file upload failed.`);
            } else {
                if (checkFile(info.file, false)) {
                    dispatch({fileList: info.fileList});
                }
            }
        },
        onRemove(file: any) {
            UploadFileService.deleteCacheFile(form.getFieldValue("name")
                , file.name)
                .then(resp => {
                    if (resp.resultCode !== 0) {
                        CommonNotice.errorFormatted(resp);
                    }
                }).catch(CommonNotice.errorFormatted);
        },
    };
    const layout = {labelCol: { span: 4 }, wrapperCol: { span: 20 }};

    const getStageTitle = () => {
        let title = '';
        switch (state.stage) {
            case UploadFileStage.SERVER_CONFIRM:
                title = intl.formatMessage({id: 'SELECT_UPLOAD_SERVER_TITLE'});
                break;
            case UploadFileStage.UPLOAD:
            case UploadFileStage.SUBMITTING:
            case UploadFileStage.SUBMIT:
            case UploadFileStage.FAILED:
                title = intl.formatMessage({id: 'UPLOAD_STAGE_TITLE'}, {server: state.name});
                break;
            default:
                break;
        }
        return title;
    };

    let okText = "";
    switch (state.stage) {
        case UploadFileStage.SERVER_CONFIRM:
            okText = intl.formatMessage({id: 'NEXT_BTN'});
            break;
        case UploadFileStage.SUBMIT:
        case UploadFileStage.FAILED:
            okText = intl.formatMessage({id: 'CLOSE'});
            break;
        default:
            okText = intl.formatMessage({id: 'SUBMIT_BTN'});
            break;
    }
    const hidden = UploadFileStage.UPLOAD !== state.stage || state.exist;

    return <Modal title={getStageTitle()}
                  visible={true} maskClosable={false}
                  destroyOnClose={true} width={800}
                  okText={okText}
                  onOk={onOk} onCancel={onCancel}>
        <Form {...layout}
              form={form}
              initialValues={{name: props.server}}>
            <Form.Item label={intl.formatMessage({id: 'NAME'})} name={"name"}>
                <Input autoComplete="off"
                       ref={inputRef}
                       autoCorrect="off"
                       autoCapitalize="off"
                       spellCheck="false"
                       readOnly={UploadFileStage.SERVER_CONFIRM !== state.stage}
                       placeholder="server name" />
            </Form.Item>
            <Form.Item name="command"
                       hidden={hidden}
                       label={intl.formatMessage({id: 'COMMAND_LABEL'})}
                       rules={[{required: false}]}>
                <Input.TextArea rows={2}
                                placeholder={intl.formatMessage({id: 'COMMAND_EXAMPLE'})}
                                autoComplete="off"/>
            </Form.Item>
            <Form.Item name="args"
                       label={intl.formatMessage({id: 'MAIN_ARGS_LABEL'})}
                       hidden={hidden}
                       rules={[{required: false}]}>
                <Input autoComplete="off"
                       placeholder={"Main arguments"}
                       autoCorrect="off"
                       autoCapitalize="off"
                       spellCheck="false"/>
            </Form.Item>
            {!hidden && <Alert icon={<BulbOutlined/>}
                               showIcon
                               message={intl.formatMessage({id: 'MORE_SETTING_INFO'})} type="warning"/>}
        </Form>
        {UploadFileStage.UPLOAD === state.stage && <Upload.Dragger {...uploadProps}>
            <p className="ant-upload-drag-icon"><InboxOutlined/></p>
            <p className="ant-upload-text">{intl.formatMessage({id: 'UPLOAD_DESC'})}</p>
            <p className="ant-upload-hint">{intl.formatMessage({id: 'UPLOAD_HINT'})}</p>
        </Upload.Dragger>}
        {UploadFileStage.SUBMITTING === state.stage && <div>
            <Result icon={<LoadingOutlined />} title={intl.formatMessage({id: 'SUBMITTING'})}/>
        </div>}
        {UploadFileStage.SUBMIT === state.stage && <div>
            <Result status="success" title={intl.formatMessage({id: 'UPLOAD_SUCCESS'})}/>
        </div>}
        {UploadFileStage.FAILED === state.stage && <div>
            <Result status="error" title={intl.formatMessage({id: 'UPLOAD_ERROR'})}/>
        </div>}
    </Modal>
});

export default UploadFileModal;
