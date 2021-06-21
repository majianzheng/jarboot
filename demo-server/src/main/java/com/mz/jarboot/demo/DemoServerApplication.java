package com.mz.jarboot.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * jarboot启动的Java进程的demo示例
 * @author jianzhengma
 */
@RequestMapping(value = "/demo-server", method ={RequestMethod.GET, RequestMethod.POST})
@Controller
@SpringBootApplication
public class DemoServerApplication {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public static void main(String[] args) {
        SpringApplication.run(DemoServerApplication.class, args);
    }

    @GetMapping(value="/getUser")
    @ResponseBody
    public String getUser() {
        logger.info(">>getUser called.");
        return "jarboot-admin";
    }

    @GetMapping(value="/add")
    @ResponseBody
    public int add(int a, int b) {
        logger.info(">>add called.");
        return a + b;
    }
}
