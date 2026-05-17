# **COMPREHENSIVE FEATURE PLAN: Local Web Portal for Android App**

## **1. FEATURE OVERVIEW**

### **Feature Name**
"Open in Web" - Local Web Portal with QR Code Access

### **Description**
Enable users to access their app's data through a web browser on devices connected to the same WiFi network. The app will run a local HTTP server hosting a responsive web UI that mirrors the mobile app's data and functionality.

### **Business Value**
- **Better UX for data viewing** on larger screens
- **Cross-device accessibility** without cloud dependency
- **Privacy-first approach** - all data stays local
- **Differentiation** from competitors
- **Reduced mobile battery usage** for browsing

---

## **2. SYSTEM ARCHITECTURE OVERVIEW**

```
┌─────────────────────────────────────────────────────────────┐
│                    ANDROID DEVICE                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         Android App UI (Jetpack Compose)             │   │
│  │  - "Open in Web" Screen                              │   │
│  │  - Toggle to Enable/Disable Web Access               │   │
│  │  - QR Code Display                                   │   │
│  │  - Live IP Address & Port                            │   │
│  └──────────────────────────────────────────────────────┘   │
│                              ↓                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │    Ktor HTTP Server (CIO Engine)                     │   │
│  │  - Port: 8192 (default, configurable)                │   │
│  │  - TLS/SSL Support (optional)                        │   │
│  │  - CORS Headers for WiFi requests                    │   │
│  │  - REST API endpoints for data                       │   │
│  │  - Static file serving (web app assets)              │   │
│  └──────────────────────────────────────────────────────┘   │
│                              ↓                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │    Data Repository & Database (Room/SQLite)         │   │
│  │  - Cards Data                                        │   │
│  │  - User Preferences                                  │   │
│  │  - Media Files                                       │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
              ↑                                    ↑
              │ Same WiFi Network                 │
              │                                   │
┌─────────────────────────────────────────────────────────────┐
│                  LAPTOP/DESKTOP BROWSER                      │
├─────────────────────────────────────────────────────────────┤
│  Scan QR → Get URL → Open http://192.168.x.x:8192          │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │    Web UI (React/Vue or HTML+CSS+JavaScript)        │   │
│  │  - Responsive Layout (Mobile → Tablet → Desktop)    │   │
│  │  - Left Navigation Sidebar (from mobile drawer)      │   │
│  │  - Cards Grid/List Display                          │   │
│  │  - All App Screens as Web Pages                      │   │
│  │  - Real-time Updates (WebSocket optional)            │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## **3. DETAILED FEATURE TASKS**

### **PHASE 1: BACKEND INFRASTRUCTURE (Server & API)**

---

#### **TASK 1.1: Set up Ktor HTTP Server**

**What to do:**
Set up an embedded Ktor HTTP server that runs on the Android device and listens on a local port for incoming web requests.

**Why it's required:**
- Ktor is lightweight, coroutine-native, and perfect for embedded Android servers
- Provides easy routing, middleware, and content negotiation
- Better than raw ServerSocket - handles HTTP complexities automatically
- CIO engine is ideal for mobile (uses coroutines, minimal overhead)

**Dependencies to add:**
```kotlin
// build.gradle.kts (Module: app)
implementation("io.ktor:ktor-server-core:2.3.12")
implementation("io.ktor:ktor-server-cio:2.3.12")
implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
implementation("io.ktor:ktor-server-cors:2.3.12")
implementation("io.ktor:ktor-server-call-logging:2.3.12")
```

**Permissions needed:**
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

**Implementation Details:**

1. **Create WebServerManager.kt** (Singleton)
   - Start/stop server lifecycle
   - Get local IP address & port
   - Handle server state (running, stopped, error)

2. **Create Application.module()** in Ktor
   ```kotlin
   fun Application.webServerModule() {
       // Install plugins
       install(ContentNegotiation) {
           json()
       }
       install(CORS) {
           allowHost("*", subDomains = listOf("192.168"))
           allowCredentials = true
       }
       install(CallLogging)
       
       // Static files
       static("") {
           resources("web") // Web assets in res/raw or assets
       }
       
       // API routes
       routing {
           get("/api/health") { call.respond(mapOf("status" to "ok")) }
       }
   }
   ```

3. **Run server in Foreground Service** (keep it running even when app goes background)
   - Create `WebServerService extends Service`
   - Post notification (required for foreground services)
   - Start in onCreate, stop in onDestroy

**Acceptance Criteria:**
- ✅ Server starts without crashing when toggle is enabled
- ✅ Server listens on port 8192 (or configurable port)
- ✅ Can access `http://192.168.x.x:8192/api/health` from browser
- ✅ CORS headers allow requests from same WiFi
- ✅ Server continues running in background
- ✅ Server stops gracefully when toggle is disabled
- ✅ Errors are logged with clear messages

---

#### **TASK 1.2: Implement IP Detection & QR Code Generation**

**What to do:**
Detect the device's local IP address on the WiFi network and generate a QR code containing the access URL.

