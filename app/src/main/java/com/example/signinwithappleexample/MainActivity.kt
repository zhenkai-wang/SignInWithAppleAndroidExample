package com.example.signinwithappleexample
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var clientId = AppleConstants.CLIENT_ID
    private var redirectUri = AppleConstants.REDIRECT_URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        apple_login_btn.setOnClickListener {
            startAppleSignIn()
        }
        edt_client_id.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                clientId = s.toString().ifBlank { AppleConstants.CLIENT_ID }
                tv_result_url.setText(generateUrl())
            }
        })
        edt_redirect_url.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                redirectUri = s.toString().ifBlank { AppleConstants.REDIRECT_URI }
                tv_result_url.setText(generateUrl())
            }
        })
        tv_result_url.setText(generateUrl())
    }

    private fun startAppleSignIn() {
        val resultUrl = tv_result_url.text.toString()
        val intent = Intent(this, AppleSignInActivity::class.java)
        intent.putExtra(AppleSignInActivity.CONSTANT_RESULT_URL, resultUrl)
        startActivity(intent)
    }

    private fun generateUrl(): String {
        val state = UUID.randomUUID().toString()
        val appleAuthURLFull =
            AppleConstants.AUTHURL + "?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&response_type=code%20id_token&scope=" + AppleConstants.SCOPE + "&response_mode=form_post&state=" + state
        return appleAuthURLFull
    }
}
