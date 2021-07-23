package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.MzException;
import com.mz.jarboot.core.cmd.annotation.Argument;
import com.mz.jarboot.core.cmd.annotation.DefaultValue;
import com.mz.jarboot.core.cmd.annotation.Description;
import com.mz.jarboot.core.cmd.annotation.Option;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.BasicTypeConvert;
import com.mz.jarboot.core.utils.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * The command line param parser, command builder get param by annotations.
 * @author jianzhengma
 */
@SuppressWarnings("all")
public class CommandArgsParser {
    private List<String> arguments = new ArrayList<>();
    private Map<String, List<String>> options = new LinkedHashMap<>();

    private Map<Integer, Argument> argumentMap = new HashMap<>();
    private Map<String, Option> optionMap = new HashMap<>();

    private Map<Method, Argument> argumentMethods = new HashMap<>();
    private Map<Method, Option> optionMethods = new HashMap<>();
    private AbstractCommand command;
    public CommandArgsParser(String args, Class<? extends AbstractCommand> cls) {
        // 构建实例，解析方法注解
        this.init(cls);

        // 解析命令行
        this.doParse(args.trim());

        // 根据类的解析和命令行的解析填充实例成员变量
        this.doInitField();
    }

    public AbstractCommand getCommand() {
        return this.command;
    }

    private void init(Class<? extends AbstractCommand> cls) {
        Constructor<? extends AbstractCommand> constructor;
        try {
            constructor = cls.getConstructor();
            command = constructor.newInstance();
            Method[] methods = cls.getMethods();
            for (Method m : methods) {
                Argument argument = m.getAnnotation(Argument.class);
                Option option = m.getAnnotation(Option.class);
                if (null != argument) {
                    argumentMethods.put(m, argument);
                    argumentMap.put(argument.index(), argument);
                }
                if (null != option) {
                    optionMap.put(option.shortName(), option);
                    optionMap.put(option.longName(), option);
                    optionMethods.put(m, option);
                }
            }
        } catch (Exception e) {
            throw new MzException(e.getMessage(), e);
        }
    }

    private void doParse(String args) {
        Iterator<String> iter = Arrays.stream(args.split(" ")).iterator();
        Option preOp = null;
        while (iter.hasNext()) {
            String s = iter.next().trim();
            if (StringUtils.isEmpty(s)) {
                continue;
            }
            if (s.startsWith("-")) {
                preOp = null;
                String op = StringUtils.trimLeadingCharacter(s, '-');
                //获取option配置，判定是否flag
                Option option = optionMap.getOrDefault(op, null);
                if (null == option) {
                    throw new MzException("不支持的选项：" + op);
                }
                if (!option.flag()) {
                    preOp = option;
                }
                List<String> opValueList = new ArrayList<>();
                options.put(option.shortName(), opValueList);
            } else {
                //可能是参数，也可能是option的值
                if (null == preOp) {
                    //参数
                    int index = arguments.size();
                    Argument argument = argumentMap.getOrDefault(index, null);
                    if (null == argument) {
                        // 输入的参数多于了命令限定的数量
                        throw new MzException("无法识别的参数:" + s);
                    }
                    arguments.add(s);
                } else {
                    //是上一个option的值，填充值
                    options.get(preOp.shortName()).add(s);
                    if (!preOp.acceptMultipleValues()) {
                        preOp = null;
                    }
                }
            }
        }
    }

    private void doInitField() {
        //set Argument
        argumentMethods.forEach(this::doInitArgumentField);
        //set Option
        optionMethods.forEach(this::doInitOptionField);
    }

    private void doInitArgumentField(Method method, Argument argument) {
        String arg = CoreConstant.EMPTY_STRING;
        if (argument.index() < arguments.size()) {
            arg = arguments.get(argument.index());
        } else {
            //获取默认值
            DefaultValue defaultValue = method.getAnnotation(DefaultValue.class);
            //缺少参数时，检查是否必须
            if (argument.required() && null == defaultValue) {
                throw new MzException(formatParamError(argument.argName(), method));
            }
            if (null != defaultValue) {
                arg = defaultValue.value();
            }
        }
        //类型转换
        callSetMethod(method, arg);
    }

    private void doInitOptionField(Method method, Option option) {
        List<String> values = options.getOrDefault(option.shortName(), null);
        if (option.flag()) {
            callMethod(method, null != values);
            return;
        }

        if ((null == values || values.isEmpty()) && option.required()) {
            DefaultValue defaultValue = method.getAnnotation(DefaultValue.class);
            if (null == defaultValue) {
                throw new MzException(formatParamError(option.longName(), method));
            }
            // 默认值不为空
            values = Arrays.asList(defaultValue.value().split(" "));
            if (values.isEmpty()) {
                throw new MzException(formatParamError(option.longName(), method));
            }
        }

        if (option.acceptMultipleValues()) {
            // 输入为List的
            callMethod(method, values);
            return;
        }
        String value = "";
        if (null != values && !values.isEmpty()) {
            value = values.get(0);
        }
        callSetMethod(method, value);
    }

    private void callSetMethod(Method method, String arg) {
        if (StringUtils.isEmpty(arg)) {
            return;
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        if (1 != paramTypes.length) {
            throw new MzException("命令定义错误，set方法应该存在一个参数！" + paramTypes.length);
        }
        Object val = BasicTypeConvert.convert(arg, paramTypes[0]);
        callMethod(method, val);
    }

    private void callMethod(Method method, Object value) {
        try {
            method.invoke(command, value);
        } catch (Exception e) {
            throw new MzException(e.getMessage(), e);
        }
    }

    private static String formatParamError(String name, Method method) {
        Description description = method.getAnnotation(Description.class);
        String desc = null == description ? CoreConstant.EMPTY_STRING : description.value();
        return String.format("The argument '%s' is required, description: %s", name, desc);
    }
}
