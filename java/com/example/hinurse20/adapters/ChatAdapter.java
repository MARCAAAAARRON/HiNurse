package com.example.hinurse20.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hinurse20.R;
import com.example.hinurse20.models.ChatMessage;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_INCOMING_TEXT = 0;
    private static final int VIEW_TYPE_OUTGOING_TEXT = 1;
    private static final int VIEW_TYPE_INCOMING_IMAGE = 2;
    private static final int VIEW_TYPE_OUTGOING_IMAGE = 3;

    private final List<ChatMessage> items = new ArrayList<>();
    private final Set<String> pendingDeletions = new HashSet<>();
    private OnMessageDeleteListener deleteListener;

    public interface OnMessageDeleteListener {
        void onDeleteMessage(ChatMessage message, int position);
    }

    public void setOnMessageDeleteListener(OnMessageDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void markPendingDeletion(String messageId) {
        pendingDeletions.add(messageId);
    }

    public void removePendingDeletion(String messageId) {
        pendingDeletions.remove(messageId);
    }

    public void setItems(List<ChatMessage> newItems) {
        // Create a map of existing items by ID for efficient lookup
        java.util.Map<String, ChatMessage> existingItems = new java.util.HashMap<>();
        for (ChatMessage item : items) {
            existingItems.put(item.getMessageId(), item);
        }
        
        // Create a map of new items by ID
        java.util.Map<String, ChatMessage> newItemsMap = new java.util.HashMap<>();
        if (newItems != null) {
            for (ChatMessage item : newItems) {
                newItemsMap.put(item.getMessageId(), item);
            }
        }
        
        // Remove items that no longer exist in the new list AND are not pending deletion
        java.util.Iterator<ChatMessage> iterator = items.iterator();
        while (iterator.hasNext()) {
            ChatMessage item = iterator.next();
            if (!newItemsMap.containsKey(item.getMessageId()) && !pendingDeletions.contains(item.getMessageId())) {
                iterator.remove();
            }
        }
        
        // Add new items that don't already exist and are not pending deletion
        if (newItems != null) {
            for (ChatMessage newItem : newItems) {
                if (!existingItems.containsKey(newItem.getMessageId()) && !pendingDeletions.contains(newItem.getMessageId())) {
                    items.add(newItem);
                }
            }
        }
        
        // Sort items by timestamp to maintain order
        java.util.Collections.sort(items, new java.util.Comparator<ChatMessage>() {
            @Override
            public int compare(ChatMessage m1, ChatMessage m2) {
                if (m1.getTimestamp() == null) return -1;
                if (m2.getTimestamp() == null) return 1;
                return m1.getTimestamp().compareTo(m2.getTimestamp());
            }
        });
        
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = items.get(position);
        if (msg == null) return VIEW_TYPE_INCOMING_TEXT;
        boolean outgoing = msg.isFromUser();
        boolean isImage = "image".equalsIgnoreCase(msg.getMessageType());
        if (isImage) {
            return outgoing ? VIEW_TYPE_OUTGOING_IMAGE : VIEW_TYPE_INCOMING_IMAGE;
        } else {
            return outgoing ? VIEW_TYPE_OUTGOING_TEXT : VIEW_TYPE_INCOMING_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_OUTGOING_TEXT: {
                View v = inflater.inflate(R.layout.item_chat_outgoing, parent, false);
                return new OutgoingTextVH(v);
            }
            case VIEW_TYPE_INCOMING_IMAGE: {
                View v = inflater.inflate(R.layout.item_chat_incoming_image, parent, false);
                return new IncomingImageVH(v);
            }
            case VIEW_TYPE_OUTGOING_IMAGE: {
                View v = inflater.inflate(R.layout.item_chat_outgoing_image, parent, false);
                return new OutgoingImageVH(v);
            }
            case VIEW_TYPE_INCOMING_TEXT:
            default: {
                View v = inflater.inflate(R.layout.item_chat_incoming, parent, false);
                return new IncomingTextVH(v);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = items.get(position);
        if (holder instanceof OutgoingTextVH) {
            OutgoingTextVH vh = (OutgoingTextVH) holder;
            vh.text.setText(msg.getMessage());
            vh.meta.setText(formatMeta(msg, true));
            vh.deleteButton.setVisibility(View.VISIBLE);
            vh.deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteMessage(msg, position);
                }
            });
        } else if (holder instanceof IncomingTextVH) {
            ((IncomingTextVH) holder).text.setText(msg.getMessage());
            ((IncomingTextVH) holder).meta.setText(formatMeta(msg, false));
        } else if (holder instanceof IncomingImageVH) {
            IncomingImageVH vh = (IncomingImageVH) holder;
            Glide.with(holder.itemView.getContext())
                    .load(msg.getImageUrl())
                    .placeholder(R.drawable.logo)
                    .into(vh.image);
            vh.meta.setText(formatMeta(msg, false));
            
            // Add click listener for image zoom
            vh.image.setOnClickListener(v -> {
                if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
                    // Show zoomed image
                    showZoomedImage(vh.image.getContext(), msg.getImageUrl());
                }
            });
        } else if (holder instanceof OutgoingImageVH) {
            OutgoingImageVH vh = (OutgoingImageVH) holder;
            Glide.with(holder.itemView.getContext())
                    .load(msg.getImageUrl())
                    .placeholder(R.drawable.logo)
                    .into(vh.image);
            vh.meta.setText(formatMeta(msg, true));
            vh.deleteButton.setVisibility(View.VISIBLE);
            vh.deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteMessage(msg, position);
                }
            });
            
            // Add click listener for image zoom
            vh.image.setOnClickListener(v -> {
                if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
                    // Show zoomed image
                    showZoomedImage(vh.image.getContext(), msg.getImageUrl());
                }
            });
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            ChatMessage message = items.get(position);
            if (message != null) {
                // Mark as pending deletion to prevent it from reappearing
                markPendingDeletion(message.getMessageId());
            }
            items.remove(position);
            notifyItemRemoved(position);
            // Notify that item ranges have changed to prevent UI inconsistencies
            notifyItemRangeChanged(position, items.size() - position);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class IncomingTextVH extends RecyclerView.ViewHolder {
        TextView text;
        TextView meta;
        IncomingTextVH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textMessage);
            meta = itemView.findViewById(R.id.textMeta);
        }
    }

    static class OutgoingTextVH extends RecyclerView.ViewHolder {
        TextView text;
        TextView meta;
        ImageButton deleteButton;
        OutgoingTextVH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textMessage);
            meta = itemView.findViewById(R.id.textMeta);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
        }
    }

    static class IncomingImageVH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView meta;
        IncomingImageVH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageMessage);
            meta = itemView.findViewById(R.id.textMeta);
        }
    }

    static class OutgoingImageVH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView meta;
        ImageButton deleteButton;
        OutgoingImageVH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageMessage);
            meta = itemView.findViewById(R.id.textMeta);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
        }
    }

    private String formatMeta(ChatMessage msg, boolean outgoing) {
        Date ts = msg.getTimestamp();
        String time = ts != null ? new SimpleDateFormat("h:mm a", Locale.getDefault()).format(ts) : "";
        if (outgoing) {
            String status = msg.getStatus() != null ? msg.getStatus() : (msg.isRead() ? "read" : "sent");
            if ("read".equalsIgnoreCase(status) && msg.getReadAt() != null) {
                String ra = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(msg.getReadAt());
                return "Read • " + ra;
            } else if ("sent".equalsIgnoreCase(status) && msg.getSentAt() != null) {
                String sa = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(msg.getSentAt());
                return "Sent • " + sa;
            } else if ("sending".equalsIgnoreCase(status)) {
                return "Sending • " + time;
            } else {
                return capitalize(status) + " • " + time;
            }
        } else {
            return time;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0,1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }
    
    private void showZoomedImage(Context context, String imageUrl) {
        // Create a dialog with the zoomed image
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_zoom_image, null);
        builder.setView(dialogView);
        
        ImageView zoomedImage = dialogView.findViewById(R.id.imageViewZoomed);
        
        // Load the image with Glide
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo)
                .into(zoomedImage);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Make dialog full screen
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        
        // Close dialog when image is clicked
        zoomedImage.setOnClickListener(v -> dialog.dismiss());
    }
}
