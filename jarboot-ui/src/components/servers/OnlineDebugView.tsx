import CommonTable from "@/components/table";
import {Result} from "antd";
import {LoadingOutlined} from "@ant-design/icons";
import {formatMsg} from "@/common/IntlFormat";
import ServerMgrService, {JvmProcess} from "@/services/ServerMgrService";
import {SuperPanel} from "@/components/servers/SuperPanel";
import * as React from "react";
import {PUB_TOPIC, pubsub} from "@/components/servers/ServerPubsubImpl";
import CommonNotice from "@/common/CommonNotice";

export default class OnlineDebugView extends React.PureComponent {
    state = {loading: false, data: [] as JvmProcess[],
        selectedRowKeys: [] as number[], searchText: '', searchedColumn: '', filteredInfo: null as any,
        selectRows: [] as JvmProcess[]};
    height = window.innerHeight - 120;
    componentDidMount() {
        this.refreshProcessList(true);
        pubsub.submit('', PUB_TOPIC.RECONNECTED, this.refreshProcessList);
    }

    componentWillUnmount() {
        pubsub.unSubmit('', PUB_TOPIC.RECONNECTED, this.refreshProcessList);
    }

    private refreshProcessList = (init: boolean = false) => {
        this.setState({loading: true});
        ServerMgrService.getJvmProcesses((resp: any) => {
            this.setState({loading: false});
            if (resp.resultCode < 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            const data = resp.result as JvmProcess[];
            if (init) {
                //初始化选中第一个
                let selectedRowKeys = [] as number[];
                let selectRows: any = [];
                if (data.length > 0) {
                    const first: JvmProcess = data[0];
                    selectedRowKeys = [first.pid];
                    selectRows = [first];
                }
                this.setState({data, selectedRowKeys, selectRows});
                return;
            }
            this.setState({data});
        });
    };

    private _getTbProps() {
        return {
            columns: [
                {
                    title: 'PID',
                    dataIndex: 'pid',
                    key: 'pid',
                    ellipsis: true,
                    width: 80,
                },
                {
                    title: formatMsg('NAME'),
                    dataIndex: 'name',
                    key: 'name',
                    ellipsis: true,
                    sorter: (a: JvmProcess, b: JvmProcess) => a.name.localeCompare(b.name),
                    sortDirections: ['descend', 'ascend'],
                },
                {
                    title: 'Attached',
                    dataIndex: 'attached',
                    key: 'attached',
                    ellipsis: true,
                    width: 120,
                    render: (attached: boolean) => attached ? "Yes" : "No",
                },
            ],
            loading: this.state.loading,
            dataSource: this.state.data,
            pagination: false,
            rowKey: 'pid',
            size: 'small',
            rowSelection: this._getRowSelection(),
            onRow: this._onRow.bind(this),
            showHeader: true,
            scroll: this.height,
        };
    }

    private _getRowSelection() {
        return {
            type: 'radio',
            onChange: (selectedRowKeys: string[], selectedRows: JvmProcess[]) => {
                this.setState({selectedRowKeys: selectedRowKeys, selectRows: selectedRows,});
            },
            selectedRowKeys: this.state.selectedRowKeys,
        };
    }

    private _onRow(record: JvmProcess) {
        return {
            onClick: () => {
                this.setState({
                    selectedRowKeys: [record.pid],
                    selectRows: [record],
                });
            },
        };
    }

    render() {
        let tableOption: any = this._getTbProps();
        tableOption.scroll = { y: this.height};
        const loading = this.state.loading && 0 === this.state.data.length;
        return <div style={{display: 'flex'}}>
            <div style={{flex: 'inherit', width: '28%'}}>
                <CommonTable toolbarGap={5} option={tableOption}
                             height={this.height}/>
            </div>
            <div style={{flex: 'inherit', width: '72%'}}>
                {loading && <Result icon={<LoadingOutlined/>} title={formatMsg('LOADING')}/>}
                {this.state.data.map((value: JvmProcess) => (
                    <SuperPanel key={value.pid}
                                server={value.name}
                                sid={value.pid + ''}
                                visible={this.state.selectedRowKeys[0] === value.pid}/>
                ))}
            </div>
        </div>;
    }
};

