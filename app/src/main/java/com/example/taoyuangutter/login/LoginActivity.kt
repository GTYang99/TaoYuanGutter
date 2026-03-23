package com.example.taoyuangutter.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.taoyuangutter.MainActivity
import com.example.taoyuangutter.databinding.ActivityLoginBinding
import com.example.taoyuangutter.gutter.GutterFormActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

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
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupOfflineButtons() {
        // 離線填寫：直接開啟 GutterFormActivity（離線模式，座標 0.0, 0.0）
        binding.btnOfflineForm.setOnClickListener {
            val intent = GutterFormActivity.newOfflineIntent(this)
            startActivity(intent)
        }
    }
}
