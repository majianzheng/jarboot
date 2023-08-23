package io.github.majianzheng.jarboot.core.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.File;

/**
 * 
 * @author majianzheng
 *
 */
public class DecompilerTest {

    @Test
    public void test() {
        String dir = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

        File classFile = new File(dir, this.getClass().getName().replace('.', '/') + ".class");

        String code = Decompiler.decompile(classFile.getAbsolutePath(), null, true);

        System.err.println(code);

        Assertions.assertThat(code).contains("/*23*/         System.err.println(code);").contains("/*32*/         int i = 0;");
    }
}
