import * as React from "react";
// @ts-ignore
import Dashboard from "@/components/dashboard/Dashboard";
import ServerSetting from "@/components/serverSetting/ServerSetting";
import {Tabs, ConfigProvider, Select} from "antd";
import zh_CN from 'antd/lib/locale-provider/zh_CN';
import en_GB from 'antd/lib/locale-provider/en_GB';

const {TabPane} = Tabs;
const localeMap: any = {'zh': zh_CN, 'en': en_GB};

export default class Index extends React.PureComponent {
    state = {locale: 'zh'};
    private _onLocaleChange = (locale: string) => {
        this.setState({locale});
    };
    render() {
        const localeSelect = <Select value={this.state.locale}
                                     style={{width: 130}}
                                     onChange={this._onLocaleChange}>
            <Select.Option value={'zh'} key={"zh"}>中文</Select.Option>
            <Select.Option value={'en'} key={"en"}>English</Select.Option>
        </Select>;
        return <ConfigProvider locale={localeMap[this.state.locale]}>
            <Tabs defaultActiveKey={'0'} size={'large'}
                  tabBarExtraContent={localeSelect}
                  style={{margin: '0 15px 0 15px'}}>
                <TabPane key={'0'} tab={'服务管理'}>
                    <Dashboard visible={true}
                               routes={[{path: "first", breadcrumbName: "服务管理"},]}/>
                </TabPane>
                <TabPane key={'1'} tab={"Arthas"}>
                    <iframe src={"/jarboot-service/arthas"} style={{width: '100%', height: '90vh'}} frameBorder={0}/>
                </TabPane>
                <TabPane key={'2'} tab={"服务设置"}>
                    <ServerSetting/>
                </TabPane>
                <TabPane key={'3'} tab={"全局配置"}>
                    开发中...
                </TabPane>
            </Tabs></ConfigProvider>;
    }
}
