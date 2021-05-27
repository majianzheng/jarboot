import * as React from "react";
import styles from "./index.less";
import {Button, Card, Tag, Space, AutoComplete, Input} from "antd";
import PropTypes from "prop-types";
import CommonTable from "../commonTable/CommonTable";
import ServerMgrService from "../../services/ServerMgrService";
import CommonNotice from '../../common/CommonNotice';
import StringUtil from "../../common/StringUtil";
import {SyncOutlined, CaretRightOutlined, ExclamationCircleOutlined, CaretRightFilled, EnterOutlined, LoadingOutlined,
    PoweroffOutlined, ReloadOutlined} from '@ant-design/icons';
import Console from "../console/Console";
import {JarBootConst} from '../../common/JarBootConst';
import WsManager from "./WsManager";
import ErrorUtil from '../../common/ErrorUtil';
import Logger from "../../common/Logger";

export default class Dashboard extends React.Component {
    static defaultProps = {
        mode: 'default',
        visible: false,
    };
    static propTypes = {
        mode: PropTypes.string,
        routes: PropTypes.array,
        visible: PropTypes.bool,
    };
    state = {command: '', executing: false, loading: false, data: [], selectedRowKeys: [], selectRows: [], current: '', oneClickLoading: false};
    allServerOut = [];
    fd = null;
    methodMap = new Map();
    height = window.innerHeight - 120;
    componentDidMount() {
        this.refreshWebServerList();

        //连接websocket
        this.initWebsocket();
        this.fd = window.setInterval(() => {
            if (this.props.visible) {
                this.initWebsocket();
            }
        }, 3000);
    }

    initWebsocket() {
        WsManager.addMessageHandler(this._onWebSocketMessage);
        WsManager.initWebsocket();
    }
    componentWillUnmount() {
        WsManager.removeMessageHandler(this._onWebSocketMessage);
        window.clearInterval(this.fd);
    }

