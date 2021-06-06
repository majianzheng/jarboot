import React, {memo} from "react";
import {BackTop, Typography} from "antd";
import {useIntl} from "umi";
import styles from '../index.less'

const {Title, Paragraph, Text} = Typography;

const JvmDoc: any = memo(() => {
    const intl = useIntl();
    return <>
        <Typography>
            <Title>jvm</Title>
            <Paragraph>
                <Text>{intl.formatMessage({id: 'JVM_DESC'})}</Text>
                <Title level={2}>{intl.formatMessage({id: 'USAGE_DEMO'})}</Title>
                <pre>
                    <div className={styles.docTable}>
                        jvm<br/>
    <table>
        <caption>RUNTIME</caption>
        <tbody>
        <tr>
            <td>MACHINE-NAME</td>
            <td>9867@admindeMBP.lan</td>
        </tr>
        <tr>
            <td>JVM-START-TIME</td>
            <td>1622896371609</td>
        </tr>
        <tr>
            <td>MANAGEMENT-SPEC-VERSION</td>
            <td>1.2</td>
        </tr>
        <tr>
            <td>SPEC-NAME</td>
            <td>Java Virtual Machine Specification</td>
        </tr>
        <tr>
            <td>SPEC-VENDOR</td>
            <td>1.8</td>
        </tr>
        <tr>
            <td>VM-NAME</td>
            <td>Java HotSpot(TM) 64-Bit Server VM</td>
        </tr>
        <tr>
            <td>VM-VENDOR</td>
            <td>Oracle Corporation</td>
        </tr>
        <tr>
            <td>VM-VERSION</td>
            <td>25.121-b13</td>
        </tr>
        <tr>
            <td>INPUT-ARGUMENTS</td>
            <td>[]</td>
        </tr>
        <tr>
            <td>CLASS-PATH</td>
            <td>/Users/user/jarboot/services/demo1-service/demo1-service.jar</td>
        </tr>
        <tr>
            <td>BOOT-CLASS-PATH</td>
            <td>/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/jre/lib/sunrsasign.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/jre/classes</td>
        </tr>
        <tr>
            <td>LIBRARY-PATH</td>
            <td>/Users/user/Library/Java/Extensions:/Library/Java/Extensions:/Network/Library/Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java:.</td>
        </tr>
        </tbody>
    </table>
    <table>
        <caption>CLASS-LOADING</caption>
        <tbody>
        <tr>
            <td>LOADED-CLASS-COUNT</td>
            <td>11146</td>
        </tr>
        <tr>
            <td>TOTAL-LOADED-CLASS-COUNT</td>
            <td>11148</td>
        </tr>
        <tr>
            <td>UNLOADED-CLASS-COUNT</td>
            <td>2</td>
        </tr>
        <tr>
            <td>IS-VERBOSE</td>
            <td>false</td>
        </tr>
        </tbody>
    </table>
    <table>
        <caption>COMPILATION</caption>
        <tbody>
        <tr>
            <td>NAME</td>
            <td>HotSpot 64-Bit Tiered Compilers</td>
        </tr>
        <tr>
            <td>TOTAL-COMPILE-TIME</td>
            <td>22792(ms)</td>
        </tr>
        </tbody>
    </table>
    <table>
        <caption>GARBAGE-COLLECTORS</caption>
        <tbody>
        <tr>
            <th></th>
            <th>collectionCount</th>
            <th>collectionTime (ms)</th>
        </tr>
        <tr>
            <td>PS Scavenge</td>
            <td>14</td>
            <td>157</td>
        </tr>
        <tr>
            <td>PS MarkSweep</td>
            <td>3</td>
            <td>386</td>
        </tr>
        </tbody>
    </table>
    <table>
        <caption>MEMORY-MANAGERS</caption>
        <tbody>
        <tr>
            <td>CodeCacheManager</td>
            <td>[Code Cache]</td>
        </tr>
        <tr>
            <td>Metaspace Manager</td>
            <td>[Metaspace, Compressed Class Space]</td>
        </tr>
        <tr>
            <td>PS Scavenge</td>
            <td>[PS Eden Space, PS Survivor Space]</td>
        </tr>
        <tr>
            <td>PS MarkSweep</td>
            <td>[PS Eden Space, PS Survivor Space, PS Old Gen]</td>
        </tr>
        </tbody>
    </table>
    <table>
        <caption>MEMORY</caption>
        <tbody>
        <tr>
            <td>HEAP-MEMORY-USAGE</td>
            <td>
                <span>init : 134217728</span><br/>
                <span>used : 79049568</span><br/>
                <span>committed : 813694976</span><br/>
                <span>max : 1908932608</span>
            </td>
        </tr>
        <tr>
            <td>NO-HEAP-MEMORY-USAGE</td>
            <td>
                <span>init : 2555904</span><br/>
                <span>used : 69758848</span><br/>
                <span>committed : 87097344</span><br/>
                <span>max : -1</span>
            </td>
        </tr>
        </tbody>
    </table>

    <table>
        <caption>OPERATING-SYSTEM</caption>
        <tbody>
        <tr>
            <td>OS</td>
            <td>Mac OS X</td>
        </tr>
        <tr>
            <td>ARCH</td>
            <td>x86_64</td>
        </tr>
        <tr>
            <td>PROCESSORS-COUNT</td>
            <td>4</td>
        </tr>
        <tr>
            <td>VERSION</td>
            <td>10.16</td>
        </tr>
        <tr>
            <td>LOAD-AVERAGE</td>
            <td>5.1181640625</td>
        </tr>
        </tbody>
    </table>
    <table>
        <caption>THREAD</caption>
        <tbody>
        <tr>
            <td>COUNT</td>
            <td>24</td>
        </tr>
        <tr>
            <td>DAEMON-COUNT</td>
            <td>19</td>
        </tr>
        <tr>
            <td>PEAK-COUNT</td>
            <td>26</td>
        </tr>
        <tr>
            <td>STARTED-COUNT</td>
            <td>31</td>
        </tr>
        <tr>
            <td>DEADLOCK-COUNT</td>
            <td>0</td>
        </tr>
        </tbody>
    </table>
    <table>
        <caption>FILE-DESCRIPTOR</caption>
        <tbody>
        <tr>
            <td>MAX-FILE-DESCRIPTOR-COUNT</td>
            <td>10240</td>
        </tr>
        <tr>
            <td>OPEN-FILE-DESCRIPTOR-COUNT</td>
            <td>74</td>
        </tr>
        </tbody>
    </table>
</div>
                </pre>
            </Paragraph>
        </Typography>
        <BackTop/>
    </>;
});

export default JvmDoc
