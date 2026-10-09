# Nada Backend Intern Assignment: Complete Execution Plan

**Task:** In-memory HTTP API in Core Java 17+ for people and connections, with shortest-path lookup (max 3 hops).
**Deadline:** Saturday, 10 Oct 2026, 9:00 PM IST. Reply to the assignment email with the GitHub repo link.
**Personal target:** Be completely done by ~6:00 PM Saturday, which leaves a 3-hour buffer.

---

## 1. What we are building

A small HTTP server with 4 endpoints:

| # | Endpoint | Purpose |
|---|---|---|
| 1 | `POST /people` | Create a person (server assigns the ID) |
| 2 | `POST /knows` | Create a mutual connection between two people |
| 3 | `GET /people/{id}/contacts` | List a person's direct contacts |
| 4 | `GET /path?from=1&to=5` | Shortest path between two people, max 3 hops |

All data lives in memory (no database). Restarting the server clears everything.

**What the reviewers check:** correctness, code structure and naming, bad-input handling, tests, commit history. They run the project from the README on a clean machine using a fixed set of API calls. The interview starts from the submission, and you will make **one live change** on a screen-share.

---

## 2. Core idea: it is a graph problem

- **Node** = a person
- **Edge** = a mutual (undirected) connection
- **Direct contacts** = a node's neighbours
- **Shortest path with a hop limit** = BFS with a depth limit of 3

Example graph:

```
Priya(4) - Rohan(2) - Asha(1) - Meera(3) - Kabir(5)
```

Stored as an adjacency list:

```
1 -> {2, 3}
2 -> {1, 4}
3 -> {1, 5}
4 -> {2}
5 -> {3}
```

- Asha(1) to Kabir(5): 1 -> 3 -> 5, **2 hops**, connected.
- Priya(4) to Kabir(5): 4 -> 2 -> 1 -> 3 -> 5, **4 hops**, over the limit, so `{"connected": false}`.

---

## 3. Technical decisions

| Decision | Choice | Why |
|---|---|---|
| Language | Java 17 | Required |
| Build tool | Maven | Common, simple, easy for a reviewer to run |
| HTTP server | JDK built-in `com.sun.net.httpserver.HttpServer` | Zero extra dependencies, so less can break on a clean machine |
| JSON | Gson | Allowed by the assignment |
| Tests | JUnit 5 | Standard; the required path tests need it |
| Storage | `HashMap` and `TreeSet` in memory | Required |
| Port | `8080` by default, overridable with the `PORT` env var | Must be stated in the README |

Optional robustness tip: add the **Maven Wrapper** (`mvn -N wrapper:wrapper`) and commit `mvnw`, `mvnw.cmd` and `.mvn/`. A reviewer then needs only Java installed, not Maven.

---

## 4. API contract

### `POST /people`
- Request: `{"name": "Asha"}`
- Success: **201** `{"id": 1, "name": "Asha"}`
- IDs are assigned by the server: 1, 2, 3, ...

### `POST /knows`
- Request: `{"a": 1, "b": 2}`
- Success: **204**, empty body
- Mutual: store both directions (1 knows 2 and 2 knows 1)
- Sending the same connection again (or `{"a":2,"b":1}`) is not an error: still **204**

### `GET /people/{id}/contacts`
- Success: **200** `[{"id": 2, "name": "Rohan"}]`
- No contacts: **200** `[]`
- Only that person's neighbours, as `{id, name}` objects, not the whole graph and not bare IDs

### `GET /path?from=1&to=5`
- Found within 3 hops: **200**
  ```json
  {"connected": true, "hops": 2, "path": [
    {"id": 1, "name": "Asha"},
    {"id": 3, "name": "Meera"},
    {"id": 5, "name": "Kabir"}
  ]}
  ```
- Not found within 3 hops: **200** `{"connected": false}` (this is a normal answer, not an error)
- `from == to` and the person exists: **200** `{"connected": true, "hops": 0, "path": [<that person>]}`
- Exactly 3 hops counts as connected. Only 4 or more is not connected.

