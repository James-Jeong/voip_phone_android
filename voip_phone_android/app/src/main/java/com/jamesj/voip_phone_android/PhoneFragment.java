package com.jamesj.voip_phone_android;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.jamesj.voip_phone_android.signal.module.SipManager;

@RequiresApi(api = Build.VERSION_CODES.O)
//public class MainActivity extends Fragment implements OnBackPressedListener {
public class PhoneFragment extends Fragment {

    private final SipManager sipManager = new SipManager();

    private ViewGroup rootView;

    ///////////////////////////////////////////////

    private OptionFragment optionFragment = null;

    private Button onButton;
    private Button offButton;
    private Button exitButton;
    private Button registerButton;
    private Button contactButton;
    private Button callButton;
    private Button byeButton;

    ///////////////////////////////////////////////

    private TextInputLayout proxyHostNameInputLayout;
    private EditText proxyHostNameEditText;
    private TextInputLayout remoteHostNameInputLayout;
    private EditText remoteHostNameEditText;

    ///////////////////////////////////////////////

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.activity_main, container, false);

        sipManager.init();

        onButton = rootView.findViewById(R.id.onButton); onButton.setBackgroundColor(Color.BLACK);
        onButton.setOnClickListener(this::onButtonClicked);

        offButton = rootView.findViewById(R.id.offButton); offButton.setBackgroundColor(Color.BLACK);
        offButton.setOnClickListener(this::offButtonClicked);

        exitButton = rootView.findViewById(R.id.exitButton); exitButton.setBackgroundColor(Color.BLACK);
        exitButton.setOnClickListener(this::exitButtonClicked);

        registerButton = rootView.findViewById(R.id.registerButton); registerButton.setBackgroundColor(Color.BLACK);
        registerButton.setOnClickListener(this::registerButtonClicked);

        contactButton = rootView.findViewById(R.id.contactButton); contactButton.setBackgroundColor(Color.BLACK);
        contactButton.setOnClickListener(this::contactButtonClicked);

        callButton = rootView.findViewById(R.id.callButton); callButton.setBackgroundColor(Color.BLACK);
        callButton.setOnClickListener(this::callButtonClicked);

        byeButton = rootView.findViewById(R.id.byeButton); byeButton.setBackgroundColor(Color.BLACK);
        byeButton.setOnClickListener(this::byeButtonClicked);

        proxyHostNameInputLayout = rootView.findViewById(R.id.proxyHostNameInputLayout);
        proxyHostNameEditText = proxyHostNameInputLayout.getEditText();
        if (proxyHostNameEditText != null) {
            proxyHostNameEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) } );
        }

        remoteHostNameInputLayout = rootView.findViewById(R.id.remoteHostNameInputLayout);
        remoteHostNameEditText = remoteHostNameInputLayout.getEditText();
        if (proxyHostNameEditText != null) {
            proxyHostNameEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) } );
        }

        offButton.setBackgroundColor(Color.RED);
        offButton.setEnabled(false);

        disableButton(registerButton);
        disableButton(contactButton);
        disableButton(callButton);
        disableButton(byeButton);

        proxyHostNameInputLayout.setEnabled(false);
        proxyHostNameInputLayout.setBackgroundColor(Color.GRAY);
        remoteHostNameInputLayout.setEnabled(false);
        remoteHostNameInputLayout.setBackgroundColor(Color.GRAY);

        return rootView;
    }

    public void setOptionActivity(OptionFragment _optionFragment) {
        optionFragment = _optionFragment;
    }

    ///////////////////////////////////////////////

    public void onButtonClicked(View view) {
        onButton.setEnabled(false);
        onButton.setBackgroundColor(Color.BLUE);

        enableButton(offButton);
        disableButton(exitButton);

        if (optionFragment.isClientMode()) {
            if (optionFragment.isUseProxy()) {
                proxyHostNameInputLayout.setEnabled(true);
                proxyHostNameInputLayout.setBackgroundColor(Color.WHITE);
                enableButton(registerButton);
            }

            enableButton(contactButton);
            enableButton(callButton);
            disableButton(byeButton);

            remoteHostNameInputLayout.setEnabled(true);
            remoteHostNameInputLayout.setBackgroundColor(Color.WHITE);
        }

        if (optionFragment != null) {
            optionFragment.control(false);
        }

        sipManager.start();
        //Toast.makeText(getContext(), "[ON]", Toast.LENGTH_SHORT).show();
    }

    public void offButtonClicked(View view) {
        offButton.setEnabled(false);
        offButton.setBackgroundColor(Color.RED);

        enableButton(onButton);
        enableButton(exitButton);

        proxyHostNameInputLayout.setEnabled(false);
        proxyHostNameInputLayout.setBackgroundColor(Color.GRAY);
        disableButton(registerButton);

        disableButton(contactButton);
        disableButton(callButton);
        disableButton(byeButton);

        remoteHostNameInputLayout.setEnabled(false);
        remoteHostNameInputLayout.setBackgroundColor(Color.GRAY);

        if (optionFragment != null) {
            optionFragment.control(true);
        }

        sipManager.stop();
        //Toast.makeText(getContext, "[OFF]", Toast.LENGTH_SHORT).show();
    }

    public void exitButtonClicked(View view) {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void registerButtonClicked(View view) {
        Toast.makeText(getContext(), "Register to [" + proxyHostNameEditText.getText() + "]", Toast.LENGTH_SHORT).show();
    }

    public void contactButtonClicked(View view) {

    }

    public void callButtonClicked(View view) {
        enableButton(byeButton);
        disableButton(callButton);

        Toast.makeText(getContext(), "Call to [" + remoteHostNameEditText.getText() + "]", Toast.LENGTH_SHORT).show();
    }

    public void byeButtonClicked(View view) {
        disableButton(byeButton);
        enableButton(callButton);

        Toast.makeText(getContext(), "Bye to [" + remoteHostNameEditText.getText() + "]", Toast.LENGTH_SHORT).show();
    }

    ///////////////////////////////////////////////

    private void enableButton(Button button) {
        button.setEnabled(true);
        button.setBackgroundColor(Color.BLACK);
    }

    private void disableButton(Button button) {
        button.setEnabled(false);
        button.setBackgroundColor(Color.GRAY);
    }

    ///////////////////////////////////////////////

    /*@Override
    public void onBackButtonPressed() {
        goToMain();
    }

    //프래그먼트 종료
    private void goToMain(){
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction().remove(MainActivity.this).commit();
        fragmentManager.popBackStack();
    }*/

}