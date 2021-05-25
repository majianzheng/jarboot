import {PureComponent} from "react";
import {List} from "antd";
import styles from "@/components/setting/index.less";
import * as React from "react";
import SettingService from "@/services/SettingService";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";
import {JarBootConst} from "@/common/JarBootConst";

export default class ArthasAdapterView extends PureComponent {
    state = {data: [], current: "", starting: false};
    componentDidMount() {
        this._query();
    }

    private _query = () => {
        SettingService.getServerList((resp: any) => {
            if (resp.resultCode < 0) {
                CommonNotice.error(resp.resultMsg);
                return;
            }
            if (resp.result.length <= 0) {
                return;
            }
            const data = resp.result.filter((value: any) => JarBootConst.STATUS_STARTED === value.status);
            let current = "";
            if (data.length > 0)
                current = data[0].name;
            this.setState({data, current});
        }, (errorMsg: any
        ) => console.warn(`${ErrorUtil.formatErrResp(errorMsg)}`));
    };

    private _onSelect = (event: any) => {
        const current = event.target.title;
        this.setState({current});
    };

    render() {
        return <div style={{display: 'flex'}}>
            <div style={{flex: 'inherit', width: '18%', height: '88vh', overflowY: 'auto'}}>
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
                <div style={{width: '82%'}}>
                    {this.state.starting &&
                    <iframe src={"/jarboot-service/arthas"}
                            style={{width: '100%', height: '90vh'}} frameBorder={0}/>}
                </div>
            </div>
        </div>
    }
}
