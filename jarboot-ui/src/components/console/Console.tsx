import React from 'react';
import styles from './index.less';
import StringUtil from "@/common/StringUtil";
import Logger from "@/common/Logger";
import {JarBootConst} from "@/common/JarBootConst";

interface ConsoleProps {
    visible?: boolean;
    content?: string;
    pubsub?: PublishSubmit;
    id: string;
    height?: string | number;
}

enum EventType {
    LINE,
    PRINT,
    BACKSPACE,
    CLEAR
}

interface ConsoleEvent {
    type: EventType,
    text?: string,
    backspaceNum?: number,
    deleted?: boolean,
}

//最大行数
const MAX_LINE = 16384;
//超出上限则移除最老的行数
const AUTO_CLEAN_LINE = 12000;
//渲染更新延迟
const MAX_UPDATE_DELAY = 128;
const MAX_FINISHED_DELAY = MAX_UPDATE_DELAY * 2;
const LINE_CUR_ATTR = 'line-cur';

class Console extends React.PureComponent<ConsoleProps> {
    private codeDom: HTMLElement|null = null;
    private loading = document.createElement('p');
    private isStartLoading = false;
    private eventQueue = [] as ConsoleEvent[];
    private lines = [] as HTMLElement[];
    private updateTimeoutFd: NodeJS.Timeout|null = null;
    private finishTimeoutFd: NodeJS.Timeout|null = null;

    componentDidMount() {
        this.updateTimeoutFd = null;
        this.finishTimeoutFd = null;
        this.eventQueue = [];
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
        if (StringUtil.isNotEmpty(this.props.content)) {
            this._resetContent(this.props.content);
        }
        const {pubsub, id} = this.props;
        if (!pubsub) {
            return;
        }
        pubsub.submit(id, JarBootConst.APPEND_LINE, this.appendLine);
        pubsub.submit(id, JarBootConst.PRINT, this.print);
        pubsub.submit(id, JarBootConst.BACKSPACE, this.backspace);
        pubsub.submit(id, JarBootConst.BACKSPACE_LINE, this.backspaceLine);
        pubsub.submit(id, JarBootConst.START_LOADING, this.startLoading);
        pubsub.submit(id, JarBootConst.FINISH_LOADING, this.finishLoading);
        pubsub.submit(id, JarBootConst.CLEAR_CONSOLE, this.clear);
    }

