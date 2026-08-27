package com.example.taoyuangutter.login

import android.content.Context
import android.content.Intent

class AuthNavigator(
    private val context: Context
) {
    fun clearAuthAndGoLogin() {
        context.getSharedPreferences("taoyuan_prefs", Context.MODE_PRIVATE).edit()
            .remove("auth_token")
            .remove("user_name")
            .remove("user_company")
            .remove("group_id")
            .apply()
        goToLogin()
    }

    fun goToLogin() {
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
