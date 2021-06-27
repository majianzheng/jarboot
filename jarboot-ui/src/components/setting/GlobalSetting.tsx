import {Form, Input, Button, InputNumber} from 'antd';
import React, {memo, useEffect} from "react";
import SettingService from "@/services/SettingService";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";
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
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
                return;
            }
            form.setFieldsValue(resp.result);
        }).catch((error: any) => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    };
    useEffect(() => onReset());
    const onSubmit = (data: any) => {
        SettingService.submitGlobalSetting(data).then(resp => {
            if (0 === resp?.resultCode) {
                CommonNotice.info("提交成功！")
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp))
            }
        }).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    };

    return (
        <Form {...layout} form={form} name="global-setting-forms" onFinish={onSubmit}>
            <Form.Item name="servicesPath"
                       label={intl.formatMessage({id: 'SERVERS_PATH'})}
                       rules={[{required: false}]}>
                <Input placeholder={"services directory"} autoComplete="off"/>
            </Form.Item>
            <Form.Item name="defaultJvmArg"
                       label={intl.formatMessage({id: 'DEFAULT_VM_OPT'})}
                       rules={[{required: false}]}>
                <Input autoComplete="off"/>
            </Form.Item>
            <Form.Item name="maxStartTime"
                       label={intl.formatMessage({id: 'MAX_START_TIME'})}
                       rules={[{required: false}]}>
                <InputNumber min={3000} max={60000} defaultValue={30000} autoComplete="off"/>
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
