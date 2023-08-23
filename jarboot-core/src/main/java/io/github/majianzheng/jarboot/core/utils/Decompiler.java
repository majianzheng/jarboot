package io.github.majianzheng.jarboot.core.utils;

import io.github.majianzheng.jarboot.common.Pair;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns.LineNumberMapping;

import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author majianzheng
 *
 */
public class Decompiler {

    public static String decompile(String classFilePath, String methodName) {
        return decompile(classFilePath, methodName, false);
    }

    public static String decompile(String classFilePath, String methodName, boolean hideUnicode) {
        return decompile(classFilePath, methodName, hideUnicode, true);
    }

    @SuppressWarnings("java:S3776")
    public static Pair<String, NavigableMap<Integer, Integer>> decompileWithMappings(String classFilePath,
            String methodName, boolean hideUnicode, boolean printLineNumber) {
        final StringBuilder sb = new StringBuilder(8192);

        final NavigableMap<Integer, Integer> lineMapping = new TreeMap<>();

        OutputSinkFactory mySink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                return Arrays.asList(SinkClass.STRING, SinkClass.DECOMPILED, SinkClass.DECOMPILED_MULTIVER,
                        SinkClass.EXCEPTION_MESSAGE, SinkClass.LINE_NUMBER_MAPPING);
            }

            @SuppressWarnings({"squid:S1604"})
            @Override
            public <T> Sink<T> getSink(final SinkType sinkType, final SinkClass sinkClass) {
                return new Sink<T>() {
                    @Override
                    public void write(T sinkable) {
                        // skip message like: Analysing type demo.MathGame
                        if (sinkType == SinkType.PROGRESS) {
                            return;
                        }
                        if (sinkType == SinkType.LINENUMBER) {
                            LineNumberMapping mapping = (LineNumberMapping) sinkable;
                            NavigableMap<Integer, Integer> classFileMappings = mapping.getClassFileMappings();
                            NavigableMap<Integer, Integer> mappings = mapping.getMappings();
                            if (classFileMappings != null && mappings != null) {
                                for (Entry<Integer, Integer> entry : mappings.entrySet()) {
                                    Integer srcLineNumber = classFileMappings.get(entry.getKey());
                                    lineMapping.put(entry.getValue(), srcLineNumber);
                                }
                            }
                            return;
                        }
                        sb.append(sinkable);
                    }
                };
            }
        };

        HashMap<String, String> options = new HashMap<>(16);
        /*
          @see org.benf.cfr.reader.util.MiscConstants.Version.getVersion() Currently,
               the cfr version is wrong. so disable show cfr version.
         */
        options.put("showversion", "false");
        options.put("hideutf", String.valueOf(hideUnicode));
        options.put("trackbytecodeloc", "true");
        if (!StringUtils.isBlank(methodName)) {
            options.put("methodname", methodName);
        }

        CfrDriver driver = new CfrDriver.Builder().withOptions(options).withOutputSink(mySink).build();
        List<String> toAnalyse = new ArrayList<>();
        toAnalyse.add(classFilePath);
        driver.analyse(toAnalyse);

        String resultCode = sb.toString();
        if (printLineNumber && !lineMapping.isEmpty()) {
            resultCode = addLineNumber(resultCode, lineMapping);
        }

        return Pair.make(resultCode, lineMapping);
    }

    public static String decompile(String classFilePath, String methodName, boolean hideUnicode,
            boolean printLineNumber) {
        return decompileWithMappings(classFilePath, methodName, hideUnicode, printLineNumber).getFirst();
    }

    @SuppressWarnings("PMD.UndefineMagicConstantRule")
    private static String addLineNumber(String src, Map<Integer, Integer> lineMapping) {
        int maxLineNumber = 0;
        for (Integer value : lineMapping.values()) {
            if (value != null && value > maxLineNumber) {
                maxLineNumber = value;
            }
        }

        String formatStr = "/*%2d*/ ";
        String emptyStr = "       ";

        StringBuilder sb = new StringBuilder();

        List<String> lines = StringUtils.toLines(src);

        if (maxLineNumber >= 100) {
            formatStr = "/*%3d*/ ";
            emptyStr = "        ";
        } else if (maxLineNumber >= 1000) {
            formatStr = "/*%4d*/ ";
            emptyStr = "         ";
        }

        int index = 0;
        for (String line : lines) {
            Integer srcLineNumber = lineMapping.get(index + 1);
            if (srcLineNumber != null) {
                sb.append(String.format(formatStr, srcLineNumber));
            } else {
                sb.append(emptyStr);
            }
            sb.append(line).append("\n");
            index++;
        }

        return sb.toString();
    }

    private Decompiler() {}
}
