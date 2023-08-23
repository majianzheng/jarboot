package io.github.majianzheng.jarboot.core.cmd.model;

import java.util.Arrays;
import java.util.Date;

/**
 * VO for TimeFragment
 * @author majiznehng
 */
public class TimeFragmentVO {
    private Integer index;
    private Date timestamp;
    private double cost;
    private boolean isReturn;
    private boolean isThrow;
    private String object;
    private String className;
    private String methodName;
    private Object[] params;
    private Object returnObj;
    private Throwable throwExp;

    public Integer getIndex() {
        return index;
    }

    public TimeFragmentVO setIndex(Integer index) {
        this.index = index;
        return this;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public TimeFragmentVO setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public double getCost() {
        return cost;
    }

    public TimeFragmentVO setCost(double cost) {
        this.cost = cost;
        return this;
    }

    public boolean isReturn() {
        return isReturn;
    }

    public TimeFragmentVO setReturn(boolean aReturn) {
        isReturn = aReturn;
        return this;
    }

    public boolean isThrow() {
        return isThrow;
    }

    public TimeFragmentVO setThrow(boolean aThrow) {
        isThrow = aThrow;
        return this;
    }

    public String getObject() {
        return object;
    }

    public TimeFragmentVO setObject(String object) {
        this.object = object;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public TimeFragmentVO setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public TimeFragmentVO setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public Object[] getParams() {
        return params;
    }

    public TimeFragmentVO setParams(Object[] params) {
        this.params = params;
        return this;
    }

    public Object getReturnObj() {
        return returnObj;
    }

    public TimeFragmentVO setReturnObj(Object returnObj) {
        this.returnObj = returnObj;
        return this;
    }

    public Throwable getThrowExp() {
        return throwExp;
    }

    public TimeFragmentVO setThrowExp(Throwable throwExp) {
        this.throwExp = throwExp;
        return this;
    }

    @Override
    public String toString() {
        return "TimeFragmentVO{" +
                "index=" + index +
                ", timestamp=" + timestamp +
                ", cost=" + cost +
                ", isReturn=" + isReturn +
                ", isThrow=" + isThrow +
                ", object='" + object + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", params=" + Arrays.toString(params) +
                ", returnObj=" + returnObj +
                ", throwExp=" + throwExp +
                '}';
    }
}
