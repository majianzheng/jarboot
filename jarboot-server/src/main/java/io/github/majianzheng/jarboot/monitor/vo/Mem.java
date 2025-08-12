package io.github.majianzheng.jarboot.monitor.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 內存相关信息
 *
 * @author majianzheng
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Mem {
    /**
     * 内存总量
     */
    private double total;

    /**
     * 已用内存
     */
    private double used;

    /**
     * 剩余内存
     */
    private double free;
}
