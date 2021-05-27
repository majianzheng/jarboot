package com.mz.jarboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class StarterDesktopUI {
    private static final Logger logger = LoggerFactory.getLogger(StarterDesktopUI.class);
    private static final String APP_TITLE = "启动管理系统";
    private TrayIcon trayIcon;
    private JFrame mainFrame;
    private boolean autoOpenMainPage = true;

    public StarterDesktopUI() {
        initUI();
    }

    private void initUI() {
        mainFrame = new JFrame(APP_TITLE);
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
        imageIcon = new ImageIcon(imageData, APP_TITLE);
        mainFrame.setIconImage(imageIcon.getImage());
        trayIcon = new TrayIcon(imageIcon.getImage());
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip(APP_TITLE);
        popupMenu = new PopupMenu();
        //添加右键菜单
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
                    //showEbrManagePage();
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
        //show.addActionListener(e -> showEbrManagePage());
        about.addActionListener(e -> this.info("关于", APP_TITLE));
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

    public boolean isAutoOpenMainPage() {
        return autoOpenMainPage;
    }

    public void setAutoOpenMainPage(boolean autoOpenMainPage) {
        this.autoOpenMainPage = autoOpenMainPage;
    }
}
