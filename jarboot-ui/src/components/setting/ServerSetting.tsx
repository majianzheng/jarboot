import * as React from "react";
import { List } from 'antd';
import ServerConfig from "@/components/setting/ServerConfig";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from '../../common/ErrorUtil';
import styles from './index.less';
import ServerMgrService from "@/services/ServerMgrService";
import Logger from "@/common/Logger";

export default class ServerSetting extends React.PureComponent {

    state = {data: [], current: ""};
    componentDidMount() {
        this._query();
    }

    private _query = () => {
        ServerMgrService.getServerList((resp: any) => {
            if (resp.resultCode < 0) {
                CommonNotice.error(resp.resultMsg);
                return;
            }
            let current = "";
            if (resp.result.length > 0)
                current = resp.result[0].name;
            this.setState({data: resp.result, current});
        }, (errorMsg: any) => Logger.warn(`${ErrorUtil.formatErrResp(errorMsg)}`));
    };
    private _onSelect = (event: any) => {
        const current = event.target.title;
        this.setState({current});
    };
    render() {
        return <div style={{display: 'flex'}}>
            <div style={{flex: 'inherit', width: '26%', height: '88vh', overflowY: 'auto'}}>
                <List size="large"
                      header={<div>服务列表</div>}
                      bordered
                      dataSource={this.state.data}
                      renderItem={(item: any) => <List.Item
                          title={item.name}
                          className={item.name === this.state.current ? styles.taskItemSelected : styles.taskItem}
                          onClick={this._onSelect}>{item.name}</List.Item>}/>
            </div>
            <div style={{flex: 'inherit', width: '72%'}}>
                <div style={{width: '80%'}}>
                    <ServerConfig server={this.state.current}/>
                </div>
            </div>
        </div>
    }
}
