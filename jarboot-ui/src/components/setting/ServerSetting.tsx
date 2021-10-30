import { Col, Row, Menu, Empty, Result, Button } from 'antd';
import ServerConfig from "@/components/setting/ServerConfig";
import CommonNotice from "@/common/CommonNotice";
import styles from './index.less';
import ServerMgrService, {ServerRunning} from "@/services/ServerMgrService";
import {memo, useEffect, useState} from "react";
import { useIntl } from 'umi';
import {LoadingOutlined, SyncOutlined} from '@ant-design/icons';

const ServerSetting = memo(() => {
    const intl = useIntl();
    const [data, setData] = useState([] as ServerRunning[]);
    const [current, setCurrent] = useState('');
    const [loading, setLoading] = useState(true);

    const query = () => {
        setLoading(true);
        ServerMgrService.getServerList((resp: any) => {
            setLoading(false);
            if (resp.resultCode < 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            const result = resp.result as ServerRunning[];
            setData(result);
            if (result.length > 0) {
                setCurrent(result[0].name);
            }
        });
    };

    useEffect(query, []);

    const onSelect = (event: any) => {
        setCurrent(event.key);
    };
    if (loading) {
        return <Result icon={<LoadingOutlined/>} title={intl.formatMessage({id: 'LOADING'})}/>;
    }
    return <>{(data?.length > 0) ? <Row>
        <Col span={6} className={styles.pageContainer}>
            <Menu
                onClick={onSelect}
                selectedKeys={[current]}
                mode="inline"
            >
                <Menu.ItemGroup title={<span>
                    <span>{intl.formatMessage({id: 'SERVER_LIST_TITLE'})}</span>
                    <Button type={"link"} onClick={query} style={{marginLeft: "50px"}}
                            icon={<SyncOutlined style={{color: 'green'}}/>}>
                        {intl.formatMessage({id: 'REFRESH_BTN'})}
                    </Button>
                </span>}>
                    <Menu.Divider/>
                    {data.map((item) => {
                        return <Menu.Item key={item.path}>{item.name}</Menu.Item>
                    })}
                </Menu.ItemGroup>
            </Menu>
        </Col>
        <Col span={18} className={styles.pageContainer}>
            <div style={{margin: '0 30px 0 5px', width: '95%'}}>
                <ServerConfig path={current}/>
            </div>
        </Col>
    </Row> : <Result icon={<Empty/>}
                     title={intl.formatMessage({id: 'SERVER_EMPTY'})}
                     extra={<Button type="primary" onClick={query}>
                         {intl.formatMessage({id: 'REFRESH_BTN'})}
                     </Button>}
    />}</>
});
export default ServerSetting;
