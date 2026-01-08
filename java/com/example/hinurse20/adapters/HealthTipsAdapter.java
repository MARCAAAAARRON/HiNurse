package com.example.hinurse20.adapters;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hinurse20.R;
import com.example.hinurse20.models.HealthTip;

import java.util.List;

public class HealthTipsAdapter extends RecyclerView.Adapter<HealthTipsAdapter.TipVH> {
    private final Context context;
    private final List<HealthTip> items;

    public HealthTipsAdapter(Context context, List<HealthTip> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public TipVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_health_tip_grid, parent, false);
        return new TipVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TipVH holder, int position) {
        HealthTip tip = items.get(position);
        holder.textTitle.setText(tip.title);
        if (tip.imageRes != null) holder.imageTip.setImageResource(tip.imageRes);

        holder.itemView.setOnClickListener(v -> showDialog(tip));
    }

    private void showDialog(HealthTip tip) {
        if (context instanceof AppCompatActivity) {
            // Use reusable dialog fragment for consistency across activities
            com.example.hinurse20.ui.TipCardDialogFragment.show(
                    ((AppCompatActivity) context).getSupportFragmentManager(),
                    tip.title,
                    tip.description,
                    tip.imageRes,
                    tip.link
            );
        } else {
            // Fallback (should rarely happen)
            View view = LayoutInflater.from(context).inflate(R.layout.layout_health_tip_dialog, null);
            ImageView image = view.findViewById(R.id.imageDialog);
            TextView textDesc = view.findViewById(R.id.textDialogDescription);
            TextView textTitle = view.findViewById(R.id.textDialogTitle);
            TextView textLink = view.findViewById(R.id.textDialogLink);
            Button buttonClose = view.findViewById(R.id.buttonDialogClose);

            if (tip.imageRes != null) image.setImageResource(tip.imageRes);
            textDesc.setText(tip.description);
            textTitle.setText(tip.title);

            if (tip.link != null && !tip.link.isEmpty()) {
                textLink.setText(tip.link);
                textLink.setVisibility(View.VISIBLE);
                textLink.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                textLink.setVisibility(View.GONE);
            }

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(view)
                    .create();
            buttonClose.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class TipVH extends RecyclerView.ViewHolder {
        ImageView imageTip;
        TextView textTitle;
        TipVH(@NonNull View itemView) {
            super(itemView);
            imageTip = itemView.findViewById(R.id.imageTip);
            textTitle = itemView.findViewById(R.id.textTitle);
        }
    }
}
