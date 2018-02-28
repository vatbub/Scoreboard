package com.github.vatbub.scoreboard;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

/**
 * Created by frede on 28.02.2018.
 */

public class EditableTextView extends android.support.v7.widget.AppCompatTextView {
    private OnChangedCallback onChangedCallback;
    private String editAlertTitle;
    private String okButtonCaption;
    private String cancelButtonCaption;

    public EditableTextView(Context context) {
        this(context, null);
    }

    public EditableTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public EditableTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addOnClickListener();
    }

    public OnChangedCallback getOnChangedCallback() {
        return onChangedCallback;
    }

    public void setOnChangedCallback(OnChangedCallback onChangedCallback) {
        this.onChangedCallback = onChangedCallback;
    }

    private void addOnClickListener() {
        internalSetOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(getEditAlertTitle());

                // Set up the input
                final EditText input = new EditText(getContext());
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton(getOkButtonCaption(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String oldText = (String) EditableTextView.this.getText();
                        String newText = input.getText().toString();
                        EditableTextView.this.setText(newText);

                        if (getOnChangedCallback() != null)
                            getOnChangedCallback().changed(oldText, newText);
                    }
                });
                builder.setNegativeButton(getCancelButtonCaption(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        throw new IllegalStateException("Custom on click listeners are not allowed");
    }

    private void internalSetOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
    }

    public String getEditAlertTitle() {
        return editAlertTitle;
    }

    public void setEditAlertTitle(String editAlertTitle) {
        this.editAlertTitle = editAlertTitle;
    }

    public String getOkButtonCaption() {
        return okButtonCaption;
    }

    public void setOkButtonCaption(String okButtonCaption) {
        this.okButtonCaption = okButtonCaption;
    }

    public String getCancelButtonCaption() {
        return cancelButtonCaption;
    }

    public void setCancelButtonCaption(String cancelButtonCaption) {
        this.cancelButtonCaption = cancelButtonCaption;
    }

    public interface OnChangedCallback {
        void changed(String oldText, String newText);
    }
}
