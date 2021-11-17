package com.jamesj.voip_phone_android.ui.screen.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.jamesj.voip_phone_android.R;
import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.media.MediaManager;
import com.jamesj.voip_phone_android.media.module.ResourceManager;
import com.jamesj.voip_phone_android.media.module.SoundHandler;
import com.jamesj.voip_phone_android.media.netty.NettyChannelManager;
import com.jamesj.voip_phone_android.service.AppInstance;
import com.jamesj.voip_phone_android.signal.module.NonceGenerator;
import com.jamesj.voip_phone_android.signal.module.SipManager;
import com.orhanobut.logger.Logger;

@RequiresApi(api = Build.VERSION_CODES.O)
//public class MainActivity extends Fragment implements OnBackPressedListener {
public class PhoneFragment extends Fragment {

    ///////////////////////////////////////////////
    // MANDATORY VARIABLES

    private SipManager sipManager = null;
    private ViewGroup rootView;
    private OptionFragment optionFragment = null;

    ///////////////////////////////////////////////
    // BUTTON

    private Button onButton;
    private Button offButton;
    private Button exitButton;
    private Button registerButton;
    private Button contactButton;
    private Button callButton;
    private Button byeButton;

    ///////////////////////////////////////////////
    // TEXT INPUT

    private TextInputLayout proxyHostNameInputLayout;
    private EditText proxyHostNameEditText;
    private TextInputLayout remoteHostNameInputLayout;
    private EditText remoteHostNameEditText;

    ///////////////////////////////////////////////
    // CALL INFO

    private String sessionId = null;
    private String callId = null;

    ///////////////////////////////////////////////
    // DTMF

    private static final String[] numbers = new String[] {
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            "*", "0", "#"};

    private static final int[] toneTypes = new int[]{
            ToneGenerator.TONE_DTMF_1,
            ToneGenerator.TONE_DTMF_2,
            ToneGenerator.TONE_DTMF_3,
            ToneGenerator.TONE_DTMF_4,
            ToneGenerator.TONE_DTMF_5,
            ToneGenerator.TONE_DTMF_6,
            ToneGenerator.TONE_DTMF_7,
            ToneGenerator.TONE_DTMF_8,
            ToneGenerator.TONE_DTMF_9,
            ToneGenerator.TONE_DTMF_S, //*
            ToneGenerator.TONE_DTMF_0,
            ToneGenerator.TONE_DTMF_P //#
    };

    private static int streamType = AudioManager.STREAM_MUSIC;
    private static int volume = 70;
    private static int durationMs = 100;
    private static final ToneGenerator toneGenerator = new ToneGenerator(streamType, volume);

    private GridLayout dtmfLayout;
    private Button dtmf0;
    private Button dtmf1;
    private Button dtmf2;
    private Button dtmf3;
    private Button dtmf4;
    private Button dtmf5;
    private Button dtmf6;
    private Button dtmf7;
    private Button dtmf8;
    private Button dtmf9;
    private Button dtmfStar;
    private Button dtmfShop;

