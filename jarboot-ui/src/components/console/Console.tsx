import React from 'react';
import styles from './index.less';
import StringUtil from "@/common/StringUtil";
import Logger from "@/common/Logger";
import {JarBootConst} from "@/common/JarBootConst";
import {ColorBasic, Color256, ColorBrightness} from "@/components/console/ColorTable";

interface ConsoleProps {
    /** 是否显示 */
    visible?: boolean;
    /** 初始内容 */
    content?: string;
    /** 订阅发布 */
    pubsub?: PublishSubmit;
    /** 唯一id */
    id: string;
    /** 高度 */
    height?: string | number;
    /** 是否自动滚动到底部 */
    autoScrollEnd?: boolean;
    /** 文字超出边界时是否自动换行 */
    wrap: boolean;
}

enum EventType {
    /** Console一行 */
    CONSOLE_EVENT,
    /** Std标准输出 */
    STD_PRINT_EVENT,
    /** std退格 */
    BACKSPACE_EVENT,
    /** 清屏 */
    CLEAR_EVENT
}

interface ConsoleEvent {
    /** 是否显示 */
    type: EventType,
    /** 是否显示 */
    text?: string,
    /** 是否显示 */
    backspaceNum?: number,
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

enum CONSOLE_TOPIC {
    APPEND_LINE,
    STD_PRINT,
    BACKSPACE,
    FINISH_LOADING,
    INSERT_TO_HEADER,
    START_LOADING,
    CLEAR_CONSOLE,
    SCROLL_TO_END,
}

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
    private intervalHandle: NodeJS.Timeout|null = null;
    private finishHandle: NodeJS.Timeout|null = null;
    private sgrOption: SgrOption = {...DEFAULT_SGR_OPTION};

    componentDidMount() {
        this.intervalHandle = null;
        this.finishHandle = null;
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

        const {pubsub, id, content} = this.props;
        //初始化code dom
        this.codeDom = document.querySelector(`code[id="id-console-${id}"]`) as Element;
        if (content?.length) {
            this.resetContent(this.props.content);
        }

        if (pubsub) {
            //初始化事件订阅
            pubsub.submit(id, CONSOLE_TOPIC.APPEND_LINE, this.onConsole);
            pubsub.submit(id, CONSOLE_TOPIC.STD_PRINT, this.onStdPrint);
            pubsub.submit(id, CONSOLE_TOPIC.BACKSPACE, this.onBackspace);
            pubsub.submit(id, CONSOLE_TOPIC.START_LOADING, this.onStartLoading);
            pubsub.submit(id, CONSOLE_TOPIC.FINISH_LOADING, this.onFinishLoading);
            pubsub.submit(id, CONSOLE_TOPIC.CLEAR_CONSOLE, this.onClear);
            pubsub.submit(id, CONSOLE_TOPIC.SCROLL_TO_END, this.scrollToEnd);
        }
    }

    componentWillUnmount() {
        this.intervalHandle = null;
        const {pubsub, id} = this.props;
        if (pubsub) {
            pubsub.unSubmit(id, CONSOLE_TOPIC.APPEND_LINE, this.onConsole);
            pubsub.unSubmit(id, CONSOLE_TOPIC.STD_PRINT, this.onStdPrint);
            pubsub.unSubmit(id, CONSOLE_TOPIC.BACKSPACE, this.onBackspace);
            pubsub.unSubmit(id, CONSOLE_TOPIC.START_LOADING, this.onStartLoading);
            pubsub.unSubmit(id, CONSOLE_TOPIC.FINISH_LOADING, this.onFinishLoading);
            pubsub.unSubmit(id, CONSOLE_TOPIC.CLEAR_CONSOLE, this.onClear);
            pubsub.submit(id, CONSOLE_TOPIC.SCROLL_TO_END, this.scrollToEnd);
        }
    }

    private resetContent = (text: string|undefined) => {
        if (text?.length) {
            this.codeDom && (this.codeDom.innerHTML = this.ansiCompile(text as string));
        }
    };

    private onClear = () => {
        if (!this.codeDom?.children?.length) {
            return;
        }
        const initLength = this.isStartLoading ? 2 : 1;
        if (this.codeDom.children.length <= initLength) {
            return;
        }
        this.eventQueue.push({type: EventType.CLEAR_EVENT});
        //异步延迟MAX_UPDATE_DELAY毫秒，统一插入
        this.trigEvent();
    };

    private onStartLoading = () => {
        if (this.isStartLoading) {
            return;
        }
        try {
            this.codeDom.append(this.loading);
            this.isStartLoading = true;
        } catch (e) {
            Logger.error(e);
        }
    };

