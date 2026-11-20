package voteup.view;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import voteup.controller.VoteUpController;
import voteup.model.LeaderboardPeriod;
import voteup.model.User;

/**
 * Shared top navigation used by authenticated screens.
 */
final class NavBar extends JPanel {

    NavBar(VoteUpController ctrl, JFrameView host, Runnable onRefresh) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        setBackground(ViewSupport.PANEL);

        User me = ctrl.getCurrentUser();
        JLabel who = new JLabel(me == null ? "" : ("Signed in as @" + me.getUsername()
                + " (" + me.getRole() + ")"));
        who.setFont(ViewSupport.BODY);
        add(who);

        JButton main = new JButton("Main");
        JButton following = new JButton("Following");
        JButton tag = new JButton("Tag");
        JButton search = new JButton("Search users");
        JButton meBtn = new JButton("My profile");
        JButton board = new JButton("Leaderboard");
        JButton refresh = new JButton("Refresh");
        JButton logout = new JButton("Logout");

        add(main);
        add(following);
        add(tag);
        add(search);
        add(meBtn);
        add(board);
        add(refresh);
        add(logout);

        main.addActionListener(e -> ctrl.showMainFeed());
        following.addActionListener(e -> ctrl.showFollowingFeed());
        tag.addActionListener(e -> {
            String raw = JOptionPane.showInputDialog(host, "Tag name:", "java");
            if (raw != null && !raw.isBlank()) {
                ctrl.showTagFeed(raw.trim());
            }
        });
        search.addActionListener(e -> {
            String q = JOptionPane.showInputDialog(host, "Search users:", "");
            if (q == null || q.isBlank()) {
                return;
            }
            try {
                var users = ctrl.searchUsers(ctrl.getCurrentUser(), q.trim());
                if (users.isEmpty()) {
                    ViewSupport.showInfo(host, "No users matched.");
                    return;
                }
                String[] names = users.stream().map(User::getUsername).toArray(String[]::new);
                String pick = (String) JOptionPane.showInputDialog(
                        host, "Open profile:", "Search",
                        JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
                if (pick != null) {
                    users.stream()
                            .filter(u -> u.getUsername().equals(pick))
                            .findFirst()
                            .ifPresent(ctrl::showProfile);
                }
            } catch (Exception ex) {
                ViewSupport.showError(host, ex.getMessage());
            }
        });
        meBtn.addActionListener(e -> {
            if (ctrl.getCurrentUser() != null) {
                ctrl.showProfile(ctrl.getCurrentUser());
            }
        });
        board.addActionListener(e -> showLeaderboard(ctrl, host));
        refresh.addActionListener(e -> {
            if (onRefresh != null) {
                onRefresh.run();
            }
        });
        logout.addActionListener(e -> {
            try {
                ctrl.logout();
            } catch (Exception ex) {
                ViewSupport.showError(host, ex.getMessage());
            }
        });
    }

    private static void showLeaderboard(VoteUpController ctrl, JFrameView host) {
        LeaderboardPeriod[] periods = LeaderboardPeriod.values();
        LeaderboardPeriod period = (LeaderboardPeriod) JOptionPane.showInputDialog(
                host, "Period:", "Leaderboard",
                JOptionPane.PLAIN_MESSAGE, null, periods, LeaderboardPeriod.DAILY);
        if (period == null) {
            return;
        }
        var standings = ctrl.getStandings(period);
        StringBuilder sb = new StringBuilder("Top 10 — ").append(period).append('\n');
        if (standings.isEmpty()) {
            sb.append("(empty)");
        } else {
            for (int i = 0; i < standings.size(); i++) {
                sb.append(i + 1).append(". @").append(standings.get(i).getUsername()).append('\n');
            }
        }
        Object[] options = ctrl.getCurrentUser() != null
                && ctrl.getCurrentUser().getRole().name().equals("ADMIN")
                ? new Object[] {"OK", "Reset period"}
                : new Object[] {"OK"};
        int choice = JOptionPane.showOptionDialog(
                host, sb.toString(), "Leaderboard",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);
        if (choice == 1) {
            ctrl.resetLeaderboard(period);
            ViewSupport.showInfo(host, "Leaderboard reset. Achievements awarded.");
        }
    }
}
