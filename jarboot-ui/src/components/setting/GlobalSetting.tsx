import {Form, Input, Button, InputNumber} from 'antd';
import React, {memo, useEffect} from "react";
import SettingService from "../../services/SettingService";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "../../common/ErrorUtil";
import Logger from "@/common/Logger";

const layout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
};
const tailLayout = {
    wrapperCol: {offset: 8, span: 16},
};

const GlobalSetting: any = memo((props: any) => {
    const [form] = Form.useForm();

    const onReset = () => {
        Logger.log(props);
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
        SettingService.submitGlobalSetting(data);
    };
    return (
        <Form {...layout} form={form} name="control-hooks" onFinish={onSubmit}>
            <Form.Item name="servicesPath" label={"Root path"} rules={[{required: false}]}>
                <Input placeholder={"services directory"}/>
            </Form.Item>
            <Form.Item name="defaultJvmArg" label={"Default VM options"} rules={[{required: false}]}>
                <Input/>
            </Form.Item>
            <Form.Item name="maxStartTime" label="Max start time" rules={[{required: false}]}>
                <InputNumber min={3000} max={60000} defaultValue={30000}/>
            </Form.Item>
            <Form.Item name="arthasHome" label={"Arthas home"} rules={[{required: false}]}>
                <Input placeholder={"Arthas installed home path"}/>
            </Form.Item>
            <Form.Item {...tailLayout}>
                <Button type="primary" htmlType="submit" style={{marginRight: 8}}>
                    Submit
                </Button>
                <Button htmlType="button" onClick={onReset}>
                    Reset
                </Button>
            </Form.Item>
        </Form>
    );
});
export default GlobalSetting;
