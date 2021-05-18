import React from 'react';
import styles from './index.less';
import PropTypes from "prop-types";
import StringUtil from "../../common/StringUtil";

export default class Console extends React.Component {
    static defaultProps = {
        visible: true,
        content: "",
    };
    static propTypes = {
        height: PropTypes.number,
        method: PropTypes.object,
        visible: PropTypes.bool,
        content: PropTypes.string,
    };
    content = React.createRef();
    loading = document.createElement('p');
    isStartLoading = false;
    componentDidMount() {
        //初始化loading
        let three1 = document.createElement('div');
        let three2 = document.createElement('div');
        let three3 = document.createElement('div');
        three1.className= styles.three1;
        three2.className= styles.three2;
        three3.className= 'three3';
        this.loading.append(three1);
        this.loading.append(three2);
        this.loading.append(three3);
        this.loading.className = styles.loading;
        if (this.props.method) {
            this.props.method.appendLine = this.appendLine;
            this.props.method.insertLineToHeader = this.insertLineToHeader;
            this.props.method.startLoading = this.startLoading;
            this.props.method.finishLoading = this.finishLoading;
            this.props.method.clear = this.clear;
        }
        if (StringUtil.isNotEmpty(this.props.content)) {
            this._resetContent(this.props.content);
        }
    }
    shouldComponentUpdate(nextProps, nextState, nextContext) {
        if (nextProps.method) {
            nextProps.method.appendLine = this.appendLine;
            nextProps.method.insertLineToHeader = this.insertLineToHeader;
            nextProps.method.startLoading = this.startLoading;
            nextProps.method.finishLoading = this.finishLoading;
            nextProps.method.clear = this.clear;
        }
        if (StringUtil.isNotEmpty(nextProps.content)) {
            this._resetContent(nextProps.content);
        }
        return true;
    }
    _resetContent(text) {
        let count = this.content.current.childNodes.length;
        if (count) {
            for (let i = 0; i < count; ++i) {
                this.content.current.removeChild(this.content.current.childNodes[0]);
            }
        }
        this.content.current.append(text);
    }
    clear = () => {
        this.content.current.innerHtml = '';
        let count = this.content.current.children.length;
        for(let i = 0; i < count; ++i){
            this.content.current.removeChild(this.content.current.children[0]);
        }
    };
    startLoading = () => {
        this._initLoading();
    };
    finishLoading = () => {
        try {
            this.content.current.removeChild(this.loading);
        } catch (error) {
            //ignore
        }
        this.isStartLoading = false;
    };
    appendLine = line => {
        if (!this.isStartLoading) {
            this.startLoading();
        }
        if (!this.content.current.hasChildNodes(this.loading)) {
            this.startLoading();
        }
        try {
            let p = this._parseLine(line);
            this.loading.before(p);
            this.content.current.scrollTop = this.content.current.scrollHeight;
        } catch (e) {
            //ignore
        }

    };

    insertLineToHeader = line => {
        if (!this.isStartLoading) {
            this.startLoading();
        }
        if (!this.content.current.hasChildNodes(this.loading)) {
            this.startLoading();
        }
        this.loading.after(this._parseLine(line));
    };

    _initLoading() {
        if (!this.isStartLoading) {
            try {
                this.content.current.append(this.loading);
                this.isStartLoading = true;
            } catch (e) {
                //ignore
            }
        }
    }
    _parseLine(line) {
        let p = document.createElement('p');
        line = line.replace(/ERROR/g, `<span class="error-log">ERROR</span>`).
        replace(/INFO/g, `<span class="info-log">INFO</span>`);
        if (line.includes('WARN')) {
            line = line.replace(/WARN/g, `<span class="warn-log">WARN</span>`);
            p.className = styles.waring;
        }
        p.innerHTML = line;
        //TODO 如果含有异常则整行标记颜色
        return p;
    }
    render() {
        let style = {display: this.props.visible ? 'block' : 'none'};
        return (<>
                <code style={style} className={styles.console} ref={this.content}>

                </code></>
        );
    }
}
