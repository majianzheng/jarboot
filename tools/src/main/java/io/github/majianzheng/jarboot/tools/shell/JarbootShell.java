package io.github.majianzheng.jarboot.tools.shell;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.common.*;
import io.github.majianzheng.jarboot.common.utils.*;
import io.github.majianzheng.jarboot.tools.common.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


/**
 * @author majianzheng
 */
public class JarbootShell {
    private static final String PID_ARG = "pid";
    private static final String HOST_ARG = "host";
    private static final String CMD_ARG = "command";
    private static final String OPTION_PRE = "-";

    /** Jarboot 服务地址 */
    private String host = "http://127.0.0.1:9899";
    /** 进程PID */
    private String pid;
    /** 启动Java进程命令行 */
    private List<String> command;
    /** 显示帮助信息 */
    private Boolean help;
    /** 是否是同步执行命令 */
    private Boolean sync;
    /** 是否是shell命令 */
    private Boolean shell;
    /** 传入的参数 */
    private final String[] args;
    /** Jarboot安装目录 */
    private String jarbootHome;
    /** 待填充的字段 */
    private String field;
    /** 是否未初始化成功 */
    private boolean notInitialized = true;

    private JarbootShell(String [] args) {
        this.args = args;
        initJarbootHome();
        String hostEnv = System.getenv(CommonConst.JARBOOT_HOST_ENV);
        if (!StringUtils.isEmpty(hostEnv)) {
            if (hostEnv.startsWith(CommonConst.HTTP) || hostEnv.startsWith(CommonConst.HTTPS)) {
                this.host = hostEnv;
            } else {
                this.host = CommonConst.HTTP + hostEnv;
            }
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
        if (null == this.args || this.args.length == 0) {
            return;
        }
        HashSet<String> optionHash = new HashSet<>();

        for (String arg : this.args) {
            if (null != this.command) {
                setField(arg);
                continue;
            }
            switch (arg) {
                case "-pid", "--pid":
                    verifyField(PID_ARG, optionHash);
                    break;
                case "-h", "-host", "--host":
                    verifyField(HOST_ARG, optionHash);
                    break;
                case "-sync", "--sync":
                    verifyField(CMD_ARG, optionHash);
                    this.sync = true;
                    command = new ArrayList<>();
                    break;
                case "-async", "--async":
                    verifyField(CMD_ARG, optionHash);
                    command = new ArrayList<>();
                    break;
                case "-c", "--command":
                    verifyField(CMD_ARG, optionHash);
                    this.shell = true;
                    this.sync = true;
                    command = new ArrayList<>();
                    break;
                case "-help", "--help":
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
                if (arg.startsWith(CommonConst.HTTP) || arg.startsWith(CommonConst.HTTPS)) {
                    this.host = arg;
                } else {
                    this.host = CommonConst.HTTP + arg;
                }
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
        jarbootHome = Utils.getJarbootHome();
    }

    public static void main(String[] args) {
        try {
            JarbootShell jarboot = new JarbootShell(args);
            jarboot.run();
        } catch (Exception e) {
            AnsiLog.error(AnsiLog.red("{}"), e.getMessage());
            AnsiLog.error(e);
        }
    }

    public void run() {
        if (notInitialized) {
            return;
        }
        if (!Boolean.TRUE.equals(this.shell)) {
            BannerUtils.print();
        }
        if (Boolean.TRUE.equals(this.help)) {
            //打印帮助
            printHelp();
            return;
        }
        if (!Boolean.TRUE.equals(this.shell)) {
            AnsiLog.println("Jarboot host: {}, checking jarboot server...", AnsiLog.cyan(host));
        }
        String url = host + CommonConst.SERVER_RUNTIME_CONTEXT;
        try {
            ServerRuntimeInfo runtimeInfo = HttpUtils.getObj(url, ServerRuntimeInfo.class, null);
            if (!Boolean.TRUE.equals(this.shell)) {
                AnsiLog.println("Jarboot server version: {}", AnsiLog.cyan(runtimeInfo.getVersion()));
            }
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
        AnsiLog.info("starting: {}", String.join(" ", command));
        try {
            Process process = createExecProcess();
            final Thread hook = JarbootThreadFactory.createThreadFactory("jarboot-hook").newThread(() -> exitHook(process));
            Runtime.getRuntime().addShutdownHook(hook);
            if (Boolean.TRUE.equals(this.sync)) {
                if (!Boolean.TRUE.equals(this.shell)) {
                    AnsiLog.info("Sync execute command waiting command exit.");
                    printHomePage();
                }
                InputStream inputStream = process.getInputStream();
                int b = -1;
                while (-1 != (b = inputStream.read())) {
                    AnsiLog.write(b);
                }
                int code = process.waitFor();
                AnsiLog.info("exit code: {}", code);
                Runtime.getRuntime().removeShutdownHook(hook);
            } else {
                Thread.sleep(3000);
                printHomePage();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AnsiLog.error(AnsiLog.red("{}"), e.getMessage());
            AnsiLog.error(e);
        } finally {
            FileUtils.deleteQuietly(getCmdFile());
            AnsiLog.info("Start process finished.");
        }
    }

    private Process createExecProcess() {
        String workHome = System.getenv("WORK_HOME");
        if (StringUtils.isEmpty(workHome)) {
            workHome = System.getProperty("user.dir");
        }
        try {
            StringBuilder sb = new StringBuilder();
            if (OSUtils.isWindows()) {
                sb.append("@echo off").append(StringUtils.LINE_BREAK);
                sb.append("set \"JARBOOT_HOME=").append(jarbootHome).append('"').append(StringUtils.LINE_BREAK)
                        .append("set \"WORK_HOME=").append(workHome).append('"').append(StringUtils.LINE_BREAK)
                        .append("cd \"").append("%WORK_HOME%").append('"').append(StringUtils.LINE_BREAK);
            } else {
                sb.append("export JARBOOT_HOME=\"").append(jarbootHome).append('"').append(StringUtils.LINE_BREAK)
                        .append("export WORK_HOME=\"").append(workHome).append('"').append(StringUtils.LINE_BREAK)
                        .append("cd \"").append("$WORK_HOME").append('"').append(StringUtils.LINE_BREAK);
            }
            sb.append(String.join(StringUtils.SPACE, getCmdStrings())).append(StringUtils.LINE_BREAK);
            File cmdFile = getCmdFile();
            FileUtils.writeStringToFile(cmdFile, sb.toString(), OSUtils.isWindows() ? "GBK" : "UTF-8");
            if (!cmdFile.setExecutable(true)) {
                AnsiLog.error("set executable failed.");
            }
            List<String> cmd = OSUtils.isWindows() ? Collections.singletonList(cmdFile.getAbsolutePath()) : Arrays.asList("bash", cmdFile.getAbsolutePath());
            return new ProcessBuilder()
                    .directory(cmdFile.getParentFile())
                    .command(cmd)
                    .start();
        } catch (Exception e) {
            throw new JarbootException(e);
        }
    }

    private ArrayList<String> getCmdStrings() {
        ArrayList<String> list = new ArrayList<>();
        if (!Boolean.TRUE.equals(this.shell)) {
            list.add(CommonConst.JAVA_CMD);
            list.add(String.format("-D%s=%s", CommonConst.REMOTE_PROP, host));
            list.add("-noverify");
            list.add(String.format("-javaagent:%s", getJarbootAgentPath()));
        }
        list.addAll(command);
        return list;
    }

    private static File getCmdFile() {
        final String a = OSUtils.isWindows() ? "cmd" : "sh";
        final String cmdFileName = String.format(".shell%s%08x.%s", StringUtils.randomString(6), System.currentTimeMillis(), a);
        return FileUtils.getFile(CacheDirHelper.getTempBashDir(), cmdFileName);
    }

    private static void exitHook(Process process) {
        if (null != process) {
            try {
                killChildren(process.children());
            } catch (Exception e) {
                AnsiLog.error(e);
            }
            try {
                process.destroyForcibly();
            } catch (Exception e) {
                // ignore
            }
        }
        File cmdFile = getCmdFile();
        if (null != cmdFile && cmdFile.exists()) {
            FileUtils.deleteQuietly(cmdFile);
        }
    }
    private static void killChildren(Stream<ProcessHandle> children) {
        if (null == children) {
            return;
        }
        children.forEach(child -> {
            try {
                killChildren(child.children());
            } catch (Exception e) {
                AnsiLog.error(e);
            }
            try {
                child.destroyForcibly();
            } catch (Exception e) {
                AnsiLog.error(e);
            }
        });
    }

    private void selectAttach() {
        Map<String, String> vms = VMUtils.getInstance().listVM();
        //排除自己
        vms.remove(PidFileHelper.PID);
        if (vms.isEmpty()) {
            AnsiLog.info("Can't found any java processes.");
            return;
        }
        Map<Integer, String> indexToPid = new HashMap<>(16);
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

        try {
            String line = System.console().readLine();
            int i = StringUtils.isBlank(line) ? 1 : Integer.parseInt(line);
            String select = indexToPid.getOrDefault(i, null);
            if (null == select) {
                AnsiLog.println("Please select an available pid.");
            } else {
                this.pid = select;
                doAttach();
            }
        } catch (NumberFormatException e) {
            AnsiLog.println("Please input an integer to select pid.");
        } catch (Exception e) {
            AnsiLog.error(e.getMessage());
            AnsiLog.error(e);
        }
    }

    private String getJarbootAgentPath() {
        File userAgentJarFile = getUserAgentJarFile();
        String agentPath = userAgentJarFile.getAbsolutePath();
        File agentJarFile = FileUtils.getFile(jarbootHome, CommonConst.COMPONENTS_NAME, CommonConst.AGENT_JAR_NAME);
        if (!userAgentJarFile.exists()) {
            // agent jar不存在，则从jar包中拷贝
            try {
                if (!userAgentJarFile.getParentFile().exists()) {
                    FileUtils.forceMkdir(userAgentJarFile.getParentFile());
                }
                FileUtils.copyFile(agentJarFile, userAgentJarFile);
                agentPath = userAgentJarFile.getAbsolutePath();
            } catch (Exception e) {
                AnsiLog.error("Copy agent jar error!", e);
                agentPath = agentJarFile.getAbsolutePath();
            }
        }
        return agentPath;
    }

    private static File getUserAgentJarFile() {
        String base = "/tmp";
        if (OSUtils.isWindows()) {
            base = System.getenv("ProgramData");
            if (StringUtils.isEmpty(base)) {
                base = "C:\\ProgramData";
            }
        }
        return FileUtils.getFile(base, "jarboot", CommonConst.AGENT_JAR_NAME);
    }

    private void printHomePage() {
        String url = host + "/jarboot/index.html";
        AnsiLog.println("Visit online diagnose: {}", AnsiLog.blue(url));
    }

    private void printHelp() {
        final String jt = OSUtils.isWindows() ? "jt.cmd" : "jt.sh";
        StringBuilder sb = new StringBuilder();
        sb
                .append("Help: \n")
                .append("The jarboot host default form environment variable: `")
                .append(AnsiLog.cyan(CommonConst.JARBOOT_HOST_ENV))
                .append("`, if none will use `127.0.0.1:9899`.\nAnd you can also use `-h` or `--host` pass it.\n")
                .append("Attach process:\nUsage: ")
                .append(AnsiLog.green(jt + " -h[-host] [eg: 127.0.0.1:9899] -pid [eg: 6868]"))
                .append("\nEXAMPLES:\n")
                .append(AnsiLog.blue(jt + " -h 192.168.1.88:9899 -pid 6868"))
                .append(StringUtils.LF)
                .append(AnsiLog.blue(jt + " -pid 6868"))
                .append(StringUtils.LF)
                .append(AnsiLog.blue(jt + " -h 192.168.1.88:9899"))
                .append(StringUtils.LF)
                .append(AnsiLog.blue(jt))
                .append(StringUtils.LF)
                .append("Start process sync or async:\nUsage: ")
                .append(AnsiLog.green(jt + " -h[-host] [eg: 127.0.0.1:9899] -async[sync] [eg: -jar demo.jar]"))
                .append("\nEXAMPLES:\n")
                .append(AnsiLog.blue(jt + " -h 192.168.1.88:9899 -sync -jar demo-app.jar"))
                .append(StringUtils.LF)
                .append(AnsiLog.blue(jt + " -async -jar demo-app.jar"))
        ;
        AnsiLog.println(sb.toString());
        printHomePage();
    }
}
