import {memo, useRef} from "react";
import {UnControlled as CodeMirror} from 'react-codemirror2'
import 'codemirror/lib/codemirror.css';
import 'codemirror/theme/material.css';
import 'codemirror/mode/javascript/javascript';
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
import {JarBootConst} from "@/common/JarBootConst";

const JadView = memo((props: any) => {
    const codeRef = useRef<any>();

    const beforeChange = () => {
        if (codeRef?.current?.ref) {
            const codeDom = codeRef.current.ref;
            if (codeDom) {
                //该开源库的高度是写死的300px的高度，因此要在渲染前设定我们的高度
                codeDom.firstChild.style.height = `${JarBootConst.PANEL_HEIGHT}px`;
            }
        }
    };

    return <>
        <CodeMirror
            ref={codeRef}
            value={props?.data?.source}
            options={{
                mode: "text/x-java",
                theme: 'material',
                lineNumbers: true,
                readOnly: true,
                styleActiveLine: true,
                lineWrapping: true,
                foldGutter: true,
                gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
                fullScreen: false,
            }}
            // onChange={(editor, data, value) => {
            // }}
            onUpdate={beforeChange}
        />
    </>
});

export default JadView;
