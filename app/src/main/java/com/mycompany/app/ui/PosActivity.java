package com.mycompany.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mycompany.app.R;
import com.mycompany.app.data.AppDatabase;
import com.mycompany.app.data.Product;

import java.util.ArrayList;
import java.util.List;

// Simple POS activity: search products, scan barcode and add to invoice (POC)
public class PosActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SCAN = 49374; // arbitrary

    private EditText etSearch;
    private Button btnScan;
    private RecyclerView rvProducts;

    private AppDatabase db;
    private List<Product> currentProducts = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos);

        etSearch = findViewById(R.id.et_search);
        btnScan = findViewById(R.id.btn_scan);
        rvProducts = findViewById(R.id.rv_products);

        db = AppDatabase.getInstance(this);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        // load initial products in background
        new Thread(() -> {
            currentProducts = db.productDao().getAll();
            runOnUiThread(() -> rvProducts.setAdapter(new ProductAdapter(currentProducts, this)));
        }).start();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                search(s.toString());
            }
        });

        btnScan.setOnClickListener(v -> startBarcodeScanner());
    }

    private void search(String q) {
        new Thread(() -> {
            List<Product> found = db.productDao().searchByName(q);
            runOnUiThread(() -> rvProducts.setAdapter(new ProductAdapter(found, PosActivity.this)));
        }).start();
    }

    private void startBarcodeScanner() {
        // Use ZXing via IntentIntegrator if present. Fallback: show Toast.
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
            startActivityForResult(intent, REQUEST_CODE_SCAN);
        } catch (Exception ex) {
            Toast.makeText(this, "Barcode scanner not installed or library missing.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && data != null) {
            String code = data.getStringExtra("SCAN_RESULT");
            handleScannedBarcode(code);
        }
    }

    private void handleScannedBarcode(String code) {
        if (code == null) return;
        new Thread(() -> {
            Product p = db.productDao().findByBarcode(code);
            runOnUiThread(() -> {
                if (p == null) {
                    Toast.makeText(PosActivity.this, getString(R.string.msg_no_product_found), Toast.LENGTH_LONG).show();
                } else {
                    // TODO: add to invoice. For POC show toast
                    Toast.makeText(PosActivity.this, p.name + " added to invoice", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}
