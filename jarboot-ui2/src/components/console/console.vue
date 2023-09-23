<template>
  <code class="console" :style="{ height: props.height, whiteSpace: props.wrap ? 'pre-wrap' : '' }" ref="consoleRef">
    <slot name="content">
      {{ props.content }}
    </slot>
  </code>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { ColorBasic, ColorBrightness, Color256 } from './ColorTable';
import Logger from '@/common/Logger';
import StringUtil from '@/common/StringUtil';
import type PublishSubmit from '@/common/PublishSubmit';
import { CONSOLE_TOPIC } from '@/types';
import { type ConsoleEvent, EventType, type SgrOption } from '@/components/console/ConsoleTypes';

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

type ConsoleProps = {
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
};

//最大行数
const MAX_LINE = 16384;
//超出上限则移除最老的行数
const AUTO_CLEAN_LINE = 12000;
//渲染更新延迟
const MAX_UPDATE_DELAY = 128;
const MAX_FINISHED_DELAY = MAX_UPDATE_DELAY * 2;
const BEGIN = '[';
const consoleRef = ref<HTMLElement>(null as unknown as HTMLElement);
const loading = document.createElement('p');
const props = defineProps<ConsoleProps>();

let isStartLoading = false;
let eventQueue = [] as ConsoleEvent[];
let finishHandle: any = null;
let intervalHandle: any = null;
let lines = [] as HTMLElement[];
let sgrOption: SgrOption = { ...DEFAULT_SGR_OPTION };

const onClear = () => {
  if (!consoleRef.value?.children?.length) {
    return;
  }
  const initLength = isStartLoading ? 2 : 1;
  if (consoleRef.value.children.length <= initLength) {
    return;
  }
  eventQueue.push({ type: EventType.CLEAR_EVENT });
  //异步延迟MAX_UPDATE_DELAY毫秒，统一插入
  trigEvent();
};
const onStartLoading = () => {
  if (isStartLoading) {
    return;
  }
  try {
    consoleRef.value.append(loading);
    isStartLoading = true;
  } catch (e) {
    Logger.error(e);
  }
};
const onFinishLoading = (str?: string) => {
  onConsole(str);
  if (finishHandle) {
    // 以最后一次生效，当前若存在则取消，重新计时
    clearTimeout(finishHandle);
  }
  //延迟异步，停止转圈
  finishHandle = setTimeout(() => {
    finishHandle = null;
    try {
      consoleRef.value.removeChild(loading);
    } catch (error) {
      //ignore
    }
    isStartLoading = false;
  }, MAX_FINISHED_DELAY);
};
const onStdPrint = (text: string | undefined) => {
  eventQueue.push({ type: EventType.STD_PRINT_EVENT, text });
  trigEvent();
};
const onConsole = (line: string | undefined) => {
  if (StringUtil.isString(line)) {
    eventQueue.push({ type: EventType.CONSOLE_EVENT, text: line });
    //异步延迟MAX_UPDATE_DELAY毫秒，统一插入
    trigEvent();
  }
};
const onBackspace = (num: string) => {
  let backspaceNum = parseInt(num);
  if (!Number.isInteger(backspaceNum)) {
    return;
  }
  eventQueue.push({ type: EventType.BACKSPACE_EVENT, backspaceNum });
  trigEvent();
};
const scrollToEnd = () => (consoleRef.value.scrollTop = consoleRef.value.scrollHeight);
const scrollToTop = () => (consoleRef.value.scrollTop = 0);
const trigEvent = () => {
  if (intervalHandle) {
    //已经触发
    return;
  }
  intervalHandle = setTimeout(eventLoop, MAX_UPDATE_DELAY);
};
/**
 * 事件循环，将一段时间内的事件收集起来统一处理
 */