**Why it's required:**
- Users need an easy way to find the correct address and port
- QR code eliminates manual IP/port entry errors
- Automatic IP detection handles different network configurations

**Dependencies to add:**
```kotlin
// QR Code generation library
implementation("io.github.g0dkar:qrcode-kotlin-android:4.5.0")
// OR alternative
implementation("com.google.zxing:core:3.5.1")
```

**Implementation Details:**

1. **Create NetworkUtils.kt**
   ```kotlin
   object NetworkUtils {
       fun getLocalIpAddress(): String? {
           try {
               val interfaces = NetworkInterface.getNetworkInterfaces()
               while (interfaces.hasMoreElements()) {
                   val networkInterface = interfaces.nextElement()
                   val addresses = networkInterface.inetAddresses
                   
                   while (addresses.hasMoreElements()) {
                       val inetAddress = addresses.nextElement()
                       // Skip loopback and IPv6
                       if (!inetAddress.isLoopbackAddress && 
                           inetAddress is Inet4Address) {
                           return inetAddress.hostAddress
                       }
                   }
               }
           } catch (ex: Exception) {
               Log.e("NetworkUtils", "Error getting IP", ex)
           }
           return null
       }
       
       fun isConnectedToWiFi(context: Context): Boolean {
           val connectivityManager = context.getSystemService(
               Context.CONNECTIVITY_SERVICE
           ) as ConnectivityManager
           val network = connectivityManager.activeNetwork ?: return false
           val capabilities = connectivityManager.getNetworkCapabilities(network)
           return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
       }
   }
   ```

2. **Create QRCodeGenerator.kt**
   ```kotlin
   object QRCodeGenerator {
       fun generateQRCode(
           text: String, 
           size: Int = 512
       ): Bitmap {
           val qrCode = QRCode.ofSquares()
               .withSize(size, size)
               .build(text)
           return qrCode.renderToImage()
       }
   }
   ```

3. **Add to WebServerManager**
   - Get IP when server starts
   - Construct URL: `http://{IP}:8192`
   - Generate QR code bitmap
   - Expose to UI via StateFlow/LiveData

**Acceptance Criteria:**
- ✅ Correctly detects device's local IPv4 address
- ✅ Detects when NOT connected to WiFi (shows warning)
- ✅ QR code generates without crashing
- ✅ QR code is scannable and contains correct URL
- ✅ URL format: `http://192.168.x.x:8192`
- ✅ Updated if WiFi connection changes
- ✅ Handles edge cases (no WiFi, multiple IPs)

---

#### **TASK 1.3: Build REST API Endpoints**

**What to do:**
Create API endpoints that serve the app's data (cards, metadata, pagination, filtering) in JSON format.

**Why it's required:**
- Web UI needs a way to fetch data
- Consistent format for all data types
- Enables real-time updates without full page refreshes
- RESTful design is familiar and standardized

**Implementation Details:**

1. **Define Data Models for API** (in `shared/models`)
   ```kotlin
   @Serializable
   data class CardResponse(
       val id: String,
       val title: String,
       val description: String,
       val imageUrl: String?,
       val metadata: Map<String, Any>,
       val createdAt: Long,
       val updatedAt: Long
   )
   
   @Serializable
   data class ApiResponse<T>(
       val success: Boolean,
       val data: T? = null,
       val error: String? = null,
       val timestamp: Long = System.currentTimeMillis()
   )
   
   @Serializable
   data class PaginatedResponse<T>(
       val items: List<T>,
       val totalCount: Int,
       val page: Int,
       val pageSize: Int,
       val hasMore: Boolean
   )
   ```

2. **Create API Routes** (extend Application.module())
   ```kotlin
   routing {
       // Cards endpoints
       get("/api/cards") {
           val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
           val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
           
           val cards = cardsRepository.getCards(page, limit)
           val total = cardsRepository.getTotalCount()
           
           call.respond(ApiResponse(
               success = true,
               data = PaginatedResponse(
                   items = cards.map { it.toResponse() },
                   totalCount = total,
                   page = page,
                   pageSize = limit,
                   hasMore = (page + 1) * limit < total
               )
           ))
       }
       
       get("/api/cards/{id}") {
           val id = call.parameters["id"] ?: return@get
           val card = cardsRepository.getCardById(id)
           
           if (card != null) {
               call.respond(ApiResponse(success = true, data = card.toResponse()))
           } else {
               call.respond(
                   HttpStatusCode.NotFound,
                   ApiResponse(success = false, error = "Card not found")
               )
           }
       }
       
       // Navigation/Sections
       get("/api/sections") {
           val sections = sectionsRepository.getSections()
           call.respond(ApiResponse(
               success = true,
               data = sections.map { it.toResponse() }
           ))
       }
       
       // Search
       get("/api/search") {
           val query = call.request.queryParameters["q"] ?: ""
           val results = cardsRepository.search(query)
           call.respond(ApiResponse(success = true, data = results))
       }
   }
   ```

3. **Add Request Validation**
   - Validate page/limit parameters
   - Sanitize search queries
   - Return proper HTTP status codes (400, 404, 500)

