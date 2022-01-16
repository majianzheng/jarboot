import {memo, useState} from "react";
import {useIntl} from "umi";
import {Menu, Dropdown, Button} from "antd";
import {
    LoadingOutlined,
    SettingOutlined,
    CodeFilled,
    CaretRightFilled,
    PoweroffOutlined
} from '@ant-design/icons';
import ServiceManager from "@/services/ServiceManager";
import styles from "./index.less";
import {CommonConst} from "@/common/CommonConst";
import * as React from "react";
import {RestartIcon} from "@/components/icons";

interface BottomBarProp {
    contentView: 'config'|'console';
    onViewChange: (key: string, value: string) => void;
}

const BottomBar = (props: BottomBarProp) => {
    const intl = useIntl();
    const [loading, setLoading] = useState(false);
    const disableOnClickButton = () => {
        if (loading) {
            return;
        }
        setLoading(true);
        setTimeout(() => setLoading(false), 5000);
    };

    const oneClickRestart = () => {
        disableOnClickButton();
        ServiceManager.oneClickRestart();
    };

    const oneClickStart = () => {
        disableOnClickButton();
        ServiceManager.oneClickStart();
    };

    const oneClickStop = () => {
        disableOnClickButton();
        ServiceManager.oneClickStop();
    };

    const onViewModeChange = (key: string) => {
        let value = '';
        switch (key) {
            case CommonConst.CONTENT_VIEW:
                value = (props.contentView === CommonConst.CONSOLE_VIEW) ?
                    CommonConst.CONFIG_VIEW : CommonConst.CONSOLE_VIEW;
                break;
            default:
                return;
        }
        props?.onViewChange(key, value);
    };

    const menu = (
        <Menu>
            <Menu.Item key="1"
                       icon={<RestartIcon className={styles.toolButtonIcon}/>}
                       onClick={oneClickRestart}>{intl.formatMessage({id: 'ONE_KEY_RESTART'})}</Menu.Item>
            <Menu.Item key="2"
                       icon={<PoweroffOutlined className={styles.toolButtonRedIcon}/>}
                       onClick={oneClickStop}>{intl.formatMessage({id: 'ONE_KEY_STOP'})}</Menu.Item>
        </Menu>
    );

    const button = (view: string) => {
        let icon;
        let text = '';
        let key = '';
        switch (view) {
            case CommonConst.CONFIG_VIEW:
                icon = <SettingOutlined className={styles.toolButtonIcon}/>;
                text = intl.formatMessage({id: 'SERVICES_CONF'});
                key = 'contentView';
                break;
            case CommonConst.CONSOLE_VIEW:
                icon = <CodeFilled className={styles.toolButtonConsoleIcon}/>;
                text = intl.formatMessage({id: 'CONSOLE_VIEW'});
                key = 'contentView';
                break;
            default:
                icon = undefined;
                break;
        }
        return (<Button type={"text"}
                        icon={icon} key={key}
                        onClick={() => onViewModeChange(key)}
                        className={styles.bottomBarButton}>{text}</Button>);
    };

    return (<div className={styles.bottomBar}>
        <Dropdown overlay={menu} disabled={loading} className={styles.bottomBarButton}>
            <Button type={"text"}
                    icon={loading ? <LoadingOutlined/> : <CaretRightFilled className={styles.toolButtonIcon}/>}
                    onClick={oneClickStart}
                    disabled={loading}>
                {intl.formatMessage({id: 'ONE_KEY_START'})}
            </Button>
        </Dropdown>
        {button(props.contentView)}
    </div>);
};

BottomBar.defaultProps = {
    contentView: CommonConst.CONFIG_VIEW,
};

export default memo(BottomBar);
