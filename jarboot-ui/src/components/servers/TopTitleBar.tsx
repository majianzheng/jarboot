import React from "react";
import styles from "@/components/servers/index.less";
import {CloseOutlined} from "@ant-design/icons";
import {Button} from "antd";

interface TopTitleBarProp {
    icon?: React.ReactNode;
    title: string|React.ReactNode;
    closeButtonTitle?: string;
    onClose: () => void;
}

const TopTitleBar = (props: TopTitleBarProp) => {

    return (<div className={styles.viewHeader}>
                <span className={styles.viewTitle}>
                    <span className={styles.viewTitleContainer}>
                       {props.icon}<label>{props.title}</label>
                    </span>
                </span>
        <div className={styles.viewHeaderTool}>
            <Button type={"link"}
                    size={"small"}
                    title={props.closeButtonTitle}
                    onClick={props.onClose}
                    icon={<CloseOutlined style={{fontSize: '1.28em'}}/>}/>
        </div>
    </div>);
}

export default TopTitleBar;
