package com.mycompany.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeActivity extends BaseActivity {

    TextView tvWelcome;
    TextView tvStatSales, tvStatInvoices, tvStatDebts, tvStatLowStock;
    Button btnProducts, btnPurchase, btnDebts, btnInvoices, btnReports, btnProfit, btnLogout;
    android.widget.ImageButton btnMenu;
    DatabaseHelper db;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        prefs = getSharedPreferences(LoginActivity.PREFS, MODE_PRIVATE);
        db = new DatabaseHelper(this);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvStatSales = findViewById(R.id.tvStatSales);
        tvStatInvoices = findViewById(R.id.tvStatInvoices);
        tvStatDebts = findViewById(R.id.tvStatDebts);
        tvStatLowStock = findViewById(R.id.tvStatLowStock);

        btnProducts = findViewById(R.id.btnProducts);
        btnPurchase = findViewById(R.id.btnPurchase);
        btnDebts = findViewById(R.id.btnDebts);
        btnInvoices = findViewById(R.id.btnInvoices);
        btnReports = findViewById(R.id.btnReports);
        btnProfit = findViewById(R.id.btnProfit);
        btnLogout = findViewById(R.id.btnLogout);
        btnMenu = findViewById(R.id.btnMenu);

        String username = getIntent().getStringExtra("username");
        String storeName = prefs.getString(LoginActivity.KEY_STORE_NAME, "نظام البقالة");
        tvWelcome.setText("🌟 " + storeName + " | مرحباً " + (username != null ? username : ""));
        tvWelcome.setSelected(true);

        loadStats();
        checkLowStock();

        btnProducts.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, ViewProductsActivity.class));
            }
        });

        btnPurchase.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, PurchaseActivity.class));
            }
        });

        btnDebts.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, DebtorsListActivity.class));
            }
        });

        btnInvoices.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, InvoicesActivity.class));
            }
        });

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, ReportsActivity.class));
            }
        });

        btnProfit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, ProfitActivity.class));
            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showOverflowMenu(v);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(HomeActivity.this)
                        .setTitle("تسجيل الخروج")
                        .setMessage("هل تريد تسجيل الخروج؟")
                        .setPositiveButton("نعم", (dialog, which) -> {
                            Intent i = new Intent(HomeActivity.this, LoginActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                            finish();
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);

        int threshold = prefs.getInt(LoginActivity.KEY_LOW_STOCK_THRESHOLD, 5);

        double todaySales = db.getTodaySalesTotal();
        int todayInvoices = db.getTodayInvoiceCount();
        double totalDebts = db.getTotalDebts();
        int lowStockCount = db.getLowStockCount(threshold);

        tvStatSales.setText(nf.format(todaySales) + " ر.س");
        tvStatInvoices.setText(String.valueOf(todayInvoices));
        tvStatDebts.setText(nf.format(totalDebts) + " ر.س");
        tvStatLowStock.setText(String.valueOf(lowStockCount));
    }

    private void checkLowStock() {
        int threshold = prefs.getInt(LoginActivity.KEY_LOW_STOCK_THRESHOLD, 5);
        int lowCount = db.getLowStockCount(threshold);
        if (lowCount > 0) {
            Cursor c = db.getLowStockProducts(threshold);
            StringBuilder sb = new StringBuilder("المنتجات التالية تحتاج إعادة تخزين:\n\n");
            int maxShow = 0;
            while (c.moveToNext() && maxShow < 5) {
                String name = c.getString(c.getColumnIndex("name"));
                int qty = c.getInt(c.getColumnIndex("retail_quantity"));
                sb.append("• ").append(name).append(" (").append(qty).append(" متبقي)\n");
                maxShow++;
            }
            c.close();
            if (lowCount > 5) sb.append("... و").append(lowCount - 5).append(" منتجات أخرى");

            new AlertDialog.Builder(this)
                    .setTitle("⚠️ تنبيه: مخزون منخفض")
                    .setMessage(sb.toString())
                    .setPositiveButton("حسناً", null)
                    .show();
        }
    }

    // القائمة المنسدلة (⋮) أعلى الشاشة: الإعدادات ودليل الاستخدام
    private void showOverflowMenu(View anchor) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_home, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_settings) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                return true;
            } else if (id == R.id.menu_help) {
                startActivity(new Intent(HomeActivity.this, HelpActivity.class));
                return true;
            }
            return false;
        });
        popup.show();
    }
}
