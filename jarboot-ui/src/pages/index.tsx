import React, {memo, useEffect, useState} from "react";
import {Tabs, ConfigProvider, Button} from "antd";
import zh_CN from 'antd/lib/locale-provider/zh_CN';
import en_GB from 'antd/lib/locale-provider/en_GB';
import About from "@/pages/help/About";
import styles from './index.less';
import {WsManager} from "@/common/WsManager";
import { useIntl, getLocale } from 'umi';
import OAuthService from "@/services/OAuthService";
import CommonNotice from "@/common/CommonNotice";
import {JarBootConst} from "@/common/JarBootConst";
import StringUtil from "@/common/StringUtil";
import {SelectLang, UserMenu, ProjectHome, JarbootVersion} from "@/components/extra";
import CommonUtils from "@/common/CommonUtils";
import ServerMgrView from "@/components/servers";
import {GlobalSetting, ServerSetting} from "@/components/setting";
import AuthControl from "@/components/auth";

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
        <TabPane key={'2'} tab={intl.formatMessage({id: 'SERVICES_CONF'})}>
            <ServerSetting/>
        </TabPane>
        <TabPane key={'3'} tab={intl.formatMessage({id: 'AUTH_CONTROL'})}>
            <AuthControl/>
        </TabPane>
        <TabPane key={'4'} tab={intl.formatMessage({id: 'SETTING'})}>
            <div style={{width: '96%'}}>
                <GlobalSetting/>
            </div>
        </TabPane>
        <TabPane key={'5'} tab={intl.formatMessage({id: 'HELP_DOC'})}>
            <About/>
        </TabPane>
    </Tabs>
});

const token = localStorage.getItem(JarBootConst.TOKEN_KEY);
//路由入口类
const index = memo(() => {
    if (StringUtil.isEmpty(token)) {
        CommonUtils.loginPage();
        return <></>;
    }
    const intl = useIntl();
    const [lang, setLang] = useState(getLocale());
    const [username, setUsername] = useState("");
    const welcome = () => {
        console.log(`%c▅▇█▓▒(’ω’)▒▓█▇▅▂`, 'color: magenta');
        console.log(`%c(灬°ω°灬) `, 'color:magenta');
        console.log(`%c（づ￣3￣）づ╭❤～`, 'color:red');
        WsManager.initWebsocket();
    }
    useEffect(() => {
        OAuthService.login().then(resp => {
            if (401 === resp.resultCode) {
                CommonUtils.loginPage();
                return;
            }
            if (resp.resultCode !== 0) {
                CommonNotice.errorFormatted(resp);
                CommonUtils.loginPage();
                return;
            }
            const jarbootUser: any = resp.result;
            setUsername(jarbootUser.username);
            JarBootConst.currentUser = jarbootUser;
            welcome();
        }).catch(CommonNotice.errorFormatted);
    }, []);

    const _onLocaleChange = (s: string) => {
        setLang(s);
    };
    const rightExtra = <div className={styles.rightExtra}>
        <JarbootVersion/>
        <Button type={"text"} href={"https://www.yuque.com/jarboot/usage/tmpomo"}
                style={{top: '-5px'}} target={"_blank"}>
            {intl.formatMessage({id: 'MENU_DOCS'})}
        </Button>
        <SelectLang onLocaleChange={_onLocaleChange}/>
        <ProjectHome iconClass={styles.githubIcon}/>
        <UserMenu username={username} className={styles.userLogin}/>
    </div>;
    const leftExtra = <div className={styles.leftExtra}><img src={require('@/assets/logo.png')} alt={"logo"}/></div>;
    const extra = {left: leftExtra, right: rightExtra};
    return <ConfigProvider locale={localeMap[lang]}>
        <TabPanes extra={extra}/>
    </ConfigProvider>;
});

export default index;
