import * as React from "react";
import { List, Typography, Divider } from 'antd';

export default class ServerSetting extends React.PureComponent {
    
    render() {
        const data = [
  'demo-server1',
  'demo-server2',
  'demo-server3',
  'demo-server4',
  'demo-server5',
  'demo-server6',
  'demo-server7',
  'demo-server8',
  'demo-server4',
  'demo-server5',
  'demo-server6',
  'demo-server7',
  'demo-server8',
];
        return <div style={{display: 'flex'}}>
            <div style={{flex: 'inherit', width: '28%', height: '500px', overflowY: 'auto'}}>
                <List size="large"
                      header={<div>服务列表</div>}
                      bordered
                      dataSource={data}
                      renderItem={item => <List.Item>{item}</List.Item>}/>
            </div>
            <div style={{flex: 'inherit', width: '72%'}}>
                配置内容，开发中...
            </div>
        </div>
    }
}