    private onFinishLoading = (str?: string) => {
        this.onConsole(str);
        if (this.finishHandle) {
            // 以最后一次生效，当前若存在则取消，重新计时
            clearTimeout(this.finishHandle);
        }
        //延迟异步，停止转圈
        this.finishHandle = setTimeout(() => {
            this.finishHandle = null;
            try {
                this.codeDom.removeChild(this.loading);
            } catch (error) {
                //ignore
            }
            this.isStartLoading = false;
        }, MAX_FINISHED_DELAY);
    };

    private onStdPrint = (text: string | undefined) => {
        this.eventQueue.push({type: EventType.STD_PRINT_EVENT, text});
        this.trigEvent();
    };

    private onConsole = (line: string | undefined) => {
        if (StringUtil.isString(line)) {
            this.eventQueue.push({type: EventType.CONSOLE_EVENT, text: line,});
            //异步延迟MAX_UPDATE_DELAY毫秒，统一插入
            this.trigEvent();
        }
    };

    private onBackspace = (num: string) => {
        let backspaceNum = parseInt(num);
        if (!Number.isInteger(backspaceNum)) {
            return;
        }
        this.eventQueue.push({type: EventType.BACKSPACE_EVENT, backspaceNum});
        this.trigEvent();
    };

    /**
     * 滚动到最后
     */
    private scrollToEnd = () => {
        this.codeDom.scrollTop = this.codeDom.scrollHeight;
    };

    /**
     * 触发事件
     * @private
     */
    private trigEvent() {
        if (this.intervalHandle) {
            //已经触发
            return;
        }
        this.intervalHandle = setTimeout(this.eventLoop, MAX_UPDATE_DELAY);
    }

    /**
     * 事件循环，将一段时间内的事件收集起来统一处理
     */
    private eventLoop = () => {
        this.intervalHandle = null;
        try {
            this.eventQueue.forEach(this.handleEvent);
            if (this.lines.length) {
                if (!this.isStartLoading) {
                    this.onStartLoading()
                }
                //使用虚拟节点将MAX_UPDATE_DELAY时间内的所有更新一块append渲染，减轻浏览器负担
                const fragment = document.createDocumentFragment();
                this.lines.forEach(l => fragment.append(l));
                this.loading.before(fragment);
            }
            this.props.autoScrollEnd && this.scrollToEnd();
        } catch (e) {
            Logger.error(e);
        } finally {
            this.eventQueue = [];
            this.lines = [];
            //检查是否需要清理，如果超过最大行数则移除最老的行
            const count = this.codeDom.children.length;
            if (count > MAX_LINE) {
                //超出的行数加上一次性清理的行
                const waitDeleteLineCount = count - MAX_LINE + AUTO_CLEAN_LINE;
                for (let i = 0; i < waitDeleteLineCount; ++i) {
                    this.codeDom.removeChild(this.codeDom.children[0]);
                }
            }
        }
    };

    /**
     * 事件处理
     * @param event 事件
     */
    private handleEvent = (event: ConsoleEvent) => {
        try {
            switch (event.type) {
                case EventType.CONSOLE_EVENT:
                    this.handleConsole(event);
                    break;
                case EventType.STD_PRINT_EVENT:
                    this.handleStdPrint(event);
                    break;
                case EventType.BACKSPACE_EVENT:
                    this.handleBackspace(event);
                    break;
                case EventType.CLEAR_EVENT:
                    this.handleClear();
                    break;
                default:
                    break;
            }
        } catch (e) {
            Logger.error(e);
        }
    };

    /**
     * 处理清屏事件
     * @private
     */
    private handleClear() {
        if (this.isStartLoading) {
            //如果处于加载中，则保留加载的动画
            this.codeDom.innerHTML = "";
            this.codeDom.append(this.loading);
        } else {
            this.codeDom.innerHTML = "";
        }
    }

    /**
     * 处理Console事件
     * @param event 事件
     * @private
     */
    private handleConsole(event: ConsoleEvent) {
        this.lines.push(this.createConsoleDiv(event));
    }

    /**
     * 创建一行Console容器
     * @param event 事件
     * @private
     */
    private createConsoleDiv(event: ConsoleEvent) {
        if (event.text?.length) {
            const text = this.ansiCompile(event.text as string);
            const div = document.createElement('div');
            div.innerHTML = text;
            return div;
        }
        return document.createElement('br');
    }

