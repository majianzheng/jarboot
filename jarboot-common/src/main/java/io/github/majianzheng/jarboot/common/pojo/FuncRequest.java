package io.github.majianzheng.jarboot.common.pojo;

/**
 * @author majianzheng
 */
public class FuncRequest {
    protected String host;
    protected String service;
    protected int func;
    protected String sid;
    protected String body;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getFunc() {
        return func;
    }

    public void setFunc(int func) {
        this.func = func;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "FuncRequest{" +
                "service='" + service + '\'' +
                ", func=" + func +
                ", sid='" + sid + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
