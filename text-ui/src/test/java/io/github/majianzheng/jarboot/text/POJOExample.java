package io.github.majianzheng.jarboot.text;

import io.github.majianzheng.jarboot.text.util.RenderUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class POJOExample {

    public class Student {
        String name;
        int age;

        public Student(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Test
    public void test1(){
        
        List<Student> list = new ArrayList<Student>();
        
        for(int i = 0; i < 10; ++i){
            list.add(new Student("name" + i, 10 + i));
        }
        
        System.err.println(RenderUtil.render(list));
    }
}
