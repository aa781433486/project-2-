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

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mycompany.app.R;
import com.mycompany.app.data.AppDatabase;
import com.mycompany.app.data.Product;

import java.util.ArrayList;
import java.util.List;

// Simple POS activity: search products, scan barcode and add to invoice (POC)
public class PosActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SCAN = 49374; // not used with IntentIntegrator

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
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.PRODUCT_CODE_TYPES);
        integrator.setPrompt("Scan a barcode");
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String code = result.getContents();
            if (code != null) {
                handleScannedBarcode(code);
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
