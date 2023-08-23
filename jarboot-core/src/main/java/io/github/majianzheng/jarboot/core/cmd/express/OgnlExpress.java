package io.github.majianzheng.jarboot.core.cmd.express;

import io.github.majianzheng.jarboot.core.utils.LogUtils;
import ognl.*;
import org.slf4j.Logger;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class OgnlExpress implements Express {
    private static final MemberAccess MEMBER_ACCESS = new DefaultMemberAccess(true);
    private static final Logger logger = LogUtils.getLogger();

    private Object bindObject;
    private final OgnlContext context;

    public OgnlExpress() {
        this(CustomClassResolver.CUSTOM_CLASS_RESOLVER);
    }

    public OgnlExpress(ClassResolver classResolver) {
        context = new OgnlContext();
        context.setClassResolver(classResolver);
        // allow private field access
        context.setMemberAccess(MEMBER_ACCESS);
    }

    @SuppressWarnings("squid:S2139")
    @Override
    public Object get(String express) throws ExpressException {
        try {
            return Ognl.getValue(express, context, bindObject);
        } catch (Exception e) {
            logger.error("Error during evaluating the expression:", e);
            throw new ExpressException(express, e);
        }
    }

    @Override
    public boolean is(String express) throws ExpressException {
        final Object ret = get(express);
        return ret instanceof Boolean && (Boolean) ret;
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
        context.setClassResolver(CustomClassResolver.CUSTOM_CLASS_RESOLVER);
        // allow private field access
        context.setMemberAccess(MEMBER_ACCESS);
        return this;
    }
}
