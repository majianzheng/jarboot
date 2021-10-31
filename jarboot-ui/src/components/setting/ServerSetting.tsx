import { Col, Row, Menu, Input, Empty, Result, Button } from 'antd';
import ServerConfig from "@/components/setting/ServerConfig";
import CommonNotice from "@/common/CommonNotice";
import styles from './index.less';
import ServerMgrService, {ServerRunning} from "@/services/ServerMgrService";
import {memo, useEffect, useState} from "react";
import { useIntl } from 'umi';
import {LoadingOutlined} from '@ant-design/icons';
import StringUtil from "@/common/StringUtil";

const ServerSetting = memo(() => {
    const intl = useIntl();
    const [data, setData] = useState([] as ServerRunning[]);
    const [current, setCurrent] = useState('');
    const [loading, setLoading] = useState(true);

    const query = (filter?: string) => {
        setLoading(true);
        ServerMgrService.getServerList((resp: any) => {
            setLoading(false);
            if (resp.resultCode < 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            let result = resp.result as ServerRunning[];
            if (StringUtil.isNotEmpty(filter)) {
                result = result.filter(s => s.name.includes(filter as string))
            }
            setData(result);
            if (result.length > 0) {
                setCurrent(result[0].path);
            }
        });
    };

    useEffect(query, []);

    const onSelect = (event: any) => {
        setCurrent(event.key);
    };

    const emptyIcon = loading ? <LoadingOutlined/> : <Empty/>;
    const menuTitle = <Input.Search placeholder="input name to search" onSearch={query} allowClear enterButton/>;
    return <Row>
        <Col span={6} className={styles.pageContainer}>
            <Menu
                onClick={onSelect}
                selectedKeys={[current]}
                mode="inline"
            >
                <Menu.ItemGroup title={menuTitle}>
                    <Menu.Divider/>
                    {data.length ? data.map((item) => {
                        return <Menu.Item key={item.path}>{item.name}</Menu.Item>
                    }) : <Empty/>}
                </Menu.ItemGroup>
            </Menu>
        </Col>
        <Col span={18} className={styles.pageContainer}>
            <div style={{margin: '0 30px 0 5px', width: '95%'}}>
                {(data?.length > 0) ?
                    <ServerConfig path={current}/> :
                    <Result icon={emptyIcon}
                            title={intl.formatMessage({id: 'SERVER_EMPTY'})}
                            extra={<Button type="primary"
                                           onClick={() => query()}>{intl.formatMessage({id: 'REFRESH_BTN'})}</Button>}/>
                }
            </div>
        </Col>
    </Row>
});
export default ServerSetting;
