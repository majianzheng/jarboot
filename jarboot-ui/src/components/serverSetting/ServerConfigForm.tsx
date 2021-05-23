import * as React from "react";
import {Form, Input, Button, InputNumber, Switch} from 'antd';

const layout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
};
const tailLayout = {
    wrapperCol: {offset: 8, span: 16},
};

const ServerConfigForm: any = (props: any) => {
    const [form] = Form.useForm();

    const onFinish = (values: any) => {
        console.log(values);
    };

    const onReset = () => {
        form.resetFields();
    };

    const onFill = () => {
        form.setFieldsValue({
            note: 'Hello world!',
            gender: 'male',
        });
    };

    return (
        <Form {...layout} form={form} name="control-hooks" onFinish={onFinish}>
            <Form.Item name="jvm" label={"VM options"} rules={[{required: false}]}>
                <Input/>
            </Form.Item>
            <Form.Item name="args" label="Main args" rules={[{required: false}]}>
                <Input/>
            </Form.Item>
            <Form.Item name="priority" label="Priority" rules={[{required: false}]}>
                <InputNumber min={1} max={9999} defaultValue={1}/>
            </Form.Item>
            <Form.Item name="daemon" label="daemon" rules={[{required: false}]}>
                <Switch defaultChecked/>
            </Form.Item>
            <Form.Item name="jarUpdateWatch" label="jar Watch" rules={[{required: false}]}>
                <Switch defaultChecked/>
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
};
export default ServerConfigForm;
