package com.jamesj.voip_phone_android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.NumberPicker;

import androidx.fragment.app.DialogFragment;

import com.jamesj.voip_phone_android.OptionFragment;

public class AudioCodecPickerDialog extends DialogFragment {

    private NumberPicker.OnValueChangeListener valueChangeListener;

    String title;
    String subtitle;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (getArguments() == null) {
            return builder.create();
        }

        final NumberPicker numberPicker = new NumberPicker(getActivity());

        //Dialog 시작 시 bundle로 전달된 값을 받아온다

        title = getArguments().getString("title");
        subtitle = getArguments().getString("subtitle");

        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(OptionFragment.AUDIO_CODECS.length - 1);
        numberPicker.setDisplayedValues(OptionFragment.AUDIO_CODECS);

        // 키보드 입력 방지
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        builder.setTitle(title);
        builder.setMessage(subtitle);

        builder.setPositiveButton("OK", (dialog, which) -> {
            valueChangeListener.onValueChange(
                    numberPicker,
                    numberPicker.getValue(),
                    numberPicker.getValue()
            );
        });

        builder.setNegativeButton("CANCEL", (dialog, which) -> {
            // ignore
        });

        builder.setView(numberPicker);
        return builder.create();
    }

    public NumberPicker.OnValueChangeListener getValueChangeListener() {
        return valueChangeListener;
    }

    public void setValueChangeListener(NumberPicker.OnValueChangeListener valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
    }

}
