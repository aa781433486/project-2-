package com.mycompany.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mycompany.app.R;
import com.mycompany.app.data.InvoiceItem;
import com.mycompany.app.data.Product;
import com.mycompany.app.data.AppDatabase;

import java.util.List;

public class InvoiceItemAdapter extends RecyclerView.Adapter<InvoiceItemAdapter.VH> {

    private List<InvoiceItem> items;
    private Context ctx;
    private OnRemoveListener removeListener;

    public interface OnRemoveListener { void onRemove(long itemId); }

    public InvoiceItemAdapter(List<InvoiceItem> items, Context ctx, OnRemoveListener removeListener) {
        this.items = items;
        this.ctx = ctx;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        InvoiceItem it = items.get(position);
        // load product name
        new Thread(() -> {
            Product p = AppDatabase.getInstance(ctx).productDao().findById(it.productId);
            String name = p != null ? p.name : "#" + it.productId;
            ((InvoiceActivity)ctx).runOnUiThread(() -> holder.tvName.setText(name));
        }).start();

        holder.tvQty.setText(String.valueOf(it.quantity));
        holder.tvTotal.setText(String.format("%.2f", it.lineTotal));

        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) removeListener.onRemove(it.id);
        });
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvQty, tvTotal;
        Button btnRemove;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvQty = itemView.findViewById(R.id.tv_item_qty);
            tvTotal = itemView.findViewById(R.id.tv_item_total);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
