package com.mz.jarboot.common;

/**
 * @author jianzhengma
 */
public interface CmdProtocol {

    String toRaw();

    void fromRaw(String raw);
}
