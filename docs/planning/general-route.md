# SmartExpApp General Route

## 1. Product Scope

SmartExpApp is a native Android app that helps users reduce food and product waste by tracking expiry dates, warning users before items expire, and using an AI Agent to answer inventory questions, suggest recipes, and recommend actions for expiring items.

### Primary user goal

Users should be able to add products quickly, know what needs attention today, and ask the Agent what to cook or do before items expire.

### MVP scope

1. Local-first persistent inventory with add, edit, delete, search, filter, and sort.
2. Expiry dashboard with expired, today, soon, and safe product groups.
3. Local reminders before products expire.
4. OCR-assisted expiry date scanning with user confirmation.
5. Barcode lookup to auto-fill product details where possible.
6. AI Agent screen with typed prompts, suggested question chips, voice input/output, and recipe help for expiring products.
7. Basic waste impact stats: saved, wasted, expired, donated, and consumed items.
8. Settings for reminders, dietary preferences, storage defaults, and app preferences.

### Storage direction

The first release is local-first. Product data, settings, scan history, Agent context, and inventory actions should be saved locally using Room. Google sign-in and cloud sync can be added later behind the same repository APIs, after the local database contract is stable.

### Out of scope for first release

1. Cloud sync, login, multi-device accounts, and household sharing.
2. Payment, grocery ordering, or retailer cart checkout.
3. Community recipe sharing.
4. Advanced nutrition coaching or medical diet advice.

### Delivery direction

The native Android app in `app/` is the main delivery target. The React/Vite prototype in `googleaistudio/` remains reference material only unless the team explicitly decides to build a web app later.

### Bottom navigation direction

The current `Meals` concept is replaced by `Agent`. Recipe suggestions still exist, but they live inside the Agent screen instead of being treated as a standalone bottom-nav product area.

## 2. Required Diagrams

### 2.1 Use Case Diagram

```mermaid
flowchart LR
    User([User])
    OCR([OCR/AI Service])
    ProductAPI([Barcode/Product API])
    AgentAI([Agent AI Service])
    RecipeAPI([Recipe API])
    Notifications([Android Notification System])
    CloudSync([Optional Cloud Sync])

    User --> AddManual[Add product manually]
    User --> ScanExpiry[Scan expiry date]
    User --> ScanBarcode[Scan barcode]
    User --> ViewInventory[View and manage inventory]
    User --> ViewDashboard[View expiry dashboard]
    User --> AskAgent[Ask Agent questions]
    User --> UsePrompt[Tap suggested Agent prompt]
    User --> VoiceChat[Speak with Agent]
    User --> MarkOutcome[Mark consumed, wasted, donated, or expired]
    User --> ConfigureSettings[Configure reminders and preferences]

    ScanExpiry --> OCR
    ScanBarcode --> ProductAPI
    AskAgent --> AgentAI
    UsePrompt --> AgentAI
    VoiceChat --> AgentAI
    AgentAI --> RecipeAPI
    ViewDashboard --> Notifications
    ConfigureSettings -. later .-> CloudSync
```

### 2.2 Architecture Diagram

```mermaid
flowchart TB
    subgraph AndroidApp[Native Android App]
        UI[Activities and XML UI screens]
        VM[View/state layer]
        Repo[Repositories]
        Agent[Agent module]
        Scanner[OCR and barcode scanner modules]
        Reminder[Notification scheduler]
        Sync[Future cloud sync service]
    end

    Room[(Room local database)]
    Camera[Camera / image picker]
    AgentAI[Gemini or third-party AI service]
    ProductAPI[Open Food Facts product API]
    RecipeAPI[Spoonacular or Edamam recipe API]
    NotificationSystem[Android notifications]
    Cloud[(Future cloud database)]

    UI --> VM
    VM --> Repo
    Repo --> Room
    Scanner --> Camera
    Scanner --> ProductAPI
    Agent --> Repo
    Agent --> AgentAI
    AgentAI --> RecipeAPI
    Reminder --> Room
    Reminder --> NotificationSystem
    Sync -. later .-> Room
    Sync -. later .-> Cloud
```

### 2.3 ERD / Database Schema

```mermaid
erDiagram
    PRODUCT {
        string id PK
        string name
        string category
        string quantity
        string unit
        string storageLocationId FK
        date expiryDate
        string barcode
        string imageUri
        string status
        datetime createdAt
        datetime updatedAt
    }

    STORAGE_LOCATION {
        string id PK
        string name
        string type
        int sortOrder
    }

    INVENTORY_ACTION {
        string id PK
        string productId FK
        string actionType
        int quantityChanged
        datetime actionAt
        string note
    }

    EXPIRY_SCAN {
        string id PK
        string productId FK
        string rawText
        date detectedDate
        float confidence
        datetime scannedAt
    }

    RECIPE_CACHE {
        string id PK
        string provider
        string title
        string imageUrl
        string sourceUrl
        string usedIngredients
        string missingIngredients
        datetime cachedAt
    }

    AGENT_MESSAGE {
        string id PK
        string role
        string message
        datetime createdAt
        string relatedProductIds
        string sourcePrompt
    }

    USER_SETTINGS {
        string id PK
        int reminderDaysBefore
        string dietaryPreferences
        string defaultStorageLocationId FK
        boolean notificationEnabled
    }

    STORAGE_LOCATION ||--o{ PRODUCT : stores
    PRODUCT ||--o{ INVENTORY_ACTION : has
    PRODUCT ||--o{ EXPIRY_SCAN : has
    PRODUCT }o--o{ RECIPE_CACHE : informs
    PRODUCT }o--o{ AGENT_MESSAGE : referenced_by
    STORAGE_LOCATION ||--o{ USER_SETTINGS : defaults_to
```

