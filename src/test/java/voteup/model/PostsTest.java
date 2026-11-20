package voteup.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import voteup.model.exception.AuthenticationException;
import voteup.model.exception.BannedException;
import voteup.model.exception.PostEmptyException;
import voteup.model.exception.PostForbiddenException;
import voteup.model.exception.PostNotFoundException;
import voteup.model.exception.ReplyBlockedException;

/**
 * Process §8–9 — Posts slice: create, reply, soft-delete, tags, empty-content rules.
 */
class PostsTest {

    private VoteUpModel model;
    private final AtomicReference<LocalDateTime> now =
            new AtomicReference<>(LocalDateTime.of(2026, 1, 1, 12, 0));

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() throws Exception {
        model = new VoteUpModel();
        now.set(LocalDateTime.of(2026, 1, 1, 12, 0));
        model.setClock(now::get);
        alice = model.register("alice", "password1");
        bob = model.register("bob", "password1");
    }

    @Test
    void createPostWithTextSucceeds() throws Exception {
        Post post = model.createPost(alice, "hello world", null, null);
        assertNotNull(post.getPostId());
        assertEquals(alice, post.getAuthor());
        assertEquals("hello world", post.getTextContent());
        assertFalse(post.isDeleted());
        assertFalse(post.isReply());
        assertEquals(now.get(), post.getCreatedAt());
    }

    @Test
    void createPostWithImageOnlySucceeds() throws Exception {
        Post post = model.createPost(alice, null, "img/photo.png", null);
        assertEquals("img/photo.png", post.getImageUrl());
        assertTrue(post.getTextContent() == null || post.getTextContent().isBlank());
    }

    @Test
    void createPostWithInitialTags() throws Exception {
        Post post = model.createPost(alice, "tagged", null, List.of(new Tag("#Java"), new Tag("mvc")));
        assertEquals(2, post.getTags().size());
        assertTrue(post.getTags().stream().anyMatch(t -> t.getName().equals("java")));
        assertTrue(post.getTags().stream().anyMatch(t -> t.getName().equals("mvc")));
    }

    @Test
    void createPostRejectsEmptyContent() {
        assertThrows(PostEmptyException.class,
                () -> model.createPost(alice, "  ", null, null));
        assertThrows(PostEmptyException.class,
                () -> model.createPost(alice, null, "  ", null));
        assertThrows(PostEmptyException.class,
                () -> model.createPost(alice, "", "", null));
    }

    @Test
    void createPostRejectsUnknownAuthor() {
        User stranger = new User("x", "stranger", "hash");
        assertThrows(AuthenticationException.class,
                () -> model.createPost(stranger, "hi", null, null));
    }

    @Test
    void createPostRejectsBannedAuthor() {
        alice.setPermanentlyBanned(true);
        assertThrows(BannedException.class,
                () -> model.createPost(alice, "hi", null, null));
    }

    @Test
    void createPostRejectsTempBannedAuthor() {
        alice.setBanExpiresAt(now.get().plusHours(1));
        assertThrows(BannedException.class,
                () -> model.createPost(alice, "hi", null, null));
    }

    @Test
    void createReplySucceedsAsFullPostWithParent() throws Exception {
        Post parent = model.createPost(alice, "root", null, null);
        Post reply = model.createReply(bob, parent, "a reply", null);
        assertTrue(reply.isReply());
        assertEquals(parent, reply.getParentPost());
        assertEquals(bob, reply.getAuthor());
        assertEquals("a reply", reply.getTextContent());
    }

    @Test
    void createReplyAllowsImageOnly() throws Exception {
        Post parent = model.createPost(alice, "root", null, null);
        Post reply = model.createReply(bob, parent, null, "img/r.png");
        assertEquals("img/r.png", reply.getImageUrl());
    }

    @Test
    void createReplyRejectsEmptyContent() throws Exception {
        Post parent = model.createPost(alice, "root", null, null);
        assertThrows(PostEmptyException.class,
                () -> model.createReply(bob, parent, " ", null));
    }

    @Test
    void createReplyRejectsMissingOrDeletedParent() throws Exception {
        Post missing = new Post("nope", alice, "ghost", null);
        assertThrows(PostNotFoundException.class,
                () -> model.createReply(bob, missing, "hi", null));

        Post parent = model.createPost(alice, "root", null, null);
        model.deletePost(alice, parent);
        assertThrows(PostNotFoundException.class,
                () -> model.createReply(bob, parent, "hi", null));
    }

    @Test
    void createReplyRejectsWhenBlockedByParentAuthor() throws Exception {
        Post parent = model.createPost(alice, "root", null, null);
        model.putBlock(alice, bob);
        assertThrows(ReplyBlockedException.class,
                () -> model.createReply(bob, parent, "hi", null));
    }

    @Test
    void authorCanSoftDeleteOwnPost() throws Exception {
        Post post = model.createPost(alice, "bye", null, null);
        model.deletePost(alice, post);
        assertTrue(post.isDeleted());
        assertEquals(alice, post.getDeletedBy());
    }

    @Test
    void modAndAdminCanSoftDeleteOthersPost() throws Exception {
        Post post = model.createPost(alice, "target", null, null);
        bob.setRole(Role.MOD);
        model.deletePost(bob, post);
        assertTrue(post.isDeleted());
        assertEquals(bob, post.getDeletedBy());

        Post post2 = model.createPost(alice, "target2", null, null);
        User admin = model.register("admin", "password1");
        admin.setRole(Role.ADMIN);
        model.deletePost(admin, post2);
        assertTrue(post2.isDeleted());
    }

    @Test
    void otherUserCannotDeletePost() throws Exception {
        Post post = model.createPost(alice, "mine", null, null);
        assertThrows(PostForbiddenException.class, () -> model.deletePost(bob, post));
        assertFalse(post.isDeleted());
    }

    @Test
    void deletePostRejectsMissingOrAlreadyDeleted() throws Exception {
        Post missing = new Post("nope", alice, "ghost", null);
        assertThrows(PostNotFoundException.class, () -> model.deletePost(alice, missing));

        Post post = model.createPost(alice, "once", null, null);
        model.deletePost(alice, post);
        assertThrows(PostNotFoundException.class, () -> model.deletePost(alice, post));
    }

    @Test
    void authorCanAddAndRemoveTags() throws Exception {
        Post post = model.createPost(alice, "t", null, null);
        model.addTags(alice, post, List.of(new Tag("#Java"), new Tag("swing")));
        assertEquals(2, post.getTags().size());

        model.addTags(alice, post, List.of(new Tag("java")));
        assertEquals(2, post.getTags().size());

        model.removeTags(alice, post, List.of(new Tag("JAVA")));
        assertEquals(1, post.getTags().size());
        assertEquals("swing", post.getTags().get(0).getName());
    }

    @Test
    void nonAuthorCannotChangeTags() throws Exception {
        Post post = model.createPost(alice, "t", null, null);
        assertThrows(PostForbiddenException.class,
                () -> model.addTags(bob, post, List.of(new Tag("x"))));
        assertThrows(PostForbiddenException.class,
                () -> model.removeTags(bob, post, List.of(new Tag("x"))));
    }

    @Test
    void tagOpsRejectDeletedPost() throws Exception {
        Post post = model.createPost(alice, "t", null, null);
        model.deletePost(alice, post);
        assertThrows(PostNotFoundException.class,
                () -> model.addTags(alice, post, List.of(new Tag("x"))));
        assertThrows(PostNotFoundException.class,
                () -> model.removeTags(alice, post, List.of(new Tag("x"))));
    }
}
