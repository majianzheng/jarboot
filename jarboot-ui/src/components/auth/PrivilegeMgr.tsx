import React, {memo, useEffect, useState} from "react";
import { useIntl } from 'umi';
import {JarBootConst} from "@/common/JarBootConst";
import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";
import PrivilegeService from "@/services/PrivilegeService";
import RoleService from "@/services/RoleService";
import {Col, Menu, Row, Card, Tree} from "antd";

/**
 * 权限管理
 * @author majianzheng
 */
const PrivilegeMgr = memo(() => {
    const intl = useIntl();
    let [loading, setLoading] = useState(true);
    let [selected, setSelected] = useState(JarBootConst.ADMIN_ROLE);
    let [data, setData] = useState([] as any[]);
    let [permissionTree, setPermissionTree] = useState([] as any[]);
    let [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
    let [checkedKeys, setCheckedKeys] = useState<React.Key[]>([]);
    let [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
    let [autoExpandParent, setAutoExpandParent] = useState<boolean>(true);

    const onExpand = (expandedKeysValue: React.Key[]) => {
        setExpandedKeys(expandedKeysValue);
        setAutoExpandParent(false);
    };

    const onCheck = (checkedKeysValue: any, info: any) => {
        if (JarBootConst.ADMIN_ROLE === selected) {
            // 内置角色权限，不可修改
            CommonNotice.info("ADMIN_ROLE can not modify!");
            return;
        }
        PrivilegeService.savePrivilege(selected, info.node.key, !info.node.checked).then(resp => {
            if (resp.resultCode !== 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            setCheckedKeys(checkedKeysValue);
        }).catch(CommonNotice.errorFormatted);
    };

    const onSelectPermission = (selectedKeysValue: React.Key[], info: any) => {
        setSelectedKeys(selectedKeysValue);
    };

    useEffect(() => init(), []);

    const init = () => {
        queryRole(() => {
            // 初始化权限树
            PrivilegeService.getPermissionInfos().then(initTree).catch(CommonNotice.errorFormatted);
        });
    };

    const initTree = (resp: any) => {
        if (resp.resultCode !== 0) {
            CommonNotice.error(ErrorUtil.formatErrResp(resp));
            return;
        }
        const infos: Array<any> = resp.result;
        const tree = new Array<any>();
        infos.forEach(info => {
            const group = parseResource(info);
            let node = tree.find(p => p.key === group);
            if (!node) {
                node = {title: group, key: group, children: new Array<any>()};
                tree.push(node);
            }
            node.children.push({title: info.name, key: info.resource});
        });
        setPermissionTree(tree);
        setExpandedKeys([tree[0].key]);
        // 获取角色的权限
        permissionTree = tree;
        queryPermission(JarBootConst.ADMIN_ROLE);
    };

    const queryPermission = (role: string = JarBootConst.ADMIN_ROLE) => {
        setLoading(true);
        let treeSelect: any = [];
        if (JarBootConst.ADMIN_ROLE === role) {
            // ADMIN_ROLE 拥有所有权限
            permissionTree.forEach(node => {
                treeSelect.push(node.key);
                if (node?.children && node.children.length > 0) {
                    node.children.forEach((c: any) => treeSelect.push(c.key));
                }
            });
            setCheckedKeys(treeSelect);
            setLoading(false);
            return;
        }
        PrivilegeService.getPrivilegeByRole(role).then(resp => {
            if (resp.resultCode !== 0) {
                CommonNotice.errorFormatted(resp);
                setCheckedKeys(treeSelect);
                return;
            }
            treeSelect = resp.result.filter((item: any) => item.permission).map((item: any) => item.resource);
            setCheckedKeys(treeSelect);
            setLoading(false);
        }).catch(error => {
            CommonNotice.errorFormatted(error);
            setCheckedKeys(treeSelect);
        });
    };

    const queryRole = (callback: () => void) => {
        RoleService.getRoleList().then(resp => {
            if (resp.resultCode !== 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            setData(resp.result);
            callback && callback();
        }).catch(CommonNotice.errorFormatted);
    };

    const onSelect = (event: any) => {
        setSelected(event.key);
        // 更新权限树显示
        queryPermission(event.key);
    };

    const parseResource = (permissionInfo: any) => {
        const s: string = permissionInfo.resource;
        const p = s.indexOf('/jarboot/');
        if (-1 === p) {
            const l1 = s.indexOf('/');
            return s.substring(0, l1);
        }
        const b = p + 9;
        const l = s.indexOf('/', b);
        return -1 === l ? s.substring(b) : s.substring(b, l);
    };

    return <Row>
        <Col span={8} style={{height: (JarBootConst.PANEL_HEIGHT - 30), overflowY: "auto"}}>
            <Menu selectedKeys={[selected]} mode="inline" onClick={onSelect}>
                <Menu.ItemGroup title={<span>
                    <span>{intl.formatMessage({id: 'ROLE'})}</span>
                </span>}>
                    <Menu.Divider/>
                    {data.map((r: string) => <Menu.Item key={r}>{r}</Menu.Item>)}
                </Menu.ItemGroup>
            </Menu>
        </Col>
        <Col span={16}>
            <Card title={intl.formatMessage({id: 'PERMISSION_CONTROL_TITLE'})}
                  size={"small"} bordered={false} loading={loading}>
                <div style={{height: (JarBootConst.PANEL_HEIGHT - 30), overflowY: "auto"}}>
                    <Tree checkable
                          onExpand={onExpand}
                          expandedKeys={expandedKeys}
                          autoExpandParent={autoExpandParent}
                          onCheck={onCheck}
                          checkedKeys={checkedKeys}
                          onSelect={onSelectPermission}
                          selectedKeys={selectedKeys}
                          treeData={permissionTree}
                    />
                </div>
            </Card>
        </Col>
    </Row>;
});

export default PrivilegeMgr;
