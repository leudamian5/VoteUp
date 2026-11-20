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

### Run unit tests (including Auth)

```bash
cd VoteUp
gradle test
```

Auth tests live in `src/test/java/voteup/model/AuthTest.java` (register / login / lockout / logout).

### Run the application

```bash
cd VoteUp
gradle run
```

**Current behavior:** `voteup.Main` constructs `VoteUpModel`, `VoteUpController`, and `LoginView`, but the Swing UI is still a stub (no usable windows yet). Process §8 is implementing the **model** first; GUI comes in a later increment.

Until the view is implemented, use **`gradle test`** to exercise Auth behavior.

### Generate Javadoc

```bash
cd VoteUp
gradle javadocAll
```

Then open `build/docs/javadoc/index.html` in a browser.

### Notes

- Persistence is still in-memory (H2 file mode comes later).
- Password hashing is SHA-256 for the local demo (`voteup.util.PasswordHasher`), not production bcrypt.