4. **Add Response Caching** (optional)
   - Cache frequently requested endpoints
   - Invalidate on data changes

**Acceptance Criteria:**
- ✅ `/api/cards` returns paginated card data
- ✅ `/api/cards/{id}` returns single card details
- ✅ `/api/sections` returns navigation sections
- ✅ `/api/search?q=term` returns search results
- ✅ All responses follow ApiResponse format
- ✅ Pagination works correctly (page, limit, hasMore)
- ✅ Returns proper HTTP status codes
- ✅ Handles errors gracefully with error messages
- ✅ API is documented (comments at minimum)

---

### **PHASE 2: FRONTEND UI (Web Interface)**

---

#### **TASK 2.1: Create Responsive Web UI Structure**

**What to do:**
Build the HTML/CSS/JavaScript foundation for the web interface that mirrors the mobile app's layout and design but optimized for desktop/laptop screens.

**Why it's required:**
- Desktop screens have different real estate (landscape, larger display)
- Navigation drawer becomes sidebar
- Cards can be displayed in larger grids
- Better ergonomics for data browsing on big screens

**Technology Stack:**
- **Option A (Recommended):** React + TypeScript + Tailwind CSS
- **Option B (Lighter):** Plain HTML + CSS Grid + Vanilla JavaScript
- **Option C:** Vue.js + Vite + Tailwind CSS

**Implementation Details (Using React):**

1. **Folder Structure**
   ```
   web/
   ├── src/
   │   ├── components/
   │   │   ├── Sidebar.tsx        # Navigation sidebar (from drawer)
   │   │   ├── CardGrid.tsx       # Grid display for cards
   │   │   ├── CardDetail.tsx     # Single card view
   │   │   ├── SearchBar.tsx      # Search functionality
   │   │   ├── Pagination.tsx     # Page navigation
   │   │   └── Layout.tsx         # Main layout wrapper
   │   ├── pages/
   │   │   ├── Dashboard.tsx      # Home/overview
   │   │   ├── CardList.tsx       # Cards list page
   │   │   ├── CardDetail.tsx     # Single card page
   │   │   └── Search.tsx         # Search results page
   │   ├── hooks/
   │   │   ├── useCards.ts        # Fetch cards data
   │   │   ├── useSections.ts     # Fetch sections/nav
   │   │   └── useSearch.ts       # Search functionality
   │   ├── api/
   │   │   └── client.ts          # API request utility
   │   ├── styles/
   │   │   └── globals.css        # Global styles
   │   ├── types/
   │   │   └── api.ts             # TypeScript types from API
   │   ├── App.tsx
   │   └── main.tsx
   ├── public/
   │   ├── index.html
   │   └── favicon.ico
   ├── package.json
   ├── tsconfig.json
   ├── vite.config.ts
   └── tailwind.config.js
   ```

2. **Main Layout Component**
   ```typescript
   // components/Layout.tsx
   export function Layout({ children }: { children: React.ReactNode }) {
       return (
           <div className="flex h-screen bg-white">
               {/* Sidebar Navigation */}
               <aside className="w-64 bg-gray-50 border-r border-gray-200 overflow-y-auto">
                   <Sidebar />
               </aside>
               
               {/* Main Content */}
               <main className="flex-1 overflow-y-auto">
                   <div className="container mx-auto px-4 py-8">
                       {children}
                   </div>
               </main>
           </div>
       );
   }
   ```

3. **Responsive Sidebar Component**
   ```typescript
   // components/Sidebar.tsx
   export function Sidebar() {
       const { sections, isLoading } = useSections();
       const [expandedSection, setExpandedSection] = useState<string | null>(null);
       
       if (isLoading) return <div>Loading...</div>;
       
       return (
           <nav className="p-4">
               <h1 className="text-xl font-bold mb-6">My App</h1>
               {sections.map((section) => (
                   <div key={section.id} className="mb-4">
                       <button
                           onClick={() => setExpandedSection(
                               expandedSection === section.id ? null : section.id
                           )}
                           className="w-full text-left p-2 hover:bg-gray-100 rounded"
                       >
                           {section.icon && <span className="mr-2">{section.icon}</span>}
                           {section.name}
                       </button>
                       {expandedSection === section.id && (
                           <div className="ml-4 mt-2">
                               {section.items?.map((item) => (
                                   <a
                                       key={item.id}
                                       href={`/${item.route}`}
                                       className="block p-2 text-sm hover:bg-gray-200 rounded"
                                   >
                                       {item.name}
                                   </a>
                               ))}
                           </div>
                       )}
                   </div>
               ))}
           </nav>
       );
   }
   ```