    /**
     * 处理STD print事件，STD核心算法
     * @param event 事件
     * @private
     */
    private handleStdPrint(event: ConsoleEvent) {
        if (!event.text?.length) {
            return;
        }

        //先处理待添加的Console行
        if (this.lines.length > 0) {
            const fragment = document.createDocumentFragment();
            this.lines.forEach(l => fragment.append(l));
            if (!this.isStartLoading) {
                this.onStartLoading();
            }
            this.loading.before(fragment);
            this.lines = [];
        }

        let text = event.text;
        let index = text.indexOf('\n');
        if (-1 == index) {
            //没有换行符时
            this.updateStdPrint(text);
            return;
        }

        //换行处理算法，解析字符串中的换行符，替换为p标签，行未结束为p标签，行结束标识为br
        while (-1 !== index) {
            let last = this.getLastLine() as HTMLElement;
            //1、截断一行；2、去掉左右尖括号"<>"；3、Ansi编译
            const left = this.ansiCompile(this.rawText(text.substring(0, index)));
            if (last) {
                if ('BR' === last.nodeName) {
                    last.before(this.createNewLine(left));
                } else if ('P' === last.nodeName) {
                    last.insertAdjacentHTML("beforeend", left);
                    last.insertAdjacentHTML('afterend', '<br/>');
                } else {
                    //其它标签
                    last.insertAdjacentHTML("afterend", `<p>${left}</p><br/>`);
                }
            } else {
                //当前为空时，插入新的p和br
                this.codeDom.insertAdjacentHTML('afterbegin', `<p>${left}</p><br/>`);
            }
            //得到下一个待处理的子串
            text = text.substring(index + 1);
            index = text.indexOf('\n');
        }
        if (text.length) {
            //换行符不在最后一位时，会剩下最后一个子字符串
            this.updateStdPrint(text);
        }
    }

    /**
     * STD print更新最后一行内容
     * @param text 内容
     * @private
     */
    private updateStdPrint(text: string) {
        text = this.ansiCompile(this.rawText(text));
        let last = this.getLastLine() as HTMLElement;
        if (last) {
            if ('BR' === last.nodeName) {
                last.replaceWith(this.createNewLine(text));
            }
            if ('P' === last.nodeName) {
                last.insertAdjacentHTML("beforeend", text);
            } else {
                last.after(this.createNewLine(text));
            }
        } else {
            this.codeDom.insertAdjacentHTML('afterbegin', `<p>${text}</p>`);
        }
    }

    /**
     * 创建STD print一行
     * @param content 内容
     */
    private createNewLine = (content: string) => {
        const line = document.createElement('p');
        line.innerHTML = content;
        return line;
    };

    /**
     * 处理退格事件，退格核心算法入口
     * @param event 事件
     * @private
     */
    private handleBackspace(event: ConsoleEvent) {
        let last = this.getLastLine() as HTMLElement;
        //backspace操作只会作用于最后一行，因此只认p标签
        if (!last || 'P' !== last.nodeName) {
            return;
        }
        let backspaceNum = event.backspaceNum as number;
        if (backspaceNum > 0) {
            const len = last.innerText.length - backspaceNum;
            if (len > 0) {
                //行内容未被全部删除时
                this.removeDeleted(last, len);
            } else {
                //行内容被全部清除时，保留一个换行符
                last.replaceWith(document.createElement('br'));
            }
        }
    }

    /**
     * 退格删除算法，留下保留的长度，剩下的去除
     * @param line p节点
     * @param len 保留的长度
     */
    private removeDeleted = (line: HTMLElement, len: number) => {
        let html = '';
        let nodes = line.childNodes;
        for(let i = 0; i < nodes.length; ++i){
            const node = nodes[i];
            const isText = ('#text' === node.nodeName);
            let text = isText ? (node.nodeValue || '') : ((node as HTMLElement).innerText);
            const remained = len - text.length;
            if (remained > 0) {
                html += (isText ? text : ((node as HTMLElement).outerHTML));
                len = remained;
            } else {
                text = (0 === remained) ? text : text.substring(0, len);
                if (isText) {
                    html += text;
                } else {
                    (node as HTMLElement).innerText = text;
                    html += ((node as HTMLElement).outerHTML);
                }
                break;
            }
        }
        line.innerHTML = html;
    };

    /**
     * 获取最后一行
     * @private
     */
    private getLastLine(): HTMLElement|null {
        if (!this.codeDom.children?.length) {
            return null;
        }
        const len = this.codeDom.children.length;
        return this.isStartLoading ? this.codeDom.children[len - 2] : this.codeDom.children[len - 1];
    }

