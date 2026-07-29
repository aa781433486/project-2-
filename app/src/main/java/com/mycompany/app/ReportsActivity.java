package com.mycompany.app;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportsActivity extends BaseActivity {

    DatabaseHelper db;
    TextView tvReportDate;
    TextView tvTodaySales, tvTodayInvoices;
    TextView tvMonthSales;
    TextView tvTotalDebts, tvTotalProducts;
    LinearLayout layoutLowStock;
    TextView tvLowStockList;
    Button btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("📊 التقارير");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = new DatabaseHelper(this);

        tvReportDate = findViewById(R.id.tvReportDate);
        tvTodaySales = findViewById(R.id.tvTodaySales);
        tvTodayInvoices = findViewById(R.id.tvTodayInvoices);
        tvMonthSales = findViewById(R.id.tvMonthSales);
        tvTotalDebts = findViewById(R.id.tvTotalDebts);
        tvTotalProducts = findViewById(R.id.tvTotalProducts);
        layoutLowStock = findViewById(R.id.layoutLowStock);
        tvLowStockList = findViewById(R.id.tvLowStockList);
        btnRefresh = findViewById(R.id.btnRefresh);

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadReport();
            }
        });

        loadReport();
    }

    private void loadReport() {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);

        String today = new SimpleDateFormat("yyyy/MM/dd - EEEE", new Locale("ar")).format(new Date());
        tvReportDate.setText("📅 " + today);

        tvTodaySales.setText(nf.format(db.getTodaySalesTotal()) + " ر.س");
        tvTodayInvoices.setText(String.valueOf(db.getTodayInvoiceCount()) + " فاتورة");
        tvMonthSales.setText(nf.format(db.getMonthSalesTotal()) + " ر.س");
        tvTotalDebts.setText(nf.format(db.getTotalDebts()) + " ر.س");
        tvTotalProducts.setText(String.valueOf(db.getProductCount()) + " منتج");

        int threshold = getSharedPreferences(LoginActivity.PREFS, MODE_PRIVATE)
                .getInt(LoginActivity.KEY_LOW_STOCK_THRESHOLD, 5);
        int lowStockCount = db.getLowStockCount(threshold);
        if (lowStockCount > 0) {
            layoutLowStock.setVisibility(View.VISIBLE);
            Cursor c = db.getLowStockProducts(threshold);
            StringBuilder sb = new StringBuilder();
            while (c.moveToNext()) {
                String name = c.getString(c.getColumnIndex("name"));
                int qty = c.getInt(c.getColumnIndex("retail_quantity"));
                double price = c.getDouble(c.getColumnIndex("retail_price"));
                sb.append("• ").append(name)
                        .append(" — الكمية: ").append(qty)
                        .append(" — السعر: ").append(nf.format(price)).append(" ر.س\n");
            }
            c.close();
            tvLowStockList.setText(sb.toString());
        } else {
            layoutLowStock.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
