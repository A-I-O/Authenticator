package com.example.authenticator

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.material.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import java.io.InputStream
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation

import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.res.painterResource
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import androidx.activity.result.PickVisualMediaRequest
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap

import java.io.File
import java.io.FileOutputStream


//import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavType
import androidx.navigation.navArgument
import coil.compose.rememberImagePainter

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(navController = navController)
                }
                composable(
                    route = "dashboard/{purpose}",
                    arguments = listOf(navArgument("purpose") { type = NavType.StringType })
                ) { navBackStackEntry ->
                    val purpose = navBackStackEntry.arguments?.getString("purpose")
                    DashboardScreen(navController = navController, purpose = purpose)
                }
            }
        }
    }
}




private fun generateQRCode(registrationNumber: String, callback: (Bitmap) -> Unit) {
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
    hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

    val writer = QRCodeWriter()
    val bitMatrix: BitMatrix
    try {
        bitMatrix = writer.encode(registrationNumber, BarcodeFormat.QR_CODE, 512, 512, hints)
    } catch (e: WriterException) {
        e.printStackTrace()
        return
    }

    val width = bitMatrix.width
    val height = bitMatrix.height
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    val blackColor = 0xFF000000.toInt() // Black color
    val whiteColor = 0xFFFFFFFF.toInt() // White color
    for (x in 0 until width) {
        for (y in 0 until height) {
            bmp.setPixel(x, y, if (bitMatrix[x, y]) blackColor else whiteColor)
        }
    }

    // Callback with the generated bitmap
    callback(bmp)
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    // Define a mutable state to control the visibility of the dialog
    val showDialog = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    var hasRunSuccessfully by remember { mutableStateOf(sharedPreferences.getBoolean("hasRunSuccessfully", false)) }

    if (hasRunSuccessfully) {
        // Display error message if the app has already run successfully before
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome back!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You've already signed in for this session. If you're a new user, please use a unique device (the one associated with your account).",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // If the app has not run successfully before, continue with the login screen
    var isPasswordVisible by remember { mutableStateOf(false) }
    Surface(color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MMUST Authentication",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "[Enter your portal.mmust.ac.ke username and password]",
                fontSize = 16.sp,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var loginMessage by remember { mutableStateOf("") }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible }
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Login button for Class
            Button(
                onClick = {
                    loginUser(username, password) { success, name, registrationNumber, program, feeBalance ->
                        if (success) {
                            UserDataHolder.name = name
                            UserDataHolder.registrationNumber = registrationNumber
                            UserDataHolder.program = program
                            UserDataHolder.feeBalance = feeBalance
                            navController.navigate("dashboard/class")
                        } else {
                            loginMessage = "Login failed. Incorrect Username or Password. Please try again."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Login for Class")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Login button for Identification
            Button(
                onClick = {
                    loginUser(username, password) { success, name, registrationNumber, program, feeBalance ->
                        if (success) {
                            UserDataHolder.name = name
                            UserDataHolder.registrationNumber = registrationNumber
                            UserDataHolder.program = program
                            UserDataHolder.feeBalance = feeBalance
                            navController.navigate("dashboard/identification")
                        } else {
                            loginMessage = "Login failed. Incorrect Username or Password. Please try again."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Login for Identification")
            }

            Spacer(modifier = Modifier.height(8.dp))
// Login button for Exam
            Button(
                onClick = {
                    loginUser(username, password) { success, name, registrationNumber, program, feeBalance ->
                        if (success) {
                            UserDataHolder.name = name
                            UserDataHolder.registrationNumber = registrationNumber
                            UserDataHolder.program = program
                            UserDataHolder.feeBalance = feeBalance
                            if (!(feeBalance.contains("-") || feeBalance == "KES 0.00")) {
                                showDialog.value = true
                            } else {
                                navController.navigate("dashboard/exam")
                            }
                        } else {
                            loginMessage = "Login failed. Incorrect Username or Password. Please try again."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Login for Exam")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display loading message if loading
            if (isLoading) {
                Text(text = "Logging in...")
            } else {
                // Display login message if not loading
                Text(
                    text = loginMessage,
                    color = if (loginMessage.contains("successful")) Color.Green else Color.Red
                )
            }
        }
    }

            // Display the dialog if showDialog is true
            if (showDialog.value) {
                AlertDialog(
                    onDismissRequest = {
                        // Dismiss the dialog if the user clicks outside of it
                        showDialog.value = false
                    },
                    title = { Text("Ineligible") },
                    text = {
                        // Display the ineligible message with fee balance and current date time
                        val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        val message = "You are ineligible. Your fee balance is ${UserDataHolder.feeBalance} as of $currentDateTime. Any changes to your financial statement will be reflected in the app in real time, only then will you be able to sign in for the exam."
                        Text(message)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                // Dismiss the dialog when the user clicks OK
                                showDialog.value = false
                            }
                        ) {
                            Text("OK")
                        }
                    }
                )
            }
            }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController, purpose: String?) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Function to handle QR code generation button click
    fun generateQRCodeButtonClicked(registrationNumber: String) {
        generateQRCode(registrationNumber) { bitmap ->
            qrBitmap = bitmap
        }
    }
    Surface(color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (purpose) {
                    "class" -> "Success, You're Signed In for Class"
                    "identification" -> "Success, You're Signed In for Identification"

                    "exam" -> "Success, You're Signed In for Exam"
                    else -> "Success, You're Signed In"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Access user data from UserDataHolder
            UserInfoItem(label = "Name", value = UserDataHolder.name)
            UserInfoItem(label = "Registration Number", value = UserDataHolder.registrationNumber)
            UserInfoItem(label = "Program", value = UserDataHolder.program)
            if (purpose == "exam") {
                UserInfoItem(label = "Fee Balance", value = UserDataHolder.feeBalance)
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Button to navigate back to login screen
            Button(
                onClick = {
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            ) {
                Text(text = "Logout")
            }
            // Button to generate QR code
            if (purpose == "identification"||purpose == "exam") {
                Button(
                    onClick = { generateQRCodeButtonClicked(UserDataHolder.registrationNumber ?: "") },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = "Generate QR Code")
                }
                // Display QR code if available
                qrBitmap?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR Code"
                    )
                }
            }
        }
    }
}
@Composable
fun UserInfoItem(label: String, value: String) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 18.sp,
            color = Color.Black
        )
    }
}

private fun loginUser(
    username: String,
    password: String,
    onLoginResult: (Boolean, String, String, String, String) -> Unit
) {
    val client = OkHttpClient()
    val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    val requestBody = """
        {
            "username": "$username",
            "password": "$password"
        }
    """.trimIndent().toRequestBody(jsonMediaType)

    val request = Request.Builder()
        .url("http://192.168.38.85:5000/login") // Server URL
//        .url("http://http://10.254.224.73:5000/login")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            // Handle network errors
            Log.e("LoginActivity", "Network call failed: ${e.message}")
            // Indicate network error to the user
            onLoginResult(false, "", "", "", "Network error. Please check your internet connection and try again.")
        }

        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body?.string()

            // Check if login was successful
            val success =
                response.isSuccessful && responseBody != null && responseBody.contains("Login successful")

            // Log the response body
            Log.d("LoginActivity", "Response body: $responseBody")
            if (success) {
                // Parse the JSON response
                val jsonResponse = JSONObject(responseBody)
                val profileData = jsonResponse.getJSONObject("profile_data")
                val name = profileData.getString("name")
                val registrationNumber = profileData.getString("registration_number")
                val feeBalance = profileData.getString("fee_balance")
                val program = profileData.getString("program")

                // Log the retrieved user details
                Log.d(
                    "LoginActivity",
                    "Name: $name, Registration Number: $registrationNumber, Program: $program, Fee Balance: $feeBalance"
                )

                // Notify the result and pass user details
                Handler(Looper.getMainLooper()).post {
                    onLoginResult(success, name, registrationNumber, program, feeBalance)
                }
            } else {
                // Handle other login failures
                Handler(Looper.getMainLooper()).post {
                    onLoginResult(success, "", "", "", "Login failed. Incorrect username or password.")
                }
            }
        }
    })
}

object UserDataHolder {
    var name: String = ""
    var registrationNumber: String = ""
    var program: String = ""
    var feeBalance: String = ""
}














