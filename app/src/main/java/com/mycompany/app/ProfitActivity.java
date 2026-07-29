package com.mycompany.app;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * شاشة تقرير الأرباح: يومي / أسبوعي / شهري / سنوي.
 *
 * لا يوجد في قاعدة البيانات أي حفظ لتاريخ تكلفة كل منتج وقت البيع، لذا يتم
 * حساب الربح تقريبياً باستخدام سعر التكلفة الحالي لكل منتج مطبّقاً على
 * الكميات التاريخية المخزّنة في تفاصيل كل فاتورة (جدول purchases). هذا
 * يعني أن الأرباح القديمة قد تتغيّر إذا عدّل المستخدم سعر شراء منتج ما.
 */
public class ProfitActivity extends BaseActivity {

  private DatabaseHelper db;
  private final DecimalFormat nf = new DecimalFormat("#,##0.##");

  private static final String[] WEEKDAY_NAMES_AR = {
    "", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
  };
  private static final String[] MONTH_NAMES_AR = {
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
  };

  // منتج مبسّط يحمل فقط البيانات اللازمة لحساب تكلفة الوحدة/الكرتون
  private static class ProductCostInfo {
    double costPrice;
    int unitsPerBox;
    String saleType;
  }

  // سطر واحد من فاتورة: اسم المنتج، الكمية، سعر البيع للوحدة/الكرتون
  private static class InvoiceLine {
    String productName;
    int qty;
    double unitPrice;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    applyTheme();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_profit);

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle("💹 الربح");
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    db = new DatabaseHelper(this);

