package com.mycompany.app;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class AddProductActivity extends BaseActivity {
	
    AutoCompleteTextView etName;
	
	// نوع البيع
	RadioGroup rgSaleType;
	RadioButton rbRetail, rbWholesale;
	
	// أقسام الإدخال
	LinearLayout layoutRetail;
	LinearLayout layoutWholesale;
	
	// ===== بيانات التجزئة =====
	EditText etCostPriceRetail;
	EditText etRetailPrice;
	EditText etRetailQuantity;
	
	// ===== بيانات الجملة =====
	EditText etCostPriceWholesale;
	EditText etWholesalePrice;
	EditText etWholesaleQuantity;
	EditText etUnitsPerBox;
	
	// أزرار ورسائل
	Button btnSave;
	TextView tvStockStatus, tvActionHint;
	LinearLayout cardStockInfo;
	
	// قاعدة البيانات
	DatabaseHelper dbHelper;
	
	// حالة المنتج
	boolean isExistingProduct = false;
	int existingId = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("➕ إضافة / تحديث منتج");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DatabaseHelper(this);

        etName = findViewById(R.id.etName);
		
		// نوع البيع
		rgSaleType = findViewById(R.id.rgSaleType);
		rbRetail = findViewById(R.id.rbRetail);
		rbWholesale = findViewById(R.id.rbWholesale);
		
		// أقسام الإدخال
		layoutRetail = findViewById(R.id.layoutRetail);
		layoutWholesale = findViewById(R.id.layoutWholesale);
		
		// بيانات التجزئة
		etCostPriceRetail = findViewById(R.id.etCostPriceRetail);
		etRetailPrice = findViewById(R.id.etRetailPrice);
		etRetailQuantity = findViewById(R.id.etRetailQuantity);
		
		// بيانات الجملة
		etCostPriceWholesale = findViewById(R.id.etCostPriceWholesale);
		etWholesalePrice = findViewById(R.id.etWholesalePrice);
		etWholesaleQuantity = findViewById(R.id.etWholesaleQuantity);
		etUnitsPerBox = findViewById(R.id.etUnitsPerBox);
		
		// عناصر أخرى
		btnSave = findViewById(R.id.btnAdd);
		tvStockStatus = findViewById(R.id.tvStockStatus);
		tvActionHint = findViewById(R.id.tvActionHint);
		cardStockInfo = findViewById(R.id.cardStockInfo);
		
		// تغيير الواجهة حسب نوع البيع
		rgSaleType.setOnCheckedChangeListener((group, checkedId) -> {
			
			if (checkedId == R.id.rbRetail) {
				
				layoutRetail.setVisibility(View.VISIBLE);
				layoutWholesale.setVisibility(View.GONE);
				
				} else if (checkedId == R.id.rbWholesale) {
				
				layoutRetail.setVisibility(View.GONE);
				layoutWholesale.setVisibility(View.VISIBLE);
				
			}
			
		});
		
		// الوضع الافتراضي عند فتح الصفحة
		rbRetail.setChecked(true);
			
		
		
        refreshAutocomplete();

        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkIfProductExists(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etName.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            etName.setText(selected);
            etName.setSelection(selected.length());
            loadProductData(selected);
        });

        btnSave.setOnClickListener(v -> saveProduct());
    }

    private void refreshAutocomplete() {
        ArrayList<String> names = new ArrayList<>();
        Cursor c = dbHelper.getAllProducts();
        while (c.moveToNext()) {
            names.add(c.getString(c.getColumnIndex("name")));
        }
        c.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        etName.setAdapter(adapter);
        etName.setThreshold(1);
    }

    private void checkIfProductExists(String name) {
        if (name.isEmpty()) {
            cardStockInfo.setVisibility(View.GONE);
            isExistingProduct = false;
            existingId = -1;
            btnSave.setText("➕ إضافة منتج جديد");
            btnSave.setBackgroundResource(R.drawable.btn_green_rounded);
            return;
        }
        Cursor c = dbHelper.searchProductsByNameExact(name);
        if (c.moveToFirst()) {
            fillFromCursor(c);
        } else {
            cardStockInfo.setVisibility(View.GONE);
            isExistingProduct = false;
            existingId = -1;
            btnSave.setText("➕ إضافة منتج جديد");
            btnSave.setBackgroundResource(R.drawable.btn_green_rounded);
        }
        c.close();
    }

    private void loadProductData(String name) {
		
		Cursor c = dbHelper.searchProductsByNameExact(name);
		
		if (c.moveToFirst()) {
			
			fillFromCursor(c);
			
			if (rbRetail.isChecked()) {
				etRetailQuantity.requestFocus();
				} else {
				etWholesaleQuantity.requestFocus();
			}
			
		}
		
		c.close();
	}

    private void fillFromCursor(Cursor c) {
		
		existingId = c.getInt(c.getColumnIndex("id"));
		isExistingProduct = true;
		
		String saleType = c.getString(c.getColumnIndex("sale_type"));
		
		int retailQty = c.getInt(c.getColumnIndex("retail_quantity"));
		int wholesaleQty = c.getInt(c.getColumnIndex("wholesale_quantity"));
		
		double cost = c.getDouble(c.getColumnIndex("cost_price"));
		double retailPrice = c.getDouble(c.getColumnIndex("retail_price"));
		double wholesalePrice = c.getDouble(c.getColumnIndex("wholesale_price"));
		
		int units = c.getInt(c.getColumnIndex("units_per_box"));
		
		int stock = retailQty + (wholesaleQty * Math.max(1, units));
		
		if (saleType.equals("retail")) {
			
			rbRetail.setChecked(true);
			
			if (etCostPriceRetail.getText().toString().isEmpty())
			etCostPriceRetail.setText(String.valueOf(cost));
			
			if (etRetailPrice.getText().toString().isEmpty())
			etRetailPrice.setText(String.valueOf(retailPrice));
			
			} else {
			
			rbWholesale.setChecked(true);
			
			if (etCostPriceWholesale.getText().toString().isEmpty())
			etCostPriceWholesale.setText(String.valueOf(cost));
			
			if (etWholesalePrice.getText().toString().isEmpty())
			etWholesalePrice.setText(String.valueOf(wholesalePrice));
			
			if (etUnitsPerBox.getText().toString().isEmpty())
			etUnitsPerBox.setText(String.valueOf(units));
		}
		
		cardStockInfo.setVisibility(View.VISIBLE);
		
		tvStockStatus.setText("📦 المخزون الحالي : " + stock);
		
		tvStockStatus.setTextColor(
		stock <= 5
		? Color.RED
		: Color.parseColor("#43A047"));
		
		tvActionHint.setText("⚡ المنتج موجود وسيتم تحديث بياناته");
		
		btnSave.setText("🔄 تحديث المنتج");
		
		btnSave.setBackgroundResource(R.drawable.btn_blue_rounded);
	}

    private void saveProduct() {
		
		String name = etName.getText().toString().trim();
		
		boolean isRetail = rbRetail.isChecked();
		boolean isWholesale = rbWholesale.isChecked();
		
		
		if (name.isEmpty()) {
			Toast.makeText(this,"أدخل اسم المنتج",Toast.LENGTH_SHORT).show();
			return;
		}
		
		
		String costStr;
		String sellStr;
		String qtyStr;
		String unitsStr = "0";
		
		
		if (isRetail) {
			
			costStr = etCostPriceRetail.getText().toString().trim();
			sellStr = etRetailPrice.getText().toString().trim();
			qtyStr = etRetailQuantity.getText().toString().trim();
			
			} else {
			
			costStr = etCostPriceWholesale.getText().toString().trim();
			sellStr = etWholesalePrice.getText().toString().trim();
			qtyStr = etWholesaleQuantity.getText().toString().trim();
			unitsStr = etUnitsPerBox.getText().toString().trim();
			
		}
		
		
		
		if(costStr.isEmpty() || sellStr.isEmpty() || qtyStr.isEmpty()){
			
			Toast.makeText(this,
			"أكمل جميع الحقول المطلوبة",
			Toast.LENGTH_SHORT).show();
			
			return;
		}
		
		
		
		if(!isRetail && unitsStr.isEmpty()){
			
			Toast.makeText(this,
			"أدخل عدد الوحدات داخل الكرتون",
			Toast.LENGTH_SHORT).show();
			
			return;
		}
		
		
		
		int quantity;
		int unitsPerBox = 0;
		
		double costPrice;
		double sellPrice;
		
		
		try {
			
			quantity = Integer.parseInt(qtyStr);
			
			costPrice = Double.parseDouble(costStr);
			
			sellPrice = Double.parseDouble(sellStr);
			
			
			if(!isRetail){
				
				unitsPerBox = Integer.parseInt(unitsStr);
				
			}
			
			
			}catch(Exception e){
			
			Toast.makeText(this,
			"⚠️ أدخل أرقام صحيحة",
			Toast.LENGTH_SHORT).show();
			
			return;
		}
		
		
		
		if(quantity <= 0){
			
			Toast.makeText(this,
			"الكمية يجب أن تكون أكبر من صفر",
			Toast.LENGTH_SHORT).show();
			
			return;
		}
		
		
		
		String saleType;
		
		int retailQuantity = 0;
		int wholesaleQuantity = 0;
		
		double retailPrice = 0;
		double wholesalePrice = 0;
		
		
		
		if(isRetail){
			
			saleType = "retail";
			
			retailQuantity = quantity;
			
			retailPrice = sellPrice;
			
			
			}else{
			
			saleType = "wholesale";
			
			wholesaleQuantity = quantity;
			
			wholesalePrice = sellPrice;
			
		}
		
		if (isExistingProduct) {
			
			Cursor c = dbHelper.searchProductsByNameExact(name);
			
			if (c.moveToFirst()) {
				
				if (isRetail) {
					
					retailQuantity += c.getInt(
					c.getColumnIndex("retail_quantity"));
					
					} else {
					
					wholesaleQuantity += c.getInt(
					c.getColumnIndex("wholesale_quantity"));
					
				}
				
			}
			
			c.close();
		}
		
		boolean result = dbHelper.addOrUpdateProduct(
		name,
		saleType,
		retailQuantity,
		wholesaleQuantity,
		unitsPerBox,
		costPrice,
		retailPrice,
		wholesalePrice
		);
		
		
		
		if(result){
			
			Toast.makeText(this,
			"✅ تم حفظ المنتج بنجاح",
			Toast.LENGTH_LONG).show();
			
			
			etName.setText("");
			
			etCostPriceRetail.setText("");
			etRetailPrice.setText("");
			etRetailQuantity.setText("");
			
			etCostPriceWholesale.setText("");
			etWholesalePrice.setText("");
			etWholesaleQuantity.setText("");
			etUnitsPerBox.setText("");
			
			
			rgSaleType.clearCheck();
			
			layoutRetail.setVisibility(View.GONE);
			layoutWholesale.setVisibility(View.GONE);
			
			
			refreshAutocomplete();
			
			
			}else{
			
			Toast.makeText(this,
			"❌ حدث خطأ أثناء الحفظ",
			Toast.LENGTH_SHORT).show();
			
		}
		
	}

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