### 2.4 Data Flow Diagram

```mermaid
flowchart LR
    User[User]
    Camera[Camera input]
    OCR[OCR/date parser]
    Barcode[Barcode scanner]
    ProductAPI[Product lookup API]
    Agent[Agent module]
    AgentAI[AI service]
    RecipeAPI[Recipe API]
    App[SmartExpApp logic]
    DB[(Room local database)]
    Notify[Notification scheduler]
    Cloud[Future cloud sync]

    User -->|manual product details| App
    User --> Camera
    Camera --> OCR
    Camera --> Barcode
    OCR -->|detected expiry candidates| App
    Barcode -->|barcode value| ProductAPI
    ProductAPI -->|product metadata| App
    App -->|confirmed product| DB
    DB -->|inventory context| Agent
    User -->|typed prompt / suggested prompt / voice| Agent
    Agent --> AgentAI
    AgentAI --> RecipeAPI
    AgentAI -->|answer or recipe guidance| Agent
    Agent -->|confirmed inventory changes only| App
    DB --> Notify
    Notify -->|reminder| User
    DB -. later .-> Cloud
```

### 2.5 Local Product Save Sequence

```mermaid
sequenceDiagram
    actor User
    participant UI as Add Product UI
    participant Repo as Product Repository
    participant DB as Room Database
    participant Reminder as Reminder Scheduler

    User->>UI: Enter or confirm product details
    UI->>Repo: Save product
    Repo->>DB: Insert or update product
    DB-->>Repo: Save success
    Repo->>Reminder: Schedule expiry reminder
    Reminder-->>Repo: Reminder scheduled
    Repo-->>UI: Product saved
```

### 2.6 OCR Add Product Sequence

```mermaid
sequenceDiagram
    actor User
    participant App as SmartExpApp
    participant Camera
    participant OCR as OCR/AI
    participant Parser as Date Parser
    participant DB as Room Database

    User->>App: Tap scan expiry date
    App->>Camera: Open camera or image picker
    Camera-->>App: Product label image
    App->>OCR: Extract text
    OCR-->>App: Raw text candidates
    App->>Parser: Parse expiry date candidates
    Parser-->>App: Dates with confidence
    App-->>User: Show candidates for confirmation
    User->>App: Confirm or correct date
    App->>DB: Save product and scan metadata
    DB-->>App: Save success
```

### 2.7 Agent Prompt Sequence

```mermaid
sequenceDiagram
    actor User
    participant AgentUI as Agent Screen
    participant Repo as Product Repository
    participant Agent as Agent Module
    participant AI as AI Service
    participant RecipeAPI as Recipe API

    User->>AgentUI: Type, speak, or tap suggested prompt
    AgentUI->>Repo: Load relevant inventory context
    Repo-->>AgentUI: Expiring and relevant products
    AgentUI->>Agent: Build minimal prompt context
    Agent->>AI: Ask question with inventory context
    AI->>RecipeAPI: Request recipe data if needed
    RecipeAPI-->>AI: Recipe candidates
    AI-->>Agent: Response and suggestions
    Agent-->>AgentUI: Text and optional spoken response
    AgentUI-->>User: Show answer, recipe help, or next action
```

### 2.8 Future Cloud Sync Sequence

```mermaid
sequenceDiagram
    actor User
    participant App as SmartExpApp
    participant DB as Room Database
    participant Auth as Google Sign-In
    participant Sync as Sync Service
    participant Cloud as Cloud Database

    User->>App: Sign in with Google later
    App->>Auth: Authenticate user
    Auth-->>App: User identity
    App->>DB: Read local records
    DB-->>Sync: Unsynced local changes
    Sync->>Cloud: Upload changes
    Cloud-->>Sync: Sync success or conflict
    Sync->>DB: Update sync status
```

## 3. GitHub Issue Backlog

| No. | Issue title | Work area |
| --- | --- | --- |
| 1 | Planning: finalize scope, diagrams, and delivery route | `01-planning-diagrams-scope` |
| 2 | Foundation: lock app architecture and persistent data layer | `02-foundation-persistence` |
| 3 | Inventory: implement CRUD, search, filter, and sort | `03-inventory-crud` |
| 4 | Expiry: dashboard status groups and local reminders | `04-expiry-reminders` |
| 5 | OCR: scan and confirm expiry dates from product labels | `05-ocr-expiry-scan` |
| 6 | Barcode: lookup products and auto-fill metadata | `06-barcode-product-lookup` |
| 7 | Agent: voice/chat assistant with recipe help | `07-agent-assistant` |
| 8 | Insights: stats, waste impact, and settings | `08-stats-settings` |
| 9 | QA and submission: tests, README, report, slides, and demo | `09-testing-docs-submission` |

## 4. Work Route

The active integration branch is `dev`. New work should branch from `dev`, merge back into `dev` after review/testing, then merge the final stable release into the repository default branch.

### Work rules

1. Keep `master` stable.
2. Use `dev` as the integration branch.
3. Use one numbered work area per issue group.
4. Open one pull request per work area into `dev`.
5. Do not mix unrelated features in the same pull request.
6. Update report/screenshots after the matching feature is working.
7. Keep public issue descriptions focused on goals and acceptance criteria, not branch details.

### Recommended merge order

1. `01-planning-diagrams-scope`
2. `02-foundation-persistence`
3. `03-inventory-crud`
4. `04-expiry-reminders`
5. `05-ocr-expiry-scan`
6. `06-barcode-product-lookup`
7. `07-agent-assistant`
8. `08-stats-settings`
9. `09-testing-docs-submission`
