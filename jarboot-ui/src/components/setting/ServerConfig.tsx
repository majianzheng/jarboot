import {Form, Input, Button, InputNumber, Switch} from 'antd';
import {memo, useEffect, useState} from "react";
import SettingService from "@/services/SettingService";
import CommonNotice from "@/common/CommonNotice";
import { useIntl } from 'umi';
import StringUtil from "@/common/StringUtil";
import {FormOutlined} from "@ant-design/icons";
import FileEditModal from "@/components/FileEditModal";

const layout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
};
const tailLayout = {
    wrapperCol: {offset: 12, span: 12},
};

const ServerConfig: any = memo((props: any) => {
    const [form] = Form.useForm();
    const intl = useIntl();
    let [visible, setVisible] = useState(false);
    let [file, setFile] = useState({name: "", content: '', onSave: (value: string) => {}});
    const onReset = () => {
        SettingService.getServerSetting(props.server
        ).then((resp: any) => {
            if (0 !== resp.resultCode) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            form.setFieldsValue(resp.result);
        }).catch(CommonNotice.errorFormatted);
    };

    useEffect(() => {
        onReset();
    });

    const onSubmit = (data: any) => {
        if (StringUtil.isEmpty(props.server)) {
            CommonNotice.info('请先选择服务');
            return;
        }
        SettingService.submitServerSetting(props.server, data).then(resp => {
            if (0 === resp?.resultCode) {
                CommonNotice.info(intl.formatMessage({id: 'SUCCESS'}));
            } else {
                CommonNotice.errorFormatted(resp);
            }
        }).catch(CommonNotice.errorFormatted);
    };
    const onJvmEdit = () => {
        setVisible(true);
        let jvm = form.getFieldValue("jvm");
        if (StringUtil.isEmpty(jvm)) {
            jvm = 'boot.vmoptions';
        }
        const onSave = (value: string) => {
            SettingService.saveVmOptions(props.server, jvm, value).then(resp => {
                if (resp.resultCode !== 0) {
                    CommonNotice.errorFormatted(resp);
                    return;
                }
            }).catch(CommonNotice.errorFormatted)
        };
        SettingService.getVmOptions(props.server, jvm).then(resp => {
            if (resp.resultCode !== 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            setFile({name: jvm, content: resp.result, onSave});
        }).catch(CommonNotice.errorFormatted)
    };

    const onArgsEdit = () => {
        setVisible(true);
        const args = form.getFieldValue('args');
        const setFieldsValue = form.setFieldsValue;
        setFile({name: 'start args', content: args, onSave: onArgsSave});
    };
    const onArgsSave = (args: string) => {
        args = args.replaceAll('\n', ' ');
        setTimeout(() => {
            form.setFieldsValue({args});
        }, 100);
    };

    return (<>
        <Form {...layout} form={form} name="control-hooks" onFinish={onSubmit}>
            <Form.Item name="jar"
                       label={intl.formatMessage({id: 'JAR_LABEL'})}
                       rules={[{required: false}]}>
                <Input placeholder={"The jar file to start"} autoComplete="off"/>
            </Form.Item>
            <Form.Item name="jvm"
                       label={intl.formatMessage({id: 'JVM_OPT_LABEL'})}
                       rules={[{required: false}]}>
                <Input autoComplete="off" placeholder={"vm options file"} onDoubleClick={onJvmEdit} addonAfter={<FormOutlined onClick={onJvmEdit}/>}/>
            </Form.Item>
            <Form.Item name="args"
                       label={intl.formatMessage({id: 'MAIN_ARGS_LABEL'})}
                       rules={[{required: false}]}>
                <Input autoComplete="off" onDoubleClick={onArgsEdit} addonAfter={<FormOutlined onClick={onArgsEdit}/>}/>
            </Form.Item>
            <Form.Item name="javaHome"
                       label={"Java Home"}
                       rules={[{required: false}]}>
                <Input autoComplete="off"/>
            </Form.Item>
            <Form.Item name="workHome"
                       label={intl.formatMessage({id: 'WORK_HOME_LABEL'})}
                       rules={[{required: false}]}>
                <Input autoComplete="off"/>
            </Form.Item>
            <Form.Item name="envp"
                       label={intl.formatMessage({id: 'ENV_LABEL'})}
                       rules={[{required: false}]}>
                <Input placeholder={"env1=val1,env2=val2"} autoComplete="off"/>
            </Form.Item>
            <Form.Item name="priority"
                       label={intl.formatMessage({id: 'PRIORITY_LABEL'})}
                       rules={[{required: false}]}>
                <InputNumber min={1} max={9999} defaultValue={1} autoComplete="off"/>
            </Form.Item>
            <Form.Item name="daemon"
                       label={intl.formatMessage({id: 'DAEMON_LABEL'})}
                       rules={[{required: false}]} valuePropName={"checked"}>
                <Switch defaultChecked/>
            </Form.Item>
            <Form.Item name="jarUpdateWatch"
                       label={intl.formatMessage({id: 'JAR_UPDATE_WATCH_LABEL'})}
                       rules={[{required: false}]} valuePropName={"checked"}>
                <Switch defaultChecked/>
            </Form.Item>
            <Form.Item {...tailLayout}>
                <Button type="primary" htmlType="submit" style={{marginRight: 8}}>
                    {intl.formatMessage({id: 'SUBMIT_BTN'})}
                </Button>
                <Button htmlType="button" onClick={onReset}>
                    {intl.formatMessage({id: 'RESET_BTN'})}
                </Button>
            </Form.Item>
        </Form>
        <FileEditModal name={file.name} content={file.content} onSave={file.onSave}
                       visible={visible} onClose={() => setVisible(false)}/>
    </>);
});
export default ServerConfig;