const eventLoop = () => {
  intervalHandle = null;
  try {
    eventQueue.forEach(handleEvent);
    if (lines.length) {
      if (!isStartLoading) {
        onStartLoading();
      }
      //使用虚拟节点将MAX_UPDATE_DELAY时间内的所有更新一块append渲染，减轻浏览器负担
      const fragment = document.createDocumentFragment();
      lines.forEach(l => fragment.append(l));
      loading.before(fragment);
    }
    props.autoScrollEnd && scrollToEnd();
  } catch (e) {
    Logger.error(e);
  } finally {
    eventQueue = [];
    lines = [];
    //检查是否需要清理，如果超过最大行数则移除最老的行
    const count = consoleRef.value.children.length;
    if (count > MAX_LINE) {
      //超出的行数加上一次性清理的行
      const waitDeleteLineCount = count - MAX_LINE + AUTO_CLEAN_LINE;
      for (let i = 0; i < waitDeleteLineCount; ++i) {
        consoleRef.value.removeChild(consoleRef.value.children[0]);
      }
    }
  }
};
const handleEvent = (event: ConsoleEvent) => {
  try {
    switch (event.type) {
      case EventType.CONSOLE_EVENT:
        handleConsole(event);
        break;
      case EventType.STD_PRINT_EVENT:
        handleStdPrint(event);
        break;
      case EventType.BACKSPACE_EVENT:
        handleBackspace(event);
        break;
      case EventType.CLEAR_EVENT:
        handleClear(event);
        break;
      default:
        break;
    }
  } catch (e) {
    Logger.error(e);
  }
};
const handleClear = (event: ConsoleEvent) => {
  if (isStartLoading) {
    //如果处于加载中，则保留加载的动画
    consoleRef.value.innerHTML = event?.text || '';
    consoleRef.value.append(loading);
  } else {
    consoleRef.value.innerHTML = event?.text || '';
  }
};
const handleConsole = (event: ConsoleEvent) => {
  lines.push(createConsoleDiv(event));
};
const createConsoleDiv = (event: ConsoleEvent) => {
  if (event.text?.length) {
    const text = ansiCompile(event.text as string);
    const div = document.createElement('div');
    div.innerHTML = text;
    return div;
  }
  return document.createElement('br');
};
const handleStdPrint = (event: ConsoleEvent) => {
  if (!event.text?.length) {
    return;
  }

  //先处理待添加的Console行
  if (lines.length > 0) {
    const fragment = document.createDocumentFragment();
    lines.forEach(l => fragment.append(l));
    if (!isStartLoading) {
      onStartLoading();
    }
    loading.before(fragment);
    lines = [];
  }

  let text = event.text;
  let index = text.indexOf('\n');
  if (-1 == index) {
    //没有换行符时
    updateStdPrint(text);
    return;
  }

  //换行处理算法，解析字符串中的换行符，替换为p标签，行未结束为p标签，行结束标识为br
  while (-1 !== index) {
    let last = getLastLine() as HTMLElement;
    //1、截断一行；2、去掉左右尖括号"<>"；3、Ansi编译
    const left = ansiCompile(rawText(text.substring(0, index)));
    if (last) {
      if ('BR' === last.nodeName) {
        last.before(createNewLine(left));
      } else if ('P' === last.nodeName) {
        last.insertAdjacentHTML('beforeend', left);
        last.insertAdjacentHTML('afterend', '<br/>');
      } else {
        //其它标签
        last.insertAdjacentHTML('afterend', `<p>${left}</p><br/>`);
      }
    } else {
      //当前为空时，插入新的p和br
      consoleRef.value.insertAdjacentHTML('afterbegin', `<p>${left}</p><br/>`);
    }
    //得到下一个待处理的子串
    text = text.substring(index + 1);
    index = text.indexOf('\n');
  }
  if (text.length) {
    //换行符不在最后一位时，会剩下最后一个子字符串
    updateStdPrint(text);
  }
};
const updateStdPrint = (text: string) => {
  text = ansiCompile(rawText(text));
  let last = getLastLine() as HTMLElement;
  if (last) {
    if ('BR' === last.nodeName) {
      last.replaceWith(createNewLine(text));
    }
    if ('P' === last.nodeName) {
      last.insertAdjacentHTML('beforeend', text);
    } else {
      last.after(createNewLine(text));
    }
  } else {
    consoleRef.value.insertAdjacentHTML('afterbegin', `<p>${text}</p>`);
  }
};
const createNewLine = (content: string) => {
  const line = document.createElement('p');
  line.innerHTML = content;
  return line;
};
/**
 * 处理退格事件，退格核心算法入口
 * @param event 事件
 * @private
 */
const handleBackspace = (event: ConsoleEvent) => {
  let last = getLastLine() as HTMLElement;
  //backspace操作只会作用于最后一行，因此只认p标签
  if (!last || 'P' !== last.nodeName) {
    return;
  }
  let backspaceNum = event.backspaceNum as number;
  if (backspaceNum > 0) {
    const len = last.innerText.length - backspaceNum;
    if (len > 0) {
      //行内容未被全部删除时
      removeDeleted(last, len);
    } else {
      //行内容被全部清除时，保留一个换行符
      last.replaceWith(document.createElement('br'));
    }
  }
};
/**
 * 退格删除算法，留下保留的长度，剩下的去除
 * @param line p节点
 * @param len 保留的长度
 */
