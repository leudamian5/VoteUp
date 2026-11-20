package voteup.view;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import voteup.controller.Controller;
import voteup.controller.VoteUpController;
import voteup.model.Model;
import voteup.model.ModelEvent;
import voteup.model.Role;
import voteup.model.User;

/**
 * VoteUp user profile screen.
 */
public class ProfileView extends JFrameView {

    private final JTextArea details = new JTextArea();
    private User target;

    /**
     * @param model      model to observe
     * @param controller controller for actions
     */
    public ProfileView(Model model, Controller controller) {
        super(model, controller);
        setTitle("VoteUp — Profile");
        buildUi();
    }

    private VoteUpController ctrl() {
        return (VoteUpController) getController();
    }

    private void buildUi() {
        ViewSupport.styleFrame(this, 720, 560);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(ViewSupport.BG);
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(ViewSupport.titledHeader("Profile"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);
        center.add(new NavBar(ctrl(), this, () -> {
            if (target != null) {
                showProfile(target);
            }
        }), BorderLayout.NORTH);
        center.add(ViewSupport.postListScroll(details), BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton follow = new JButton("Follow");
        JButton unfollow = new JButton("Unfollow");
        JButton block = new JButton("Block");
        JButton unblock = new JButton("Unblock");
        JButton edit = new JButton("Edit my profile");
        JButton deleteMe = new JButton("Delete my account");
        JButton tempBan = new JButton("Temp ban");
        JButton permBan = new JButton("Perm ban");
        JButton promote = new JButton("Promote");
        JButton demote = new JButton("Demote");
        actions.add(follow);
        actions.add(unfollow);
        actions.add(block);
        actions.add(unblock);
        actions.add(edit);
        actions.add(deleteMe);
        actions.add(tempBan);
        actions.add(permBan);
        actions.add(promote);
        actions.add(demote);
        center.add(actions, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);
        setContentPane(root);

        follow.addActionListener(e -> social(true, false, false, false));
        unfollow.addActionListener(e -> social(false, true, false, false));
        block.addActionListener(e -> social(false, false, true, false));
        unblock.addActionListener(e -> social(false, false, false, true));
        edit.addActionListener(e -> editProfile());
        deleteMe.addActionListener(e -> deleteAccount());
        tempBan.addActionListener(e -> ban(true));
        permBan.addActionListener(e -> ban(false));
        promote.addActionListener(e -> promote(true));
        demote.addActionListener(e -> promote(false));
    }

    private void social(boolean follow, boolean unfollow, boolean block, boolean unblock) {
        User me = ctrl().getCurrentUser();
        if (me == null || target == null) {
            return;
        }
        try {
            if (follow) {
                ctrl().follow(me, target);
            } else if (unfollow) {
                ctrl().unfollow(me, target);
            } else if (block) {
                ctrl().block(me, target);
                ViewSupport.showInfo(this, "Blocked. Returning to main feed.");
                ctrl().showMainFeed();
                return;
            } else if (unblock) {
                ctrl().unblock(me, target);
            }
            showProfile(target);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void editProfile() {
        User me = ctrl().getCurrentUser();
        if (me == null || target == null || !me.getUserId().equals(target.getUserId())) {
            showError("You can only edit your own profile.");
            return;
        }
        JTextField bio = new JTextField(me.getBio() == null ? "" : me.getBio());
        JTextField pic = new JTextField(me.getProfilePictureUrl() == null ? "" : me.getProfilePictureUrl());
        Object[] fields = {"Bio:", bio, "Profile picture URL:", pic};
        int ok = JOptionPane.showConfirmDialog(this, fields, "Edit profile", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            String b = bio.getText().isBlank() ? null : bio.getText().trim();
            String p = pic.getText().isBlank() ? null : pic.getText().trim();
            ctrl().updateProfile(me, b, p);
            showProfile(me);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteAccount() {
        User me = ctrl().getCurrentUser();
        if (me == null || target == null || !me.getUserId().equals(target.getUserId())) {
            showError("You can only delete your own account.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(
                this, "Delete your account? Posts remain; username becomes deleted_<id>.",
                "Delete account", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            ctrl().deleteAccount(me);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void ban(boolean temp) {
        User me = ctrl().getCurrentUser();
        if (me == null || target == null || !ctrl().isStaff(me)) {
            showError("Moderator or admin required.");
            return;
        }
        try {
            if (temp) {
                String hours = JOptionPane.showInputDialog(this, "Duration hours:", "24");
                if (hours == null) {
                    return;
                }
                ctrl().issueTempBan(me, target, Integer.parseInt(hours.trim()));
            } else {
                ctrl().issuePermBan(me, target);
            }
            showProfile(target);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void promote(boolean up) {
        User me = ctrl().getCurrentUser();
        if (me == null || target == null || me.getRole() != Role.ADMIN) {
            showError("Admin required.");
            return;
        }
        try {
            if (up) {
                ctrl().promoteToMod(me, target);
            } else {
                ctrl().demoteFromMod(me, target);
            }
            showProfile(target);
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
        });
    }

    /**
     * Displays an error message to the user.
     *
     * @param message error text
     */
    public void showError(String message) {
        ViewSupport.showError(this, message);
    }

    /**
     * Shows a user profile.
     *
     * @param target profile owner
     */
    public void showProfile(User target) {
        this.target = target;
        User me = ctrl().getCurrentUser();
        try {
            User profile = ctrl().getProfile(me, target);
            this.target = profile;
            StringBuilder sb = new StringBuilder();
            sb.append('@').append(profile.getUsername()).append('\n');
            sb.append("Role: ").append(profile.getRole()).append('\n');
            sb.append("Deleted: ").append(profile.isDeleted()).append('\n');
            sb.append("Permanently banned: ").append(profile.isPermanentlyBanned()).append('\n');
            sb.append("Temp ban active: ").append(profile.isTempBanned()).append('\n');
            sb.append("Bio: ").append(profile.getBio() == null ? "(none)" : profile.getBio()).append('\n');
            sb.append("Picture: ")
                    .append(profile.getProfilePictureUrl() == null ? "(none)" : profile.getProfilePictureUrl())
                    .append('\n');
            if (me != null && !me.getUserId().equals(profile.getUserId())) {
                sb.append("You follow: ").append(ctrl().isFollowing(me, profile)).append('\n');
                sb.append("You blocked: ").append(ctrl().isBlocked(me, profile)).append('\n');
            }
            if (!profile.getAchievements().isEmpty()) {
                sb.append("\nAchievements:\n");
                profile.getAchievements().forEach(a ->
                        sb.append(" - ").append(a.getTitle()).append(" (").append(a.getLeaderboardPeriod())
                                .append(")\n"));
            }
            details.setText(sb.toString());
            details.setCaretPosition(0);
            setTitle("VoteUp — @" + profile.getUsername());
        } catch (Exception ex) {
            showError(ex.getMessage());
            ctrl().showMainFeed();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged(ModelEvent event) {
        if (isVisible() && target != null) {
            showProfile(target);
        }
    }
}
