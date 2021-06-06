import * as React from "react";
import { Col, Row, Menu } from 'antd';
import ServerConfig from "@/components/setting/ServerConfig";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from '../../common/ErrorUtil';
import styles from './index.less';
import ServerMgrService from "@/services/ServerMgrService";
import Logger from "@/common/Logger";
import {memo} from "react";
import { useIntl } from 'umi';

const ServerListHeader: any = memo(() => {
    const intl = useIntl();
    return <div>{intl.formatMessage({id: 'SERVER_LIST_TITLE'})}</div>;
});

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
            this.setState({data: resp.result});
        }, (errorMsg: any) => Logger.warn(`${ErrorUtil.formatErrResp(errorMsg)}`));
    };
    private _onSelect = (event: any) => {
        const current = event.key;
        this.setState({current});
    };
    render() {
        return <Row>
            <Col span={6} className={styles.pageContainer}>
                <Menu
                    onClick={this._onSelect}
                    defaultSelectedKeys={[this.state.data[0]]}
                    mode="inline"
                >
                    <Menu.ItemGroup title={<ServerListHeader/>}>
                        <Menu.Divider/>
                        {this.state.data.map((item: any) => {
                            return <Menu.Item key={item.name}>{item.name}</Menu.Item>
                        })}
                    </Menu.ItemGroup>
                </Menu>
            </Col>
            <Col span={18} className={styles.pageContainer}>
                <div style={{margin: '0 30px 0 5px', width: '80%'}}>
                    <ServerConfig server={this.state.current}/>
                </div>
            </Col>
        </Row>
    }
}
