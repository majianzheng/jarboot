import React, {memo, useEffect, useRef} from "react";
import {useIntl} from "umi";
import LoginForm from "@/pages/login/LoginForm";
import styles from "./index.less";
import BgAnimation from "@/pages/login/BgAnimation";
import JarbootDesc from "@/pages/login/JarbootDesc";
import {ProjectHome, SelectLang} from "@/components/extra";
import {Button} from "antd";
import {JarBootConst} from "@/common/JarBootConst";

const LoginMenu = memo(() => {
    const intl = useIntl();
    return <div className={styles.loginMenu}>
        <img height={40} className={styles.logoImg} src={require('@/assets/logo.png')} alt={"logo"}/>
        <span className={styles.rightItems}>
            <Button type={"link"} href={JarBootConst.DOCS_URL}
                    className={styles.rightMenuDocs}>
                {intl.formatMessage({id: 'MENU_DOCS'})}
            </Button>
            <SelectLang className={styles.loginSelectLang}/>
            <ProjectHome iconClass={styles.loginGithubIcon}/>
        </span>
    </div>;
});

const Login = memo(() => {
    const devRef: any = useRef();
    useEffect(() => {
        const bga = new BgAnimation(devRef);
        bga.run();
    }, []);
    return <div className={styles.loginPage} ref={devRef}>
        <LoginMenu/>
        <JarbootDesc/>
        <LoginForm/>
    </div>;
});

export default Login;
