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
| Account | `AccountTest.java` | deleteAccount / updateProfile / getProfile / username reuse |

### Run the GUI application

```bash
cd VoteUp
gradle run
```

**Demo logins** (seeded on startup):

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ADMIN |
| `alice` | `alice123` | USER (follows bob; has a tagged post) |
| `bob` | `bob123` | USER |

You can also **Register** a new account from the login screen.

**Screens available for acceptance screenshots:** Login/Register, Main feed, Following feed, Tag feed, Post + replies, Profile (follow/block/edit/moderation), Leaderboard dialog, Search-users dialog, New-post / Reply dialogs.

### Attaching images to posts

In **New post** / **Reply**, the image field accepts any of these formats (Swing loads and scales the picture in the feed):

| Format | Example |
|--------|---------|
| HTTP(S) URL | `https://example.com/pic.jpg` |
| Local absolute path | `C:\Users\You\Pictures\cat.png` |
| Local relative path | `demo\cat.png` (relative to the process working directory, usually `VoteUp/`) |
| `file:` URI | `file:///C:/Users/You/Pictures/cat.png` |
| Classpath resource | `classpath:voteup/demo/sample.png` |

Notes:

- Supported types are whatever Java `ImageIO` can read on your JDK (typically **PNG, JPEG, GIF, BMP**).
- A bundled demo image ships at `src/main/resources/voteup/demo/sample.png`. Bob’s seeded post uses `classpath:voteup/demo/sample.png`.
- If the file/URL cannot be loaded, the post still appears and shows a small failure hint under the text.

### Generate Javadoc

```bash
cd VoteUp
gradle javadocAll
```

Then open `build/docs/javadoc/index.html` in a browser.

### Notes

- Persistence is still in-memory (H2 file mode comes later). Data resets when the app exits.
- Password hashing is SHA-256 for the local demo (`voteup.util.PasswordHasher`), not production bcrypt.
- Design lock: one engagement row per (user, post); LIKE↔DISLIKE switch sets `createdAt` to now.
- Design lock: block removes follows in **both** directions; unblock does not restore them.
- Design lock: feeds are newest-first with tie-break `created_at DESC`, `author.created_at ASC`, `post_id ASC`; page size max `FEED_PAGE_LIMIT` (25).
- Design lock: leaderboard standings computed on demand (likes−dislikes since `lastResetTime`); eligible users with no engagements count as **implicit 0** (zeros rank above negatives); top 10; equal scores use `firstReachedAt` then account age; exclude deleted/perm-banned; achievements awarded per reset event.
- Design lock: temp ban uses `banExpiresAt` (not login lockout); perm ban ≠ delete (username kept, posts remain); bans revoke sessions.
- Design lock: account delete → `deleted_<userId>`, sessions revoked, posts remain; username becomes reusable; blocks hide profiles.
