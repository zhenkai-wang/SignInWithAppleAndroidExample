package com.example.signinwithappleexample

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.io.OutputStreamWriter
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class AppleSignInActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var loadingView: View

    lateinit var appleAuthCode: String
    lateinit var appleClientSecret: String

    var appleId = ""
    var appleFirstName = ""
    var appleMiddleName = ""
    var appleLastName = ""
    var appleEmail = ""
    var appleAccessToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resultUrl = intent.getStringExtra(CONSTANT_RESULT_URL) ?: ""

        setContentView(R.layout.activity_apple_sign_in)
        loadingView = findViewById(R.id.loading_overlay)

        webView = findViewById<WebView>(R.id.webView).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    loadingView.visibility = View.GONE
                    super.onPageFinished(view, url)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    loadingView.visibility = View.VISIBLE
                    super.onPageStarted(view, url, favicon)
                }
            }
        }.also {
            Toast.makeText(this@AppleSignInActivity, resultUrl, Toast.LENGTH_LONG).show()
            Log.e("wang", resultUrl)
            it.loadUrl(resultUrl)
        }
    }

    // A client to know about WebView navigation
    // For API 21 and above
    @Suppress("OverridingDeprecatedMember")
    inner class AppleWebViewClient : WebViewClient() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            Toast.makeText(this@AppleSignInActivity, "Url: ${request?.url}", Toast.LENGTH_LONG)
                .show()
            Log.e("wang", "Url: ${request?.url}")
            if (request?.url.toString().startsWith(AppleConstants.REDIRECT_URI)) {
                handleUrl(request?.url.toString())
                // Close the dialog after getting the authorization code
                if (request?.url.toString().contains("success=")) {
//                    appledialog.dismiss()
                }
                return true
            }
            return true
        }

        // For API 19 and below
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Toast.makeText(this@AppleSignInActivity, "Url: $url", Toast.LENGTH_LONG).show()
            Log.e("wang", "Url: $url")
            if (url.startsWith(AppleConstants.REDIRECT_URI)) {
                handleUrl(url)
                // Close the dialog after getting the authorization code
                if (url.contains("success=")) {
//                    appledialog.dismiss()
                }
                return true
            }
            return false
        }

        // Check WebView url for access token code or error
        @SuppressLint("LongLogTag")
        private fun handleUrl(url: String) {

            val uri = Uri.parse(url)

            val success = uri.getQueryParameter("success")
            if (success == "true") {

                // Get the Authorization Code from the URL
                appleAuthCode = uri.getQueryParameter("code") ?: ""
                Log.i("Apple Code: ", appleAuthCode)

                // Get the Client Secret from the URL
                appleClientSecret = uri.getQueryParameter("client_secret") ?: ""
                Log.i("Apple Client Secret: ", appleClientSecret)

                //Check if user gave access to the app for the first time by checking if the url contains their email
                if (url.contains("email")) {

                    //Get user's First Name
                    val firstName = uri.getQueryParameter("first_name")
                    Log.i("Apple User First Name: ", firstName ?: "")
                    appleFirstName = firstName ?: "Not exists"

                    //Get user's Middle Name
                    val middleName = uri.getQueryParameter("middle_name")
                    Log.i("Apple User Middle Name: ", middleName ?: "")
                    appleMiddleName = middleName ?: "Not exists"

                    //Get user's Last Name
                    val lastName = uri.getQueryParameter("last_name")
                    Log.i("Apple User Last Name: ", lastName ?: "")
                    appleLastName = lastName ?: "Not exists"

                    //Get user's email
                    val email = uri.getQueryParameter("email")
                    Log.i("Apple User Email: ", email ?: "Not exists")
                    appleEmail = email ?: ""
                }

                // Exchange the Auth Code for Access Token
                requestForAccessToken(appleAuthCode, appleClientSecret)
            } else if (success == "false") {
                Log.e("ERROR", "We couldn't get the Auth Code")
            }
        }
    }

    private fun requestForAccessToken(code: String, clientSecret: String) {

        val grantType = "authorization_code"

        val postParamsForAuth =
            "grant_type=" + grantType + "&code=" + code + "&redirect_uri=" + AppleConstants.REDIRECT_URI + "&client_id=" + AppleConstants.CLIENT_ID + "&client_secret=" + clientSecret

        CoroutineScope(Dispatchers.Default).launch {
            val httpsURLConnection =
                withContext(Dispatchers.IO) { URL(AppleConstants.TOKENURL).openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "POST"
            httpsURLConnection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded"
            )
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = true
            withContext(Dispatchers.IO) {
                val outputStreamWriter = OutputStreamWriter(httpsURLConnection.outputStream)
                outputStreamWriter.write(postParamsForAuth)
                outputStreamWriter.flush()
            }
            val response = httpsURLConnection.inputStream.bufferedReader()
                .use { it.readText() }  // defaults to UTF-8

            val jsonObject = JSONTokener(response).nextValue() as JSONObject

            val accessToken = jsonObject.getString("access_token") // Here is the access token
            Log.i("Apple Access Token is: ", accessToken)
            appleAccessToken = accessToken

            val expiresIn = jsonObject.getInt("expires_in") // When the access token expires
            Log.i("expires in: ", expiresIn.toString())

            val refreshToken =
                jsonObject.getString("refresh_token") // The refresh token used to regenerate new access tokens. Store this token securely on your server.
            Log.i("refresh token: ", refreshToken)


            val idToken =
                jsonObject.getString("id_token") // A JSON Web Token that contains the user’s identity information.
            Log.i("ID Token: ", idToken)

            // Get encoded user id by splitting idToken and taking the 2nd piece
            val encodedUserID = idToken.split(".")[1]

            //Decode encodedUserID to JSON
            val decodedUserData = String(Base64.decode(encodedUserID, Base64.DEFAULT))
            val userDataJsonObject = JSONObject(decodedUserData)
            // Get User's ID
            val userId = userDataJsonObject.getString("sub")
            Log.i("Apple User ID :", userId)
            appleId = userId

            withContext(Dispatchers.Main) {
                openDetailsActivity()
            }
        }
    }

    private fun openDetailsActivity() {
        val myIntent = Intent(this, DetailsActivity::class.java)
        myIntent.putExtra("apple_id", appleId)
        myIntent.putExtra("apple_first_name", appleFirstName)
        myIntent.putExtra("apple_middle_name", appleMiddleName)
        myIntent.putExtra("apple_last_name", appleLastName)
        myIntent.putExtra("apple_email", appleEmail)
        myIntent.putExtra("apple_access_token", appleAccessToken)
        startActivity(myIntent)
    }

    companion object {
        const val CONSTANT_RESULT_URL = "result_url"
    }
}
