import React from 'react';
import styles from './index.less';
import StringUtil from "@/common/StringUtil";
import Logger from "@/common/Logger";
import {JarBootConst} from "@/common/JarBootConst";
import {ColorBasic, Color256, ColorBrightness} from "@/components/console/ColorTable";

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

interface SgrOption {
    /** 前景色 */
    foregroundColor: string;
    /** 背景色 */
    backgroundColor: string;
    /** 是否粗体 */
    bold: boolean;
    /** 是否弱化 */
    weaken: boolean;
    /** 是否因此 */
    hide: boolean;
    /** 反显，前景色和背景色掉换 */
    exchange: boolean;
    /** 倾斜 */
    oblique: boolean;
    /** 下划线 */
    underline: boolean;
    /** 上划线 */
    overline: boolean;
    /** 贯穿线 */
    through: boolean;
    /** 缓慢闪烁 */
    slowBlink: boolean;
    /** 快速闪烁 */
    fastBlink: boolean;
}

//最大行数
const MAX_LINE = 16384;
//超出上限则移除最老的行数
const AUTO_CLEAN_LINE = 12000;
//渲染更新延迟
const MAX_UPDATE_DELAY = 128;
const MAX_FINISHED_DELAY = MAX_UPDATE_DELAY * 2;
const LINE_CUR_ATTR = 'line-cur';
const BEGIN = '[';
const DEFAULT_SGR_OPTION: SgrOption = {
    backgroundColor: '',
    exchange: false,
    foregroundColor: '',
    hide: false,
    weaken: false,
    bold: false,
    oblique: false,
    underline: false,
    overline: false,
    through: false,
    slowBlink: false,
    fastBlink: false,
};

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
    private codeDom: Element | any = null;
    private loading = document.createElement('p');
    private isStartLoading = false;
    private eventQueue = [] as ConsoleEvent[];
    private lines = [] as HTMLElement[];
    private intavalHandle: NodeJS.Timeout|null = null;
    private finishTimeoutFd: NodeJS.Timeout|null = null;
    private sgrOption: SgrOption = {...DEFAULT_SGR_OPTION};

    componentDidMount() {
        this.intavalHandle = null;
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
            this.resetContent(this.props.content);
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
        this.intavalHandle = null;
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
        this.codeDom = document.querySelector(`code[id="id-console-${this.props.id}"]`) as Element;
    };

    private resetContent = (text: string|undefined) => {
        if (StringUtil.isEmpty(text)) {
            return;
        }
        this.init();
        this.codeDom && (this.codeDom.innerHTML = this.ansiCompile(text as string));
    };

    private clear = () => {
        this.eventQueue.push({type: EventType.CLEAR});
        //异步延迟MAX_UPDATE_DELAY毫秒，统一插入
        this.trigEvent();
    };

    private startLoading = () => {
        if (!this.isStartLoading) {
            try {
                this.codeDom.append(this.loading);
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
                this.codeDom.removeChild(this.loading);
            } catch (error) {
                //ignore
            }
            this.isStartLoading = false;
        }, MAX_FINISHED_DELAY);
    };

    private print = (text: string | undefined) => {
        this.eventQueue.push({type: EventType.PRINT, text});
        this.trigEvent();
    };

    private appendLine = (line: string | undefined) => {
        if (StringUtil.isString(line)) {
            this.eventQueue.push({type: EventType.LINE, text: line,});
            //异步延迟MAX_UPDATE_DELAY毫秒，统一插入
            this.trigEvent();
        }
    };

    private trigEvent() {
        if (this.intavalHandle) {
            //已经触发
            return;
        }
        this.init();
        this.intavalHandle = setTimeout(() => {
            this.intavalHandle = null;
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
                    this.codeDom.scrollTop = this.codeDom.scrollHeight;
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
        if (event.text) {
            event.text = this.ansiCompile(event.text as string);
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
        const line = this.createLine(event.text as string);
        this.lines.push(line);
    }

    private handlePrint(event: ConsoleEvent) {
        if (this.lines.length > 0) {
            let preLine = this.lines[this.lines.length - 1];
            if (preLine.hasAttribute(LINE_CUR_ATTR)) {
                //行未结束，将当前行附加到上一行
                preLine.innerHTML += event.text as string;
                event.deleted = true;
            } else {
                const line = this.createLine(event.text as string);
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
        this.trigEvent();
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
        if (!this.codeDom?.children?.length) {
            return null;
        }
        const len = this.codeDom.children.length;
        return this.isStartLoading ? this.codeDom.children[len - 2] : this.codeDom.children[len - 1];
    }

    private createLine = (line: string) => {
        if (0 === line?.length) {
            return document.createElement('br');
        }
        let p = document.createElement('p');
        p.innerHTML = line;
        return p;
    };

    private ansiCompile(line: string) {
        //色彩支持： \033[31m 文字 \033[0m
        let begin = line.indexOf(BEGIN);
        let preIndex = 0;
        let preBegin = -1;
        while (-1 !== begin) {
            const mBegin = begin + BEGIN.length;
            const mIndex = line.indexOf('m', mBegin);
            if (-1 == mIndex) {
                break;
            }
            const preStyle = this.toStyle();
            const termStyle = line.substring(mBegin, mIndex);
            //格式控制
            if (StringUtil.isNotEmpty(preStyle)) {
                const styled = this.styleText(line.substring(preIndex, begin), preStyle);
                const text = (preIndex > 0 && -1 !== preBegin) ? (line.substring(0, preBegin) + styled) : styled;
                line = (text + line.substring(mIndex + 1));
                preIndex = text.length;
            } else {
                const text = line.substring(0, begin);
                line = (text + line.substring(mIndex + 1));
                preIndex = text.length;
            }
            //解析termStyle: 32m、 48;5;4m
            if (!this.parseTermStyle(termStyle)) {
                Logger.error('parseTermStyle failed.', termStyle, line);
            }
            preBegin = begin;
            begin = line.indexOf(BEGIN, preIndex);
        }
        const style = this.toStyle();
        if (StringUtil.isNotEmpty(style)) {
            if (preIndex > 0) {
                line = (line.substring(0, preIndex) + this.styleText(line.substring(preIndex), style));
            } else {
                line = this.styleText(line, style);
            }
        }
        return line;
    }

    /**
     * ig: \033[32m、 \033[48;5;4m
     * @return 是否成功
     * @param styles
     */
    private parseTermStyle(styles: string): boolean {
        if (StringUtil.isEmpty(styles)) {
            return false;
        }
        const sgrList: string[] = styles.split(';');
        while (sgrList.length > 0) {
            const sgr = sgrList.shift() as string;
            const number = parseInt(sgr);
            if (isNaN(number)) {
                return false;
            }
            const index = (number % 10);
            const type = Math.floor((number / 10));
            switch (type) {
                case 0:
                    //特殊格式控制
                    this.specCtl(index, true);
                    break;
                case 1:
                    //字体控制，暂不支持
                    break;
                case 2:
                    //特殊格式关闭
                    this.specCtl(index, false);
                    break;
                case 3:
                    //前景色
                    this.setForeground(index, sgrList, true);
                    break;
                case 4:
                    //背景色
                    this.setBackground(index, sgrList, true);
                    break;
                case 5:
                    // 51: Framed、52: Encircled、53: 上划线、54: Not framed or encircled、55: 关闭上划线
                    switch (index) {
                        case 3:
                            // 53: 上划线
                            this.sgrOption.overline = true;
                            break;
                        case 5:
                            // 55: 关闭上划线
                            this.sgrOption.overline = false;
                            break;
                        default:
                            //其他暂不支持
                            break;
                    }
                    break;
                case 6:
                    //表意文字，暂不支持
                    // 60: 表意文字下划线或右边线
                    // 61: 表意文字双下划线或双右边线
                    // 62: 表意文字上划线或左边线
                    // 63: 表意文字双上划线或双左边线
                    // 64: 表意文字着重标志
                    // 65: 表意文字属性关闭
                    break;
                case 9:
                    //前景色，亮色系
                    this.setForeground(index, sgrList, false);
                    break;
                case 10:
                    //背景色，亮色系
                    this.setBackground(index, sgrList, false);
                    break;
                default:
                    //其他情况暂未支持
                    break;
            }
        }
        return true;
    }

    private parseSgr256Or24Color(sgrList: string[]) {
        //如果是2，则使用24位色彩格式，格式为：2;r;g;b
        //如果是5，则使用256色彩索引表
        const type = sgrList.shift();
        let color = '';
        switch (type) {
            case '2':
                const r = parseInt(sgrList.shift() as string);
                if (isNaN(r)) {
                    return color;
                }
                const g = parseInt(sgrList.shift() as string);
                if (isNaN(g)) {
                    return color;
                }
                const b = parseInt(sgrList.shift() as string);
                if (isNaN(b)) {
                    return color;
                }
                color = `rgb(${r},${g},${b})`;
                break;
            case '5':
                const index = parseInt(sgrList.shift() as string);
                if (isNaN(index)) {
                    return color;
                }
                color = Color256[index];
                break;
            default:
                break;
        }
        return color;
    }

    private specCtl(index: number, value: boolean) {
        switch (index) {
            case 0:
                //关闭所有格式，还原为初始状态，轻拷贝
                if (value) {
                    this.sgrOption = {...DEFAULT_SGR_OPTION};
                }
                break;
            case 1:
                //粗体/高亮显示
                this.sgrOption.bold = value;
                break;
            case 2:
                //弱化、模糊（※）
                this.sgrOption.weaken = value;
                break;
            case 3:
                //斜体（※）
                this.sgrOption.oblique = value;
                break;
            case 4:
                //下划线
                this.sgrOption.underline = value;
                break;
            case 5:
                //闪烁（慢）
                this.sgrOption.slowBlink = value;
                break;
            case 6:
                //闪烁（快）（※）
                this.sgrOption.fastBlink = value;
                break;
            case 7:
                //交换背景色与前景色
                this.sgrOption.exchange = value;
                break;
            case 8:
                //隐藏（伸手不见五指，啥也看不见）（※）
                this.sgrOption.hide = value;
                break;
            case 9:
                //划除
                this.sgrOption.through = value;
                break;
            default:
                break;
        }
    }

    private setForeground(index: number, sgrList: string[], basic: boolean) {
        switch (index) {
            case 8:
                //设置前景色
                const color = this.parseSgr256Or24Color(sgrList);
                this.sgrOption.foregroundColor = color;
                break;
            case 9:
                //恢复默认
                this.sgrOption.foregroundColor = '';
                break;
            default:
                const fontColor = (basic ? ColorBasic[index] : ColorBrightness[index]) || '';
                this.sgrOption.foregroundColor = fontColor;
                break;
        }
    }

    private setBackground(index: number, sgrList: string[], basic: boolean) {
        switch (index) {
            case 8:
                const color = this.parseSgr256Or24Color(sgrList);
                this.sgrOption.backgroundColor = color;
                break;
            case 9:
                //恢复默认
                this.sgrOption.backgroundColor = '';
                break;
            default:
                const bgColor = (basic ? ColorBasic[index] : ColorBrightness[index]) || '';
                this.sgrOption.backgroundColor = bgColor;
                break;
        }
    }

    private toStyle(): string {
        let style = '';
        if (this.sgrOption.hide) {
            style += `visibility:hidden;`;
        }
        if (this.sgrOption.exchange) {
            //前景色、背景色调用
            const foregroundColor = StringUtil.isEmpty(this.sgrOption.backgroundColor) ? '#263238' : this.sgrOption.backgroundColor;
            const backgroundColor = StringUtil.isEmpty(this.sgrOption.foregroundColor) ? 'seashell' : this.sgrOption.foregroundColor;
            style += `color:${foregroundColor};background:${backgroundColor};`;
        } else {
            if (StringUtil.isNotEmpty(this.sgrOption.backgroundColor)) {
                style += `background:${this.sgrOption.backgroundColor};`;
            }
            if (StringUtil.isNotEmpty(this.sgrOption.foregroundColor)) {
                style += `color:${this.sgrOption.foregroundColor};`;
            }
        }
        if (this.sgrOption.bold) {
            style += `font-weight:bold;`;
        }
        if (this.sgrOption.oblique) {
            style += `font-style:oblique;`;
        }
        let decorationLine = '';
        if (this.sgrOption.underline) {
            decorationLine += `underline `;
        }
        if (this.sgrOption.through) {
            decorationLine += `line-through `;
        }
        if (this.sgrOption.overline) {
            decorationLine += `overline `;
        }
        if (decorationLine.length) {
            style += `text-decoration-line:${decorationLine.trim()};`
        }
        if (this.sgrOption.weaken) {
            style += `opacity:.5;`;
        }
        let animation = '';
        if (this.sgrOption.slowBlink) {
            const blink = styles['blink'];
            animation += `${blink} 600ms infinite `;
        }
        if (this.sgrOption.fastBlink) {
            const blink = styles['blink'];
            animation += `${blink} 120ms infinite `;
        }
        if (animation.length) {
            style += `animation:${animation};-webkit-animation:${animation};`
        }
        return style;
    }

    private styleText(text: string, style: string): string {
        if (StringUtil.isEmpty(style)) {
            return text;
        }
        return `<span style="${style}">${text}</span>`;
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
