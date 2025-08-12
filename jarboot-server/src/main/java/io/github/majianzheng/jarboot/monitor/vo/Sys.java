package io.github.majianzheng.jarboot.monitor.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统相关信息
 *
 * @author majianzheng
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Sys {
    /**
     * 服务器名称
     */
    private String computerName;

    /**
     * 项目路径
     */
    private String userDir;

    /**
     * 操作系统
     */
    private String osName;

    /**
     * 系统架构
     */
    private String osArch;

}