4. **Card Grid Component**
   ```typescript
   // components/CardGrid.tsx
   export function CardGrid({ cards, isLoading }: CardGridProps) {
       if (isLoading) return <LoadingSkeletons />;
       
       return (
           <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
               {cards.map((card) => (
                   <CardGridItem key={card.id} card={card} />
               ))}
           </div>
       );
   }
   
   function CardGridItem({ card }: { card: Card }) {
       return (
           <Link href={`/cards/${card.id}`}>
               <div className="bg-white rounded-lg shadow hover:shadow-lg transition p-4">
                   {card.imageUrl && (
                       <img
                           src={card.imageUrl}
                           alt={card.title}
                           className="w-full h-48 object-cover rounded mb-2"
                       />
                   )}
                   <h3 className="font-bold text-lg">{card.title}</h3>
                   <p className="text-gray-600 text-sm">{card.description}</p>
                   <div className="mt-2 text-xs text-gray-400">
                       Updated: {new Date(card.updatedAt).toLocaleDateString()}
                   </div>
               </div>
           </Link>
       );
   }
   ```

5. **Build & Embed Configuration**
   - Build React app with `npm run build`
   - Output goes to `dist/` folder
   - Copy `dist/` to Android app's `src/main/assets/web/` or `res/raw/web/`
   - Ktor serves these static files

**Acceptance Criteria:**
- ✅ Layout has responsive sidebar on desktop (>1024px)
- ✅ Sidebar collapses on tablet/mobile (<768px) with hamburger menu
- ✅ Card grid responsive (1 col mobile, 2 col tablet, 3+ col desktop)
- ✅ All navigation sections from app drawer are available
- ✅ Cards display with images, title, description
- ✅ Click card opens detail view
- ✅ Proper spacing and alignment on all screen sizes
- ✅ No horizontal scroll on any screen size
- ✅ Mobile-friendly touch targets (>44px)
- ✅ Fast load time (<2 seconds on good WiFi)

---

#### **TASK 2.2: Create API Client & Data Hooks**

**What to do:**
Build JavaScript/TypeScript utilities to fetch data from the Android backend API and manage state in the web UI.

**Why it's required:**
- Centralized API communication
- Error handling and retry logic
- Loading states for UX
- Type safety with TypeScript

**Implementation Details:**

