package com.jamesj.voip_phone_android.ui.screen.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.jamesj.voip_phone_android.R;

public class CheckableContactLinearLayout extends LinearLayout implements Checkable {

    ///////////////////////////////////////////////

    public CheckableContactLinearLayout(Context context) {
        super(context);
    }

    public CheckableContactLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    ///////////////////////////////////////////////

    @Override
    public void setChecked(boolean checked) {
        CheckBox checkBox = findViewById(R.id.contact_checkbox);
        if (checkBox.isChecked() != checked) {
            checkBox.setChecked(checked);
        }
        /*if (checkBox.isChecked() && checked) {
            checkBox.setChecked(false);
        } else {
            checkBox.setChecked(true);
        }*/
    }

    @Override
    public boolean isChecked() {
        CheckBox checkBox = findViewById(R.id.contact_checkbox);
        return checkBox.isChecked();
    }

    @Override
    public void toggle() {
        CheckBox checkBox = findViewById(R.id.contact_checkbox);
        setChecked(!checkBox.isChecked());
    }

    ///////////////////////////////////////////////

}
