package voteup;

import voteup.controller.VoteUpController;
import voteup.model.Role;
import voteup.model.Tag;
import voteup.model.User;
import voteup.model.VoteUpModel;
import voteup.model.exception.StorageException;
import voteup.persistence.H2VoteUpStore;

/**
 * Application entry point for VoteUp.
 */
public class Main {

    /**
     * Launches VoteUp with a shared local H2 file database, seeds demo data when
     * the DB is empty, and opens the login view.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        try {
            H2VoteUpStore store = new H2VoteUpStore();
            store.ensureSchema();
            VoteUpModel model = new VoteUpModel(store);
            if (store.isEmpty()) {
                seedDemoData(model);
                model.flush();
            }
            VoteUpController controller = new VoteUpController(model);
            controller.start();
        } catch (StorageException | IllegalStateException ex) {
            System.err.println("Failed to start VoteUp with H2 store: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Seeds a few accounts/posts so acceptance walks can start immediately.
     *
     * @param model application model
     */
    private static void seedDemoData(VoteUpModel model) {
        try {
            User admin = model.register("admin", "admin123");
            admin.setRole(Role.ADMIN);

            User alice = model.register("alice", "alice123");
            User bob = model.register("bob", "bob123");

            model.createPost(alice, "Welcome to VoteUp! Try liking this post.", null,
                    java.util.List.of(new Tag("welcome"), new Tag("java")));
            model.createPost(bob, "Bob here — follow me to see this on Following.",
                    "classpath:voteup/demo/sample.png",
                    java.util.List.of(new Tag("intro"), new Tag("photo")));
            model.followUser(alice, bob);
        } catch (Exception ex) {
            System.err.println("Demo seed failed: " + ex.getMessage());
        }
    }
}
