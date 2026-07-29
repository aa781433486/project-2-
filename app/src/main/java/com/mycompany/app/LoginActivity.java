package com.mycompany.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class LoginActivity extends BaseActivity {

    public static final String PREFS = "grocery_prefs";
    public static final String KEY_FINGERPRINT_ENABLED = "fingerprint_enabled";
    public static final String KEY_SAVED_USERNAME = "saved_username";
    public static final String KEY_SAVED_PASSWORD = "saved_password";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_STORE_NAME = "store_name";
    public static final String KEY_LOW_STOCK_THRESHOLD = "low_stock_threshold";

    EditText etUsername, etPassword;
    Button btnLogin, btnCreate, btnFingerprint;
    TextView tvWelcomeLogin, tvSetupHint;
    DatabaseHelper db;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        db = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnCreate = findViewById(R.id.btnCreate);
        btnFingerprint = findViewById(R.id.btnFingerprint);
        tvWelcomeLogin = findViewById(R.id.tvWelcomeLogin);
        tvSetupHint = findViewById(R.id.tvSetupHint);

        String storeName = prefs.getString(KEY_STORE_NAME, "نظام البقالة");
        tvWelcomeLogin.setText("🌟 " + storeName + " 🌟");
        tvWelcomeLogin.setSelected(true);

        updateUIForUserState();

        btnLogin.setOnClickListener(v -> loginUser());

        btnCreate.setOnClickListener(v -> createFirstUser());

        btnFingerprint.setOnClickListener(v -> showBiometricPrompt());

        etPassword.setOnClickListener(v -> togglePasswordVisibility());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIForUserState();
    }

    private void updateUIForUserState() {
        boolean userExists = db.anyUserExists();
        if (userExists) {
            btnCreate.setVisibility(View.GONE);
            tvSetupHint.setVisibility(View.GONE);
        } else {
            btnCreate.setVisibility(View.VISIBLE);
            tvSetupHint.setVisibility(View.VISIBLE);
            tvSetupHint.setText("⚠️ لا يوجد حساب. أنشئ حساباً واحداً للمالك ثم لن يظهر هذا الزر مجدداً.");
        }

        boolean fingerprintEnabled = prefs.getBoolean(KEY_FINGERPRINT_ENABLED, false);
        boolean hasSavedCreds = !prefs.getString(KEY_SAVED_USERNAME, "").isEmpty();
        if (fingerprintEnabled && hasSavedCreds && userExists && isBiometricAvailable()) {
            btnFingerprint.setVisibility(View.VISIBLE);
        } else {
            btnFingerprint.setVisibility(View.GONE);
        }
    }

    private void createFirstUser() {
        if (db.anyUserExists()) {
            Toast.makeText(this, "❌ يوجد حساب بالفعل. لا يمكن إنشاء حساب آخر.", Toast.LENGTH_LONG).show();
            updateUIForUserState();
            return;
        }
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "الرجاء ملء اسم المستخدم وكلمة المرور", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 4) {
            Toast.makeText(this, "كلمة المرور يجب أن تكون 4 أحرف على الأقل", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean created = db.addUser(username, password);
        if (created) {
            saveCredentials(username, password);
            Toast.makeText(this, "✅ تم إنشاء حساب المالك بنجاح!", Toast.LENGTH_LONG).show();
            updateUIForUserState();
            goHome(username);
        } else {
            Toast.makeText(this, "فشل الإنشاء، حاول مرة أخرى", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "الرجاء ملء الحقول", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!db.anyUserExists()) {
            Toast.makeText(this, "لا يوجد حساب. اضغط 'إنشاء حساب المالك' أولاً", Toast.LENGTH_LONG).show();
            return;
        }
        if (db.checkUser(username, password)) {
            saveCredentials(username, password);
            goHome(username);
        } else {
            Toast.makeText(this, "❌ اسم المستخدم أو كلمة المرور غير صحيحة", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePasswordVisibility() {
        int currentType = etPassword.getInputType();
        boolean isVisible = (currentType & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        if (isVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private boolean isBiometricAvailable() {
        BiometricManager bm = BiometricManager.from(this);
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        performFingerprintLogin();
                    }
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Toast.makeText(LoginActivity.this, "خطأ: " + errString, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(LoginActivity.this, "فشل التحقق، حاول مرة أخرى", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("تسجيل الدخول بالبصمة")
                .setSubtitle("ضع إصبعك على مستشعر البصمة")
                .setNegativeButtonText("إلغاء")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void performFingerprintLogin() {
        String username = prefs.getString(KEY_SAVED_USERNAME, "");
        String password = prefs.getString(KEY_SAVED_PASSWORD, "");
        if (!username.isEmpty() && db.checkUser(username, password)) {
            Toast.makeText(this, "✅ تم تسجيل الدخول بالبصمة", Toast.LENGTH_SHORT).show();
            goHome(username);
        } else {
            Toast.makeText(this, "فشل الدخول، يرجى استخدام كلمة المرور", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCredentials(String username, String password) {
        prefs.edit().putString(KEY_SAVED_USERNAME, username).putString(KEY_SAVED_PASSWORD, password).apply();
    }

    private void goHome(String username) {
        Intent i = new Intent(this, HomeActivity.class);
        i.putExtra("username", username);
        startActivity(i);
        finish();
    }
}