---

## 5. Validation rules (complete)

Error body format everywhere: `{"error": "<message>"}`

### Status codes cheat sheet

| Code | Meaning | Used for |
|---|---|---|
| 200 | OK | Successful GETs, including `connected: false` |
| 201 | Created | `POST /people` |
| 204 | No Content | `POST /knows` (success, nothing to return) |
| 400 | Bad Request | The client sent invalid input |
| 404 | Not Found | Unknown person, unknown route |
| 405 | Method Not Allowed | Wrong HTTP method (extra, optional) |
| 500 | Internal Server Error | Unexpected bug (should never happen on normal input) |

### `POST /people`

| Situation | Example | Response |
|---|---|---|
| Valid | `{"name":"Asha"}` | 201 |
| Invalid JSON | `{name: Asha` | 400 |
| Empty body | (nothing) | 400 |
| Body is not a JSON object | `"hello"` or `[1,2]` | 400 |
| `name` missing | `{}` or `{"naam":"x"}` | 400 |
| `name` empty | `{"name":""}` | 400 |
| `name` blank | `{"name":"   "}` | 400 |
| `name` not a string | `{"name":123}` or `{"name":null}` | 400 (recommended) |
| Hindi name | `{"name":"आशा"}` | 201, returned exactly as sent |

Rules:
- Check blank with `name.trim().isEmpty()`, but **store and return the original string**, not the trimmed one ("exactly as supplied").

### `POST /knows`

| Situation | Example | Response |
|---|---|---|
| Valid | `{"a":1,"b":2}` | 204 |
| Duplicate | same again, or reversed | 204 |
| Invalid JSON / empty body | broken | 400 |
| `a` or `b` missing | `{"a":1}` | 400 |
| `a` or `b` not an integer | `{"a":"x","b":2}` or `{"a":1.5,"b":2}` | 400 (recommended) |
| `a == b` | `{"a":1,"b":1}` | 400 |
| A person does not exist | `{"a":1,"b":99}` | 404 `{"error":"no person 99"}` |

Order of checks: **400 checks first** (JSON, missing, type, `a == b`), **then 404** (existence).

### `GET /people/{id}/contacts`

| Situation | Example | Response |
|---|---|---|
| Has contacts | `/people/1/contacts` | 200 with a list |
| No contacts | a person with no connections | 200 `[]` |
| Unknown ID | `/people/9/contacts` | 404 `{"error":"no person 9"}` |
| ID not a number | `/people/abc/contacts` | 400 |

`[]` versus 404: a person who exists but has no friends gets `[]`. A person who does not exist gets 404.

### `GET /path?from=..&to=..`

| Situation | Example | Response |
|---|---|---|
| `from` or `to` missing | `/path?from=1` | 400 |
| Malformed | `/path?from=abc&to=2` | 400 |
| Unknown person | `/path?from=1&to=99` | 404 `{"error":"no person 99"}` |
| Same person | `/path?from=1&to=1` | 200, hops 0, one-person path |
| Within 3 hops | | 200, `connected: true` |
| Beyond 3 hops or no route | | 200 `{"connected": false}` |

Order of checks: **400** (missing or malformed), then **404** (existence), then **BFS**.

### Extras (safe-side, not required)
- Unknown URL, for example `/xyz`: 404 with an error object
- Wrong method, for example `GET /knows`: 405
- Catch-all for unexpected exceptions: 500 with an error object, and the server must not crash

---

## 6. Project structure

