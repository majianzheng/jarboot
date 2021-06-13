package com.mz.jarboot.core.cmd;

import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.StringUtils;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

/**
 * The command line param parser, command builder get param by annotations.
 * @author jianzhengma
 */
public class CommandArgsParser {
    private List<String> arguments = new ArrayList<>();
    private Map<String, List<String>> options = new LinkedHashMap<>();
    public CommandArgsParser(String args) {
        this.doParse(args.trim());
    }
    private void doParse(String args) {
        Iterator<String> iter = Arrays.stream(args.split(" ")).iterator();
        String preOp = null;
        while (iter.hasNext()) {
            String s = iter.next().trim();
            if (s.startsWith("-")) {
                String op = StringUtils.trimLeadingCharacter(s, '-');
                options.put(op, new ArrayList<>());
                preOp = op;
            } else {
                //可能是参数，也可能是option的值
                if (null == preOp) {
                    //参数
                    arguments.add(s);
                } else {
                    //是上一个option的值，填充值
                    options.get(preOp).add(s);
                    preOp = null;
                }
            }
        }
    }
    public String getArgument(int index) {
        if (index >= arguments.size()) {
            return CoreConstant.EMPTY_STRING;
        }
        return arguments.get(index);
    }

    public String getOptionValue(String shortName, String longName) {
        List<String> list = getMultiOptionValue(shortName, longName);
        return (CollectionUtils.isEmpty(list)) ? CoreConstant.EMPTY_STRING : list.get(0);
    }

    public List<String> getMultiOptionValue(String shortName, String longName) {
        List<String> list1 = options.get(shortName);
        List<String> list2 = options.get(longName);
        if (CollectionUtils.isNotEmpty(list2)) {
            list1.addAll(list2);
        }
        return list1;
    }

    public boolean hasOption(String shortName, String longName) {
        return options.containsKey(shortName) || options.containsKey(longName);
    }
}
