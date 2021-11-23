import React, {PureComponent} from 'react';
import {Table, Button} from 'antd';
import StringUtil from "../../common/StringUtil";
import styles from './index.less';
import {SettingOutlined} from '@ant-design/icons';

interface CommonTableProp {
    option: any,
    toolbar?: any[],
    showToolbarName?: boolean,
    height: number,
    showSelRowNum?: boolean,
    getNewColumn?: (item: any) => void,
    name?: string,
    style?: any,
}

const toolBarHeight = 32;
const paginationHeight = 64;
const columnHeight = 35;

export default class CommonTable extends PureComponent<CommonTableProp> {
    static defaultProps = {
        showSelRowNum: false,
        option: {},
        showToolbarName: false,
    };
    tableContentHeight = 0;
    currentTableClass = `table-${Date.now()}`;
    state = {showColumnSetting: false, columns: new Array<any>()};

    componentDidMount() {
        this._resetTableHeight();
        if (this.props?.showSelRowNum) {
            this.setState({columns: this._filterColumns(this.props.option.columns)});
        }
    }

    componentDidUpdate(prevProps: CommonTableProp, prevState: any, snapshot: any) {
        if (prevProps?.option?.columns?.length > 1 &&
            prevProps.option.columns[0]?.key !== this.state.columns[0]?.key) {
            this.setState({columns: prevProps.option.columns});
        }
        this._resetTableHeight();
    }

    _filterColumns(columns: any[]) {
        return columns.filter(column => column.visible === undefined || column.visible === true);
    }

    _resetTableHeight() {
        let {option} = this.props;
        let currentTableClass = this.currentTableClass;
        let tbColumnHeight = 35;
        let domTbContainer = document.querySelector(`.${currentTableClass} .ant-spin-container`);
        if (domTbContainer !== null) {
            let tableContentHeight = this.tableContentHeight;
            let tableBodyHeight = tableContentHeight - tbColumnHeight;
            let domTbPlaceholder: any = document.querySelector(`.${currentTableClass} .ant-table-placeholder`);
            if (domTbPlaceholder !== null) {
                if (!option.pagination || option.pagination === false) {
                    domTbPlaceholder.style.height = `${tableBodyHeight - tbColumnHeight}px`;
                } else {
                    domTbPlaceholder.style.height = `${tableBodyHeight}px`;
                }
            }

            let domTbBody: any = document.querySelector(`.${currentTableClass} .ant-table-body`);
            if (domTbBody != null) {
                domTbBody.style.height = `${tableBodyHeight}px`;
            }
        }
    }

    columnSettingToggle = (item: any) => {
        this.setState({showColumnSetting: item});
    };

    getNewColumn = (item: any) => {
        this.setState({columns: item});
        this.props?.getNewColumn && this.props.getNewColumn(item);
    };

    private getOptions() {
        let option = this.props.option;
        if (option?.rowSelection !== undefined) {
            option.rowSelection.columnWidth = 50;
        }
        let height: any = this.props.height;
        if (StringUtil.isNotEmpty(height)) {
            let tableContentHeight = height;
            if (undefined !== this.props.toolbar || this.props.showSelRowNum) {
                tableContentHeight = tableContentHeight - toolBarHeight
            }
            if (option.pagination !== undefined && option.pagination !== false) {
                tableContentHeight = tableContentHeight - paginationHeight;
            }
            option.scroll = {y: tableContentHeight - columnHeight};
            this.tableContentHeight = tableContentHeight;
        }
        return option;
    }

    render() {
        let option = this.getOptions();
        let hasButton = false;
        if ((this.props.toolbar && this.props.toolbar.length !== 0) || this.props.showSelRowNum) {
            hasButton = true;
        }
        let style = this.props.style;
        if (!style) {
            style = {position: 'relative'};
        }
        return (
            <div className={`${styles.commonTable} ${this.currentTableClass} `} style={style}>
                {hasButton && (
                    <div className={styles.toolBar} style={{height: `${toolBarHeight}px`}}>
                        {this.props.toolbar && this.props.toolbar.length > 0 && this.props.toolbar.map(element =>
                            <Button onClick={element.onClick} key={element.key} type={"text"}
                                    disabled={element.disabled}
                                    style={this.props.showToolbarName ? {} : {marginRight: 2}}
                                    icon={element.icon}
                                    title={element.title}>{this.props.showToolbarName && element.title}</Button>
                        )}
                        {
                            this.props.showSelRowNum &&
                                <button onClick={this.columnSettingToggle.bind(this, true)} key={'columnSetting'}
                                        style={{float: 'right'}}>
                                    <SettingOutlined/>
                                </button>
                        }
                    </div>
                )}
                <Table {...option} columns={this.state.columns}/>
            </div>
        );
    }
}
