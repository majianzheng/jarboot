package io.github.majianzheng.jarboot.demo;

import io.github.majianzheng.jarboot.api.AgentService;
import io.github.majianzheng.jarboot.api.JarbootFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * jarboot启动的Java进程的demo示例
 * @author majianzheng
 */
@SuppressWarnings({"squid:S106", "squid:S1181"})
public class DemoServerApplication implements Runnable {
    private static final int INVALID_NUM = -1;
    private static final int FIB_FUNC = 1;
    private static final int POW_FUNC = 2;
    private static AgentService agentService = null;

    private final JTextField execLimitInput;
    private final JTextField execIntervalInput;
    private final JTextField fibInput;
    private final JTextField pow1Input;
    private final JTextField pow2Input;
    final JProgressBar progressBar = new JProgressBar(0, 100);
    final JLabel costLabel = new JLabel("耗时：");
    final JLabel resultLabel = new JLabel();
    Thread runFuncThread = null;
    private volatile int func = 0;
    
    public static void main(String[] args) {
        //打印banner
        printBanner();
        String ver = System.getenv("JARBOOT_DOCKER");
        if (null == ver || ver.isEmpty()) {
            //启动界面
            new DemoServerApplication();
            log("\033[4;95m启动进度\033[0m模拟中\033[5m...\033[0m");
            finish();
        } else {
            //docker模式下
            log("当前正在使用\033[1;36mDocker\033[0m，\033[4;95m启动进度\033[0m模拟中\033[5m...\033[0m");
            finish();
            try {
                TimeUnit.DAYS.sleep(1);
            } catch (InterruptedException e) {
                log(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void notice(String msg, String sessionId) {
        if (null != agentService) {
            agentService.noticeInfo(msg, sessionId);
        }
    }

    private static void finish() {
        final int max = 50;
        final String back = "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b" +
                "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b";
        //模拟启动进度
        StringBuilder percent = new StringBuilder(1024);
        for (int i = 0; i < max; ++i) {
            int color = (52 + i * 3);
            percent
                    .append("\033[48;5;")
                    .append(color)
                    .append("m \033[0m");
            StringBuilder sb = new StringBuilder(1024);
            if (i > 0) {
                sb.append(back);
            }
            sb
                    .append(percent.toString())
                    .append("\033[2;48;5;239m");
            for (int j = i + 1; j < max; ++j) {
                sb.append(' ');
            }
            sb
                    .append("\033[0m \033[1;38;5;")
                    .append(color)
                    .append("m")
                    .append((i + 1) * 2)
                    .append("%\033[0m");
            System.out.print(sb.toString());
            sleep(188);
        }

        //启动完成可主动调用setStarted通知Jarboot完成，否则将会在没有控制台输出的一段时间后才判定为完成。
        try {
            agentService = JarbootFactory.createAgentService();
            agentService.setStarted();
            agentService.noticeInfo("启动Demo成功！", null);
        } catch (Throwable e) {
            //ignore
        }
    }

    @SuppressWarnings("PMD.AvoidManuallyCreateThreadRule")
    private void callFunc(int f) {
        if (null != runFuncThread && runFuncThread.isAlive()) {
            log("正在执行中，请稍后...");
            return;
        }
        this.func = f;
        runFuncThread = new Thread(this);
        runFuncThread.start();
    }

    public static void log(String text) {
        System.out.println("[\033[32mINFO\033[0m] " + text);
    }
    
    private String doFib(int limit, int interval) {
        log("开始执行【斐波那契数列】计算>>>");
        System.out.println();
        String text = fibInput.getText();
        log("输入值为：" + text);
        int n;
        try {
            n = Integer.parseInt(text);
        } catch (Exception e) {
            log("转换错误，必须为整数值，执行中止！");
            return "输入参数错误";
        }
        int result = 0;
        for (int i = 0; i < limit; ++i) {
            result = fib(n);
            progressBar.setValue(i);
            if (interval > 0) {
                sleep(interval);
            }
        }
        return String.valueOf(result);
    }
    
    private String doPow(int limit, int interval) {
        log("开始执行【数值整数次方】计算>>>");
        String text1 = pow1Input.getText();
        String text2 = pow2Input.getText();
        log("输入值为：" + text1 + ", " + text2);
        double a1;
        int c;
        try {
            a1 = Double.parseDouble(text1);
            c = Integer.parseInt(text2);
        } catch (Exception e) {
            log("转换错误，必须为数值，执行中止！");
            return "输入参数错误";
        }
        double result = 0;
        
        for (int i = 0; i < limit; ++i) {
            result = pow(a1, c);
            progressBar.setValue(i);
            if (interval > 0) {
                sleep(interval);
            }
        }
        return String.valueOf(result);
    }
    
    public static int fib(int n) {
        int a = 0;
        int b = 1;
        int sum;
        for (int i = 0; i < n; ++i) {
            sum = (a + b) % 100000007;
            a = b;
            b = sum;
        }
        return a;
    }
    
    public static double pow(double x, int n) {
        if (0 == x) {
            return 0;
        }
        long b = n;
        double res = 1.0;
        if (b < 0) {
            x = 1 / x;
            b = -b;
        }
        while (b > 0) {
            if ((b & 1) == 1) {
                res *= x;
            }
            x *= x;
            b >>= 1;
        }
        return res;
    }
    
    private int getLimit() {
        String text = execLimitInput.getText();
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return INVALID_NUM;
        }
    }
    
    private int getInterval() {
        String text = execIntervalInput.getText();
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return INVALID_NUM;
        }
    }
    
    private static void sleep(long n) {
        try {
            TimeUnit.MILLISECONDS.sleep(n);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void printBanner() {
        try (InputStream is = DemoServerApplication.class.getResourceAsStream("/banner.txt");
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)){
            for (;;) {
                String line = br.readLine();
                if (null == line) {
                    return;
                }
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
    
    @Override
    public void run() {
        resultLabel.setText("");
        costLabel.setText("");
        int limit = getLimit();
        int interval = getInterval();
        if (limit < 1) {
            log("执行次数必须为大于0的整数");
            return;
        }
        if (interval < 0) {
            log("间隔必须为大于0的整数");
            return;
        }
        log("开始执行，次数：" + limit + ", 间隔：" + interval + " ms");
        String result = "";
        progressBar.setMaximum(limit - 1);
        progressBar.setValue(0);
        long begin = System.currentTimeMillis();
        switch (func) {
            case FIB_FUNC:
                result = doFib(limit, interval);
                break;
            case POW_FUNC:
                result = doPow(limit, interval);
                break;
            default:
                log("不支持的函数：" + func);
                break;
        }
        long cost = System.currentTimeMillis() - begin;
        if (!result.isEmpty()) {
            String rlt = "结果：" + result;
            log(rlt);
            resultLabel.setText(rlt);
        }
        String costStr = String.format("耗时：%d ms", cost);
        log("计算完成，" + costStr);
        costLabel.setText(costStr);
        func = INVALID_NUM;
        if (null != agentService) {
            agentService.noticeInfo("计算完成", null);
        }
    }

    private DemoServerApplication() {
        JFrame mainFrame = new JFrame("Jarboot Demo Server —— 演示程序");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 400);
        mainFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridLayout(2, 1));

        // 简介
        String text = "使用Jarboot启动后，可执行测试的算法\n"
                + "输出流默认会实时显示到Jarboot的界面，可使用stdout off命令关闭\n"
                + "help  查看支持的命令列表\n"
                + "jad io.github.majianzheng.jarboot.demo.cmd.PowCommandProcessor        反编译命令\n"
                + "watch io.github.majianzheng.jarboot.demo.DemoServerApplication fib        监控fib函数执行命令\n"
                + "trace io.github.majianzheng.jarboot.demo.DemoServerApplication fib        追踪fib函数调用栈命令\n"
                + "watch io.github.majianzheng.jarboot.demo.DemoServerApplication pow\n"
                + "trace io.github.majianzheng.jarboot.demo.DemoServerApplication pow";
        JTextArea desc = new JTextArea(text);
        desc.setEditable(false);
        desc.setCursor(null);
        desc.setOpaque(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setColumns(50);
        mainPanel.add(desc);

        JPanel execPanel = new JPanel(new GridLayout(5, 4, 5, 10));
        //第一行，执行次数、
        JLabel execLimitLabel = new JLabel("算法执行次数：", SwingConstants.CENTER);
        execLimitInput = new JTextField("1000", 15);
        JLabel execIntervalLabel = new JLabel("算法执行间隔(ms)：", SwingConstants.CENTER);
        execIntervalInput = new JTextField("3", 15);

        execPanel.add(execLimitLabel);
        execPanel.add(execLimitInput);
        execPanel.add(execIntervalLabel);
        execPanel.add(execIntervalInput);
        mainPanel.add(execPanel);

        //第二行算法按钮
        JButton btn1 = new JButton("斐波那契数列");
        btn1.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callFunc(FIB_FUNC);
            }
        });
        fibInput = new JTextField("100", 40);
        execPanel.add(btn1);
        execPanel.add(fibInput);
        execPanel.add(new JLabel());
        execPanel.add(new JLabel());


        JButton btn2 = new JButton("整数次方计算");
        btn2.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callFunc(POW_FUNC);
            }
        });
        pow1Input = new JTextField("2", 20);
        pow2Input = new JTextField("14", 20);
        execPanel.add(btn2);
        execPanel.add(pow1Input);
        execPanel.add(pow2Input);
        execPanel.add(new JLabel());

        //进度条
        JLabel progressLabel = new JLabel("执行进度：", SwingConstants.CENTER);
        execPanel.add(progressLabel);
        execPanel.add(progressBar);
        //耗时
        execPanel.add(costLabel);
        //结果
        execPanel.add(resultLabel);

        mainFrame.setContentPane(mainPanel);
        mainFrame.setVisible(true);
    }
}
