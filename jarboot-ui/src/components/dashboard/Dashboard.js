import * as React from "react";
import styles from "./index.less";
import {Button, Card, Tag, Space, Select} from "antd";
import PropTypes from "prop-types";
import CommonTable from "../commonTable/CommonTable";
import SettingService from "../../services/SettingService";
import CommonNotice from '../../common/CommonNotice';
import StringUtil from "../../common/StringUtil";
import {SyncOutlined, CaretRightOutlined, ExclamationCircleOutlined, CaretRightFilled,
    PoweroffOutlined, ReloadOutlined} from '@ant-design/icons';
import Console from "../console/Console";
import {JarBootConst} from '../../common/JarBootConst';
import WsManager from "./WsManager";
import ErrorUtil from '../../common/ErrorUtil';

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
    state = {tip: '', loading: false, data: [], selectedRowKeys: [], selectRows: [], current: '', basicServer: [], oneClickLoading: false};
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
            console.log(`接受到空数据！`);
            return;
        }
        let msgBody = null;
        try {
            msgBody = JSON.parse(data);
        } catch (error) {
            console.warn(error);
            console.log(data);
            CommonNotice.error(`解析消息失败！`);
            return;
        }
        if (this.methodMap.size <= 0) {
            console.log("this.methodMap 为空！");
            return;
        }
        const handler = this.methodMap.get(msgBody.server);
        switch (msgBody.msgType) {
            case JarBootConst.MSG_TYPE_OUT:
                handler.appendLine(msgBody.text);
                break;
            case JarBootConst.MSG_TYPE_START:
                console.log(`启动中${msgBody.server}...`);
                handler.startLoading();
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STARTING);
                break;
            case JarBootConst.MSG_TYPE_STOP:
                console.log(`停止中${msgBody.server}...`);
                handler.startLoading();
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STOPPING);
                break;
            case JarBootConst.MSG_TYPE_START_ERROR:
                console.log(`启动失败${msgBody.server}`);
                CommonNotice.error(`启动服务${msgBody.server}失败！`);
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STOPPED);
                break;
            case JarBootConst.MSG_TYPE_STARTED:
                console.log(`启动成功${msgBody.server}`);
                handler.finishLoading();
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STARTED, msgBody.text)
                break;
            case JarBootConst.MSG_TYPE_STOP_ERROR:
                console.log(`停止失败${msgBody.server}`);
                CommonNotice.error(`停止服务${msgBody.server}失败！`);
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STARTED);
                break;
            case JarBootConst.MSG_TYPE_STOPPED:
                console.log(`停止成功${msgBody.server}`);
                handler.finishLoading();
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STOPPED)
                break;
            case JarBootConst.MSG_TYPE_RESTART:
                console.log(`重启成功${msgBody.server}`);
                handler.finishLoading();
                this._updateServerStatus(msgBody, JarBootConst.STATUS_STARTED, msgBody.text)
                break;
            case JarBootConst.MSG_TYPE_NOTICE:
                this._notice(msgBody.text, msgBody.serverType);
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
        switch (msgBody.serverType) {
            case JarBootConst.SERVER_TYPE_WEB:
                this._updateWebServerRow(msgBody.server, status, pid)
                break;
            case JarBootConst.SERVER_TYPE_CORE:
                let coreServer = {...this.state.coreServer};
                coreServer.status = status;
                coreServer.pid = pid;
                this.setState({coreServer, current: msgBody.server});
                break;
            case JarBootConst.SERVER_TYPE_EXT:
                this._updateBasicServerRow(msgBody.server, status, pid)
                break;
            default:
                break;
        }
    }

    _updateWebServerRow(server, status, pid = '') {
        let {data} = this.state;
        data = data.map(value => {
            if (value.name === server) {
                value.status = status;
                value.pid = pid;
            }
            return value;
        });
        this.setState({data, current: server});
    }
    _updateBasicServerRow(server, status, pid = '') {
        let {basicServer} = this.state;
        if (!basicServer || basicServer.length <= 0) {
            return;
        }
        basicServer = basicServer.map(value => {
            if (value.name === server) {
                value.status = status;
                value.pid = pid;
            }
            return value;
        });
        this.setState({basicServer, current: server});
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
                {
                    title: 'PID',
                    dataIndex: 'pid',
                    key: 'pid',
                    ellipsis: true,
                },
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
                }, () => {
                    //TODO 切换配置，并保存
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
        SettingService.getWebServerList(resp => {
            this.setState({loading: false});
            if (resp.resultCode < 0) {
                CommonNotice.error(resp.resultMsg);
                return;
            }
            this.setState({data: resp.result});
          this._initAllServerOut(resp.result);
        }, errorMsg => console.warn(`${ErrorUtil.formatErrResp(errorMsg)}`));
    };
    startWebServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            CommonNotice.info("请选择一个服务执行");
            return;
        }
        this.setState({out: "", loading: true});
        this.initWebsocket();
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        SettingService.startWebServer(this.state.selectedRowKeys, resp => {
            this.setState({loading: false});
            if (resp.resultCode < 0) {
                CommonNotice.error(resp.resultMsg);
            }
        }, errorMsg => console.log(`${ErrorUtil.formatErrResp(errorMsg)}`));
    };
    _clearDisplay = server => {
      const handler = this.methodMap.get(server);
      handler && handler.clear()
    };
    stopWebServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            CommonNotice.info("请选择一个服务执行");
            return;
        }
        this.setState({out: "", loading: true});
        this.initWebsocket();
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        SettingService.stopWebServer(this.state.selectedRowKeys, resp => {
            this.setState({loading: false});
            if (resp.resultCode < 0) {
                CommonNotice.error(resp.resultMsg);
            }
        }, errorMsg => CommonNotice.error(`${ErrorUtil.formatErrResp(errorMsg)}`));
    };
    restartWebServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            CommonNotice.info("请选择一个服务执行");
            return;
        }
        this.setState({out: "", loading: true});
        this.initWebsocket();
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        SettingService.restartWebServer(this.state.selectedRowKeys, resp => {
            this.setState({loading: false});
            if (resp.resultCode < 0) {
                CommonNotice.error(resp.resultMsg);
            }
        }, errorMsg => CommonNotice.error(`${ErrorUtil.formatErrResp(errorMsg)}`));
    };
    _getTbBtnProps = () => {
        return [
            {
                name: '启动',
                key: 'start ',
                icon: <CaretRightFilled style={{color: 'green', fontSize: '18px'}}/>,
                onClick: this.startWebServer,
            },
            {
                name: '停止',
                key: 'stop',
                icon: <PoweroffOutlined style={{color: 'red', fontSize: '18px'}}/>,
                onClick: this.stopWebServer,
            },
            {
                name: '重启',
                key: 'reset',
                icon: <ReloadOutlined style={{color: '#1890ff', fontSize: '18px'}}/>,
                onClick: this.restartWebServer,
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
        SettingService.oneClickRestart();
    };
    oneClickStart = () => {
        this.initWebsocket();
        this._disableOnClickButton();
        SettingService.oneClickStart();
    };
    oneClickStop = () => {
        this.initWebsocket();
        this._disableOnClickButton();
        SettingService.oneClickStop();
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
    render() {
        let tableOption = this._getTbProps();
        tableOption.scroll = { y: this.height};
        let outTitle = (<><span>输出 - </span>
                <Select placeholder={"选择服务查看输出"} value={this.state.current}
                        style={{width: 300}}
                        onChange={value => this.setState({current: value})}>
                    {this.allServerOut.map(d => (<Select.Option key={d} value={d}>{d}</Select.Option>))}
                </Select></>);

        return (<div>
            <Space size={'middle'}>
                <Button type={'primary'} loading={this.state.oneClickLoading} onClick={this.oneClickRestart}>一键重启</Button>
                <Button loading={this.state.oneClickLoading} onClick={this.oneClickStart}>一键启动</Button>
                <Button loading={this.state.oneClickLoading} onClick={this.oneClickStop}>一键停止</Button>
            </Space>
            <div style={{display: 'flex'}}>
                <div style={{flex: 'inherit', width: '30%'}}>
                    <CommonTable bordered tableOption={tableOption} tableButtons={this._getTbBtnProps()} height={this.height}/>
                </div>
                <div style={{flex: 'auto'}}>
                    <Card title={outTitle} size={"small"}
                          extra={<Button type={"link"} onClick={() => this._clearDisplay(this.state.current)}>清空</Button>}>
                        <div className={styles.outPanel}>
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
