import React, {memo} from "react";
import styles from "./index.less";

const JarbootDesc = memo(() => {

    return <div className={styles.leftDesc}>
        <div className={styles.jarbootName}>Jarboot</div>
        <div className={styles.jarbootDescription}>
            an easy-to-use Java process starter, manage, monitor and debug a series of Java instance.
        </div>
    </div>
});

export default JarbootDesc;
