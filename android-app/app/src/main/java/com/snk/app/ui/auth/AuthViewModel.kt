package com.snk.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snk.app.data.auth.AuthErrorCode
import com.snk.app.data.auth.AuthRepository
import com.snk.app.data.auth.AuthResult
import com.snk.app.data.auth.AuthenticatedAccount
import com.snk.app.data.auth.AuthenticatedSessionManager
import com.snk.app.data.auth.RegistrationResponse
import com.snk.app.data.auth.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

sealed interface AuthUiState {
    data object Restoring : AuthUiState
    data class SignedOut(val message: String? = null) : AuthUiState
    data class Pending(val approvalTicket: String?) : AuthUiState
    data object Rejected : AuthUiState
    data object Disabled : AuthUiState
    data class MustChangePassword(val account: AuthenticatedAccount) : AuthUiState
    data class Authenticated(val account: AuthenticatedAccount) : AuthUiState
}

object AuthStatePolicy {
    fun fromLogin(result: AuthResult<AuthenticatedAccount>): AuthUiState = when (result) {
        is AuthResult.Success -> if (result.value.mustChangePassword) {
            AuthUiState.MustChangePassword(result.value)
        } else AuthUiState.Authenticated(result.value)
        is AuthResult.Failure -> when (result.code) {
            AuthErrorCode.ACCOUNT_PENDING -> AuthUiState.Pending(null)
            AuthErrorCode.ACCOUNT_REJECTED -> AuthUiState.Rejected
            AuthErrorCode.ACCOUNT_DISABLED -> AuthUiState.Disabled
            AuthErrorCode.MUST_CHANGE_PASSWORD -> AuthUiState.SignedOut("请先修改密码。")
            AuthErrorCode.INVALID_CREDENTIALS -> AuthUiState.SignedOut("用户名或密码错误。")
            AuthErrorCode.NETWORK -> AuthUiState.SignedOut("网络不可用，请稍后重试。")
            else -> AuthUiState.SignedOut("登录失败，请稍后重试。")
        }
    }

    fun fromRegistration(result: AuthResult<RegistrationResponse>): AuthUiState = when (result) {
        is AuthResult.Success -> AuthUiState.Pending(result.value.approvalTicket)
        is AuthResult.Failure -> AuthUiState.SignedOut(
            if (result.code == AuthErrorCode.NETWORK) "网络不可用，请稍后重试。" else "注册失败，请检查填写内容。",
        )
    }
}

class AuthViewModel(
    private val repository: AuthRepository,
    private val sessions: AuthenticatedSessionManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow<AuthUiState>(AuthUiState.Restoring)
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()
    private val approvalCheckMutex = Mutex()

    init { restore() }

    fun restore() {
        viewModelScope.launch {
            mutableState.value = AuthUiState.Restoring
            mutableState.value = when (sessions.restoreSession()) {
                SessionState.AUTHENTICATED -> sessions.currentAccount()?.let(AuthUiState::Authenticated) ?: AuthUiState.SignedOut()
                SessionState.MUST_CHANGE_PASSWORD -> sessions.currentAccount()?.let(AuthUiState::MustChangePassword) ?: AuthUiState.SignedOut()
                SessionState.PENDING -> AuthUiState.Pending(repository.pendingApprovalTicket())
                SessionState.REJECTED -> AuthUiState.Rejected
                SessionState.DISABLED -> AuthUiState.Disabled
                SessionState.SIGNED_OUT -> AuthUiState.SignedOut()
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            mutableState.value = AuthUiState.Restoring
            mutableState.value = AuthStatePolicy.fromLogin(repository.login(username, password))
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            mutableState.value = AuthUiState.Restoring
            mutableState.value = AuthStatePolicy.fromRegistration(repository.register(username, password))
        }
    }

    fun checkApproval() {
        if (!approvalCheckMutex.tryLock()) return
        viewModelScope.launch {
            try {
                val ticket = (mutableState.value as? AuthUiState.Pending)?.approvalTicket
                    ?: repository.pendingApprovalTicket()
                    ?: return@launch
                when (val result = repository.registrationStatus(ticket)) {
                    is AuthResult.Success -> mutableState.value = when (result.value.accountStatus) {
                        "ACTIVE" -> AuthUiState.SignedOut("审核已通过，请登录。")
                        "REJECTED" -> AuthUiState.Rejected
                        "DISABLED" -> AuthUiState.Disabled
                        else -> AuthUiState.Pending(ticket)
                    }
                    is AuthResult.Failure -> if (result.code != AuthErrorCode.NETWORK) {
                        mutableState.value = AuthUiState.SignedOut("无法查询审核状态，请重新登录。")
                    }
                }
            } finally { approvalCheckMutex.unlock() }
        }
    }

    fun backToLogin(message: String? = null) { mutableState.value = AuthUiState.SignedOut(message) }

    fun logout() { viewModelScope.launch {
        val result = repository.logout()
        mutableState.value = AuthUiState.SignedOut(if (result is AuthResult.Failure) "本机已退出，服务器会话将在下次鉴权时失效。" else "已退出登录。")
    } }

    fun changePassword(oldPassword: String, newPassword: String) { viewModelScope.launch {
        when (repository.changePassword(oldPassword, newPassword)) {
            is AuthResult.Success -> { repository.logout(); mutableState.value = AuthUiState.SignedOut("密码已修改，请重新登录。") }
            is AuthResult.Failure -> mutableState.value = sessions.currentAccount()?.let {
                if (it.mustChangePassword) AuthUiState.MustChangePassword(it) else AuthUiState.Authenticated(it)
            } ?: AuthUiState.SignedOut("修改密码失败，请重新登录。")
        }
    } }
}
