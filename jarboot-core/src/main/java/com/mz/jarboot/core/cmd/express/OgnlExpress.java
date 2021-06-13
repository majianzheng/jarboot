package com.mz.jarboot.core.cmd.express;

import com.mz.jarboot.core.constant.CoreConstant;
import ognl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class OgnlExpress implements Express {
    private static final MemberAccess MEMBER_ACCESS = new DefaultMemberAccess(true);
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);

    private Object bindObject;
    private final OgnlContext context;

    public OgnlExpress() {
        this(CustomClassResolver.customClassResolver);
    }

    public OgnlExpress(ClassResolver classResolver) {
        context = new OgnlContext();
        context.setClassResolver(classResolver);
        // allow private field access
        context.setMemberAccess(MEMBER_ACCESS);
    }

    @Override
    public Object get(String express) throws com.mz.jarboot.core.cmd.express.ExpressException {
        try {
            return Ognl.getValue(express, context, bindObject);
        } catch (Exception e) {
            logger.error("Error during evaluating the expression:", e);
            throw new com.mz.jarboot.core.cmd.express.ExpressException(express, e);
        }
    }

    @Override
    public boolean is(String express) throws com.mz.jarboot.core.cmd.express.ExpressException {
        final Object ret = get(express);
        return null != ret && ret instanceof Boolean && (Boolean) ret;
    }

    @Override
    public Express bind(Object object) {
        this.bindObject = object;
        return this;
    }

    @Override
    public Express bind(String name, Object value) {
        context.put(name, value);
        return this;
    }

    @Override
    public Express reset() {
        context.clear();
        context.setClassResolver(CustomClassResolver.customClassResolver);
        // allow private field access
        context.setMemberAccess(MEMBER_ACCESS);
        return this;
    }
}
