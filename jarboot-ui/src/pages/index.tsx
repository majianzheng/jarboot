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
    <TabPane key={'1'} tab={"设置"}>

    </TabPane>
</Tabs>