const removeDeleted = (line: HTMLElement, len: number) => {
  let html = '';
  let nodes = line.childNodes;
  for (let i = 0; i < nodes.length; ++i) {
    const node = nodes[i];
    const isText = '#text' === node.nodeName;
    let text = isText ? node.nodeValue || '' : (node as HTMLElement).innerText;
    const remained = len - text.length;
    if (remained > 0) {
      html += isText ? text : (node as HTMLElement).outerHTML;
      len = remained;
    } else {
      text = 0 === remained ? text : text.substring(0, len);
      if (isText) {
        html += text;
      } else {
        (node as HTMLElement).innerText = text;
        html += (node as HTMLElement).outerHTML;
      }
      break;
    }
  }
  line.innerHTML = html;
};
const getLastLine = (): Element | null => {
  if (!consoleRef.value.children?.length) {
    return null;
  }
  const len = consoleRef.value.children.length;
  return isStartLoading ? consoleRef.value.children[len - 2] : consoleRef.value.children[len - 1];
};
/**
 * Ansi核心算法入口
 * @param content 待解析的内容
 * @return {string} 解析后内容
 * @private
 */
const ansiCompile = (content: string): string => {
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
    const preStyle = toStyle();
    const termStyle = content.substring(mBegin, mIndex);
    //格式控制
    if (preStyle.length) {
      const styled = styleText(content.substring(preIndex, begin), preStyle);
      const text = preIndex > 0 && -1 !== preBegin ? content.substring(0, preBegin) + styled : styled;
      content = text + content.substring(mIndex + 1);
      preIndex = text.length;
    } else {
      const text = content.substring(0, begin);
      content = text + content.substring(mIndex + 1);
      preIndex = text.length;
    }
    //解析termStyle: 32m、 48;5;4m
    if (!parseTermStyle(termStyle)) {
      Logger.error('parseTermStyle failed.', termStyle, content);
    }
    preBegin = begin;
    begin = content.indexOf(BEGIN, preIndex);
  }
  const style = toStyle();
  if (style.length) {
    if (preIndex > 0) {
      content = content.substring(0, preIndex) + styleText(content.substring(preIndex), style);
    } else {
      content = styleText(content, style);
    }
  }
  return content;
};
const rawText = (text: string): string => {
  if (text.length) {
    return text.replace('<', '&lt;').replace('>', '&gt;');
  }
  return text;
};
/**
 * 样式包裹
 * @param text 文本
 * @param style 样式
 */
const styleText = (text: string, style: string): string => {
  if (style.length) {
    return `<span style="${style}">${rawText(text)}</span>`;
  }
  return text;
};
/**
 * ig: \033[32m、 \033[48;5;4m
 * @return 是否成功
 * @param styles 以分号分隔的数字字符串
 */
const parseTermStyle = (styles: string): boolean => {
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
    const index = number % 10;
    const type = Math.floor(number / 10);
    switch (type) {
      case 0:
        //特殊格式控制
        specCtl(index, true);
        break;
      case 1:
        //字体控制，暂不支持
        break;
      case 2:
        //特殊格式关闭
        specCtl(index, false);
        break;
      case 3:
        //前景色
        setForeground(index, sgrList, true);
        break;
      case 4:
        //背景色
        setBackground(index, sgrList, true);
        break;
      case 5:
        // 51: Framed、52: Encircled、53: 上划线、54: Not framed or encircled、55: 关闭上划线
        switch (index) {
          case 3:
            // 53: 上划线
            sgrOption.overline = true;
            break;
          case 5:
            // 55: 关闭上划线
            sgrOption.overline = false;
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
        setForeground(index, sgrList, false);
        break;
      case 10:
        //背景色，亮色系
        setBackground(index, sgrList, false);
        break;
      default:
        //其他情况暂未支持
        break;
    }
  }
  return true;
};

function parseRgb(sgrList: string[]): string {
  //依次取出r、g、b的值
  const r = parseInt(sgrList.shift() as string);
  if (isNaN(r)) {
    return '';
  }
  const g = parseInt(sgrList.shift() as string);
  if (isNaN(g)) {
    return '';
  }
  const b = parseInt(sgrList.shift() as string);
  if (isNaN(b)) {
    return '';
  }
  return `rgb(${r},${g},${b})`;
}

function parseColor256(sgrList: string[]): string {
  const index = parseInt(sgrList.shift() as string);
  if (isNaN(index)) {
    return '';
  }
  return Color256[index] || '';
}

/**
 * 256色、24位色解析
 * @param sgrList 颜色参数
 * @return {string} color
 */
