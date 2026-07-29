package com.mycompany.app;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ViewProductsActivity extends BaseActivity {

    private final DecimalFormat nf = new DecimalFormat("#,##0.##");

    EditText etSearchProducts;
	Button btnSearchType,
	btnAddProduct,
	btnEditDeleteProduct,
	btnExportProducts,
	btnImportProducts;
	
    TableLayout tableProducts;
    DatabaseHelper db;
    int searchType = 0;

    private static final int MENU_EXPORT = 100;
    private static final int MENU_IMPORT = 101;
    private static final int REQUEST_EXPORT = 2001;
    private static final int REQUEST_IMPORT = 2002;

    private String pendingExportData = null;
    private String pendingExportFilename = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_products);

        db = new DatabaseHelper(this);
        etSearchProducts = findViewById(R.id.etSearchProducts);
        btnSearchType = findViewById(R.id.btnSearchType);
        tableProducts = findViewById(R.id.tableProducts);

        // ✅ التأكد من وجود الأزرار في ملف XML
        btnAddProduct = findViewById(R.id.btnAddProduct);
        btnEditDeleteProduct = findViewById(R.id.btnEditDeleteProduct);
		btnExportProducts = findViewById(R.id.btnExportProducts);
		btnImportProducts = findViewById(R.id.btnImportProducts);

        if (btnAddProduct != null) {
            btnAddProduct.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(ViewProductsActivity.this, AddProductActivity.class);
						startActivity(i);
					}
				});
        }

        if (btnEditDeleteProduct != null) {
            btnEditDeleteProduct.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(ViewProductsActivity.this, EditProductActivity.class);
						startActivity(i);
					}
				});
        }
		
		if (btnExportProducts != null) {
			btnExportProducts.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					exportDataFlexible();
				}
			});
		}
		
		if (btnImportProducts != null) {
			btnImportProducts.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					importDataFlexible();
				}
			});
		}

        // 🔹 زر تغيير نوع البحث
        btnSearchType.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					searchType = (searchType + 1) % 3;
					switch (searchType) {
						case 0:
							btnSearchType.setText("اسم");
							break;
						case 1:
							btnSearchType.setText("سعر");
							break;
						case 2:
							btnSearchType.setText("كمية");
							break;
					}
					searchProducts(etSearchProducts.getText().toString().trim());
				}
			});

        // 🔹 البحث أثناء الكتابة
        etSearchProducts.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					searchProducts(s.toString().trim());
				}

				@Override
				public void afterTextChanged(Editable s) {}
			});

        searchProducts("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // إعادة تحميل القائمة عند الرجوع من شاشة إضافة/تعديل منتج
        // بحيث تعكس أي تغييرات جديدة في المخزون أو الأسعار فوراً
        if (etSearchProducts != null) {
            searchProducts(etSearchProducts.getText().toString().trim());
        }
    }

    private void searchProducts(String query) {
		
		int childCount = tableProducts.getChildCount();
		
		if (childCount > 1) {
			tableProducts.removeViews(1, childCount - 1);
		}
		
		
		Cursor cursor = db.getAllProducts();
		
		int rowNumber = 1;
		
		
		if (cursor != null) {
			
			
			while (cursor.moveToNext()) {
				
				
				String name = cursor.getString(
				cursor.getColumnIndex("name")
				);
				
				
				String saleType = cursor.getString(
				cursor.getColumnIndex("sale_type")
				);
				
				
				int retailQuantity = cursor.getInt(
				cursor.getColumnIndex("retail_quantity")
				);
				
				
				int wholesaleQuantity = cursor.getInt(
				cursor.getColumnIndex("wholesale_quantity")
				);
				
				
				int unitsPerBox = cursor.getInt(
				cursor.getColumnIndex("units_per_box")
				);
				
				
				double costPrice = cursor.getDouble(
				cursor.getColumnIndex("cost_price")
				);
				
				
				double retailPrice = cursor.getDouble(
				cursor.getColumnIndex("retail_price")
				);
				
				
				double wholesalePrice = cursor.getDouble(
				cursor.getColumnIndex("wholesale_price")
				);
				
				
				
				// البحث بالاسم
				if(searchType == 0 &&
				!name.contains(query))
				continue;
				
				
				
				// البحث بالسعر
				if(searchType == 1 &&
				!String.valueOf(retailPrice + wholesalePrice)
				.startsWith(query))
				continue;
				
				
				
				// البحث بالكمية
				if(searchType == 2 &&
				!String.valueOf(retailQuantity + wholesaleQuantity)
				.startsWith(query))
				continue;
				
				
				
				// حساب الربح بشكل منظم: سطر مستقل لكل نوع بيع (تجزئة/جملة)
				// بدلاً من نص واحد مدموج، لتسهيل القراءة عند وجود النوعين معاً
				StringBuilder profitBuilder = new StringBuilder();
				StringBuilder saleBuilder = new StringBuilder();
				
				boolean hasRetail = retailQuantity > 0 || saleType.equals("retail");
				boolean hasWholesale = wholesaleQuantity > 0 || saleType.equals("wholesale");
				
				double retailUnitCost = costPrice;
				
				if (hasWholesale && unitsPerBox > 0) {
					// عند وجود جملة فقط (بدون وحدات محولة)، سعر الشراء المخزّن هو سعر الكرتون
					// لذا سعر شراء الوحدة الفعلي = سعر الكرتون ÷ عدد الوحدات بالكرتون
					if (!hasRetail || retailQuantity == 0) {
						retailUnitCost = costPrice / unitsPerBox;
					}
				}
				
				if (hasRetail) {
					double retailProfit = retailPrice - (retailQuantity > 0 ? costPrice : retailUnitCost);
					profitBuilder.append("تجزئة: ").append(nf.format(retailProfit)).append(" / وحدة");
					saleBuilder.append("تجزئة: ").append(nf.format(retailPrice)).append(" ر.س");
				}
				
				if (hasWholesale) {
					double wholesaleProfit = wholesalePrice - costPrice;
					if (profitBuilder.length() > 0) profitBuilder.append("\n");
					if (saleBuilder.length() > 0) saleBuilder.append("\n");
					profitBuilder.append("جملة: ").append(nf.format(wholesaleProfit)).append(" / كرتون");
					saleBuilder.append("جملة: ").append(nf.format(wholesalePrice)).append(" ر.س");
				}
				
				String profitText = profitBuilder.toString();
				String saleText = saleBuilder.toString();
				
				TableRow row = new TableRow(this);
				
				row.setPadding(4,4,4,4);
				
				// تلوين حسب المخزون
				int totalStock =
				retailQuantity + wholesaleQuantity;
				
				
				if(totalStock < 5)
				
				row.setBackgroundColor(
				Color.parseColor("#FFCDD2")
				);
				
				else if(totalStock <= 20)
				
				row.setBackgroundColor(
				Color.parseColor("#FFF9C4")
				);
				
				else
				
				row.setBackgroundColor(
				Color.parseColor("#C8E6C9")
				);
				
				TextView tvNumber =
				createCell(
				String.valueOf(rowNumber++)
				);
				
				
				TextView tvName =
				createCell(name);
				
				String typeText;
				
				if(saleType.equals("retail"))
				
				typeText = "تجزئة";
				
				else
				
				typeText = "جملة";
				
				if(retailQuantity > 0 &&
				wholesaleQuantity > 0)
				
				typeText = "تجزئة + جملة";
				
				TextView tvType =
				createCell(typeText);
				
				TextView tvUnits =
				createCell(
				retailQuantity > 0 ?
				String.valueOf(retailQuantity)
				:
				"0"
				);
				
				TextView tvBoxes =
				createCell(
				wholesaleQuantity > 0 ?
				String.valueOf(wholesaleQuantity)
				:
				"0"
				);
				
				TextView tvCost =
				createCell(
				String.valueOf(costPrice)
				);
				
				TextView tvSale =
				createCell(saleText);
				
				TextView tvProfit =
				createCell(profitText);
			
				// ترتيب الأعمدة حسب RTL
				row.addView(tvProfit);
				row.addView(tvSale);
				row.addView(tvCost);
				row.addView(tvBoxes);
				row.addView(tvUnits);
				row.addView(tvType);
				row.addView(tvName);
				row.addView(tvNumber);
				
				tableProducts.addView(row);
				
				
			}
			
			cursor.close();
			
		}
		
	}
    // ========================
    // التصدير والاستيراد المرن باستخدام SAF
    // ========================

    // يبدأ تجهيز بيانات CSV ثم يفتح نافذة اختيار المكان واسم الملف
    private void exportDataFlexible() {
        Cursor cursor = db.getAllProducts();
        if (cursor == null) {
            Toast.makeText(this, "لا توجد بيانات للتصدير", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        // header
        sb.append("id,name,quantity,cost_price,price,wholesale_price,units_per_box\n");
        if (cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndex("id");
			int nameIdx = cursor.getColumnIndex("name");
			int qtyIdx = cursor.getColumnIndex("quantity");
			int costIdx = cursor.getColumnIndex("cost_price");
			int priceIdx = cursor.getColumnIndex("price");
			int wholesaleIdx = cursor.getColumnIndex("wholesale_price");
			int unitsIdx = cursor.getColumnIndex("units_per_box");

            do {
                String id = idIdx != -1 ? String.valueOf(cursor.getInt(idIdx)) : "0";
                String name = nameIdx != -1 ? cursor.getString(nameIdx) : "";
                // sanitize commas/newlines in name so CSV remains simple
                name = name.replace("\n"," ").replace("\r"," ").replace(","," ");
				String qty = String.valueOf(cursor.getInt(qtyIdx));
				
				String cost = String.valueOf(cursor.getDouble(costIdx));
				
				String price = String.valueOf(cursor.getDouble(priceIdx));
				
				String wholesale = String.valueOf(cursor.getDouble(wholesaleIdx));
				
				String units = String.valueOf(cursor.getInt(unitsIdx));

                sb.append(id)
				.append(",")
				.append(name)
				.append(",")
				.append(qty)
				.append(",")
				.append(cost)
				.append(",")
				.append(price)
				.append(",")
				.append(wholesale)
				.append(",")
				.append(units)
				.append("\n");
            } while (cursor.moveToNext());
        }

        try { cursor.close(); } catch (Exception ignored) {}

        pendingExportData = sb.toString();
        pendingExportFilename = "products_export_" + System.currentTimeMillis() + ".csv";

        // اطلب من المستخدم اختيار مكان واسم الملف
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, pendingExportFilename);
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    // يفتح حوار اختيار ملف CSV للاستيراد
    private void importDataFlexible() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*"); // يقبل csv أو text
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    // استقبال نتيجة اختيار المستخدم
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            // المستخدم ألغى أو لم يختَر شيئًا
            return;
        }

        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == REQUEST_EXPORT) {
            // نكتب pendingExportData إلى الـ URI
            if (pendingExportData == null) {
                Toast.makeText(this, "لا توجد بيانات جاهزة للتصدير", Toast.LENGTH_SHORT).show();
                return;
            }
            OutputStream os = null;
            BufferedWriter bw = null;
            try {
                os = getContentResolver().openOutputStream(uri);
                if (os == null) throw new Exception("تعذر فتح المخرج");
                bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                bw.write(pendingExportData);
                bw.flush();
                Toast.makeText(this, "تم التصدير بنجاح", Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(this, "فشل التصدير: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                try { if (bw != null) bw.close(); } catch (Exception ignored) {}
                try { if (os != null) os.close(); } catch (Exception ignored) {}
                pendingExportData = null;
                pendingExportFilename = null;
            }
        } else if (requestCode == REQUEST_IMPORT) {
            // نقرأ الملف المحدد بواسطة المستخدم ونستورد محتواه
            InputStream is = null;
            BufferedReader br = null;
            List<ProductRow> rows = new ArrayList<>();
            try {
                is = getContentResolver().openInputStream(uri);
                if (is == null) throw new Exception("تعذر فتح الملف للقراءة");
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (first) { first = false;
                        if (line.trim().toLowerCase().startsWith("id,")) continue;
                    }
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length < 7) continue;
                    String name = parts[1];
                    int qty = 0;
					
					double costPrice = 0;
					
					double price = 0;
					
					double wholesalePrice = 0;
					
					int unitsPerBox = 1;
                    try {
						qty = Integer.parseInt(parts[2]);
					} catch (Exception ignored) {}
					
					try {
						costPrice = Double.parseDouble(parts[3]);
					} catch (Exception ignored) {}
					
					try {
						price = Double.parseDouble(parts[4]);
					} catch (Exception ignored) {}
					
					try {
						wholesalePrice = Double.parseDouble(parts[5]);
					} catch (Exception ignored) {}
					
					try {
						unitsPerBox = Integer.parseInt(parts[6]);
					} catch (Exception ignored) {}
						
                    rows.add(new ProductRow(
					name,
					qty,
					costPrice,
					price,
					wholesalePrice,
					unitsPerBox));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(this, "فشل قراءة الملف: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                return;
            } finally {
                try { if (br != null) br.close(); } catch (Exception ignored) {}
                try { if (is != null) is.close(); } catch (Exception ignored) {}
            }

            if (rows.isEmpty()) {
                Toast.makeText(this, "الملف لا يحتوي على بيانات صالحة", Toast.LENGTH_SHORT).show();
                return;
            }

            // ابدأ عملية استبدال البيانات في قاعدة البيانات
            SQLiteDatabase wdb = null;
            try {
                wdb = db.getWritableDatabase();
                wdb.beginTransaction();
                // حذف كل السجلات الحالية
                wdb.execSQL("DELETE FROM products");
                // إدراج السجلات من الملف
                for (ProductRow r : rows) {
                    ContentValues cv = new ContentValues();
					
					cv.put("name", r.name);
					
					cv.put("quantity", r.quantity);
					
					cv.put("cost_price", r.costPrice);
					
					cv.put("price", r.price);
					
					cv.put("wholesale_price", r.wholesalePrice);
					
					cv.put("units_per_box", r.unitsPerBox);
					
					wdb.insert("products", null, cv);
                }
                wdb.setTransactionSuccessful();
                Toast.makeText(this, "تم الاستيراد بنجاح (" + rows.size() + " سجلات)", Toast.LENGTH_LONG).show();
                // إعادة تحميل العرض
                searchProducts("");
            } catch (Exception ex) {
                Toast.makeText(this, "خطأ أثناء إدخال البيانات: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                try { if (wdb != null) wdb.endTransaction(); } catch (Exception ignored) {}
            }
        }
    }

    // مساعدة صغيرة لاحتواء صف المنتج من CSV قبل الإدخال
    private static class ProductRow {
		
		String name;
		
		int quantity;
		
		double costPrice;
		
		double price;
		
		double wholesalePrice;
		
		int unitsPerBox;
		
		ProductRow(
		String n,
		int q,
		double c,
		double p,
		double w,
		int u) {
			
			name = n;
			quantity = q;
			costPrice = c;
			price = p;
			wholesalePrice = w;
			unitsPerBox = u;
		}
	}
    


private TextView createCell(String text){
	
	TextView tv = new TextView(this);
	
	tv.setText(text);
	
	tv.setGravity(Gravity.CENTER);
	
	tv.setPadding(8,8,8,8);
	
	tv.setBackgroundResource(
	android.R.drawable.dialog_holo_light_frame
	);
	
	return tv;
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_products, menu);
    return true;
}
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.menu_export) {
        exportDataFlexible();
        return true;
    }

    if (id == R.id.menu_import) {
        importDataFlexible();
        return true;
    }

    return super.onOptionsItemSelected(item);
}

}
