import {Form, Input, Button, Switch} from 'antd';
import React, {memo, useEffect} from "react";
import SettingService from "@/services/SettingService";
import CommonNotice from "@/common/CommonNotice";
import { useIntl } from 'umi';

const layout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
};
const tailLayout = {
    wrapperCol: {offset: 12, span: 12},
};

const GlobalSetting: any = memo(() => {
    const [form] = Form.useForm();
    const intl = useIntl();
    const onReset = () => {
        SettingService.getGlobalSetting().then((resp: any) => {
            if (0 !== resp.resultCode) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            form.setFieldsValue(resp.result);
        }).catch(CommonNotice.errorFormatted);
    };
    useEffect(() => onReset(), []);
    const onSubmit = (data: any) => {
        SettingService.submitGlobalSetting(data).then(resp => {
            if (0 === resp?.resultCode) {
                CommonNotice.info(intl.formatMessage({id: 'SUCCESS'}));
            } else {
                CommonNotice.errorFormatted(resp)
            }
        }).catch(CommonNotice.errorFormatted);
    };

    return (
        <Form {...layout} form={form} name="global-setting-forms" onFinish={onSubmit}>
            <Form.Item name="workspace"
                       label={intl.formatMessage({id: 'SERVERS_PATH'})}
                       rules={[{required: false}]}>
                <Input placeholder={"Workspace directory"}
                       autoComplete="off" autoCorrect="off"
                       autoCapitalize="off"
                       spellCheck="false"/>
            </Form.Item>
            <Form.Item name="defaultVmOptions"
                       label={intl.formatMessage({id: 'DEFAULT_VM_OPT'})}
                       rules={[{required: false}]}>
                <Input autoComplete="off"
                       autoCorrect="off"
                       autoCapitalize="off"
                       spellCheck="false"/>
            </Form.Item>
            <Form.Item name="servicesAutoStart"
                       label={intl.formatMessage({id: 'AUTO_START_AFTER_INIT'})}
                       valuePropName={"checked"}
                       rules={[{required: false}]}>
                <Switch/>
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
export default GlobalSetting;