```
nada-people-api/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/java/com/nada/people/
    │   ├── Main.java                      starts the server
    │   ├── model/
    │   │   ├── Person.java                record Person(int id, String name)
    │   │   └── PathResult.java            connected / hops / path
    │   ├── service/
    │   │   └── PeopleService.java         all business logic + BFS
    │   ├── exception/
    │   │   ├── BadRequestException.java   -> 400
    │   │   └── NotFoundException.java     -> 404
    │   └── http/
    │       ├── ApiServer.java             creates server, registers routes
    │       ├── PeopleHandler.java         /people and /people/{id}/contacts
    │       ├── KnowsHandler.java          /knows
    │       ├── PathHandler.java           /path
    │       └── HttpUtil.java              read body, send JSON, parse query, error mapping
    └── test/java/com/nada/people/
        ├── service/PeopleServiceTest.java
        └── service/PathFindingTest.java   the required tests
```

Principle: **the service layer knows nothing about HTTP**, and **the HTTP layer contains no business logic**. This makes the service easy to unit test and makes live changes in the interview easier.

---

## 7. Design details

### 7.1 Classes and responsibilities

```java
public record Person(int id, String name) {}

public record PathResult(boolean connected, Integer hops, List<Person> path) {
    // static factories: notConnected() and of(List<Person> path)
}

public class PeopleService {
    public synchronized Person addPerson(String name)          // validates name, assigns ID
    public synchronized void connect(int a, int b)             // validates, adds both directions
    public synchronized List<Person> getContacts(int id)       // 404 if unknown
    public synchronized PathResult findPath(int from, int to)  // 404 if unknown, BFS depth <= 3
}
```

Division of validation:
- **HTTP layer** validates the *shape* of the request: JSON syntax, field present, correct type, query params parse as integers.
- **Service layer** validates *business rules*: name not blank, `a != b`, person exists.

### 7.2 Data structures

```java
Map<Integer, Person> people = new HashMap<>();
Map<Integer, Set<Integer>> adjacency = new HashMap<>();   // inner Set: TreeSet<Integer>
int nextId = 1;
```

- Use a **Set** (not a List) so duplicate connections do nothing.
- Use **TreeSet** so contacts and BFS order are deterministic (sorted by ID). Mention this in the README.
- `synchronized` on the service methods protects the check-then-act sequences (ID assignment, existence checks) from concurrent requests.

### 7.3 Exceptions

```java
class BadRequestException extends RuntimeException { ... }   // -> 400
class NotFoundException   extends RuntimeException { ... }   // -> 404
```

Throw them from the service and handlers. **One central place** in the HTTP layer catches them and converts them to a status code plus `{"error": "..."}`. This avoids repeated try/catch code in every handler.

### 7.4 JSON gotchas with Gson

1. **Gson is lenient.** It accepts some malformed input. After parsing, always check that the result is a JSON object (`isJsonObject()`), otherwise return 400.
2. **Do not map the request body straight to a POJO.** Gson silently converts `{"name": 123}` into the string `"123"`, and `{"a": 1.5}` into `1`. Instead, parse into a `JsonObject` and check each field yourself (present, not null, correct primitive type).
3. **Disable HTML escaping**: `new GsonBuilder().disableHtmlEscaping().create()`. Otherwise a name like `Tom & Jerry` is returned as `Tom \u0026 Jerry`, which breaks "returned exactly as supplied".
4. **Null fields are omitted** by Gson by default, which is what you want for `{"connected": false}` (no `hops`, no `path`).
5. Always catch `JsonSyntaxException` (and `JsonParseException`) and map them to 400.

### 7.5 HTTP gotchas with the JDK HttpServer

1. **UTF-8 everywhere.** Read the request body with `new String(body.readAllBytes(), StandardCharsets.UTF_8)`. Write the response with `getBytes(StandardCharsets.UTF_8)`. Set `Content-Type: application/json; charset=utf-8`.
2. **204 has no body.** Use `exchange.sendResponseHeaders(204, -1)`. For responses with a body, use the byte length: `sendResponseHeaders(status, bytes.length)`.
3. **Contexts match by prefix.** `createContext("/people", ...)` also matches `/peoplexyz`. Parse the path segments yourself inside the handler and return 404 for anything that is not exactly `/people` or `/people/{id}/contacts`.
4. **Always close the exchange** (`exchange.close()` in a `finally`).
5. **Query parsing:** use `exchange.getRequestURI().getRawQuery()`, split on `&` and `=`. Missing key or `NumberFormatException` means 400. Very large numbers also throw `NumberFormatException`, which is fine (400).
6. **Method check** in each handler: wrong method returns 405.

