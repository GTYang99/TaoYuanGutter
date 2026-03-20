package com.example.taoyuangutter.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.example.taoyuangutter.MainActivity
import com.example.taoyuangutter.R
import com.example.taoyuangutter.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Inflate the layout for this activity
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Add logic to check if user is already logged in and navigate to MainActivity if so.
        // TODO: Set up listeners for login button, forgot password, etc.
        setupLoginButtonListener()
    }

    private fun setupLoginButtonListener() {
        binding.loginButton.setOnClickListener {
            // In a real app, you would validate credentials here and then:
            // 1. If valid, navigate to MainActivity.
            // 2. If invalid, show an error message.
            // For now, we'll just navigate to MainActivity directly.
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close LoginActivity so the user can't go back to it
        }
    }

    // TODO: Implement logic for forgot password, if needed.
}
