import {notification} from 'antd';
import ErrorUtil from "@/common/ErrorUtil";
import {JarBootConst} from "@/common/JarBootConst";
import {getLocale} from "@@/plugin-locale/localeExports";

/**
 * 通知组件
 * @author majianzheng
 */
export default class CommonNotice {
    /**
     * 成功通知
     * @param msg 消息
     * @param description 描述
     */
    static success(msg: string, description = '') {
        notification['success']({
            message: msg,
            description: description,
        });
    }

    /**
     * 消息通知
     * @param msg 消息
     * @param description 描述
     */
    static info(msg: string | any, description: any = '') {
        notification['info']({
            message: msg,
            description: description,
        });
    }

    /**
     * 错误通知
     * @param msg 消息
     * @param description 描述
     */
    static error = (msg: string | any, description: any = '') => {
        notification['error']({
            message: msg,
            description: description,
        });
    };

    /**
     * 警告通知
     * @param msg 消息
     * @param description 描述
     */
    static warn = (msg: string | any, description: any = '') => {
        notification['warn']({
            message: msg,
            description: description,
        });
    };

    /**
     * 错误通知
     * @param error 消息
     */
    static errorFormatted = (error: any) => {
        CommonNotice.error(ErrorUtil.formatErrResp(error));
    };
}

const notSelectInfo = () => {
    if (JarBootConst.ZH_CN === getLocale()) {
        CommonNotice.info('请点击选择一行执行');
    } else {
        CommonNotice.info('Please select one to operate');
    }
};

export { notSelectInfo }
