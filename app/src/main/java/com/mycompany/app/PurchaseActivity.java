package com.mycompany.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PurchaseActivity extends BaseActivity {

    DatabaseHelper db;
    AutoCompleteTextView etCustomer;
    RadioGroup rgPaymentType, rgSaleType;
    RadioButton rbCash, rbDebt, rbRetail, rbWholesale;
    LinearLayout itemsContainer;
    Button btnAddItem, btnComplete;
    TextView tvTotal, tvTotalLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("📋 إنشاء فاتورة");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = new DatabaseHelper(this);

        etCustomer = findViewById(R.id.etCustomer);
        rgPaymentType = findViewById(R.id.rgPaymentType);
        rbCash = findViewById(R.id.rbCash);
        rbDebt = findViewById(R.id.rbDebt);
        rgSaleType = findViewById(R.id.rgSaleType);
        rbRetail = findViewById(R.id.rbRetail);
        rbWholesale = findViewById(R.id.rbWholesale);
        itemsContainer = findViewById(R.id.itemsContainer);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnComplete = findViewById(R.id.btnComplete);
        tvTotal = findViewById(R.id.tvTotal);
        tvTotalLabel = findViewById(R.id.tvTotalLabel);

        setupCustomerAutoComplete();
        setupPaymentTypeListener();

        btnAddItem.setOnClickListener(v -> addItemRow(null, 1, 0.0));
        addItemRow(null, 1, 0.0);
        btnComplete.setOnClickListener(v -> confirmPurchase());
    }

    // ==================== نوع البيع لكل منتج ====================
    // يحدّث تسمية الحقل والسعر ويتحقق من توافق المنتج مع نوع البيع
    // الحالي للفاتورة (تجزئة = وحدات فقط، جملة = كراتين فقط، إلا إذا
    // كان للمنتج مخزون بالوحدات من تحويل كراتين سابقًا).
    private void updateRowForSaleType(View row) {
        AutoCompleteTextView etProduct = row.findViewById(R.id.etProduct);
        EditText etPrice = row.findViewById(R.id.etPrice);
        TextView tvStock = row.findViewById(R.id.tvStock);
        TextView tvQtyLabel = row.findViewById(R.id.tvQtyLabel);

        if (tvQtyLabel != null) {
            tvQtyLabel.setText(isWholesale() ? "الكراتين" : "الوحدات");
        }

        String prodName = etProduct.getText().toString().trim();
        if (prodName.isEmpty()) {
            if (tvStock != null) tvStock.setVisibility(View.GONE);
            return;
        }

        Cursor c = db.searchProductsByNameExact(prodName);
        if (!c.moveToFirst()) {
            c.close();
            return;
        }
        int retailQty = c.getInt(c.getColumnIndex("retail_quantity"));
        int wholesaleQty = c.getInt(c.getColumnIndex("wholesale_quantity"));
        double retailPrice = c.getDouble(c.getColumnIndex("retail_price"));
        double wholesalePrice = c.getDouble(c.getColumnIndex("wholesale_price"));
        c.close();

        if (isWholesale()) {
            if (wholesaleQty <= 0) {
                Toast.makeText(
                        this,
                        "⚠️ \"" + prodName + "\" لا يُباع بالجملة — تمت إزالته من الفاتورة",
                        Toast.LENGTH_LONG)
                        .show();
                etProduct.setText("");
                etPrice.setText("");
                if (tvStock != null) tvStock.setVisibility(View.GONE);
                recalcTotal();
                return;
            }
            etPrice.setText(String.valueOf(wholesalePrice));
            if (tvStock != null) {
                tvStock.setText("متوفر: " + wholesaleQty + " كرتون");
                tvStock.setTextColor(wholesaleQty <= 5 ? Color.RED : Color.parseColor("#43A047"));
                tvStock.setVisibility(View.VISIBLE);
            }
        } else {
            if (retailQty <= 0) {
                Toast.makeText(
                        this,
                        "⚠️ \"" + prodName + "\" يُباع بالكرتون فقط — تمت إزالته من الفاتورة",
                        Toast.LENGTH_LONG)
                        .show();
                etProduct.setText("");
                etPrice.setText("");
                if (tvStock != null) tvStock.setVisibility(View.GONE);
                recalcTotal();
                return;
            }
            etPrice.setText(String.valueOf(retailPrice));
            if (tvStock != null) {
                tvStock.setText("متوفر: " + retailQty + " وحدة");
                tvStock.setTextColor(retailQty <= 5 ? Color.RED : Color.parseColor("#43A047"));
                tvStock.setVisibility(View.VISIBLE);
            }
        }
        recalcTotal();
    }

    private void refreshAllRowsForSaleType() {
        for (int i = 0; i < itemsContainer.getChildCount(); i++) {
            updateRowForSaleType(itemsContainer.getChildAt(i));
        }
    }

    private void setupCustomerAutoComplete() {
        ArrayList<String> customerNames = new ArrayList<>();
        Cursor c = db.getAllDebtors();
        while (c.moveToNext()) customerNames.add(c.getString(c.getColumnIndex("name")));
        c.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, customerNames);
        etCustomer.setAdapter(adapter);
        etCustomer.setThreshold(1);
    }

    private void setupPaymentTypeListener() {
        rgPaymentType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDebt) {
                btnComplete.setBackgroundResource(R.drawable.btn_red_rounded);
                btnComplete.setText("⚠️ إكمال — دين");
            } else {
                btnComplete.setBackgroundResource(R.drawable.btn_green_rounded);
                btnComplete.setText("✅ إكمال — نقد");
            }
        });
        rbCash.setChecked(true);
        rbRetail.setChecked(true);
		rgSaleType.setOnCheckedChangeListener((group, checkedId) -> {
			refreshAllRowsForSaleType();
		});
    }

    private boolean isWholesale() {
        return rbWholesale != null && rbWholesale.isChecked();
    }

    private void addItemRow(String productName, int qty, double price) {
        final View row = LayoutInflater.from(this).inflate(R.layout.purchase_item_row, itemsContainer, false);
        final AutoCompleteTextView etProduct = row.findViewById(R.id.etProduct);
        final EditText etQty = row.findViewById(R.id.etQty);
        final EditText etPrice = row.findViewById(R.id.etPrice);
        final TextView tvStock = row.findViewById(R.id.tvStock);
		final TextView tvQtyLabel = row.findViewById(R.id.tvQtyLabel);
        ImageButton btnRemove = row.findViewById(R.id.btnRemove);

        if (productName != null) etProduct.setText(productName);
        etQty.setText(String.valueOf(qty));
        if (price > 0) etPrice.setText(String.valueOf(price));

        ArrayList<String> productNames = new ArrayList<>();
        Cursor pc = db.getAllProducts();
        while (pc.moveToNext()) productNames.add(pc.getString(pc.getColumnIndex("name")));
        pc.close();
        ArrayAdapter<String> prodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, productNames);
        etProduct.setAdapter(prodAdapter);
        etProduct.setThreshold(1);

        etProduct.setOnItemClickListener((parent, view, position, id) -> {
            String sel = (String) parent.getItemAtPosition(position);
            etProduct.setText(sel);
            etProduct.setSelection(sel.length());
            updateRowForSaleType(row);
        });

        if (tvQtyLabel != null) {
            tvQtyLabel.setText(isWholesale() ? "الكراتين" : "الوحدات");
        }
        if (productName != null) {
            updateRowForSaleType(row);
        }

        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { recalcTotal(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etQty.addTextChangedListener(tw);
        etPrice.addTextChangedListener(tw);

        btnRemove.setOnClickListener(v -> {
            if (itemsContainer.getChildCount() > 1) {
                itemsContainer.removeView(row);
                recalcTotal();
            } else {
                Toast.makeText(this, "يجب أن يكون هناك منتج واحد على الأقل", Toast.LENGTH_SHORT).show();
            }
        });

        itemsContainer.addView(row);
        recalcTotal();
    }

    private void recalcTotal() {
        double total = 0.0;
        for (int i = 0; i < itemsContainer.getChildCount(); i++) {
            View r = itemsContainer.getChildAt(i);
            EditText etQty = r.findViewById(R.id.etQty);
            EditText etPrice = r.findViewById(R.id.etPrice);
            int q = 0; double p = 0;
            try { q = Integer.parseInt(etQty.getText().toString().trim()); } catch (Exception ignored) {}
            try { p = Double.parseDouble(etPrice.getText().toString().trim()); } catch (Exception ignored) {}
            total += q * p;
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);
        tvTotal.setText(nf.format(total) + " ر.س");
    }

    private void confirmPurchase() {
        String customer = etCustomer.getText().toString().trim();
        if (customer.isEmpty()) {
            Toast.makeText(this, "الرجاء إدخال اسم العميل", Toast.LENGTH_SHORT).show();
            return;
        }
        String paymentType = rbDebt.isChecked() ? "دين" : "نقد";
        if (paymentType.equals("دين")) {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ تأكيد البيع بالدَّيْن")
                    .setMessage("سيُسجَّل الدَّيْن باسم: " + customer + "\nهل تريد المتابعة؟")
                    .setPositiveButton("نعم", (dialog, which) -> completePurchase(customer, "دين"))
                    .setNegativeButton("إلغاء", null)
                    .show();
        } else {
            completePurchase(customer, "نقد");
        }
    }

    private void completePurchase(String customer, String paymentType) {
        String saleType = isWholesale() ? "جملة" : "تجزئة";
        ArrayList<String> lines = new ArrayList<>();
        double total = 0.0;

        for (int i = 0; i < itemsContainer.getChildCount(); i++) {
            View r = itemsContainer.getChildAt(i);
            AutoCompleteTextView etProd = r.findViewById(R.id.etProduct);
            EditText etQty = r.findViewById(R.id.etQty);
            EditText etPrice = r.findViewById(R.id.etPrice);

            String prod = etProd.getText().toString().trim();
            if (prod.isEmpty()) continue;
            int q = 0; double p = 0.0;
            try { q = Integer.parseInt(etQty.getText().toString().trim()); } catch (Exception ignored) {}
            try { p = Double.parseDouble(etPrice.getText().toString().trim()); } catch (Exception ignored) {}
            if (q <= 0) continue;

            Cursor cur = db.searchProductsByNameExact(prod);
            if (cur == null || !cur.moveToFirst()) {
                if (cur != null) cur.close();
                Toast.makeText(this, "❌ المنتج \"" + prod + "\" غير موجود", Toast.LENGTH_LONG).show();
                return;
            }
            int retailQty = cur.getInt(cur.getColumnIndex("retail_quantity"));
            int wholesaleQty = cur.getInt(cur.getColumnIndex("wholesale_quantity"));
            cur.close();

            if (isWholesale()) {
                if (wholesaleQty <= 0) {
                    Toast.makeText(this, "❌ \"" + prod + "\" لا يُباع بالجملة", Toast.LENGTH_LONG).show();
                    return;
                }
                if (q > wholesaleQty) {
                    Toast.makeText(this, "❌ كمية غير كافية لـ\"" + prod + "\"\nالمتوفر: " + wholesaleQty + " كرتون", Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                if (retailQty <= 0) {
                    Toast.makeText(this, "❌ \"" + prod + "\" يُباع بالكرتون فقط", Toast.LENGTH_LONG).show();
                    return;
                }
                if (q > retailQty) {
                    Toast.makeText(this, "❌ كمية غير كافية لـ\"" + prod + "\"\nالمتوفر: " + retailQty + " وحدة", Toast.LENGTH_LONG).show();
                    return;
                }
            }

			lines.add(prod + "," + q + "," + p + "," + (q * p));
			
			total += q * p;
        }

        if (lines.isEmpty()) {
            Toast.makeText(this, "الرجاء إضافة منتجات صالحة", Toast.LENGTH_SHORT).show();
            return;
        }

        // خصم الكمية من المخزون: بالكرتون من مخزون الجملة، وبالوحدة من مخزون التجزئة
        for (int i = 0; i < itemsContainer.getChildCount(); i++) {
            View r = itemsContainer.getChildAt(i);
            AutoCompleteTextView etProd = r.findViewById(R.id.etProduct);
            EditText etQty = r.findViewById(R.id.etQty);
            String prod = etProd.getText().toString().trim();
            if (prod.isEmpty()) continue;
            int q = 0;
            try { q = Integer.parseInt(etQty.getText().toString().trim()); } catch (Exception ignored) {}
            if (q > 0) {
                if (isWholesale()) {
                    db.reduceWholesaleQuantityByName(prod, q);
                } else {
                    db.reduceProductQuantityByName(prod, q);
                }
            }
        }

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        StringBuilder sb = new StringBuilder();
        for (String s : lines) sb.append(s).append("\n");
        db.addPurchase(customer, total, date, paymentType, saleType, sb.toString());

        if (paymentType.equals("دين")) db.addOrUpdateDebtor(customer, total);

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        nf.setMaximumFractionDigits(2);
        Toast.makeText(this,
                "✅ تم تسجيل الفاتورة\nالعميل: " + customer
                        + "\nالدفع: " + paymentType + " | النوع: " + saleType
                        + "\nالمجموع: " + nf.format(total) + " ر.س",
                Toast.LENGTH_LONG).show();

        itemsContainer.removeAllViews();
        addItemRow(null, 1, 0.0);
        etCustomer.setText("");
        rbCash.setChecked(true);
        rbRetail.setChecked(true);
        recalcTotal();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
