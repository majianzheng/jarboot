import Icon from "@ant-design/icons";

const Svg = (props: any) => (
    <svg className="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg"
         p-id="3840" width="200" height="200" {...props}>
        <path d="M640 768h64a106.666667 106.666667 0 1 0 0-213.333333H128v-85.333334h576a192 192 0 1 1 0 384H640v85.333334l-170.666667-128 170.666667-128v85.333333zM128 170.666667h768v85.333333H128V170.666667z m256 597.333333v85.333333H128v-85.333333h256z"
            p-id="3841"></path>
    </svg>);

const TextWrapIcon = (props: any) => <Icon component={Svg} {...props} />;

export default TextWrapIcon;
