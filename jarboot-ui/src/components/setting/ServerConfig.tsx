import {Form, Input, Button, InputNumber, Switch} from 'antd';
import {memo, useEffect} from "react";
import SettingService from "@/services/SettingService";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";
import { useIntl } from 'umi';
import StringUtil from "@/common/StringUtil";

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
    const onReset = () => {
        SettingService.getServerSetting(props.server
        ).then((resp: any) => {
            if (0 !== resp.resultCode) {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
                return;
            }
            form.setFieldsValue(resp.result);
        }).catch((error: any) => CommonNotice.error(ErrorUtil.formatErrResp(error)));
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
                CommonNotice.info("提交成功！")
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp))
            }
        }).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    };
    return (
        <Form {...layout} form={form} name="control-hooks" onFinish={onSubmit}>
            <Form.Item name="jar"
                       label={intl.formatMessage({id: 'JAR_LABEL'})}
                       rules={[{required: false}]}>
                <Input placeholder={"指定Main Class所在的jar，为空则默认第一个"} autoComplete="off"/>
            </Form.Item>
            <Form.Item name="jvm"
                       label={intl.formatMessage({id: 'JVM_OPT_LABEL'})}
                       rules={[{required: false}]}>
                <Input autoComplete="off"/>
            </Form.Item>
            <Form.Item name="args"
                       label={intl.formatMessage({id: 'MAIN_ARGS_LABEL'})}
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
    );
});
export default ServerConfig;
