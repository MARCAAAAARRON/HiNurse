package com.example.hinurse20;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class IconSelectionActivity extends BaseActivity {
    private RecyclerView recyclerViewIcons;
    private ImageButton buttonBack;
    private Button buttonConfirm;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private IconAdapter adapter;
    private int selectedIconIndex = -1;
    private int currentSelectedIconIndex = -1; // Currently displayed icon in user profile

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icon_selection);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerViewIcons = findViewById(R.id.recyclerViewIcons);
        buttonBack = findViewById(R.id.buttonBack);
        buttonConfirm = findViewById(R.id.buttonConfirm);

        // Setup RecyclerView with 2 columns
        adapter = new IconAdapter();
        recyclerViewIcons.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewIcons.setAdapter(adapter);

        // Load current selected icon
        loadCurrentIconSelection();

        // Setup back button
        if (buttonBack != null) {
            buttonBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // Setup icon click listener (just selects, doesn't save yet)
        adapter.setOnIconClickListener(new OnIconClickListener() {
            @Override
            public void onIconClick(int iconIndex) {
                selectedIconIndex = iconIndex;
                adapter.setSelectedIcon(iconIndex);
                // Enable confirm button if a new icon is selected
                updateConfirmButton();
            }
        });
        
        // Setup confirm button
        if (buttonConfirm != null) {
            buttonConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedIconIndex >= 0) {
                        saveIconSelection(selectedIconIndex);
                    }
                }
            });
        }
    }
    
    private void updateConfirmButton() {
        if (buttonConfirm != null) {
            // Enable button if an icon is selected and it's different from current
            boolean hasSelection = selectedIconIndex >= 0;
            boolean isDifferent = selectedIconIndex != currentSelectedIconIndex;
            
            buttonConfirm.setEnabled(hasSelection && isDifferent);
            buttonConfirm.setAlpha(hasSelection && isDifferent ? 1.0f : 0.5f);
        }
    }

    private void loadCurrentIconSelection() {
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            DocumentSnapshot doc = task.getResult();
                            Long iconIndex = doc.getLong("selectedIconIndex");
                            if (iconIndex != null) {
                                currentSelectedIconIndex = iconIndex.intValue();
                                selectedIconIndex = currentSelectedIconIndex;
                                adapter.setSelectedIcon(selectedIconIndex);
                                updateConfirmButton();
                            }
                        }
                    }
                });
    }

    private void saveIconSelection(int iconIndex) {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        
        // Clear photoUrl and photoBase64 since we're using icon now
        db.collection("users").document(userId)
                .update("selectedIconIndex", iconIndex, "photoUrl", null, "photoBase64", null)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Return result to EditProfileActivity
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("iconIndex", iconIndex);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                    }
                });
    }

    // Interface for icon click listener
    interface OnIconClickListener {
        void onIconClick(int iconIndex);
    }

    private class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {
        private List<Integer> iconResources;
        private int selectedIcon = -1;
        private OnIconClickListener listener;

        IconAdapter() {
            iconResources = ProfileIconHelper.getAllProfileIcons();
        }

        void setOnIconClickListener(OnIconClickListener listener) {
            this.listener = listener;
        }

        void setSelectedIcon(int iconIndex) {
            int previous = selectedIcon;
            selectedIcon = iconIndex;
            if (previous >= 0 && previous < iconResources.size()) {
                notifyItemChanged(previous);
            }
            if (selectedIcon >= 0 && selectedIcon < iconResources.size()) {
                notifyItemChanged(selectedIcon);
            }
        }

        @NonNull
        @Override
        public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_icon_selection, parent, false);
            return new IconViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
            int iconRes = iconResources.get(position);
            holder.imageIcon.setImageResource(iconRes);
            
            // Show selection indicator
            if (position == selectedIcon) {
                holder.viewSelected.setVisibility(View.VISIBLE);
                holder.itemView.setAlpha(1.0f);
            } else {
                holder.viewSelected.setVisibility(View.GONE);
                holder.itemView.setAlpha(0.7f);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onIconClick(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return iconResources.size();
        }

        class IconViewHolder extends RecyclerView.ViewHolder {
            ImageView imageIcon;
            View viewSelected;

            IconViewHolder(@NonNull View itemView) {
                super(itemView);
                imageIcon = itemView.findViewById(R.id.imageIcon);
                viewSelected = itemView.findViewById(R.id.viewSelected);
            }
        }
    }
}

