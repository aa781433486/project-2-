package com.mycompany.app;

import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base class shared by every screen in the app.
 *
 * It centralizes dark/light theme handling so the whole app can switch
 * themes instantly (without forcing the user to restart the app):
 *
 *  - Call {@link #applyTheme()} as the very first line of onCreate(),
 *    before super.onCreate(). It applies the dark theme if the user's
 *    saved preference asks for it, and remembers what was applied.
 *  - onResume() then compares the *currently saved* preference against
 *    what was applied when the activity was created. If the user changed
 *    the setting (e.g. from the Settings screen) while this activity was
 *    in the background, it calls recreate() so the new theme is picked up
 *    immediately the next time this screen becomes visible.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private boolean appliedDarkMode;

    protected void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS, MODE_PRIVATE);
        appliedDarkMode = prefs.getBoolean(LoginActivity.KEY_DARK_MODE, false);
        if (appliedDarkMode) {
            setTheme(R.style.AppTheme_Dark);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS, MODE_PRIVATE);
        boolean currentDarkMode = prefs.getBoolean(LoginActivity.KEY_DARK_MODE, false);
        if (currentDarkMode != appliedDarkMode) {
            recreate();
        }
    }
}
