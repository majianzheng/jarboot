package com.mz.jarboot.core;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.*;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.HttpUtils;
import com.mz.jarboot.core.utils.StringUtils;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author majianzheng
 */
public class Jarboot {
    private static final String PID_ARG = "pid";
    private static final String HOST_ARG = "host";
    private static final String CMD_ARG = "command";
    private static final String OPTION_PRE = "-";
    private static final String JARBOOT_HOST_ENV = "JARBOOT_HOST";

    /** Jarboot 服务地址 */
    private String host = "127.0.0.1:9899";
    /** 进程PID */
    private String pid;
    /** 启动Java进程命令行 */
    private List<String> command;
    /** 显示帮助信息 */
    private Boolean help;
    /** 是否是同步执行命令 */
    private Boolean sync;
    /** 传入的参数 */
    private final String[] args;
    /** Jarboot安装目录 */
    private String jarbootHome;
    /** 待填充的字段 */
    private String field;
    /** 是否未初始化成功 */
    private boolean notInitialized = true;

    private Jarboot(String [] args) {
        this.args = args;
        initJarbootHome();
        String h = System.getenv(JARBOOT_HOST_ENV);
        if (!StringUtils.isEmpty(h)) {
            this.host = h;
        }
        try {
            initArgs();
            notInitialized = false;
        } catch (JarbootException e) {
            printHelp();
            AnsiLog.error(e.getMessage());
        }
    }

    private void initArgs() {
        if (null == this.args || this.args.length <= 0) {
            return;
        }
        HashSet<String> optionHash = new HashSet<>();
        for (String arg : this.args) {

            switch (arg) {
                case "-pid":
                case "--pid":
                    verifyField(PID_ARG, optionHash);
                    break;
                case "-h":
                case "-host":
                case "--host":
                    verifyField(HOST_ARG, optionHash);
                    break;
                case "-sync":
                case "--sync":
                    verifyField(CMD_ARG, optionHash);
                    this.sync = true;
                    command = new ArrayList<>();
                    break;
                case "-async":
                case "--async":
                    verifyField(CMD_ARG, optionHash);
                    command = new ArrayList<>();
                    break;
                case "-help":
                case "--help":
                    this.help = true;
                    break;
                default:
                    setField(arg);
                    break;
            }
        }
    }

    private void setField(String arg) {
        if (null == field) {
            if (arg.startsWith(OPTION_PRE)) {
                throw new JarbootException(String.format("Not supported option: \"-%s\".", arg));
            }
            return;
        }
        switch (field) {
            case PID_ARG:
                this.pid = arg;
                field = null;
                break;
            case HOST_ARG:
                this.host = arg;
                field = null;
                break;
            case CMD_ARG:
                command.add(arg);
                break;
            default:
                break;
        }
    }

    private void verifyField(String argType, HashSet<String> optionHash) {
        if (optionHash.contains(argType)) {
            throw new JarbootException(String.format("Multi \"%s\" options", argType));
        }
        if (null != field) {
            throw new JarbootException(String.format("The argument '%s' is required", field));
        }
        optionHash.add(argType);
        field = argType;
    }

    private void initJarbootHome() {
        jarbootHome = System.getenv(CommonConst.JARBOOT_HOME);
        if (StringUtils.isEmpty(jarbootHome)) {
            jarbootHome = System.getProperty(CommonConst.JARBOOT_HOME);
        }
        if (StringUtils.isEmpty(jarbootHome)) {
            CodeSource codeSource = Jarboot.class.getProtectionDomain().getCodeSource();
            try {
                File agentJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                jarbootHome = agentJarFile.getParentFile().getParentFile().getPath();
            } catch (Exception e) {
                jarbootHome = "bin";
            }
        }
        if (jarbootHome.contains(StringUtils.SPACE)) {
            throw new JarbootException(CommonConst.JARBOOT_HOME + " path has space ` `.");
        }
    }

    public static void main(String[] args) {
        try {
            Jarboot jarboot = new Jarboot(args);
            jarboot.run();
        } catch (Exception e) {
            AnsiLog.error(e);
        }
    }

