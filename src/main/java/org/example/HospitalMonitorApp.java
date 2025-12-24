package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HospitalMonitorApp extends JFrame {
    private JPanel mainContainer;
    private CardLayout cardLayout;
    private List<Patient> patients = new ArrayList<>();
    private DataSimulator simulator = new DataSimulator();

    // UI 组件：用于在详情页显示数字
    private JLabel nameLabel, hrLabel, bpLabel, tempLabel, rrLabel;

    public HospitalMonitorApp() {
        setTitle("BioEng Remote Patient Monitor");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. 初始化8位患者数据
        for (int i = 1; i <= 8; i++) {
            patients.add(new Patient(i, "Patient " + i));
        }

        // 2. 设置布局管理器 (用来切换界面)
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // 3. 创建两个主要面板
        JPanel selectionPanel = createSelectionPanel(); // 选人界面
        JPanel detailPanel = createDetailPanel();       // 详情界面

        mainContainer.add(selectionPanel, "SELECTION");
        mainContainer.add(detailPanel, "DETAIL");

        add(mainContainer);

        // 4. 启动后台模拟器线程 (每秒更新一次数据)
        startSimulation();
    }

    // --- 界面 A: 8个格子的选择界面 ---
    private JPanel createSelectionPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 10, 10)); // 2行4列
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        for (Patient p : patients) {
            JButton btn = new JButton("<html><h2>" + p.name + "</h2>Select to Monitor</html>");
            btn.setBackground(new Color(173, 216, 230)); // 浅蓝色，模仿你们的图
            btn.addActionListener(e -> showPatientDetail(p));
            panel.add(btn);
        }
        return panel;
    }

    // --- 界面 B: 单个患者详情界面 (模仿你们的草图) ---
    private JPanel createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 顶部：返回按钮 + 患者信息
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backBtn = new JButton("<< Back to Overview");
        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "SELECTION"));
        nameLabel = new JLabel("Patient Name");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topBar.add(backBtn);
        topBar.add(nameLabel);

        // 中间：仪表盘 (左边画图，右边数字)
        JPanel dashboard = new JPanel(new GridLayout(1, 2));

        // 左边：ECG 区域 (先放个占位符，以后在这里画波形)
        JPanel ecgPanel = new JPanel();
        ecgPanel.setBackground(Color.BLACK);
        ecgPanel.setBorder(BorderFactory.createTitledBorder("ECG Trace (Real-time)"));
        ecgPanel.add(new JLabel("<html><font color='white'>[ECG Graph Area]</font></html>"));

        // 右边：4个数值 (Grid 2x2)
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        hrLabel = createStatBox(statsPanel, "Heart Rate", "bpm");
        rrLabel = createStatBox(statsPanel, "Resp Rate", "/min");
        tempLabel = createStatBox(statsPanel, "Body Temp", "°C");
        bpLabel = createStatBox(statsPanel, "Blood Pressure", "mmHg");

        dashboard.add(ecgPanel);
        dashboard.add(statsPanel);

        // 底部：历史报告按钮
        JPanel bottomBar = new JPanel();
        bottomBar.add(new JButton("View Past Report"));
        bottomBar.add(new JButton("Generate Report"));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(dashboard, BorderLayout.CENTER);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    // 辅助方法：创建漂亮的数字显示框
    private JLabel createStatBox(JPanel container, String title, String unit) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        box.setBackground(Color.WHITE);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel valueLbl = new JLabel("--");
        valueLbl.setFont(new Font("Arial", Font.BOLD, 40));
        valueLbl.setHorizontalAlignment(SwingConstants.CENTER);
        valueLbl.setForeground(Color.BLUE); // 模仿你们的蓝色风格

        JLabel unitLbl = new JLabel(unit);
        unitLbl.setHorizontalAlignment(SwingConstants.CENTER);

        box.add(titleLbl, BorderLayout.NORTH);
        box.add(valueLbl, BorderLayout.CENTER);
        box.add(unitLbl, BorderLayout.SOUTH);

        container.add(box);
        return valueLbl;
    }

    // --- 逻辑: 切换到详情页并开始更新UI ---
    private Patient currentPatient; // 当前正在看的患者

    private void showPatientDetail(Patient p) {
        this.currentPatient = p;
        nameLabel.setText(p.name);
        cardLayout.show(mainContainer, "DETAIL");
    }

    // --- 核心引擎: 每一秒生成数据并刷新界面 ---
    private void startSimulation() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 1. 为所有8位患者生成新数据
                for (Patient p : patients) {
                    VitalSigns newVitals = simulator.generateNormalData();
                    p.addRecord(newVitals);
                }

                // 2. 如果当前正在看某位患者，刷新屏幕上的数字
                if (currentPatient != null) {
                    VitalSigns v = currentPatient.getLatestRecord();
                    if (v != null) {
                        // Swing 更新必须在事件线程中做
                        SwingUtilities.invokeLater(() -> {
                            hrLabel.setText(String.valueOf(v.heartRate));
                            rrLabel.setText(String.valueOf(v.respiratoryRate));
                            tempLabel.setText(String.format("%.1f", v.bodyTemp));
                            bpLabel.setText(v.systolicBP + "/" + v.diastolicBP);
                        });
                    }
                }
            }
        }, 0, 1000); // 0延迟，每1000毫秒(1秒)执行一次
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HospitalMonitorApp().setVisible(true);
        });
    }
}