package voteup.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import voteup.model.Post;
import voteup.model.Tag;

/**
 * Shared Swing helpers for VoteUp screens.
 */
final class ViewSupport {

    static final Color BG = new Color(0xF4, 0xF7, 0xF5);
    static final Color PANEL = Color.WHITE;
    static final Color ACCENT = new Color(0x4B, 0x26, 0xBD);
    static final Color TEXT = new Color(0x1C, 0x28, 0x2A);
    static final Color MUTED = new Color(0x5C, 0x6B, 0x6E);
    static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);
    static final Font HEADING = new Font("Segoe UI", Font.BOLD, 16);
    static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ViewSupport() {
    }

    static void styleFrame(JFrameView frame, int width, int height) {
        frame.setDefaultCloseOperation(JFrameView.HIDE_ON_CLOSE);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);
    }

    static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "VoteUp", JOptionPane.ERROR_MESSAGE);
    }

    static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "VoteUp", JOptionPane.INFORMATION_MESSAGE);
    }

    static String formatPost(Post post) {
        String tags = post.getTags().stream()
                .map(Tag::getName)
                .map(t -> "#" + t)
                .collect(Collectors.joining(" "));
        StringBuilder sb = new StringBuilder();
        sb.append('@').append(post.getAuthor().getUsername());
        if (post.isReply()) {
            sb.append("  (reply)");
        }
        sb.append("  ·  score ").append(post.score());
        sb.append("  ·  ").append(post.getCreatedAt() == null ? "" : TIME.format(post.getCreatedAt()));
        sb.append('\n');
        if (post.getTextContent() != null && !post.getTextContent().isBlank()) {
            sb.append(post.getTextContent().trim()).append('\n');
        }
        if (post.getImageUrl() != null && !post.getImageUrl().isBlank()) {
            sb.append("[image] ").append(post.getImageUrl().trim()).append('\n');
        }
        if (!tags.isBlank()) {
            sb.append(tags);
        }
        return sb.toString().trim();
    }

    static JScrollPane postListScroll(JTextArea area) {
        area.setEditable(false);
        area.setFont(BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(PANEL);
        area.setForeground(TEXT);
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xD0, 0xD8, 0xD5)));
        return scroll;
    }

    static void fillPosts(JTextArea area, List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            area.setText("(no posts)");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < posts.size(); i++) {
            if (i > 0) {
                sb.append("\n\n────────────────────────\n\n");
            }
            sb.append('[').append(i + 1).append("] ").append(formatPost(posts.get(i)));
        }
        area.setText(sb.toString());
        area.setCaretPosition(0);
    }

    static JPanel titledHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ACCENT);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel label = new JLabel(title);
        label.setForeground(Color.WHITE);
        label.setFont(TITLE);
        header.add(label, BorderLayout.WEST);
        return header;
    }

    static Dimension buttonSize() {
        return new Dimension(110, 28);
    }

    static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
