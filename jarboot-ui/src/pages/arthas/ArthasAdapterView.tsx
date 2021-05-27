import React, {PureComponent, RefObject} from "react";
import {Button, List, Result, Tag, Space} from "antd";
import styles from "@/components/setting/index.less";
import {BugOutlined, SyncOutlined} from '@ant-design/icons';
import ServerMgrService from "@/services/ServerMgrService";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";
import {JarBootConst} from "@/common/JarBootConst";
import ArthasAdapterService from "@/services/ArthasAdapterService";
import StringUtil from "@/common/StringUtil";
import Logger from "@/common/Logger";

export default class ArthasAdapterView extends PureComponent {
    state = {data: [], current: "", starting: false, installed: false, attachedServer: ''};
    iframeRef: RefObject<HTMLIFrameElement> = React.createRef();
    componentDidMount() {
        this._checkInstalled();
        this._query();
    }

    private _checkInstalled = () => {
        ArthasAdapterService.checkArthasInstalled().then(resp => {
            if (resp.resultCode !== 0) {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
                return;
            }
            this.setState({installed: resp.result});
        }).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    };

    private _getStarting = () => {
        if (!this.state.installed) {
            this._checkInstalled();
            return;
        }
        ArthasAdapterService.getCurrentRunning().then(resp => {
            if (resp.resultCode !== 0) {
                CommonNotice.error(ErrorUtil.formatErrResp(resp));
                return;
            }
            let starting = StringUtil.isNotEmpty(resp.result);
            this.setState({attachedServer: resp.result, starting});
        }).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    };

    private _query = () => {
        ServerMgrService.getServerList((resp: any) => {
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
            this._getStarting();
        }, (errorMsg: any
        ) => Logger.warn(`${ErrorUtil.formatErrResp(errorMsg)}`));
    };

    private _onSelect = (event: any) => {
        const current = event.target.title;
        this.setState({current});
    };

    private attachToServer = () => {
        if (StringUtil.isEmpty(this.state.current)) {
            CommonNotice.info("请先选中要调试的服务");
            return;
        }
        ArthasAdapterService.attachToServer(this.state.current).then(resp => {
            if (resp.resultCode < 0) {
                CommonNotice.error(resp.resultMsg);
                return;
            }
            this._getStarting();
        }).catch(error => CommonNotice.error(ErrorUtil.formatErrResp(error)));
    };

    render() {
        const arthasConsole = <div>
            {this.state.starting ?
            <iframe src={"/jarboot-arthas/arthas"} ref={this.iframeRef}
                    style={{width: '100%', height: '90vh'}} frameBorder={0}/> :
                <Result title={"Arthas is not started, select one server to attach."}
                        extra={<Button type={"primary"} onClick={this._getStarting}>Check</Button>}/>}
        </div>;

        return <div style={{display: 'flex'}}>
            <div style={{flex: 'inherit', width: '18%', height: '88vh', overflowY: 'auto'}}>
                <List size="large"
                      header={<div>运行中的服务<Space size={"large"}>
                          <Button onClick={this._query} type={"link"} icon={<SyncOutlined/>}/>
                          <Button onClick={this.attachToServer} type={"link"}
                                  icon={<BugOutlined style={{color: 'green'}}/>}/>
                      </Space></div>}
                      bordered
                      dataSource={this.state.data}
                      renderItem={(item: any) => <List.Item
                          title={item.name}
                          actions={[ this.state.attachedServer === item.name ?
                              <Tag icon={<SyncOutlined spin/>} color={"processing"}>调试中</Tag> :
                              <Button onClick={this.attachToServer}
                                      icon={<BugOutlined style={{color: 'green'}}/>} type={"link"}>调试</Button>]}
                          className={item.name === this.state.current ? styles.taskItemSelected : styles.taskItem}
                          onClick={this._onSelect}>{item.name}</List.Item>}/>
            </div>
            <div style={{flex: 'inherit', width: '80%'}}>
                <div>
                    {this.state.installed ? arthasConsole :
                        <Result title={"Arthas is not installed or not config in jarboot."}
                                extra={<Button type={"primary"} onClick={this._checkInstalled}>Check</Button>}/>}
                </div>
            </div>
        </div>
    }
}
