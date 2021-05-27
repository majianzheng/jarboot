import {Form, Input, Button, InputNumber, Switch} from 'antd';
import {memo, useEffect} from "react";
import SettingService from "../../services/SettingService";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "../../common/ErrorUtil";

const layout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
};
const tailLayout = {
    wrapperCol: {offset: 8, span: 16},
};

const ServerConfig: any = memo((props: any) => {
    const [form] = Form.useForm();
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
        SettingService.submitServerSetting(props.server, data);
    };
    return (
        <Form {...layout} form={form} name="control-hooks" onFinish={onSubmit}>
            <Form.Item name="jar" label={"jar"} rules={[{required: false}]}>
                <Input placeholder={"指定Main Class所在的jar，为空则默认第一个"}/>
            </Form.Item>
            <Form.Item name="jvm" label={"VM options"} rules={[{required: false}]}>
                <Input/>
            </Form.Item>
            <Form.Item name="args" label="Main args" rules={[{required: false}]}>
                <Input/>
            </Form.Item>
            <Form.Item name="priority" label="Priority" rules={[{required: false}]}>
                <InputNumber min={1} max={9999} defaultValue={1}/>
            </Form.Item>
            <Form.Item name="daemon" label="daemon" rules={[{required: false}]} valuePropName={"checked"}>
                <Switch defaultChecked/>
            </Form.Item>
            <Form.Item name="jarUpdateWatch" label="jar Watch" rules={[{required: false}]} valuePropName={"checked"}>
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
});
export default ServerConfig;
