package com.example.hinurse20.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.hinurse20.R;

/**
 * Reusable tip card dialog shown across activities.
 * Usage:
 *   TipCardDialogFragment.show(getSupportFragmentManager(), title, description, imageResId, link);
 */
public class TipCardDialogFragment extends DialogFragment {
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESC = "arg_desc";
    private static final String ARG_IMAGE = "arg_image";
    private static final String ARG_LINK = "arg_link";

    public static void show(@NonNull androidx.fragment.app.FragmentManager fm,
                            @NonNull String title,
                            @NonNull String description,
                            @Nullable Integer imageRes,
                            @Nullable String link) {
        TipCardDialogFragment f = new TipCardDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TITLE, title);
        b.putString(ARG_DESC, description);
        if (imageRes != null) b.putInt(ARG_IMAGE, imageRes);
        if (link != null) b.putString(ARG_LINK, link);
        f.setArguments(b);
        f.show(fm, "TipCardDialogFragment");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_health_tip_dialog, null);
        ImageView image = view.findViewById(R.id.imageDialog);
        TextView textDesc = view.findViewById(R.id.textDialogDescription);
        TextView textTitle = view.findViewById(R.id.textDialogTitle);
        TextView textLink = view.findViewById(R.id.textDialogLink);
        Button buttonClose = view.findViewById(R.id.buttonDialogClose);

        Bundle args = getArguments();
        String title = args != null ? args.getString(ARG_TITLE) : null;
        String desc = args != null ? args.getString(ARG_DESC) : null;
        Integer imageRes = (args != null && args.containsKey(ARG_IMAGE)) ? args.getInt(ARG_IMAGE) : null;
        String link = args != null ? args.getString(ARG_LINK) : null;

        if (title != null) textTitle.setText(title);
        if (desc != null) textDesc.setText(desc);
        if (imageRes != null) image.setImageResource(imageRes);

        if (link != null && !link.isEmpty()) {
            textLink.setText(link);
            textLink.setVisibility(View.VISIBLE);
            textLink.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            textLink.setVisibility(View.GONE);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        // Remove the default dialog window background (white frame)
        if (dialog.getWindow() != null) {
            android.graphics.drawable.ColorDrawable transparent = new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT);
            dialog.getWindow().setBackgroundDrawable(transparent);
        }

        buttonClose.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null && d.getWindow() != null && getActivity() != null) {
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            int targetWidth = (int) (dm.widthPixels * 0.96f);
            d.getWindow().setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
