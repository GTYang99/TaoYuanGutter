package com.example.taoyuangutter.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.taoyuangutter.MainActivity
import com.example.taoyuangutter.api.ApiResult
import com.example.taoyuangutter.api.GutterRepository
import com.example.taoyuangutter.databinding.ActivityLoginBinding
import com.example.taoyuangutter.gutter.GutterFormActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repository = GutterRepository()

    companion object {
        private const val PREFS_NAME  = "taoyuan_prefs"
        private const val KEY_TOKEN   = "auth_token"
        private const val KEY_NAME    = "user_name"
        private const val KEY_COMPANY = "user_company"

        /** 從 SharedPreferences 取出已儲存的 token（無則回傳 null） */
        fun getSavedToken(context: Context): String? =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_TOKEN, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLoginButtonListener()
        setupOfflineButtons()
    }

    private fun setupLoginButtonListener() {
        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text?.toString()?.trim() ?: ""
            val password = binding.passwordEditText.text?.toString() ?: ""

            if (username.isEmpty()) {
                binding.usernameEditText.error = "請輸入帳號"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.passwordEditText.error = "請輸入密碼"
                return@setOnClickListener
            }

            setLoading(true)

            lifecycleScope.launch {
                when (val result = repository.login(username, password)) {
                    is ApiResult.Success -> {
                        val loginData = result.data.data
                        if (loginData != null) {
                            // 儲存 token 與使用者資訊
                            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                                .putString(KEY_TOKEN,   loginData.token)
                                .putString(KEY_NAME,    loginData.name    ?: "")
                                .putString(KEY_COMPANY, loginData.company ?: "")
                                .apply()
                        }
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is ApiResult.Error -> {
                        setLoading(false)
                        Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupOfflineButtons() {
        // 離線填寫：直接開啟 GutterFormActivity（離線模式，座標 0.0, 0.0）
        binding.btnOfflineForm.setOnClickListener {
            val intent = GutterFormActivity.newOfflineIntent(this)
            startActivity(intent)
        }
    }

    /** 登入進行中：停用按鈕、顯示 loading 狀態 */
    private fun setLoading(loading: Boolean) {
        binding.loginButton.isEnabled = !loading
        // 若 layout 有 progressBar 可在此控制顯示；目前以按鈕 enabled 狀態作為視覺回饋
        binding.loginButton.text = if (loading) "登入中…" else "登入"
    }
}
