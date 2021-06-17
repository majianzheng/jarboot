import React, {memo} from "react";
import {BackTop, Typography} from "antd";
import {useIntl} from "umi";
import {JarBootConst} from "@/common/JarBootConst";
import {CodeEditor} from "@/components";

const { Title, Paragraph, Text } = Typography;

const JadDoc: any = memo(() => {
    const intl = useIntl();
    const code = `jad com.mz.jarboot.demo.DemoServerApplication\n\n/*
 * Decompiled with CFR.
 *
 * Could not load the following classes:
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.stereotype.Controller
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestMethod
 *  org.springframework.web.bind.annotation.ResponseBody
 */
package com.mz.jarboot.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping(value={"/demo-server"}, method={RequestMethod.GET, RequestMethod.POST})
@Controller
@SpringBootApplication
public class DemoServerApplication {
    @GetMapping(value={"/getUser"})
    @ResponseBody
    public String getUser() {
        return "jarboot-admin";
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoServerApplication.class, args);
    }

    @GetMapping(value={"/add"})
    @ResponseBody
    public int add(int a, int b) {
        return a + b;
    }
}

`;
    return <>
        <Typography>
            <Title>jad</Title>
            <Paragraph>
                <Text>{intl.formatMessage({id: 'JAD_DESC'})}</Text>
                <Title level={2}>{intl.formatMessage({id: 'USAGE_DEMO'})}</Title>
                <CodeEditor height={JarBootConst.PANEL_HEIGHT}
                            readOnly={true}
                            source={code}/>
            </Paragraph>
        </Typography>
        <BackTop/>
    </>;
});

export default JadDoc
