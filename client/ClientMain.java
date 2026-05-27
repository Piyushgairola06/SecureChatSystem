package client;

import server.EncryptionUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Phase 3 – Swing GUI Client
 * Replaces the console client with a proper chat window.
 */
public class ClientMain extends JFrame {

    private static final String HOST = "localhost";
    private static final int    PORT = 5000;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Network ──────────────────────────────────────────────────────────────
    private Socket socket;
    private BufferedReader serverReader;
    private PrintWriter    serverWriter;
    private ClientSender   sender;
    private ClientReceiver receiver;

    // ── UI – Login ───────────────────────────────────────────────────────────
    private JPanel    loginPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JRadioButton loginRadio, registerRadio;
    private JButton   connectBtn;
    private JLabel    statusLabel;

    // ── UI – Chat ────────────────────────────────────────────────────────────
    private JPanel    chatPanel;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton   sendBtn;
    private JLabel    titleLabel;

    // ── Constructor ──────────────────────────────────────────────────────────

    public ClientMain() {
        super("Secure Chat – Phase 3");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 560);
        setMinimumSize(new Dimension(600, 450));
        setLocationRelativeTo(null);

        buildLoginPanel();
        buildChatPanel();

        getContentPane().setLayout(new CardLayout());
        getContentPane().add(loginPanel, "LOGIN");
        getContentPane().add(chatPanel,  "CHAT");

