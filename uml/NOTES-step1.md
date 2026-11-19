# VoteUp UML — Process Step 1 (First Take)

**Status:** First take only — not final. Will be updated when code exists and is reverse-engineered (Process §6–7, §12).

**Location:** `VoteUp/uml/` (GitHub-connected repo root: `VoteUp/`)

---

## Diagram files (Mermaid)

| File | Contents |
|------|----------|
| `01-mvc-framework.mmd` | `Model`, `View`, `Controller`, `ModelListener`, `ModelEvent`, `AbstractModel`, `AbstractController`, `JFrameView` |
| `02-domain-model.mmd` | Domain entities, enums, `VoteUpModel` (extends `AbstractModel`) |
| `03-controllers-views-main.mmd` | `VoteUpController`, Swing views, `Main.main` |
| `04-exceptions.mmd` | Initial checked/domain exception hierarchy |

Render in [Mermaid Live Editor](https://mermaid.live) or any Mermaid-capable viewer. Export SVG/PNG for submission when needed.

---

## Packages (Process / submission)

| Package | Responsibility |
|---------|----------------|
| `voteup.model` | MVC model framework types + domain + `VoteUpModel` |
| `voteup.model.exception` | Domain/API exceptions (initial set) |
| `voteup.controller` | MVC controller framework + `VoteUpController` |
| `voteup.view` | MVC view framework + Swing screens |
| `voteup` | `Main` entry point (`main`) |

Optional later: `voteup.util` (e.g. H2 helpers) — not required for this first take.

---

## MVC shape (matches course Process §1)

- **Interfaces:** `Model`, `View`, `Controller`, `ModelListener`
- **Event:** `ModelEvent`
- **Abstract classes:** `AbstractModel`, `AbstractController`, `JFrameView`
- **App model:** `VoteUpModel extends AbstractModel` — business API lives here so model can be unit-tested without controller/view (Process §9)
- **App controller:** `VoteUpController extends AbstractController` — delegates to `VoteUpModel`, wires views
- **Views:** Login, Main feed, Following, Tag, Post page, Profile — extend `JFrameView`, implement `modelChanged`

---

## Constructors & `main` (first take)

- Domain types: primary constructors as shown on the diagrams
- `VoteUpModel()`, `VoteUpController()`, `VoteUpController(VoteUpModel)`
- Each view: `ViewName(Model model, Controller controller)`
- `voteup.Main.main(String[] args)` — creates model, controller, shows `LoginView`

---

## Initial exceptions & which ops throw them (first take)

| Operation (model/controller) | Throws (initial) |
|------------------------------|------------------|
| `register` | `UsernameTakenException`, `ValidationException` |
| `login` | `AuthenticationException`, `LockoutException`, `BannedException` |
| `logout` / `deleteAccount` | `AuthenticationException`, `AuthorizationException` |
| `updateProfile` | `AuthenticationException`, `AuthorizationException` |
| `getProfile` | `ProfileUnavailableException`, `UserNotFoundException` |
| `follow` / `unfollow` / `block` / `unblock` | `AuthenticationException`, `SocialException`, `UserNotFoundException` |
| `createPost` / `createReply` | `AuthenticationException`, `PostEmptyException`, `ReplyBlockedException`, `PostNotFoundException`, `BannedException` |
| `deletePost` / `addTags` / `removeTags` | `AuthenticationException`, `PostForbiddenException`, `PostNotFoundException` |
| `applyEngagement` / `removeEngagement` | `AuthenticationException`, `EngagementException`, `PostNotFoundException` |
| Feed / search ops | `AuthenticationException` (where required), `ValidationException` |
| `issueTempBan` / `issuePermBan` / `promoteToMod` / `demoteFromMod` | `AuthorizationException`, `ModerationException`, `ValidationException` |
| Persistence-facing ops (later) | `StorageException` |

Exact `throws` clauses will be refined in Process §3 (API stubs + Javadoc).

---

## Locked design decisions (context only — implement later in increments)

Used to shape attributes/operations; not fully implemented yet:

1. Soft-delete posts; block ⇒ unfollow both directions  
2. Separate feed/profile concerns via `VoteUpModel` API (controllers/views thin)  
3. Deleted username → `deleted_<userId>` (reusable names)  
4. LIKE↔DISLIKE upsert; `createdAt = now` on switch  
5. Local H2 file persistence (later); no cloud  
6. Replies = posts with `parentPost`; text and/or image  
7. Feed tie-break: `created_at DESC`, `author.created_at ASC`, `post_id ASC`  
8. `banExpiresAt` ≠ login lockout; `lockoutTier` + `lastLockEndedAt` for 15m→30m→24h reset  
9. Leaderboard: no standings cache; `firstReachedAt` for 10th-place tie; achievements per reset event  
10. Standings exclude deleted / permanently banned users  

---

## Explicitly out of scope for this diagram

- Polls, GIFs, forgot-password (not in SRS)  
- Full method bodies / H2 schema classes (Process §2+)  
- JUnit (Process §8+)  

---

## Next process step

**Process §2:** ✅ Done — skeleton under `VoteUp/src/main/java` (compiles).

**Process §3–5:** ✅ Done — API declarations + Javadoc + compiling stubs; `gradle javadocAll` produces docs under `build/docs/javadoc` (all visibility).

**Process §6:** Reverse-engineer UML from current code (optional sync of `uml/*.mmd`).

**Process §7:** Review API vs Javadoc/UML; iterate if needed.

**Process §8:** First JUnit + real implementations (model methods).
