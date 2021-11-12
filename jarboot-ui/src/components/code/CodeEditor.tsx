import React, {memo, useEffect, useRef, useState} from "react";
import {UnControlled as CodeMirror} from 'react-codemirror2'
import 'codemirror/lib/codemirror.css';
import 'codemirror/theme/material.css';
import 'codemirror/mode/javascript/javascript';
import 'codemirror/mode/markdown/markdown';
import 'codemirror/mode/shell/shell';
import 'codemirror/mode/yaml/yaml';
import 'codemirror/mode/properties/properties';
import 'codemirror/mode/css/css';
import 'codemirror/mode/htmlembedded/htmlembedded';
import 'codemirror/mode/sql/sql';
import 'codemirror/mode/xml/xml.js';
import 'codemirror/mode/python/python.js';
import 'codemirror/mode/perl/perl.js';
import 'codemirror/mode/clike/clike.js';
import 'codemirror/addon/display/fullscreen.css';
import 'codemirror/addon/display/fullscreen.js';
import 'codemirror/addon/fold/foldgutter.css';
import 'codemirror/addon/fold/foldcode.js';
import 'codemirror/addon/fold/foldgutter.js';
import 'codemirror/addon/fold/brace-fold.js';
import 'codemirror/addon/fold/comment-fold.js';
import 'codemirror/addon/selection/active-line';
import StringUtil from "@/common/StringUtil";
import CodeMode from "@/components/code/CodeMode";

interface CodeEditorProps {
    source: string;
    readOnly?: boolean;
    height: string|number;
    fullScreen?: boolean;
    mode?: CodeMode;
    onChange?: (editor: any, data: any, value: string) => void;
}

const CodeEditor = (props: CodeEditorProps) => {

    const codeRef = useRef<any>();
    const [code, setCode] = useState('');

    const init = () => {
        //这里为什么这么干呢？因为CodeMirror库定死了300px的高度，要想改高度必须这么干
        if (codeRef?.current?.ref) {
            const codeDom = codeRef.current.ref;
            if (codeDom) {
                //该开源库的高度是写死的300px的高度，因此要在渲染前设定我们的高度
                if (StringUtil.isString(props.height)) {
                    codeDom.firstChild.style.height = props.height;
                    return;
                }
                if (Number.NaN != Number(props.height)) {
                    codeDom.firstChild.style.height = `${props.height}px`;
                }
            }
        }
    };

    useEffect(init, []);

    useEffect(() => {
        //这里为什么非要加个setTimeout呢？因为CodeMirror这个库有bug，它有时候太快了会显示异常，所以这里加了个延迟更新
        setTimeout(() => {
            setCode(props.source || '');
        }, 500);
    }, [props.source]);

    return <div>
        <CodeMirror
            ref={codeRef}
            value={code}
            options={{
                mode: props.mode,
                theme: 'material',
                lineNumbers: true,
                readOnly: props.readOnly,
                styleActiveLine: true,
                lineWrapping: true,
                foldGutter: true,
                gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
                fullScreen: props.fullScreen,
            }}
            onChange={(editor, data, value) => {
                props?.onChange && props.onChange(editor, data, value);
            }}/>
    </div>
};

CodeEditor.defaultProps = {
    mode: CodeMode.JAVA
};

export default memo(CodeEditor);
