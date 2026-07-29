package com.mycompany.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;

public class SettingsActivity extends BaseActivity {

    SharedPreferences prefs;
    Switch switchFingerprint, switchDarkMode;
    EditText etStoreName, etLowStockThreshold;
    Button btnSave;
    TextView tvFingerprintStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(LoginActivity.PREFS, MODE_PRIVATE);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("⚙️ الإعدادات");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        switchFingerprint = findViewById(R.id.switchFingerprint);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        etStoreName = findViewById(R.id.etStoreName);
        etLowStockThreshold = findViewById(R.id.etLowStockThreshold);
        btnSave = findViewById(R.id.btnSaveSettings);
        tvFingerprintStatus = findViewById(R.id.tvFingerprintStatus);

        switchFingerprint.setChecked(prefs.getBoolean(LoginActivity.KEY_FINGERPRINT_ENABLED, false));
        switchDarkMode.setChecked(prefs.getBoolean(LoginActivity.KEY_DARK_MODE, false));
        etStoreName.setText(prefs.getString(LoginActivity.KEY_STORE_NAME, "بقالتي"));
        etLowStockThreshold.setText(String.valueOf(
                prefs.getInt(LoginActivity.KEY_LOW_STOCK_THRESHOLD, 5)));

        BiometricManager bm = BiometricManager.from(this);
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                != BiometricManager.BIOMETRIC_SUCCESS) {
            switchFingerprint.setEnabled(false);
            switchFingerprint.setChecked(false);
            tvFingerprintStatus.setText("⚠️ مستشعر البصمة غير متاح على هذا الجهاز");
            tvFingerprintStatus.setVisibility(View.VISIBLE);
        } else {
            String savedUser = prefs.getString(LoginActivity.KEY_SAVED_USERNAME, "");
            if (savedUser.isEmpty()) {
                tvFingerprintStatus.setText("ℹ️ سجّل الدخول بكلمة المرور أولاً لتفعيل البصمة");
                tvFingerprintStatus.setVisibility(View.VISIBLE);
            } else {
                tvFingerprintStatus.setVisibility(View.GONE);
            }
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        boolean fingerprintEnabled = switchFingerprint.isEnabled() && switchFingerprint.isChecked();
        boolean darkMode = switchDarkMode.isChecked();
        String storeName = etStoreName.getText().toString().trim();
        if (storeName.isEmpty()) storeName = "بقالتي";

        int lowStockThreshold;
        try {
            lowStockThreshold = Integer.parseInt(etLowStockThreshold.getText().toString().trim());
            if (lowStockThreshold < 0) lowStockThreshold = 5;
        } catch (Exception e) {
            lowStockThreshold = 5;
        }

        prefs.edit()
                .putBoolean(LoginActivity.KEY_FINGERPRINT_ENABLED, fingerprintEnabled)
                .putBoolean(LoginActivity.KEY_DARK_MODE, darkMode)
                .putString(LoginActivity.KEY_STORE_NAME, storeName)
                .putInt(LoginActivity.KEY_LOW_STOCK_THRESHOLD, lowStockThreshold)
                .apply();

        Toast.makeText(this, "✅ تم حفظ الإعدادات", Toast.LENGTH_SHORT).show();

        // تطبيق الوضع الداكن/الفاتح فوراً دون الحاجة لإعادة تشغيل التطبيق
        recreate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
