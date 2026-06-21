# ADR-0007: Desktop Application Architecture - JavaFX with Offline-First Design

## Status
Accepted

## Context
SmartStock AI includes a desktop application for warehouse and inventory operations. This application must:
- **Responsiveness**: Fast, native-like performance (not Electron)
- **Reliability**: Operate offline when network is unavailable
- **Consistency**: Sync with server when connectivity restored
- **Professional**: Enterprise-grade UI/UX (not web-based)
- **Lightweight**: Low memory footprint for warehouse terminals
- **Maintainable**: Leverage team's Java expertise

Desktop application is critical for warehouse operations where:
- Network connectivity is unreliable (warehouse floors may have dead zones)
- Low-latency response is essential (quick stock movements)
- Professional UI expected (not browser-based)
- Multiple concurrent workstations using shared terminal stations

The choice between JavaFX, Electron, or web-based PWA significantly impacts responsiveness, offline capability, and maintenance burden.

## Decision
Implement **JavaFX Desktop Application with Offline-First Architecture**:

### 1. **Technology Stack**
- **Framework**: JavaFX (native, not Electron)
- **Language**: Java 21 (consistent with backend)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Build**: Maven with JavaFX plugins
- **Dependencies**:
  - ControlsFX (extended controls library)
  - Ikonli (icon library)
  - Mapstruct (DTO mapping)
  - Jackson (JSON serialization)
  - SQLite (local database for offline caching)
  - HTTP Client (for API communication)

### 2. **Architecture Layers**

**Presentation Layer (FXML + CSS)**
- FXML templates define UI structure
- CSS styling for themes (dark mode, light mode)
- Responsive layouts (adapt to different screen sizes)
- No business logic in controllers

**ViewModel Layer**
- Holds UI state (selected warehouse, current user, filters)
- Coordinates between UI and services
- Implements data binding to UI components
- Handles user interactions (button clicks, form submissions)

**Service Layer**
- APIClient: Communicates with backend services
- OfflineCache: Manages local SQLite database
- Sync: Reconciles local changes with server
- EventBus: Notifies UI of data changes

**Persistence Layer**
- SQLite for offline caching
- Stores last-known state of products, inventory, users
- Tracks local changes (mutations) that need sync
- Periodically purges old cached data

### 3. **Offline-First Workflow**

**Online Mode**
```
User Action (Stock In)
     ↓
ViewModel processes action
     ↓
APIClient sends to backend API
     ↓
Backend persists and publishes event
     ↓
APIClient receives confirmation
     ↓
ViewModel updates local state
     ↓
UI refreshes
```

**Offline Mode**
```
User Action (Stock In)
     ↓
ViewModel detects network unavailable
     ↓
Store operation in local SQLite
     ↓
Confirm to user: "Saved locally, will sync when online"
     ↓
UI updates with local state
     ↓
Display "Offline" indicator to user
```

**Sync (When Online)**
```
Detect network restored
     ↓
Query local database for pending operations
     ↓
Send pending operations to backend (batch if possible)
     ↓
Backend processes and confirms
     ↓
Clear local pending queue
     ↓
Update UI to reflect server state
     ↓
Display "In Sync" to user
```

### 4. **Data Sync Strategy**

**Local Cache Database**
```sql
-- Cached data from server
CREATE TABLE product_cache (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  sku VARCHAR(50) NOT NULL,
  category VARCHAR(100),
  unit_price DECIMAL(10, 2),
  cached_at TIMESTAMP,
  cached_version INT
);

CREATE TABLE inventory_cache (
  id UUID PRIMARY KEY,
  product_id UUID NOT NULL,
  warehouse_id UUID NOT NULL,
  quantity_on_hand INT,
  quantity_reserved INT,
  cached_at TIMESTAMP
);

-- Pending operations (to be synced)
CREATE TABLE pending_operations (
  id UUID PRIMARY KEY,
  operation_type VARCHAR(50),  -- 'STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT'
  payload JSONB,
  status VARCHAR(20),  -- 'PENDING', 'SYNCED', 'FAILED'
  created_at TIMESTAMP,
  synced_at TIMESTAMP
);
```

**Sync Process**
1. On startup, load cache from SQLite
2. Show cached data immediately (instant perceived performance)
3. In background, query backend API for updates
4. Apply differential updates only
5. Every 5 minutes, send any pending operations to server
6. Handle conflicts: server version always wins (last-write-wins strategy)

### 5. **User Experience Features**

**Connectivity Indicator**
- Top-right corner shows connection status
- Green: Online and synced
- Yellow: Online but syncing
- Red: Offline (only local operations possible)

**Conflict Resolution**
- If server has newer data, show dialog to user
- Options: Keep local changes, use server version, or merge
- For stock quantities: always use server version (source of truth)

**Offline Features**
- View cached products and inventory
- Perform stock movements locally
- Search works on local cache
- Reports show local data only

**Sync Status**
- Show number of pending operations
- Estimated time until sync
- Sync error details if sync fails
- Manual "Sync Now" button

### 6. **Security**