        showLogin();
        setVisible(true);
    }

    // ── Login panel ──────────────────────────────────────────────────────────

    private void buildLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(new Color(30, 30, 46));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(49, 50, 68));
        card.setBorder(new EmptyBorder(30, 40, 30, 40));
        card.setMaximumSize(new Dimension(340, 400));

        JLabel title = new JLabel("🔒 Secure Chat");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(137, 180, 250));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Phase 3  –  AES-128 · SHA-256");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sub.setForeground(new Color(108, 112, 134));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        usernameField = styledTextField("Username");
        passwordField = new JPasswordField();
        stylePasswordField(passwordField, "Password");

        loginRadio    = new JRadioButton("Login",    true);
        registerRadio = new JRadioButton("Register", false);
        styleRadio(loginRadio);
        styleRadio(registerRadio);
        ButtonGroup bg = new ButtonGroup();
        bg.add(loginRadio); bg.add(registerRadio);
        JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        radioRow.setOpaque(false);
        radioRow.add(loginRadio);
        radioRow.add(registerRadio);

        connectBtn = new JButton("Connect");
        connectBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        connectBtn.setBackground(new Color(137, 180, 250));
        connectBtn.setForeground(new Color(30, 30, 46));
        connectBtn.setFocusPainted(false);
        connectBtn.setBorderPainted(false);
        connectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        connectBtn.setMaximumSize(new Dimension(260, 38));
        connectBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectBtn.addActionListener(e -> attemptConnect());

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(243, 139, 168));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Enter key on password field
        passwordField.addActionListener(e -> attemptConnect());

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(20));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(10));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(12));
        card.add(radioRow);
        card.add(Box.createVerticalStrut(16));
        card.add(connectBtn);
        card.add(Box.createVerticalStrut(8));
        card.add(statusLabel);

        loginPanel.add(card);
    }

    // ── Chat panel ───────────────────────────────────────────────────────────

    private void buildChatPanel() {
        chatPanel = new JPanel(new BorderLayout(0, 0));
        chatPanel.setBackground(new Color(30, 30, 46));

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(49, 50, 68));
        topBar.setBorder(new EmptyBorder(10, 16, 10, 16));
        titleLabel = new JLabel("Secure Chat");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(new Color(137, 180, 250));
        JLabel enc = new JLabel("🔒 AES-128 encrypted");
        enc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        enc.setForeground(new Color(166, 227, 161));
        topBar.add(titleLabel, BorderLayout.WEST);
        topBar.add(enc,        BorderLayout.EAST);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        chatArea.setBackground(new Color(30, 30, 46));
        chatArea.setForeground(new Color(205, 214, 244));
        chatArea.setCaretColor(new Color(205, 214, 244));
        chatArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(30, 30, 46));

        // Bottom input bar
        JPanel bottomBar = new JPanel(new BorderLayout(8, 0));
        bottomBar.setBackground(new Color(49, 50, 68));
        bottomBar.setBorder(new EmptyBorder(8, 12, 8, 12));

        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        inputField.setBackground(new Color(69, 71, 90));
        inputField.setForeground(new Color(205, 214, 244));
        inputField.setCaretColor(new Color(205, 214, 244));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(88, 91, 112), 1),
            new EmptyBorder(6, 10, 6, 10)
        ));
        inputField.addActionListener(e -> sendMessage());

        sendBtn = new JButton("Send");
        sendBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        sendBtn.setBackground(new Color(137, 180, 250));
        sendBtn.setForeground(new Color(30, 30, 46));
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());

        // Hint label
        JLabel hint = new JLabel(
            "<html><font color='#6c7086'>" +
            "/msg user text &nbsp;·&nbsp; #channel text &nbsp;·&nbsp; " +
            "/join ch &nbsp;·&nbsp; /leave ch &nbsp;·&nbsp; /list &nbsp;·&nbsp; /help</font></html>"
        );
        hint.setFont(new Font("SansSerif", Font.PLAIN, 10));
        hint.setBorder(new EmptyBorder(0, 12, 4, 0));

        bottomBar.add(inputField, BorderLayout.CENTER);
        bottomBar.add(sendBtn,    BorderLayout.EAST);

        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setBackground(new Color(49, 50, 68));
        bottomWrapper.add(hint,      BorderLayout.NORTH);
        bottomWrapper.add(bottomBar, BorderLayout.CENTER);

        chatPanel.add(topBar,         BorderLayout.NORTH);
        chatPanel.add(scroll,         BorderLayout.CENTER);
        chatPanel.add(bottomWrapper,  BorderLayout.SOUTH);
    }

    // ── Connection logic ─────────────────────────────────────────────────────

    private void attemptConnect() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password required.");
            return;
        }

        connectBtn.setEnabled(false);
        statusLabel.setText("Connecting...");

        new Thread(() -> {
            try {
                socket       = new Socket(HOST, PORT);
                serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                serverWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                sender       = new ClientSender(serverWriter);

                String prompt = serverReader.readLine(); // "LOGIN"
                if (!"LOGIN".equals(prompt)) throw new IOException("Unexpected server response");

                String action = loginRadio.isSelected() ? "LOGIN" : "REGISTER";
                serverWriter.println(action);
                serverWriter.println(username);
                serverWriter.println(password);

                String response = serverReader.readLine();
                if (response == null || !response.startsWith("OK")) {
                    String reason = response != null ? response.replace("REJECT ", "") : "No response";
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("❌ " + reason);
                        connectBtn.setEnabled(true);
                    });
                    socket.close();
                    return;
                }

                // Connected – switch to chat
                final String uname = username;
                SwingUtilities.invokeLater(() -> {
                    titleLabel.setText("Secure Chat  –  " + uname);
                    showChat();
                });

                // Start receiver thread
                receiver = new ClientReceiver(serverReader, msg ->
                    SwingUtilities.invokeLater(() -> appendMessage(msg))
                );
                new Thread(receiver).start();

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("❌ " + ex.getMessage());
                    connectBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private void sendMessage() {
        if (sender == null) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");

        if (text.equals("/quit")) {
            sender.send("/quit");
            System.exit(0);
            return;
        }
        sender.send(text);
        // Echo own message locally
        appendMessage("[" + LocalDateTime.now().format(TIME_FMT) + "] [You] " + text);
    }

    private void appendMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    // ── Card switching ───────────────────────────────────────────────────────

    private void showLogin() {
        ((CardLayout) getContentPane().getLayout()).show(getContentPane(), "LOGIN");
    }

    private void showChat() {
        ((CardLayout) getContentPane().getLayout()).show(getContentPane(), "CHAT");
        inputField.requestFocusInWindow();
    }

    // ── Style helpers ────────────────────────────────────────────────────────

    private JTextField styledTextField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBackground(new Color(69, 71, 90));
        f.setForeground(new Color(205, 214, 244));
        f.setCaretColor(new Color(205, 214, 244));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(88, 91, 112), 1),
            new EmptyBorder(6, 10, 6, 10)
        ));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return f;
    }

    private void stylePasswordField(JPasswordField f, String placeholder) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBackground(new Color(69, 71, 90));
        f.setForeground(new Color(205, 214, 244));
        f.setCaretColor(new Color(205, 214, 244));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(88, 91, 112), 1),
            new EmptyBorder(6, 10, 6, 10)
        ));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    private void styleRadio(JRadioButton r) {
        r.setFont(new Font("SansSerif", Font.PLAIN, 12));
        r.setForeground(new Color(205, 214, 244));
        r.setBackground(new Color(49, 50, 68));
        r.setFocusPainted(false);
    }

    // ── Entry point ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Use system look-and-feel as base, then override with our dark colors
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(ClientMain::new);
    }
}
