import { setLocale, useIntl, getLocale } from 'umi';
import {Button} from "antd";
import React from "react";

const SelectLang = (props: any) => {
    const intl = useIntl();
    const changLang = () => {
        const locale = getLocale();
        if (!locale || locale === 'zh-CN') {
            props.onLocaleChange && props.onLocaleChange('en-US');
            setLocale('en-US', false);
        } else {
            props.onLocaleChange && props.onLocaleChange('zh-CN');
            setLocale('zh-CN', false);
        }
    };
    return <Button size="small" style={{margin: '0 8px',}} onClick={changLang} className={props.className}>
        {intl.formatMessage({id: 'navbar.lang'})}
    </Button>;
};
export default SelectLang;
