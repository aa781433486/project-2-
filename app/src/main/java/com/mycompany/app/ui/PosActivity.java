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
import com.mycompany.app.data.Invoice;
import com.mycompany.app.data.InvoiceItem;
import com.mycompany.app.data.Product;
import com.mycompany.app.util.LanguageHelper;

import java.util.ArrayList;
import java.util.List;

// POS activity: search products, scan barcode and add to invoice (POC)
public class PosActivity extends AppCompatActivity {

    private EditText etSearch;
    private Button btnScan;
    private Button btnLang;
    private RecyclerView rvProducts;

    private AppDatabase db;
    private List<Product> currentProducts = new ArrayList<>();

    // active invoice id
    private long activeInvoiceId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // apply language before setContentView
        LanguageHelper.applyLanguage(this);

        setContentView(R.layout.activity_pos);

        etSearch = findViewById(R.id.et_search);
        btnScan = findViewById(R.id.btn_scan);
        btnLang = findViewById(R.id.btn_lang);
        rvProducts = findViewById(R.id.rv_products);

        db = AppDatabase.getInstance(this);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        // create or open an active invoice
        new Thread(() -> {
            Invoice inv = new Invoice(System.currentTimeMillis(), 0.0, 0.0, "", 0);
            activeInvoiceId = db.invoiceDao().insert(inv);
        }).start();

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

        btnLang.setOnClickListener(v -> toggleLanguage());

        findViewById(R.id.btn_view_invoice).setOnClickListener(v -> {
            // For now just toast invoice id
            Toast.makeText(PosActivity.this, "Invoice ID: " + activeInvoiceId, Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleLanguage() {
        // toggle between ar and en
        // simple implementation: check current locale by reading string resource
        String current = getResources().getConfiguration().locale.getLanguage();
        String next = current.equals("ar") ? "en" : "ar";
        LanguageHelper.setLanguage(this, next);
        // restart activity to apply language
        Intent i = getIntent();
        finish();
        startActivity(i);
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
            if (p == null) {
                runOnUiThread(() -> Toast.makeText(PosActivity.this, getString(R.string.msg_no_product_found), Toast.LENGTH_LONG).show());
                return;
            }

            addProductToInvoice(p);

            runOnUiThread(() -> Toast.makeText(PosActivity.this, p.name + " added to invoice", Toast.LENGTH_LONG).show());
        }).start();
    }

    public void addProductToInvoice(Product p) {
        if (activeInvoiceId <= 0) return;

        new Thread(() -> {
            // check if item exists for this invoice
            InvoiceItem existing = db.invoiceItemDao().findByInvoiceAndProduct(activeInvoiceId, p.id);
            if (existing != null) {
                existing.quantity += 1;
                existing.lineTotal = existing.quantity * existing.unitPrice;
                db.invoiceItemDao().update(existing);
            } else {
                double unitPrice = p.priceRetail;
                double lineTotal = unitPrice * 1;
                double profit = (unitPrice - p.pricePurchase) * 1;
                InvoiceItem item = new InvoiceItem(activeInvoiceId, p.id, 1, unitPrice, lineTotal, profit);
                db.invoiceItemDao().insert(item);
            }

            // update invoice total
            Invoice inv = db.invoiceDao().findById(activeInvoiceId);
            if (inv != null) {
                List<InvoiceItem> items = db.invoiceItemDao().findByInvoice(activeInvoiceId);
                double total = 0.0;
                for (InvoiceItem it : items) total += it.lineTotal;
                inv.total = total;
                db.invoiceDao().update(inv);
            }
        }).start();
    }
}
