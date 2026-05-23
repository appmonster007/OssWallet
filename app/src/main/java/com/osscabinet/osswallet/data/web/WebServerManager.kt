package com.osscabinet.ossWallet.data.web

import android.content.Context
import android.graphics.Bitmap
import com.osscabinet.ossWallet.BuildConfig
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.model.web.WebServerState
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.CurrencyRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.repository.WebServerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val webServerPreferences: WebServerPreferences,
    private val budgetRepository: BudgetRepository,
    private val loanRepository: LoanRepository,
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val tagRepository: TagRepository,
    private val currencyRepository: CurrencyRepository,
    private val filterRepository: FilterRepository,
    private val cardRepository: CardRepository,
    private val cardComputerRegistry: CardComputerRegistry,
    private val templateRepository: TemplateRepository
) : WebServerRepository {

    private val _serverState = MutableStateFlow<WebServerState>(WebServerState.Stopped)
    override val serverState: StateFlow<WebServerState> = _serverState.asStateFlow()
    override val isEnabled: Flow<Boolean> = webServerPreferences.isEnabled

    private var engine: EmbeddedServer<*, *>? = null
    private var serverScope: CoroutineScope? = null
    private val authSessionManager = AuthSessionManager()

    override fun isRunning(): Boolean = _serverState.value is WebServerState.Running

    override fun getAccessUrl(): String? =
        (_serverState.value as? WebServerState.Running)?.accessUrl

    override fun getAccessCode(): String? =
        (_serverState.value as? WebServerState.Running)?.accessCode

    override suspend fun setEnabled(enabled: Boolean) = webServerPreferences.setEnabled(enabled)

    override fun generateQrCode(url: String): Bitmap? =
        runCatching { QRCodeGenerator.generate(url) }.getOrNull()

    override fun isConnectedToWifi(): Boolean = NetworkUtils.isConnectedToWifi(context)

    override suspend fun start() {
        if (isRunning()) return
        _serverState.value = WebServerState.Starting

        val ip = NetworkUtils.getWifiInterfaceIp(context)
            ?: NetworkUtils.getAnyAvailableIp()
            ?: "127.0.0.1"

        authSessionManager.initialize()
        val accessCode = authSessionManager.getAccessCode()

        val routes = WebApiRoutes(
            context = context,
            authSessionManager = authSessionManager,
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            settingsRepository = settingsRepository,
            budgetRepository = budgetRepository,
            loanRepository = loanRepository,
            plannedPaymentRepository = plannedPaymentRepository,
            tagRepository = tagRepository,
            currencyRepository = currencyRepository,
            filterRepository = filterRepository,
            cardRepository = cardRepository,
            cardComputerRegistry = cardComputerRegistry,
            templateRepository = templateRepository
        )

        try {
            val serverJson = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
            engine = embeddedServer(CIO, host = ip, port = SERVER_PORT) {
                install(ContentNegotiation) { json(serverJson) }
                install(Compression) { gzip() }
                install(StatusPages) {
                    exception<Throwable> { call, _ ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("success" to false, "error" to "Internal server error")
                        )
                    }
                }
                install(DefaultHeaders) {
                    header(
                        "Content-Security-Policy",
                        "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'"
                    )
                    header("X-Content-Type-Options", "nosniff")
                    header("X-Frame-Options", "DENY")
                    header("Referrer-Policy", "no-referrer")
                }
                if (BuildConfig.DEBUG) {
                    install(io.ktor.server.plugins.calllogging.CallLogging)
                }
                with(routes) { installRoutes() }
            }.start(wait = false)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            serverScope = scope

            scope.launch {
                while (isActive) {
                    delay(5 * 60 * 1000L)
                    authSessionManager.purgeExpiredSessions()
                }
            }

            _serverState.value = WebServerState.Running(
                ip = ip,
                port = SERVER_PORT,
                accessCode = accessCode
            )
        } catch (e: Exception) {
            _serverState.value = WebServerState.Error(e.message ?: "Failed to start server")
            engine = null
        }
    }

    override suspend fun stop() {
        authSessionManager.invalidateSession()
        engine?.stop(gracePeriodMillis = 500, timeoutMillis = 2000)
        engine = null
        serverScope?.cancel()
        serverScope = null
        _serverState.value = WebServerState.Stopped
    }

    companion object {
        const val SERVER_PORT = 8192
    }
}