---

## 8. BFS algorithm

Why BFS: it explores level by level (1 hop, 2 hops, 3 hops), so the first time it reaches the target, that is the shortest path. DFS does not guarantee this.

```
findPath(from, to):
    validate from and to exist            -> NotFoundException("no person X")
    if from == to:
        return connected, hops 0, path [person(from)]

    queue   = [(from, depth 0)]
    visited = {from}
    parent  = {}                           // child -> who we came from

    while queue not empty:
        (node, depth) = queue.poll()
        if depth == 3: continue            // do not expand beyond 3 hops
        for neighbour in adjacency[node]:
            if neighbour in visited: continue
            visited.add(neighbour)
            parent[neighbour] = node
            if neighbour == to:
                path = walk back through parent from `to` to `from`, reverse it
                return connected, hops = path.size() - 1, path
            queue.add((neighbour, depth + 1))

    return notConnected
```

Key points:
- **Queue** gives level-by-level order, which guarantees the shortest path.
- **Visited** prevents infinite loops (1 -> 2 -> 1 -> 2 ...).
- **Parent map** lets you rebuild the path at the end.
- **Depth limit:** a node at depth 3 is processed but its neighbours are not queued. So a target at exactly 3 hops is found (it is discovered while processing a depth-2 node), and a target at 4 hops is never reached.

Worked trace, Asha(1) to Kabir(5):

| Step | Pop | New neighbours | Queue after | Parent updates |
|---|---|---|---|---|
| Start | | | [1] | |
| 1 | 1 | 2, 3 | [2, 3] | 2<-1, 3<-1 |
| 2 | 2 | 4 (1 already visited) | [3, 4] | 4<-2 |
| 3 | 3 | 5 (1 visited), 5 is the target | stop | 5<-3 |

Walk back: 5 <- 3 <- 1, reversed: 1 -> 3 -> 5. Hops = 2.

---

## 9. Execution plan, step by step

### Time plan

| When | What |
|---|---|
| Tonight | Phase 0, 1, 2 (setup, service, BFS, tests) |
| Saturday morning | Phase 3, 4, 5 (HTTP layer, endpoints, manual testing) |
| Saturday afternoon | Phase 6, 7 (README, clean-clone check, submit) |
| By ~6 PM | Submitted, with a buffer before the 9 PM deadline |

Commit after every small, meaningful step. The reviewers read the commit history, so avoid one giant commit at the end. Write messages in the imperative: "Add ...", "Fix ...".

### Phase 0: Setup (~20 min)

Tasks:
1. Check versions: `java -version` (17 or higher), `mvn -version`.
2. Create the repo: `mkdir nada-people-api && cd nada-people-api && git init`.
3. Create the folder structure from section 6.
4. Add `.gitignore`: `target/`, `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`.
5. Add `pom.xml` (see Appendix A).
6. Verify: `mvn compile` succeeds.

Done when: `mvn compile` prints `BUILD SUCCESS`.
Commit: `Initialize Maven project with Gson and JUnit 5`

### Phase 1: Model, exceptions, basic service (~45 min)

Tasks:
1. `Person` record.
2. `BadRequestException`, `NotFoundException`.
3. `PeopleService` with `addPerson`, `connect`, `getContacts`.
   - `addPerson`: reject null or blank name (400), assign `nextId++`, store the original name, create an empty neighbour set.
   - `connect`: reject `a == b` (400), reject unknown IDs (404), add each to the other's set.
   - `getContacts`: unknown ID (404), map neighbour IDs to `Person` objects.

