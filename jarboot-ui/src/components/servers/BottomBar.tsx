import {memo, useState} from "react";
import {useIntl} from "umi";
import {Menu, Dropdown, Button} from "antd";
import {
    LoadingOutlined,
    SettingOutlined,
    BarsOutlined,
    CodeFilled,
    CaretRightFilled, PoweroffOutlined, ReloadOutlined
} from '@ant-design/icons';
import ServerMgrService from "@/services/ServerMgrService";
import styles from "./index.less";
import {JarBootConst} from "@/common/JarBootConst";
import * as React from "react";
import {RestartIcon, TreeIcon} from "@/components/icons";

interface BottomBarProp {
    sideView: 'tree'|'list';
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
        ServerMgrService.oneClickRestart();
    };

    const oneClickStart = () => {
        disableOnClickButton();
        ServerMgrService.oneClickStart();
    };

    const oneClickStop = () => {
        disableOnClickButton();
        ServerMgrService.oneClickStop();
    };

    const onViewModeChange = (key: string) => {
        let value = '';
        switch (key) {
            case JarBootConst.SIDE_VIEW:
                value = (props.sideView === JarBootConst.TREE_VIEW) ? JarBootConst.LIST_VIEW : JarBootConst.TREE_VIEW;
                break;
            case JarBootConst.CONTENT_VIEW:
                value = (props.contentView === JarBootConst.CONSOLE_VIEW) ?
                    JarBootConst.CONFIG_VIEW : JarBootConst.CONSOLE_VIEW;
                break;
            default:
                return;
        }
        props?.onViewChange(key, value);
    }

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
            case JarBootConst.LIST_VIEW:
                icon = <BarsOutlined className={styles.toolButtonIcon}/>;
                text = intl.formatMessage({id: 'LIST_VIEW'});
                key = 'sideView';
                break;
            case JarBootConst.TREE_VIEW:
                icon = <TreeIcon className={styles.toolButtonIcon}/>;
                text = intl.formatMessage({id: 'TREE_VIEW'});
                key = 'sideView';
                break;
            case JarBootConst.CONFIG_VIEW:
                icon = <SettingOutlined className={styles.toolButtonIcon}/>;
                text = intl.formatMessage({id: 'SERVICES_CONF'});
                key = 'contentView';
                break;
            case JarBootConst.CONSOLE_VIEW:
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
        {button(props.sideView)}
        {button(props.contentView)}
    </div>);
};

BottomBar.defaultProps = {
    sideView: JarBootConst.TREE_VIEW,
    contentView: JarBootConst.CONFIG_VIEW,
};

export default memo(BottomBar);
