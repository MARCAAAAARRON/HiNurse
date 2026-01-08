package com.example.hinurse20;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ToastHelper {
    
    /**
     * Show a custom toast with logo icon
     * @param context The context
     * @param message The message to display
     * @param duration Toast duration (Toast.LENGTH_SHORT or Toast.LENGTH_LONG)
     */
    public static void showToastWithLogo(Context context, String message, int duration) {
        // Create custom toast layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View toastView = inflater.inflate(R.layout.custom_toast, null);
        
        ImageView iconView = toastView.findViewById(R.id.toast_icon);
        TextView textView = toastView.findViewById(R.id.toast_text);
        
        // Set logo icon
        iconView.setImageResource(R.drawable.logo2);
        textView.setText(message);
        
        Toast toast = new Toast(context);
        toast.setDuration(duration);
        toast.setView(toastView);
        toast.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
    }
    
    /**
     * Show a short toast with logo
     */
    public static void showShort(Context context, String message) {
        showToastWithLogo(context, message, Toast.LENGTH_SHORT);
    }
    
    /**
     * Show a long toast with logo
     */
    public static void showLong(Context context, String message) {
        showToastWithLogo(context, message, Toast.LENGTH_LONG);
    }
}


