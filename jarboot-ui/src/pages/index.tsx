import * as React from "react";
import {Tabs, ConfigProvider, Select} from "antd";
import zh_CN from 'antd/lib/locale-provider/zh_CN';
import en_GB from 'antd/lib/locale-provider/en_GB';
import {GlobalSetting, ServerSetting, Dashboard} from "@/components";
import ArthasAdapterView from "@/pages/arthas/ArthasAdapterView";

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
                    <ArthasAdapterView/>
                </TabPane>
                <TabPane key={'2'} tab={"服务设置"}>
                    <ServerSetting/>
                </TabPane>
                <TabPane key={'3'} tab={"全局配置"}>
                    <div style={{width: '66%'}}>
                        <GlobalSetting/>
                    </div>
                </TabPane>
            </Tabs>
        </ConfigProvider>;
    }
}
