# Code Standards — WaveHouse Android

> **Phiên bản:** 1.0.0 | **Cập nhật:** 2026-04-17 | **Kotlin** · **Jetpack Compose** · **MVVM + Clean**

## Documentation Maintenance
**Last Updated:** 2026-04-17  
**Document Version:** 1.0  
**Maintained By:** Development Team

---

## 1. Naming Conventions

### 1.1 Files & Classes
| Type | Convention | Ví dụ |
|------|-----------|-------|
| Activity | `XxxActivity` | `MainActivity` |
| Fragment | — (không dùng) | — |
| Composable Screen | `XxxScreen` | `ProductListScreen` |
| Composable Component | `XxxCard`, `XxxItem` | `ProductCard`, `StockBadge` |
| ViewModel | `XxxViewModel` | `ProductViewModel` |
| UseCase | `XxxUseCase` | `GetProductsUseCase` |
| Repository (interface) | `XxxRepository` | `ProductRepository` |
| Repository (impl) | `XxxRepositoryImpl` | `ProductRepositoryImpl` |
| Data Source | `XxxDataSource` | `ProductRemoteDataSource` |
| Room Entity | `XxxEntity` | `ProductEntity` |
| Room DAO | `XxxDao` | `ProductDao` |
| DTO (API) | `XxxDto` | `ProductDto` |
| Domain Model | `Xxx` | `Product`, `StockEntry` |
| Hilt Module | `XxxModule` | `NetworkModule` |
| UI State | `XxxUiState` | `ProductUiState` |

### 1.2 Variables & Functions
```kotlin
// ✅ camelCase cho variables
val productList: List<Product> = emptyList()
var isLoading: Boolean = false

// ✅ SCREAMING_SNAKE_CASE cho constants
const val DEFAULT_PAGE_SIZE = 20
const val DB_PRODUCTS = "products"

// ✅ Prefix 'is/has/can' cho Boolean
val isLoggedIn: Boolean
val hasPermission: Boolean

// ✅ Prefix '_' cho backing StateFlow
private val _uiState = MutableStateFlow(ProductUiState())
val uiState = _uiState.asStateFlow()
```

---

## 2. Architecture Rules

### 2.1 Layer Dependencies
```
UI → ViewModel → UseCase → Repository Interface → Data Sources
         ↑ No Android framework in Domain/Data (except Room/Retrofit annotations)
```

### 2.2 ViewModel Rules
```kotlin
// ✅ Dùng StateFlow cho UI state
class ProductViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState = _uiState.asStateFlow()

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getProductsUseCase().collect { result ->
                when (result) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(products = result.data, isLoading = false)
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(error = result.message, isLoading = false)
                    }
                }
            }
        }
    }
}

// ❌ Không inject Context vào ViewModel
// ❌ Không gọi Database/API trực tiếp từ ViewModel
```

### 2.3 UseCase Rules
```kotlin
// ✅ Một UseCase = một hành động
class GetProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    operator fun invoke(
        warehouseId: String,
        page: Int = 0
    ): Flow<ApiResult<List<Product>>> = repository.getProducts(warehouseId, page)
}

// ❌ Không đặt multiple business logic trong một UseCase
```

### 2.4 Repository Rules
```kotlin
// ✅ Interface trong Domain layer
interface ProductRepository {
    fun getProducts(warehouseId: String, page: Int): Flow<ApiResult<List<Product>>>
    suspend fun getProductById(id: String): ApiResult<Product>
    suspend fun createProduct(product: Product): ApiResult<Unit>
    suspend fun updateProduct(product: Product): ApiResult<Unit>
    suspend fun deleteProduct(id: String): ApiResult<Unit>
}

// ✅ Implementation trong Data layer
class ProductRepositoryImpl @Inject constructor(
    private val remoteSource: ProductRemoteDataSource,
    private val localDao: ProductDao
) : ProductRepository { ... }
```

---

## 3. Compose UI Standards

### 3.1 Screen Structure
```kotlin
@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProductListContent(
        uiState = uiState,
        onProductClick = onNavigateToDetail,
        onRetry = viewModel::loadProducts
    )
}

// ✅ Tách Content composable để preview được
@Composable
private fun ProductListContent(
    uiState: ProductListUiState,
    onProductClick: (String) -> Unit,
    onRetry: () -> Unit
) { ... }

@Preview
@Composable
private fun ProductListContentPreview() {
    ProductListContent(
        uiState = ProductListUiState(products = previewProducts),
        onProductClick = {},
        onRetry = {}
    )
}
```

### 3.2 UI State Pattern
```kotlin
data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false
)
```

---

## 4. Error Handling

```kotlin
// Sealed class cho API results
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val cause: Throwable? = null
    ) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}

// Extension để map results
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> this
    is ApiResult.Loading -> ApiResult.Loading
}
```

---

## 5. Coroutines & Flow

```kotlin
// ✅ Dùng callbackFlow cho Firebase Database listeners
fun getProductsFlow(warehouseId: String): Flow<ApiResult<List<Product>>> = callbackFlow {
    trySend(ApiResult.Loading)
    val listener = database.getReference("products")
        .orderByChild("warehouseId").equalTo(warehouseId)
        .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = snapshot.children.mapNotNull { it.getValue(ProductDto::class.java)?.toProduct() }
                trySend(ApiResult.Success(products))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ApiResult.Error(error.message))
            }
        })
    awaitClose { database.getReference("products").removeEventListener(listener) }
}

// ✅ Dùng withContext(Dispatchers.IO) cho suspend calls
suspend fun createProduct(product: Product): ApiResult<Unit> = withContext(Dispatchers.IO) {
    try {
        val ref = database.getReference("products").push()
        ref.setValue(product.toDto()).await()
        ApiResult.Success(Unit)
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Failed to create product")
    }
}
```

---

## 6. Dependency Injection (Hilt)

```kotlin
// Module example
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase =
        FirebaseDatabase.getInstance().also {
            it.setPersistenceEnabled(true) // Offline support
        }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}

// ✅ Constructor injection preferred
class ProductRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val productDao: ProductDao
) : ProductRepository
```

---

## 7. Testing Standards

```kotlin
// ViewModel test với MockK + Turbine
@Test
fun `loadProducts emits success state`() = runTest {
    val fakeProducts = listOf(Product(...))
    coEvery { getProductsUseCase(any()) } returns flowOf(ApiResult.Success(fakeProducts))

    viewModel.loadProducts("warehouse1")

    viewModel.uiState.test {
        val state = awaitItem()
        assertThat(state.products).isEqualTo(fakeProducts)
        assertThat(state.isLoading).isFalse()
    }
}
```

---

## 8. Git Commit Convention

```
feat: ✨ thêm tính năng quét barcode sản phẩm
fix: 🐛 sửa lỗi tính tồn kho khi xuất kho
docs: 📝 cập nhật README
style: 💄 điều chỉnh UI ProductCard
refactor: ♻️ tách StockRepository theo feature
test: ✅ thêm unit test GetProductsUseCase
chore: 🔧 cập nhật dependencies Gradle
```

---

## 9. Điều Cấm Tuyệt Đối

- ❌ Hardcode credentials/API keys trong source code
- ❌ Gọi Database trực tiếp từ Composable hoặc ViewModel
- ❌ `runBlocking` trong production code
- ❌ Ignore exception mà không log
- ❌ `var` trong data class domain model (dùng `val` + `copy()`)
- ❌ God ViewModel (> 300 lines → tách UseCase)
