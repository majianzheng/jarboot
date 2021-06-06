import * as React from "react";
import {Tabs, ConfigProvider, Tooltip, Avatar, Button, Popover, Menu} from "antd";
import zh_CN from 'antd/lib/locale-provider/zh_CN';
import en_GB from 'antd/lib/locale-provider/en_GB';
import {GlobalSetting, ServerSetting, ServerMgrView} from "@/components";
import Help from "@/pages/help/Help";
import {UserOutlined, GithubOutlined, CaretDownOutlined, LoginOutlined} from '@ant-design/icons';
import styles from './index.less';
import {WsManager} from "@/common/WsManager";
import { setLocale, useIntl, getLocale } from 'umi';
import {memo} from "react";

const {TabPane} = Tabs;
const localeMap: any = {'zh-CN': zh_CN, 'en-US': en_GB};

/**
 * 主页
 * @author majianzheng
 */

//主界面内容
const TabPanes: any = memo((props: any) => {
    const intl = useIntl();
    return <Tabs defaultActiveKey={'0'} size={'large'}
                 tabBarExtraContent={props.extra}
                 style={{margin: '0 15px 0 15px'}}>
        <TabPane key={'0'} tab={intl.formatMessage({id: 'SERVICES_MGR'})}>
            <ServerMgrView/>
        </TabPane>
        {/*与Arthas的集成将作为隐藏的功能*/}
        {/*<TabPane key={'1'} tab={"Arthas"}>*/}
        {/*    <ArthasAdapterView/>*/}
        {/*</TabPane>*/}
        <TabPane key={'2'} tab={intl.formatMessage({id: 'SERVICES_CONF'})}>
            <ServerSetting/>
        </TabPane>
        <TabPane key={'3'} tab={intl.formatMessage({id: 'SETTING'})}>
            <div style={{width: '66%'}}>
                <GlobalSetting/>
            </div>
        </TabPane>
        <TabPane key={'4'} tab={intl.formatMessage({id: 'HELP_DOC'})}>
            <Help/>
        </TabPane>
    </Tabs>
});

//国际化，语言切换按钮
const SelectLang = (props: any) => {
    const intl = useIntl();
    const changLang = () => {
        const locale = getLocale();
        if (!locale || locale === 'zh-CN') {
            props.onLocaleChange('en-US');
            setLocale('en-US', false);
        } else {
            props.onLocaleChange('zh-CN');
            setLocale('zh-CN', false);
        }
    };
    return <Button
        size="small"
        style={{margin: '0 8px',}}
        onClick={changLang}>
        {intl.formatMessage({id: 'navbar.lang'})}
    </Button>;
};

//用户下拉菜单
const UserMenu: any = memo((props: any) => {
    const handleClick = (event: any) => {

    };

    return <>
        <Menu
            onClick={handleClick}
            defaultSelectedKeys={['quick-start']}
            mode="inline"
        >
            <Menu.Item><UserOutlined/>游客</Menu.Item>
            <Menu.Divider/>
            <Menu.Item key="quick-start">
                <LoginOutlined />
                鉴权功能暂未实现
            </Menu.Item>
        </Menu>
    </>;
});

//路由入口类
export default class Index extends React.PureComponent {
    state = {locale: 'zh-CN'};
    componentDidMount() {
        console.log(`%c▅▇█▓▒(’ω’)▒▓█▇▅▂`, 'color: yellow');
        console.log(`%c(灬°ω°灬) `, 'color:yellow');
        console.log(`%c（づ￣3￣）づ╭❤～`, 'color:yellow');
        WsManager.initWebsocket();
        const locale = getLocale();
        this.setState({locale});
    }

    private _onLocaleChange = (locale: string) => {
        //setLocale(locale, false);
        this.setState({locale});
    };
    render() {
        const rightExtra = <div className={styles.rightExtra}>
            <SelectLang onLocaleChange={this._onLocaleChange}/>
            <Tooltip title={"Github"}>
                <a target={"_blank"}
                   href={"https://github.com/majianzheng/jarboot"}
                   className={styles.githubIcon}>
                    <GithubOutlined/>
                </a>
            </Tooltip>
            <Popover content={<UserMenu/>} placement="bottomRight">
                <Avatar className={styles.userLogin} icon={<UserOutlined/>}/>
                <CaretDownOutlined style={{verticalAlign: 'text-top'}}/>
            </Popover>
        </div>;
        const leftExtra = <div className={styles.leftExtra}><img src={require('@/assets/logo.png')} alt={"logo"}/></div>;
        const extra = {left: leftExtra, right: rightExtra};
        return <ConfigProvider locale={localeMap[this.state.locale]}>
            <TabPanes extra={extra}/>
        </ConfigProvider>;
    }
}