Done when: you can call these methods from a scratch `main` or a quick test and see correct results.
Commits:
- `Add Person model and custom exceptions`
- `Add PeopleService with addPerson, connect and getContacts`

### Phase 2: BFS and the required tests (~1 hr)

Tasks:
1. `PathResult` record with factories.
2. `findPath` with BFS and the depth limit (section 8).
3. `PathFindingTest` (see section 10).
4. Run `mvn test` until everything passes.

Done when: both required chain tests pass, plus the extra tests.
Commits:
- `Add PathResult model`
- `Implement bounded BFS shortest path`
- `Add path-finding tests covering the 3-hop limit`
- `Add PeopleService unit tests for validation and contacts`

### Phase 3: HTTP infrastructure (~45 min)

Tasks:
1. `HttpUtil` with: read body as UTF-8 string, send JSON response with status, send empty response (204), parse query string, and a single method that turns `BadRequestException`, `NotFoundException` and unexpected exceptions into 400, 404 and 500 with `{"error": ...}`.
2. A shared `Gson` instance (HTML escaping disabled).
3. `ApiServer`: creates the `HttpServer`, registers contexts `/people`, `/knows`, `/path`, plus a catch-all `/` that returns 404.
4. `Main`: reads `PORT` (default 8080), starts the server, prints the URL.

Done when: the server starts and `curl localhost:8080/xyz` returns a 404 error object.
Commits:
- `Add HTTP utilities for JSON, errors and query parsing`
- `Add ApiServer and Main entry point`

### Phase 4: Endpoints, one at a time (~1.5 hr)

Do them in this order and commit after each:

1. **`POST /people`** (`PeopleHandler`): parse body into `JsonObject`, validate `name` (present, string, not null), call the service, return 201 with `{id, name}`.
   Commit: `Implement POST /people`
2. **`GET /people/{id}/contacts`** (same handler): parse path segments, parse the ID (400 if not a number), call the service, return 200 with the list.
   Commit: `Implement GET /people/{id}/contacts`
3. **`POST /knows`** (`KnowsHandler`): parse and validate `a` and `b` as integers, call the service, return 204.
   Commit: `Implement POST /knows`
4. **`GET /path`** (`PathHandler`): parse `from` and `to`, call the service, return 200 with the `PathResult`.
   Commit: `Implement GET /path`
5. **Method checks and routing edge cases:** 405 for wrong methods, 404 for unknown sub-paths.
   Commit: `Handle unsupported methods and unknown routes`

Done when: every row in the section 5 validation tables behaves as specified.

### Phase 5: Manual end-to-end testing (~45 min)

1. Start the server: `mvn compile exec:java`.
2. Run the happy path (curl cheat sheet below): create 5 people, connect them, check contacts, check paths.
3. Run every row of the validation tables and note the status code.
4. Test Hindi names (use a JSON file with `--data-binary @file`, or Postman/Bruno, because some Windows terminals garble non-ASCII input).
5. Test the 3-hop boundary through the API as well, not only in unit tests.
6. Fix bugs; commit each fix separately, for example `Fix 400 for non-integer path parameters`.

### Phase 6: README (~30 min)

See section 12 for the outline. Write it **after** everything works, so that every command in it is something you have actually run.
Commit: `Add README with run, test, next steps and AI disclosure`

### Phase 7: Final verification and submission (~30 min)

1. Clone the repo into a **fresh folder**: `git clone <url> /tmp/verify && cd /tmp/verify`.
2. Follow the README exactly, copy-pasting commands. Run the server and tests.
3. Run the section 13 checklist.
4. If the repo is private, invite **harshtitoria-dev** and **jordandivyansh** as collaborators.
5. Reply to the assignment email with the repo link, **before 9 PM IST on Sat 10 Oct**.

---

## 10. Testing plan

### How tests work (JUnit 5 in one minute)

Every test has three parts: **setup, action, assert**.

