# LifeMap

> Mobile Computing project — Group 13, Reutlingen University (Bachelor MKI, SoSe 2026).

**LifeMap** is a native **Android** application that helps people discover and join local social
events and connect with others who share their interests. Users create a profile, browse and
create events shown on an interactive map, and build a friends network through friend requests.
Everything is stored and synchronized in real time through a **Firebase** backend.

---

## Table of contents

- [Concept](#concept)
- [Tech stack](#tech-stack)
- [Features](#features)
- [Architecture](#architecture)
  - [Layers](#layers)
  - [Component diagram](#component-diagram)
  - [Reactive data flow (MVVM)](#reactive-data-flow-mvvm)
  - [Navigation](#navigation)
- [Data model (Cloud Firestore)](#data-model-cloud-firestore)
- [Security rules](#security-rules)
- [Offline support](#offline-support)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
- [Notable engineering decisions](#notable-engineering-decisions)
- [Documentation](#documentation)
- [Team](#team)

---

## Concept

Meeting new people and finding things to do locally is hard, especially for students and newcomers
to a city. Existing social networks revolve around people you already know, and event platforms
rarely connect you with others who share your interests. LifeMap combines a **map of nearby social
events** with a lightweight **social layer** (profiles, shared interests and friendships), so users
can both discover activities around them and connect with like-minded people.

---

## Tech stack

| Area | Technology |
|------|-----------|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (Compose BOM 2026.02.01), Material 3 |
| Architecture | MVVM + Repository, unidirectional data flow |
| Async / state | Kotlin Coroutines + Flow / StateFlow |
| Navigation | Jetpack Navigation Compose 2.9.7 |
| Backend | Firebase Authentication, Cloud Firestore, Firebase Storage (BoM 34.13.0) |
| Maps | osmdroid 6.1.20 (OpenStreetMap — no API key) |
| Images | Coil 2.6.0 |
| Build | Gradle (version catalog), AGP 9.1.1 · minSdk 26 · target/compileSdk 36 |

---

## Features

### Authentication (`ui/auth`, `AuthRepository`)
- Register with email + password (creates the Auth account **and** the Firestore profile).
- Log in / log out.
- **Forgot password**: sends a Firebase password‑reset email.
- **Delete account**: re‑authenticates with the password, then deletes the user's friendship
  documents, their Firestore profile and finally the Auth account.

### Profile (`ProfileScreen`, `ProfileViewModel`)
- View and edit name, description, location, Instagram, phone, **hobbies** and **languages**.
- Set a **profile photo** from gallery or camera (uploaded to Firebase Storage).
- Edits survive navigation (`rememberSaveable`) and are persisted to Firestore with success/error
  feedback.
- **Connectivity‑aware**: editing/saving is disabled while offline.

### Friends (`FriendsScreen`, `FriendDetailScreen`, `FriendshipRepository`)
- Suggested users, with **interests in common** highlighted.
- Send a friend request → recipient sees it under **Friend Requests**.
- Accept / decline incoming requests, cancel **sent** requests, remove existing friends.
- Friend detail screen with the full profile and the correct relationship action.

### Events (`EventsScreen`, `EventDetailScreen`, `NewScreen`, `EventRepository`)
- Browse events partitioned into **nearby / joined / recently joined / owned**.
- Filter by category, age range, gender preference, time and group size.
- **Create** an event (name, description, date/time, location, category, image…).
- Open an event's detail screen, **edit** it (owner) or **join / leave** it (atomic update of
  `participantIds` + `participantsCount`).

### Map (`MapScreen`, `MapViewModel`, `LocationPickerScreen`)
- Interactive OpenStreetMap view that places **markers for events** (the same data as the Events
  tab). Private events are only shown to their owner and participants.
- Location picker used when creating an event.

---

## Architecture

LifeMap follows a **layered, MVVM** architecture with the **Repository pattern** isolating Firebase.

### Layers

```
┌─────────────────────────────────────────────────────────────┐
│  UI layer — Jetpack Compose screens                          │
│  LoginScreen, EventsScreen, MapScreen, FriendsScreen,        │
│  ProfileScreen, EventDetailScreen, FriendDetailScreen …      │
└───────────────▲───────────────────────────┬─────────────────┘
                │ observes StateFlow         │ user actions (fun calls)
┌───────────────┴───────────────────────────▼─────────────────┐
│  Presentation layer — ViewModels                             │
│  AuthVM, ProfileVM, EventsVM, MapVM, NewEventVM,             │
│  EventDetailVM, FriendsVM, FriendDetailVM                    │
│  → expose immutable UI state as sealed classes / StateFlow   │
└───────────────▲───────────────────────────┬─────────────────┘
                │ Flow<...> / Result<...>    │ suspend calls
┌───────────────┴───────────────────────────▼─────────────────┐
│  Data layer — Repositories (single source of truth)          │
│  AuthRepository, UserRepository, EventRepository,            │
│  FriendshipRepository, StorageRepository                     │
└───────────────▲───────────────────────────┬─────────────────┘
                │ data classes               │ SDK calls
┌───────────────┴───────────────────────────▼─────────────────┐
│  Model layer            │   Firebase backend                 │
│  User, Event, Friendship│   Auth · Cloud Firestore · Storage │
└─────────────────────────┴────────────────────────────────────┘
```

Each layer depends only on the one below it. The UI never touches Firebase directly — it only
renders the state the ViewModels expose and forwards user intents back to them.

### Component diagram

```
                       ┌────────────────────┐
                       │   MainActivity      │
                       │   Root() gate       │
                       └─────────┬──────────┘
              AuthState.Unauth   │   AuthState.Authenticated
                  ┌──────────────┴───────────────┐
                  ▼                               ▼
          ┌───────────────┐              ┌──────────────────┐
          │  AuthNavHost  │              │   App() NavHost   │
          │ login/register│              │ Events·Map·New·   │
          │ forgot_pass   │              │ Friends·Profile   │
          └──────┬────────┘              │ + friends/{uid}   │
                 │                       └─────────┬─────────┘
                 ▼                                 ▼
          ┌──────────────┐              ┌────────────────────┐
          │ AuthViewModel│              │ Feature ViewModels  │
          └──────┬───────┘              └─────────┬──────────┘
                 ▼                                 ▼
        ┌─────────────────────────────────────────────────────┐
        │  Repositories: Auth · User · Event · Friendship ·     │
        │                Storage                                │
        └───────────────────────┬──────────────────────────────┘
                                 ▼
        ┌─────────────────────────────────────────────────────┐
        │  Firebase:  Authentication · Cloud Firestore ·        │
        │             Storage                                   │
        └─────────────────────────────────────────────────────┘
```

### Reactive data flow (MVVM)

- ViewModels expose state as **`StateFlow`**; screens collect it with
  `collectAsStateWithLifecycle()`. State is modelled with **sealed classes**
  (`Loading | Loaded | Error`, e.g. `FriendsState`, `EventsState`, `FriendDetailState`), so the UI
  must handle every case.
- Repositories expose real‑time reads as **`Flow`** built with `callbackFlow` wrapping Firestore's
  `addSnapshotListener` (and cleaning up via `awaitClose`). Each `toObject()` is guarded so a single
  malformed document cannot crash the listener.
- Writes are `suspend` functions returning Kotlin **`Result<T>`**, so callers handle success/failure
  explicitly (instead of callbacks).
- ViewModels combine sources: `combine()` (e.g. `FriendsViewModel` merges users + friendships into
  *my friends / incoming / sent / suggested*; `EventsViewModel` merges events + filters + recently
  joined), and `flatMapLatest()` (e.g. `ProfileViewModel` switches to the current user's document
  when auth state changes). Flows are shared with
  `stateIn(SharingStarted.WhileSubscribed(5000))` so listeners are active only while a screen is on
  screen.

### Navigation

```
Root()
 ├─ AuthState.Loading         → loading spinner
 ├─ AuthState.Unauthenticated → AuthNavHost
 │     ├─ login
 │     ├─ register
 │     └─ forgot_password
 └─ AuthState.Authenticated   → App()  (Scaffold + bottom TabRow)
       ├─ events
       ├─ map
       ├─ new
       ├─ friends
       │    └─ friends/{uid}   (friend detail)
       └─ profile
```

---

## Data model (Cloud Firestore)

Three top‑level collections. Every model field has a default value (so Firestore can deserialize by
reflection) and the document id is exposed via `@DocumentId`.

### `users/{uid}` — document id = Firebase Auth uid
| Field | Type | Notes |
|-------|------|-------|
| `uid` | String `@DocumentId` | = Auth uid |
| `displayName`, `displayNameLower` | String | name + lowercase copy for search |
| `email`, `description` | String | |
| `birthday` | Timestamp? | used to compute age |
| `sex` | String | Male / Female / Other / "" |
| `photoUrl` | String? | Firebase Storage URL |
| `languages`, `hobbies` | List\<String\> | interests |
| `location`, `locationLat`, `locationLng` | String / Double? | |
| `phone`, `instagram` | String | |
| `createdAt`, `updatedAt` | Timestamp | server timestamps |

### `events/{id}`
| Field | Type | Notes |
|-------|------|-------|
| `id` | String `@DocumentId` | |
| `name`, `description` | String | |
| `ownerId` | String | creator uid |
| `dateTime` | Timestamp? | |
| `locationText`, `locationLat`, `locationLng`, `geohash` | String / Double | map marker |
| `isPublic` | Boolean | invite‑only events hidden on the map |
| `limitPeople` | Int | 0 = no limit |
| `participantIds` | List\<String\> | uids that joined |
| `tags` | List\<String\> | |
| `category`, `ageRange`, `genderPref` | String | filter chips |
| `createdAt`, `updatedAt` | Timestamp | |

### `friendships/{id}` — id = `"{smallerUid}_{largerUid}"` (`Friendship.buildId`)
| Field | Type | Notes |
|-------|------|-------|
| `id` | String `@DocumentId` | deterministic, one doc per pair |
| `userIds` | List\<String\> | the two members, sorted |
| `status` | String | `pending` / `accepted` / `blocked` |
| `requestedBy` | String | who sent the request |
| `blockedBy` | String? | |
| `createdAt`, `acceptedAt` | Timestamp? | |

---

## Security rules

Firestore access is restricted (see the project's `firestore.rules` in the Firebase console):

- **users** — readable by any authenticated user; writable only by the owner (`uid == userId`).
- **events** — readable by anyone signed in; **create** requires `ownerId == auth.uid`; **update**
  is owner‑only **except** non‑owners may change only `participantIds` / `participantsCount`
  (join/leave); **delete** is owner‑only.
- **friendships** — readable/writable only by the two members (`auth.uid in userIds`); a request
  can only be **created** by the user listed in `requestedBy`.
- Everything else is denied by default.

---

## Offline support

`LifeMapApplication` initializes Firebase on startup and enables **Firestore persistent (disk)
cache**. Reads are served from cache first and writes are queued locally, syncing automatically when
connectivity returns. The Profile screen additionally observes connectivity and disables editing
while offline.

---

## Project structure

```
app/src/main/java/com/example/livemap/
├─ MainActivity.kt              # Root() auth gate + bottom-tab NavHost
├─ LifeMapApplication.kt        # Firebase init + Firestore offline cache
│
├─ data/
│  ├─ model/                    # User, Event, Friendship  (Firestore documents)
│  └─ repository/               # Auth, User, Event, Friendship, Storage + AuthState/FormState
│
├─ ui/
│  ├─ auth/                     # AuthNavHost, AuthViewModel, Login/Register screens
│  ├─ events/                   # Events/Map/NewEvent/EventDetail ViewModels + state
│  ├─ friends/                  # Friends/FriendDetail ViewModels + state
│  ├─ profile/                  # ProfileViewModel
│  └─ theme/                    # Compose theme
│
├─ composables/                 # Reusable UI (search bar, date/time picker, fields…)
├─ aux_files/                   # configs, geocoding, savers, uri helpers
│
├─ *Screen.kt                   # Top-level Compose screens (Events, Map, Friends, Profile, …)
└─ ...

docs/                           # Project documentation (see below)
```

---

## Getting started

### Prerequisites
- Android Studio (latest), JDK 11+.
- An Android device/emulator with **API 26+** (Android 8.0).
- A **Firebase project** with Authentication (Email/Password), Cloud Firestore and Storage enabled.

### Setup
1. Clone the repo and open it in Android Studio.
2. Add your Firebase config file at **`app/google-services.json`**
   (download it from the Firebase console — it is required to build).
3. In the Firebase console: enable **Email/Password** auth, create the **Firestore** database and
   publish the security rules described above, and enable **Storage**.
4. Build & run:
   ```bash
   ./gradlew installDebug      # or run from Android Studio
   ```

### Permissions used
`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `CAMERA`.

---

## Notable engineering decisions

- **`@DocumentId` on `User.uid` / `Event.id` / `Friendship.id`** — the uid is the document id and is
  *not* stored as a field; without `@DocumentId` every user deserialized with an empty id, which
  crashed the Friends list (duplicate `LazyColumn` key) and broke profile saving.
- **Deterministic friendship id** (`buildId`) guarantees a single document per pair of users.
- **`Result`‑based repositories + UI feedback** — write failures surface as Toasts/messages instead
  of failing silently.
- **Sensitive operations require re‑authentication** (account deletion) and respect the security
  rules (e.g. friendship cleanup runs while still authenticated).
- **Guarded deserialization** — `toObject()` is wrapped so one malformed document never tears down a
  real‑time listener.

---

## Documentation

Full project documentation and templates live in [`docs/`](docs/):
- `LifeMap_Project_Documentation.docx` — the formal project report (architecture, requirements,
  results). Generated/regenerated with `python docs/generate_documentation.py`.
- `template.md` / `documentation-example.md` — course format references.

---

## Team

Group 13 — Mobile Computing, SoSe 2026, Reutlingen University.
<!-- TODO: list team members (name + student number). -->
