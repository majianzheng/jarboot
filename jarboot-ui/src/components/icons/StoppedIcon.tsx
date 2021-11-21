import Icon from "@ant-design/icons";

const StoppedSvg = (props: any) => (
    <svg className="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg"
         p-id="8813" width="1em" height="1em" {...props}>
        <path
            d="M512 42.667008C254.733312 42.667008 42.665984 254.736384 42.665984 512s212.067328 469.331968 469.331968 469.331968c257.26464 0 469.334016-212.067328 469.334016-469.331968 0-257.26464-212.069376-469.334016-469.334016-469.334016M512 1024C228.692992 1024 0 795.307008 0 512 0 228.692992 228.692992 0 512 0c283.307008 0 512 228.692992 512 512 0 283.307008-228.692992 512-512 512"
            p-id="8814" fill="#d81e06"></path>
        <path d="M384 384h256v256H384z" p-id="8815" fill="#d81e06"></path>
    </svg>);

const StoppedIcon = (props: any) => <Icon component={StoppedSvg} {...props} />;

export default StoppedIcon;
