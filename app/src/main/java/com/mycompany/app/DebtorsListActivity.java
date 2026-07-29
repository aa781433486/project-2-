package com.mycompany.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.FilterQueryProvider;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.util.TypedValue;
import java.text.NumberFormat;
import java.util.Locale;
import android.view.Window;
import android.graphics.Color;
import android.widget.LinearLayout;
public class DebtorsListActivity extends Activity {

    DatabaseHelper db;
    ListView listDebtors;
    TextView tvTotalDebt;
    EditText etSearchDebtor;
    SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debtors_list);

        db = new DatabaseHelper(this);
        listDebtors = (ListView) findViewById(R.id.listDebtors);
        tvTotalDebt = (TextView) findViewById(R.id.tvTotalDebt);
        etSearchDebtor = (EditText) findViewById(R.id.etSearchDebtor);

        loadDebtors();

        // البحث الديناميكي
        etSearchDebtor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchDebtors(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // الأزرار الأساسية (افترضت وجودهم في layout)
        findViewById(R.id.btnAddDebtor).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showAddDialog(); }
        });

        findViewById(R.id.btnEditDebtor).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showEditDialog(); }
        });

        findViewById(R.id.btnDeleteDebtor).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showDeleteDialog(); }
        });

        // عند الضغط على عنصر في القائمة نعرض خيارات (عرض فواتير / تسديد)
        listDebtors.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor) parent.getItemAtPosition(position);
                if (c != null) {
                    int nameIdx = c.getColumnIndex("name");
                    int debtIdx = c.getColumnIndex("total_debt");
                    String debtorName = "";
                    double debtAmount = 0.0;
                    if (nameIdx != -1) debtorName = c.getString(nameIdx);
                    if (debtIdx != -1) debtAmount = c.getDouble(debtIdx);
                    showDebtorOptionsDialog(debtorName, debtAmount);
                }
            }
        });
    }

    // تحميل المديونين وتنسيق العرض
    private void loadDebtors() {
        Cursor cursor = db.getAllDebtors();
        if (cursor != null) {
            String[] from = {"name", "total_debt"};
            int[] to = {android.R.id.text1, android.R.id.text2};

            // نستخدم simple_list_item_2 لكن ننسّق الــ text1 ليبدو "زرّي"
            adapter = new SimpleCursorAdapter(
                    this,
                    android.R.layout.simple_list_item_2,
                    cursor,
                    from,
                    to,
                    0
            );

            adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor c, int columnIndex) {
                    // نص المبلغ (text2) -> ننسّق المسافة وواحداث النقطة العشرية
                    if (view.getId() == android.R.id.text2) {
                        double amount = 0.0;
                        try { amount = c.getDouble(columnIndex); } catch (Exception ignored) {}
                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        nf.setMaximumFractionDigits(2);
                        ((TextView) view).setText("المبلغ: " + nf.format(amount) + " ريال");
                        ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                        ((TextView) view).setTypeface(null, Typeface.ITALIC);
                        ((TextView) view).setPadding(24, 8, 24, 8);
                        return true;
                    }

                    // اسم المدين (text1) -> نجعله يبدو كزر بزر بارز
                    if (view.getId() == android.R.id.text1) {
                        String name = "";
                        try { name = c.getString(columnIndex); } catch (Exception ignored) {}
                        TextView tv = (TextView) view;
                        tv.setText(name);
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        tv.setTypeface(null, Typeface.BOLD);
                        tv.setPadding(20, 24, 20, 24);
                        tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

                        // خلفية برمجية مستديرة تشبه زر
                        GradientDrawable gd = new GradientDrawable();
                        gd.setCornerRadius(18f * getResources().getDisplayMetrics().density);
                        gd.setColor(0xFFE9F7EF); // خلفية خفيفة
                        gd.setStroke(2, 0xFF2ECC71); // خط أخضر رفيع
                        tv.setBackground(gd);

                        // ظل داخلي بسيط (API صغيرة) — تحسين بصري بسيط:
                        tv.setElevation(2f * getResources().getDisplayMetrics().density);

                        return true;
                    }
                    return false;
                }
            });

            listDebtors.setAdapter(adapter);

            double total = db.getTotalDebts();
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
            nf.setMaximumFractionDigits(2);
            tvTotalDebt.setText("الإجمالي الكلي: " + nf.format(total) + " ريال");
            tvTotalDebt.setTypeface(null, Typeface.BOLD);
        } else {
            tvTotalDebt.setText("الإجمالي الكلي: 0 ريال");
        }
    }

    private void searchDebtors(String query) {
        Cursor cursor = db.searchDebtors(query);
        if (cursor != null && adapter != null) {
            adapter.changeCursor(cursor);
        }
    }

	// 🟢 حوار الخيارات عند الضغط على مديون
	private void showDebtorOptionsDialog(final String name, final double amount) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("👤 العميل: " + name);
		b.setMessage(String.format(Locale.ENGLISH, "💰 المبلغ المستحق: %.2f ريال", amount));

		// ترتيب الأزرار (تسديد ← إلغاء ← عرض الفواتير)
		b.setPositiveButton("💵 تسديد", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showPaymentDialogFor(name, amount);
				}
			});

		b.setNeutralButton("❌ إلغاء", null);

		b.setNegativeButton("📜 عرض الفواتير", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(DebtorsListActivity.this, InvoicesActivity.class);
					intent.putExtra("selectedCustomer", name);
					startActivity(intent);
				}
			});

		// إنشاء الـ Dialog
		AlertDialog dlg = b.create();
		dlg.show();

		// 🌈 خلفية متدرجة جميلة للنافذة
		GradientDrawable bg = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{Color.parseColor("#E3F2FD"), Color.parseColor("#BBDEFB")}
		);
		bg.setCornerRadius(30f);
		dlg.getWindow().setBackgroundDrawable(bg);

		// 🎨 تنسيق العنوان والنص
		TextView title = dlg.findViewById(android.R.id.title);
		TextView msg = dlg.findViewById(android.R.id.message);

		if (title != null) {
			title.setTextSize(22);
			title.setTextColor(Color.parseColor("#1E88E5"));
			title.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
			title.setGravity(Gravity.CENTER);
		}

		if (msg != null) {
			msg.setTextSize(20);
			msg.setTextColor(Color.parseColor("#263238"));
			msg.setGravity(Gravity.CENTER);
		}

		// 🔘 الأزرار
		Button btnPay = dlg.getButton(AlertDialog.BUTTON_POSITIVE);   // تسديد
		Button btnCancel = dlg.getButton(AlertDialog.BUTTON_NEUTRAL); // إلغاء
		Button btnView = dlg.getButton(AlertDialog.BUTTON_NEGATIVE);  // عرض الفواتير

		GradientDrawable shapePay = new GradientDrawable();
		shapePay.setCornerRadius(20);
		shapePay.setColor(Color.parseColor("#4CAF50"));

		GradientDrawable shapeCancel = new GradientDrawable();
		shapeCancel.setCornerRadius(20);
		shapeCancel.setColor(Color.parseColor("#9E9E9E"));

		GradientDrawable shapeView = new GradientDrawable();
		shapeView.setCornerRadius(20);
		shapeView.setColor(Color.parseColor("#2196F3"));

		if (btnPay != null) {
			btnPay.setTypeface(null, Typeface.BOLD);
			btnPay.setAllCaps(false);
			btnPay.setBackground(shapePay);
			btnPay.setTextColor(Color.WHITE);
			btnPay.setPadding(25, 15, 25, 15);
		}

		if (btnCancel != null) {
			btnCancel.setTypeface(null, Typeface.BOLD);
			btnCancel.setAllCaps(false);
			btnCancel.setBackground(shapeCancel);
			btnCancel.setTextColor(Color.WHITE);
			btnCancel.setPadding(25, 15, 25, 15);
		}

		if (btnView != null) {
			btnView.setTypeface(null, Typeface.BOLD);
			btnView.setAllCaps(false);
			btnView.setBackground(shapeView);
			btnView.setTextColor(Color.WHITE);
			btnView.setPadding(25, 15, 25, 15);
		}

		// 🔹 ترتيب الأزرار أفقيًا مع مسافات بينها
		LinearLayout buttonLayout = (LinearLayout) btnPay.getParent();
		if (buttonLayout != null) {
			buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
			buttonLayout.setGravity(Gravity.CENTER);
			buttonLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);

			GradientDrawable divider = new GradientDrawable();
			divider.setColor(Color.TRANSPARENT);
			divider.setSize(30, 0); // مسافة 30dp بين الأزرار
			buttonLayout.setDividerDrawable(divider);
		}
	}
	// 🟢 نافذة تسديد
	// 🟢 نافذة تسديد
	private void showPaymentDialogFor(final String debtorName, final double currentTotalDebt) {
		final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null);
		final AutoCompleteTextView etName = dialogView.findViewById(R.id.etDebtorName);
		final EditText etAmount = dialogView.findViewById(R.id.etPaymentAmount);

		if (etName != null) {
			etName.setText(debtorName);
			setupNameAutoComplete(etName);
		}

		if (etAmount != null) {
			etAmount.setHint(String.format(Locale.ENGLISH, "المبلغ (المتبقي: %.2f)", currentTotalDebt));
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("💵 تسديد المبلغ");
		builder.setView(dialogView);
		builder.setNegativeButton("❌ إلغاء", null);
		builder.setPositiveButton("✅ تسديد", null);

		final AlertDialog dialog = builder.create();
		dialog.setCancelable(false);
		dialog.show();

		// العنوان
		TextView title = dialog.findViewById(android.R.id.title);
		if (title != null) {
			title.setTextSize(22);
			title.setTextColor(Color.parseColor("#2C3E50"));
			title.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
			title.setGravity(Gravity.CENTER);
		}

		// الحقول
		if (etName != null) {
			etName.setTextSize(18);
			etName.setTextColor(Color.parseColor("#2C3E50"));
			etName.setBackgroundColor(Color.parseColor("#ECF0F1"));
			etName.setPadding(20, 20, 20, 20);
		}
		if (etAmount != null) {
			etAmount.setTextSize(18);
			etAmount.setTextColor(Color.parseColor("#2C3E50"));
			etAmount.setBackgroundColor(Color.parseColor("#ECF0F1"));
			etAmount.setPadding(20, 20, 20, 20);
		}

		// الأزرار
		Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
		Button btnNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

		GradientDrawable shapePositive = new GradientDrawable();
		shapePositive.setCornerRadius(20);
		shapePositive.setColor(Color.parseColor("#2ECC71"));

		GradientDrawable shapeNegative = new GradientDrawable();
		shapeNegative.setCornerRadius(20);
		shapeNegative.setColor(Color.parseColor("#E74C3C"));

		if (btnPositive != null) {
			btnPositive.setAllCaps(false);
			btnPositive.setTypeface(null, Typeface.BOLD);
			btnPositive.setBackground(shapePositive);
			btnPositive.setTextColor(Color.WHITE);
			btnPositive.setPadding(25, 15, 25, 15);
		}
		if (btnNegative != null) {
			btnNegative.setAllCaps(false);
			btnNegative.setBackground(shapeNegative);
			btnNegative.setTextColor(Color.WHITE);
			btnNegative.setPadding(25, 15, 25, 15);
		}

		// ✅ إضافة مسافة بسيطة بين الزرين بطريقة آمنة
		if (btnPositive != null && btnNegative != null) {
			LinearLayout.LayoutParams paramsPositive = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
			paramsPositive.setMargins(20, 0, 0, 0); // مسافة يسار زر "تسديد"
			btnPositive.setLayoutParams(paramsPositive);
		}

		if (btnPositive != null) {
			btnPositive.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String name = debtorName;
						if (etName != null) {
							String n = etName.getText().toString().trim();
							if (!n.isEmpty()) name = n;
						}

						String amountStr = (etAmount != null) ? etAmount.getText().toString().trim() : "";

						if (name.isEmpty()) {
							Toast.makeText(DebtorsListActivity.this, "الرجاء إدخال الاسم", Toast.LENGTH_SHORT).show();
							return;
						}
						if (amountStr.isEmpty()) {
							Toast.makeText(DebtorsListActivity.this, "الرجاء إدخال المبلغ", Toast.LENGTH_SHORT).show();
							return;
						}

						double payVal;
						try {
							payVal = Double.parseDouble(amountStr);
						} catch (NumberFormatException e) {
							Toast.makeText(DebtorsListActivity.this, "مبلغ غير صالح", Toast.LENGTH_SHORT).show();
							return;
						}

						double currentDebt = db.getDebtForDebtor(name);
						if (payVal > currentDebt) {
							Toast.makeText(DebtorsListActivity.this, "المبلغ أكبر من المديونية", Toast.LENGTH_SHORT).show();
							return;
						}

						db.payDebt(name, payVal);
						loadDebtors();

						double newDebt = currentDebt - payVal;

						if (Math.abs(newDebt) < 0.0001) {
							final String debtorNameFinal = name;
							new AlertDialog.Builder(DebtorsListActivity.this)
								.setTitle("✅ تم السداد بالكامل")
								.setMessage("هل تريد حذف جميع فواتير هذا العميل؟")
								.setPositiveButton("نعم - احذف الفواتير", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface d, int w) {
										db.deleteInvoicesByDebtor(debtorNameFinal);
										Toast.makeText(DebtorsListActivity.this, "تم حذف الفواتير ✅", Toast.LENGTH_SHORT).show();
										loadDebtors();
										dialog.dismiss();
									}
								})
								.setNegativeButton("لا - اتركها", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface d, int w) {
										Toast.makeText(DebtorsListActivity.this, "تمت العملية ✅", Toast.LENGTH_SHORT).show();
										dialog.dismiss();
									}
								})
								.show();
						} else {
							Toast.makeText(DebtorsListActivity.this, "تم خصم المبلغ بنجاح 💵", Toast.LENGTH_SHORT).show();
							dialog.dismiss();
						}
					}
				});
		}
	}

    // ---------- دوال العرض/إضافة/تعديل/حذف ----------

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_debtor, null);
        final EditText etName = (EditText) dialogView.findViewById(R.id.etName);
        final EditText etDebt = (EditText) dialogView.findViewById(R.id.etDebt);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("إضافة مديون");
        b.setView(dialogView);
        b.setNegativeButton("إلغاء", null);
        b.setPositiveButton("حفظ", null);

        final AlertDialog dialog = b.create();
        dialog.show();

        Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btnSave != null) {
            btnSave.setAllCaps(false);
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    String name = etName.getText().toString().trim();
                    String debtStr = etDebt.getText().toString().trim();
                    if (name.isEmpty() || debtStr.isEmpty()) {
                        Toast.makeText(DebtorsListActivity.this, "الرجاء إدخال الاسم والمبلغ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double val = Double.parseDouble(debtStr);
                        db.addDebtor(name, val);
                        loadDebtors();
                        Toast.makeText(DebtorsListActivity.this, "تمت الإضافة ✅", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } catch (NumberFormatException e) {
                        Toast.makeText(DebtorsListActivity.this, "مبلغ غير صالح", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void showEditDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_debtor, null);
        View nameView = dialogView.findViewById(R.id.etName);
        final AutoCompleteTextView etName;
        if (nameView instanceof AutoCompleteTextView) {
            etName = (AutoCompleteTextView) nameView;
        } else {
            AutoCompleteTextView tmp = new AutoCompleteTextView(this);
            tmp.setLayoutParams(nameView.getLayoutParams());
            if (nameView instanceof EditText) {
                CharSequence hint = ((EditText) nameView).getHint();
                if (hint != null) tmp.setHint(hint);
            }
            ViewGroup parent = (ViewGroup) nameView.getParent();
            if (parent != null) {
                int idx = parent.indexOfChild(nameView);
                parent.removeViewAt(idx);
                parent.addView(tmp, idx);
            }
            etName = tmp;
        }

        final EditText etDebt = (EditText) dialogView.findViewById(R.id.etDebt);

        Cursor cursorForNames = db.getAllDebtors();
        String[] from = {"name"};
        int[] to = {android.R.id.text1};
        final SimpleCursorAdapter nameAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_1,
                cursorForNames,
                from,
                to,
                0
        );

        nameAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override public Cursor runQuery(CharSequence constraint) {
                if (constraint == null) return null;
                return db.searchDebtors(constraint.toString() + "%");
            }
        });

        etName.setThreshold(1);
        etName.setAdapter(nameAdapter);

        etName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor) parent.getItemAtPosition(position);
                if (c != null) {
                    int nameIdx = c.getColumnIndex("name");
                    int debtIdx = c.getColumnIndex("total_debt");
                    String selectedName = nameIdx != -1 ? c.getString(nameIdx) : "";
                    double debt = debtIdx != -1 ? c.getDouble(debtIdx) : 0.0;
                    etName.setText(selectedName);
                    etDebt.setText(String.format(Locale.ENGLISH, "%.2f", debt));
                }
            }
        });

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("تعديل مديون");
        b.setView(dialogView);
        b.setNegativeButton("إلغاء", null);
        b.setPositiveButton("حفظ", null);

        final AlertDialog dialog = b.create();
        dialog.show();

        Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btnSave != null) {
            btnSave.setAllCaps(false);
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    String name = etName.getText().toString().trim();
                    String debtStr = etDebt.getText().toString().trim();
                    if (name.isEmpty() || debtStr.isEmpty()) {
                        Toast.makeText(DebtorsListActivity.this, "الرجاء إدخال الاسم والمبلغ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double val = Double.parseDouble(debtStr);
                        db.updateDebtor(name, val);
                        loadDebtors();
                        Toast.makeText(DebtorsListActivity.this, "تم التعديل ✅", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } catch (NumberFormatException e) {
                        Toast.makeText(DebtorsListActivity.this, "مبلغ غير صالح", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void showDeleteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_debtor, null);
        final EditText etName = (EditText) dialogView.findViewById(R.id.etName);
        dialogView.findViewById(R.id.etDebt).setVisibility(View.GONE); // لا نحتاج المبلغ للحذف

        new AlertDialog.Builder(this)
            .setTitle("حذف مديون")
            .setView(dialogView)
            .setPositiveButton("حذف", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        db.deleteDebtor(name);
                        db.deleteInvoicesByDebtor(name); // حذف الفواتير المرتبطة أيضًا
                        loadDebtors();
                        Toast.makeText(DebtorsListActivity.this, "تم الحذف بنجاح 🗑️", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DebtorsListActivity.this, "الرجاء إدخال الاسم", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    // تهيئة AutoComplete للاسماء (استخدمت في التسديد)
    private void setupNameAutoComplete(AutoCompleteTextView etName) {
        Cursor cursorForNames = db.getAllDebtors();
        String[] from = {"name"};
        int[] to = {android.R.id.text1};

        final SimpleCursorAdapter nameAdapter = new SimpleCursorAdapter(
            this,
            android.R.layout.simple_list_item_1,
            cursorForNames,
            from,
            to,
            0
        );

        nameAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                if (constraint == null) return null;
                return db.searchDebtors(constraint.toString() + "%");
            }
        });

        etName.setThreshold(1);
        etName.setAdapter(nameAdapter);
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		// ✅ إعادة تحميل قائمة المديونين تلقائيًا عند الرجوع من أي صفحة
		loadDebtors();
	}
}
