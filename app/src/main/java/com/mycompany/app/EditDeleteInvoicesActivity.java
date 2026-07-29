package com.mycompany.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class EditDeleteInvoicesActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText("صفحة تعديل / حذف الفواتير (قيد الإنشاء)");
        tv.setTextSize(20);
        setContentView(tv);
    }
}
