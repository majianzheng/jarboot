import React from 'react';
import styles from './index.less';
import StringUtil from "@/common/StringUtil";
import Logger from "@/common/Logger";
import {JarBootConst} from "@/common/JarBootConst";
import {BasicColor} from "@/components/console/ColorTable";

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

enum ParseStyleState {
    FAILED,
    SUCCESS,
    RESETED
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
const RESET = "[0m";

const Banner = (
    <div className={styles.banner}>
        <br/>
        <p>
            <span className={styles.red}><span>&nbsp;&nbsp; &nbsp; </span>,--.</span>
            <span className={styles.green}><span>&nbsp; &nbsp; &nbsp; &nbsp; </span></span>
            <span className={styles.yellow}> <span>&nbsp; &nbsp; &nbsp; </span></span>
            <span className={styles.blue}>,--. <span>&nbsp; </span></span>
            <span className={styles.magenta}><span>&nbsp; &nbsp; &nbsp; &nbsp;</span></span>
            <span className={styles.cyan}> <span>&nbsp; &nbsp; &nbsp; </span></span>
            <span className={styles.red}><span>&nbsp; </span>,--.<span>&nbsp; &nbsp;</span></span>
        </p>
        <p>
            <span className={styles.red}><span>&nbsp;&nbsp; &nbsp; </span>|<span>&nbsp; </span>|</span>
            <span className={styles.green}> ,--,--.</span>
            <span className={styles.yellow}>,--.--.</span>
            <span className={styles.blue}>|<span>&nbsp; </span>|-. </span>
            <span className={styles.magenta}> ,---. </span>
            <span className={styles.cyan}> ,---. </span>
            <span className={styles.red}>,-'<span>&nbsp; </span>'-. </span>
        </p>
        <p>
            <span className={styles.red}>,--. |<span>&nbsp; </span>|</span>
            <span className={styles.green}>' ,-.<span>&nbsp; </span>|</span>
            <span className={styles.yellow}>|<span>&nbsp; </span>.--'</span>
            <span className={styles.blue}>| .-. '</span>
            <span className={styles.magenta}>| .-. |</span>
            <span className={styles.cyan}>| .-. |</span>
            <span className={styles.red}>'-.<span>&nbsp; </span>.-' </span>
        </p>
        <p>
            <span className={styles.red}>|<span>&nbsp; </span>'-'<span>&nbsp; </span>/</span>
            <span className={styles.green}>\ '-'<span>&nbsp; </span>|</span>
            <span className={styles.yellow}>|<span>&nbsp; </span>| </span>
            <span className={styles.blue}>  | `-' |</span>
            <span className={styles.magenta}>' '-' '</span>
            <span className={styles.cyan}>' '-' '</span>
            <span className={styles.red}><span>&nbsp; </span>|<span>&nbsp; </span>|<span>&nbsp; &nbsp;</span></span>
        </p>
        <p>
            <span className={styles.red}> `-----' </span>
            <span className={styles.green}> `--`--'</span>
            <span className={styles.yellow}>`--'<span>&nbsp; &nbsp;</span></span>
            <span className={styles.blue}> `---' </span>
            <span className={styles.magenta}> `---' </span>
            <span className={styles.cyan}> `---' </span>
            <span className={styles.red}><span>&nbsp; </span>`--'<span>&nbsp; &nbsp;</span></span>
        </p>
        <br/>
        <br/>
        <p>Jarboot console, docs: <span className={styles.cyan}>{JarBootConst.DOCS_URL}</span></p>
        <p>Diagnose command, type ‘help’ and hit ‘ENTER’ to see.</p>
    </div>
);

/**
 * 控制台终端类
 * @author majianzheng
 */
class Console extends React.PureComponent<ConsoleProps> {
    private codeDom: HTMLElement|null = null;
    private loading = document.createElement('p');
    private isStartLoading = false;
    private eventQueue = [] as ConsoleEvent[];
    private lines = [] as HTMLElement[];
    private updateTimeoutFd: NodeJS.Timeout|null = null;
    private finishTimeoutFd: NodeJS.Timeout|null = null;
    private currentStyle: string|null = null;

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
        //色彩支持： \033[31m 文字 \033[0m
        const BEGIN = '[';
        let begin = line.indexOf(BEGIN);
        let end = 0;
        let preIndex = 0;
        let preLength = line.length;
        const noBegin = -1 === begin;
        while(-1 !== begin) {
            const mBegin = begin + BEGIN.length;
            const mIndex = line.indexOf('m', mBegin);
            if (-1 == mIndex) {
                break;
            }
            const termStyle = line.substring(mBegin, mIndex);
            //解析termStyle: 32m、 48;5;4m
            switch (this.parseTermStyle(termStyle)) {
                case ParseStyleState.FAILED:
                    begin = line.indexOf(BEGIN, begin + BEGIN.length);
                    continue;
                case ParseStyleState.RESETED:
                    const text = this.colorText(line.substring(preIndex, begin));
                    this.currentStyle = null;
                    preLength = line.length;
                    line = text + line.substring(mIndex + 1);
                    begin = text.length + preIndex;
                    break;
                case ParseStyleState.SUCCESS:
                    break;
                default:
                    break;
            }

            end = line.indexOf(RESET, begin + BEGIN.length);
            if (-1 === end) {
                //未结束，将开始后的都染色
                const start = begin + BEGIN.length + 3;
                line = (line.substring(0, begin) + this.colorText(line.substring(start)));
                break;
            }
            if (null !== this.currentStyle) {
                const start = begin + BEGIN.length + 3;
                const text = this.colorText(line.substring(start, end));
                line = (line.substring(0, begin) + text + line.substring(end + RESET.length));
                end = start + text.length;
            }
            this.currentStyle = null;
            preIndex = begin;
            preLength = line.length;
            begin = line.indexOf(BEGIN, end);
        }
        if (null !== this.currentStyle) {
            end = line.indexOf(RESET);
            if (-1 === end) {
                noBegin && (line = this.colorText(line));
            } else {
                const text = this.colorText(line.substring(0, end));
                line = (text + line.substring(end + RESET.length));
                this.currentStyle = null;
            }
        }
        p.innerHTML = line;
        return p;
    };

    /**
     * ig: \033[32m、 \033[48;5;4m
     * @return 是否成功
     * @param styles
     */
    private parseTermStyle(styles: string): ParseStyleState {
        if (StringUtil.isEmpty(styles)) {
            return ParseStyleState.FAILED;
        }
        const list = styles.split(';');
        let style = '';
        let reset = false;
        list.forEach(s => {
            const number = parseInt(s);
            if (isNaN(number)) {
                return;
            }
            const index = (number % 10);
            const type = Math.floor((number / 10));
            switch (type) {
                case 0:
                    //特殊格式控制
                    switch (index) {
                        case 0:
                            //关闭所有格式，还原为初始状态
                            reset = true;
                            break;
                        case 1:
                            //粗体/高亮显示
                            style += `font-weight:bold;`;
                            break;
                        case 2:
                            //模糊（※）
                            break;
                        case 3:
                            //斜体（※）
                            break;
                        case 4:
                            //下划线
                            break;
                        case 5:
                            //闪烁（慢）
                            break;
                        case 6:
                            //闪烁（快）（※）
                            break;
                        case 7:
                            //交换背景色与前景色
                            break;
                        case 8:
                            //隐藏（伸手不见五指，啥也看不见）（※）
                            break;
                        default:
                            break;
                    }
                    break;
                case 3:
                    //前景色
                    const fontColor = BasicColor[index];
                    fontColor && (style += `color:${fontColor};`);
                    break;
                case 4:
                    //背景色
                    const bgColor = BasicColor[index];
                    bgColor && (style += `background:${bgColor};`);
                    break;
                default:
                    //todo 其他情况暂未支持
                    break;
            }
        });
        if (StringUtil.isNotEmpty(style)) {
            this.currentStyle = style;
        }
        if (reset && 1 === list.length) {
            return ParseStyleState.RESETED;
        }
        return ParseStyleState.SUCCESS;
    }

    private colorText(text: string): string {
        if (StringUtil.isEmpty(this.currentStyle)) {
            return text;
        }
        return `<span style="${this.currentStyle}">${text}</span>`;
    }

    render() {
        const style: any = {display: false === this.props.visible ? 'none' : 'block'};
        if (this.props.height) {
            style.height = this.props.height;
        }
        return <code id={`id-console-${this.props.id}`} style={style} className={styles.console}>
            {Banner}
        </code>;
    }
}
export default Console;
