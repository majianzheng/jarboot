package com.mz.jarboot.demo;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * jarboot启动的Java进程的demo示例
 * @author majianzheng
 */
@SuppressWarnings("all")
public class DemoServerApplication implements Runnable {
    private static final int INVALID_NUM = -1;
    private static final int FIB_FUNC = 1;
    private static final int POW_FUNC = 2;
    
    private JTextField execLimitInput;
    private JTextField execIntervalInput;
    private JTextField fibInput;
    private JTextField pow1Input;
    private JTextField pow2Input;
    final JProgressBar progressBar = new JProgressBar(0, 100);
    final JLabel costLabel = new JLabel("耗时：");
    final JLabel resultLabel = new JLabel();
    Thread runFuncThread = null;
    private volatile int func = 0;
    
    private DemoServerApplication() {
        JFrame mainFrame = new JFrame("Jarboot Demo Server —— 演示程序");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 400);
        mainFrame.setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new GridLayout(2, 1));
        
        // 简介
        String text = "使用Jarboot启动后，执行 stdout on 命令，开启程序输出流显示，"
                + "然后执行算法就可以在Jarboot的界面看到程序的输出内容。\n\n"
                + "首先请执行命令：stdout on    开启输出流实时显示到Jarboot的界面的功能\n"
                + "jad com.mz.jarboot.demo.DemoServerApplication        反编译命令\n"
                + "watch com.mz.jarboot.demo.DemoServerApplication fib        监控fib函数执行命令\n"
                + "trace com.mz.jarboot.demo.DemoServerApplication fib        追踪fib函数调用栈命令\n"
                + "watch com.mz.jarboot.demo.DemoServerApplication pow\ntrace com.mz.jarboot.demo.DemoServerApplication pow";
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
    
    public static void main(String[] args) {
        printBanner();
        new DemoServerApplication();
        log("Jarboot Demo Server started!");
    }
    
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
        System.out.println(text);
    }
    
    private String doFib(int limit, int interval) {
        log("开始执行【斐波那契数列】计算>>>");
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
        int c = 0;
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
    
    private void sleep(long n) {
        try {
            TimeUnit.MILLISECONDS.sleep(n);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void printBanner() {
        try (InputStream is = DemoServerApplication.class.getResourceAsStream("/banner.txt");
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);){
            for (;;) {
                String line = br.readLine();
                if (null == line) {
                    return;
                }
                log(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
    }
}