    _onWebSocketMessage = data => {
        if (StringUtil.isEmpty(data)) {
            Logger.log(`接受到空数据！`);
            return;
        }
        let msgBody = null;
        try {
            msgBody = JSON.parse(data);
        } catch (error) {
            Logger.warn(error);
            Logger.log(data);
            CommonNotice.error(`解析消息失败！`);
            return;
        }
        if (this.methodMap.size <= 0) {
            Logger.log("this.methodMap 为空！");
            return;
        }
        const handler = this.methodMap.get(msgBody.server);
        switch (msgBody.msgType) {
            case JarBootConst.MSG_TYPE_OUT:
                handler.appendLine(msgBody.text);
                break;
            case JarBootConst.MSG_TYPE_START:
                Logger.log(`启动中${msgBody.server}...`);
                handler.startLoading();
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STARTING);
                break;
            case JarBootConst.MSG_TYPE_STOP:
                Logger.log(`停止中${msgBody.server}...`);
                handler.startLoading();
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STOPPING);
                break;
            case JarBootConst.MSG_TYPE_START_ERROR:
                Logger.log(`启动失败${msgBody.server}`);
                CommonNotice.error(`启动服务${msgBody.server}失败！`);
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STOPPED);
                break;
            case JarBootConst.MSG_TYPE_STARTED:
                Logger.log(`启动成功${msgBody.server}`);
                handler.finishLoading();
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STARTED, msgBody.text)
                break;
            case JarBootConst.MSG_TYPE_STOP_ERROR:
                Logger.log(`停止失败${msgBody.server}`);
                CommonNotice.error(`停止服务${msgBody.server}失败！`);
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STARTED);
                break;
            case JarBootConst.MSG_TYPE_STOPPED:
                Logger.log(`停止成功${msgBody.server}`);
                handler.finishLoading();
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STOPPED)
                break;
            case JarBootConst.MSG_TYPE_RESTART:
                Logger.log(`重启成功${msgBody.server}`);
                handler.finishLoading();
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STARTED, msgBody.text)
                break;
            case JarBootConst.MSG_TYPE_CMD_FINISH:
                handler.appendLine(msgBody.text);
                this.setState({executing: false});
                handler.finishLoading();
                break;
            case JarBootConst.NOTICE_ERROR:
            case JarBootConst.NOTICE_WARN:
            case JarBootConst.NOTICE_INFO:
                this._notice(msgBody.text, msgBody.msgType);
                break;
            default:
                handler.appendLine(msgBody.text);
                break;
        }
    };
    _notice(text, type) {
        switch (type) {
            case JarBootConst.NOTICE_INFO:
                CommonNotice.info("提示", text);
                break;
            case JarBootConst.NOTICE_WARN:
                CommonNotice.error("警告", text);
                break;
            case JarBootConst.NOTICE_ERROR:
                CommonNotice.error("错误", text);
                break;
            default:
                //ignore
                break;
        }
    }

    _updateServerStatus(msgBody, status, pid = '') {
        let {data} = this.state;
        data = data.map(value => {
            if (value.name === msgBody.server) {
                value.status = status;
                value.pid = pid;
            }
            return value;
        });
        this.setState({data, current: msgBody.server});
    }

    _getTbProps() {
        return {
            columns: [
                {
                    title: '名字',
                    dataIndex: 'name',
                    key: 'name',
                    ellipsis: true,
                },
                // {
                //     title: 'PID',
                //     dataIndex: 'pid',
                //     key: 'pid',
                //     ellipsis: true,
                // },
                // {
                //     title: '端口',
                //     dataIndex: 'port',
                //     key: 'port',
                //     ellipsis: true,
                // },
                {
                    title: '状态',
                    dataIndex: 'status',
                    key: 'status',
                    ellipsis: true,
                    width: 120,
                    render: text => this._translateStatus(text),
                },
            ],
            loading: this.state.loading,
            dataSource: this.state.data,
            pagination: false,
            rowKey: 'name',
            size: 'small',
            rowSelection: this._getRowSelection(),
            onRow: this._onRow.bind(this),
            showHeader: true,
            scroll: this.height,
        };
    }
    _translateStatus(status) {
        let tag;
        switch (status) {
            case JarBootConst.STATUS_STARTED:
                tag = <Tag icon={<CaretRightOutlined/>} color={"success"}>{status}</Tag>
                break;
            case JarBootConst.STATUS_STOPPED:
                tag = <Tag icon={<ExclamationCircleOutlined style={{color: '#f50'}}/>} color={"volcano"}>{status}</Tag>
                break;
            case JarBootConst.STATUS_STARTING:
                tag = <Tag icon={<SyncOutlined spin/>} color={"processing"}>{status}</Tag>
                break;
            case JarBootConst.STATUS_STOPPING:
                tag = <Tag icon={<SyncOutlined spin/>} color={"default"}>{status}</Tag>
                break;
            default:
                tag = <Tag color={"default"}>{status}</Tag>
                break;
        }
        return tag;
    }
    _getRowSelection() {
        return {
            type: 'checkbox',
            onChange: (selectedRowKeys, selectedRows) => {
                this.setState({
                    selectedRowKeys: selectedRowKeys,
                    selectRows: selectedRows,
                });
            },
            selectedRowKeys: this.state.selectedRowKeys,
        };
    }
    _onRow(record) {
        return {
            onClick: event => {
                if (this.props.onRowClick) {
                    this.props.onRowClick(event);
                }
                this.setState({
                    selectedRowKeys: [record.name],
                    selectRows: [record],
                    current: record.name,
                });
            },
            onDoubleClick: event => {
                if (this.props.onRowDbClick) {
                    this.props.onRowDbClick(event);
                }
            },
        };
    }

    _initAllServerOut(webServer) {
        let allList = [""];
        if (webServer && webServer.length > 0) {
            allList = allList.concat(webServer.map(value => value.name));
        }
          allList.forEach(value => {
            if (!this.methodMap.has(value)) {
              this.methodMap.set(value, {});
            }
          });
          this.allServerOut = allList;
    }

    refreshWebServerList = () => {
        this.setState({loading: true});
        ServerMgrService.getServerList(resp => {
            this.setState({loading: false});
            if (resp.resultCode < 0) {
                CommonNotice.error(resp.resultMsg);
                return;
            }
            this.setState({data: resp.result});
          this._initAllServerOut(resp.result);
        }, errorMsg => Logger.warn(`${ErrorUtil.formatErrResp(errorMsg)}`));
    };
    _finishCallback = resp => {
        this.setState({loading: false});
        if (resp.resultCode < 0) {
            CommonNotice.error(resp.resultMsg);
        }
    };
    startServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            CommonNotice.info("请选择一个服务执行");
            return;
        }
        this.setState({out: "", loading: true});
        this.initWebsocket();
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        ServerMgrService.startServer(this.state.selectedRowKeys, this._finishCallback,
                errorMsg => Logger.log(ErrorUtil.formatErrResp(errorMsg)));
    };
    _clearDisplay = server => {
      const handler = this.methodMap.get(server);
      handler && handler.clear()
    };
    stopServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            CommonNotice.info("请选择一个服务执行");
            return;
        }
        this.setState({out: "", loading: true});
        this.initWebsocket();
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        ServerMgrService.stopServer(this.state.selectedRowKeys, this._finishCallback,
                errorMsg => CommonNotice.error(ErrorUtil.formatErrResp(errorMsg)));
    };
    restartServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            CommonNotice.info("请选择一个服务执行");
            return;
        }
        this.setState({out: "", loading: true});
        this.initWebsocket();
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        ServerMgrService.restartServer(this.state.selectedRowKeys, this._finishCallback,
                errorMsg => CommonNotice.error(ErrorUtil.formatErrResp(errorMsg)));
    };
    _getTbBtnProps = () => {
        return [
            {
                name: '启动',
                key: 'start ',
                icon: <CaretRightFilled style={{color: 'green', fontSize: '18px'}}/>,
                onClick: this.startServer,
            },
            {
                name: '停止',
                key: 'stop',
                icon: <PoweroffOutlined style={{color: 'red', fontSize: '18px'}}/>,
                onClick: this.stopServer,
            },
            {
                name: '重启',
                key: 'reset',
                icon: <ReloadOutlined style={{color: '#1890ff', fontSize: '18px'}}/>,
                onClick: this.restartServer,
            },
            {
                name: '刷新',
                key: 'refresh',
                icon: <SyncOutlined style={{color: '#1890ff', fontSize: '18px'}}/>,
                onClick: this.refreshWebServerList,
            }
        ]
    };

    oneClickRestart = () => {
        this.initWebsocket();
        this._disableOnClickButton();
        ServerMgrService.oneClickRestart();
    };
    oneClickStart = () => {
        this.initWebsocket();
        this._disableOnClickButton();
        ServerMgrService.oneClickStart();
    };
    oneClickStop = () => {
        this.initWebsocket();
        this._disableOnClickButton();
        ServerMgrService.oneClickStop();
    };
    _disableOnClickButton() {
        if (this.state.oneClickLoading) {
            return;
        }
        this.setState({oneClickLoading: true}, () => {
            const td = setTimeout(() => {
                clearTimeout(td);
                this.setState({oneClickLoading: false});
            }, 5000);
        });
    }
    _statusIcon(status) {
        let tag;
        let style = {fontSize: 20};
        switch (status) {
            case JarBootConst.STATUS_STARTED:
                tag = <CaretRightOutlined style={style}/>;
                break;
            case JarBootConst.STATUS_STOPPED:
                tag = <ExclamationCircleOutlined style={{color: '#f50', ...style}}/>;
                break;
            case JarBootConst.STATUS_STARTING:
                tag = <SyncOutlined spin style={style}/>;
                break;
            case JarBootConst.STATUS_STOPPING:
                tag = <SyncOutlined spin style={style}/>;
                break;
            default:
                tag = status;
                break;
        }
        return tag;
    }
    _onExecCommand = () => {
        this.setState({executing: true});
        const handler = this.methodMap.get(this.state.current);
        handler.appendLine(`>${this.state.command}`);
        //格式：cmd 服务名 命令
        const text = `cmd ${this.state.current} ${this.state.command}`;
        WsManager.sendMessage(text);
        // ServerMgrService.sendCommand(this.state.current, this.state.command, () => {
        //     this.setState({executing: false});
        //     handler.finishLoading();
        // });
    };
    render() {
        let tableOption = this._getTbProps();
        tableOption.scroll = { y: this.height};
        let outTitle = (<>
            <AutoComplete placeholder={"输入命令执行"}
                          disabled={this.state.executing}
                          open={false}
                          value={this.state.command}
                          onChange={command => this.setState({command})}
                          options={[
                              {label: "jvm", value: "jvm"},
                              {label: "thread", value: "thread"},
                          ]}
                          children={<Input onPressEnter={this._onExecCommand}
                                           addonAfter={this.state.executing ? <LoadingOutlined/> :
                                               <EnterOutlined onClick={this._onExecCommand}/>}/>}
                          style={{width: '90%'}}/>
        </>);

        return (<div>
            <Space size={'middle'}>
                <Button type={'primary'} loading={this.state.oneClickLoading} onClick={this.oneClickRestart}>一键重启</Button>
                <Button loading={this.state.oneClickLoading} onClick={this.oneClickStart}>一键启动</Button>
                <Button loading={this.state.oneClickLoading} onClick={this.oneClickStop}>一键停止</Button>
            </Space>
            <div style={{display: 'flex'}}>
                <div style={{flex: 'inherit', width: '28%'}}>
                    <CommonTable bordered tableOption={tableOption} tableButtons={this._getTbBtnProps()} height={this.height}/>
                </div>
                <div style={{flex: 'inherit', width: '72%'}}>
                    <Card title={outTitle} size={"small"}
                          extra={<Button type={"link"} onClick={() => this._clearDisplay(this.state.current)}>清空</Button>}>
                        <div className={styles.outPanel}>
                            <Console key={'-1'} visible={this.state.current === ""}
                                     method={this.methodMap.get("")}/>
                            {this.allServerOut.map(value => (
                                <Console key={value} visible={this.state.current === value}
                                         method={this.methodMap.get(value)}/>
                            ))}
                        </div>
                    </Card>
                </div>
            </div>
        </div>);
    }
}