    componentWillUnmount() {
        this.updateTimeoutFd = null;
        this.codeDom = null;
        const {pubsub, id} = this.props;
        if (!pubsub) {
            return;
        }
        pubsub.unSubmit(id, JarBootConst.APPEND_LINE, this.appendLine);
        pubsub.unSubmit(id, JarBootConst.PRINT, this.print);
        pubsub.unSubmit(id, JarBootConst.BACKSPACE, this.backspace);
        pubsub.unSubmit(id, JarBootConst.BACKSPACE_LINE, this.backspaceLine);
        pubsub.unSubmit(id, JarBootConst.START_LOADING, this.startLoading);
        pubsub.unSubmit(id, JarBootConst.FINISH_LOADING, this.finishLoading);
        pubsub.unSubmit(id, JarBootConst.CLEAR_CONSOLE, this.clear);
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
        this.codeDom = document.querySelector(`code[id="id-console-${this.props.id}"]`);
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
        this.eventQueue.push({type: EventType.CLEAR});
        //异步延迟MAX_UPDATE_DELAY毫秒，统一插入
        this.trigEvent();
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

    private print = (text: string | undefined) => {
        this.eventQueue.push({type: EventType.PRINT, text});
        this.trigEvent();
    }

    private appendLine = (line: string | undefined) => {
        if (typeof line !== 'string') {
            return;
        }
        this.eventQueue.push({type: EventType.LINE, text: line,});
        //异步延迟MAX_UPDATE_DELAY毫秒，统一插入
        this.trigEvent();
    };

    private trigEvent() {
        if (this.updateTimeoutFd) {
            //已经触发
            return;
        }
        this.init();
        this.updateTimeoutFd = setTimeout(() => {
            this.updateTimeoutFd = null;
            this.eventQueue.forEach(this.handleEvent);
            try {
                if (this.lines.length) {
                    if (!this.isStartLoading) {
                        this.startLoading()
                    }
                    //使用虚拟节点将MAX_UPDATE_DELAY时间内的所有更新一块append渲染，减轻浏览器负担
                    const fragment = document.createDocumentFragment();
                    this.lines.forEach(l => fragment.append(l));
                    this.loading.before(fragment);
                    if (this.codeDom) {
                        this.codeDom.scrollTop = this.codeDom.scrollHeight;
                    }
                }
            } catch (e) {
                Logger.error(e);
            } finally {
                this.eventQueue = [];
                this.lines = [];
            }
        }, MAX_UPDATE_DELAY);
    }

    private handleEvent = (event: ConsoleEvent) => {
        if (event.deleted) {
            return;
        }
        switch (event.type) {
            case EventType.LINE:
                this.handlePrintln(event);
                break;
            case EventType.PRINT:
                this.handlePrint(event);
                break;
            case EventType.BACKSPACE:
                this.handleBackspace(event);
                break;
            case EventType.CLEAR:
                this.handleClear();
                break;
            default:
                break;
        }
    };

    private handleClear() {
        if (!this.codeDom) {
            return;
        }
        if (this.isStartLoading) {
            //如果处于加载中，则保留加载的动画
            this.codeDom.innerHTML = "";
            this.codeDom.append(this.loading);
        } else {
            this.codeDom.innerHTML = "";
        }
    }

    private handlePrintln(event: ConsoleEvent) {
        if (this.lines.length > 0) {
            let preLine = this.lines[this.lines.length - 1];
            if (preLine.hasAttribute(LINE_CUR_ATTR)) {
                //行未结束，将当前行附加到上一行
                preLine.innerHTML += event.text;
                preLine.removeAttribute(LINE_CUR_ATTR);
                event.deleted = true;
                return;
            }
        }
        const line = this._parseLine(event.text as string);
        this.lines.push(line);
    }

    private handlePrint(event: ConsoleEvent) {
        if (this.lines.length > 0) {
            let preLine = this.lines[this.lines.length - 1];
            if (preLine.hasAttribute(LINE_CUR_ATTR)) {
                //行未结束，将当前行附加到上一行
                preLine.innerHTML += event.text;
                event.deleted = true;
            } else {
                const line = this._parseLine(event.text as string);
                line.setAttribute(LINE_CUR_ATTR, 'true');
                this.lines.push(line);
            }
            return;
        }

        let last = this.getLastLine();
        if (last && event.text) {
            if ('BR' === last.tagName) {
                last.replaceWith(document.createElement('p'));
            }
            if (last.hasAttribute(LINE_CUR_ATTR)) {
                last.insertAdjacentHTML("beforeend", event.text);
            } else {
                const line = document.createElement('p');
                line.innerHTML = event.text;
                last.setAttribute(LINE_CUR_ATTR, 'true');
                last.after(line);
            }
        } else {
            const line = document.createElement('p');
            line.innerHTML = event.text as string;
            line.setAttribute(LINE_CUR_ATTR, 'true');
            this.loading.before(line);
        }
    }

    private handleBackspace(event: ConsoleEvent) {
        let backspaceNum = event.backspaceNum as number;
        while (this.lines.length > 0 && backspaceNum > 0) {
            let pre = this.lines[this.lines.length - 1];
            const len = pre.innerHTML.length - backspaceNum;
            if (len > 0) {
                pre.innerHTML = pre.innerHTML.substring(0, len);
                backspaceNum = 0;
            } else if (0 === len) {
                pre.innerHTML = '';
                backspaceNum = 0;
            } else {
                this.lines.pop();
                backspaceNum = Math.abs(len + 1);
            }
        }

        let last = this.getLastLine();
        if (!last) {
            return;
        }
        let i = last.innerHTML.length;
        while (last && backspaceNum > 0) {
            i = last.innerHTML.length - backspaceNum;
            last.setAttribute(LINE_CUR_ATTR, 'true');
            if (i > 0) {
                last.innerHTML = last.innerHTML.substring(0, i);
                backspaceNum = 0;
            } else if (i === 0) {
                last.innerHTML = '';
                backspaceNum = 0;
            } else {
                this.codeDom?.removeChild(last);
                last = this.getLastLine();
                backspaceNum = Math.abs(i + 1);
            }
        }
    }

    private backspace = (num: string) => {
        let backspaceNum = parseInt(num);
        if (!Number.isInteger(backspaceNum)) {
            return;
        }
        this.eventQueue.push({type: EventType.BACKSPACE, backspaceNum});
    };

    private backspaceLine = (line?: string) => {
        let last = this.getLastLine();
        if (last) {
            if (line) {
                last.innerHTML = line;
            } else {
                this.codeDom?.removeChild(last);
            }
        }
    };

    private getLastLine() {
        if (!this.codeDom || !this.codeDom?.children?.length) {
            return null;
        }
        const len = this.codeDom.children.length;
        return this.isStartLoading ? this.codeDom.children[len - 2] : this.codeDom.children[len - 1];
    }

    private _parseLine = (line: string) => {
        if (0 === line?.length) {
            return document.createElement('br');
        }
        let p = document.createElement('p');
        line = line.replace(/ERROR/g, `<span class="${styles.errorLog}">ERROR</span>`).
        replace(/INFO/g, `<span class="${styles.infoLog}">INFO</span>`);
        if (line.includes('WARN')) {
            line = line.replace(/WARN/g, `<span class="${styles.warnLog}">WARN</span>`);
            p.className = styles.waring;
        }
        p.innerHTML = line;
        return p;
    };

    render() {
        const style: any = {display: false === this.props.visible ? 'none' : 'block'};
        if (this.props.height) {
            style.height = this.props.height;
        }
        return <code id={`id-console-${this.props.id}`} style={style} className={styles.console}>
            <p className={styles.consoleTitle}>Jarboot Console</p>
        </code>;
    }
}
export default Console;