    loadProfitReport();
  }

  @Override
  protected void onResume() {
    super.onResume();
    loadProfitReport();
  }

  @Override
  public boolean onSupportNavigateUp() {
    finish();
    return true;
  }

  private void loadProfitReport() {
    Map<String, ProductCostInfo> products = loadProductCostMap();
    List<PurchaseRecord> purchases = loadPurchaseRecords();

    Calendar now = Calendar.getInstance();

    renderDaily(purchases, products, now);
    renderWeekly(purchases, products, now);
    renderMonthly(purchases, products, now);
    renderYearly(purchases, products, now);
  }

  // ==================== تحميل البيانات ====================

  private Map<String, ProductCostInfo> loadProductCostMap() {
    Map<String, ProductCostInfo> map = new HashMap<>();
    Cursor cursor = db.getAllProducts();
    if (cursor != null) {
      int nameIdx = cursor.getColumnIndex("name");
      int costIdx = cursor.getColumnIndex("cost_price");
      int unitsIdx = cursor.getColumnIndex("units_per_box");
      int saleTypeIdx = cursor.getColumnIndex("sale_type");
      while (cursor.moveToNext()) {
        ProductCostInfo info = new ProductCostInfo();
        info.costPrice = costIdx != -1 ? cursor.getDouble(costIdx) : 0;
        info.unitsPerBox = unitsIdx != -1 ? cursor.getInt(unitsIdx) : 0;
        info.saleType = saleTypeIdx != -1 ? cursor.getString(saleTypeIdx) : "retail";
        String name = nameIdx != -1 ? cursor.getString(nameIdx) : null;
        if (name != null) map.put(name, info);
      }
      cursor.close();
    }
    return map;
  }

  private static class PurchaseRecord {
    Date date;
    String saleType; // "تجزئة" أو "جملة"
    List<InvoiceLine> lines = new ArrayList<>();
  }

  private List<PurchaseRecord> loadPurchaseRecords() {
    List<PurchaseRecord> records = new ArrayList<>();
    Cursor cursor = db.getAllPurchases();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    if (cursor != null) {
      int dateIdx = cursor.getColumnIndex("date");
      int saleTypeIdx = cursor.getColumnIndex("sale_type");
      int detailsIdx = cursor.getColumnIndex("details");
      while (cursor.moveToNext()) {
        String dateStr = dateIdx != -1 ? cursor.getString(dateIdx) : null;
        if (TextUtils.isEmpty(dateStr)) continue;

        Date date;
        try {
          date = sdf.parse(dateStr);
        } catch (ParseException e) {
          continue;
        }
        if (date == null) continue;

        PurchaseRecord record = new PurchaseRecord();
        record.date = date;
        record.saleType = saleTypeIdx != -1 ? cursor.getString(saleTypeIdx) : "تجزئة";

        String details = detailsIdx != -1 ? cursor.getString(detailsIdx) : null;
        if (!TextUtils.isEmpty(details)) {
          for (String line : details.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            if (parts.length < 3) continue;
            try {
              InvoiceLine il = new InvoiceLine();
              il.productName = parts[0];
              il.qty = Integer.parseInt(parts[1].trim());
              il.unitPrice = Double.parseDouble(parts[2].trim());
              record.lines.add(il);
            } catch (Exception ignored) {
            }
          }
        }
        records.add(record);
      }
      cursor.close();
    }
    return records;
  }

  // حساب تكلفة الوحدة الواحدة المباعة في هذا السطر (وحدة تجزئة أو كرتون جملة)
  private double lineUnitCost(InvoiceLine line, String purchaseSaleType, Map<String, ProductCostInfo> products) {
    ProductCostInfo info = products.get(line.productName);
    if (info == null) return 0;

    boolean soldAsWholesale = "جملة".equals(purchaseSaleType);

    if (soldAsWholesale) {
      // سعر شراء الكرتون مخزّن مباشرة كتكلفة للمنتج ذو نوع البيع "wholesale"
      return info.costPrice;
    }

    // بيع بالتجزئة: إذا كان أصل المنتج جملة (التكلفة مخزّنة على مستوى الكرتون)
    // نحسب تكلفة الوحدة = تكلفة الكرتون ÷ عدد الوحدات بالكرتون
    if ("wholesale".equals(info.saleType) && info.unitsPerBox > 0) {
      return info.costPrice / info.unitsPerBox;
    }

    return info.costPrice;
  }

  private double lineProfit(InvoiceLine line, String purchaseSaleType, Map<String, ProductCostInfo> products) {
    double unitCost = lineUnitCost(line, purchaseSaleType, products);
    return line.qty * (line.unitPrice - unitCost);
  }

  // ==================== التجميع والعرض ====================

  private void renderDaily(List<PurchaseRecord> purchases, Map<String, ProductCostInfo> products, Calendar now) {
    int days = 7;
    double[] totals = new double[days];
    String[] labels = new String[days];

    Calendar cal = (Calendar) now.clone();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    // نبني تاريخ بداية كل يوم من الأقدم إلى الأحدث (اليوم في النهاية)
    Calendar[] dayStarts = new Calendar[days];
    for (int i = days - 1; i >= 0; i--) {
      Calendar c = (Calendar) cal.clone();
      c.add(Calendar.DAY_OF_YEAR, -(days - 1 - i));
      dayStarts[i] = c;
      int weekday = c.get(Calendar.DAY_OF_WEEK);
      labels[i] = WEEKDAY_NAMES_AR[weekday] + " " + c.get(Calendar.DAY_OF_MONTH) + "/" + (c.get(Calendar.MONTH) + 1);
    }

    for (PurchaseRecord p : purchases) {
      Calendar pc = Calendar.getInstance();
      pc.setTime(p.date);
      for (int i = 0; i < days; i++) {
        Calendar dayStart = dayStarts[i];
        Calendar dayEnd = (Calendar) dayStart.clone();
        dayEnd.add(Calendar.DAY_OF_YEAR, 1);
        if (!p.date.before(dayStart.getTime()) && p.date.before(dayEnd.getTime())) {
          totals[i] += sumProfit(p, products);
          break;
        }
      }
    }

    double todayTotal = totals[days - 1];

    android.widget.TextView tvProfit = findViewById(R.id.tvDayProfit);
    tvProfit.setText(nf.format(todayTotal) + " ر.س");

    StringBuilder sb = new StringBuilder();
    for (int i = days - 1; i >= 0; i--) {
      sb.append(labels[i]).append(": ").append(nf.format(totals[i])).append(" ر.س");
      if (i > 0) sb.append("\n");
    }
    ((android.widget.TextView) findViewById(R.id.tvDayList)).setText(sb.toString());

    setChartData(R.id.chartDay, totals);
  }

  private void renderWeekly(List<PurchaseRecord> purchases, Map<String, ProductCostInfo> products, Calendar now) {
    int weeks = 6;
    double[] totals = new double[weeks];
    String[] labels = new String[weeks];

    Calendar cal = (Calendar) now.clone();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    cal.setFirstDayOfWeek(Calendar.SUNDAY);
    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

    Calendar[] weekStarts = new Calendar[weeks];
    for (int i = weeks - 1; i >= 0; i--) {
      Calendar c = (Calendar) cal.clone();
      c.add(Calendar.WEEK_OF_YEAR, -(weeks - 1 - i));
      weekStarts[i] = c;
      labels[i] = (c.get(Calendar.DAY_OF_MONTH)) + "/" + (c.get(Calendar.MONTH) + 1);
    }

    for (PurchaseRecord p : purchases) {
      for (int i = 0; i < weeks; i++) {
        Calendar weekStart = weekStarts[i];
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.WEEK_OF_YEAR, 1);
        if (!p.date.before(weekStart.getTime()) && p.date.before(weekEnd.getTime())) {
          totals[i] += sumProfit(p, products);
          break;
        }
      }
    }

    double weekTotal = totals[weeks - 1];

    ((android.widget.TextView) findViewById(R.id.tvWeekProfit)).setText(nf.format(weekTotal) + " ر.س");

    StringBuilder sb = new StringBuilder();
    for (int i = weeks - 1; i >= 0; i--) {
      sb.append("أسبوع ").append(labels[i]).append(": ").append(nf.format(totals[i])).append(" ر.س");
      if (i > 0) sb.append("\n");
    }
    ((android.widget.TextView) findViewById(R.id.tvWeekList)).setText(sb.toString());

    setChartData(R.id.chartWeek, totals);
  }

  private void renderMonthly(List<PurchaseRecord> purchases, Map<String, ProductCostInfo> products, Calendar now) {
    int months = 6;
    double[] totals = new double[months];
    String[] labels = new String[months];

    Calendar cal = (Calendar) now.clone();
    cal.set(Calendar.DAY_OF_MONTH, 1);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    Calendar[] monthStarts = new Calendar[months];
    for (int i = months - 1; i >= 0; i--) {
      Calendar c = (Calendar) cal.clone();
      c.add(Calendar.MONTH, -(months - 1 - i));
      monthStarts[i] = c;
      labels[i] = MONTH_NAMES_AR[c.get(Calendar.MONTH)];
    }

    for (PurchaseRecord p : purchases) {
      for (int i = 0; i < months; i++) {
        Calendar monthStart = monthStarts[i];
        Calendar monthEnd = (Calendar) monthStart.clone();
        monthEnd.add(Calendar.MONTH, 1);
        if (!p.date.before(monthStart.getTime()) && p.date.before(monthEnd.getTime())) {
          totals[i] += sumProfit(p, products);
          break;
        }
      }
    }

    double monthTotal = totals[months - 1];

    ((android.widget.TextView) findViewById(R.id.tvMonthProfit)).setText(nf.format(monthTotal) + " ر.س");

    StringBuilder sb = new StringBuilder();
    for (int i = months - 1; i >= 0; i--) {
      sb.append(labels[i]).append(": ").append(nf.format(totals[i])).append(" ر.س");
      if (i > 0) sb.append("\n");
    }
    ((android.widget.TextView) findViewById(R.id.tvMonthList)).setText(sb.toString());

    setChartData(R.id.chartMonth, totals);
  }

  private void renderYearly(List<PurchaseRecord> purchases, Map<String, ProductCostInfo> products, Calendar now) {
    int years = 4;
    double[] totals = new double[years];
    int[] yearNumbers = new int[years];

    int currentYear = now.get(Calendar.YEAR);
    for (int i = 0; i < years; i++) {
      yearNumbers[i] = currentYear - (years - 1 - i);
    }

    for (PurchaseRecord p : purchases) {
      Calendar pc = Calendar.getInstance();
      pc.setTime(p.date);
      int y = pc.get(Calendar.YEAR);
      for (int i = 0; i < years; i++) {
        if (yearNumbers[i] == y) {
          totals[i] += sumProfit(p, products);
          break;
        }
      }
    }

    double yearTotal = totals[years - 1];

    ((android.widget.TextView) findViewById(R.id.tvYearProfit)).setText(nf.format(yearTotal) + " ر.س");

    StringBuilder sb = new StringBuilder();
    for (int i = years - 1; i >= 0; i--) {
      sb.append(yearNumbers[i]).append(": ").append(nf.format(totals[i])).append(" ر.س");
      if (i > 0) sb.append("\n");
    }
    ((android.widget.TextView) findViewById(R.id.tvYearList)).setText(sb.toString());

    setChartData(R.id.chartYear, totals);
  }

  private double sumProfit(PurchaseRecord p, Map<String, ProductCostInfo> products) {
    double sum = 0;
    for (InvoiceLine line : p.lines) {
      sum += lineProfit(line, p.saleType, products);
    }
    return sum;
  }

  private void setChartData(int viewId, double[] totals) {
    float[] floats = new float[totals.length];
    for (int i = 0; i < totals.length; i++) floats[i] = (float) totals[i];
    MiniChartView chart = findViewById(viewId);
    if (chart != null) chart.setData(floats);
  }
}
