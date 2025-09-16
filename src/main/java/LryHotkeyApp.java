import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LryHotkeyApp implements NativeKeyListener {

    private final String defaultCommand; // 默认命令
    private String currentCommand;       // 当前执行的命令

    public LryHotkeyApp() {
        // 默认命令（初始分辨率）
        defaultCommand = "C:\\\\Program Files (x86)\\\\Microsoft\\\\Edge\\\\Application\\\\msedge.exe " +
                "--new-window https://www.cnki.net/ " +
                "--start-maximized " +
                "--window-position=0,0 " +
                "--window-size=1920,1080";
        currentCommand = defaultCommand;
    }

    public static void main(String[] args) {
        // 关闭 JNativeHook 自带日志输出
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
            return;
        }

        LryHotkeyApp app = new LryHotkeyApp();
        GlobalScreen.addNativeKeyListener(app);

        // 添加托盘图标
        if (SystemTray.isSupported()) {
            try {
                app.addTrayIcon();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("当前系统不支持托盘图标");
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // 检测 Alt + S（且没有 Ctrl / Shift / Meta）
        if (e.getKeyCode() == NativeKeyEvent.VC_S
                && (e.getModifiers() & NativeKeyEvent.ALT_MASK) != 0
                && (e.getModifiers() & (NativeKeyEvent.SHIFT_MASK | NativeKeyEvent.CTRL_MASK | NativeKeyEvent.META_MASK)) == 0) {
            onHotkeyTriggered();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
    }

    private void onHotkeyTriggered() {
        System.out.println("检测到 Alt+S，执行命令：" + currentCommand);

        try {
            runCommand(currentCommand);  // ✅ 仅在快捷键触发时执行
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "执行失败！请检查命令是否正确。",
                    "执行错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 执行命令
     */
    private void runCommand(String command) throws IOException {
        List<String> parts = new ArrayList<>();
        try (Scanner sc = new Scanner(command)) {
            sc.useDelimiter(" ");
            while (sc.hasNext()) {
                parts.add(sc.next().replace("\"", "")); // 去掉引号
            }
        }
        new ProcessBuilder(parts).start();
    }

    /**
     * 托盘图标 + AWT 原生菜单
     */
    private void addTrayIcon() throws Exception {
        // 获取系统托盘
        SystemTray tray = SystemTray.getSystemTray();

        // 托盘图标
        Image image;
        try {
            // URL url = new URL("https://icons.iconarchive.com/icons/paomedia/small-n-flat/32/sign-check-icon.png");
            URL url = this.getClass().getResource("icon/icon.png");
            image = ImageIO.read(url);
        } catch (IOException e) {
            image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }

        TrayIcon trayIcon = new TrayIcon(image, "Lry Shortcuts：按下 Alt+S 执行当前命令");
        trayIcon.setImageAutoSize(true);

        // ---------------------
        // AWT 原生菜单
        // ---------------------
        PopupMenu popup = new PopupMenu();

        // Edit Command
        MenuItem editItem = new MenuItem("Edit Command");
        editItem.addActionListener(e -> SwingUtilities.invokeLater(this::showEditDialog));
        popup.add(editItem);

        // Reset to Default
        MenuItem resetItem = new MenuItem("Reset to Default");
        resetItem.addActionListener(e -> {
            currentCommand = defaultCommand;
            JOptionPane.showMessageDialog(null,
                    "已恢复为默认命令。",
                    "信息",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        popup.add(resetItem);

        // Quit
        MenuItem exitItem = new MenuItem("Quit");
        exitItem.addActionListener(e -> {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ex) {
                ex.printStackTrace();
            }
            tray.remove(trayIcon);
            System.exit(0);
        });
        popup.add(exitItem);

        // 绑定菜单到托盘
        trayIcon.setPopupMenu(popup);

        tray.add(trayIcon);
    }

    /**
     * 加载并缩放图标
     */
    private ImageIcon loadIcon(String resourcePath) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url == null) {
                return null;
            }
            Image img = ImageIO.read(url);
            if (img == null) {
                return null;
            }
            Image scaled = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 编辑命令对话框
     */
    private void showEditDialog() {
        try {
            // 使用系统原生样式（Windows 下更协调）
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
        }

        JDialog dialog = new JDialog((Frame) null, "Edit Command", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 整体布局 + padding
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 输入框 + Label
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        JLabel label = new JLabel("Command:");
        label.setPreferredSize(new Dimension(80, 25));
        JTextField textField = new JTextField(currentCommand, 50);
        textField.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputPanel.add(label, BorderLayout.WEST);
        inputPanel.add(textField, BorderLayout.CENTER);

        // 按钮区
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        JButton confirmBtn = new JButton("Confirm");
        JButton cancelBtn = new JButton("Cancel");
        Dimension btnSize = new Dimension(90, 28);
        confirmBtn.setPreferredSize(btnSize);
        cancelBtn.setPreferredSize(btnSize);
        buttonPanel.add(confirmBtn);
        buttonPanel.add(cancelBtn);

        content.add(inputPanel, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setContentPane(content);

        // Confirm 按钮逻辑
        confirmBtn.addActionListener(e -> {
            String newCommand = textField.getText().trim();
            if (newCommand.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "命令不能为空！",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // 测试执行命令
                List<String> parts = new ArrayList<>();
                try (Scanner sc = new Scanner(newCommand)) {
                    sc.useDelimiter(" ");
                    while (sc.hasNext()) {
                        parts.add(sc.next().replace("\"", ""));
                    }
                }
                Process process = new ProcessBuilder(parts).start();
                try {
                    process.destroy();
                } catch (Exception ignore) {
                }

                currentCommand = newCommand;
                JOptionPane.showMessageDialog(dialog,
                        "命令已更新成功！",
                        "信息",
                        JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "命令执行失败，请重新输入！",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Cancel 按钮逻辑
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }
}
