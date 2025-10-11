# 📱 Linky - Link Saver

## 1️⃣ Project Goal
Create an **offline-first Android app** that allows users to **save, organize, and manage web links** with snapshots for future reference.  
Users can:
- Save links with custom **title**, **notes**, and **preview image**.
- Store **snapshots** (reader mode, PDF, or full-page screenshot) locally.
- Organize links into **folders/collections**.
- Optionally **sync metadata** (not snapshots) across devices via Appwrite.

The app must work **fully offline**, with optional online sync for users who want cross-device access.

---

## 2️⃣ Core Features
| Feature | Description |
|--------|-------------|
| **Link Management** | Add, edit, delete links with title, note, URL, preview image. |
| **Snapshots (Local Only)** | Capture reader mode text, PDF, or full-page screenshot. |
| **Collections/Folders** | Organize links into folders for better management. |
| **Search & Filter** | Search links/folders by title, note, or tags. Filter by favorites, archived, or deleted. |
| **Optional Online Sync** | Sync metadata (but NOT snapshots) to Appwrite. |
| **Flagging/Archiving/Trash** | Mark important links, archive, or move to trash. |
| **Versioned Snapshots** | Each link can have multiple snapshots (different timestamps). |
| **Preview Auto-fetch** | Auto-fetch title & preview image when a link is added. |
| **Offline-First** | App works completely offline with local Room DB. |

---

## 3️⃣ UI & Navigation

### 📱 Navigation Structure
- **Bottom Navigation Tabs**
    - **Home** 🏠 – All links, with search & filters.
    - **Collections** 📂 – User-created folders for organizing links.
    - **Settings** ⚙️ – Sync options, app settings, export/import.
- **Floating Action Button (FAB)** ➕
    - Add new link (opens Add Link Page).

### 🖼️ Screens
| Screen | Key UI Elements | Description |
|-------|------------------|-------------|
| **Splash** | App logo + loading indicator | Check login state, network status, and perform initial sync. |
| **Home** | Search bar, list/grid of saved links, filters (favorites, archived). | Default landing page showing all links. |
| **Collections** | Folder list with counts, search bar, create/edit folder button. | Manage folders/collections. |
| **Link Details** | Link preview, notes, list of snapshots (PDF/Reader/Image). | View/edit link info and open snapshots. |
| **Add Link** | URL field, auto-fetch preview, custom title/note, folder selection, snapshot options. | Add or edit a link. |
| **Snapshot Viewer** | Open saved snapshot (Reader text, PDF, or full screenshot). | Full-screen view of stored content. |
| **Settings** | Sync controls, login/logout, export/import, theme toggle. | Manage app-wide settings and sync preferences. |

---

## 4️⃣ Feature Implementation (Brief)

### 🔗 Link Management
- **Tech**: Room DB for local storage.
- **Implementation**:
    - Auto-fetch title & preview using a lightweight HTTP call (e.g., `Jsoup` or `Retrofit`).
    - Store link metadata in Room (`id`, `title`, `url`, `note`, `folderId`, `previewPath`, `previewUrl`, `updatedAt`, `syncToRemote`).
    - Allow custom title/notes before saving.

### 📸 Snapshots
- **Options**:
    - **Reader Mode** → Extract main article text (via Jsoup or custom parser).
    - **PDF Capture** → Use `PdfRenderer` or `WebView` printing APIs.
    - **Full Page Screenshot** → Use `WebView.capturePicture()` or custom capture.
- **Storage**:
    - Local only, saved in `filesDir` or `MediaStore`.
    - Linked to `snapshot` table in Room.

### 📂 Collections/Folders
- **Tech**: Room DB with Folder table.
- **Implementation**:
    - Users create folders to group links.
    - Each link can belong to one folder.
    - CRUD operations with timestamps for sync.

### 🔍 Search & Filters
- **Tech**: Room `LIKE` queries with `Flow`/`LiveData`.
- **Implementation**:
    - Search across `title`, `note`, and `url`.
    - Filters for favorites, archived, trashed.

### ☁️ Optional Online Sync
- **Backend**: Appwrite Database.
- **Sync Logic**:
    - Only metadata (links, folders) is synced.
    - Uses **incremental sync** (updatedAt + dirty flag).
    - Snapshots remain **local only**.

