package com.expense.ui;

import com.expense.service.*;
import com.expense.ui.panels.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MainFrame — uses plain constructor injection (no @Autowired, no @Component).
 * Spring wires it via AppConfig.mainFrame(...) so CGLIB never touches this class.
 */
public class MainFrame extends JFrame {

    private final TransactionService transactionService;
    private final ReportService reportService;
    private final FraudDetectionService fraudService;
    private final PdfExportService pdfExportService;

    private JLabel clockLabel;
    private Timer clockTimer;

    // Constructed by AppConfig — all deps injected here
    public MainFrame(TransactionService transactionService,
                     ReportService reportService,
                     FraudDetectionService fraudService,
                     PdfExportService pdfExportService) {
        this.transactionService = transactionService;
        this.reportService      = reportService;
        this.fraudService       = fraudService;
        this.pdfExportService   = pdfExportService;
        // Build UI immediately in constructor — all deps are ready
        initComponents();
    }

    private void initComponents() {
        applyGlobalLaf();

        setTitle("Smart Expense Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        JPanel header    = buildHeader();
        JPanel navBar    = buildNavBar();
        JPanel statusBar = buildStatusBar();
        JPanel content   = buildContent();

        add(header,    BorderLayout.NORTH);
        add(navBar,    BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);
        add(content,   BorderLayout.CENTER);

        // Wire nav buttons → CardLayout
        String[] cards = {"dashboard", "transactions", "reports", "fraud"};
        int navIdx = 0;
        for (Component c : navBar.getComponents()) {
            if (c instanceof JButton btn) {
                int idx = navIdx++;
                btn.addActionListener(e -> {
                    ((CardLayout) content.getLayout()).show(content, cards[idx]);
                    for (Component nb : navBar.getComponents())
                        if (nb instanceof JButton b) b.setBackground(NAV_BG);
                    btn.setBackground(NAV_ACTIVE);
                });
            }
        }

        // Highlight first button
        for (Component c : navBar.getComponents()) {
            if (c instanceof JButton btn) { btn.setBackground(NAV_ACTIVE); break; }
        }

        startClock();
    }

    // ── Design tokens ────────────────────────────────────────────────────────
    private static final Color HEADER_L  = new Color(26,  42,  58);
    private static final Color HEADER_R  = new Color(41,  82, 128);
    private static final Color NAV_BG    = new Color(30,  55,  80);
    private static final Color NAV_HOVER = new Color(38,  65,  92);
    private static final Color NAV_ACTIVE= new Color(41, 128, 185);
    private static final Color STATUS_BG = new Color(26,  38,  52);

    private void applyGlobalLaf() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("Panel.background",      new Color(242, 245, 250));
        UIManager.put("Table.gridColor",        new Color(220, 225, 232));
        UIManager.put("TableHeader.background", NAV_ACTIVE);
        UIManager.put("TableHeader.foreground", Color.WHITE);
    }

    // ── Header ───────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, HEADER_L, getWidth(), 0, HEADER_R));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setBorder(new EmptyBorder(14, 24, 14, 24));
        header.setPreferredSize(new Dimension(0, 64));

        // Left: icon + title block
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel logo = new JLabel("\uD83D\uDCB0");
        logo.setFont(new Font("SansSerif", Font.PLAIN, 26));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 0));
        titleBlock.setOpaque(false);
        JLabel title = new JLabel("Smart Expense Tracker");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Spring Boot  +  AI Fraud Detection");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 10));
        sub.setForeground(new Color(149, 185, 218));
        titleBlock.add(title);
        titleBlock.add(sub);

        left.add(logo);
        left.add(titleBlock);
        header.add(left, BorderLayout.WEST);

        // Right: status pills + clock
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(pill("H2 DB",    new Color(39, 174, 96)));
        right.add(pill("API \u2713", new Color(52, 152, 219)));
        right.add(pill("ML Active", new Color(142, 68, 173)));
        clockLabel = new JLabel();
        clockLabel.setForeground(new Color(200, 220, 240));
        clockLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        right.add(clockLabel);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JLabel pill(String text, Color bg) {
        JLabel lbl = new JLabel("  " + text + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setBackground(bg);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setOpaque(false);
        lbl.setBorder(new EmptyBorder(3, 0, 3, 0));
        return lbl;
    }

    // ── Left nav ─────────────────────────────────────────────────────────────
    private JPanel buildNavBar() {
        JPanel nav = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(22, 38, 55));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setPreferredSize(new Dimension(190, 0));
        nav.setBorder(new EmptyBorder(16, 0, 16, 0));

        nav.add(navBtn("\uD83D\uDCCA", "Dashboard"));
        nav.add(navBtn("\uD83D\uDCB3", "Transactions"));
        nav.add(navBtn("\uD83D\uDCC8", "Reports"));
        nav.add(navBtn("\uD83D\uDEA8", "Fraud Alerts"));
        nav.add(Box.createVerticalGlue());

        JLabel ver = new JLabel("  v1.0.0 \u00b7 2026");
        ver.setForeground(new Color(70, 95, 120));
        ver.setFont(new Font("SansSerif", Font.PLAIN, 10));
        ver.setAlignmentX(Component.LEFT_ALIGNMENT);
        ver.setBorder(new EmptyBorder(0, 16, 0, 0));
        nav.add(ver);
        return nav;
    }

    private JButton navBtn(String icon, String label) {
        JButton btn = new JButton(icon + "  " + label);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(new Color(189, 210, 230));
        btn.setBackground(NAV_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setMaximumSize(new Dimension(190, 46));
        btn.setPreferredSize(new Dimension(190, 46));
        btn.setBorder(new EmptyBorder(0, 20, 0, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!btn.getBackground().equals(NAV_ACTIVE)) btn.setBackground(NAV_HOVER);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (!btn.getBackground().equals(NAV_ACTIVE)) btn.setBackground(NAV_BG);
            }
        });
        return btn;
    }

    // ── Content area ─────────────────────────────────────────────────────────
    private JPanel buildContent() {
        JPanel content = new JPanel(new CardLayout());
        content.setBackground(new Color(242, 245, 250));
        content.add(new DashboardPanel(transactionService, reportService),            "dashboard");
        content.add(new TransactionPanel(transactionService),                         "transactions");
        content.add(new ReportPanel(reportService, transactionService, pdfExportService), "reports");
        content.add(new FraudPanel(fraudService),                                     "fraud");
        return content;
    }

    // ── Status bar ───────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(STATUS_BG);
        bar.setBorder(new EmptyBorder(5, 16, 5, 16));
        bar.setPreferredSize(new Dimension(0, 26));

        JLabel left = new JLabel(
            "\u2713 Connected  \u2502  H2 In-Memory DB  \u2502  Fraud Detection: ON  \u2502  PDF Export: iText 7");
        left.setForeground(new Color(100, 130, 160));
        left.setFont(new Font("SansSerif", Font.PLAIN, 10));

        JLabel right = new JLabel("Smart Expense Tracker \u00a9 2026");
        right.setForeground(new Color(70, 95, 120));
        right.setFont(new Font("SansSerif", Font.PLAIN, 10));

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Live clock ───────────────────────────────────────────────────────────
    private void startClock() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE, MMM d  HH:mm:ss");
        clockTimer = new Timer(1000, e -> clockLabel.setText(LocalDateTime.now().format(dtf)));
        clockTimer.start();
        clockLabel.setText(LocalDateTime.now().format(dtf));
    }
}
