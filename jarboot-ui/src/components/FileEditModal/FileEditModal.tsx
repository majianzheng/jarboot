import {memo, useState} from "react";
import {Modal} from "antd";
import CodeEditor, {CodeMode} from "@/components/code";
import StringUtil from "@/common/StringUtil";

interface FileEditModalProp {
    visible: boolean;
    content: string;
    name: string;
    onClose: () => void;
    onSave: (value: string) => void;
    onChange: (value: string) => void;
}

const FileEditModal: any = memo((props: FileEditModalProp) => {
    let [content, setContent] = useState("");
    const closeModal = () => {
        props?.onClose();
    };
    const onOk = () => {
        const s = StringUtil.isEmpty(content) ? props.content : content;
        props?.onSave && props.onSave(s);
        closeModal();
    };
    const onChange = (editor: any, data: any, value: string) => {
        props?.onChange && props.onChange(value);
        setContent(value);
    };

    return <Modal title={props.name} visible={props.visible} width={860} maskClosable={false}
                  destroyOnClose={true} onOk={onOk} onCancel={closeModal}>
        <CodeEditor height={window.innerHeight - 320}
                    readOnly={false} onChange={onChange} mode={CodeMode.PROPERTY}
                    source={props?.content}/>
    </Modal>
});

export default FileEditModal;