### 🏷️ Flags & Archiving
- Add boolean flags (`isFavorite`, `isArchived`, `isDeleted`) to Links table.
- `deletedAt` supports soft delete for trash bin.

### ⚙️ Settings & Login
- **Optional login** using **OTP email auth** (Appwrite).
- Users without accounts can use app offline forever.
- Users with accounts can sync metadata across devices.

---

## 5️⃣ Tech Stack

| Layer | Tech |
|------|------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose *(or XML if preferred)* |
| **Navigation** | Jetpack Navigation Component |
| **Local DB** | Room with Flow |
| **Backend** | Appwrite (Database + Authentication) |
| **Networking** | Retrofit + OkHttp |
| **DI** | Koin |
| **Background Tasks** | WorkManager |
| **Logging** | Timber |
| **Preview Fetching** | Jsoup / Coil for images |
| **PDF & Screenshots** | Android WebView + Print APIs |

---

## 6️⃣ Data Sync Details

### 🔑 Principles
- Offline-first with **incremental sync**:
    - **Pull only** remote records where `updatedAt > last_pull_time`.
    - **Push only** local records with `syncToRemote = true`.
- **Snapshots never synced** to Appwrite.

### 🔹 Local (Room DB)
- **Tables**:
    - `links` (metadata + sync flags)
    - `folders` (metadata + sync flags)
    - `snapshots` (local-only)
    - `config` (key-value: `last_pull_time`)
- **Key Fields**: `id`, `updatedAt`, `syncToRemote`, `deletedAt`

### 🔹 Remote (Appwrite)
- **Collections**:
    - `links` (metadata only, no snapshots)
    - `folders`
- Key fields: `id`, `title`, `url`, `note`, `previewUrl`, `updatedAt`, `deletedAt`

### 🔹 Sync Algorithm
1. **Pull Phase**
    - Fetch records from Appwrite where `updatedAt > last_pull_time`.
    - Insert or update local records if remote is newer.
    - Update `last_pull_time` after successful pull.

2. **Push Phase**
    - Find local records where `syncToRemote = true`.
    - Upsert to Appwrite with latest metadata.
    - Mark records as synced (`syncToRemote = false`).

3. **Conflict Resolution**
    - **Last-write-wins** using `updatedAt`.
    - Remote overwrites local if newer; local overwrites remote if newer.

### 🔹 Triggers
- App launch (if online & logged in)
- Manual **Sync Now** button
- Background sync (WorkManager)

---

## 7️⃣ User Flow (High-Level)


```

App Launch → Splash Screen  
│  
Check login & network  
│  
┌───── Online? ─────┐  
│ │  
No → Load local DB Yes → Incremental Sync  
│  
┌──────── Pull ─────────┐  
│ Fetch remote updates │  
│ Update local DB │  
└───────────────────────┘  
│  
┌──────── Push ─────────┐  
│ Upload local changes │  
│ Mark synced │  
└───────────────────────┘  
│  
App UI Ready

```

---

## 8️⃣ Future Enhancements
- 🔄 **Realtime Sync** via Appwrite subscriptions.
- 🌐 **Link Import/Export** to CSV/JSON.
- 🌙 **Dark Mode** toggle in settings.
- 📤 **Share Snapshots** externally.
- 🏷️ Tagging system for links.

---



# LinkSaver – Sync Plan

## Part 1: Theory (How Sync Works)

### 1. Objective
Enable efficient, offline-first two-way synchronization of **link metadata** between **local Room DB** and **Appwrite backend**, while keeping snapshots completely local.

**Goals:**
- Minimize network usage
- Avoid full dataset fetches/pushes
- Ensure conflict-safe updates
- Support optional login

---

### 2. Core Idea
- Track changes using **timestamps** (`updatedAt`) and a **dirty flag** (`syncToRemote`)
- Pull only records that changed **since last successful pull**
- Push only records marked as **dirty** locally
- Snapshots are never uploaded; stored locally only

---

### 3. Data Structures

**Local (Room DB):**
- **Links Table:** metadata, flags, `updatedAt`, `syncToRemote`, `deletedAt`
- **Config Table:** stores `last_pull_time`

**Remote (Appwrite):**
- **Links Collection:** metadata only (`id`, `title`, `url`, `note`, `updatedAt`, `deletedAt`, `previewUrl`)
- Snapshots are **never synced**

