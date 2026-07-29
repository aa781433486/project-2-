package com.mycompany.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.text.DateFormatSymbols;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import java.io.FileOutputStream;
import android.graphics.Typeface;
import android.graphics.Color;
import android.widget.AutoCompleteTextView;

public class InvoicesActivity extends Activity {

    DatabaseHelper db;
    Spinner spCustomers;
    ListView lvInvoices;
    EditText etSearch; // حقل بحث ديناميكي (إن لم يكن موجود في XML، ننشئه برمجياً)
    ArrayList<String> customerList;
    ArrayAdapter<String> customerAdapter;
    ArrayList<HashMap<String, String>> invoicesList;
    SimpleAdapter invoicesAdapter;
        private TextView tvTotalAllInvoices;

    // request codes for SAF
    private static final int REQUEST_CODE_EXPORT_CSV = 2001;
    private static final int REQUEST_CODE_IMPORT_CSV = 2002;

    // pending export data (CSV text) — نملأها قبل فتح نافذة اختيار المكان
    private String pendingExportData = null;

    // SharedPreferences key for store name
    private static final String PREFS = "app_prefs";
    private static final String KEY_STORE_NAME = "store_name";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_invoices);

                // تهيئة قاعدة البيانات
                db = new DatabaseHelper(this);

                // ربط عناصر الواجهة
                spCustomers = findViewById(R.id.spCustomers);
                lvInvoices = findViewById(R.id.lvInvoices);

                // إنشاء TextView لعرض المجموع الكلي لجميع الفواتير
                tvTotalAllInvoices = new TextView(this);
                tvTotalAllInvoices.setTextSize(18);
                tvTotalAllInvoices.setTypeface(null, Typeface.BOLD);
                tvTotalAllInvoices.setTextColor(Color.parseColor("#1E8449")); // أخضر غامق
                tvTotalAllInvoices.setGravity(Gravity.CENTER);
                tvTotalAllInvoices.setPadding(0, 20, 0, 20);

                // إضافته أسفل التصميم الرئيسي باستخدام LinearLayout من XML
                LinearLayout mainLayout = findViewById(R.id.mainLayout); // تأكد أن لديك android:id="@+id/mainLayout" في XML
                if (mainLayout != null) {
                        mainLayout.addView(tvTotalAllInvoices);
                }

                // إعداد حقل البحث (إذا لم يكن موجودًا في XML سيتم إنشاؤه برمجيًا)
                setupSearchFieldIfNeeded();

                // نطلب اسم المحل أول مرة إن لم يكن محفوظًا
                ensureStoreName();

                // تحميل العملاء (يعيد تحميل الفواتير كذلك للعميل الأول)
                loadCustomers();

                // تفاعل الاختيار في Spinner
                spCustomers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                        if (customerList != null && !customerList.isEmpty()) {
                                                String selectedCustomer = customerList.get(position);
                                                loadInvoices(selectedCustomer);
                                        }
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                        // لا حاجة لأي شيء هنا، فقط نتركه فارغًا
                                }
                        });

                // نحدد سلوك الضغط على عنصر الفاتورة في القائمة
                lvInvoices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View itemView, int position, long id) {
                                        HashMap<String, String> invoice = invoicesList.get(position);
                                        showInvoiceDetails(invoice.get("id"));
                                }
                        });
        }
       

    // إنشاء حقل البحث لو لم يكن موجودا في XML
    private void setupSearchFieldIfNeeded() {
                try {
                        int rid = getResources().getIdentifier("etSearchInvoices", "id", getPackageName());
                        if (rid != 0) {
                                etSearch = findViewById(rid);
                        }
                        if (etSearch == null) {
                                etSearch = new EditText(this);
                                etSearch.setHint("بحث بالتاريخ (مثال: 1/10/2025)");
                                etSearch.setPadding(12, 12, 12, 12);

                                View root = findViewById(android.R.id.content);
                                if (root instanceof ViewGroup) {
                                        ViewGroup vg = (ViewGroup) root;
                                        View spinner = findViewById(R.id.spCustomers);
                                        if (spinner != null && spinner.getParent() instanceof ViewGroup) {
                                                ViewGroup parent = (ViewGroup) spinner.getParent();
                                                int index = parent.indexOfChild(spinner);
                                                parent.addView(etSearch, index + 1);
                                        } else {
                                                vg.addView(etSearch, 0);
                                        }
                                }
                        }
                } catch (Exception ignored) {}

                if (etSearch != null) {
                        etSearch.addTextChangedListener(new TextWatcher() {
                                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                        @Override public void afterTextChanged(Editable s) {}
                                        @Override
                                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                String q = s.toString().trim();
                                                String selectedCustomer = null;
                                                if (customerList != null && !customerList.isEmpty() && spCustomers.getSelectedItemPosition() >= 0) {
                                                        selectedCustomer = customerList.get(spCustomers.getSelectedItemPosition());
                                                }
                                                // فلترة الفواتير باستخدام نفس التنسيق المستخدم في loadInvoices
                                                filterInvoices(selectedCustomer, q);
                                        }
                                });
                }
        }

        private void filterInvoices(String customer, String dateQuery) {
                ArrayList<HashMap<String, String>> invoicesList = new ArrayList<>();
                Cursor c = db.getAllPurchases();
                double totalAll = 0;

                if (c != null) {
                        while (c.moveToNext()) {
                                // قراءة البيانات لكل فاتورة بشكل مستقل
                                String cust = c.getString(c.getColumnIndexOrThrow("customer"));
                                if (customer != null && !customer.equals(cust)) continue;

                                long id = c.getLong(c.getColumnIndexOrThrow("_id"));
                                String dateStr = c.getString(c.getColumnIndexOrThrow("date"));
                                double total = c.getDouble(c.getColumnIndexOrThrow("total"));

                                // تنسيق التاريخ مستقل لكل فاتورة
                                String displayDate = dateStr;
                                try {
                                        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                        Date dt = parseFormat.parse(dateStr);
                                        SimpleDateFormat dateFmt = new SimpleDateFormat("d/M/yyyy", Locale.US);
                                        displayDate = dateFmt.format(dt);
                                } catch (Exception ignored) {}

                                // فلترة حسب التاريخ
                                if (dateQuery != null && !dateQuery.isEmpty() && !displayDate.contains(dateQuery))
                                        continue;

                                // المجموع الكلي منفصل عن البيانات المعروضة
                                totalAll += total;

                                // كل فاتورة لها HashMap مستقل
                                HashMap<String, String> invoiceMap = new HashMap<>();
                                invoiceMap.put("id", String.valueOf(id));
                                invoiceMap.put("date", "📅 التاريخ: " + displayDate);
                                invoiceMap.put("total", "رقم الفاتورة: " + id + " | 💰 الإجمالي: " + String.format(Locale.ENGLISH, "%.2f", total));

                                invoicesList.add(invoiceMap);
                        }
                        c.close();
                }

                // إنشاء Adapter مستقل للقائمة
                SimpleAdapter adapter = new SimpleAdapter(
            this,
            invoicesList,
            android.R.layout.simple_list_item_2,
            new String[]{"date", "total"},
            new int[]{android.R.id.text1, android.R.id.text2}
                );

                adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                                @Override
                                public boolean setViewValue(View view, Object data, String textRepresentation) {
                                        if (view instanceof TextView) {
                                                TextView tv = (TextView) view;
                                                if (view.getId() == android.R.id.text1) {
                                                        tv.setText(data.toString());
                                                        tv.setTextColor(Color.parseColor("#2E86C1"));
                                                        tv.setTextSize(16);
                                                        tv.setTypeface(null, Typeface.BOLD);
                                                        return true;
                                                } else if (view.getId() == android.R.id.text2) {
                                                        tv.setText(data.toString());
                                                        tv.setTextColor(Color.parseColor("#117A65"));
                                                        tv.setTextSize(14);
                                                        return true;
                                                }
                                        }
                                        return false;
                                }
                        });

                lvInvoices.setAdapter(adapter);

                // تحديث المجموع الكلي منفصل
                if (tvTotalAllInvoices != null) {
                        tvTotalAllInvoices.setText(String.format(Locale.ENGLISH, "💵 المجموع الكلي لجميع فواتير العميل: %.2f", totalAll));
                }
        }
        
    // ================== إنشاء القائمة الثلاث نقاط برمجياً ==================
    // ثوابت للأوامر
        private static final int MENU_ID_EXPORT_CSV = 1001;
        private static final int MENU_ID_IMPORT_CSV = 1002;
        private static final int MENU_ID_EDIT_STORE_NAME = 1003;

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                // نضيف العناصر برمجياً لتفادي مشاكل موارد القوائم
                menu.add(0, MENU_ID_EXPORT_CSV, 0, "تصدير الفواتير");
                menu.add(0, MENU_ID_IMPORT_CSV, 1, "استيراد الفواتير");
                menu.add(0, MENU_ID_EDIT_STORE_NAME, 2, "تغيير اسم البقالة");
                return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == MENU_ID_EXPORT_CSV) {
                        exportInvoicesCSV();
                        return true;
                } else if (id == MENU_ID_IMPORT_CSV) {
                        startImportPick(); // اختيار ملف ثم عرض خيار Merge/Replace
                        return true;
                } else if (id == MENU_ID_EDIT_STORE_NAME) {
                        promptForStoreName(); // تغيير اسم البقالة يدوياً
                        return true;
                }

                return super.onOptionsItemSelected(item);
        }

    // ----------------- تأكد من اسم البقالة عند أول تشغيل -----------------
    private void ensureStoreName() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String store = sp.getString(KEY_STORE_NAME, null);
        if (store == null || store.trim().isEmpty()) {
            promptForStoreName();
        }
    }

    private void promptForStoreName() {
        final EditText et = new EditText(this);
        et.setHint("اسم البقالة");
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String cur = sp.getString(KEY_STORE_NAME, "");
        et.setText(cur);

        new AlertDialog.Builder(this)
                .setTitle("ادخل اسم البقالة")
                .setView(et)
                .setCancelable(false)
                .setPositiveButton("حفظ", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        String v = et.getText().toString().trim();
                        if (v.isEmpty()) v = "اسم البقالة";
                        SharedPreferences.Editor ed = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
                        ed.putString(KEY_STORE_NAME, v);
                        ed.apply();
                        Toast.makeText(InvoicesActivity.this, "تم الحفظ: " + v, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    // ----------------- التصدير: تجهيز وفتح نافذة اختيار الملف -----------------
    private void exportInvoicesCSV() {
        try {
            Cursor c = db.getAllPurchases();
            if (c == null || c.getCount() == 0) {
                Toast.makeText(this, "لا توجد فواتير للتصدير", Toast.LENGTH_SHORT).show();
                if (c != null) c.close();
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("customer,date,total,details\n");
            while (c.moveToNext()) {
                String customer = safeGetString(c, "customer");
                String date = safeGetString(c, "date");
                double total = safeGetDouble(c, "total");
                String details = safeGetString(c, "details");
                String detailsEscaped = details.replace("\n", "\\n").replace("\r", "");
                sb.append(customer).append(",").append(date).append(",").append(total).append(",").append(detailsEscaped).append("\n");
            }
            c.close();

            pendingExportData = sb.toString();

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "invoices_export.csv");
            startActivityForResult(intent, REQUEST_CODE_EXPORT_CSV);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "خطأ أثناء تجهيز التصدير: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // يكتب pendingExportData إلى uri
    private void writeInvoicesToCSV(Uri uri) {
        if (uri == null || pendingExportData == null) {
            Toast.makeText(this, "لا توجد بيانات للتصدير", Toast.LENGTH_SHORT).show();
            return;
        }
        OutputStream os = null;
        BufferedWriter bw = null;
        try {
            os = getContentResolver().openOutputStream(uri);
            if (os == null) throw new Exception("تعذر فتح الملف للكتابة");
            bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            bw.write(pendingExportData);
            bw.flush();
            Toast.makeText(this, "تم التصدير بنجاح", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, "فشل التصدير: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try { if (bw != null) bw.close(); } catch (Exception ignored) {}
            try { if (os != null) os.close(); } catch (Exception ignored) {}
            pendingExportData = null;
        }
    }

    // ----------------- الاستيراد: اختيار الملف ثم إظهار خيار الدمج/الاستبدال -----------------
    private void startImportPick() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"text/csv", "text/comma-separated-values", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_CODE_IMPORT_CSV);
    }

    // نقرأ الملف كـ String ثم نعرض حوار الاختيار Merge / Replace
    private void prepareImportDecision(final Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "ملف غير صالح", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder sb = new StringBuilder();
                InputStream is = null;
                BufferedReader br = null;
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
                    }
                    is = getContentResolver().openInputStream(uri);
                    if (is == null) throw new Exception("تعذر فتح الملف");
                    br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(InvoicesActivity.this, "فشل قراءة الملف: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                } finally {
                    try { if (br != null) br.close(); } catch (Exception ignored) {}
                    try { if (is != null) is.close(); } catch (Exception ignored) {}
                }

                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        showImportChoiceDialog(uri, sb.toString());
                    }
                });
            }
        }).start();
    }

    // حوار الخيار Merge/Replace
    private void showImportChoiceDialog(final Uri uri, final String fileContent) {
        new AlertDialog.Builder(this)
                .setTitle("استيراد الفواتير")
                .setMessage("اختر طريقة الاستيراد:\n\n• دمج (Merge): يضيف الفواتير الجديدة ويحدّث المطابقة بدون تكرار\n• استبدال (Replace): يمسح ويستبدل كل الفواتير بالموجود في الملف")
                .setPositiveButton("دمج (Merge)", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        importCsvContent(uri, fileContent, true);
                    }
                })
                .setNegativeButton("استبدال (Replace)", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        importCsvContent(uri, fileContent, false);
                    }
                })
                .setNeutralButton("إلغاء", null)
                .show();
    }

    // تنفيذ الاستيراد (merge=true => دمج/تحديث، false => استبدال كامل)
    private void importCsvContent(final Uri uri, final String fileContent, final boolean merge) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader br = null;
                SQLiteDatabase wdb = null;
                try {
                    br = new BufferedReader(new InputStreamReader(new java.io.ByteArrayInputStream(fileContent.getBytes("UTF-8"))));
                    String line;
                    boolean firstLine = true;

                    wdb = db.getWritableDatabase();
                    wdb.beginTransaction();

                    if (!merge) {
                        // استبدال كامل: حذف كل الفواتير الحالية
                        wdb.delete("purchases", null, null);
                    }

                    while ((line = br.readLine()) != null) {
                        if (firstLine) {
                            firstLine = false;
                            if (line.trim().toLowerCase().startsWith("customer,")) continue;
                        }
                        if (line.trim().isEmpty()) continue;

                        // نجزّئ إلى 4 أجزاء فقط ليبقى details كاملاً حتى لو احتوى فواصل
                        String[] parts = line.split(",", 4);
                        if (parts.length < 4) continue;

                        String customer = parts[0].trim();
                        String date = parts[1].trim();
                        double total = 0;
                        try { total = Double.parseDouble(parts[2].trim()); } catch (Exception ignored) {}
                        String details = parts[3].replace("\\n", "\n");

                        if (merge) {
                            Cursor cursor = wdb.rawQuery("SELECT id FROM purchases WHERE customer=? AND date=? AND total=?",
                                    new String[]{customer, date, String.valueOf(total)});
                            if (cursor != null && cursor.moveToFirst()) {
                                ContentValues cv = new ContentValues();
                                cv.put("details", details);
                                wdb.update("purchases", cv, "customer=? AND date=? AND total=?",
                                        new String[]{customer, date, String.valueOf(total)});
                                cursor.close();
                            } else {
                                ContentValues cv = new ContentValues();
                                cv.put("customer", customer);
                                cv.put("date", date);
                                cv.put("total", total);
                                cv.put("details", details);
                                wdb.insert("purchases", null, cv);
                            }
                        } else {
                            ContentValues cv = new ContentValues();
                            cv.put("customer", customer);
                            cv.put("date", date);
                            cv.put("total", total);
                            cv.put("details", details);
                            wdb.insert("purchases", null, cv);
                        }
                    }

                    wdb.setTransactionSuccessful();
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(InvoicesActivity.this, "تم الاستيراد بنجاح (" + (merge ? "دمج" : "استبدال") + ")", Toast.LENGTH_LONG).show();
                            loadCustomers();
                        }
                    });

                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(InvoicesActivity.this, "فشل الاستيراد: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    try { if (wdb != null) wdb.endTransaction(); } catch (Exception ignored) {}
                    try { if (br != null) br.close(); } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    // ----------------- استقبال نتيجة اختيار الملف/المكان -----------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == REQUEST_CODE_EXPORT_CSV) {
            writeInvoicesToCSV(uri);
        } else if (requestCode == REQUEST_CODE_IMPORT_CSV) {
            prepareImportDecision(uri);
        }
    }

    // ----------------- تحميل العملاء والفواتير -----------------
    private void loadCustomers() {
        customerList = new ArrayList<>();
        Cursor c = db.getAllPurchases();
        ArrayList<String> tempList = new ArrayList<>();
        if (c != null) {
            while (c.moveToNext()) {
                String cust = safeGetString(c, "customer");
                if (!tempList.contains(cust)) tempList.add(cust);
            }
            c.close();
        }
        customerList.addAll(tempList);
        customerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, customerList);
        customerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCustomers.setAdapter(customerAdapter);
        if (!customerList.isEmpty()) {
            spCustomers.setSelection(0);
            loadInvoices(customerList.get(0));
        } else {
            if (invoicesList != null) invoicesList.clear();
            if (invoicesAdapter != null) invoicesAdapter.notifyDataSetChanged();
        }
    }

    private void loadInvoices(String customer) {
                invoicesList = new ArrayList<>();
                Cursor c = db.getAllPurchases();
                double totalAll = 0; // لتخزين مجموع كل الفواتير

                if (c != null) {
                        while (c.moveToNext()) {
                                String cust = safeGetString(c, "customer");
                                if (!cust.equals(customer)) continue;

                                // رقم الفاتورة
                                int idCol = c.getColumnIndex("_id");
                                if (idCol == -1) idCol = c.getColumnIndex("id");
                                String id = idCol != -1 ? String.valueOf(c.getInt(idCol)) : "";

                                // التاريخ
                                String dateStr = safeGetString(c, "date");
                                String displayDate = dateStr;
                                try {
                                        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                        Date dt = parseFormat.parse(dateStr);
                                        SimpleDateFormat dateFmt = new SimpleDateFormat("d/M/yyyy", Locale.US);
                                        displayDate = dateFmt.format(dt);
                                } catch (Exception ignored) {}

                                // المجموع الجزئي للفاتورة
                                double total = safeGetDouble(c, "total");
                                totalAll += total;

                                NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                                nf.setMaximumFractionDigits(2);

                                // إعداد بيانات كل فاتورة بشكل منسق
                                HashMap<String, String> map = new HashMap<>();
                                map.put("date", displayDate);
                                map.put("id", id);
                                map.put("total", nf.format(total));

                                invoicesList.add(map);
                        }
                        c.close();
                }

                // استخدام SimpleAdapter مع ViewBinder لتنسيق كل فاتورة كصندوق
                invoicesAdapter = new SimpleAdapter(
                        this,
                        invoicesList,
                        android.R.layout.simple_list_item_2,
                        new String[]{"date", "total"},
                        new int[]{android.R.id.text1, android.R.id.text2}
                );

                invoicesAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                                @Override
                                public boolean setViewValue(View view, Object data, String textRepresentation) {
                                        if (view instanceof TextView) {
                                                TextView tv = (TextView) view;

                                                if (view.getId() == android.R.id.text1) {
                                                        tv.setText("📅 " + textRepresentation);
                                                        tv.setTextColor(Color.parseColor("#2E86C1"));
                                                        tv.setTextSize(16);
                                                        tv.setTypeface(null, Typeface.BOLD);
                                                        tv.setPadding(20, 20, 20, 10);
                                                        tv.setBackgroundColor(Color.parseColor("#D6EAF8"));
                                                        tv.setGravity(Gravity.START);
                                                        return true;
                                                } else if (view.getId() == android.R.id.text2) {
                                                        tv.setText("💰 الإجمالي: " + textRepresentation);
                                                        tv.setTextColor(Color.parseColor("#117A65"));
                                                        tv.setTextSize(14);
                                                        tv.setTypeface(null, Typeface.NORMAL);
                                                        tv.setPadding(20, 0, 20, 20);
                                                        tv.setBackgroundColor(Color.parseColor("#D6EAF8"));
                                                        tv.setGravity(Gravity.START);
                                                        return true;
                                                }
                                        }
                                        return false;
                                }
                        });

                lvInvoices.setDividerHeight(15); // مسافة بين الفواتير
                lvInvoices.setAdapter(invoicesAdapter);

                // إنشاء TextView لعرض المجموع الكلي أسفل الصفحة
                LinearLayout mainLayout = findViewById(R.id.mainLayout);
                if (mainLayout != null) {
                        if (tvTotalAllInvoices != null) mainLayout.removeView(tvTotalAllInvoices);

                        tvTotalAllInvoices = new TextView(this);
                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        nf.setMaximumFractionDigits(2);
                        tvTotalAllInvoices.setText(String.format(Locale.US,
                                                                                                         "💵 المجموع الكلي لجميع فواتير العميل: %s", nf.format(totalAll)));
                        tvTotalAllInvoices.setTextSize(18);
                        tvTotalAllInvoices.setTextColor(Color.parseColor("#B03A2E"));
                        tvTotalAllInvoices.setTypeface(null, Typeface.BOLD_ITALIC);
                        tvTotalAllInvoices.setGravity(Gravity.CENTER);
                        tvTotalAllInvoices.setPadding(0, 30, 0, 30);

                        mainLayout.addView(tvTotalAllInvoices);
                }
        }

    // ----------------- عرض تفاصيل الفاتورة مع أزرار حذف وتعديل -----------------
    private void showInvoiceDetails(final String invoiceId) {
                try {
                        if (invoiceId == null || invoiceId.trim().isEmpty()) {
                                Toast.makeText(this, "خطأ: رقم الفاتورة غير صالح", Toast.LENGTH_SHORT).show();
                                return;
                        }

                        Cursor purchaseCursor = db.getPurchaseById(Long.parseLong(invoiceId));
                        if (purchaseCursor == null || !purchaseCursor.moveToFirst()) {
                                if (purchaseCursor != null) purchaseCursor.close();
                                Toast.makeText(this, "لم يتم العثور على بيانات الفاتورة", Toast.LENGTH_SHORT).show();
                                return;
                        }

                        final String customerName = safeGetString(purchaseCursor, "customer");
						final String dateStored = safeGetString(purchaseCursor, "date");
						final String detailsText = safeGetString(purchaseCursor, "details");
                        purchaseCursor.close();

                        // ---- إنشاء الحوار وإعداد الـ View (نضمن inflate جديد بدون parent) ----
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("تفاصيل الفاتورة");
                        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_invoice_details, null, false);

                        final TextView tvStoreName = dialogView.findViewById(R.id.tvStoreName);
                        final TextView tvDate = dialogView.findViewById(R.id.tvDate);
                        final TextView tvTime = dialogView.findViewById(R.id.tvTime);
                        final TableLayout table = dialogView.findViewById(R.id.tableProducts);
                        final TextView tvTotal = dialogView.findViewById(R.id.tvTotal);

                        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
                        String storeName = sp.getString(KEY_STORE_NAME, "بقالتي");
                        tvStoreName.setText(storeName);

                        // تحويل التاريخ للعرض (إنجليزي) — إذا كان ممكناً
                        String displayDate = dateStored != null ? dateStored : "";
                        String displayTime = "";
                        if (dateStored != null && !dateStored.trim().isEmpty()) {
                                try {
                                        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                        Date dt = parseFormat.parse(dateStored);
                                        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.US);
                                        displayDate = dateFmt.format(dt);
                                        displayTime = timeFmt.format(dt);
                                } catch (Exception ignored) {
                                        // fallback: split if possible
                                        String[] parts = dateStored.split("\\s+");
                                        if (parts.length >= 2) { displayDate = parts[0]; displayTime = parts[1]; }
                                }
                        }
                        tvDate.setText("Date: " + (displayDate == null ? "" : displayDate));
                        tvTime.setText("Time: " + (displayTime == null ? "" : displayTime));

                        // ---- تفريغ الجدول وإعادة بناء الرأس دائماً (تجنّب أي بقايا من استدعاءات سابقة) ----
                        table.removeAllViews();

                        TableRow header = new TableRow(this);
                        String[] headers = {"المجموع", "السعر", "الكمية", "المنتج"};
                        for (String h : headers) {
                                TextView tv = new TextView(this);
                                tv.setText(h);
                                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                                tv.setPadding(12, 12, 12, 12);
                                tv.setGravity(Gravity.CENTER);
                                tv.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"));
                                tv.setTextColor(android.graphics.Color.WHITE);
                                header.addView(tv);
                        }
                        table.addView(header);

                        // ---- إضافة صفوف العناصر من detailsText (إن وجدت) ----
                        double total = 0.0;
                        if (detailsText != null && !detailsText.trim().isEmpty()) {
                                String[] lines = detailsText.split("\n");
                                for (String line : lines) {
                                        if (line.trim().isEmpty()) continue;
                                        String[] itemParts = line.split(",");
                                        if (itemParts.length >= 4) {
                                                TableRow row = new TableRow(this);
                                                // عرض بترتيب: المجموع - السعر - الكمية - المنتج (كما بالواجهة)
                                                String[] reversed = {
                                                        itemParts[3].trim(),
                                                        itemParts[2].trim(),
                                                        itemParts[1].trim(),
                                                        itemParts[0].trim()
                                                };
                                                for (String p : reversed) {
                                                        TextView tv = new TextView(this);
                                                        tv.setText(p);
                                                        tv.setPadding(12, 12, 12, 12);
                                                        tv.setGravity(Gravity.CENTER);
                                                        tv.setBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"));
                                                        row.addView(tv);
                                                }
                                                table.addView(row);
                                                try { total += Double.parseDouble(itemParts[3].trim()); } catch (Exception ignored) {}
                                        }
                                }
                        }

                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        nf.setMaximumFractionDigits(2);
                        tvTotal.setText(nf.format(total) + ":المجموع الكلي");

                        // ---- أزرار أسفل الجدول: تعديل وحذف ----
                        // ---- أزرار أسفل الجدول: تعديل، حذف، طباعة ----
                        final LinearLayout buttonsRow = new LinearLayout(this);
                        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
                        buttonsRow.setPadding(8, 12, 8, 8);
                        buttonsRow.setGravity(Gravity.CENTER);

                        final Button btnEdit = new Button(this);
                        btnEdit.setText("تعديل");

                        final Button btnDelete = new Button(this);
                        btnDelete.setText("حذف");

                        final Button btnPrint = new Button(this);
                        btnPrint.setText("طباعة"); // زر الطباعة الجديد

                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                        lp.setMargins(8, 0, 8, 0);

                        buttonsRow.addView(btnEdit, lp);
                        buttonsRow.addView(btnDelete, lp);
                        buttonsRow.addView(btnPrint, lp);

