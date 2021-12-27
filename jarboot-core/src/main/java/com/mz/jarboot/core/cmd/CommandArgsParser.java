package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.api.cmd.annotation.Argument;
import com.mz.jarboot.api.cmd.annotation.DefaultValue;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Option;
import com.mz.jarboot.core.utils.BasicTypeConvert;
import com.mz.jarboot.common.utils.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The command line param parser, command builder get param by annotations.
 * @author majianzheng
 */
@SuppressWarnings("all")
public class CommandArgsParser {
    private static final char QUOTATION1 = '\'';
    private static final char QUOTATION2 = '"';
    private static final char SPACE = ' ';

    private List<String> arguments = new ArrayList<>();
    private Map<String, List<String>> options = new LinkedHashMap<>();

    private Map<Integer, Argument> argumentMap = new HashMap<>();
    private Map<String, Option> optionMap = new HashMap<>();

    private Map<Method, Argument> argumentMethods = new HashMap<>();
    private Map<Method, Option> optionMethods = new HashMap<>();
    private List<String> splitedArgs;
    private Object command;
    public CommandArgsParser(String args, Object obj) {
        // 构建实例，解析方法注解
        this.init(obj);

        // 解析命令行
        splitedArgs = splitArgs(args.trim());
        this.doParse();
    }

    public CommandArgsParser(String[] args, Object obj) {
        this.init(obj);
        splitedArgs = Arrays.stream(args).collect(Collectors.toList());
        this.doParse();
    }

    public void postConstruct() {
        // 根据类的解析和命令行的解析填充实例成员变量
        this.doInitField();
    }

    public String[] getSplitedArgs() {
        return splitedArgs.toArray(new String[splitedArgs.size()]);
    }

    private void init(Object obj) {
        Class<?> cls = obj.getClass();
        Constructor<? extends AbstractCommand> constructor;
        try {
            command = obj;
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
            throw new JarbootException(e.getMessage(), e);
        }
    }

    private void doParse() {
        if (optionMap.isEmpty() && argumentMap.isEmpty()) {
            return;
        }
        Iterator<String> iter = splitedArgs.iterator();
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
                if (null == option && !optionMap.isEmpty()) {
                    throw new JarbootException("不支持的选项：" + op);
                }
                if (null != option && !option.flag()) {
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
                    if (null == argument && !argumentMap.isEmpty()) {
                        // 输入的参数多于了命令限定的数量
                        throw new JarbootException("无法识别的参数:" + s);
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

    public static List<String> splitArgs(String args) {
        ArrayList<String> argsList = new ArrayList<>();
        args = args.trim();
        final int invalidPos = -1;
        // 将传入的整个参数拆分，遇到空格拆分，将引号包裹的视为一个
        int preQuotation  = invalidPos;
        char preQuot = '"';
        int preChar = SPACE;
        // 使用快慢指针算法
        int slow =0, fast = 0;
        for (; fast < args.length(); ++fast) {
            char c = args.charAt(fast);
            if (QUOTATION1 == c || QUOTATION2 == c) {
                if (invalidPos == preQuotation) {
                    if (SPACE == preChar) {
                        //引号开始位置
                        preQuotation = fast;
                        preQuot = c;
                    }
                } else {
                    //判定前一个字符是否是转义符
                    if ('\\' != preChar && preQuot == c) {
                        //引号结束位置，获取引号内的字符串
                        String str = args.substring(preQuotation + 1, fast);
                        //todo 转义符替换
                        argsList.add(str);
                        slow = fast + 1;
                        preQuotation = invalidPos;
                    }
                }
            } else if (SPACE == c){
                if (invalidPos == preQuotation) {
                    if (SPACE != preChar) {
                        addArgsToList(args, slow, fast, argsList);
                    }
                    slow = fast + 1;
                }
            } else {
                // do nothing
            }
            preChar = c;
        }
        addArgsToList(args, slow, fast, argsList);
        return argsList;
    }

    private static void addArgsToList(String args, int slow, int fast, List<String> list) {
        if (slow == fast) {
            return;
        }
        String arg = args.substring(slow, fast);
        list.add(arg);
    }

    private void doInitField() {
        //set Argument
        argumentMethods.forEach(this::doInitArgumentField);
        //set Option
        optionMethods.forEach(this::doInitOptionField);
    }

    private void doInitArgumentField(Method method, Argument argument) {
        String arg = StringUtils.EMPTY;
        if (argument.index() < arguments.size()) {
            arg = arguments.get(argument.index());
        } else {
            //获取默认值
            DefaultValue defaultValue = method.getAnnotation(DefaultValue.class);
            //缺少参数时，检查是否必须
            if (argument.required() && null == defaultValue) {
                throw new JarbootException(formatParamError(argument.argName(), method));
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

        if ((null == values || values.isEmpty())) {
            DefaultValue defaultValue = method.getAnnotation(DefaultValue.class);
            if (null == defaultValue && option.required()) {
                throw new JarbootException(formatParamError(option.longName(), method));
            }
            // 默认值不为空
            if (null != defaultValue) {
                values = splitArgs(defaultValue.value());
                if (option.required() && null != values && values.isEmpty()) {
                    throw new JarbootException(formatParamError(option.longName(), method));
                }
            }
        }

        if (option.acceptMultipleValues()) {
            // 输入为List的
            callMethod(method, values);
            return;
        }
        String value = StringUtils.EMPTY;
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
            throw new JarbootException("命令定义错误，set方法应该存在一个参数！" + paramTypes.length);
        }
        Object val = BasicTypeConvert.convert(arg, paramTypes[0]);
        callMethod(method, val);
    }

    private void callMethod(Method method, Object value) {
        try {
            method.invoke(command, value);
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    private static String formatParamError(String name, Method method) {
        Description description = method.getAnnotation(Description.class);
        String desc = null == description ? StringUtils.EMPTY : description.value();
        return String.format("The argument '%s' is required, description: %s", name, desc);
    }
}
