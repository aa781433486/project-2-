*** Begin Patch
*** Update File: app/src/main/java/com/mycompany/app/ui/PosActivity.java
@@
-        findViewById(R.id.btn_view_invoice).setOnClickListener(v -> {
-            // For now just toast invoice id
-            Toast.makeText(PosActivity.this, "Invoice ID: " + activeInvoiceId, Toast.LENGTH_SHORT).show();
-        });
+        findViewById(R.id.btn_view_invoice).setOnClickListener(v -> {
+            // open InvoiceActivity
+            if (activeInvoiceId > 0) {
+                Intent i = new Intent(PosActivity.this, InvoiceActivity.class);
+                i.putExtra(InvoiceActivity.EXTRA_INVOICE_ID, activeInvoiceId);
+                startActivity(i);
+            } else {
+                Toast.makeText(PosActivity.this, "No active invoice", Toast.LENGTH_SHORT).show();
+            }
+        });
*** End Patch
