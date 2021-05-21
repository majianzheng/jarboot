import * as React from "react";
// @ts-ignore
import Dashboard from "@/components/dashboard/Dashboard";
import {Tabs} from "antd";

const {TabPane} = Tabs;

export default () => <Tabs defaultActiveKey={'0'} size={'large'} style={{margin: '0 15px 0 15px'}}>
    <TabPane key={'0'} tab={'服务管理'}>
        <Dashboard visible={true}
                   routes={[{path: "first", breadcrumbName: "服务管理"},]}/>
    </TabPane>
    <TabPane key={'1'} tab={"Arthas"}>
        使用Arthas调试指定的服务进程
    </TabPane>
    <TabPane key={'2'} tab={"服务设置"}>
        JVM参数、启动参数、服务守护、jar文件更新监控等设定
    </TabPane>
    <TabPane key={'3'} tab={"全局配置"}>
        默认JVM参数配置
    </TabPane>
</Tabs>
