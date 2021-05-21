package com.mz.jarboot;

import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.utils.SettingUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

public class StarterDesktopUI {
    private static final Logger logger = LoggerFactory.getLogger(StarterDesktopUI.class);

    private TrayIcon trayIcon;
    private JFrame mainFrame;
    private boolean isFirstSetup;
    private boolean isDebugMode = false;
    private Properties settingProperties;
    private boolean autoOpenMainPage = true;
    /**
     * 检查系统是否已经启动
     * @return 是否已启动
     */
    public boolean checkSystemStarted() {

        File cacheFile = new File(SettingUtils.getCacheFilePath());
        this.isFirstSetup = (!cacheFile.exists() || !cacheFile.isFile());
        this.settingProperties = PropertyFileUtils.getCurrentSettings();
        String debugMode = settingProperties.getProperty(CommonConst.DEBUG_MODE_KEY);
        if (StringUtils.equals("true", debugMode)) {
            this.isDebugMode = true;
        }
        if (!this.isDebugMode) {
            //初始化安装根目录
            this.initRootPath();
        }
        //获取配置文件里的端口号
        String port = settingProperties.getProperty("port");
        if (StringUtils.isEmpty(port)) {
            this.error("错误", "无法获取EBR 系统管理服务的配置文件！" );
            JOptionPane.showMessageDialog(this.mainFrame,
                    "无法读取到配置文件，请确认是否移动或删除了文件。\n建议：尝试重新安装解决。", "错误",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        return false;
    }

    private void initRootPath() {
        String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        path = StringUtils.removeStart(path, "/");
        path = path.replace("file:/", "");
        String root = PropertyFileUtils.getCurrentSetting(CommonConst.ROOT_DIR_KEY);
        int p = path.lastIndexOf(root);
        if (-1 != p) {
            int p1 = p + root.length();
            path = path.substring(0, p1);
            if (File.separatorChar == '\\') {
                path = path.replace("/", "\\\\");
            }
            PropertyFileUtils.setCurrentSetting(CommonConst.ROOT_PATH_KEY, path);
        }
    }

    public StarterDesktopUI() {
        initUI();
    }

    private void initUI() {
        mainFrame = new JFrame("启动管理系统");
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setSize(300, 220);
        JPanel panel = new JPanel();
        JLabel startText = new JLabel("系统启动中，请稍后...");
        Font font = new Font("宋体", Font.BOLD, 20);	//新建一个 Font 对象
        startText.setFont(font);
        panel.add(startText);
        mainFrame.setContentPane(panel);
        PopupMenu popupMenu;
        SystemTray systemTray;
        ImageIcon imageIcon;
        if (!SystemTray.isSupported()) {
            return;
        }
        byte[] imageData = new byte[1];
        try {
            ClassPathResource resource = new ClassPathResource("logo.png");
            InputStream fis = resource.getInputStream();
            //申请2M空间
            byte[] b = new byte[1024 * 1024 * 2];
            int count = fis.read(b);
            if (count > 0) {
                ByteBuffer buffer = ByteBuffer.allocate(count);
                buffer.put(b, 0, count);
                imageData = buffer.array();
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        imageIcon = new ImageIcon(imageData, "启动管理系统");
        mainFrame.setIconImage(imageIcon.getImage());
        trayIcon = new TrayIcon(imageIcon.getImage());
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("启动管理系统");
        popupMenu = new PopupMenu();
        //添加右键菜单
        //java.util.List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments()
        //System.out.println("\n#####################运行时设置的JVM参数#######################")
        //System.out.println(inputArgs)
        MenuItem exit = new MenuItem("退出");
        MenuItem show = new MenuItem("服务管理");
        MenuItem about = new MenuItem("关于");
        popupMenu.add(about);
        popupMenu.add(show);
        popupMenu.add(exit);
        trayIcon.setPopupMenu(popupMenu);
        systemTray = SystemTray.getSystemTray();
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            logger.error(e.getMessage(), e);
            return;
        }
        trayIcon.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    //双击托盘图标打开主界面
                    showEbrManagePage();
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {
                // Do nothing because of X and Y.
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                // Do nothing because of X and Y.
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                // Do nothing because of X and Y.
            }
            @Override
            public void mouseExited(MouseEvent e) {
                // Do nothing because of X and Y.
            }
        });
        exit.addActionListener(e -> {
            //提示是否退出
            int rlt = JOptionPane.showConfirmDialog(null,
                    "退出将停止系统管理服务！确定要继续退出？",
                    "提示", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (0 == rlt) {
                //点击了Yes，执行退出
                System.exit(0);
            }
        });
        show.addActionListener(e -> showEbrManagePage());
        about.addActionListener(e -> this.info("关于", "启动管理系统"));
    }

    public void setMainViewVisible(boolean visible) {
        mainFrame.setVisible(visible);
    }

    public void info(String caption, String text) {
        trayIcon.displayMessage(caption, text, TrayIcon.MessageType.INFO);
    }

    public void error(String caption, String text) {
        trayIcon.displayMessage(caption, text, TrayIcon.MessageType.ERROR);
    }

    public void warning(String caption, String text) {
        trayIcon.displayMessage(caption, text, TrayIcon.MessageType.WARNING);
    }

    public void showEbrManagePage() {
        String url = settingProperties.getProperty("default-page");
        if (this.isFirstSetup) {
            this.isFirstSetup = false;
        }
        try {
            SettingUtils.browse1(url);
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }

    public boolean isAutoOpenMainPage() {
        return autoOpenMainPage;
    }

    public void setAutoOpenMainPage(boolean autoOpenMainPage) {
        this.autoOpenMainPage = autoOpenMainPage;
    }
}
