package com.mz.jarboot.core.resp;


/**
 * handle the command's response to jarboot-server
 * @author jianzhengma
 */
public interface ResponseStream {
    void write(String data);
}