const parseSgr256Or24Color = (sgrList: string[]): string => {
  //如果是2，则使用24位色彩格式，格式为：2;r;g;b
  //如果是5，则使用256色彩索引表
  const type = sgrList.shift();
  let color = '';
  switch (type) {
    case '2':
      //使用24位色彩格式，格式为：2;r;g;b
      color = parseRgb(sgrList);
      break;
    case '5':
      //使用256色彩索引表
      color = parseColor256(sgrList);
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
const specCtl = (index: number, value: boolean) => {
  switch (index) {
    case 0:
      //关闭所有格式，还原为初始状态，轻拷贝
      if (value) {
        sgrOption = { ...DEFAULT_SGR_OPTION };
      }
      break;
    case 1:
      //粗体/高亮显示
      sgrOption.bold = value;
      break;
    case 2:
      //弱化、模糊（※）
      sgrOption.weaken = value;
      break;
    case 3:
      //斜体（※）
      sgrOption.oblique = value;
      break;
    case 4:
      //下划线
      sgrOption.underline = value;
      break;
    case 5:
      //闪烁（慢）
      sgrOption.slowBlink = value;
      break;
    case 6:
      //闪烁（快）（※）
      sgrOption.fastBlink = value;
      break;
    case 7:
      //交换背景色与前景色
      sgrOption.exchange = value;
      break;
    case 8:
      //隐藏（伸手不见五指，啥也看不见）（※）
      sgrOption.hide = value;
      break;
    case 9:
      //划除
      sgrOption.through = value;
      break;
    default:
      break;
  }
};
/**
 * 前景色设置
 * @param index {number} 类型
 * @param sgrList {string[]} 颜色配置
 * @param basic {boolean} 是否是基本色
 * @private
 */
const setForeground = (index: number, sgrList: string[], basic: boolean) => {
  switch (index) {
    case 8:
      //设置前景色
      sgrOption.foregroundColor = parseSgr256Or24Color(sgrList);
      break;
    case 9:
      //恢复默认
      sgrOption.foregroundColor = '';
      break;
    default:
      sgrOption.foregroundColor = (basic ? ColorBasic[index] : ColorBrightness[index]) || '';
      break;
  }
};
/**
 * 背景色设置
 * @param index {number} 类型
 * @param sgrList {string[]} 颜色配置
 * @param basic {boolean} 是否是基本色
 * @private
 */
const setBackground = (index: number, sgrList: string[], basic: boolean) => {
  switch (index) {
    case 8:
      sgrOption.backgroundColor = parseSgr256Or24Color(sgrList);
      break;
    case 9:
      //恢复默认
      sgrOption.backgroundColor = '';
      break;
    default:
      sgrOption.backgroundColor = (basic ? ColorBasic[index] : ColorBrightness[index]) || '';
      break;
  }
};
/**
 * 将Ansi的配置转换为css样式
 * @private
 */
const toStyle = (): string => {
  let style = '';
  if (sgrOption.hide) {
    //隐藏，但需要保留位置
    style += `visibility:hidden;`;
  }
  if (sgrOption.exchange) {
    //前景色、背景色掉换
    const foregroundColor = StringUtil.isEmpty(sgrOption.backgroundColor) ? '#263238' : sgrOption.backgroundColor;
    const backgroundColor = StringUtil.isEmpty(sgrOption.foregroundColor) ? 'seashell' : sgrOption.foregroundColor;
    style += `color:${foregroundColor};background:${backgroundColor};`;
  } else {
    if (StringUtil.isNotEmpty(sgrOption.backgroundColor)) {
      style += `background:${sgrOption.backgroundColor};`;
    }
    if (StringUtil.isNotEmpty(sgrOption.foregroundColor)) {
      style += `color:${sgrOption.foregroundColor};`;
    }
  }
  if (sgrOption.bold) {
    style += `font-weight:bold;`;
  }
  if (sgrOption.oblique) {
    style += `font-style:oblique;`;
  }
  let decorationLine = '';
  if (sgrOption.underline) {
    decorationLine += `underline `;
  }
  if (sgrOption.through) {
    decorationLine += `line-through `;
  }
  if (sgrOption.overline) {
    decorationLine += `overline `;
  }
  if (decorationLine.length) {
    style += `text-decoration-line:${decorationLine.trim()};`;
  }
  if (sgrOption.weaken) {
    style += `opacity:.5;`;
  }
  let animation = '';
  if (sgrOption.slowBlink) {
    animation = `blink 800ms infinite `;
  }
  if (sgrOption.fastBlink) {
    //同时存在慢闪烁和快闪烁时，使用快的
    animation = `blink 200ms infinite `;
  }
  if (animation.length) {
    style += `animation:${animation};-webkit-animation:${animation};`;
  }
  return style;
};

onMounted(() => {
  intervalHandle = null;
  finishHandle = null;
  eventQueue = [];
  //初始化loading
  let three1 = document.createElement('div');
  let three2 = document.createElement('div');
  let three3 = document.createElement('div');
  three1.setAttribute('class', 'three1');
  three2.setAttribute('class', 'three2');
  three3.setAttribute('class', 'three3');
  loading.append(three1);
  loading.append(three2);
  loading.append(three3);
  loading.setAttribute('class', 'loading');

  const { pubsub, id } = props;
  //初始化code dom
  // if (content?.length) {
  //   resetContent(props.content);
  // }

  if (pubsub) {
    //初始化事件订阅
    pubsub.submit(id, CONSOLE_TOPIC.APPEND_LINE, onConsole);
    pubsub.submit(id, CONSOLE_TOPIC.STD_PRINT, onStdPrint);
    pubsub.submit(id, CONSOLE_TOPIC.BACKSPACE, onBackspace);
    pubsub.submit(id, CONSOLE_TOPIC.START_LOADING, onStartLoading);
    pubsub.submit(id, CONSOLE_TOPIC.FINISH_LOADING, onFinishLoading);
    pubsub.submit(id, CONSOLE_TOPIC.CLEAR_CONSOLE, onClear);
    pubsub.submit(id, CONSOLE_TOPIC.SCROLL_TO_END, scrollToEnd);
    pubsub.submit(id, CONSOLE_TOPIC.SCROLL_TO_TOP, scrollToTop);
  }
});
onUnmounted(() => {
  intervalHandle = null;
  const { pubsub, id } = props;
  if (pubsub) {
    pubsub.unSubmit(id, CONSOLE_TOPIC.APPEND_LINE, onConsole);
    pubsub.unSubmit(id, CONSOLE_TOPIC.STD_PRINT, onStdPrint);
    pubsub.unSubmit(id, CONSOLE_TOPIC.BACKSPACE, onBackspace);
    pubsub.unSubmit(id, CONSOLE_TOPIC.START_LOADING, onStartLoading);
    pubsub.unSubmit(id, CONSOLE_TOPIC.FINISH_LOADING, onFinishLoading);
    pubsub.unSubmit(id, CONSOLE_TOPIC.CLEAR_CONSOLE, onClear);
    pubsub.unSubmit(id, CONSOLE_TOPIC.SCROLL_TO_END, scrollToEnd);
    pubsub.unSubmit(id, CONSOLE_TOPIC.SCROLL_TO_TOP, scrollToTop);
  }
});
</script>

<style lang="less">
@import '@/assets/main.less';

@console-line-height: 1.186;

.console {
  flex: none;
  height: 100%;
  width: 100%;
  background: @console-background;
  overflow: auto;
  font-family: @console-font-family;
  color: @console-color;
  line-height: @console-line-height;
  white-space: pre;
  tab-size: 4;
  //火狐浏览器隐藏滚动条
  scrollbar-width: none;
  //IE、Edge自动隐藏滚动条
  -ms-overflow-style: -ms-autohiding-scrollbar;
  p,
  div {
    margin: 0;
    ln {
      color: gray;
      margin: 0 8px 0 2px;
      min-width: 45px;
      display: inline-block;
      border-right: 1px solid;
      .textNoSelect();
    }
  }
  table {
    border: inset olivedrab;
    margin: 0 0 0 15px;
    tr > td,
    tr > th {
      padding: 0 6px 0 6px;
    }
  }
}

.console::-webkit-scrollbar {
  /*滚动条整体样式*/
  width: @scrollbar-size;
  height: 0;
}

.loading {
  width: 150px;
  margin: 2px auto;
  text-align: center;
  div {
    width: 18px;
    height: 18px;
    border-radius: 100%;
    display: inline-block;
    background-color: #1ee6e2;
    -webkit-animation: three 1.4s infinite ease-in-out;
    animation: three 1.4s infinite ease-in-out;
    -webkit-animation-fill-mode: both;
    animation-fill-mode: both;
  }
}
.loading .three1 {
  -webkit-animation-delay: -0.3s;
  animation-delay: -0.3s;
}
.loading .three2 {
  -webkit-animation-delay: -0.15s;
  animation-delay: -0.15s;
}
@-webkit-keyframes three {
  0%,
  80%,
  100% {
    -webkit-transform: scale(0);
  }
  40% {
    -webkit-transform: scale(1);
  }
}
@keyframes three {
  0%,
  80%,
  100% {
    -webkit-transform: scale(0);
  }
  40% {
    -webkit-transform: scale(1);
  }
}
</style>
