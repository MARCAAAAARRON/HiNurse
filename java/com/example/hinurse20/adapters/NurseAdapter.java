package com.example.hinurse20.adapters;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hinurse20.ProfileIconHelper;
import com.example.hinurse20.R;

import java.util.ArrayList;
import java.util.List;

public class NurseAdapter extends RecyclerView.Adapter<NurseAdapter.NurseViewHolder> {
    private final List<NurseItem> nurses = new ArrayList<>();
    private OnNurseClickListener listener;

    public interface OnNurseClickListener {
        void onNurseClick(String nurseId, String nurseName);
    }

    public void setOnNurseClickListener(OnNurseClickListener listener) {
        this.listener = listener;
    }

    public static class NurseItem {
        private String nurseId;
        private String name;
        private String photoUrl;
        private String photoBase64;
        private boolean isOnline;

        public NurseItem(String nurseId, String name, String photoUrl, String photoBase64, boolean isOnline) {
            this.nurseId = nurseId;
            this.name = name;
            this.photoUrl = photoUrl;
            this.photoBase64 = photoBase64;
            this.isOnline = isOnline;
        }

        public String getNurseId() { return nurseId; }
        public String getName() { return name; }
        public String getPhotoUrl() { return photoUrl; }
        public String getPhotoBase64() { return photoBase64; }
        public boolean isOnline() { return isOnline; }
    }

    @NonNull
    @Override
    public NurseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nurse, parent, false);
        return new NurseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NurseViewHolder holder, int position) {
        NurseItem nurse = nurses.get(position);
        
        // Set name
        holder.textName.setText(nurse.getName());
        
        // Set status
        holder.textStatus.setText(nurse.isOnline() ? "Available" : "Offline");
        holder.textStatus.setTextColor(holder.itemView.getContext().getResources()
                .getColor(nurse.isOnline() ? R.color.purple : android.R.color.darker_gray));
        
        // Set profile picture
        // Note: selectedIconIndex would need to be added to NurseItem if needed
        int defaultIcon = ProfileIconHelper.getProfileIconForUser(nurse.getNurseId());
        
        if (nurse.getPhotoUrl() != null && !nurse.getPhotoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(nurse.getPhotoUrl())
                    .circleCrop()
                    .placeholder(defaultIcon)
                    .error(defaultIcon)
                    .into(holder.imageProfile);
        } else if (nurse.getPhotoBase64() != null && !nurse.getPhotoBase64().isEmpty()) {
            try {
                byte[] bytes = android.util.Base64.decode(nurse.getPhotoBase64(), android.util.Base64.DEFAULT);
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.imageProfile.setImageBitmap(bmp);
            } catch (Exception e) {
                holder.imageProfile.setImageResource(defaultIcon);
            }
        } else {
            holder.imageProfile.setImageResource(defaultIcon);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNurseClick(nurse.getNurseId(), nurse.getName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return nurses.size();
    }

    public void setNurses(List<NurseItem> newNurses) {
        nurses.clear();
        if (newNurses != null) {
            nurses.addAll(newNurses);
        }
        notifyDataSetChanged();
    }

    static class NurseViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProfile;
        TextView textName;
        TextView textStatus;

        NurseViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            textName = itemView.findViewById(R.id.textName);
            textStatus = itemView.findViewById(R.id.textStatus);
        }
    }
}