**Key Fields:**  
| Field | Purpose |
|-------|---------|
| `id` | Unique identifier, same locally and remotely |
| `updatedAt` | Last modification timestamp |
| `syncToRemote` | Marks local changes to push |
| `deletedAt` | Soft delete tracking |
| `last_pull_time` | Tracks last successful pull |

---

### 4. Sync Workflow (Theory)

#### A. On App Launch
1. Check if user is logged in (sync optional)
2. Check network connectivity
    - Offline → skip remote sync, load local DB
    - Online → start incremental sync

#### B. Incremental Sync
1. **Pull Phase:**
    - Query Appwrite for records with `updatedAt > last_pull_time`
    - Insert new records locally or update if remote is newer
2. **Push Phase:**
    - Find local records where `syncToRemote = true`
    - Upsert them to Appwrite
    - Mark local records as synced (`syncToRemote = false`)

#### C. Manual / Background Sync
- Manual: “Sync Now” button
- Background: WorkManager scheduled tasks
- Optional: Realtime updates via Appwrite

#### D. Conflict Handling
- Last-write-wins using `updatedAt`
- Remote wins if newer, local wins if newer
- Optional: prompt user if both changed the same field simultaneously

---

### 5. Local Update Rules
- **Add/Edit Link:** `updatedAt = now`, `syncToRemote = true`
- **Delete Link:** set `deletedAt = now`, `syncToRemote = true`
- Snapshots updated locally, **never synced**

---

### 6. Sync Benefits
- Fast: only changed records moved
- Safe: conflict-free with last-write-wins
- Flexible: offline-first, optional login
- Scalable: easily add new tables/fields

---
## Part 2: Technical Implementation (Pseudo/Overview)

### 1. Pull Remote Changes
```kotlin
val lastPull = configDao.get("last_pull_time") ?: 0L
val remoteChanges = appwrite.fetchLinks(updatedSince = lastPull)

for (remote in remoteChanges) {
    val local = linkDao.getById(remote.id)

    if (local == null) {
        linkDao.insert(remote.toEntity(syncToRemote = false))
    } else if (remote.updatedAt > local.updatedAt) {
        linkDao.update(remote.toEntity(syncToRemote = false))
    }
    // else: local is newer → leave for push
}

// Update last successful pull timestamp
configDao.set("last_pull_time", System.currentTimeMillis())

```

----------

### 2. Push Local Changes

```kotlin
val dirtyLinks = linkDao.getDirtyLinks() // where syncToRemote = true

for (link in dirtyLinks) {
    appwrite.upsertLink(
        id = link.id,
        data = link.toRemoteMap(updatedAt = link.updatedAt)
    )
    linkDao.markSynced(link.id) // set syncToRemote = false
}

```

----------

### 3. Local Update Logic

```kotlin
// When user adds, edits, or deletes a link
linkDao.update(
    link.copy(
        updatedAt = System.currentTimeMillis(),
        syncToRemote = true
    )
)

```

----------

### 4. Sync Triggers

-   App launch (if online & logged in)

-   Manual “Sync Now”

-   Background schedule (WorkManager)

-   Optional: Realtime updates trigger pull


----------

### 5. Snapshots

-   Stored **only locally**

-   Each link can have multiple snapshots, one version per type

-   No snapshot data is ever pushed to Appwrite


----------

### 6. Flow Diagram (Textual)

```
User opens app → Splash Screen
          │
  Check login & network
          │
     ┌───── Online? ─────┐
     │                    │
   No → Load local DB     Yes → Incremental Sync
                             │
          ┌─────────────── Pull ───────────────┐
          │ Fetch remote links updated since last_pull_time
          │ Insert or update local DB as needed
          │ Update last_pull_time
          └────────────────────────────────────┘
                             │
          ┌─────────────── Push ────────────────┐
          │ Query local dirty links (syncToRemote)
          │ Upsert to Appwrite
          │ Mark synced locally
          └────────────────────────────────────┘
                             │
                  App UI fully loaded
                             │
         Local edits → mark dirty, wait for next sync

```

----------

### ✅ Key Advantages

-   Efficient delta-based sync (no full dataset transfer)

-   Conflict resolution handled automatically

-   Offline-first: app fully functional without network

-   Snapshots remain local for privacy and storage efficiency
    