    public void run() {
        if (notInitialized) {
            return;
        }
        String banner = AnsiLog.enableColor() ? colorBanner()
              : "     ,--.               ,--.                   ,--.   \n" +
                "     |  | ,--,--.,--.--.|  |-.  ,---.  ,---. ,-'  '-. \n" +
                ",--. |  |' ,-.  ||  .--'| .-. '| .-. || .-. |'-.  .-' \n" +
                "|  '-'  /\\ '-'  ||  |   | `-' |' '-' '' '-' '  |  |   \n" +
                " `-----'  `--`--'`--'    `---'  `---'  `---'   `--'   ";
        AnsiLog.println(banner);
        if (Boolean.TRUE.equals(this.help)) {
            //打印帮助
            printHelp();
            return;
        }
        AnsiLog.println("Jarboot host: {}, checking jarboot server...", AnsiLog.cyan(host));
        HttpUtils.setHost(host);
        try {
            String version = HttpUtils.getJson("/api/jarboot/cloud/version", String.class);
            AnsiLog.println("Jarboot server version: {}", AnsiLog.cyan(version));
        } catch (Exception e) {
            AnsiLog.error(e);
            AnsiLog.error("Check Jarboot server {} failed! Please input running Jarboot server host.", host);
            return;
        }

        if (null != command && !command.isEmpty()) {
            if (null != pid && !pid.isEmpty()) {
                AnsiLog.error("command and pid can not use in the same time.");
                printHelp();
                return;
            }
            //将执行命令
            this.doCommand();
            printHomePage();
            return;
        }
        if (null != pid && !pid.isEmpty()) {
            //Attach 指定进程
            this.doAttach();
        } else {
            this.selectAttach();
        }
        printHomePage();
    }

    private void doAttach() {
        AnsiLog.info("Attaching pid: {}", AnsiLog.cyan(pid));
        Object vm = null;
        try {
            vm = VMUtils.getInstance().attachVM(pid);
            VMUtils.getInstance().loadAgentToVM(vm, getJarbootAgentPath(), host);
        } catch (Exception e) {
            AnsiLog.error(e);
        } finally {
            if (null != vm) {
                VMUtils.getInstance().detachVM(vm);
            }
            AnsiLog.info("Attach pid: {} finished.", pid);
        }
    }