    /**
     * Ansi核心算法入口
     * @param content 待解析的内容
     * @return {string} 解析后内容
     * @private
     */
    private ansiCompile(content: string): string {
        //色彩支持： \033[31m 文字 \033[0m
        let begin = content.indexOf(BEGIN);
        let preIndex = 0;
        let preBegin = -1;
        while (-1 !== begin) {
            const mBegin = begin + BEGIN.length;
            const mIndex = content.indexOf('m', mBegin);
            if (-1 == mIndex) {
                break;
            }
            const preStyle = this.toStyle();
            const termStyle = content.substring(mBegin, mIndex);
            //格式控制
            if (preStyle.length) {
                const styled = this.styleText(content.substring(preIndex, begin), preStyle);
                const text = (preIndex > 0 && -1 !== preBegin) ? (content.substring(0, preBegin) + styled) : styled;
                content = (text + content.substring(mIndex + 1));
                preIndex = text.length;
            } else {
                const text = content.substring(0, begin);
                content = (text + content.substring(mIndex + 1));
                preIndex = text.length;
            }
            //解析termStyle: 32m、 48;5;4m
            if (!this.parseTermStyle(termStyle)) {
                Logger.error('parseTermStyle failed.', termStyle, content);
            }
            preBegin = begin;
            begin = content.indexOf(BEGIN, preIndex);
        }
        const style = this.toStyle();
        if (style.length) {
            if (preIndex > 0) {
                content = (content.substring(0, preIndex) + this.styleText(content.substring(preIndex), style));
            } else {
                content = this.styleText(content, style);
            }
        }
        return content;
    }

    /**
     * 尖括号转义
     * @param text 字符串
     * @return {string}
     */
    private rawText = (text: string): string => {
        if (text.length) {
            return text.replaceAll('<', '&lt;').replaceAll('>', '&gt;');
        }
        return text;
    };

    /**
     * 样式包裹
     * @param text 文本
     * @param style 样式
     */
    private styleText = (text: string, style: string): string => {
        if (style.length) {
            return `<span style="${style}">${this.rawText(text)}</span>`;
        }
        return text;
    };

    /**
     * ig: \033[32m、 \033[48;5;4m
     * @return 是否成功
     * @param styles 以分号分隔的数字字符串
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

    /**
     * 256色、24位色解析
     * @param sgrList 颜色参数
     * @return {string} color
     */
    private parseSgr256Or24Color = (sgrList: string[]): string => {
        //如果是2，则使用24位色彩格式，格式为：2;r;g;b
        //如果是5，则使用256色彩索引表
        const type = sgrList.shift();
        let color = '';
        switch (type) {
            case '2':
                //使用24位色彩格式，格式为：2;r;g;b
                //依次取出r、g、b的值
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
                //使用256色彩索引表
                const index = parseInt(sgrList.shift() as string);
                if (isNaN(index)) {
                    return color;
                }
                color = Color256[index] || '';
                break;
            default:
                break;
        }
        return color;
    };

    /**
     * 特殊格式设置
     * @param index {number} 类型
     * @param value {boolean} 是否启用
     * @private
     */
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

    /**
     * 前景色设置
     * @param index {number} 类型
     * @param sgrList {string[]} 颜色配置
     * @param basic {boolean} 是否是基本色
     * @private
     */
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

    /**
     * 背景色设置
     * @param index {number} 类型
     * @param sgrList {string[]} 颜色配置
     * @param basic {boolean} 是否是基本色
     * @private
     */
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

    /**
     * 将Ansi的配置转换为css样式
     * @private
     */
    private toStyle(): string {
        let style = '';
        if (this.sgrOption.hide) {
            //隐藏，但需要保留位置
            style += `visibility:hidden;`;
        }
        if (this.sgrOption.exchange) {
            //前景色、背景色掉换
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
            animation = `${blink} 800ms infinite `;
        }
        if (this.sgrOption.fastBlink) {
            const blink = styles['blink'];
            //同时存在慢闪烁和快闪烁时，使用快的
            animation = `${blink} 200ms infinite `;
        }
        if (animation.length) {
            style += `animation:${animation};-webkit-animation:${animation};`
        }
        return style;
    }

    render() {
        const style: any = {display: false === this.props.visible ? 'none' : 'block'};
        if (this.props.height) {
            style.height = this.props.height;
        }
        if (this.props.wrap) {
            style.whiteSpace = "pre-wrap";
        }
        return <code id={`id-console-${this.props.id}`}
                     style={style}
                     className={styles.console}>
            {Banner}
        </code>;
    }
}

export {CONSOLE_TOPIC};
export default Console;
