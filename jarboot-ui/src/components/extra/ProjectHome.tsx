import {Tooltip} from "antd";
import {GithubOutlined} from "@ant-design/icons";


const ProjectHome = (props: any) => {
    return <Tooltip title={"Github"}>
        <a target={"_blank"}
           href={"https://github.com/majianzheng/jarboot"}
           className={props.iconClass}>
            <GithubOutlined/>
        </a>
    </Tooltip>;
};
export default ProjectHome;
