package io.github.majianzheng.jarboot.text;

import io.github.majianzheng.jarboot.text.lang.LangRenderUtil;

/**
 * 
 * @author duanling 2015年12月3日 上午11:38:48
 *
 */
public class HighlightExample {

    public static void main(String[] args) {

        String code = "int a = 123; \nString s = \"sssss\";";

        System.out.println(LangRenderUtil.render(code));
    }

}
