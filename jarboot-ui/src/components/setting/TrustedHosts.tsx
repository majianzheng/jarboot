import React, {useEffect, useState} from "react";
import { List, Button, Modal, Input, Popconfirm } from 'antd';
import {PlusSquareOutlined, SyncOutlined} from "@ant-design/icons";
import SettingService from "@/services/SettingService";
import CommonNotice from "@/common/CommonNotice";
import {useIntl} from "umi";
import {DeleteIcon} from "@/components/icons";
import styles from "@/common/global.less";

const TrustedHosts = () => {
    const intl = useIntl();
    const [loading, setLoading] = useState(true);
    const [hosts, setHosts] = useState([] as string[]);
    const query = () => {
        setLoading(true);
        SettingService.getTrustedHosts()
            .then(resp => {
            if (0 !== resp.resultCode) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            setHosts(resp.result);
        })
            .catch(CommonNotice.errorFormatted)
            .finally(() => setLoading(false));
    };

    useEffect(query, []);

    const onAdd = () => {
        let host = '';
        const content = <Input onChange={value => (host = value.target.value)}/>;
        Modal.confirm({
            title: intl.formatMessage({id: 'CREATE'}),
            content,
            icon: <PlusSquareOutlined/>,
            onOk: () => {
                console.info(host);
                if (host.length) {
                    return SettingService
                        .addTrustedHost(host)
                        .then(resp => {
                            if (0 === resp.resultCode) {
                                query();
                            } else {
                                CommonNotice.errorFormatted(resp);
                                return Promise.reject(resp.resultMsg);
                            }
                        })
                        .catch(CommonNotice.errorFormatted);
                }
                CommonNotice.error(intl.formatMessage({id: 'EMPTY_INPUT_MSG'}));
                return Promise.reject("Host is empty");
            },
        });
    };

    const onRemove = (host: string) => {
        SettingService
            .removeTrustedHost(host)
            .then(resp => {
                if (0 === resp.resultCode) {
                    query();
                } else {
                    CommonNotice.errorFormatted(resp);
                }
            })
            .catch(CommonNotice.errorFormatted);
    };

    const getHeader = () => (<>
        <Button type={"text"} onClick={onAdd}
                icon={<PlusSquareOutlined className={styles.toolButtonIcon}/>}>
            {intl.formatMessage({id: 'CREATE'})}
        </Button>
        <Button type={"text"} onClick={query}
                icon={<SyncOutlined className={styles.toolButtonIcon}/>}>
            {intl.formatMessage({id: 'REFRESH_BTN'})}
        </Button>
    </>);

    return (<div style={{margin: '5px 50px 0 30px'}}>
        <List
        loading={loading}
        header={getHeader()}
        itemLayout="horizontal"
        dataSource={hosts}
        renderItem={item => (
            <List.Item actions={[
                <Popconfirm title={intl.formatMessage({id: 'DELETE_HOST_MSG'})}
                            icon={<DeleteIcon className={styles.toolButtonRedIcon}/>}
                            onConfirm={() => onRemove(item)}>
                    <Button type={"link"}
                            icon={<DeleteIcon className={styles.toolButtonRedIcon}/>}
                            key={`del-key-${item}`}>
                        {intl.formatMessage({id: 'DELETE'})}
                    </Button>
                </Popconfirm>]}>
                {item}
            </List.Item>
        )}
    />
    </div>);
};

export default TrustedHosts;
