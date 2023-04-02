import type {FuncCode} from "@/common/EventConst";

export type MsgData = {
    event: number;
    sid: string;
    body: any;
}
export type MsgReq = {
    service?: string;
    sid?: string;
    body: string;
    func: FuncCode;
}
export enum CONSOLE_TOPIC {
    APPEND_LINE,
    STD_PRINT,
    BACKSPACE,
    FINISH_LOADING,
    INSERT_TO_HEADER,
    START_LOADING,
    CLEAR_CONSOLE,
    SCROLL_TO_END,
    SCROLL_TO_TOP,
}
export interface TreeNode {
    sid?: string;
    title?: string;
    key?: string;
    selectable?: boolean;
}

export interface ServiceInstance extends TreeNode {
    name: string;
    status?: string;
    group?: string;
    path?: string;

    onlineDebug: boolean;

    attaching: boolean;

    pid: number;

    remote: boolean;

    attached: boolean;
    children?: ServiceInstance[];
}

export interface JvmProcess extends TreeNode {
    fullName?: string;
    pid: number;
    attached: boolean;
    remote: string;
    attaching: boolean;
    trusted: boolean;
    children?: JvmProcess[];
}
