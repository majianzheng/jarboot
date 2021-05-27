import React, {memo, useEffect} from "react";
import {Button, Form, Input} from "antd";
import OAuthService from "@/services/OAuthService";
import StringUtil from "@/common/StringUtil";

const layout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
};
const tailLayout = {
    wrapperCol: {offset: 8, span: 16},
};

const Login: any = memo(() => {
    useEffect(() => {
        OAuthService.getCurrentUser().then((resp: any) => {
            if (StringUtil.isNotEmpty(resp?.result?.userName)) {
                location.assign("/");
            }
        });
    });
    const onSubmit = (data: any) => {
        OAuthService.login(data.userName, data.password);
    };
    const [form] = Form.useForm();
    return <div style={{width: "50%"}}>
        <Form {...layout} form={form} name="control-hooks" onFinish={onSubmit}>
            <Form.Item name="userName" label={"User"} rules={[{required: true}]}>
                <Input placeholder={"Input user name."}/>
            </Form.Item>
            <Form.Item name="password" label={"Password"} rules={[{required: true}]}>
                <Input.Password/>
            </Form.Item>
            <Form.Item {...tailLayout}>
                <Button type="primary" htmlType="submit" style={{marginRight: 8}}>
                    Login
                </Button>
            </Form.Item>
        </Form>
    </div>
});
