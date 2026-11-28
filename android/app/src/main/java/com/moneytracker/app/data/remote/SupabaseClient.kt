package com.moneytracker.app.data.remote

import com.moneytracker.app.BuildConfig
import com.moneytracker.app.data.local.SessionStore
import com.moneytracker.app.data.model.AuthSession
import com.moneytracker.app.data.model.AuthUser
import com.moneytracker.app.data.model.Category
import com.moneytracker.app.data.model.CategoryInsert
import com.moneytracker.app.data.model.Expense
import com.moneytracker.app.data.model.ExpenseInsert
import com.moneytracker.app.data.model.ExpenseUpdate
import com.moneytracker.app.data.model.Income
import com.moneytracker.app.data.model.IncomeInsert
import com.moneytracker.app.data.model.IncomeUpdate
import com.moneytracker.app.data.model.PaymentMethod
import com.moneytracker.app.data.model.PaymentMethodInsert
import com.moneytracker.app.data.model.RecurringInsert
import com.moneytracker.app.data.model.RecurringTransaction
import com.moneytracker.app.data.model.RecurringUpdate
import com.moneytracker.app.data.model.SignUpResult
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.data.model.WalletInsert
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class SupabaseException(message: String, val statusCode: Int? = null) : Exception(message)

class UnauthorizedException(message: String = "Session expired. Please sign in again.") :
    Exception(message)