**Token Storage**
- Access token stored in memory only (lost on app close)
- Refresh token stored in OS keychain (encrypted)
- Automatic token refresh before expiration

**Offline Operations**
- Only authorized users can modify data offline
- Authentication required before offline operations allowed
- Session timeout: 8 hours (suitable for warehouse shift)

**Cache Encryption**
- SQLite database encrypted with sqlcipher (password-based)
- Password derived from user credentials

## Alternatives Considered

### Option 1: Electron (Web-Based Desktop)
Build desktop app using Electron (Chromium + Node.js)

**Pros:**
- Single codebase for web and desktop
- Easy to find developers
- Quick to develop initial features
- Good for modern UIs with web frameworks

**Cons:**
- High memory footprint (Chromium process overhead)
- Slower startup time
- Not truly native (feels "off" on Windows/Mac/Linux)
- Difficult to optimize for warehouse terminals
- More difficult to implement true offline-first
- Team expertise in Java better leveraged with native

### Option 2: Progressive Web App (PWA)
Browser-based application with offline capability

**Pros:**
- No installation required
- Cross-platform
- Modern development practices

**Cons:**
- Browser limitations (file system access, hardware control)
- Limited offline capability compared to native
- Dependent on network connectivity
- Difficult to control UI responsiveness
- Browser security model limits functionality
- Poor for unreliable warehouse networks

### Option 3: Qt (C++ Desktop Framework)
Cross-platform C++ desktop framework

**Pros:**
- Excellent performance
- Lightweight
- Native look and feel

**Cons:**
- Team expertise in C++ not established
- Slower development cycle
- Smaller ecosystem compared to JavaFX
- More difficult recruitment

## Consequences

### Positive
- **Responsiveness**: Native JavaFX performance comparable to compiled code
- **Offline Capability**: Can work for extended periods offline; sync when reconnected
- **Resilient**: Warehouse operations continue even during network outages
- **User Friendly**: Professional desktop experience vs. web interface
- **Team Expertise**: Leverages team's Java knowledge
- **Lightweight**: Lower memory footprint than Electron
- **Fast Startup**: Launches quickly (seconds, not tens of seconds)
- **Data Security**: Offline cache encrypted; credentials never persisted to disk
- **Consistent**: Same backend API as web/mobile clients; consistent behavior
- **Scalable UI**: FXML and CSS enable UI evolution without code changes

### Negative
- **Native Dependencies**: Java must be installed on workstations
- **UI Update Burden**: Desktop UI diverges from future web UI; requires double development
- **Testing Complexity**: Must test offline scenarios and sync edge cases
- **Data Inconsistency**: Temporary inconsistency between local and server state
- **Conflict Resolution**: Complex logic needed to handle sync conflicts
- **Cache Management**: Must decide when to purge old cached data
- **Development Overhead**: MVVM pattern requires careful separation of concerns
- **Storage Overhead**: Local SQLite database duplicates data

### Trade-offs
- **Development Speed vs. Performance**: JavaFX slower to develop than web but better performance
- **Simplicity vs. Offline Capability**: Offline-first adds complexity but enables warehouse resilience
- **Cache Consistency vs. Storage**: Maintain local cache for performance at cost of storage and sync complexity

## Future Considerations

1. **Biometric Authentication**: Fingerprint/face recognition for warehouse terminals
   - Faster authentication than passwords
   - Suitable for shared warehouse terminal stations

2. **Barcode Scanning**: Hardware barcode scanner integration
   - Real-time product lookup
   - Quick stock movement entry
   - Reduce manual data entry errors

3. **Mobile Application**: Native mobile app (iOS/Android) for tablet/phone use
   - Same backend API as desktop
   - Responsive layout for smaller screens
   - Touch-optimized controls

4. **Accessibility**: WCAG 2.1 AA compliance
   - Screen reader support
   - High contrast mode
   - Keyboard navigation

5. **Localization**: Support multiple languages
   - Warehouse operations may span multiple countries
   - Extensible resource bundle system

6. **Advanced Conflict Resolution**: Smarter merge strategies
   - For inventory, use server quantity as source of truth but merge other fields
   - Preserve user intent when possible

7. **Offline Analytics**: Local reporting on cached data
   - Analyze patterns in offline period
   - Understand warehouse behavior offline

8. **Cloud Sync Options**: Support multiple cloud providers
   - Primary: Internal API
   - Fallback: Cloud backup (AWS, Azure, GCP)
   - Enables disaster recovery

## Implementation Guidance

- Project structure follows Maven standards with src/main/java, src/main/resources, src/test/java
- FXML templates in resources/fxml organized by feature
- CSS stylesheets in resources/css with theme support
- ViewModels named *ViewModel and hold no references to UI components
- Controllers named *Controller and delegate business logic to ViewModels
- Services implement interfaces for testability
- Unit tests for ViewModels mock API and cache layers
- Integration tests verify sync behavior with local database
- Error handling: show user-friendly messages, log technical details
- Logging includes correlation IDs for matching with server logs
- Configuration externalized to properties files (API URLs, cache settings)
- Installer package includes Java runtime for end-user convenience
