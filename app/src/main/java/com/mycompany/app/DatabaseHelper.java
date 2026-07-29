package com.mycompany.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "grocery.db";
  private static final int DATABASE_VERSION = 6;
  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
	"CREATE TABLE IF NOT EXISTS products ("
	+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
	+ "name TEXT UNIQUE,"
	+ "sale_type TEXT DEFAULT 'retail',"
	+ "retail_quantity INTEGER DEFAULT 0,"
	+ "wholesale_quantity INTEGER DEFAULT 0,"
	+ "units_per_box INTEGER DEFAULT 0,"
	+ "cost_price REAL DEFAULT 0,"
	+ "retail_price REAL DEFAULT 0,"
	+ "wholesale_price REAL DEFAULT 0)"
	);

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS users ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "username TEXT UNIQUE,"
            + "password TEXT)");

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS debtors ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "name TEXT UNIQUE,"
            + "total_debt REAL DEFAULT 0)");

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS purchases ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "customer TEXT,"
            + "total REAL DEFAULT 0,"
            + "date TEXT,"
            + "payment_type TEXT DEFAULT 'نقد',"
            + "sale_type TEXT DEFAULT 'تجزئة',"
            + "details TEXT)");

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS payments ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "debtor_name TEXT,"
            + "amount REAL DEFAULT 0,"
            + "date INTEGER)");

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS suppliers ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "name TEXT UNIQUE,"
            + "phone TEXT,"
            + "notes TEXT)");
  }

  // يتحقق إن كان عمود معيّن موجودًا في جدول معيّن (لتفادي كسر الترقية عند فحص أعمدة قديمة)
  private boolean columnExists(SQLiteDatabase db, String table, String column) {
	  Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
	  boolean found = false;
	  int nameIdx = c.getColumnIndex("name");
	  while (c.moveToNext()) {
		  if (column.equalsIgnoreCase(c.getString(nameIdx))) {
			  found = true;
			  break;
		  }
	  }
	  c.close();
	  return found;
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	  
	  if (oldVersion < 6) {
		  
		  try {
			  db.execSQL("ALTER TABLE products ADD COLUMN cost_price REAL DEFAULT 0");
			  } catch (Exception ignored) {
		  }
		  
		  try {
			  db.execSQL("ALTER TABLE products ADD COLUMN wholesale_price REAL DEFAULT 0");
			  } catch (Exception ignored) {
		  }
		  
		  try {
			  db.execSQL("ALTER TABLE products ADD COLUMN units_per_box INTEGER DEFAULT 1");
			  } catch (Exception ignored) {
		  }

		  // هذه الأعمدة الثلاثة هي أساس نظام تجزئة/جملة الحالي، ولم تكن تُضاف أبداً
		  // لقواعد بيانات قديمة تمت ترقيتها من نسخة سابقة (كانت تستخدم quantity/price فقط).
		  // غيابها هو سبب تحطم التطبيق فوراً عند أي استعلام يقرأ retail_quantity/wholesale_quantity/sale_type.
		  try {
			  db.execSQL("ALTER TABLE products ADD COLUMN sale_type TEXT DEFAULT 'retail'");
			  } catch (Exception ignored) {
		  }

		  try {
			  db.execSQL("ALTER TABLE products ADD COLUMN retail_quantity INTEGER DEFAULT 0");
			  } catch (Exception ignored) {
		  }

		  try {
			  db.execSQL("ALTER TABLE products ADD COLUMN wholesale_quantity INTEGER DEFAULT 0");
			  } catch (Exception ignored) {
		  }

		  // إذا كان الجدول القديم يحتوي على الأعمدة القديمة quantity/price، ننقل قيمها
		  // إلى retail_quantity/retail_price بدل تركها صفراً (تفادي فقدان بيانات المستخدم).
		  if (columnExists(db, "products", "quantity") && columnExists(db, "products", "price")) {
			  try {
				  db.execSQL("UPDATE products SET retail_quantity = quantity, sale_type = 'retail' WHERE retail_quantity = 0");
				  db.execSQL("UPDATE products SET retail_price = price WHERE retail_price = 0");
				  } catch (Exception ignored) {
			  }
		  }
	  }
	  
	  if (oldVersion < 5) {
		  try {
			  db.execSQL("ALTER TABLE products ADD COLUMN units_per_box INTEGER DEFAULT 1");
			  } catch (Exception ignored) {
		  }
	  }
	  
    if (oldVersion < 2) {
      db.execSQL(
          "CREATE TABLE IF NOT EXISTS debtors (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT"
              + " UNIQUE, total_debt REAL DEFAULT 0)");
      db.execSQL(
          "CREATE TABLE IF NOT EXISTS purchases (id INTEGER PRIMARY KEY AUTOINCREMENT, customer"
              + " TEXT, total REAL DEFAULT 0, date TEXT, details TEXT)");
    }
    if (oldVersion < 3) {
      db.execSQL(
          "CREATE TABLE IF NOT EXISTS payments (id INTEGER PRIMARY KEY AUTOINCREMENT, debtor_name"
              + " TEXT, amount REAL DEFAULT 0, date INTEGER)");
      db.execSQL(
          "CREATE TABLE IF NOT EXISTS suppliers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT"
              + " UNIQUE, phone TEXT, notes TEXT)");
      try {
        db.execSQL("ALTER TABLE products ADD COLUMN cost_price REAL DEFAULT 0");
      } catch (Exception ignored) {
      }
      try {
        db.execSQL("ALTER TABLE purchases ADD COLUMN payment_type TEXT DEFAULT 'نقد'");
      } catch (Exception ignored) {
      }
    }
    if (oldVersion < 4) {
      try {
        db.execSQL("ALTER TABLE products ADD COLUMN wholesale_price REAL DEFAULT 0");
      } catch (Exception ignored) {
      }
      try {
        db.execSQL("ALTER TABLE purchases ADD COLUMN sale_type TEXT DEFAULT 'تجزئة'");
      } catch (Exception ignored) {
      }
    }
  }

  // ==================== مستخدمون ====================
  public boolean addUser(String username, String password) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues cv = new ContentValues();
    cv.put("username", username);
    cv.put("password", password);
    long result = db.insert("users", null, cv);
    return result != -1;
  }

  public boolean checkUser(String username, String password) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor =
        db.rawQuery(
            "SELECT * FROM users WHERE username=? AND password=?",
            new String[] {username, password});
    boolean exists = cursor.getCount() > 0;
    cursor.close();
    return exists;
  }

  public boolean anyUserExists() {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor c = db.rawQuery("SELECT COUNT(*) FROM users", null);
    int count = 0;
    if (c.moveToFirst()) count = c.getInt(0);
    c.close();
    return count > 0;
  }

  public boolean userExists(String username) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery("SELECT id FROM users WHERE username=?", new String[] {username});
    boolean exists = cursor.moveToFirst();
    cursor.close();
    return exists;
  }

  // ==================== منتجات ====================
  
  public boolean addProduct(
  String name,
  String saleType,
  
  int retailQuantity,
  int wholesaleQuantity,
  
  int unitsPerBox,
  
  double costPrice,
  double retailPrice,
  double wholesalePrice) {
	  
	  SQLiteDatabase db = getWritableDatabase();
	  
	  ContentValues cv = new ContentValues();
	  
	  cv.put("name", name);
	  
	  cv.put("sale_type", saleType);
	  
	  cv.put("retail_quantity", retailQuantity);
	  
	  cv.put("wholesale_quantity", wholesaleQuantity);
	  
	  cv.put("units_per_box", unitsPerBox);
	  
	  cv.put("cost_price", costPrice);
	  
	  cv.put("retail_price", retailPrice);
	  
	  cv.put("wholesale_price", wholesalePrice);
	  
	  return db.insert("products", null, cv) != -1;
  }

  public boolean productExists(String name) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery("SELECT id FROM products WHERE name = ?", new String[] {name});
    boolean exists = cursor.moveToFirst();
    cursor.close();
    return exists;
  }

  public boolean updateProductQuantity(String name, int addQty) {
	  
	  SQLiteDatabase db = getWritableDatabase();
	  
	  Cursor c = db.rawQuery(
	  "SELECT retail_quantity FROM products WHERE name=?",
	  new String[]{name});
	  
	  if (!c.moveToFirst()) {
		  
		  c.close();
		  return false;
	  }
	  
	  int qty = c.getInt(
	  c.getColumnIndex("retail_quantity"));
	  
	  c.close();
	  
	  ContentValues cv = new ContentValues();
	  
	  cv.put("retail_quantity", qty + addQty);
	  
	  return db.update(
	  "products",
	  cv,
	  "name=?",
	  new String[]{name}) > 0;
  }

  public boolean addOrUpdateProduct(
  
  String name,
  String saleType,
  
  int retailQuantity,
  int wholesaleQuantity,
  
  int unitsPerBox,
  
  double costPrice,
  
  double retailPrice,
  
  double wholesalePrice) {
	  
	  SQLiteDatabase db = getWritableDatabase();
	  
	  Cursor cursor =
	  db.rawQuery(
	  "SELECT id FROM products WHERE name=?",
	  new String[]{name});
	  
	  ContentValues cv = new ContentValues();
	  
	  cv.put("sale_type", saleType);
	  
	  cv.put("retail_quantity", retailQuantity);
	  
	  cv.put("wholesale_quantity", wholesaleQuantity);
	  
	  cv.put("units_per_box", unitsPerBox);
	  
	  cv.put("cost_price", costPrice);
	  
	  cv.put("retail_price", retailPrice);
	  
	  cv.put("wholesale_price", wholesalePrice);
	  
	  if (cursor.moveToFirst()) {
		  
		  int id =
		  cursor.getInt(
		  cursor.getColumnIndex("id"));
		  
		  cursor.close();
		  
		  cv.put("name", name);
		  
		  return db.update(
		  "products",
		  cv,
		  "id=?",
		  new String[]{String.valueOf(id)})
		  > 0;
	  }
	  
	  cursor.close();
	  
	  cv.put("name", name);
	  
	  return db.insert("products", null, cv) != -1;
  }

  public boolean reduceProductQuantityByName(String name, int reduceBy) {
	  
	  SQLiteDatabase db = getWritableDatabase();
	  
	  Cursor c = db.rawQuery(
	  "SELECT retail_quantity FROM products WHERE name=?",
	  new String[]{name});
	  
	  if (!c.moveToFirst()) {
		  
		  c.close();
		  return false;
	  }
	  
	  int qty = c.getInt(
	  c.getColumnIndex("retail_quantity"));
	  
	  c.close();
	  
	  qty -= reduceBy;
	  
	  if (qty < 0)
	  qty = 0;
	  
	  ContentValues cv = new ContentValues();
	  
	  cv.put("retail_quantity", qty);
	  
	  return db.update(
	  "products",
	  cv,
	  "name=?",
	  new String[]{name}) > 0;
  }

  public boolean reduceWholesaleQuantityByName(String name, int reduceBy) {

	  SQLiteDatabase db = getWritableDatabase();

	  Cursor c = db.rawQuery(
	  "SELECT wholesale_quantity FROM products WHERE name=?",
	  new String[]{name});

	  if (!c.moveToFirst()) {

		  c.close();
		  return false;
	  }

	  int qty = c.getInt(
	  c.getColumnIndex("wholesale_quantity"));

	  c.close();

	  qty -= reduceBy;

	  if (qty < 0)
	  qty = 0;

	  ContentValues cv = new ContentValues();

	  cv.put("wholesale_quantity", qty);

	  return db.update(
	  "products",
	  cv,
	  "name=?",
	  new String[]{name}) > 0;
  }

  public boolean updateProduct(int id, String name, int quantity, double price) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues cv = new ContentValues();
    cv.put("name", name);
    cv.put("quantity", quantity);
    cv.put("price", price);
    return db.update("products", cv, "id=?", new String[] {String.valueOf(id)}) > 0;
  }

  public boolean updateProductFull(
      int id, String name, int quantity, double price, double wholesalePrice) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues cv = new ContentValues();
    cv.put("name", name);
    cv.put("quantity", quantity);
    cv.put("price", price);
    cv.put("wholesale_price", wholesalePrice);
    return db.update("products", cv, "id=?", new String[] {String.valueOf(id)}) > 0;
  }
  

  public boolean deleteProduct(int id) {
    SQLiteDatabase db = this.getWritableDatabase();
    return db.delete("products", "id=?", new String[] {String.valueOf(id)}) > 0;
  }

  public Cursor getAllProducts() {
    SQLiteDatabase db = this.getReadableDatabase();
    return db.rawQuery("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC", null);
  }

  public Cursor searchProductsByName(String query) {
    SQLiteDatabase db = this.getReadableDatabase();
    return db.rawQuery(
        "SELECT * FROM products WHERE name LIKE ? ORDER BY name COLLATE NOCASE ASC",
        new String[] {"%" + query + "%"});
  }
  

  public Cursor searchProductsByNameExact(String name) {
    SQLiteDatabase db = this.getReadableDatabase();
    return db.rawQuery("SELECT * FROM products WHERE name = ? LIMIT 1", new String[] {name});
  }

  public Cursor searchProductsByNamePrefix(String query) {
    SQLiteDatabase db = this.getReadableDatabase();
    return db.rawQuery(
        "SELECT * FROM products WHERE name LIKE ? ORDER BY name COLLATE NOCASE ASC",
        new String[] {query + "%"});
  }

  public double getProductPriceByName(String name, boolean wholesale) {
	  
	  SQLiteDatabase db = getReadableDatabase();
	  
	  String column;
	  
	  if (wholesale)
	  column = "wholesale_price";
	  else
	  column = "retail_price";
	  
	  Cursor c = db.rawQuery(
	  "SELECT " + column + " FROM products WHERE name=?",
	  new String[]{name});
	  
	  double price = 0;
	  
	  if (c.moveToFirst())
	  price = c.getDouble(0);
	  
	  c.close();
	  
	  return price;
  }

  public int getProductCount() {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor c = db.rawQuery("SELECT COUNT(*) FROM products", null);
    int count = 0;
    if (c.moveToFirst()) count = c.getInt(0);
    c.close();
    return count;
  }

  public Cursor getProductNamesCursor() {
	  
	  SQLiteDatabase db = getReadableDatabase();
	  
	  return db.rawQuery(
	  
	  "SELECT " +
	  "id AS _id," +
	  "name," +
	  "sale_type," +
	  "retail_quantity," +
	  "wholesale_quantity," +
	  "retail_price," +
	  "wholesale_price," +
	  "units_per_box " +
	  "FROM products " +
	  "ORDER BY name COLLATE NOCASE ASC",
	  
	  null);
  }

  // ==================== إحصائيات ====================
  public double getTodaySalesTotal() {
    SQLiteDatabase db = this.getReadableDatabase();
    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    Cursor c =
        db.rawQuery(
            "SELECT SUM(total) FROM purchases WHERE date LIKE ?", new String[] {today + "%"});
    double total = 0.0;
    if (c.moveToFirst()) total = c.getDouble(0);
    c.close();
    return total;
  }

  public int getTodayInvoiceCount() {
    SQLiteDatabase db = this.getReadableDatabase();
    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    Cursor c =
        db.rawQuery("SELECT COUNT(*) FROM purchases WHERE date LIKE ?", new String[] {today + "%"});
    int count = 0;
    if (c.moveToFirst()) count = c.getInt(0);
    c.close();
    return count;
  }

  public double getMonthSalesTotal() {
    SQLiteDatabase db = this.getReadableDatabase();
    String month = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
    Cursor c =
        db.rawQuery(
            "SELECT SUM(total) FROM purchases WHERE date LIKE ?", new String[] {month + "%"});
    double total = 0.0;
    if (c.moveToFirst()) total = c.getDouble(0);
    c.close();
    return total;
  }

  public int getLowStockCount(int threshold) {
	  
	  SQLiteDatabase db = getReadableDatabase();
	  
	  Cursor c = db.rawQuery(
	  
	  "SELECT COUNT(*) FROM products WHERE retail_quantity<=?",
	  
	  new String[]{String.valueOf(threshold)});
	  
	  int count = 0;
	  
	  if (c.moveToFirst())
	  count = c.getInt(0);
	  
	  c.close();
	  
	  return count;
  }

  public Cursor getLowStockProducts(int threshold) {
	  
	  SQLiteDatabase db = getReadableDatabase();
	  
	  return db.rawQuery(
	  
	  "SELECT * FROM products WHERE retail_quantity<=? ORDER BY retail_quantity ASC",
	  
	  new String[]{String.valueOf(threshold)});
  }

  // ==================== مديونون ====================
  public Cursor getAllDebtors() {
    SQLiteDatabase db = this.getReadableDatabase();
    return db.rawQuery(
        "SELECT id as _id, name, total_debt FROM debtors ORDER BY total_debt DESC", null);
  }

  public double getTotalDebts() {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery("SELECT SUM(total_debt) FROM debtors", null);
    double total = 0.0;
    if (cursor.moveToFirst()) total = cursor.getDouble(0);
    cursor.close();
    return total;
  }

  public double getDebtForDebtor(String name) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor c = db.rawQuery("SELECT total_debt FROM debtors WHERE name=?", new String[] {name});
    double debt = 0.0;
    if (c != null) {
      if (c.moveToFirst()) debt = c.getDouble(0);
      c.close();
    }
    return debt;
  }

  public boolean addDebtor(String name, double totalDebt) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues cv = new ContentValues();
    cv.put("name", name);
    cv.put("total_debt", totalDebt);
    long res = db.insertWithOnConflict("debtors", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    if (res == -1) {
      Cursor c = db.rawQuery("SELECT total_debt FROM debtors WHERE name=?", new String[] {name});
      if (c.moveToFirst()) {
        double existing = c.getDouble(0);
        c.close();
        ContentValues cv2 = new ContentValues();
        cv2.put("total_debt", existing + totalDebt);
        return db.update("debtors", cv2, "name=?", new String[] {name}) > 0;
      }
      c.close();
      return false;
    }
    return true;
  }

  public boolean addOrUpdateDebtor(String name, double amountToAdd) {
    SQLiteDatabase db = this.getWritableDatabase();
    Cursor c = db.rawQuery("SELECT id, total_debt FROM debtors WHERE name=?", new String[] {name});
    if (c.moveToFirst()) {
      double existing = c.getDouble(c.getColumnIndex("total_debt"));
      c.close();
      ContentValues cv = new ContentValues();
      cv.put("total_debt", existing + amountToAdd);
      return db.update("debtors", cv, "name=?", new String[] {name}) > 0;
    }
    c.close();
    return addDebtor(name, amountToAdd);
  }

  public boolean updateDebtor(String name, double newDebt) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues cv = new ContentValues();
    cv.put("total_debt", Math.max(0, newDebt));
    return db.update("debtors", cv, "name=?", new String[] {name}) > 0;
  }

  public boolean deleteDebtor(String name) {
    SQLiteDatabase db = this.getWritableDatabase();
    return db.delete("debtors", "name=?", new String[] {name}) > 0;
  }

  public Cursor searchDebtors(String query) {
    SQLiteDatabase db = this.getReadableDatabase();
    return db.rawQuery(
        "SELECT id as _id, name, total_debt FROM debtors WHERE name LIKE ? ORDER BY total_debt"
            + " DESC",
        new String[] {"%" + query + "%"});
  }

  public void payDebt(String debtorName, double amount) {
    if (amount <= 0) return;
    double currentDebt = getDebtForDebtor(debtorName);
    double newDebt = Math.max(0, currentDebt - amount);
    updateDebtor(debtorName, newDebt);
    insertPaymentRecord(debtorName, amount);
  }

  // ==================== مشتريات / فواتير ====================
  public long addPurchase(
      String customer,
      double total,
      String date,
      String paymentType,
      String saleType,
      String details) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues cv = new ContentValues();
    cv.put("customer", customer);
    cv.put("total", total);
    cv.put("date", date);
    cv.put("payment_type", paymentType);
    cv.put("sale_type", saleType != null ? saleType : "تجزئة");
    cv.put("details", details);
    return db.insert("purchases", null, cv);
  }

  public long addPurchase(
      String customer, double total, String date, String paymentType, String details) {
    return addPurchase(customer, total, date, paymentType, "تجزئة", details);
  }

  public long addPurchase(String customer, double total, String details) {
    String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    return addPurchase(customer, total, now, "نقد", "تجزئة", details);
  }

  public Cursor getAllPurchases() {
    SQLiteDatabase db = this.getReadableDatabase();
    return db.rawQuery(
        "SELECT id as _id, customer, total, date, payment_type, sale_type, details FROM purchases"
            + " ORDER BY id DESC",
        null);
  }

  public Cursor getPurchaseById(long id) {
    SQLiteDatabase db = this.getReadableDatabase();
    return db.rawQuery("SELECT * FROM purchases WHERE id=?", new String[] {String.valueOf(id)});
  }

  public Cursor searchPurchases(String keyword) {
    SQLiteDatabase db = this.getReadableDatabase();
    String q = "%" + keyword + "%";
    return db.rawQuery(
        "SELECT id as _id, customer, total, date FROM purchases WHERE customer LIKE ? OR date LIKE"
            + " ? ORDER BY id DESC",
        new String[] {q, q});
  }

  public boolean deletePurchase(long id) {
    SQLiteDatabase db = this.getWritableDatabase();
    return db.delete("purchases", "id=?", new String[] {String.valueOf(id)}) > 0;
  }

  public boolean deleteInvoicesByDebtor(String name) {
    SQLiteDatabase db = this.getWritableDatabase();
    return db.delete("purchases", "customer=?", new String[] {name}) >= 0;
  }

  public void recalculateDebtorTotal(String debtorName) {

    SQLiteDatabase db = this.getWritableDatabase();

    Cursor c1 =
        db.rawQuery(
            "SELECT SUM(total) FROM purchases WHERE customer=? AND payment_type='آجل'",
            new String[] {debtorName});

    double invoiceTotal = 0;

    if (c1.moveToFirst()) {
      invoiceTotal = c1.getDouble(0);
    }

    c1.close();

    Cursor c2 =
        db.rawQuery(
            "SELECT SUM(amount) FROM payments WHERE debtor_name=?", new String[] {debtorName});

    double paid = 0;

    if (c2.moveToFirst()) {
      paid = c2.getDouble(0);
    }

    c2.close();

    double debt = invoiceTotal - paid;

    if (debt < 0) debt = 0;

    ContentValues cv = new ContentValues();
    cv.put("total_debt", debt);

    int rows = db.update("debtors", cv, "name=?", new String[] {debtorName});

    if (rows == 0 && debt > 0) {
      cv.put("name", debtorName);
      db.insert("debtors", null, cv);
    }
  }

  // ==================== مدفوعات ====================
  public void insertPaymentRecord(String debtorName, double amount) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put("debtor_name", debtorName);
    values.put("amount", amount);
    values.put("date", System.currentTimeMillis());
    db.insert("payments", null, values);
  }

  // ==================== موردون ====================
  public boolean addSupplier(String name, String phone, String notes) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues cv = new ContentValues();
    cv.put("name", name);
    cv.put("phone", phone);
    cv.put("notes", notes);
    return db.insertWithOnConflict("suppliers", null, cv, SQLiteDatabase.CONFLICT_IGNORE) != -1;
  }

  public Cursor getAllSuppliers() {
    SQLiteDatabase db = this.getReadableDatabase();
    return db.rawQuery("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE ASC", null);
  }
  
  public int getBoxesCount(int quantity, int unitsPerBox) {
	  
	  if (unitsPerBox <= 1)
	  return 0;
	  
	  return quantity / unitsPerBox;
  }
  
  public double getProfitPerUnit(double costPrice, double sellPrice) {
	  
	  return sellPrice - costPrice;
  }
  
  public double getProfitPerBox(double costPrice, double wholesalePrice) {
	  
	  return wholesalePrice - costPrice;
  }
  
  public Cursor getProductById(int id) {
	  
	  SQLiteDatabase db = getReadableDatabase();
	  
	  return db.rawQuery(
	  
	  "SELECT * FROM products WHERE id=?",
	  
	  new String[]{String.valueOf(id)});
  }
  
  public boolean updateProductNew(
  
  int id,
  String name,
  String saleType,
  
  int retailQuantity,
  int wholesaleQuantity,
  
  int unitsPerBox,
  
  double costPrice,
  double retailPrice,
  double wholesalePrice) {
	  
	  SQLiteDatabase db = getWritableDatabase();
	  
	  ContentValues cv = new ContentValues();
	  
	  cv.put("name", name);
	  
	  cv.put("sale_type", saleType);
	  
	  cv.put("retail_quantity", retailQuantity);
	  
	  cv.put("wholesale_quantity", wholesaleQuantity);
	  
	  cv.put("units_per_box", unitsPerBox);
	  
	  cv.put("cost_price", costPrice);
	  
	  cv.put("retail_price", retailPrice);
	  
	  cv.put("wholesale_price", wholesalePrice);
	  
	  return db.update(
	  "products",
	  cv,
	  "id=?",
	  new String[]{String.valueOf(id)}) > 0;
  }
  
  
  
}
