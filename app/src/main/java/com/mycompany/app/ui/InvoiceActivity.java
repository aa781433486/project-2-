package com.mycompany.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mycompany.app.R;
import com.mycompany.app.data.AppDatabase;
import com.mycompany.app.data.Invoice;
import com.mycompany.app.data.InvoiceItem;
import com.mycompany.app.data.Product;

import java.util.ArrayList;
import java.util.List;

public class InvoiceActivity extends AppCompatActivity {

    public static final String EXTRA_INVOICE_ID = "invoice_id";

    private RecyclerView rvItems;
    private TextView tvTotal;
    private Button btnSave;

    private AppDatabase db;
    private long invoiceId = -1;
    private List<InvoiceItem> items = new ArrayList<>();
    private InvoiceItemAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice);

        rvItems = findViewById(R.id.rv_invoice_items);
        tvTotal = findViewById(R.id.tv_total);
        btnSave = findViewById(R.id.btn_save_invoice);

        db = AppDatabase.getInstance(this);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InvoiceItemAdapter(items, this, id -> removeItem(id));
        rvItems.setAdapter(adapter);

        invoiceId = getIntent().getLongExtra(EXTRA_INVOICE_ID, -1);
        if (invoiceId <= 0) {
            Toast.makeText(this, "Invalid invoice", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadItems();

        btnSave.setOnClickListener(v -> {
            Toast.makeText(InvoiceActivity.this, "Invoice saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadItems() {
        new Thread(() -> {
            List<InvoiceItem> dbItems = db.invoiceItemDao().findByInvoice(invoiceId);
            items.clear();
            items.addAll(dbItems);
            double total = 0.0;
            for (InvoiceItem it : items) total += it.lineTotal;
            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                tvTotal.setText(String.format("%.2f", total));
            });
        }).start();
    }

    private void removeItem(long itemId) {
        new Thread(() -> {
            // delete item
            for (InvoiceItem it : items) {
                if (it.id == itemId) {
                    db.invoiceItemDao().delete(it);
                    break;
                }
            }
            // refresh
            loadItems();
        }).start();
    }
}