```java
@Test
void threeHopsIsConnected() {
    PeopleService service = new PeopleService();            // setup
    Person a = service.addPerson("A");
    Person b = service.addPerson("B");
    Person c = service.addPerson("C");
    Person d = service.addPerson("D");
    service.connect(a.id(), b.id());
    service.connect(b.id(), c.id());
    service.connect(c.id(), d.id());

    PathResult result = service.findPath(a.id(), d.id());   // action

    assertTrue(result.connected());                         // assert
    assertEquals(3, result.hops());
}
```

Useful assertions: `assertEquals(expected, actual)`, `assertTrue`, `assertFalse`, `assertThrows(BadRequestException.class, () -> ...)`.

### Required tests (from the assignment)

| Test | Setup | Expected |
|---|---|---|
| Three hops | chain A-B-C-D, A to D | connected, hops = 3 |
| Four hops | chain A-B-C-D-E, A to E | not connected |

### Recommended extra tests

| Test | Expected |
|---|---|
| Same person | connected, hops 0, path has only that person |
| Two people with no connection | not connected |
| Shortcut exists (A-B-C-D plus A-D) | hops = 1, not 3 |
| Path contents | names and IDs in the right order |
| Unknown person in `findPath` | `NotFoundException` |
| Blank name | `BadRequestException` |
| Duplicate `connect` | no error, contacts show the person only once |
| `connect(a, a)` | `BadRequestException` |
| Hindi name round trip | `addPerson("आशा")` returns the exact same string |
| Contacts of a person with no connections | empty list |

### Optional (if time remains): HTTP-level test

Start the server on a random port inside a test and call it with `java.net.http.HttpClient` to check status codes. Good, but lower priority than the unit tests.

### Running tests

```bash
mvn test
```

---

## 11. Manual testing: curl cheat sheet

Happy path:

```bash
curl -i -X POST localhost:8080/people -H "Content-Type: application/json" -d '{"name":"Asha"}'
curl -i -X POST localhost:8080/people -H "Content-Type: application/json" -d '{"name":"Rohan"}'
curl -i -X POST localhost:8080/knows  -H "Content-Type: application/json" -d '{"a":1,"b":2}'
curl -i localhost:8080/people/1/contacts
curl -i "localhost:8080/path?from=1&to=2"
```

Validation checks:

```bash
curl -i -X POST localhost:8080/people -d '{bad json'              # 400
curl -i -X POST localhost:8080/people -d '{"name":"   "}'         # 400
curl -i -X POST localhost:8080/people -d '{}'                     # 400
curl -i -X POST localhost:8080/knows  -d '{"a":1,"b":1}'          # 400
curl -i -X POST localhost:8080/knows  -d '{"a":1,"b":99}'         # 404
curl -i localhost:8080/people/99/contacts                         # 404
curl -i localhost:8080/people/abc/contacts                        # 400
curl -i "localhost:8080/path?from=1"                              # 400
curl -i "localhost:8080/path?from=1&to=99"                        # 404
curl -i "localhost:8080/path?from=1&to=1"                         # 200, hops 0
```

Hindi test (avoids terminal encoding issues):

```bash
# body.json contains: {"name":"आशा"}  (saved as UTF-8)
curl -i -X POST localhost:8080/people -H "Content-Type: application/json" --data-binary @body.json
```

---

## 12. README outline

The assignment requires these four sections:

1. **How to run the server**: prerequisites (Java 17+, Maven, or `./mvnw` if you added the wrapper), the exact command (`mvn compile exec:java`), the **port** (8080, overridable with `PORT`).
2. **How to run the tests**: `mvn test`.
3. **What I would do next with more time**, for example:
   - Persistence (database) instead of in-memory storage
   - Finer-grained concurrency instead of `synchronized` on the whole service
   - HTTP-level integration tests
   - Configurable hop limit (`maxHops` query param)
   - Pagination for contacts
   - Input size limits and request logging
   - Bidirectional BFS for very large graphs
   - Dockerfile and an OpenAPI spec
   - `DELETE` endpoints and removing connections
