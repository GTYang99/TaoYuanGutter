package com.example.taoyuangutter.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.taoyuangutter.MainActivity
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPasswordToggle()
        setupLoginButtonListener()
    }

    private fun setupPasswordToggle() {
        binding.ivPasswordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivPasswordToggle.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivPasswordToggle.setImageResource(R.drawable.ic_visibility)
            }
            // Move cursor to the end
            binding.passwordEditText.setSelection(binding.passwordEditText.text?.length ?: 0)
        }
    }

    private fun setupLoginButtonListener() {
        binding.loginButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
