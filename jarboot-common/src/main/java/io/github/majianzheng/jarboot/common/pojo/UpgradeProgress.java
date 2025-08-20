package io.github.majianzheng.jarboot.common.pojo;

import lombok.Data;

@Data
public class UpgradeProgress {
    String host;
    Integer action; // 0 文件解压缩 1 文件检验 2 开始升级 -1 升级失败
    String msg;
}
