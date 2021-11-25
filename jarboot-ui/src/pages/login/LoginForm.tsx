import React, {memo, useEffect} from "react";
import {Button, Form, Input} from "antd";
import OAuthService from "@/services/OAuthService";
import StringUtil from "@/common/StringUtil";
import {useIntl} from "umi";
import CommonNotice from "@/common/CommonNotice";
import {JarBootConst} from "@/common/JarBootConst";
import {UserOutlined, LockOutlined} from "@ant-design/icons";
import styles from "./index.less";

const LoginForm = memo(() => {
    const intl = useIntl();
    useEffect(() => {
        OAuthService.getCurrentUser().then((resp: any) => {
            if (0 === resp?.resultCode && StringUtil.isNotEmpty(resp?.result?.username)) {
                location.assign("/");
            }
        });
    }, []);
    const onSubmit = (data: any) => {
        OAuthService.login(data.username, data.password).then(resp => {
            if (resp.resultCode !== 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            const jarbootUser: any = resp.result;
            localStorage.setItem(JarBootConst.TOKEN_KEY, "Bearer " + jarbootUser.accessToken);
            location.assign("/");
        }).catch(CommonNotice.errorFormatted);
    };

    return <div className={styles.loginForm}>
        <div className={styles.loginHeader}>{intl.formatMessage({id: 'LOGIN'})}</div>
        <div className={styles.internalSysTip}>
            <div>{intl.formatMessage({id: 'INTERNAL_SYS_TIP'})}</div>
            <div>{intl.formatMessage({id: 'INTERNAL_SYS_TIP1'})}</div>
        </div>
        <Form name="main_login"
              className={styles.loginFormView}
              onFinish={onSubmit}>
            <Form.Item name="username"
                       rules={[{ required: true, message: intl.formatMessage({id: 'INPUT_USERNAME'}) }]}>
                <Input prefix={<UserOutlined className="site-form-item-icon" />}
                       placeholder={intl.formatMessage({id: 'USER_NAME'})}
                       autoComplete="off"/>
            </Form.Item>
            <Form.Item name="password"
                       rules={[{ required: true, message: intl.formatMessage({id: 'INPUT_PASSWORD'}) }]}>
                <Input.Password prefix={<LockOutlined className="site-form-item-icon" />}
                                placeholder={intl.formatMessage({id: 'PASSWORD'})}/>
            </Form.Item>
            <Form.Item>
                <Button type="primary" htmlType="submit">
                    {intl.formatMessage({id: 'LOGIN'})}
                </Button>
            </Form.Item>
        </Form>
    </div>
});
export default LoginForm
