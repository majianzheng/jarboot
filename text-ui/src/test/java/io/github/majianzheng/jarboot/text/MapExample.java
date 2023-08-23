package io.github.majianzheng.jarboot.text;

import io.github.majianzheng.jarboot.text.renderers.MapRenderer;
import io.github.majianzheng.jarboot.text.util.RenderUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapExample {

    @Test
    public void test() {
        List<Map<?, ?>> mapList = new ArrayList<Map<?, ?>>();

        Map<String, String> student1 = new HashMap<String, String>();
        student1.put("name", "tom");
        student1.put("age", "18");
        student1.put("email", "tom@test.com");

        Map<String, String> student2 = new HashMap<String, String>();
        student2.put("name", "hello");
        student2.put("age", "18");
        student2.put("email", "hello@test.com");

        Map<String, String> student3 = new HashMap<String, String>();
        student3.put("name", "world");
        student3.put("age", "18");
        student3.put("email", "world@test.com");

        mapList.add(student1);
        mapList.add(student2);
        mapList.add(student3);

        String result = RenderUtil.render(mapList.iterator(), new MapRenderer());
        System.out.println(result);
    }
}
