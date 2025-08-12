package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.core.cmd.model.EnhancerAffectVO;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author majianzheng
 */
public class ViewRenderUtilTest {

    @Test
    public void testRenderEnhancerAffect() {
        EnhancerAffectVO affectVO = new EnhancerAffectVO(100, 2, 3, 4);
        String str = ViewRenderUtil.renderEnhancerAffect(affectVO);
        assertEquals("Affect(class count: 3 , method count: 2) cost in 100 ms, listenerId: 4\n", str);

        List<String> classDumpFiles = new ArrayList<>();
        classDumpFiles.add("file1");
        affectVO.setClassDumpFiles(classDumpFiles);
        List<String> methods = new ArrayList<>();
        methods.add("method1");
        affectVO.setMethods(methods);
        Throwable throwable = new JarbootException("test");
        affectVO.setThrowable(throwable);
        str = ViewRenderUtil.renderEnhancerAffect(affectVO);
        assertEquals("[dump: file1]\n" +
                "[Affect method: method1]\n" +
                "Affect(class count: 3 , method count: 2) cost in 100 ms, listenerId: 4\n" +
                "Enhance error! exception: io.github.majianzheng.jarboot.common.JarbootException: test\n", str);
    }
}
