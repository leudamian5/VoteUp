package voteup.model;

/**
 * Authorization role for a {@link User}.
 */
public enum Role {
    /** Standard community member. */
    USER,
    /** Moderator with ban/delete privileges. */
    MOD,
    /** Administrator who can promote/demote mods. */
    ADMIN
}
