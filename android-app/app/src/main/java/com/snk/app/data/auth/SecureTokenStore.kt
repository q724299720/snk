package com.snk.app.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

private val Context.secureAuthDataStore by preferencesDataStore(name = "secure_auth_metadata")

class SecureTokenStore(
    context: Context,
    private val keyAlias: String = "snk_refresh_token_key_v1",
) : SecureTokenStoreContract {
    private val appContext = context.applicationContext
    private val encryptedPreferences = appContext.getSharedPreferences(ENCRYPTED_PREFS, Context.MODE_PRIVATE)

    override suspend fun save(session: StoredAuthenticatedSession) {
        encryptedPreferences.edit()
            .putString(Keys.REFRESH_TOKEN_CIPHERTEXT, encrypt(session.refreshToken, "refresh-token"))
            .remove(Keys.APPROVAL_TICKET_CIPHERTEXT)
            .commit()
        appContext.secureAuthDataStore.edit { metadata ->
            metadata[Keys.USER_ID] = session.account.userId
            metadata[Keys.USERNAME] = session.account.username
            metadata[Keys.ROLE] = session.account.role
            metadata[Keys.DEVICE_ID] = session.deviceId
            metadata[Keys.MUST_CHANGE_PASSWORD] = session.account.mustChangePassword
            metadata[Keys.SESSION_STATE] = "AUTHENTICATED"
            metadata[Keys.CIPHERTEXT_VERSION] = 1
        }
    }

    override suspend fun read(): StoredAuthenticatedSession? {
        val metadata = preferences()
        if (metadata[Keys.SESSION_STATE] != "AUTHENTICATED") return null
        val ciphertext = encryptedPreferences.getString(Keys.REFRESH_TOKEN_CIPHERTEXT, null) ?: return null
        return runCatching {
            StoredAuthenticatedSession(
                account = AuthenticatedAccount(
                    userId = metadata[Keys.USER_ID] ?: return null,
                    username = metadata[Keys.USERNAME] ?: return null,
                    role = metadata[Keys.ROLE] ?: return null,
                    mustChangePassword = metadata[Keys.MUST_CHANGE_PASSWORD] ?: false,
                ),
                deviceId = metadata[Keys.DEVICE_ID] ?: return null,
                refreshToken = decrypt(ciphertext, "refresh-token"),
            )
        }.getOrNull()
    }

    override suspend fun clear() {
        encryptedPreferences.edit().clear().commit()
        appContext.secureAuthDataStore.edit { metadata ->
            metadata.remove(Keys.USER_ID)
            metadata.remove(Keys.USERNAME)
            metadata.remove(Keys.ROLE)
            metadata.remove(Keys.MUST_CHANGE_PASSWORD)
            metadata.remove(Keys.SESSION_STATE)
            metadata.remove(Keys.CIPHERTEXT_VERSION)
        }
    }

    override suspend fun saveApprovalTicket(ticket: String) {
        encryptedPreferences.edit()
            .putString(Keys.APPROVAL_TICKET_CIPHERTEXT, encrypt(ticket, "approval-ticket"))
            .commit()
    }

    override suspend fun readApprovalTicket(): String? = encryptedPreferences
        .getString(Keys.APPROVAL_TICKET_CIPHERTEXT, null)
        ?.let { runCatching { decrypt(it, "approval-ticket") }.getOrNull() }

    private fun encrypt(plaintext: String, aad: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(aad.toByteArray())
        val encrypted = cipher.doFinal(plaintext.toByteArray())
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(packedValue: String, aad: String): String {
        val packed = Base64.decode(packedValue, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, packed.copyOfRange(0, 12)))
        cipher.updateAAD(aad.toByteArray())
        return cipher.doFinal(packed.copyOfRange(12, packed.size)).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private suspend fun preferences() = appContext.secureAuthDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .first()

    private object Keys {
        const val REFRESH_TOKEN_CIPHERTEXT = "refresh_token_ciphertext"
        const val APPROVAL_TICKET_CIPHERTEXT = "approval_ticket_ciphertext"
        val USER_ID = longPreferencesKey("account_user_id")
        val USERNAME = stringPreferencesKey("account_username")
        val ROLE = stringPreferencesKey("account_role")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val MUST_CHANGE_PASSWORD = booleanPreferencesKey("must_change_password")
        val SESSION_STATE = stringPreferencesKey("session_state")
        val CIPHERTEXT_VERSION = intPreferencesKey("ciphertext_version")
    }

    companion object {
        private const val ENCRYPTED_PREFS = "secure_auth_tokens"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

class PersistentDeviceIdProvider(context: Context) : DeviceIdProvider {
    private val appContext = context.applicationContext
    override suspend fun get(): String {
        val existing = appContext.secureAuthDataStore.data.first()[DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        appContext.secureAuthDataStore.edit { it[DEVICE_ID] = generated }
        return generated
    }
    private companion object { val DEVICE_ID = stringPreferencesKey("device_id") }
}
