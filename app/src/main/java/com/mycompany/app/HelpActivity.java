package com.mycompany.app;

import android.os.Bundle;

/**
 * دليل استخدام مبسّط يشرح الشاشات والوظائف الرئيسية في التطبيق.
 */
public class HelpActivity extends BaseActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    applyTheme();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_help);

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle("📖 دليل الاستخدام");
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override
  public boolean onSupportNavigateUp() {
    finish();
    return true;
  }
}
