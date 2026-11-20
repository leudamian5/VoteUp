# VoteUp UML & how to run

## UML diagrams

Process **step 1 / 6** class diagrams (Mermaid + reverse-engineered SVG).

1. Open `*.mmd` in [Mermaid Live](https://mermaid.live) or a Mermaid preview.
2. Open `model-uml.svg`, `controller-uml.svg`, `view-uml.svg` in a browser or IDE.
3. Read `NOTES-step1.md` for packages, exceptions, and design context.

| Order | File |
|------:|------|
| 1 | `01-mvc-framework.mmd` |
| 2 | `02-domain-model.mmd` |
| 3 | `03-controllers-views-main.mmd` |
| 4 | `04-exceptions.mmd` |

---

## How to run VoteUp (for yourself)

Work from the **`VoteUp/`** project root (the GitHub repo folder), not the parent `working-repo` folder.

### Prerequisites

- JDK 17+ (this machine uses JDK 27 via `gradle.properties`)
- Gradle on your PATH (`gradle`), or use a wrapper later if you add one

### Compile

```bash
cd VoteUp
gradle compileJava
```

### Run unit tests

```bash
cd VoteUp
gradle test
```

Model tests under `src/test/java/voteup/model/`:

| Slice | File | Covers |
|-------|------|--------|
| Auth | `AuthTest.java` | register / login / lockout / logout |
| Posts | `PostsTest.java` | create / reply / soft-delete / tags / empty content |
| Engagement | `EngagementTest.java` | like / dislike / switch (`createdAt` reset) / remove / score |
| Social | `SocialTest.java` | follow / unfollow / block / unblock / visibility helpers |
| Feeds | `FeedsTest.java` | main / following / tag / post+replies / search / paging / tie-break |
| Leaderboard | `LeaderboardTest.java` | standings / reset / achievements / firstReachedAt ties |
| Moderation | `ModerationTest.java` | temp/perm ban / promote / demote / session revoke / mod delete |

### Run the application

```bash
cd VoteUp
gradle run
```

**Current behavior:** `voteup.Main` constructs `VoteUpModel`, `VoteUpController`, and `LoginView`, but the Swing UI is still a stub (no usable windows yet). Process §8–9 is implementing the **model** first; GUI comes in a later increment.

Until the view is implemented, use **`gradle test`** to exercise model behavior.

### Generate Javadoc

```bash
cd VoteUp
gradle javadocAll
```

Then open `build/docs/javadoc/index.html` in a browser.

### Notes

- Persistence is still in-memory (H2 file mode comes later).
- Password hashing is SHA-256 for the local demo (`voteup.util.PasswordHasher`), not production bcrypt.
- Design lock: one engagement row per (user, post); LIKE↔DISLIKE switch sets `createdAt` to now.
- Design lock: block removes follows in **both** directions; unblock does not restore them.
- Design lock: feeds are newest-first with tie-break `created_at DESC`, `author.created_at ASC`, `post_id ASC`; page size max `FEED_PAGE_LIMIT` (25).
- Design lock: leaderboard standings computed on demand (likes−dislikes since `lastResetTime`); top 10; equal scores use `firstReachedAt` then account age; exclude deleted/perm-banned; achievements awarded per reset event.
- Design lock: temp ban uses `banExpiresAt` (not login lockout); perm ban ≠ delete (username kept, posts remain); bans revoke sessions.
