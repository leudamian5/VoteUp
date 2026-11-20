package voteup.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import voteup.controller.Controller;
import voteup.controller.VoteUpController;
import voteup.model.Model;
import voteup.model.ModelEvent;

/**
 * VoteUp login / registration screen.
 */
public class LoginView extends JFrameView {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JLabel statusLabel = new JLabel(" ");

    /**
     * @param model      model to observe
     * @param controller controller for actions
     */
    public LoginView(Model model, Controller controller) {
        super(model, controller);
        setTitle("VoteUp — Login / Register");
        buildUi();
    }

    private VoteUpController ctrl() {
        return (VoteUpController) getController();
    }

    private void buildUi() {
        ViewSupport.styleFrame(this, 480, 360);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(ViewSupport.BG);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.add(ViewSupport.titledHeader("VoteUp"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        gc.gridy = 0;
        form.add(new JLabel("Username"), gc);
        gc.gridx = 1;
        form.add(usernameField, gc);
        gc.gridx = 0;
        gc.gridy = 1;
        form.add(new JLabel("Password"), gc);
        gc.gridx = 1;
        form.add(passwordField, gc);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        buttons.add(loginBtn);
        buttons.add(registerBtn);

        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        form.add(buttons, gc);

        statusLabel.setForeground(ViewSupport.MUTED);
        statusLabel.setFont(ViewSupport.BODY);
        gc.gridy = 3;
        form.add(statusLabel, gc);

        root.add(form, BorderLayout.CENTER);
        JLabel hint = new JLabel("Demo: admin/admin123 · alice/alice123 · bob/bob123");
        hint.setForeground(ViewSupport.MUTED);
        hint.setFont(ViewSupport.BODY);
        root.add(hint, BorderLayout.SOUTH);
        setContentPane(root);

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> doRegister());
        passwordField.addActionListener(e -> doLogin());
    }

    private void doLogin() {
        try {
            ctrl().login(usernameField.getText(), new String(passwordField.getPassword()));
            passwordField.setText("");
            statusLabel.setText(" ");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void doRegister() {
        try {
            ctrl().register(usernameField.getText(), new String(passwordField.getPassword()));
            statusLabel.setText("Registered. You can log in now.");
            ViewSupport.showInfo(this, "Account created. Please log in.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Makes this window visible.
     */
    public void showWindow() {
        ViewSupport.onEdt(() -> {
            setVisible(true);
            toFront();
            usernameField.requestFocusInWindow();
        });
    }

    /**
     * Displays an error message to the user.
     *
     * @param message error text
     */
    public void showError(String message) {
        statusLabel.setText(message == null ? "Error" : message);
        ViewSupport.showError(this, message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged(ModelEvent event) {
        // Login screen does not need live refresh.
    }
}
