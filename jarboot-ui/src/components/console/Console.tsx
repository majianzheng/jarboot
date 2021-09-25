import React from 'react';
import styles from './index.less';
import StringUtil from "@/common/StringUtil";
import Logger from "@/common/Logger";
import {JarBootConst} from "@/common/JarBootConst";

interface ConsoleProps {
    visible?: boolean;
    content?: string;
    pubsub?: PublishSubmit;
    server: string;
}
//最大行数
const MAX_LINE = 16384;
//超出上限则移除最老的行数
const AUTO_CLEAN_LINE = 12000;
//渲染更新延迟
const MAX_UPDATE_DELAY = 128;
const MAX_FINISHED_DELAY = MAX_UPDATE_DELAY * 2;

class Console extends React.PureComponent<ConsoleProps> {
    private codeDom: HTMLElement|null = null;
    private loading = document.createElement('p');
    private isStartLoading = false;
    private waitToAppend = [] as string[];
    private updateTimeoutFd: NodeJS.Timeout|null = null;
    private finishTimeoutFd: NodeJS.Timeout|null = null;

    componentDidMount() {
        this.updateTimeoutFd = null;
        this.finishTimeoutFd = null;
        this.waitToAppend = [];
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
        const {pubsub, server} = this.props;
        pubsub?.submit(server, JarBootConst.APPEND_LINE, this.appendLine);
        pubsub?.submit(server, JarBootConst.BACKSPACE_LINE, this.backspaceLine);
        pubsub?.submit(server, JarBootConst.INSERT_TO_HEADER, this.insertLineToHeader);
        pubsub?.submit(server, JarBootConst.START_LOADING, this.startLoading);
        pubsub?.submit(server, JarBootConst.FINISH_LOADING, this.finishLoading);
        pubsub?.submit(server, JarBootConst.CLEAR_CONSOLE, this.clear);
        if (StringUtil.isNotEmpty(this.props.content)) {
            this._resetContent(this.props.content);
        }
    }

    componentWillUnmount() {
        const {pubsub, server} = this.props;
        pubsub?.unSubmit(server, JarBootConst.APPEND_LINE, this.appendLine);
        pubsub?.unSubmit(server, JarBootConst.BACKSPACE_LINE, this.backspaceLine);
        pubsub?.unSubmit(server, JarBootConst.INSERT_TO_HEADER, this.insertLineToHeader);
        pubsub?.unSubmit(server, JarBootConst.START_LOADING, this.startLoading);
        pubsub?.unSubmit(server, JarBootConst.FINISH_LOADING, this.finishLoading);
        pubsub?.unSubmit(server, JarBootConst.CLEAR_CONSOLE, this.clear);
        this.updateTimeoutFd = null;
        this.codeDom = null;
    }

    private init = () => {
        if (this.codeDom) {
            const count = this.codeDom.children.length;
            if (count > MAX_LINE) {
                //如果超过最大行数则移除最老的行
                for (let i = 0; i < AUTO_CLEAN_LINE; ++i) {
                    this.codeDom.removeChild(this.codeDom.children[0]);
                }
            }
            return;
        }
        this.codeDom = document.querySelector(`#id-console-${this.props.server}`);
    };
    private _resetContent = (text: string|undefined) => {
        if (null == text) {
            return;
        }
        this.init();
        let count = this.codeDom?.childNodes.length;
        if (count) {
            for (let i = 0; i < count; ++i) {
                this.codeDom?.removeChild(this.codeDom.childNodes[0]);
            }
        }
        this.codeDom?.append(text);
    };
    private clear = () => {
        this.init();
        if (!this.codeDom) {
            return;
        }
        let count = this.codeDom.children.length;
        if (count > 0 && this.loading == this.codeDom.children[count - 1]) {
            //如果处于加载中，则保留加载的动画
            --count;
        }
        for(let i = 0; i < count; ++i){
            this.codeDom.removeChild(this.codeDom.children[0]);
        }
    };
    private startLoading = () => {
        if (!this.isStartLoading) {
            try {
                this.codeDom?.append(this.loading);
                this.isStartLoading = true;
            } catch (e) {
                Logger.error(e);
            }
        }
    };
    private finishLoading = (str?: string) => {
        this.init();
        this.appendLine(str);
        if (this.finishTimeoutFd) {
            // 以最后一次生效，当前若存在则取消，重新计时
            clearTimeout(this.finishTimeoutFd);
        }
        //延迟异步，停止转圈
        this.finishTimeoutFd = setTimeout(() => {
            this.finishTimeoutFd = null;
            try {
                this.codeDom?.removeChild(this.loading);
            } catch (error) {
                //ignore
            }
            this.isStartLoading = false;
        }, MAX_FINISHED_DELAY);

    };
    private appendLine = (line: string | undefined) => {
        if (typeof line !== 'string') {
            return;
        }
        this.waitToAppend.push(line);
        //异步延迟MAX_UPDATE_DELAY毫秒，统一插入
        if (!this.updateTimeoutFd) {
            this.init();
            this.updateTimeoutFd = setTimeout(() => {
                this.updateTimeoutFd = null;
                if (!this.isStartLoading) {
                    this.startLoading()
                }
                //使用虚拟节点将MAX_UPDATE_DELAY时间内的所有更新一块append渲染，减轻浏览器负担
                const fragment = document.createDocumentFragment();
                try {
                    this.waitToAppend.forEach(l => fragment.append(this._parseLine(l)));
                    this.loading.before(fragment);
                    if (this.codeDom) {
                        this.codeDom.scrollTop = this.codeDom.scrollHeight;
                    }
                } catch (e) {
                    Logger.error(e);
                } finally {
                    this.waitToAppend = [];
                }
            }, MAX_UPDATE_DELAY);
        }
    };

    private insertLineToHeader = (line: string) => {
        this.init();
        if (!this.isStartLoading) {
            this.startLoading();
        }
        this.loading.after(this._parseLine(line));
    };

    private backspaceLine = (line?: string) => {
        if (!this.codeDom || !this.codeDom.children.length) {
            return;
        }
        const len = this.codeDom.children.length;
        let last = this.isStartLoading ? this.codeDom.children[len - 2] : this.codeDom.children[len - 1];
        if (last) {
            if (line) {
                last.innerHTML = line;
            } else {
                this.codeDom.removeChild(last);
            }
        }
    };

    private _parseLine = (line: string) => {
        if (0 === line?.length) {
            return document.createElement('br');
        }
        let p = document.createElement('p');
        line = line.replace(/ERROR/g, `<span class="error-log">ERROR</span>`).
        replace(/INFO/g, `<span class="info-log">INFO</span>`);
        if (line.includes('WARN')) {
            line = line.replace(/WARN/g, `<span class="warn-log">WARN</span>`);
            p.className = styles.waring;
        }
        p.innerHTML = line;
        return p;
    };

    render() {
        let style = {display: false === this.props.visible ? 'none' : 'block'};
        return (<>
                <code id={`id-console-${this.props.server}`} style={style} className={styles.console}>
                    <p style={{fontSize: 28, textAlign: "center", color: "blueviolet"}}>Jarboot Console</p>
                </code>
            </>
        );
    }
}
export default Console;
