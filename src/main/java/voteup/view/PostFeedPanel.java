package voteup.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import voteup.model.Post;
import voteup.model.Tag;

/**
 * Scrollable list of post cards that can render attached images.
 */
final class PostFeedPanel extends JPanel {

    private final JPanel list = new JPanel();
    private final JScrollPane scroll;

    PostFeedPanel() {
        super(new BorderLayout());
        setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(ViewSupport.BG);
        list.setBorder(new EmptyBorder(4, 4, 4, 4));
        scroll = new JScrollPane(list);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xD0, 0xD8, 0xD5)));
        add(scroll, BorderLayout.CENTER);
    }

    void setPosts(List<Post> posts) {
        list.removeAll();
        if (posts == null || posts.isEmpty()) {
            JLabel empty = new JLabel("(no posts)");
            empty.setFont(ViewSupport.BODY);
            empty.setForeground(ViewSupport.MUTED);
            empty.setBorder(new EmptyBorder(12, 12, 12, 12));
            list.add(empty);
        } else {
            for (int i = 0; i < posts.size(); i++) {
                list.add(card(i + 1, posts.get(i)));
                list.add(Box.createVerticalStrut(10));
            }
        }
        list.revalidate();
        list.repaint();
        scroll.getVerticalScrollBar().setValue(0);
    }

    private static JPanel card(int number, Post post) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ViewSupport.PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD0, 0xD8, 0xD5)),
                new EmptyBorder(10, 12, 10, 12)));
        card.setAlignmentX(LEFT_ALIGNMENT);

        String tags = post.getTags().stream()
                .map(Tag::getName)
                .map(t -> "#" + t)
                .collect(Collectors.joining(" "));

        StringBuilder meta = new StringBuilder();
        meta.append('[').append(number).append("] @").append(post.getAuthor().getUsername());
        if (post.isReply()) {
            meta.append("  (reply)");
        }
        meta.append("  ·  score ").append(post.score());
        if (post.getCreatedAt() != null) {
            meta.append("  ·  ").append(ViewSupport.TIME.format(post.getCreatedAt()));
        }
        if (!tags.isBlank()) {
            meta.append("  ·  ").append(tags);
        }

        JLabel metaLabel = new JLabel(meta.toString());
        metaLabel.setFont(ViewSupport.HEADING);
        metaLabel.setForeground(ViewSupport.TEXT);
        metaLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(metaLabel);
        card.add(Box.createVerticalStrut(6));

        if (post.getTextContent() != null && !post.getTextContent().isBlank()) {
            JTextArea text = new JTextArea(post.getTextContent().trim());
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setFont(ViewSupport.BODY);
            text.setForeground(ViewSupport.TEXT);
            text.setBackground(ViewSupport.PANEL);
            text.setBorder(BorderFactory.createEmptyBorder());
            text.setAlignmentX(LEFT_ALIGNMENT);
            card.add(text);
            card.add(Box.createVerticalStrut(8));
        }

        if (post.getImageUrl() != null && !post.getImageUrl().isBlank()) {
            JLabel image = ImageSupport.imageLabel(post.getImageUrl());
            if (image != null) {
                card.add(image);
            }
        }

        card.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }
}
