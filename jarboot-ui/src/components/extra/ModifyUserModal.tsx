import React, {memo} from "react";
import {Modal, Form, Input} from "antd";
import UserService from "@/services/UserService";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";
import {useIntl} from "umi";
import {LockOutlined, UserOutlined} from "@ant-design/icons";

interface ModifyPasswordModalProp {
    visible: boolean;
    username: string;
    onClose: (success?: boolean) => void;
    isCreate: boolean;
};

const ModifyUserModal: any = memo((props: ModifyPasswordModalProp) => {
    const intl = useIntl();
    const [form] = Form.useForm();

    const onModifyPassword = () => {
        form.submit();
    };

    const onSubmit = (data: any) => {
        const doSubmit = props.isCreate ? UserService.createUser : UserService.updateUserPassword;
        doSubmit(data.username, data.password).then(resp => {
            if (0 === resp.resultCode) {
                props.onClose && props.onClose(true);
            } else {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
            }
        }).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    };
    const onCancel = () => {
        props.onClose && props.onClose(false);
    };
    const style = {height: '38px', fontSize: '16px', width: '100%'};
    return <>
        {props.visible &&
        <Modal title={intl.formatMessage({id: props.isCreate ? 'CREATE_USER' : 'MODIFY_PWD'})}
               visible={true}
               destroyOnClose={true}
               onOk={onModifyPassword}
               onCancel={onCancel}>
            <Form form={form}
                  name="user-password-modify-form"
                  initialValues={{username: props.username, password: '', repeat: ''}}
                  onFinish={onSubmit}>
                <Form.Item name="username" rules={[{ required: true, message: intl.formatMessage({id: 'INPUT_USERNAME'}) }]}>
                    <Input prefix={<UserOutlined className="site-form-item-icon" />}
                           readOnly={!props.isCreate}
                           autoComplete="off"
                           autoCorrect="off"
                           autoCapitalize="off"
                           spellCheck="false"
                           placeholder={intl.formatMessage({id: 'USER_NAME'})} style={style}/>
                </Form.Item>
                <Form.Item name="password"
                           rules={[{ required: true, message: intl.formatMessage({id: 'INPUT_PASSWORD'})}]}>
                    <Input.Password prefix={<LockOutlined className="site-form-item-icon" />}
                                    autoComplete="off"
                                    autoCorrect="off"
                                    autoCapitalize="off"
                                    spellCheck="false"
                                    placeholder={intl.formatMessage({id: 'PASSWORD'})} style={style}/>
                </Form.Item>
                <Form.Item name="repeat"
                           rules={[{ required: true, message: intl.formatMessage({id: 'REPEAT_PASSWORD'}) },
                               ({ getFieldValue }) => ({
                                   validator(_, value) {
                                       if (!value || getFieldValue('password') === value) {
                                           return Promise.resolve();
                                       }
                                       return Promise.reject(new Error(intl.formatMessage({id: 'PWD_NOT_MATCH'})));
                                   },
                               })]}>
                    <Input.Password prefix={<LockOutlined className="site-form-item-icon" />}
                                    autoComplete="off"
                                    autoCorrect="off"
                                    autoCapitalize="off"
                                    spellCheck="false"
                                    placeholder={intl.formatMessage({id: 'REPEAT_PASSWORD'})} style={style}/>
                </Form.Item>
            </Form>
        </Modal>}
    </>;
});
ModifyUserModal.defaultProps = {
    isCreate: false,
    visible: false,
};
export default ModifyUserModal;
