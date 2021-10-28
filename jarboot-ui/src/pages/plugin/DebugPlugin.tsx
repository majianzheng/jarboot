import {Button, Form, Input} from "antd";
import {useIntl} from "umi";
import Request from "@/common/Request";
import CommonNotice from "@/common/CommonNotice";
import {memo, useState} from "react";
import {SuperPanel} from "@/components/servers/SuperPanel";
import {WsManager} from "@/common/WsManager";

const layout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
};
const tailLayout = {
    wrapperCol: {offset: 12, span: 12},
};

WsManager.initWebsocket();

const DebugPlugin = memo(() => {
    const intl = useIntl();
    const [server, setServer] = useState('');

    const onSubmit = (data: any) => {
        setServer("");
        setServer(data.server);
        data.runnable = false;
        Request.post(`/api/jarboot/plugin/debug/startServer`, data).then(resp => {
            if (resp?.resultCode === 0) {
                CommonNotice.info("Start success!");
            } else {
                CommonNotice.errorFormatted(resp);
            }
        }).catch(CommonNotice.errorFormatted);
    };

    return (<>
        <h2 style={{textAlign: "center"}}>Jarboot debug plugin UI</h2>
        <h3 style={{textAlign: "center"}}>Start server</h3>
        <Form {...layout} onFinish={onSubmit}>
            <Form.Item name="server"
                       label={"server"}
                       rules={[{required: true}]}>
                <Input autoComplete="off"/>
            </Form.Item>
            <Form.Item name="userDefineRunArgument"
                       label={intl.formatMessage({id: 'USER_DEFINE_RUN_LABEL'})}
                       rules={[{required: true}]}>
                <Input.TextArea rows={2}
                                placeholder={"Example:  1) -jar xx.jar    2) MainClassName    " +
                                "3) -cp xx.jar *.*.MainClass mainMethod    4) -classpath **.jar *.*ClassName"}
                                autoComplete="off"/>
            </Form.Item>
            <Form.Item name="args"
                       label={intl.formatMessage({id: 'MAIN_ARGS_LABEL'})}
                       rules={[{required: false}]}>
                <Input autoComplete="off"
                       placeholder={"Main arguments"}
                       autoCorrect="off"
                       autoCapitalize="off"
                       spellCheck="false"/>
            </Form.Item>
            <Form.Item name="jdkPath"
                       label={"JDK"}
                       rules={[{required: false}]}>
                <Input autoComplete="off"/>
            </Form.Item>
            <Form.Item name="workDirectory"
                       label={intl.formatMessage({id: 'WORK_HOME_LABEL'})}
                       rules={[{required: false}]}>
                <Input autoComplete="off"
                       autoCorrect="off"
                       autoCapitalize="off"
                       spellCheck="false"/>
            </Form.Item>
            <Form.Item name="env"
                       label={intl.formatMessage({id: 'ENV_LABEL'})}
                       rules={[{required: false}]}>
                <Input placeholder={"env1=val1,env2=val2"}
                       autoComplete="off"
                       autoCorrect="off"
                       autoCapitalize="off"
                       spellCheck="false"/>
            </Form.Item>
            <Form.Item {...tailLayout}>
                <Button type="primary" htmlType="submit" style={{marginRight: 8}}>
                    Start
                </Button>
            </Form.Item>
        </Form>
        {server && <SuperPanel server={server} visible={true} key={server}/>}
    </>)
});

export default DebugPlugin;
