import React, {PureComponent} from 'react';
import {Table, Button} from 'antd';
import StringUtil from "../../common/StringUtil";
import PropTypes from 'prop-types';
import styles from './commonTable.less';
import {SettingOutlined} from '@ant-design/icons';

export default class CommonTable extends PureComponent {
    static defaultProps = {
        showSelRowNum: false
    };

    static propTypes = {
        tableOption: PropTypes.object,
        tableButtons: PropTypes.array,
        height: PropTypes.number,
        showSelRowNum: PropTypes.bool,
        getNewColumn: PropTypes.func,
        name: PropTypes.string
    };


    constructor(props) {
        super(props);
        this.state = {
            showColumnSetting: false,
            columns: this.props.tableOption.columns
        };
        this.tableContentHeight = 0;
        this.currentTableClass = `pharmaTable${new Date().getTime()}`;
    }

    componentDidMount() {
        this._resetTableHeight();
        if (this.props.showSelRowNum) {
            this.setState({
                columns: this._filterColumns(this.props.tableOption.columns)
            });
        }
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        if (prevProps.tableOption.columns.length > 1 && prevProps.tableOption.columns[0].key !== this.state.columns[0].key) {
            this.setState({
                columns: prevProps.tableOption.columns
            });
        }
        this._resetTableHeight();
    }

    _filterColumns(columns) {
        return columns.filter(column => {
            return column.visible === undefined || columns.visible === true;
        });
    }

    _resetTableHeight() {
        let {tableOption} = this.props;
        let currentTableClass = this.currentTableClass;
        let tbColumnHeight = 35;
        let domTbContainer = document.querySelector(`.${currentTableClass} .ant-spin-container`);
        if (domTbContainer !== null) {
            let tableContentHeight = this.tableContentHeight;
            let tableBodyHeight = tableContentHeight - tbColumnHeight;
            let domTbPlaceholder = document.querySelector(
                `.${currentTableClass} .ant-table-placeholder`,
            );
            if (domTbPlaceholder !== null) {
                if (!tableOption.pagination || tableOption.pagination === false) {
                    domTbPlaceholder.style.height = `${tableBodyHeight - tbColumnHeight}px`;
                } else {
                    domTbPlaceholder.style.height = `${tableBodyHeight}px`; //表格暂无数据区域
                }
            }

            // domTbContainer.style.height = `${tableContentHeight}px`;
            let domTbBody = document.querySelector(`.${currentTableClass} .ant-table-body`); //表格内容主体
            if (domTbBody != null) {
                domTbBody.style.height = `${tableBodyHeight}px`; //表格内容主体body
            }
        }
    }

    columnSettingToggle = (item) => {
        this.setState({showColumnSetting: item});
    };

    getNewColumn = (item) => {
        this.setState({
            columns: item
        });
        let getNewColumn = this.props.getNewColumn;
        if (getNewColumn) {
            getNewColumn(item);
        }
    };

    render() {
        let toolBarHeight = 32;
        let paginationHeight = 64;
        let columnHeight = 35;
        let tableOption = this.props.tableOption;
        if (tableOption.rowSelection !== undefined) {
            tableOption.rowSelection.columnWidth = 50;
        }
        let height = this.props.height;
        if (StringUtil.isNotEmpty(height)) {
            let tableContentHeight = height;
            if (undefined !== this.props.tableButtons || this.props.showSelRowNum) {
                tableContentHeight = tableContentHeight - toolBarHeight
            }
            if (tableOption.pagination !== undefined && tableOption.pagination !== false) {
                tableContentHeight = tableContentHeight - paginationHeight;
            }
            tableOption.scroll = {y: tableContentHeight - columnHeight};
            this.tableContentHeight = tableContentHeight;
        }
        let hasButton = false;
        if ((this.props.tableButtons && this.props.tableButtons.length !== 0) || this.props.showSelRowNum) {
            hasButton = true;
        }
        let style = this.props.style;
        if (!style) {
            style = {position: 'relative'};
        }
        return (
            <div className={`${styles.commonTable} ${this.currentTableClass} `} style={style}>
                {hasButton ? (
                    <div className="toolBar" style={{height: `${toolBarHeight}px`, background: '#f5f5f5'}}>
                        {this.props.tableButtons && this.props.tableButtons.length !== 0 ? this.props.tableButtons.map(element => {
                            if (element.custom === true) {
                                return element.customUI;
                            } else {
                                return (
                                    <Button onClick={element.onClick} key={element.key} type={"text"}
                                            disabled={element.disabled}
                                            style={{marginRight: '5px', marginLeft: '5px'}}
                                            icon={element.icon}
                                            title={element.name}/>
                                );
                            }
                        }) : null}
                        {
                            this.props.showSelRowNum ?
                                <button onClick={this.columnSettingToggle.bind(this, true)} key={'columnSetting'}
                                        style={{float: 'right'}}>
                                    <SettingOutlined/>
                                </button> : null
                        }
                    </div>
                ) : null}
                <Table bordered {...tableOption} columns={this.state.columns}/>
            </div>
        );
    }
}
