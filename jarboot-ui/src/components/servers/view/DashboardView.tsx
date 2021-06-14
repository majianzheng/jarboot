import React, {memo} from 'react';
// @ts-ignore
import CommonTable from "../../commonTable/CommonTable";
import {Progress, Row, Col, Descriptions} from "antd";
import StringUtil from "@/common/StringUtil";
import styles from "../index.less";

const progressValueFormat = (percent: number|undefined) => <span style={{color: percent && percent > 90 ? 'red' : 'green', fontSize: '8px'}}>{percent}%</span>;
const mu = 1024 * 1024;

const cpuColorFormat = (cpu: number) => {
    let color;
    if (cpu < 10) {
        color = 'green';
    } else if (cpu >= 10 && cpu <= 20) {
        color = 'magenta';
    } else {
        color = 'red';
    }
    return <span style={{color}}>{cpu}</span>;
};

const DashboardView = memo((props: any) => {
    const height = 320;

    const renderState = (state: any) => {
        if (StringUtil.isEmpty(state)) {
            return '-';
        }
        switch (state) {
            case 'NEW':
                state = <span style={{color: "cyan"}}>{state}</span>;
                break;
            case 'RUNNABLE':
                state = <span style={{color: "green"}}>{state}</span>;
                break;
            case 'BLOCKED':
                state = <span style={{color: "red"}}>{state}</span>;
                break;
            case 'WAITING':
                state = <span style={{color: "yellow"}}>{state}</span>;
                break;
            case 'TIMED_WAITING':
                state = <span style={{color: "magenta"}}>{state}</span>;
                break;
            case 'TERMINATED':
                state = <span style={{color: "blue"}}>{state}</span>;
                break;
            default:
                break;
        }
        return state;
    };
    const getThreadTableProps = () => {
        return {
            columns: [
                {
                    title: 'id',
                    dataIndex: 'id',
                    key: 'id',
                    width: 50,
                },
                {
                    title: 'NAME',
                    dataIndex: 'name',
                    key: 'name',
                    ellipsis: true,
                },
                {
                    title: 'GROUP',
                    dataIndex: 'group',
                    key: 'group',
                    ellipsis: true,
                    render: (text: string) => (StringUtil.isEmpty(text)) ? '-' : text,
                },
                {
                    title: 'PRIORITY',
                    dataIndex: 'priority',
                    key: 'priority',
                },
                {
                    title: 'STATE',
                    dataIndex: 'state',
                    key: 'state',
                    render: renderState,
                },
                {
                    title: '%CPU',
                    dataIndex: 'cpu',
                    key: 'cpu',
                    width: 50,
                    render: cpuColorFormat,
                },
                {
                    title: 'DELTA_TIME',
                    dataIndex: 'deltaTime',
                    key: 'deltaTime',
                },
                {
                    title: 'TIME',
                    dataIndex: 'time',
                    key: 'time',
                },
                {
                    title: 'INTERRUPTED',
                    dataIndex: 'interrupted',
                    key: 'interrupted',
                    render: (interrupted: boolean) => interrupted ? "true" : "false"
                },
                {
                    title: 'DAEMON',
                    dataIndex: 'daemon',
                    key: 'daemon',
                    render: (daemon: boolean) => daemon ? "true" : <span style={{color: 'magenta'}}>false</span>
                },
            ],
            loading: false,
            dataSource: props.data.threads,
            pagination: false,
            //rowKey: 'id',
            size: 'small',
            showHeader: true,
            scroll: height,
        };
    };

    const getMemoryDataSource = () => {
        let data = new Array<any>();
        const memoryInfo: any = props.data.memoryInfo;
        let index = 0;
        for (let key in memoryInfo) {
            if (memoryInfo.hasOwnProperty(key)) {
                const m = memoryInfo[key];
                console.log(key, m);
                if (!(m instanceof Array) || m.length <= 0) {
                    continue;
                }
                m.forEach(row => {
                    let item = {id: index++, ...row};
                    data.push(item);
                });
            }
        }
        const gcInfos: any = props.data.gcInfos;
        if (gcInfos instanceof Array && gcInfos.length > 0) {
            index = 0;
            gcInfos.forEach(row => {
                let d = data[index++];
                d['GC'] = `${row.name}.count`;
                d['GCInfo'] = row.collectionCount;
                d = data[index++];
                d['GC'] = `${row.name}.time(ms)`;
                d['GCInfo'] = row.collectionTime;
            })
        }
        return data;
    };

    const getMemoryTableProps = () => {
        return {
            columns: [
                {
                    title: 'NAME',
                    dataIndex: 'name',
                    key: 'name',
                    ellipsis: true,
                },
                {
                    title: 'used(M)',
                    dataIndex: 'used',
                    key: 'used',
                    ellipsis: true,
                    render: (item: any) => (item/mu).toFixed(2)
                },
                {
                    title: 'total(M)',
                    dataIndex: 'total',
                    key: 'total',
                    render: (item: any) => (item/mu).toFixed(2)
                },
                {
                    title: 'max(M)',
                    dataIndex: 'max',
                    key: 'max',
                    render: (item: any) => (item/mu).toFixed(2)
                },
                {
                    title: 'usage',
                    dataIndex: 'usage',
                    key: 'usage',
                    render: (item: any, record: any) => {
                        let percent = (record.used / record.total) * 100;
                        percent = parseFloat(percent.toFixed(2));
                        return <Progress size={"small"}
                                         format={progressValueFormat}
                                         percent={percent}/>
                    }
                },
                {
                    title: 'GC',
                    dataIndex: 'GC',
                    key: 'GC',
                },
                {
                    title: '',
                    dataIndex: 'GCInfo',
                    key: 'GCInfo',
                },
            ],
            loading: false,
            dataSource: getMemoryDataSource(),
            pagination: false,
            rowKey: 'id',
            size: 'small',
            showHeader: true,
            scroll: height,
        };
    };

    let thrTableOption: any = getThreadTableProps();
    thrTableOption.scroll = { y: height};
    let memTableOption: any = getMemoryTableProps();
    memTableOption.scroll = { y: height};
    const runtimeInfo = props?.data?.runtimeInfo;
    return <>
        <div className={styles.smallTable}>
            <CommonTable bordered tableOption={thrTableOption} height={height}/>
            <Row>
                <Col span={16}>
                    <CommonTable bordered tableOption={memTableOption} height={height}/>
                </Col>
                <Col span={8} style={{overflowY: "auto"}}>
                    <Descriptions size={'small'} column={1} bordered>
                        <Descriptions.Item label="os">{runtimeInfo?.osName} {runtimeInfo?.osVersion}</Descriptions.Item>
                        <Descriptions.Item label="average">{runtimeInfo?.systemLoadAverage}</Descriptions.Item>
                        <Descriptions.Item label="processors">{runtimeInfo?.processors}</Descriptions.Item>
                        <Descriptions.Item label="uptime">{runtimeInfo?.uptime}</Descriptions.Item>
                        <Descriptions.Item label="java.version">{runtimeInfo?.javaVersion}</Descriptions.Item>
                        <Descriptions.Item label="java.home" contentStyle={{textOverflow: "ellipsis"}}>{runtimeInfo?.javaHome}</Descriptions.Item>
                    </Descriptions>
                </Col>
            </Row>
        </div>
    </>
});

export default DashboardView;