4. **AI tools used**: be honest and specific. For example: "Used Claude to understand the problem statement, discuss the approach and review my work. Code was written and tested by me." Only write what is true.

Also helpful:
- Short API summary with example requests and responses
- Design notes: layering, why BFS, why TreeSet (deterministic order), validation order (400 before 404)
- Project structure

---

## 13. Pre-submission checklist

- [ ] `mvn test` passes from a fresh clone
- [ ] Server starts using only the README instructions, on the documented port
- [ ] All 4 endpoints return the correct success codes (201, 204, 200, 200)
- [ ] Every validation row in section 5 returns the right status and an `{"error": ...}` body where applicable
- [ ] 3-hop boundary: A to D connected, A to E not connected (unit tests **and** manually through the API)
- [ ] `from == to` returns hops 0 with a single-person path
- [ ] Duplicate `/knows` returns 204
- [ ] Hindi names come back exactly as sent
- [ ] `{"connected": false}` has no extra fields
- [ ] No leftover debug prints, commented-out code or unused files
- [ ] `.gitignore` excludes `target/` and IDE files
- [ ] Commit history is granular with clear messages
- [ ] README has all four required sections, and AI usage is disclosed honestly
- [ ] If the repo is private: **harshtitoria-dev** and **jordandivyansh** invited
- [ ] Repo link sent by **reply to the assignment email**, before 9 PM IST on Sat 10 Oct

---

## 14. Interview preparation (live change on screen-share)

You will be asked to change something live, so you must understand **every line** you submit.

Prepare by being able to explain, without looking:
- Why BFS and not DFS
- Where the depth limit is enforced, and why exactly 3 hops is still "connected"
- Why `Set` for adjacency, and why `TreeSet`
- How validation flows: HTTP layer for shape, service for rules, one central error mapper
- Why 400 is checked before 404
- Why `connected: false` is a 200

Likely live changes, and where you would make them:

| Possible change | Where |
|---|---|
| Change the hop limit (for example to 4, or make it a query param) | `PeopleService.findPath` (constant or parameter) plus `PathHandler` |
| Add `GET /people` (list everyone) | `PeopleService` plus `PeopleHandler` |
| Add `DELETE /knows` | `PeopleService.disconnect` plus `KnowsHandler` |
| Search people by name | `PeopleService` plus a query param in `PeopleHandler` |
| Return all shortest paths instead of one | Modify BFS to keep multiple parents |
| Add a new validation rule | Service layer plus a test |

Practice making one of these changes yourself, including its test, before the interview.

---

## 15. Common pitfalls

1. Storing connections in only one direction (the connection must be mutual).
2. Using `List` for neighbours, which allows duplicates.
3. Treating exactly 3 hops as "too far" (off-by-one).
4. Forgetting to mark nodes visited, which causes infinite loops.
5. Returning 200 with an empty body for `POST /knows` instead of 204.
6. Trimming the name before storing it, which breaks "exactly as supplied".
7. Gson escaping `&`, `<`, `>` and `'`, which alters names.
8. Gson silently coercing `{"name": 123}` to `"123"`.
9. Missing `charset=utf-8`, which garbles Hindi names.
10. Returning 404 for `connected: false` (it is a 200).
11. Checking existence (404) before checking input shape (400).
12. Writing the README commands without testing them on a fresh clone.
13. One giant commit instead of a readable history.

---

## Appendix A: `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.nada</groupId>
  <artifactId>people-api</artifactId>
  <version>1.0.0</version>

  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <dependency>
      <groupId>com.google.code.gson</groupId>
      <artifactId>gson</artifactId>
      <version>2.10.1</version>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.10.2</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.2.5</version>
      </plugin>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.2.0</version>
        <configuration>
          <mainClass>com.nada.people.Main</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

Run the server: `mvn compile exec:java`
Run the tests: `mvn test`
