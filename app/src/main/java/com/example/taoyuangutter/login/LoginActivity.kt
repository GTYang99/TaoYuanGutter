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
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repository = GutterRepository()

    companion object {
        private const val PREFS_NAME       = "taoyuan_prefs"
        private const val KEY_TOKEN        = "auth_token"
        private const val KEY_NAME         = "user_name"
        private const val KEY_COMPANY      = "user_company"
        private const val KEY_GROUP_ID     = "group_id"
        private const val KEY_REMEMBER_ME  = "remember_me"
        private const val KEY_SAVED_USER   = "saved_username"
        private const val KEY_SAVED_PWD    = "saved_password"

        /** 從 SharedPreferences 取出已儲存的 token（無則回傳 null） */
        fun getSavedToken(context: Context): String? =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_TOKEN, null)

        /** 從 SharedPreferences 取出登入後的 group_id（無則回傳 -1） */
        fun getSavedGroupId(context: Context): Int =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_GROUP_ID, -1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadRememberedAccount()
        setupLoginButtonListener()
        setupOfflineButtons()
    }

    private fun loadRememberedAccount() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isRemembered = prefs.getBoolean(KEY_REMEMBER_ME, false)
        if (isRemembered) {
            binding.cbRememberMe.isChecked = true
            binding.usernameEditText.setText(prefs.getString(KEY_SAVED_USER, ""))
            binding.passwordEditText.setText(prefs.getString(KEY_SAVED_PWD, ""))
        }
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
                        val isRememberMe = binding.cbRememberMe.isChecked
                        
                        val editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                        
                        // 儲存 token 與使用者資訊
                        if (loginData != null) {
                            editor.putString(KEY_TOKEN,   loginData.token)
                                .putString(KEY_NAME,    loginData.name    ?: "")
                                .putString(KEY_COMPANY, loginData.company ?: "")
                                .putInt(KEY_GROUP_ID,   loginData.groupId ?: -1)
                        }

                        // 處理記住密碼邏輯
                        if (isRememberMe) {
                            editor.putBoolean(KEY_REMEMBER_ME, true)
                            editor.putString(KEY_SAVED_USER, username)
                            editor.putString(KEY_SAVED_PWD, password)
                        } else {
                            editor.putBoolean(KEY_REMEMBER_ME, false)
                            editor.remove(KEY_SAVED_USER)
                            editor.remove(KEY_SAVED_PWD)
                        }
                        editor.apply()

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
        // 離線填寫：進入主畫面，但不打後端 API（可編輯多點位草稿）
        binding.btnOfflineForm.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OFFLINE_MAIN, true)
            }
            startActivity(intent)
            finish()
        }
    }

    /** 登入進行中：停用按鈕、顯示 loading 狀態 */
    private fun setLoading(loading: Boolean) {
        binding.loginButton.isEnabled = !loading
        // 若 layout 有 progressBar 可在此控制顯示；目前以按鈕 enabled 狀態作為視覺回饋
        binding.loginButton.text = if (loading) "登入中…" else "登入"
    }
}