    private void doCommand() {
        AnsiLog.info("Starting process: {}", command);
        //处理
        ArrayList<String> list = new ArrayList<>();
        list.add("java");
        list.add(String.format("-D%s=%s", CommonConst.JARBOOT_HOME, jarbootHome));
        list.add(String.format("-D%s=%s", CommonConst.REMOTE_PROP, host));
        list.add(String.format("-javaagent:%s", getJarbootAgentPath()));
        list.addAll(command);
        String[] cmd = list.toArray(new String[0]);
        try {
            AnsiLog.info("cmd: {}", list);
            Process p = Runtime.getRuntime().exec(cmd);
            if (Boolean.TRUE.equals(this.sync)) {
                AnsiLog.info("Sync execute command waiting command exit.");
                printHomePage();
                p.waitFor();
            } else {
                Thread.sleep(3000);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AnsiLog.error(e);
        } finally {
            AnsiLog.info("Start process finished.");
        }
    }

    private void selectAttach() {
        Map<Integer, String> vms = VMUtils.getInstance().listVM();
        //排除自己
        int curPid = Integer.parseInt(PidFileHelper.PID);
        vms.remove(curPid);
        if (vms.isEmpty()) {
            AnsiLog.info("Can't found any java processes.");
            return;
        }
        Map<Integer, Integer> indexToPid = new HashMap<>(16);
        AtomicInteger index = new AtomicInteger(0);
        AnsiLog.info("Found existing java process, " +
                "please choose one and input the serial number of the process, eg : 1. Then hit ENTER.");
        vms.forEach((k, v) -> {
            int i = index.incrementAndGet();
            String colorPid = AnsiLog.cyan(String.valueOf(k));
            String colorIndex = AnsiLog.green(String.valueOf(i));
            //截取名字
            int p = v.indexOf(' ');
            if (p > 0) {
                v = v.substring(0, p);
            }
            if (indexToPid.isEmpty()) {
                AnsiLog.println("{} [{}]: {} {}", AnsiLog.red("*"), colorIndex, colorPid, v);
            } else {
                AnsiLog.println("  [{}]: {} {}", colorIndex, colorPid, v);
            }
            indexToPid.put(i, k);
        });

        try (InputStreamReader in = new InputStreamReader(System.in);
             BufferedReader br = new BufferedReader(in)){
            String line = br.readLine();
            int i = StringUtils.isBlank(line) ? 1 : Integer.parseInt(line);
            Integer select = indexToPid.getOrDefault(i, null);
            if (null == select) {
                AnsiLog.println("Please select an available pid.");
            } else {
                this.pid = select.toString();
                doAttach();
            }
        } catch (NumberFormatException e) {
            AnsiLog.println("Please input an integer to select pid.");
        } catch (Exception e) {
            AnsiLog.error(e);
        }
    }

    private String getJarbootAgentPath() {
        File agentJar = FileUtils.getFile(jarbootHome, "bin", CommonConst.AGENT_JAR_NAME);
        if (agentJar.exists()) {
            return agentJar.getPath();
        } else {
            throw new JarbootException(CommonConst.AGENT_JAR_NAME + " not exist!");
        }
    }

    private void printHomePage() {
        String url = String.format("http://%s/jarboot/index.html", host);
        AnsiLog.println("Visit online diagnose: {}", AnsiLog.blue(url));
    }

    private void printHelp() {
        final String jt = OSUtils.isWindows() ? "jt.cmd" : "jt.sh";
        StringBuilder sb = new StringBuilder();
        sb
                .append("Help: \n")
                .append("The jarboot host default form environment variable: `")
                .append(AnsiLog.cyan(JARBOOT_HOST_ENV))
                .append("`, if none will use `127.0.0.1:9899`.\nAnd you can also use `-h` or `--host` pass it.\n")
                .append("Attach process:\nUsage: ")
                .append(AnsiLog.green(jt + " -h[-host] [eg: 127.0.0.1:9899] -pid [eg: 6868]"))
                .append(CoreConstant.EXAMPLE)
                .append(AnsiLog.blue(jt + " -h 192.168.1.88:9899 -pid 6868"))
                .append('\n')
                .append(AnsiLog.blue(jt + " -pid 6868"))
                .append('\n')
                .append(AnsiLog.blue(jt + " -h 192.168.1.88:9899"))
                .append('\n')
                .append(AnsiLog.blue(jt))
                .append('\n')
                .append("Start process sync or async:\nUsage: ")
                .append(AnsiLog.green(jt + " -h[-host] [eg: 127.0.0.1:9899] -async[sync] [eg: -jar demo.jar]"))
                .append(CoreConstant.EXAMPLE)
                .append(AnsiLog.blue(jt + " -h 192.168.1.88:9899 -sync -jar demo-app.jar"))
                .append('\n')
                .append(AnsiLog.blue(jt + " -async -jar demo-app.jar"))
        ;
        AnsiLog.println(sb.toString());
        printHomePage();
    }

    private String colorBanner() {
        return          "\033[31m     ,--.\033[0m\033[32m        \033[0m\033[33m       \033[0m\033[34m,--.   \033[0m\033[35m       \033[0m\033[36m       \033[0m\033[31m  ,--.   \033[0m\n" +
                        "\033[31m     |  |\033[0m\033[32m ,--,--.\033[0m\033[33m,--.--.\033[0m\033[34m|  |-. \033[0m\033[35m ,---. \033[0m\033[36m ,---. \033[0m\033[31m,-'  '-. \033[0m\n" +
                        "\033[31m,--. |  |\033[0m\033[32m' ,-.  |\033[0m\033[33m|  .--'\033[0m\033[34m| .-. '\033[0m\033[35m| .-. |\033[0m\033[36m| .-. |\033[0m\033[31m'-.  .-' \033[0m\n" +
                        "\033[31m|  '-'  /\033[0m\033[32m\\ '-'  |\033[0m\033[33m|  |  \033[0m\033[34m | `-' |\033[0m\033[35m' '-' '\033[0m\033[36m' '-' '\033[0m\033[31m  |  |   \033[0m\n" +
                        "\033[31m `-----' \033[0m\033[32m `--`--'\033[0m\033[33m`--'   \033[0m\033[34m `---' \033[0m\033[35m `---' \033[0m\033[36m `---' \033[0m\033[31m  `--'   \033[0m\n";
    }
}
