package com.mycompany.app;

import android.app.AlertDialog;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class EditProductActivity extends BaseActivity {
	
	AutoCompleteTextView etSearchName;
	
	
	EditText etEditName;
	
	
	// نوع البيع الجديد
	RadioGroup rgEditSaleType;
	RadioButton rbEditRetail;
	RadioButton rbEditWholesale;
	
	
	// التجزئة
	LinearLayout layoutEditRetail;
	
	EditText etEditRetailCost;
	EditText etEditRetailSell;
	EditText etEditRetailQty;
	
	
	// الجملة
	LinearLayout layoutEditWholesale;
	
	EditText etEditBoxCost;
	EditText etEditBoxSell;
	EditText etEditBoxesQty;
	EditText etEditUnitsPerBox;
	
	
	// الكروت والنتائج
	LinearLayout layoutResults;
	LinearLayout layoutSelectedCard;
	
	
	TextView tvSelectedLabel;
	TextView tvCurrentStock;
	
	
	// التحويل
	LinearLayout layoutConvert;
	EditText etConvertBoxes;
	EditText etConvertRetailPrice;
	TextView tvConvertUnitCost;
	
	
	// أزرار
	Button btnUpdate;
	Button btnDelete;
	
	
	int selectedId = -1;

	// بيانات المنتج الأصلية عند التحديد (تُستخدم للتحقق والتحويل)
	String originalSaleType = "retail";
	int originalRetailQty = 0;
	int originalWholesaleQty = 0;
	double currentRetailPrice = 0;

	// حارس لمنع تشغيل مستمع تغيير نوع البيع أثناء إعادة ضبط الاختيار برمجياً
	boolean isRevertingSaleType = false;
	
	
	DatabaseHelper dbHelper;
	
	
	NumberFormat nf;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    applyTheme();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_product);

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle("✏️ تعديل / حذف منتج");
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
    nf.setMaximumFractionDigits(2);

    dbHelper = new DatabaseHelper(this);

    etSearchName = findViewById(R.id.etSearchName);

    etEditName = findViewById(R.id.etEditName);

    // نوع البيع الجديد
	rgEditSaleType = findViewById(R.id.rgEditSaleType);
	
	rbEditRetail = findViewById(R.id.rbEditRetail);
	
	rbEditWholesale = findViewById(R.id.rbEditWholesale);
	
	
	// قسم التجزئة الجديد
	layoutEditRetail = findViewById(R.id.layoutEditRetail);
	
	etEditRetailCost = findViewById(R.id.etEditRetailCost);
	
	etEditRetailSell = findViewById(R.id.etEditRetailSell);
	
	etEditRetailQty = findViewById(R.id.etEditRetailQty);
	
	
	// قسم الجملة الجديد
	layoutEditWholesale = findViewById(R.id.layoutEditWholesale);
	
	etEditBoxCost = findViewById(R.id.etEditBoxCost);
	
	etEditBoxSell = findViewById(R.id.etEditBoxSell);
	
	etEditBoxesQty = findViewById(R.id.etEditBoxesQty);
	
	etEditUnitsPerBox = findViewById(R.id.etEditUnitsPerBox);

    // قسم التحويل
    layoutConvert = findViewById(R.id.layoutConvert);

    etConvertBoxes = findViewById(R.id.etConvertBoxes);

    etConvertRetailPrice = findViewById(R.id.etConvertRetailPrice);

    tvConvertUnitCost = findViewById(R.id.tvConvertUnitCost);

    btnUpdate = findViewById(R.id.btnUpdate);

    btnDelete = findViewById(R.id.btnDelete);

    layoutSelectedCard = findViewById(R.id.layoutSelectedCard);

    layoutResults = findViewById(R.id.layoutResults);

    tvSelectedLabel = findViewById(R.id.tvSelectedLabel);

    tvCurrentStock = findViewById(R.id.tvCurrentStock);

    setupAutocomplete();

    etSearchName.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            searchAndShowResults(s.toString().trim());
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });

    etSearchName.setOnItemClickListener(
        (parent, view, position, id) -> {
          String selected = (String) parent.getItemAtPosition(position);
          selectProduct(selected);
        });

    btnUpdate.setOnClickListener(v -> updateProduct());
    btnDelete.setOnClickListener(v -> deleteProduct());

    // تحديث سعر شراء الوحدة المحسوب تلقائياً كل مرة تتغير بيانات الكرتون
    // أو عدد الكراتين المطلوب تحويلها (سعر شراء الكرتون ÷ عدد الوحدات بالكرتون)
    TextWatcher convertCostWatcher =
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateConvertUnitCostDisplay();
          }

          @Override
          public void afterTextChanged(Editable s) {}
        };
    etEditBoxCost.addTextChangedListener(convertCostWatcher);
    etEditUnitsPerBox.addTextChangedListener(convertCostWatcher);

    // تبديل نوع البيع أثناء التعديل: إظهار حقول التحويل عند تحويل منتج
    // "جملة" إلى "تجزئة" (تحويل بعض الكراتين إلى وحدات)، ومنع التحويل
    // إذا كان المنتج لا يملك مخزوناً قابلاً للتحويل من النوع الآخر.
    rgEditSaleType.setOnCheckedChangeListener(
        (group, checkedId) -> {
          if (isRevertingSaleType) return;

          if (checkedId == R.id.rbEditRetail) {

            if ("wholesale".equals(originalSaleType)) {

              if (originalWholesaleQty > 0) {

                // تحويل جزء من الكراتين إلى تجزئة (لا نفقد بيانات الجملة)
                layoutConvert.setVisibility(View.VISIBLE);
                layoutEditRetail.setVisibility(View.GONE);
                layoutEditWholesale.setVisibility(View.VISIBLE);

                etConvertBoxes.setText("");
                etConvertRetailPrice.setText(
                    currentRetailPrice > 0 ? String.valueOf(currentRetailPrice) : "");
                updateConvertUnitCostDisplay();

              } else {

                // منتج جملة بدون أي كراتين متوفرة للتحويل: لا يمكن التبديل لتجزئة
                revertSaleTypeSelection(R.id.rbEditWholesale);

                new AlertDialog.Builder(this)
                    .setTitle("⚠️ لا يمكن التحويل")
                    .setMessage(
                        "هذا المنتج يُباع بالجملة فقط ولا يوجد كراتين متاحة للتحويل.\n"
                            + "قم أولاً بتحويل كرتون واحد أو أكثر إلى وحدات تجزئة.")
                    .setPositiveButton("حسناً", null)
                    .show();
              }

            } else {

              layoutConvert.setVisibility(View.GONE);
              layoutEditRetail.setVisibility(View.VISIBLE);
              layoutEditWholesale.setVisibility(View.GONE);
            }

          } else if (checkedId == R.id.rbEditWholesale) {

            if ("retail".equals(originalSaleType)) {

              // منتج تجزئة بحت: لا يوجد مسار لتحويله إلى جملة من هذه الشاشة
              revertSaleTypeSelection(R.id.rbEditRetail);

              new AlertDialog.Builder(this)
                  .setTitle("⚠️ لا يمكن التحويل")
                  .setMessage("هذا المنتج يُباع بالتجزئة فقط ولا يمكن تحويله إلى جملة.")
                  .setPositiveButton("حسناً", null)
                  .show();

            } else {

              layoutConvert.setVisibility(View.GONE);
              layoutEditRetail.setVisibility(View.GONE);
              layoutEditWholesale.setVisibility(View.VISIBLE);
            }
          }
        });
  }

  // يعيد ضبط اختيار نوع البيع دون إعادة تشغيل مستمع التغيير (لمنع حلقة لا نهائية)
  private void revertSaleTypeSelection(int radioButtonId) {
    isRevertingSaleType = true;
    rgEditSaleType.check(radioButtonId);
    isRevertingSaleType = false;
  }

  // يحسب ويعرض سعر شراء الوحدة المتوقع بعد التحويل = سعر شراء الكرتون ÷ عدد الوحدات بالكرتون
  private void updateConvertUnitCostDisplay() {
    if (tvConvertUnitCost == null) return;
    try {
      double boxCost = Double.parseDouble(etEditBoxCost.getText().toString().trim());
      int unitsPerBox = Integer.parseInt(etEditUnitsPerBox.getText().toString().trim());
      if (unitsPerBox > 0) {
        double unitCost = boxCost / unitsPerBox;
        tvConvertUnitCost.setText(
            "💡 سعر شراء الوحدة المحسوب: " + nf.format(unitCost) + " ر.س");
        return;
      }
    } catch (Exception ignored) {
    }
    tvConvertUnitCost.setText("💡 سعر شراء الوحدة المحسوب: --");
  }

  private void setupAutocomplete() {
    ArrayList<String> names = new ArrayList<>();
    Cursor c = dbHelper.getAllProducts();
    while (c.moveToNext()) names.add(c.getString(c.getColumnIndex("name")));
    c.close();
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
    etSearchName.setAdapter(adapter);
    etSearchName.setThreshold(1);
  }

  private void searchAndShowResults(String query) {
    if (query.isEmpty()) {
      layoutResults.removeAllViews();
      layoutSelectedCard.setVisibility(View.GONE);
      selectedId = -1;
      return;
    }

    layoutResults.removeAllViews();
    Cursor c = dbHelper.searchProductsByName(query);
    int count = 0;
    while (c.moveToNext() && count < 8) {
      final int id = c.getInt(c.getColumnIndex("id"));
      final String name = c.getString(c.getColumnIndex("name"));
      final String saleType = c.getString(c.getColumnIndex("sale_type"));
      final int retailQty = c.getInt(c.getColumnIndex("retail_quantity"));
      final int wholesaleQty = c.getInt(c.getColumnIndex("wholesale_quantity"));
      final double retailPrice = c.getDouble(c.getColumnIndex("retail_price"));
      final double wholesalePrice = c.getDouble(c.getColumnIndex("wholesale_price"));

      View item =
          buildResultItem(id, name, saleType, retailQty, wholesaleQty, retailPrice, wholesalePrice);
      layoutResults.addView(item);
      count++;
    }
    c.close();
  }

  private View buildResultItem(
      int id,
      String name,
      String saleType,
      int retailQty,
      int wholesaleQty,
      double retailPrice,
      double wholesalePrice) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.VERTICAL);
    row.setPadding(16, 14, 16, 14);
    row.setBackgroundResource(R.drawable.purchase_item_bg);

    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.setMargins(0, 0, 0, 8);
    row.setLayoutParams(lp);

    TextView tvName = new TextView(this);
    tvName.setText("📦 " + name);
    tvName.setTextSize(16f);
    tvName.setTextColor(Color.parseColor("#212121"));
    tvName.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(tvName);

    TextView tvInfo = new TextView(this);
    StringBuilder info = new StringBuilder();
    if (retailQty > 0) info.append("الوحدات: ").append(retailQty)
        .append(" (").append(nf.format(retailPrice)).append(" ر.س)");
    if (wholesaleQty > 0) {
      if (info.length() > 0) info.append(" | ");
      info.append("الكراتين: ").append(wholesaleQty)
          .append(" (").append(nf.format(wholesalePrice)).append(" ر.س)");
    }
    tvInfo.setText(info.toString());
    tvInfo.setTextSize(13f);
    int totalStock = retailQty + wholesaleQty;
    tvInfo.setTextColor(totalStock <= 5 ? Color.RED : Color.parseColor("#757575"));
    row.addView(tvInfo);

    row.setOnClickListener(
	v -> {
		selectProductById(id, name);
		etSearchName.setText(name);
		layoutResults.removeAllViews();
	});
    return row;
  }

  private void selectProduct(String name) {
    Cursor c = dbHelper.searchProductsByNameExact(name);
    if (c.moveToFirst()) {
      int id = c.getInt(c.getColumnIndex("id"));
      selectProductById(id, name);
    }
    c.close();
    layoutResults.removeAllViews();
  }

  private void selectProductById(int id, String name) {
	  
	  selectedId = id;
	  
	  layoutSelectedCard.setVisibility(View.VISIBLE);
	  
	  Cursor c = dbHelper.getProductById(id);
	  
	  if (c.moveToFirst()) {
		  
		  String saleType = c.getString(
		  c.getColumnIndex("sale_type")
		  );
		  
		  tvSelectedLabel.setText("✅ تم اختيار: " + name);
		  
		  
		  int retailQty = c.getInt(
		  c.getColumnIndex("retail_quantity")
		  );
		  
		  int wholesaleQty = c.getInt(
		  c.getColumnIndex("wholesale_quantity")
		  );
		  
		  int units = c.getInt(
		  c.getColumnIndex("units_per_box")
		  );
		  
		  
		  double cost = c.getDouble(
		  c.getColumnIndex("cost_price")
		  );
		  
		  double retailPrice = c.getDouble(
		  c.getColumnIndex("retail_price")
		  );
		  
		  double wholesalePrice = c.getDouble(
		  c.getColumnIndex("wholesale_price")
		  );
		  
		  
		  tvCurrentStock.setText(
		  "الوحدات: " + retailQty +
		  " | الكراتين: " + wholesaleQty
		  );
		  
		  
		  etEditName.setText(name);

		  // نحفظ البيانات الأصلية قبل تفعيل مستمع تغيير نوع البيع
		  originalSaleType = saleType;
		  originalRetailQty = retailQty;
		  originalWholesaleQty = wholesaleQty;
		  currentRetailPrice = retailPrice;

		  etConvertBoxes.setText("");
		  etConvertRetailPrice.setText("");
		  layoutConvert.setVisibility(View.GONE);

		  if ("retail".equals(saleType)) {
			  
			  
			  etEditRetailCost.setText(
			  String.valueOf(cost)
			  );
			  
			  etEditRetailSell.setText(
			  String.valueOf(retailPrice)
			  );
			  
			  etEditRetailQty.setText(
			  String.valueOf(retailQty)
			  );

			  rbEditRetail.setChecked(true);
			  
			  
			  } else {
			  
			  
			  etEditBoxCost.setText(
			  String.valueOf(cost)
			  );
			  
			  etEditBoxSell.setText(
			  String.valueOf(wholesalePrice)
			  );
			  
			  etEditBoxesQty.setText(
			  String.valueOf(wholesaleQty)
			  );
			  
			  etEditUnitsPerBox.setText(
			  String.valueOf(units)
			  );

			  rbEditWholesale.setChecked(true);
			  
		  }
		  
		  
		  c.close();
		  
		  } else {
		  
		  c.close();
		  
		  Toast.makeText(
		  this,
		  "❌ لم يتم العثور على المنتج",
		  Toast.LENGTH_SHORT
		  ).show();
	  }
  }

  private void updateProduct() {
	  
	  if (selectedId == -1) {
		  
		  Toast.makeText(this, "⚠️ اختر منتج أولاً", Toast.LENGTH_SHORT).show();
		  return;
	  }
	  
	  String name = etEditName.getText().toString().trim();
	  
	  if (name.isEmpty()) {
		  
		  Toast.makeText(this, "⚠️ أدخل اسم المنتج", Toast.LENGTH_SHORT).show();
		  return;
	  }
	  
	  boolean isRetail = rbEditRetail.isChecked();

	  // وضع التحويل: منتج جملة أصلاً ونريد تحويل بعض الكراتين إلى تجزئة
	  boolean isConverting =
	  isRetail
	  && "wholesale".equals(originalSaleType)
	  && layoutConvert.getVisibility() == View.VISIBLE;

	  if (isConverting) {
		  convertBoxesToRetail(name);
		  return;
	  }
	  
	  String saleType;
	  
	  int retailQuantity = 0;
	  int wholesaleQuantity = 0;
	  int unitsPerBox = 0;
	  
	  double costPrice = 0;
	  double retailPrice = 0;
	  double wholesalePrice = 0;
	  
	  try {
		  
		  if (isRetail) {
			  
			  saleType = "retail";
			  
			  costPrice = Double.parseDouble(etEditRetailCost.getText().toString().trim());
			  
			  retailPrice = Double.parseDouble(etEditRetailSell.getText().toString().trim());
			  
			  retailQuantity = Integer.parseInt(etEditRetailQty.getText().toString().trim());
			  
			  } else {
			  
			  saleType = "wholesale";
			  
			  costPrice = Double.parseDouble(etEditBoxCost.getText().toString().trim());
			  
			  wholesalePrice = Double.parseDouble(etEditBoxSell.getText().toString().trim());
			  
			  wholesaleQuantity = Integer.parseInt(etEditBoxesQty.getText().toString().trim());
			  
			  unitsPerBox = Integer.parseInt(etEditUnitsPerBox.getText().toString().trim());

			  // نحافظ على وحدات التجزئة الموجودة مسبقاً (نتيجة تحويل سابق)
			  // بدلاً من فقدها عند تعديل بيانات الكرتون فقط
			  retailQuantity = originalRetailQty;
			  retailPrice = currentRetailPrice;
			  
		  }
		  
		  } catch (Exception e) {
		  
		  Toast.makeText(this, "⚠️ تأكد من إدخال جميع البيانات", Toast.LENGTH_SHORT).show();
		  return;
	  }
	  
	  boolean result = dbHelper.updateProductNew(
	  selectedId,
	  name,
	  saleType,
	  retailQuantity,
	  wholesaleQuantity,
	  unitsPerBox,
	  costPrice,
	  retailPrice,
	  wholesalePrice);
	  
	  if (result) {
		  
		  Toast.makeText(this, "✅ تم تحديث المنتج", Toast.LENGTH_SHORT).show();
		  
		  clearSelection();
		  
		  setupAutocomplete();
		  
		  } else {
		  
		  Toast.makeText(this, "❌ فشل التحديث", Toast.LENGTH_SHORT).show();
	  }
  }

  // تحويل عدد من الكراتين (جملة) إلى وحدات تجزئة لمنتج يباع أصلاً بالكرتون فقط.
  // سعر شراء الوحدة يُحسب تلقائياً من سعر شراء الكرتون ÷ عدد الوحدات في الكرتون.
  private void convertBoxesToRetail(String name) {

	  double boxCost;
	  double boxSell;
	  int currentBoxes;
	  int unitsPerBox;

	  try {

		  boxCost = Double.parseDouble(etEditBoxCost.getText().toString().trim());
		  boxSell = Double.parseDouble(etEditBoxSell.getText().toString().trim());
		  currentBoxes = Integer.parseInt(etEditBoxesQty.getText().toString().trim());
		  unitsPerBox = Integer.parseInt(etEditUnitsPerBox.getText().toString().trim());

	  } catch (Exception e) {

		  Toast.makeText(this, "⚠️ تأكد من إدخال بيانات الكرتون كاملة", Toast.LENGTH_SHORT).show();
		  return;
	  }

	  if (unitsPerBox <= 0) {

		  Toast.makeText(this, "⚠️ عدد الوحدات داخل الكرتون غير صحيح", Toast.LENGTH_SHORT).show();
		  return;
	  }

	  String boxesStr = etConvertBoxes.getText().toString().trim();
	  String retailPriceStr = etConvertRetailPrice.getText().toString().trim();

	  if (boxesStr.isEmpty() || retailPriceStr.isEmpty()) {

		  Toast.makeText(
		  this, "⚠️ أدخل عدد الكراتين وسعر بيع الوحدة للتحويل", Toast.LENGTH_SHORT).show();
		  return;
	  }

	  int boxesToConvert;
	  double unitSellPrice;

	  try {

		  boxesToConvert = Integer.parseInt(boxesStr);
		  unitSellPrice = Double.parseDouble(retailPriceStr);

	  } catch (Exception e) {

		  Toast.makeText(this, "⚠️ أدخل أرقام صحيحة", Toast.LENGTH_SHORT).show();
		  return;
	  }

	  if (boxesToConvert <= 0) {

		  Toast.makeText(
		  this, "⚠️ عدد الكراتين المحوّلة يجب أن يكون أكبر من صفر", Toast.LENGTH_SHORT).show();
		  return;
	  }

	  if (boxesToConvert > currentBoxes) {

		  Toast.makeText(
		  this,
		  "❌ لا يمكن تحويل أكثر من عدد الكراتين المتوفرة فعلياً (" + currentBoxes + ")",
		  Toast.LENGTH_LONG)
		  .show();
		  return;
	  }

	  int unitsAdded = boxesToConvert * unitsPerBox;
	  int newWholesaleQty = currentBoxes - boxesToConvert;
	  int newRetailQty = originalRetailQty + unitsAdded;

	  boolean result =
	  dbHelper.updateProductNew(
	  selectedId,
	  name,
	  "wholesale",
	  newRetailQty,
	  newWholesaleQty,
	  unitsPerBox,
	  boxCost,
	  unitSellPrice,
	  boxSell);

	  if (result) {

		  Toast.makeText(
		  this,
		  "✅ تم تحويل " + boxesToConvert + " كرتون إلى " + unitsAdded + " وحدة تجزئة",
		  Toast.LENGTH_LONG)
		  .show();

		  clearSelection();

		  setupAutocomplete();

		  } else {

		  Toast.makeText(this, "❌ فشل التحديث", Toast.LENGTH_SHORT).show();
	  }
  }
  

  private void deleteProduct() {
    if (selectedId == -1) {
      Toast.makeText(this, "⚠️ ابحث عن منتج واختره أولاً", Toast.LENGTH_SHORT).show();
      return;
    }
    String name = etEditName.getText().toString().trim();
    new AlertDialog.Builder(this)
        .setTitle("🗑️ تأكيد الحذف")
        .setMessage(
            "هل أنت متأكد من حذف المنتج:\n\"" + name + "\"؟\nلا يمكن التراجع عن هذا الإجراء!")
        .setPositiveButton(
            "نعم، احذف",
            (dialog, which) -> {
              if (dbHelper.deleteProduct(selectedId)) {
                Toast.makeText(this, "🗑️ تم حذف: " + name, Toast.LENGTH_SHORT).show();
                clearSelection();
                setupAutocomplete();
              } else {
                Toast.makeText(this, "❌ فشل الحذف", Toast.LENGTH_SHORT).show();
              }
            })
        .setNegativeButton("إلغاء", null)
        .show();
  }

  private void clearSelection() {
	  
	  selectedId = -1;
	  
	  etSearchName.setText("");
	  
	  etEditName.setText("");
	  
	  etEditRetailCost.setText("");
	  
	  etEditRetailSell.setText("");
	  
	  etEditRetailQty.setText("");
	  
	  etEditBoxCost.setText("");
	  
	  etEditBoxSell.setText("");
	  
	  etEditBoxesQty.setText("");
	  
	  etEditUnitsPerBox.setText("");
	  
	  etConvertBoxes.setText("");
	  
	  etConvertRetailPrice.setText("");

	  if (tvConvertUnitCost != null) tvConvertUnitCost.setText("💡 سعر شراء الوحدة المحسوب: --");

	  originalSaleType = "retail";
	  originalRetailQty = 0;
	  originalWholesaleQty = 0;
	  currentRetailPrice = 0;

	  layoutConvert.setVisibility(View.GONE);
	  
	  layoutSelectedCard.setVisibility(View.GONE);
	  
	  layoutResults.removeAllViews();
  }

  @Override
  public boolean onSupportNavigateUp() {
    finish();
    return true;
  }
}
