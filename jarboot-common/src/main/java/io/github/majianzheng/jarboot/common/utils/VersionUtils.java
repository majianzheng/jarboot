package io.github.majianzheng.jarboot.common.utils;

import java.io.InputStream;
import java.util.Comparator;
import java.util.Objects;
import java.util.Properties;

/**
 * Version utils.
 *
 * @author majianzheng
 */
@SuppressWarnings({"squid:S1444", "squid:ClassVariableVisibilityCheck"})
public class VersionUtils {
    
    public static String version;
    
    private static String clientVersion;
    
    /**
     * current version.
     */
    public static final String VERSION_PLACEHOLDER = "${project.version}";
    
    private static final String NACOS_VERSION_FILE = "jarboot-version.txt";
    
    static {
        try (InputStream in = VersionUtils.class.getClassLoader().getResourceAsStream(NACOS_VERSION_FILE)) {
            Properties props = new Properties();
            props.load(in);
            String val = props.getProperty("version");
            if (val != null && !VERSION_PLACEHOLDER.equals(val)) {
                version = val;
                clientVersion = "Jarboot:v" + VersionUtils.version;
            }
        } catch (Exception e) {
            //ignore
        }
    }
    
    private static final Comparator<String> STRING_COMPARATOR = String::compareTo;
    
    /**
     * compare two version who is latest.
     *
     * @param versionA version A, like x.y.z(-beta)
     * @param versionB version B, like x.y.z(-beta)
     * @return compare result
     */
    public static int compareVersion(final String versionA, final String versionB) {
        final String[] sA = versionA.split("\\.");
        final String[] sB = versionB.split("\\.");
        int expectSize = 3;
        if (sA.length != expectSize || sB.length != expectSize) {
            throw new IllegalArgumentException("version must be like x.y.z(-beta)");
        }
        int first = Objects.compare(sA[0], sB[0], STRING_COMPARATOR);
        if (first != 0) {
            return first;
        }
        int second = Objects.compare(sA[1], sB[1], STRING_COMPARATOR);
        if (second != 0) {
            return second;
        }
        return Objects.compare(sA[2].split("-")[0], sB[2].split("-")[0], STRING_COMPARATOR);
    }
    
    public static String getFullClientVersion() {
        return clientVersion;
    }
    private VersionUtils() {}
}