1. **API Client**
   ```typescript
   // api/client.ts
   class ApiClient {
       private baseUrl: string = `http://${window.location.hostname}:8192`;
       
       async fetch<T>(
           endpoint: string,
           options?: RequestInit
       ): Promise<T> {
           try {
               const response = await fetch(`${this.baseUrl}${endpoint}`, {
                   headers: {
                       'Content-Type': 'application/json',
                       ...options?.headers,
                   },
                   ...options,
               });
               
               if (!response.ok) {
                   throw new Error(`API Error: ${response.status}`);
               }
               
               return await response.json();
           } catch (error) {
               console.error('API request failed:', error);
               throw error;
           }
       }
       
       getCards(page: number = 0, limit: number = 20) {
           return this.fetch<ApiResponse<PaginatedResponse<Card>>>(
               `/api/cards?page=${page}&limit=${limit}`
           );
       }
       
       getCard(id: string) {
           return this.fetch<ApiResponse<Card>>(`/api/cards/${id}`);
       }
       
       getSections() {
           return this.fetch<ApiResponse<Section[]>>('/api/sections');
       }
       
       search(query: string) {
           return this.fetch<ApiResponse<Card[]>>(
               `/api/search?q=${encodeURIComponent(query)}`
           );
       }
   }
   
   export const apiClient = new ApiClient();
   ```

2. **Custom Hooks**
   ```typescript
   // hooks/useCards.ts
   export function useCards(page: number = 0, limit: number = 20) {
       const [data, setData] = useState<PaginatedResponse<Card> | null>(null);
       const [loading, setLoading] = useState(true);
       const [error, setError] = useState<string | null>(null);
       
       useEffect(() => {
           setLoading(true);
           setError(null);
           
           apiClient.getCards(page, limit)
               .then((response) => {
                   if (response.success && response.data) {
                       setData(response.data);
                   } else {
                       setError(response.error || 'Unknown error');
                   }
               })
               .catch((err) => setError(err.message))
               .finally(() => setLoading(false));
       }, [page, limit]);
       
       return { data, loading, error };
   }
   
   export function useSections() {
       const [sections, setSections] = useState<Section[]>([]);
       const [loading, setLoading] = useState(true);
       
       useEffect(() => {
           apiClient.getSections()
               .then((response) => {
                   if (response.success && response.data) {
                       setSections(response.data);
                   }
               })
               .finally(() => setLoading(false));
       }, []);
       
       return { sections, loading };
   }
   
   export function useSearch(query: string) {
       const [results, setResults] = useState<Card[]>([]);
       const [loading, setLoading] = useState(false);
       
       useEffect(() => {
           if (!query.trim()) {
               setResults([]);
               return;
           }
           
           setLoading(true);
           apiClient.search(query)
               .then((response) => {
                   if (response.success && response.data) {
                       setResults(response.data);
                   }
               })
               .finally(() => setLoading(false));
       }, [query]);
       
       return { results, loading };
   }
   ```

3. **Error Boundaries & Fallbacks**
   ```typescript
   export function ErrorBoundary({ children }: { children: React.ReactNode }) {
       const [hasError, setHasError] = useState(false);
       
       if (hasError) {
           return (
               <div className="p-4 bg-red-50 text-red-800 rounded">
                   <h2 className="font-bold">Oops! Something went wrong</h2>
                   <p>Unable to load data. Please refresh the page.</p>
                   <button
                       onClick={() => window.location.reload()}
                       className="mt-2 px-4 py-2 bg-red-600 text-white rounded"
                   >
                       Refresh
                   </button>
               </div>
           );
       }
       
       return (
           <ErrorBoundaryWrapper onError={() => setHasError(true)}>
               {children}
           </ErrorBoundaryWrapper>
       );
   }
   ```

**Acceptance Criteria:**
- ✅ API calls work and return correct data
- ✅ Loading states display during fetch
- ✅ Errors are caught and displayed to user
- ✅ Pagination parameters work
- ✅ Search queries filter results correctly
- ✅ TypeScript types match API responses
- ✅ No console errors for normal operations
- ✅ Handles API timeouts gracefully

---

#### **TASK 2.3: Implement Search & Filtering**

**What to do:**
Add search functionality and filtering options to help users find specific cards/content on the web interface.

**Why it's required:**
- Desktop users expect search functionality
- Better discoverability of content
- Performance improvement (reduce scrolling for large datasets)

**Implementation Details:**

1. **Search Component**
   ```typescript
   // components/SearchBar.tsx
   export function SearchBar({
       onSearch,
       placeholder = "Search cards...",
   }: SearchBarProps) {
       const [query, setQuery] = useState("");
       const [debounce, setDebounce] = useState<NodeJS.Timeout | null>(null);
       
       const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
           const value = e.target.value;
           setQuery(value);
           
           // Debounce API calls
           if (debounce) clearTimeout(debounce);
           
           const timeout = setTimeout(() => {
               onSearch(value);
           }, 300);
           
           setDebounce(timeout);
       };
       
       return (
           <div className="relative mb-6">
               <input
                   type="text"
                   value={query}
                   onChange={handleChange}
                   placeholder={placeholder}
                   className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2"
               />
               {query && (
                   <button
                       onClick={() => {
                           setQuery("");
                           onSearch("");
                       }}
                       className="absolute right-3 top-2 text-gray-400"
                   >
                       ✕
                   </button>
               )}
           </div>
       );
   }
   ```

2. **Filter Sidebar**
   ```typescript
   // components/FilterPanel.tsx
   export function FilterPanel({ onFilterChange }: FilterPanelProps) {
       const [filters, setFilters] = useState({
           category: "",
           sortBy: "updated",
           dateRange: "all",
       });
       
       const handleFilterChange = (key: string, value: any) => {
           const newFilters = { ...filters, [key]: value };
           setFilters(newFilters);
           onFilterChange(newFilters);
       };
       
       return (
           <div className="bg-gray-50 p-4 rounded-lg">
               <h3 className="font-bold mb-4">Filters</h3>
               
               <div className="space-y-4">
                   <FilterOption
                       label="Sort By"
                       value={filters.sortBy}
                       options={[
                           { value: "updated", label: "Recently Updated" },
                           { value: "created", label: "Newest" },
                           { value: "title", label: "Title A-Z" },
                       ]}
                       onChange={(val) => handleFilterChange("sortBy", val)}
                   />
                   
                   <FilterOption
                       label="Date Range"
                       value={filters.dateRange}
                       options={[
                           { value: "all", label: "All Time" },
                           { value: "week", label: "Past Week" },
                           { value: "month", label: "Past Month" },
                       ]}
                       onChange={(val) => handleFilterChange("dateRange", val)}
                   />
               </div>
           </div>
       );
   }
   ```

3. **Search Results Page**
   ```typescript
   // pages/Search.tsx
   export function SearchPage() {
       const [searchQuery, setSearchQuery] = useState("");
       const { results, loading } = useSearch(searchQuery);
       
       return (
           <Layout>
               <div className="max-w-4xl mx-auto">
                   <h1 className="text-3xl font-bold mb-6">Search</h1>
                   
                   <SearchBar onSearch={setSearchQuery} />
                   
                   {searchQuery && (
                       <div className="mb-4 text-gray-600">
                           Found {results.length} results for "{searchQuery}"
                       </div>
                   )}
                   
                   {loading && <LoadingSpinner />}
                   {!loading && results.length === 0 && searchQuery && (
                       <EmptyState message="No results found" />
                   )}
                   {!loading && results.length > 0 && (
                       <CardGrid cards={results} isLoading={false} />
                   )}
               </div>
           </Layout>
       );
   }
   ```

**Acceptance Criteria:**
- ✅ Search is real-time with debouncing (300ms)
- ✅ Results update as user types
- ✅ Clear button clears search
- ✅ No results message when nothing matches
- ✅ Filter options work and affect results
- ✅ Sorting options reorder results
- ✅ Multiple filters can be combined
- ✅ Search is case-insensitive
- ✅ Results count displayed

---

### **PHASE 3: ANDROID UI & INTEGRATION**

---

#### **TASK 3.1: Create "Open in Web" Screen**

**What to do:**
Build a new Android screen (Composable) with a toggle to enable/disable web access, displays QR code, and shows the connection URL.

**Why it's required:**
- Users need a central place to manage web access
- Clear status indication (enabled/disabled)
- Easy access to connection details

**Implementation Details (Jetpack Compose):**

```kotlin
// ui/screens/OpenInWebScreen.kt

