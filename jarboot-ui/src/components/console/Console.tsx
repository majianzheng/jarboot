import React, {useEffect} from 'react';
import styles from './index.less';
import StringUtil from "@/common/StringUtil";
import Logger from "@/common/Logger";

interface ConsoleProps {
    visible?: boolean;
    content?: string;
    pubsub?: PublishSubmit;
    server: string;
}
const MAX_LINE = 20000;
const AUTO_CLEAN_LINE = 8000; //超出上限则移除最老的行数

const Console = (props: ConsoleProps) => {
    const id = `id-${props.server}`;
    let codeDom: HTMLElement|null = null;
    let loading = document.createElement('p');
    let isStartLoading = false;
    useEffect(() => {
        //初始化loading
        let three1 = document.createElement('div');
        let three2 = document.createElement('div');
        let three3 = document.createElement('div');
        three1.className= styles.three1;
        three2.className= styles.three2;
        three3.className= 'three3';
        loading.append(three1);
        loading.append(three2);
        loading.append(three3);
        loading.className = styles.loading;
        const {pubsub, server} = props;
        pubsub?.submit(server, 'appendLine', appendLine);
        pubsub?.submit(server, 'insertLineToHeader', insertLineToHeader);
        pubsub?.submit(server, 'startLoading', startLoading);
        pubsub?.submit(server, 'finishLoading', finishLoading);
        pubsub?.submit(server, 'clear', clear);
        if (StringUtil.isNotEmpty(props.content)) {
            _resetContent(props.content);
        }
    }, []);

    const init = () => {
        if (null !== codeDom) {
            return;
        }
        codeDom = document.querySelector(`#${id}`);
    };
    const _resetContent = (text: string|undefined) => {
        if (null == text) {
            return;
        }
        init();
        let count = codeDom?.childNodes.length;
        if (count) {
            for (let i = 0; i < count; ++i) {
                codeDom?.removeChild(codeDom.childNodes[0]);
            }
        }
        codeDom?.append(text);
    };
    const clear = () => {
        init();
        if (!codeDom) {
            return;
        }
        //codeDom.innerHTML = '';
        let count = codeDom.children.length;
        if (count > 0 && loading == codeDom.children[count - 1]) {
            //如果处于加载中，则保留加载的动画
            --count;
        }
        for(let i = 0; i < count; ++i){
            codeDom.removeChild(codeDom.children[0]);
        }
    };
    const startLoading = () => {
        _initLoading();
    };
    const finishLoading = () => {
        init();
        try {
            codeDom?.removeChild(loading);
        } catch (error) {
            //ignore
        }
        isStartLoading = false;
    };
    const appendLine = (line: string) => {
        init();
        if (StringUtil.isEmpty(line) || !codeDom) {
            //忽略空字符串
            return;
        }
        const count = codeDom.children.length;
        if (count > MAX_LINE) {
            //如果超过最大行数则移除最老的行
            for (let i = 0; i < AUTO_CLEAN_LINE; ++i) {
                codeDom.removeChild(codeDom.children[0]);
            }
        }
        if (!isStartLoading) {
            startLoading();
        }
        try {
            let p = _parseLine(line);
            loading.before(p);
            codeDom.scrollTop = codeDom.scrollHeight;
        } catch (e) {
            Logger.error(e);
        }

    };

    const insertLineToHeader = (line: string) => {
        init();
        if (!isStartLoading) {
            startLoading();
        }
        loading.after(_parseLine(line));
    };

    const _initLoading = () => {
        init();
        if (!isStartLoading) {
            try {
                codeDom?.append(loading);
                isStartLoading = true;
            } catch (e) {
                Logger.error(e);
            }
        }
    };
    const _parseLine = (line: string) => {
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
    };

    let style = {display: false === props.visible ? 'none' : 'block'};
    return (<>
            <code id={id} style={style} className={styles.console}>
            </code>
        </>
    );
}
export default Console;