// نضيف الزرّات بعد التأكد من عدم وجود parent سابق
                        if (buttonsRow.getParent() != null) {
                                ((ViewGroup) buttonsRow.getParent()).removeView(buttonsRow);
                        }
                        table.addView(buttonsRow);

// ---- استدعاء الطباعة عند الضغط على زر الطباعة ----
                        btnPrint.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                                printInvoice(invoiceId); // هنا نستدعي دالتك الخاصة بالطباعة
                                        }
                                });

                        // ---- بناء وعرض الديالوج (نمرّر الـ dialogView مرة واحدة فقط) ----
                        final AlertDialog dialog = builder.setView(dialogView)
                .setPositiveButton("إغلاق", null)
                .create();
                        dialog.show();

                        // ---- حذف الفاتورة: بعد التأكيد نحذف ونغلق نافذة التفاصيل فورًا ----
                        btnDelete.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                                new AlertDialog.Builder(InvoicesActivity.this)
                                                        .setTitle("تأكيد الحذف")
                                                        .setMessage("هل أنت متأكد أنك تريد حذف هذه الفاتورة نهائيًا؟")
                                                        .setPositiveButton("نعم", new DialogInterface.OnClickListener() {
                                                                @Override public void onClick(DialogInterface confirmDialog, int which) {
                                                                        try {
                                                                                long id = Long.parseLong(invoiceId);
                                                                                boolean ok = db.deletePurchase(id);
                                                                                if (ok) {
                                                                                        Toast.makeText(InvoicesActivity.this, "تم حذف الفاتورة", Toast.LENGTH_SHORT).show();
                                                                                        loadCustomers(); // تحديث القائمة في الخلفية
                                                                                        // إغلاق نافذة التفاصيل فوراً
                                                                                        try { if (dialog.isShowing()) dialog.dismiss(); } catch (Exception ignored) {}
                                                                                } else {
                                                                                        Toast.makeText(InvoicesActivity.this, "فشل الحذف", Toast.LENGTH_SHORT).show();
                                                                                }
                                                                        } catch (Exception e) {
                                                                                e.printStackTrace();
                                                                                Toast.makeText(InvoicesActivity.this, "خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                                        }
                                                                }
                                                        })
                                                        .setNegativeButton("إلغاء", null)
                                                        .show();
                                        }
                                });

                        // ---- زر التعديل: يفتح حوار التعديل، وبعد الانتهاء نعيد تحميل المحتوى داخل نفس الديالوج ----
                        btnEdit.setOnClickListener(new View.OnClickListener() {
                                        @Override public void onClick(View v) {
                                                // استدعاء دالة التعديل التي تقبل Runnable للتحديث بعد الحفظ
                                                showEditInvoiceDialog(invoiceId, customerName, dateStored, detailsText, new Runnable() {
                                                                @Override public void run() {
                                                                        // بعد الحفظ نعيد تحميل بيانات الفاتورة ونعيد بناء الجدول داخل نفس الديالوج
                                                                        Cursor updatedCursor = db.getPurchaseById(Long.parseLong(invoiceId));
                                                                        if (updatedCursor != null && updatedCursor.moveToFirst()) {
                                                                                try {
                                                                                        final String updatedDate = safeGetString(updatedCursor, "date");
                                                                                        final String updatedDetails = safeGetString(updatedCursor, "details");
                                           
                                                                                        // تحديث التاريخ المعروض
                                                                                        String newDisplayDate = updatedDate;
                                                                                        try {
                                                                                                SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                                                                                Date dt = parseFormat.parse(updatedDate);
                                                                                                SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                                                                                newDisplayDate = dateFmt.format(dt);
                                                                                        } catch (Exception ignored) {}

                                                                                        tvDate.setText("Date: " + newDisplayDate);

                                                                                        // إعادة بناء الجدول (تفريغ وإضافة رأس ثم الصفوف)
                                                                                        table.removeAllViews();
                                                                                        TableRow hdr = new TableRow(InvoicesActivity.this);
                                                                                        String[] heads = {"المجموع", "السعر", "الكمية", "المنتج"};
                                                                                        for (String h : heads) {
                                                                                                TextView ttv = new TextView(InvoicesActivity.this);
                                                                                                ttv.setText(h);
                                                                                                ttv.setTypeface(null, android.graphics.Typeface.BOLD);
                                                                                                ttv.setPadding(12, 12, 12, 12);
                                                                                                ttv.setGravity(Gravity.CENTER);
                                                                                                ttv.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"));
                                                                                                ttv.setTextColor(android.graphics.Color.WHITE);
                                                                                                hdr.addView(ttv);
                                                                                        }
                                                                                        table.addView(hdr);
                                                                                        db.recalculateDebtorTotal(customerName);

                                                                                        double newTotal = 0.0;
                                                                                        if (updatedDetails != null && !updatedDetails.trim().isEmpty()) {
                                                                                                String[] lines = updatedDetails.split("\n");
                                                                                                for (String line : lines) {
                                                                                                        if (line.trim().isEmpty()) continue;
                                                                                                        String[] itemParts = line.split(",");
                                                                                                        if (itemParts.length >= 4) {
                                                                                                                TableRow row = new TableRow(InvoicesActivity.this);
                                                                                                                String[] reversed = {
                                                                                                                        itemParts[3].trim(),
                                                                                                                        itemParts[2].trim(),
                                                                                                                        itemParts[1].trim(),
                                                                                                                        itemParts[0].trim()
                                                                                                                };
                                                                                                                for (String p : reversed) {
                                                                                                                        TextView tv = new TextView(InvoicesActivity.this);
                                                                                                                        tv.setText(p);
                                                                                                                        tv.setPadding(12, 12, 12, 12);
                                                                                                                        tv.setGravity(Gravity.CENTER);
                                                                                                                        tv.setBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"));
                                                                                                                        row.addView(tv);
                                                                                                                }
                                                                                                                table.addView(row);
                                                                                                                try { newTotal += Double.parseDouble(itemParts[3].trim()); } catch (Exception ignored) {}
                                                                                                        }
                                                                                                }
                                                                                        }

                                                                                        NumberFormat nf2 = NumberFormat.getNumberInstance(Locale.ENGLISH);
                                                                                        nf2.setMaximumFractionDigits(2);
                                                                                        tvTotal.setText("المجموع الكلي:  " + nf2.format(newTotal));

                                                                                        // أعد إضافة زرّي التعديل والحذف في نهاية الجدول (لأننا مسحنا كل شيء)
                                                                                        if (buttonsRow.getParent() != null) {
                                                                                                
                                                                                                ((ViewGroup) buttonsRow.getParent()).removeView(buttonsRow);
                                                                                        }
                                                                                        table.addView(buttonsRow);

                                                                                } catch (Exception ex) {
                                                                                        ex.printStackTrace();
                                                                                } finally {
                                                                                        updatedCursor.close();
                                                                                }
                                                                        }
                                                                }
                                                        });
                                        }
                                });

                } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "حصل خطأ عند عرض الفاتورة: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
        }
   

        
        // ----------------- تعديل الفاتورة: حوار ملئ بالبيانات -----------------
        private void showEditInvoiceDialog(final String invoiceId, String customer, String date, String details, final Runnable onUpdate) {
                View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit_invoice, null);

                final EditText etCustomer = v.findViewById(R.id.etCustomer);
                final EditText etDate = v.findViewById(R.id.etDate);

                if (date != null && !date.trim().isEmpty()) {
                        try {
                                // التاريخ المخزن في قاعدة البيانات
                                SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                Date dt = parseFormat.parse(date);

                                // صيغة العرض المطلوبة
                                SimpleDateFormat englishFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                String formatted = englishFormat.format(dt);

                                // إجبار الأرقام على أن تكون لاتينية (0-9)
                                formatted = convertArabicIndicDigitsToLatin(formatted);

                                etDate.setText(formatted);
                        } catch (Exception e) {
                                // fallback: إذا فشل التحويل، نحول أي أرقام عربية إلى لاتينية
                                etDate.setText(convertArabicIndicDigitsToLatin(date));
                        }
                } else {
                        etDate.setText("");
                }
                
                final LinearLayout container = v.findViewById(R.id.itemRowsViews);
                final TextView tvTotal = v.findViewById(R.id.tvEditTotal);

                // تحويل كل سطر من details إلى صف مستقل
                if (details != null && !details.trim().isEmpty()) {
                        String[] lines = details.split("\n");
                        for (String line : lines) {
                                if (line.trim().isEmpty()) continue;
                                String[] parts = line.split(",");
                                if (parts.length >= 4) {
                                        addItemRow(container, parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(), tvTotal);
                                }
                        }
                }
                
                

                etCustomer.setText(customer);
                etCustomer.setEnabled(false);
                etDate.setText(date);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("تعديل الفاتورة");
                builder.setView(v);
                builder.setNegativeButton("إلغاء", null);
                builder.setPositiveButton("حفظ", null);

                final AlertDialog dialog = builder.create();
                dialog.show();

                // زر الإضافة
                Button btnAdd = v.findViewById(R.id.btnAddItem);
                btnAdd.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                        addItemRow(container, "", "", "", "", tvTotal);
                                }
                        });

                // زر الحفظ
                Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btnSave.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                        String newDate = etDate.getText().toString().trim();
                                        double newTotal = 0;
                                        StringBuilder sbDetails = new StringBuilder();

                                        for (int i = 0; i < container.getChildCount(); i++) {
                                                LinearLayout row = (LinearLayout) container.getChildAt(i);

                                                EditText etProduct = (EditText) row.getTag(R.id.etProduct);
                                                EditText etQty = (EditText) row.getTag(R.id.etQty);
                                                EditText etPrice = (EditText) row.getTag(R.id.etPrice);
                                                EditText etSubtotal = (EditText) row.getTag(R.id.etSubtotal);

                                                if (etProduct == null || etQty == null || etPrice == null || etSubtotal == null) continue;

                                                String product = etProduct.getText().toString().trim();
                                                String qty = etQty.getText().toString().trim();
                                                String price = etPrice.getText().toString().trim();
                                                String subtotal = etSubtotal.getText().toString().trim();

                                                if (!product.isEmpty()) {
                                                        sbDetails.append(product).append(",")
                                                                .append(qty).append(",")
                                                                .append(price).append(",")
                                                                .append(subtotal).append("\n");
                                                        try { newTotal += Double.parseDouble(subtotal); } catch (Exception ignored) {}
                                                }
                                        }

                                        String newDetails = sbDetails.toString();

                                        try {
                                                ContentValues cv = new ContentValues();
                                                cv.put("date", newDate);
                                                cv.put("details", newDetails);
                                                cv.put("total", newTotal);

                                                SQLiteDatabase wdb = db.getWritableDatabase();
                                                int rows = wdb.update("purchases", cv, "id=?", new String[]{invoiceId});

                                                if (rows > 0) {
                                                        Toast.makeText(InvoicesActivity.this, "تم التحديث", Toast.LENGTH_SHORT).show();
                                                        loadCustomers();
                                                        dialog.dismiss(); // يغلق فقط بعد النجاح
                                                        if (onUpdate != null) onUpdate.run(); // تحديث التفاصيل مباشرة بعد الحفظ
                                                } else {
                                                        Toast.makeText(InvoicesActivity.this, "فشل التحديث", Toast.LENGTH_SHORT).show();
                                                }
                                        } catch (Exception e) {
                                                e.printStackTrace();
                                                Toast.makeText(InvoicesActivity.this, "خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                }
                        });
        }

        private void addItemRow(
		LinearLayout container,
		String product,
		String qty,
		String price,
		String subtotal,
		final TextView tvTotal) {
			
			
			View rowView = LayoutInflater.from(this)
			.inflate(R.layout.row_item_invoice, container, false);
			
			
			final LinearLayout row = (LinearLayout) rowView;
			
			
			final AutoCompleteTextView etProduct = row.findViewById(R.id.etProduct);
			
			
			final EditText etQty = row.findViewById(R.id.etQty);
			etQty.setText(qty);
			
			
			final EditText etPrice = row.findViewById(R.id.etPrice);
			etPrice.setText(price);
			
			
			final EditText etSubtotal = row.findViewById(R.id.etSubtotal);
			etSubtotal.setText(subtotal);
			
			
			row.setTag(R.id.etProduct, etProduct);
			row.setTag(R.id.etQty, etQty);
			row.setTag(R.id.etPrice, etPrice);
			row.setTag(R.id.etSubtotal, etSubtotal);
			
			
			
			final LinearLayout finalContainer = container;
			final TextView finalTvTotal = tvTotal;
			
			
			TextWatcher watcher = new TextWatcher() {
				
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}
				
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}
				
				
				@Override
				public void afterTextChanged(Editable s) {
					
					double qtyVal = 0;
					double priceVal = 0;
					
					
					try {
						qtyVal = Double.parseDouble(
						etQty.getText().toString().trim()
						);
						} catch (Exception ignored) {
					}
					
					
					try {
						priceVal = Double.parseDouble(
						etPrice.getText().toString().trim()
						);
						} catch (Exception ignored) {
					}
					
					
					
					double subTotal = qtyVal * priceVal;
					
					
					etSubtotal.setText(
					String.format(Locale.US, "%.2f", subTotal)
					);
					
					
					
					double total = 0;
					
					
					for (int i = 0; i < finalContainer.getChildCount(); i++) {
						
						LinearLayout r =
						(LinearLayout) finalContainer.getChildAt(i);
						
						
						EditText st =
						(EditText) r.getTag(R.id.etSubtotal);
						
						
						if (st != null) {
							
							try {
								
								total += Double.parseDouble(
								st.getText().toString().trim()
								);
								
								} catch (Exception ignored) {
							}
						}
					}
					
					
					
					finalTvTotal.setText(
					String.format(Locale.US, "المجموع: %.2f", total)
					);
				}
			};
			
			
			
			etQty.addTextChangedListener(watcher);
			etPrice.addTextChangedListener(watcher);
			
			
			
			container.addView(rowView);
		}
        
        // ----------------- مساعدات لقراءة Cursor بأمان -----------------
        private String safeGetString(Cursor c, String col) {
                try {
                        int idx = c.getColumnIndex(col);
                        if (idx == -1) idx = c.getColumnIndexOrThrow(col);
                        String s = c.getString(idx);
                        return s != null ? s : "";
                } catch (Exception e) {
                        return "";
                }
        }

        private double safeGetDouble(Cursor c, String col) {
                try {
                        int idx = c.getColumnIndex(col);
                        if (idx == -1) idx = c.getColumnIndexOrThrow(col);
                        return c.getDouble(idx);
                } catch (Exception e) {
                        return 0.0;
                }
        }
        
        // دالة مساعدة لتحويل الأرقام العربية-الهندية إلى لاتينية
        private String convertArabicIndicDigitsToLatin(String s) {
                if (s == null) return null;
                StringBuilder out = new StringBuilder(s.length());
                for (int i = 0; i < s.length(); ++i) {
                        char ch = s.charAt(i);
                        if (ch >= '\u0660' && ch <= '\u0669') {
                                out.append((char) ('0' + (ch - '\u0660')));
                        } else if (ch >= '\u06F0' && ch <= '\u06F9') {
                                out.append((char) ('0' + (ch - '\u06F0')));
                        } else {
                                out.append(ch);
                        }
                }
                return out.toString();
        }
        
        // ================== طباعة الفاتورة ==================
        private void printInvoice(final String invoiceId) {
                try {
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                                Toast.makeText(this, "الطباعة غير مدعومة على هذا الإصدار", Toast.LENGTH_SHORT).show();
                                return;
                        }

                        if (invoiceId == null || invoiceId.trim().isEmpty()) {
                                Toast.makeText(this, "رقم الفاتورة غير صالح للطباعة", Toast.LENGTH_SHORT).show();
                                return;
                        }

                        Cursor purchaseCursor = db.getPurchaseById(Long.parseLong(invoiceId));
                        if (purchaseCursor == null || !purchaseCursor.moveToFirst()) {
                                if (purchaseCursor != null) purchaseCursor.close();
                                Toast.makeText(this, "لم يتم العثور على بيانات الفاتورة للطباعة", Toast.LENGTH_SHORT).show();
                                return;
                        }

                        final String dateStored = safeGetString(purchaseCursor, "date");
                        final String detailsText = safeGetString(purchaseCursor, "details");
                        final String customerName = safeGetString(purchaseCursor, "customer");
                        final double totalAmount = safeGetDouble(purchaseCursor, "total");
                        purchaseCursor.close();

                        // تحويل التاريخ للإنجليزية
                        String dateEnglish = dateStored;
                        try {
                                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
                                Date date = inputFormat.parse(dateStored);
                                dateEnglish = outputFormat.format(date);
                        } catch (Exception ignored) {}

                        // إنشاء View للطباعة
                        final LinearLayout printLayout = new LinearLayout(this);
                        if (printLayout.getParent() != null) {
                                ((ViewGroup) printLayout.getParent()).removeView(printLayout);
                        }

                        printLayout.setOrientation(LinearLayout.VERTICAL);
                        printLayout.setGravity(Gravity.CENTER_HORIZONTAL); // توسيط كل العناصر
                        int padding = (int) (8 * getResources().getDisplayMetrics().density);
                        printLayout.setPadding(padding, padding, padding, padding);

                        // العنوان
                        TextView tvHeader = new TextView(this);
                        tvHeader.setText("فاتورة");
                        tvHeader.setTextSize(22);
                        tvHeader.setTypeface(null, Typeface.BOLD);
                        tvHeader.setTextColor(Color.parseColor("#2E86C1")); // أزرق
                        tvHeader.setGravity(Gravity.CENTER);
                        printLayout.addView(tvHeader);

                        // اسم العميل
                        TextView tvCustomer = new TextView(this);
                        tvCustomer.setText("اسم العميل: " +customerName );
                        tvCustomer.setTextSize(16);
                        tvCustomer.setGravity(Gravity.CENTER);
                        printLayout.addView(tvCustomer);

                        // التاريخ
                        TextView tvDate = new TextView(this);
                        tvDate.setText(dateEnglish + " :  التاريخ ");
                        tvDate.setTextSize(16);
                        tvDate.setGravity(Gravity.CENTER);
                        printLayout.addView(tvDate);

                        // جدول المنتجات
                        TableLayout table = new TableLayout(this);
                        table.setTextDirection(View.TEXT_DIRECTION_ANY_RTL); // عرض من اليمين لليسار

// رأس الجدول
                        // رأس الجدول
                        TableRow headerRow = new TableRow(this);
                        String[] headers = {"الإجمالي", "السعر", "الكمية", "المنتج"};
                        for (String h : headers) {
                                TextView tv = new TextView(this);
                                tv.setText(h);
                                tv.setTypeface(null, Typeface.BOLD);
                                tv.setPadding(12, 12, 12, 12);
                                tv.setGravity(Gravity.CENTER);
                                tv.setBackgroundColor(Color.parseColor("#D6EAF8")); // ✅ خلفية زرقاء فاتحة
                                tv.setTextColor(Color.BLACK);
                                TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                                tv.setLayoutParams(params);
                                headerRow.addView(tv);
                        }
                        table.addView(headerRow);

// بيانات الجدول
                        if (detailsText != null && !detailsText.trim().isEmpty()) {
                                String[] lines = detailsText.split("\n");
                                for (String line : lines) {
                                        if (line.trim().isEmpty()) continue;
                                        String[] parts = line.split(",");
                                        if (parts.length >= 4) {
                                                TableRow row = new TableRow(this);

                                                // ✅ نفس ترتيب الأعمدة مع الأرقام إنجليزية
                                                String[] rowVals = {
                                                        String.format(Locale.US, "%s", parts[3].trim()),  // الإجمالي
                                                        String.format(Locale.US, "%s", parts[2].trim()),  // السعر
                                                        parts[1].trim(),                                  // الكمية
                                                        parts[0].trim()                                   // المنتج
                                                };

                                                for (String p : rowVals) {
                                                        TextView tv = new TextView(this);
                                                        tv.setText(p);
                                                        tv.setPadding(12, 12, 12, 12);
                                                        tv.setGravity(Gravity.CENTER);
                                                        tv.setTextColor(Color.DKGRAY);
                                                        tv.setBackgroundColor(Color.parseColor("#FDFEFE")); // ✅ خلفية بيضاء
                                                        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                                                        tv.setLayoutParams(params);
                                                        row.addView(tv);
                                                }
                                                table.addView(row);
                                        }
                                }
                        }

// إضافة الجدول إلى الـ Layout الرئيسي
                        printLayout.addView(table);

                        // المجموع
                        TextView tvTotal = new TextView(this);
                        tvTotal.setText("المجموع الكلي : " + String.format(Locale.US, "%.2f", totalAmount));
                        tvTotal.setTextSize(18);
                        tvTotal.setTypeface(null, Typeface.BOLD);
                        tvTotal.setTextColor(Color.parseColor("#C0392B")); // أحمر غامق
                        tvTotal.setGravity(Gravity.CENTER);
                        tvTotal.setPadding(0, 12, 0, 0);
                        printLayout.addView(tvTotal);

                        // تنفيذ الطباعة
                        final PrintManager printManager = (PrintManager) this.getSystemService(PRINT_SERVICE);
                        if (printManager == null) {
                                Toast.makeText(this, "خدمة الطباعة غير متاحة", Toast.LENGTH_SHORT).show();
                                return;
                        }

                        String jobName = "فاتورة_" + invoiceId;

                        printManager.print(jobName, new PrintDocumentAdapter() {
                                        @Override
                                        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                                                                 CancellationSignal cancellationSignal,
                                                                                 LayoutResultCallback callback, Bundle extras) {
                                                if (cancellationSignal != null && cancellationSignal.isCanceled()) {
                                                        callback.onLayoutCancelled();
                                                        return;
                                                }

                                                PrintDocumentInfo info = new PrintDocumentInfo.Builder("invoice_" + invoiceId + ".pdf")
                                                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                                        .setPageCount(1)
                                                        .build();
                                                callback.onLayoutFinished(info, true);
                                        }

                                        @Override
                                        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                                                                CancellationSignal cancellationSignal,
                                                                                WriteResultCallback callback) {
                                                PdfDocument pdfDocument = null;
                                                try {
                                                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(600, 800, 1).create();
                                                        pdfDocument = new PdfDocument();
                                                        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                                                        int w = pageInfo.getPageWidth();
                                                        int h = pageInfo.getPageHeight();
                                                        int ws = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY);
                                                        int hs = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY);
                                                        printLayout.measure(ws, hs);
                                                        printLayout.layout(0, 0, w, h);

                                                        Canvas canvas = page.getCanvas();
                                                        printLayout.draw(canvas);

                                                        pdfDocument.finishPage(page);

                                                        FileOutputStream fos = new FileOutputStream(destination.getFileDescriptor());
                                                        pdfDocument.writeTo(fos);
                                                        fos.flush();
                                                        callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                                                } catch (Exception e) {
                                                        e.printStackTrace();
                                                        try {
                                                                callback.onWriteFailed(e.getMessage());
                                                        } catch (Exception ignored) {}
                                                } finally {
                                                        try { if (pdfDocument != null) pdfDocument.close(); } catch (Exception ignored) {}
                                                }
                                        }
                                }, null);

                } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "خطأ أثناء الطباعة: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
        }
        
}