@Composable
fun OpenInWebScreen(viewModel: OpenInWebViewModel = hiltViewModel()) {
    val isEnabled by viewModel.isWebServerEnabled.collectAsState()
    val ipAddress by viewModel.ipAddress.collectAsState()
    val port by viewModel.port.collectAsState()
    val qrCode by viewModel.qrCodeBitmap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Open in Web") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Toggle Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Web Access",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (isEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) Color.Green else Color.Red
                        )
                    }
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.toggleWebServer(it) },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            // Error Display
            if (error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    backgroundColor = Color(0xFFFFEBEE)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Error", style = MaterialTheme.typography.titleSmall)
                        Text(
                            error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
            }
            
            // Connection Details
            if (isEnabled && ipAddress != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Connection Details",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // QR Code
                        if (qrCode != null) {
                            Image(
                                bitmap = qrCode!!.asImageBitmap(),
                                contentDescription = "Connection QR Code",
                                modifier = Modifier
                                    .size(300.dp)
                                    .padding(bottom = 16.dp)
                                    .border(2.dp, Color.Gray, RoundedCornerShape(8.dp))
                            )
                        } else if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(300.dp))
                        }
                        
                        // URL Display
                        val url = "http://$ipAddress:$port"
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF5F5F5)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    url,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = LocalContext.current
                                            .getSystemService(Context.CLIPBOARD_SERVICE)
                                            as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText("connection_url", url)
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        }
                        
                        // Instructions
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0F4FF)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "On your laptop/desktop:",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "• Scan the QR code with your phone's camera, OR\n" +
                                    "• Enter the URL in your web browser\n" +
                                    "• Make sure both devices are on the same WiFi network",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Disabled State Info
            if (!isEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    backgroundColor = Color(0xFFFFF3E0)
                ) {
                    Text(
                        "Enable the toggle above to start sharing your data on the web. " +
                        "Your device must be connected to WiFi.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
```

**ViewModel:**

```kotlin
// viewmodel/OpenInWebViewModel.kt

@HiltViewModel
class OpenInWebViewModel @Inject constructor(
    private val webServerManager: WebServerManager,
    private val context: Context
) : ViewModel() {
    
    private val _isWebServerEnabled = MutableStateFlow(false)
    val isWebServerEnabled = _isWebServerEnabled.asStateFlow()
    
    private val _ipAddress = MutableStateFlow<String?>(null)
    val ipAddress = _ipAddress.asStateFlow()
    
    private val _port = MutableStateFlow(8192)
    val port = _port.asStateFlow()
    
    private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
    val qrCodeBitmap = _qrCodeBitmap.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    init {
        observeWebServerState()
    }
    
    fun toggleWebServer(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                if (enabled) {
                    // Check WiFi connection
                    if (!NetworkUtils.isConnectedToWiFi(context)) {
                        _error.value = "Please connect to WiFi first"
                        _isLoading.value = false
                        return@launch
                    }
                    
                    webServerManager.startServer()
                    _ipAddress.value = NetworkUtils.getLocalIpAddress()
                    _port.value = 8192
                    
                    // Generate QR code
                    val url = "http://${_ipAddress.value}:${_port.value}"
                    _qrCodeBitmap.value = QRCodeGenerator.generateQRCode(url)
                    
                    _isWebServerEnabled.value = true
                } else {
                    webServerManager.stopServer()
                    _isWebServerEnabled.value = false
                    _qrCodeBitmap.value = null
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                _isWebServerEnabled.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun observeWebServerState() {
        viewModelScope.launch {
            webServerManager.isRunning.collect { running ->
                _isWebServerEnabled.value = running
            }
        }
    }
}
```

**Acceptance Criteria:**
- ✅ Toggle switch enables/disables web server
- ✅ QR code displays when server is enabled
- ✅ Connection URL shows IP and port
- ✅ Copy button copies URL to clipboard
- ✅ Error message shows if WiFi not connected
- ✅ Loading indicator during server startup
- ✅ Instructions are clear for users
- ✅ Design is clean and matches app theme
- ✅ Responsive on different screen sizes
- ✅ No crashes on toggle

---

#### **TASK 3.2: Create Foreground Service for Web Server**

**What to do:**
Implement a foreground service that keeps the HTTP server running even when the app goes to background.

**Why it's required:**
- Users need web access even when app isn't in foreground
- Foreground service required to keep HTTP server alive
- Shows notification so users know service is active
- Can be stopped from notification

**Implementation Details:**

```kotlin
// services/WebServerService.kt

@HiltAndroidApp
class WebServerService : Service() {
    @Inject
    lateinit var webServerManager: WebServerManager
    
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "web_server"
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVER -> startWebServer()
            ACTION_STOP_SERVER -> stopWebServer()
        }
        return START_STICKY
    }
    
    private fun startWebServer() {
        try {
            webServerManager.startServer()
            
            val ipAddress = NetworkUtils.getLocalIpAddress()
            val port = 8192
            val url = "http://$ipAddress:$port"
            
            val notification = createNotification(url)
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("WebServerService", "Failed to start server", e)
            stopSelf()
        }
    }
    
    private fun stopWebServer() {
        webServerManager.stopServer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotification(url: String): Notification {
        val stopIntent = Intent(this, WebServerService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Web Access Active")
            .setContentText(url)
            .setSmallIcon(R.drawable.ic_web)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Web Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when web access is active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        const val ACTION_START_SERVER = "com.example.ACTION_START_SERVER"
        const val ACTION_STOP_SERVER = "com.example.ACTION_STOP_SERVER"
    }
}
```

**AndroidManifest.xml:**

```xml
<service
    android:name=".services.WebServerService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="dataSync" />

<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

**Acceptance Criteria:**
- ✅ Service starts when toggle is enabled
- ✅ Service stops when toggle is disabled
- ✅ Notification shows while service is active
- ✅ Notification shows correct URL
- ✅ Stop button in notification works
- ✅ Service survives app backgrounding
- ✅ No crash if service is killed
- ✅ Foreground service properly declared

---

### **PHASE 4: SECURITY & OPTIMIZATION**

---

#### **TASK 4.1: Implement Basic Security**

**What to do:**
Add security measures to protect user data on the local network.

**Why it's required:**
- Data should not be accessible to unauthorized devices
- Even on local networks, basic auth is good practice
- Prevents accidental exposure on public WiFi

**Implementation Details:**

```kotlin
// security/AuthenticationManager.kt

object AuthenticationManager {
    fun generateAccessCode(): String {
        return (1000..9999).random().toString() // 4-digit code
    }
    
    fun hashCode(code: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(code.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

// Add to Ktor routing
fun Application.installAuth() {
    val accessCode = AuthenticationManager.generateAccessCode()
    val hashedCode = AuthenticationManager.hashCode(accessCode)
    
    install(Authentication) {
        bearer("access-token") {
            authenticate { tokenCredential ->
                if (tokenCredential.token == accessCode) {
                    UserIdPrincipal("user")
                } else {
                    null
                }
            }
        }
    }
    
    routing {
        get("/api/access-code-required") {
            call.respond(mapOf("required" to true))
        }
        
        post("/api/authenticate") {
            val request = call.receive<AuthRequest>()
            val hash = AuthenticationManager.hashCode(request.code)
            
            if (hash == hashedCode) {
                call.respond(mapOf(
                    "success" to true,
                    "token" to accessCode
                ))
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("success" to false)
                )
            }
        }
        
        // Protected endpoints
        authenticate("access-token") {
            get("/api/cards") {
                // Normal card listing
            }
        }
    }
}
```

**Web UI Login:**

```typescript
// pages/Login.tsx
export function LoginPage({ onAuth }: LoginPageProps) {
    const [code, setCode] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    
    const handleSubmit = async () => {
        setLoading(true);
        setError("");
        
        try {
            const response = await fetch(
                `http://${window.location.hostname}:8192/api/authenticate`,
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ code }),
                }
            );
            
            const data = await response.json();
            if (data.success) {
                localStorage.setItem("auth_token", data.token);
                onAuth(data.token);
            } else {
                setError("Invalid code");
            }
        } catch (err) {
            setError("Connection error");
        } finally {
            setLoading(false);
        }
    };
    
    return (
        <div className="flex items-center justify-center h-screen">
            <div className="p-8 bg-white rounded-lg shadow w-96">
                <h1 className="text-2xl font-bold mb-4">Enter Access Code</h1>
                <p className="text-gray-600 mb-6">
                    Check your phone for the 4-digit access code
                </p>
                
                <input
                    type="text"
                    maxLength={4}
                    value={code}
                    onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
                    placeholder="0000"
                    className="w-full px-4 py-2 border-2 text-center text-2xl tracking-widest"
                />
                
                {error && <p className="text-red-600 mt-2">{error}</p>}
                
                <button
                    onClick={handleSubmit}
                    disabled={code.length !== 4 || loading}
                    className="w-full mt-4 py-2 bg-blue-600 text-white rounded disabled:opacity-50"
                >
                    {loading ? "Verifying..." : "Verify"}
                </button>
            </div>
        </div>
    );
}
```

**Acceptance Criteria:**
- ✅ 4-digit code displayed on phone when server starts
- ✅ Web interface requires code before showing data
- ✅ Code changes each time server restarts
- ✅ Wrong code rejected with error
- ✅ Token stored in localStorage after auth
- ✅ API endpoints protected with token check
- ✅ No sensitive data leaks in error messages
- ✅ HTTPS optional for additional security

---

#### **TASK 4.2: Performance Optimization**

**What to do:**
Optimize server and web UI for fast loading and smooth interaction over WiFi.

**Why it's required:**
- WiFi speeds vary (and may not be fast)
- User experience depends on responsiveness
- Large datasets need pagination
- Images need optimization

**Implementation Details:**

1. **Server-side Caching:**
   ```kotlin
   install(CachingHeaders) {
       options { outgoingContent ->
           when (outgoingContent.contentType?.withoutParameters()) {
               ContentType.Application.Json -> CachingOptions(
                   cacheControl = CacheControl.MaxAge(maxAgeSeconds = 60)
               )
               ContentType.Image.Any -> CachingOptions(
                   cacheControl = CacheControl.MaxAge(maxAgeSeconds = 3600)
               )
               else -> null
           }
       }
   }
   ```

2. **Image Optimization:**
   - Serve thumbnail versions for grids
   - Lazy load images in web UI
   - Compress images on Android before serving

3. **Web UI Optimization:**
   ```typescript
   // Lazy load images
   <img
       src={placeholder}
       data-src={actualSrc}
       onVisible={() => loadImage()}
       className="lazy"
   />
   
   // Virtualize long lists
   import { FixedSizeList } from 'react-window';
   
   <FixedSizeList
       height={600}
       itemCount={cards.length}
       itemSize={100}
   >
       {({ index, style }) => (
           <CardGridItem
               card={cards[index]}
               style={style}
           />
       )}
   </FixedSizeList>
   ```

4. **API Response Compression:**
   ```kotlin
   install(Compression) {
       gzip {
           priority = 1.0
       }
       deflate {
           priority = 0.9
       }
   }
   ```

**Acceptance Criteria:**
- ✅ First page load <2 seconds on good WiFi
- ✅ Cards load in grid <500ms
- ✅ Images load lazily and show placeholder
- ✅ Pagination works for large datasets
- ✅ No full page refresh on navigation
- ✅ Smooth scroll on card lists
- ✅ API responses gzipped
- ✅ Cache headers set correctly

---

## **4. BUILD & DEPLOYMENT CHECKLIST**

### **Pre-Launch Tests**

- [ ] **Server Tests**
  - [ ] Start/stop server multiple times
  - [ ] IP address detection on different networks
  - [ ] Port conflicts handled
  - [ ] Graceful shutdown
  - [ ] Memory leaks on long-running server

- [ ] **Web UI Tests**
  - [ ] All pages load without errors
  - [ ] Navigation works on desktop (1920x1080)
  - [ ] Navigation works on tablet (1024x768)
  - [ ] Mobile view works (as fallback)
  - [ ] Search functionality works
  - [ ] Cards display correctly
  - [ ] Images load properly
  - [ ] Pagination works

- [ ] **Network Tests**
  - [ ] Connect phone & laptop on same WiFi
  - [ ] Can access web UI from laptop
  - [ ] WiFi disconnect handled gracefully
  - [ ] IP change detected (WiFi switch)
  - [ ] Works on 2.4GHz and 5GHz networks

- [ ] **Security Tests**
  - [ ] Access code blocks unauthorized access
  - [ ] Token expires properly
  - [ ] No data exposed in logs
  - [ ] SSL/TLS working (if enabled)

### **Release Checklist**

- [ ] Code reviewed
- [ ] Tests passed (>80% coverage)
- [ ] Documentation updated
- [ ] User guide written
- [ ] API documentation generated
- [ ] Privacy policy updated
- [ ] No crashes on real devices
- [ ] Performance acceptable

---

## **5. TECH STACK SUMMARY**

| Layer | Technology | Version | Reason |
|-------|-----------|---------|--------|
| **Android Server** | Ktor CIO | 2.3.12+ | Lightweight, coroutine-native |
| **QR Code (Android)** | qrcode-kotlin-android | 4.5.0 | Pure Kotlin, no dependencies |
| **Web Frontend** | React + TypeScript | Latest | Type-safe, component-based |
| **Styling** | Tailwind CSS | Latest | Utility-first, responsive |
| **State Management** | React Hooks | Built-in | No external lib needed |
| **Build Tool** | Vite | Latest | Fast, modern |
| **Database** | Room/SQLite | Built-in | Already in your app |
| **Encryption** | TLS + Custom Auth | Standard | XChaCha20-Poly1305 optional |

---

## **6. ESTIMATED EFFORT**

| Phase | Tasks | Est. Hours | Complexity |
|-------|-------|-----------|-----------|
| **Phase 1** | Backend Setup | 20-24 | Medium |
| **Phase 2** | Web UI | 30-40 | Medium-High |
| **Phase 3** | Android UI | 12-16 | Medium |
| **Phase 4** | Security & Optimization | 16-20 | High |
| **Total** | All | **78-100 hours** | **Medium-High** |

**Timeline:** 2-3 weeks for 1 developer, 1 week for 2 developers

---

## **7. NEXT STEPS**

1. **Validate Requirements** - Confirm feature scope with stakeholders
2. **Setup Development Environment** - Create separate branches
3. **Start Phase 1** - Begin with server setup
4. **Iterate & Test** - Test frequently on real devices
5. **Security Review** - Have code reviewed for security
6. **Beta Testing** - Test with select users before release

---

This is a comprehensive, production-ready feature plan. Each task includes clear acceptance criteria so you can verify completion. Would you like deeper guidance on any specific task or starter code templates for any phase?