    ///////////////////////////////////////////////

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.activity_main, container, false);

        onButton = rootView.findViewById(R.id.onButton); onButton.setBackgroundColor(Color.BLACK);
        onButton.setOnClickListener(this::onButtonClicked);

        offButton = rootView.findViewById(R.id.offButton); offButton.setBackgroundColor(Color.BLACK);
        offButton.setOnClickListener(this::offButtonClicked);

        exitButton = rootView.findViewById(R.id.exitButton); exitButton.setBackgroundColor(Color.BLACK);
        exitButton.setOnClickListener(this::exitButtonClicked);

        registerButton = rootView.findViewById(R.id.registerButton); registerButton.setBackgroundColor(Color.BLACK);
        registerButton.setOnClickListener(this::registerButtonClicked);

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
        disableButton(callButton);
        disableButton(byeButton);

        proxyHostNameInputLayout.setEnabled(false);
        //proxyHostNameInputLayout.setBackgroundColor(Color.GRAY);
        remoteHostNameInputLayout.setEnabled(false);
        //remoteHostNameInputLayout.setBackgroundColor(Color.GRAY);

        //
        dtmfLayout = rootView.findViewById(R.id.dtmf_layout);

        dtmf1 = rootView.findViewById(R.id.dtmf_1); dtmf1.setBackgroundColor(Color.BLACK);
        dtmf1.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[0], durationMs); });

        dtmf2 = rootView.findViewById(R.id.dtmf_2); dtmf2.setBackgroundColor(Color.BLACK);
        dtmf2.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[1], durationMs); });

        dtmf3 = rootView.findViewById(R.id.dtmf_3); dtmf3.setBackgroundColor(Color.BLACK);
        dtmf3.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[2], durationMs); });

        dtmf4 = rootView.findViewById(R.id.dtmf_4); dtmf4.setBackgroundColor(Color.BLACK);
        dtmf4.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[3], durationMs); });

        dtmf5 = rootView.findViewById(R.id.dtmf_5); dtmf5.setBackgroundColor(Color.BLACK);
        dtmf5.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[4], durationMs); });

        dtmf6 = rootView.findViewById(R.id.dtmf_6); dtmf6.setBackgroundColor(Color.BLACK);
        dtmf6.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[5], durationMs); });

        dtmf7 = rootView.findViewById(R.id.dtmf_7); dtmf7.setBackgroundColor(Color.BLACK);
        dtmf7.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[6], durationMs); });

        dtmf8 = rootView.findViewById(R.id.dtmf_8); dtmf8.setBackgroundColor(Color.BLACK);
        dtmf8.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[7], durationMs); });

        dtmf9 = rootView.findViewById(R.id.dtmf_9); dtmf9.setBackgroundColor(Color.BLACK);
        dtmf9.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[8], durationMs); });

        dtmfStar = rootView.findViewById(R.id.dtmf_star); dtmfStar.setBackgroundColor(Color.BLACK);
        dtmfStar.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[9], durationMs); });

        dtmf0 = rootView.findViewById(R.id.dtmf_0); dtmf0.setBackgroundColor(Color.BLACK);
        dtmf0.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[10], durationMs); });

        dtmfShop = rootView.findViewById(R.id.dtmf_shop); dtmfShop.setBackgroundColor(Color.BLACK);
        dtmfShop.setOnClickListener(v -> { toneGenerator.startTone(toneTypes[11], durationMs); });
        //

        disableDtmf();

        return rootView;
    }

    public void setOptionActivity(OptionFragment _optionFragment) {
        optionFragment = _optionFragment;
    }

    ///////////////////////////////////////////////

    public void onButtonClicked(View view) {
        if (sipManager != null) {
            return;
        }

        sipManager = new SipManager();
        if (sipManager.init()) {
            Toast.makeText(getContext(), "[ON]", Toast.LENGTH_SHORT).show();
        } else {
            return;
        }

        onButton.setEnabled(false);
        onButton.setBackgroundColor(Color.BLUE);

        enableButton(offButton);
        disableButton(exitButton);

        if (optionFragment.isClientMode()) {
            if (optionFragment.isUseProxy()) {
                proxyHostNameInputLayout.setEnabled(true);
                //proxyHostNameInputLayout.setBackgroundColor(Color.WHITE);
                enableButton(registerButton);
            }

            enableButton(callButton);
            if (optionFragment.isDtmf()) {
                enableDtmf();
            }
            disableButton(byeButton);

            remoteHostNameInputLayout.setEnabled(true);
            //remoteHostNameInputLayout.setBackgroundColor(Color.WHITE);
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
        //proxyHostNameInputLayout.setBackgroundColor(Color.GRAY);
        disableButton(registerButton);

        disableButton(callButton);
        disableDtmf();
        disableButton(byeButton);

        remoteHostNameInputLayout.setEnabled(false);
        //remoteHostNameInputLayout.setBackgroundColor(Color.GRAY);

        if (optionFragment != null) {
            optionFragment.control(true);
        }

        if (sipManager != null && callId != null) {
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            sipManager.sendBye(
                    callId,
                    remoteHostNameEditText.getText().toString(),
                    configManager.getToIp(),
                    configManager.getToPort()
            );
        }

        if (sipManager != null) {
            sipManager.stop();
            sipManager = null;
        }

        NettyChannelManager.getInstance().stop();
        SoundHandler.getInstance().stop();

        sessionId = null;
        callId = null;

        Toast.makeText(getContext(), "[OFF]", Toast.LENGTH_SHORT).show();
    }

    public void exitButtonClicked(View view) {
        MediaManager.getInstance().stop();
        ResourceManager.getInstance().releaseResource();

        System.runFinalization();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public void registerButtonClicked(View view) {
        Toast.makeText(getContext(), "Register to [" + proxyHostNameEditText.getText() + "]", Toast.LENGTH_SHORT).show();
    }

    public void callButtonClicked(View view) {
        if (sipManager == null) {
            return;
        }

        if (callId != null) {
            Toast.makeText(getContext(), "Call is ongoing. [" + remoteHostNameEditText.getText() + "] (callId=" + callId + ")", Toast.LENGTH_SHORT).show();
            return;
        }

        if (remoteHostNameEditText.getText() == null || remoteHostNameEditText.getText().length() == 0) {
            return;
        }

        enableButton(byeButton);
        disableButton(callButton);
        disableButton(onButton);
        disableButton(exitButton);

        sessionId = NonceGenerator.createRandomNonce();
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        callId = sipManager.sendInvite(
                sessionId,
                configManager.getHostName(),
                remoteHostNameEditText.getText().toString(),
                configManager.getToIp(),
                configManager.getToPort()
        );

        Toast.makeText(getContext(), "Call to [" + remoteHostNameEditText.getText() + "] (callId=" + callId + ")", Toast.LENGTH_SHORT).show();
    }

    public void byeButtonClicked(View view) {
        if (sipManager == null) {
            return;
        }

        if (callId == null) {
            Toast.makeText(getContext(), "Call is not exist. [" + remoteHostNameEditText.getText() + "] (callId=" + callId + ")", Toast.LENGTH_SHORT).show();
            return;
        }

        disableButton(byeButton);
        enableButton(callButton);
        enableButton(offButton);

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        sipManager.sendBye(
                callId,
                remoteHostNameEditText.getText().toString(),
                configManager.getToIp(),
                configManager.getToPort()
        );

        Toast.makeText(getContext(), "Bye to [" + remoteHostNameEditText.getText() + "] (callId=" + callId + ")", Toast.LENGTH_SHORT).show();

        sessionId = null;
        callId = null;
    }

    ///////////////////////////////////////////////

    private void enableDtmf() {
        enableButton(dtmf1); enableButton(dtmf2); enableButton(dtmf3);
        enableButton(dtmf4); enableButton(dtmf5); enableButton(dtmf6);
        enableButton(dtmf7); enableButton(dtmf8); enableButton(dtmf9);
        enableButton(dtmfStar); enableButton(dtmf0); enableButton(dtmfShop);
    }

    private void disableDtmf() {
        disableButton(dtmf1); disableButton(dtmf2); disableButton(dtmf3);
        disableButton(dtmf4); disableButton(dtmf5); disableButton(dtmf6);
        disableButton(dtmf7); disableButton(dtmf8); disableButton(dtmf9);
        disableButton(dtmfStar); disableButton(dtmf0); disableButton(dtmfShop);
    }

    private void enableButton(Button button) {
        button.setEnabled(true);
        button.setBackgroundColor(Color.BLACK);
    }

    private void disableButton(Button button) {
        button.setEnabled(false);
        button.setBackgroundColor(Color.GRAY);
    }

    public void processInvite(String callId, String fromNo) {
        Handler processInviteHandler = new Handler(Looper.getMainLooper());
        processInviteHandler.postDelayed(() -> {
            if (sipManager == null) {
                Toast.makeText(getContext(), "Sip stack is not initiated. Fail to recv the invite request.", Toast.LENGTH_SHORT).show();
                return;
            }

            this.callId = callId;

            if (fromNo != null) {
                remoteHostNameEditText.setText(fromNo);
            }

            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            if (configManager.isCallAutoAccept()) {
                new Thread(() -> {
                    try {
                        if (sipManager.sendInviteOk(this.callId)) {
                            Logger.d("Call from [%s] (callId=%s)", remoteHostNameEditText.getText(), callId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                enableButton(byeButton);
                disableButton(callButton);
                disableButton(onButton);
                disableButton(exitButton);

                Toast.makeText(getContext(), "Call from [" + remoteHostNameEditText.getText() + "] (callId=" + callId + ")", Toast.LENGTH_SHORT).show();
            } else {
                // 클라이언트 입장에서 자동 호 수락 옵션이 켜져있으면, 상대방 UA (프록시) 로 200 OK 를 바로 전송한다.
                Handler alertInviteHandler = new Handler(Looper.getMainLooper());
                alertInviteHandler.postDelayed(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("CALL FROM [ " + fromNo + " ]");
                    builder.setPositiveButton("ACCEPT", (dialog, which) -> {
                        new Thread(() -> {
                            try {
                                if (sipManager.sendInviteOk(this.callId)) {
                                    Logger.d("Call from [%s] (callId=%s)", remoteHostNameEditText.getText(), callId);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

                        enableButton(byeButton);
                        disableButton(callButton);
                        disableButton(onButton);
                        disableButton(exitButton);

                        Toast.makeText(getContext(), "Call from [" + remoteHostNameEditText.getText() + "] (callId=" + callId + ")", Toast.LENGTH_SHORT).show();
                    });
                    builder.setNegativeButton("DECLINE", (dialog, which) -> {
                        new Thread(() -> {
                            try {
                                sipManager.sendCancel(
                                        this.callId,
                                        remoteHostNameEditText.getText().toString(),
                                        configManager.getToIp(),
                                        configManager.getToPort()
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

                        Toast.makeText(getContext(), "Cancel to [" + remoteHostNameEditText.getText() + "] (callId=" + callId + ")", Toast.LENGTH_SHORT).show();
                    });
                    builder.create().show();
                }, 0);
            }
        }, 0);
    }

    public void processBye() {
        if (callId == null) {
            Toast.makeText(getContext(), "Call is not exist. Fail to recv the byte request.", Toast.LENGTH_SHORT).show();
            return;
        }

        disableButton(byeButton);
        enableButton(callButton);
        enableButton(offButton);

        Toast.makeText(getContext(), "Bye from [" + remoteHostNameEditText.getText() + "] (callId=" + callId + ")", Toast.LENGTH_SHORT).show();

        sessionId = null;
        callId = null;
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

    public EditText getRemoteHostNameEditText() {
        return remoteHostNameEditText;
    }
}