@OptIn(ExperimentalSerializationApi::class)
class SupabaseClient(
    private val sessionStore: SessionStore,
    private val baseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/'),
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY
) {
    private val refreshMutex = Mutex()

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val explicitNullJson: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = true
    }

    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.NONE
        }
        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }
    }

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() &&
            !baseUrl.contains("placeholder.supabase.co") &&
            anonKey.isNotBlank() &&
            anonKey != "placeholder-key"

    // --- Auth (GoTrue) ---

    @Serializable
    private data class CredentialsPayload(
        val email: String,
        val password: String
    )

    @Serializable
    private data class RefreshPayload(
        @SerialName("refresh_token") val refreshToken: String
    )

    @Serializable
    private data class SignUpRawResponse(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long? = null,
        @SerialName("expires_at") val expiresAt: Long? = null,
        val user: AuthUser? = null,
        val id: String? = null,
        val email: String? = null
    )

    suspend fun signIn(email: String = OWNER_EMAIL, password: String): AuthSession {
        if (!isAllowedOwnerEmail(email)) {
            throw SupabaseException("This app is locked to $OWNER_EMAIL.", 403)
        }
        val response = httpClient.post("$baseUrl/auth/v1/token") {
            parameter("grant_type", "password")
            header("apikey", anonKey)
            contentType(ContentType.Application.Json)
            setBody(CredentialsPayload(OWNER_EMAIL, password))
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw SupabaseException(extractErrorMessage(bodyText, "Sign in failed"), response.status.value)
        }
        val parsed = json.decodeFromString<AuthSession>(bodyText)
        val normalized = normalizeExpiry(parsed)
        sessionStore.saveSession(normalized)
        return normalized
    }

    suspend fun signUp(email: String, password: String): SignUpResult {
        val response = httpClient.post("$baseUrl/auth/v1/signup") {
            header("apikey", anonKey)
            contentType(ContentType.Application.Json)
            setBody(CredentialsPayload(email.trim(), password))
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw SupabaseException(extractErrorMessage(bodyText, "Sign up failed"), response.status.value)
        }
        val raw = json.decodeFromString<SignUpRawResponse>(bodyText)
        if (!raw.accessToken.isNullOrBlank() && !raw.refreshToken.isNullOrBlank() && raw.user != null) {
            val session = normalizeExpiry(
                AuthSession(
                    accessToken = raw.accessToken,
                    refreshToken = raw.refreshToken,
                    expiresIn = raw.expiresIn ?: 3600L,
                    expiresAtEpochSec = raw.expiresAt ?: 0L,
                    user = raw.user
                )
            )
            sessionStore.saveSession(session)
            return SignUpResult(session = session, requiresEmailConfirmation = false)
        }
        return SignUpResult(session = null, requiresEmailConfirmation = true)
    }

    suspend fun refreshSession(refreshToken: String): AuthSession {
        val response = httpClient.post("$baseUrl/auth/v1/token") {
            parameter("grant_type", "refresh_token")
            header("apikey", anonKey)
            contentType(ContentType.Application.Json)
            setBody(RefreshPayload(refreshToken))
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            if (shouldClearSessionOnRefreshFailure(response.status.value, bodyText)) {
                sessionStore.clearSession()
                throw UnauthorizedException(
                    extractErrorMessage(bodyText, "Session expired. Please sign in again.")
                )
            }
            throw SupabaseException(
                extractErrorMessage(bodyText, "Temporary session refresh error"),
                response.status.value
            )
        }
        val parsed = json.decodeFromString<AuthSession>(bodyText)
        val normalized = normalizeExpiry(parsed)
        sessionStore.saveSession(normalized)
        return normalized
    }

    suspend fun signOut() {
        val token = sessionStore.getSession()?.accessToken
        if (!token.isNullOrBlank()) {
            runCatching {
                httpClient.post("$baseUrl/auth/v1/logout") {
                    header("apikey", anonKey)
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
        sessionStore.clearSession()
    }

    private fun normalizeExpiry(session: AuthSession): AuthSession {
        val nowSec = System.currentTimeMillis() / 1000L
        val computedExpiresAt = if (session.expiresAtEpochSec > 0L) {
            session.expiresAtEpochSec
        } else {
            nowSec + session.expiresIn
        }
        return session.copy(expiresAtEpochSec = computedExpiresAt)
    }

    private suspend fun getValidSession(): AuthSession {
        val current = sessionStore.getSession() ?: throw UnauthorizedException()
        val nowSec = System.currentTimeMillis() / 1000L
        if (current.expiresAtEpochSec > 0L && nowSec >= current.expiresAtEpochSec - 60L) {
            return refreshWithLock(current.refreshToken, current.accessToken)
        }
        return current
    }

    private suspend fun refreshWithLock(refreshToken: String, failedAccessToken: String): AuthSession {
        return refreshMutex.withLock {
            val latest = sessionStore.getSession()
            if (latest != null && latest.accessToken != failedAccessToken) {
                return@withLock latest
            }
            val tokenToUse = latest?.refreshToken?.takeIf { it.isNotBlank() } ?: refreshToken
            refreshSession(tokenToUse)
        }
    }

    private suspend fun executeAuthenticated(
        block: suspend (AuthSession) -> HttpResponse
    ): Pair<HttpResponse, String> {
        val initialSession = getValidSession()
        val firstResp = block(initialSession)
        if (firstResp.status != HttpStatusCode.Unauthorized) {
            return firstResp to firstResp.bodyAsText()
        }
        // Retry once after token refresh
        val refreshed = refreshWithLock(initialSession.refreshToken, initialSession.accessToken)
        val retryResp = block(refreshed)
        return retryResp to retryResp.bodyAsText()
    }

    private fun HttpRequestBuilder.applySupabaseAuth(session: AuthSession, prefer: String? = null) {
        header("apikey", anonKey)
        header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
        if (prefer != null) {
            header("Prefer", prefer)
        }
    }

    private inline fun <reified T> decodeOrThrow(
        response: HttpResponse,
        bodyText: String,
        fallbackError: String
    ): T {
        if (!response.status.isSuccess()) {
            throw SupabaseException(extractErrorMessage(bodyText, fallbackError), response.status.value)
        }
        return json.decodeFromString(bodyText)
    }

    private fun verifySuccess(response: HttpResponse, bodyText: String, fallbackError: String) {
        if (!response.status.isSuccess()) {
            throw SupabaseException(extractErrorMessage(bodyText, fallbackError), response.status.value)
        }
    }

    private fun extractErrorMessage(bodyText: String, fallback: String): String {
        val parsed = runCatching { json.decodeFromString<JsonObject>(bodyText) }.getOrNull()
        val msg = (parsed?.get("error_description") as? JsonPrimitive)?.contentOrNull
            ?: (parsed?.get("msg") as? JsonPrimitive)?.contentOrNull
            ?: (parsed?.get("message") as? JsonPrimitive)?.contentOrNull
            ?: (parsed?.get("error") as? JsonPrimitive)?.contentOrNull
        return msg?.takeIf { it.isNotBlank() } ?: fallback
    }

    // --- Wallets ---

    suspend fun getWallets(userId: String): List<Wallet> {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.get("$baseUrl/rest/v1/wallets") {
                applySupabaseAuth(session)
                parameter("select", "*")
                parameter("user_id", "eq.$userId")
                parameter("order", "sort_order.asc")
            }
        }
        return decodeOrThrow(resp, text, "Failed to load wallets")
    }

    suspend fun insertWallet(payload: WalletInsert): Wallet {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.post("$baseUrl/rest/v1/wallets") {
                applySupabaseAuth(session, prefer = "return=representation")
                contentType(ContentType.Application.Json)
                setBody(listOf(payload))
            }
        }
        val list: List<Wallet> = decodeOrThrow(resp, text, "Failed to add wallet")
        return list.firstOrNull() ?: throw SupabaseException("Wallet insert returned empty response")
    }

    suspend fun deleteWallet(id: String, userId: String) {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.delete("$baseUrl/rest/v1/wallets") {
                applySupabaseAuth(session)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
            }
        }
        verifySuccess(resp, text, "Failed to delete wallet")
    }

    // --- Categories ---

    suspend fun getCategories(userId: String): List<Category> {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.get("$baseUrl/rest/v1/categories") {
                applySupabaseAuth(session)
                parameter("select", "*")
                parameter("user_id", "eq.$userId")
                parameter("order", "sort_order.asc")
            }
        }
        return decodeOrThrow(resp, text, "Failed to load categories")
    }

    suspend fun insertCategory(payload: CategoryInsert): Category {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.post("$baseUrl/rest/v1/categories") {
                applySupabaseAuth(session, prefer = "return=representation")
                contentType(ContentType.Application.Json)
                setBody(listOf(payload))
            }
        }
        val list: List<Category> = decodeOrThrow(resp, text, "Failed to add category")
        return list.firstOrNull() ?: throw SupabaseException("Category insert returned empty response")
    }

    suspend fun deleteCategory(id: String, userId: String) {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.delete("$baseUrl/rest/v1/categories") {
                applySupabaseAuth(session)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
            }
        }
        verifySuccess(resp, text, "Failed to delete category")
    }

    // --- Payment Methods ---

    suspend fun getPaymentMethods(userId: String): List<PaymentMethod> {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.get("$baseUrl/rest/v1/payment_methods") {
                applySupabaseAuth(session)
                parameter("select", "*")
                parameter("user_id", "eq.$userId")
                parameter("order", "sort_order.asc")
            }
        }
        return decodeOrThrow(resp, text, "Failed to load payment methods")
    }

    suspend fun insertPaymentMethod(payload: PaymentMethodInsert): PaymentMethod {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.post("$baseUrl/rest/v1/payment_methods") {
                applySupabaseAuth(session, prefer = "return=representation")
                contentType(ContentType.Application.Json)
                setBody(listOf(payload))
            }
        }
        val list: List<PaymentMethod> = decodeOrThrow(resp, text, "Failed to add payment method")
        return list.firstOrNull() ?: throw SupabaseException("Payment method insert returned empty response")
    }

    suspend fun updatePaymentMethodSortOrder(id: String, userId: String, sortOrder: Int) {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.patch("$baseUrl/rest/v1/payment_methods") {
                applySupabaseAuth(session)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
                contentType(ContentType.Application.Json)
                setBody("""{"sort_order":$sortOrder}""")
            }
        }
        verifySuccess(resp, text, "Failed to reorder payment method")
    }

    suspend fun deletePaymentMethod(id: String, userId: String) {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.delete("$baseUrl/rest/v1/payment_methods") {
                applySupabaseAuth(session)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
            }
        }
        verifySuccess(resp, text, "Failed to delete payment method")
    }

    // --- Expenses ---

    suspend fun getExpenses(
        userId: String,
        startDate: String? = null,
        endDate: String? = null
    ): List<Expense> {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.get("$baseUrl/rest/v1/expenses") {
                applySupabaseAuth(session)
                parameter("select", EXPENSE_SELECT)
                parameter("user_id", "eq.$userId")
                if (!startDate.isNullOrBlank()) {
                    parameter("expense_date", "gte.$startDate")
                }
                if (!endDate.isNullOrBlank()) {
                    parameter("expense_date", "lte.$endDate")
                }
                parameter("order", "expense_date.desc,created_at.desc")
            }
        }
        return decodeOrThrow(resp, text, "Failed to load expenses")
    }

    suspend fun insertExpenses(payloads: List<ExpenseInsert>): List<Expense> {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.post("$baseUrl/rest/v1/expenses") {
                applySupabaseAuth(session, prefer = "return=representation")
                parameter("select", EXPENSE_SELECT)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(payloads))
            }
        }
        return decodeOrThrow(resp, text, "Failed to add expense")
    }

    suspend fun updateExpense(id: String, userId: String, payload: ExpenseUpdate): Expense {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.patch("$baseUrl/rest/v1/expenses") {
                applySupabaseAuth(session, prefer = "return=representation")
                parameter("select", EXPENSE_SELECT)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
                contentType(ContentType.Application.Json)
                setBody(explicitNullJson.encodeToString(payload))
            }
        }
        val list: List<Expense> = decodeOrThrow(resp, text, "Failed to update expense")
        return list.firstOrNull() ?: throw SupabaseException("Expense update returned empty response")
    }

    suspend fun deleteExpense(id: String, userId: String) {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.delete("$baseUrl/rest/v1/expenses") {
                applySupabaseAuth(session)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
            }
        }
        verifySuccess(resp, text, "Failed to delete expense")
    }

    suspend fun deleteExpensesByIds(ids: List<String>, userId: String) {
        if (ids.isEmpty()) return
        val inFilter = "in.(${ids.joinToString(",")})"
        val (resp, text) = executeAuthenticated { session ->
            httpClient.delete("$baseUrl/rest/v1/expenses") {
                applySupabaseAuth(session)
                parameter("id", inFilter)
                parameter("user_id", "eq.$userId")
            }
        }
        verifySuccess(resp, text, "Failed to delete duplicate expenses")
    }

    // --- Income ---

    suspend fun getIncome(
        userId: String,
        startDate: String? = null,
        endDate: String? = null
    ): List<Income> {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.get("$baseUrl/rest/v1/income") {
                applySupabaseAuth(session)
                parameter("select", INCOME_SELECT)
                parameter("user_id", "eq.$userId")
                if (!startDate.isNullOrBlank()) {
                    parameter("income_date", "gte.$startDate")
                }
                if (!endDate.isNullOrBlank()) {
                    parameter("income_date", "lte.$endDate")
                }
                parameter("order", "income_date.desc,created_at.desc")
            }
        }
        return decodeOrThrow(resp, text, "Failed to load income")
    }

    suspend fun insertIncome(payload: IncomeInsert): Income {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.post("$baseUrl/rest/v1/income") {
                applySupabaseAuth(session, prefer = "return=representation")
                parameter("select", INCOME_SELECT)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(listOf(payload)))
            }
        }
        val list: List<Income> = decodeOrThrow(resp, text, "Failed to add income")
        return list.firstOrNull() ?: throw SupabaseException("Income insert returned empty response")
    }

    suspend fun updateIncome(id: String, userId: String, payload: IncomeUpdate): Income {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.patch("$baseUrl/rest/v1/income") {
                applySupabaseAuth(session, prefer = "return=representation")
                parameter("select", INCOME_SELECT)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
                contentType(ContentType.Application.Json)
                setBody(explicitNullJson.encodeToString(payload))
            }
        }
        val list: List<Income> = decodeOrThrow(resp, text, "Failed to update income")
        return list.firstOrNull() ?: throw SupabaseException("Income update returned empty response")
    }

    suspend fun deleteIncome(id: String, userId: String) {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.delete("$baseUrl/rest/v1/income") {
                applySupabaseAuth(session)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
            }
        }
        verifySuccess(resp, text, "Failed to delete income")
    }

    suspend fun deleteIncomeByIds(ids: List<String>, userId: String) {
        if (ids.isEmpty()) return
        val inFilter = "in.(${ids.joinToString(",")})"
        val (resp, text) = executeAuthenticated { session ->
            httpClient.delete("$baseUrl/rest/v1/income") {
                applySupabaseAuth(session)
                parameter("id", inFilter)
                parameter("user_id", "eq.$userId")
            }
        }
        verifySuccess(resp, text, "Failed to delete duplicate income")
    }

    // --- Recurring Transactions ---

    suspend fun getRecurring(userId: String, activeOnly: Boolean = false): List<RecurringTransaction> {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.get("$baseUrl/rest/v1/recurring_transactions") {
                applySupabaseAuth(session)
                parameter("select", RECURRING_SELECT)
                parameter("user_id", "eq.$userId")
                if (activeOnly) {
                    parameter("is_active", "eq.true")
                }
                parameter("order", "day_of_month.asc")
            }
        }
        return decodeOrThrow(resp, text, "Failed to load recurring rules")
    }

    suspend fun insertRecurring(payload: RecurringInsert): RecurringTransaction {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.post("$baseUrl/rest/v1/recurring_transactions") {
                applySupabaseAuth(session, prefer = "return=representation")
                parameter("select", RECURRING_SELECT)
                contentType(ContentType.Application.Json)
                setBody(explicitNullJson.encodeToString(listOf(payload)))
            }
        }
        val list: List<RecurringTransaction> = decodeOrThrow(resp, text, "Failed to create recurring rule")
        return list.firstOrNull() ?: throw SupabaseException("Recurring insert returned empty response")
    }

    suspend fun updateRecurring(
        id: String,
        userId: String,
        payload: RecurringUpdate
    ): RecurringTransaction {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.patch("$baseUrl/rest/v1/recurring_transactions") {
                applySupabaseAuth(session, prefer = "return=representation")
                parameter("select", RECURRING_SELECT)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
                contentType(ContentType.Application.Json)
                setBody(explicitNullJson.encodeToString(payload))
            }
        }
        val list: List<RecurringTransaction> = decodeOrThrow(resp, text, "Failed to update recurring rule")
        return list.firstOrNull() ?: throw SupabaseException("Recurring update returned empty response")
    }

    suspend fun updateRecurringActive(
        id: String,
        userId: String,
        isActive: Boolean
    ): RecurringTransaction {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.patch("$baseUrl/rest/v1/recurring_transactions") {
                applySupabaseAuth(session, prefer = "return=representation")
                parameter("select", RECURRING_SELECT)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
                contentType(ContentType.Application.Json)
                setBody("""{"is_active":$isActive}""")
            }
        }
        val list: List<RecurringTransaction> = decodeOrThrow(resp, text, "Failed to toggle recurring rule")
        return list.firstOrNull() ?: throw SupabaseException("Recurring toggle returned empty response")
    }

    suspend fun updateRecurringLastPosted(id: String, userId: String, lastPostedDate: String) {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.patch("$baseUrl/rest/v1/recurring_transactions") {
                applySupabaseAuth(session)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
                contentType(ContentType.Application.Json)
                setBody("""{"last_posted_date":"$lastPostedDate"}""")
            }
        }
        verifySuccess(resp, text, "Failed to update recurring last_posted_date")
    }

    suspend fun deleteRecurring(id: String, userId: String) {
        val (resp, text) = executeAuthenticated { session ->
            httpClient.delete("$baseUrl/rest/v1/recurring_transactions") {
                applySupabaseAuth(session)
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
            }
        }
        verifySuccess(resp, text, "Failed to delete recurring rule")
    }

    companion object {
        const val OWNER_EMAIL = "ec.mataac@gmail.com"

        private const val EXPENSE_SELECT =
            "*,wallets(name,color),categories(name,icon),payment_methods(label,type)"
        private const val INCOME_SELECT =
            "*,wallets(name,color)"
        private const val RECURRING_SELECT =
            "*,wallets(name,color),categories(name,icon),payment_methods(label,type)"

        private val policyJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private val PERMANENT_REFRESH_ERROR_CODES = setOf(
            "refresh_token_not_found",
            "refresh_token_already_used",
            "session_not_found",
            "session_expired",
            "bad_jwt",
            "user_not_found",
            "user_banned"
        )

        private val PERMANENT_REFRESH_TEXT_MARKERS = listOf(
            "invalid refresh token",
            "refresh token not found",
            "already used",
            "invalid_grant"
        )

        internal fun isAllowedOwnerEmail(email: String): Boolean =
            email.trim().lowercase() == OWNER_EMAIL

        internal fun shouldClearSessionOnRefreshFailure(statusCode: Int, bodyText: String): Boolean {
            if (statusCode >= 500 || statusCode == 408 || statusCode == 429 || statusCode !in setOf(400, 401, 403)) {
                return false
            }
            if (bodyText.isBlank()) return false

            val parsed = runCatching { policyJson.decodeFromString<JsonObject>(bodyText) }.getOrNull()
                ?: return false

            fun field(name: String): String =
                (parsed[name] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

            val error = field("error")
            val errorCode = field("error_code").lowercase()
            val msg = field("msg")
            val message = field("message")
            val errorDescription = field("error_description")

            if (error.equals("invalid_grant", ignoreCase = true)) {
                return true
            }
            if (errorCode in PERMANENT_REFRESH_ERROR_CODES) {
                return true
            }

            val combined = listOf(error, errorCode, msg, message, errorDescription)
                .joinToString(" ")
                .lowercase()
            return PERMANENT_REFRESH_TEXT_MARKERS.any { marker -> combined.contains(marker) }
        }
    }
}
