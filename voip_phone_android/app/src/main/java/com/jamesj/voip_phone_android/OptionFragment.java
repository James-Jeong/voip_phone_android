package com.jamesj.voip_phone_android;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.media.MediaManager;
import com.jamesj.voip_phone_android.media.module.ResourceManager;
import com.jamesj.voip_phone_android.service.AppInstance;
import com.jamesj.voip_phone_android.ui.AudioCodecPickerDialog;
import com.orhanobut.logger.Logger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class OptionFragment extends Fragment implements NumberPicker.OnValueChangeListener {

    private MasterFragmentActivity masterFragmentActivity;
    private ViewGroup rootView;

    ///////////////////////////////////////////////

    public static String[] AUDIO_CODECS = { "ALAW", "ULAW", "AMR-NB", "AMR-WB", "EVS" };
    private Button audioCodecSelectButton;
    private TextView selectedAudioCodec;

    ///////////////////////////////////////////////

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch clientModeSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch useProxySwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch proxyModeSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch autoAcceptSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch dtmfSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch sendWavSwitch;

    ///////////////////////////////////////////////

    private TextInputLayout localHostNameInputLayout;
    private EditText localHostNameEditText;

    private TextInputLayout fromSipIpInputLayout;
    private EditText fromSipIpEditText;

    private TextInputLayout fromSipPortInputLayout;
    private EditText fromSipPortEditText;

    private TextInputLayout mediaIpInputLayout;
    private EditText mediaIpEditText;

    private TextInputLayout mediaPortInputLayout;
    private EditText mediaPortEditText;

    private TextInputLayout recordPathInputLayout;
    private EditText recordPathEditText;

    ///////////////////////////////////////////////

    private Button localHostNameEnterButton;
    private Button fromSipIpEnterButton;
    private Button fromSipPortEnterButton;
    private Button mediaIpEnterButton;
    private Button mediaPortEnterButton;
    private Button recordPathEnterButton;

    ///////////////////////////////////////////////


    public OptionFragment(MasterFragmentActivity masterFragmentActivity) {
        this.masterFragmentActivity = masterFragmentActivity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getContext() == null) {
            return null;
        }

        rootView = (ViewGroup) inflater.inflate(R.layout.activity_option, container, false);

        BufferedReader bufferedReader = null;

        try {
            File configFile = new File(rootView.getContext().getFilesDir() + "/user_conf.ini");
            if (!configFile.exists() || configFile.length() == 0) {
                // Load Config
                AssetManager assetManager = getResources().getAssets();
                bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open("user_conf.ini"), StandardCharsets.UTF_8));

                FileOutputStream fileOutputStream = rootView.getContext().openFileOutput("user_conf.ini", MODE_PRIVATE);
                DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

                String readLine;
                while ((readLine = bufferedReader.readLine()) != null) {
                    dataOutputStream.write(readLine.getBytes(StandardCharsets.UTF_8));
                    dataOutputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                }
                bufferedReader.close();
                dataOutputStream.close();
                fileOutputStream.close();
            }

            ConfigManager configManager = new ConfigManager(rootView.getContext());
            configManager.load(rootView.getContext().openFileInput("user_conf.ini"));
            AppInstance.getInstance().setConfigManager(configManager);

            //MediaManager.getInstance().start();
            ResourceManager.getInstance().initResource();
            //

            // AUDIO-CODEC
            audioCodecSelectButton = rootView.findViewById(R.id.audioCodecSelectButton);
            audioCodecSelectButton.setOnClickListener(v -> showAudioCodecPicker(v, "Select audio codec", ""));
            audioCodecSelectButton.setBackgroundColor(Color.BLACK);

            selectedAudioCodec = rootView.findViewById(R.id.selectedAudioCodec);
            selectedAudioCodec.setText(configManager.getPriorityAudioCodec());
            MediaManager.getInstance().setPriorityCodec(configManager.getPriorityAudioCodec());
            //

            // SWITCHES
            clientModeSwitch = rootView.findViewById(R.id.clientModeSwitch);
            useProxySwitch = rootView.findViewById(R.id.useProxySwitch);
            proxyModeSwitch = rootView.findViewById(R.id.proxyModeSwitch);
            autoAcceptSwitch = rootView.findViewById(R.id.autoAcceptSwitch);
            dtmfSwitch = rootView.findViewById(R.id.dtmfSwitch);
            sendWavSwitch = rootView.findViewById(R.id.sendWavSwitch);

            if (configManager.isUseClient()) {
                clientModeSwitch.setTextColor(Color.MAGENTA);
                clientModeSwitch.setChecked(true);
                clientModeSwitch.setEnabled(false);

                audioCodecSelectButton.setEnabled(true);
                proxyModeSwitch.setChecked(false);
                useProxySwitch.setEnabled(true);
                autoAcceptSwitch.setEnabled(true);
                dtmfSwitch.setEnabled(true);
                sendWavSwitch.setEnabled(true);
            } else if (configManager.isProxyMode()) {
                proxyModeSwitch.setTextColor(Color.MAGENTA);
                proxyModeSwitch.setChecked(true);
                proxyModeSwitch.setEnabled(false);

                audioCodecSelectButton.setEnabled(false);
                clientModeSwitch.setChecked(false);
                useProxySwitch.setEnabled(false);
                autoAcceptSwitch.setEnabled(false);
                dtmfSwitch.setEnabled(false);
                sendWavSwitch.setEnabled(false);
            }

            if (configManager.isUseProxy()) {
                useProxySwitch.setChecked(true);
            }
            if (configManager.isCallAutoAccept()) {
                autoAcceptSwitch.setChecked(true);
            }
            if (configManager.isDtmf()) {
                dtmfSwitch.setChecked(true);
            }
            if (configManager.isSendWav()) {
                sendWavSwitch.setChecked(true);
            }

            clientModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    clientModeSwitch.setTextColor(Color.MAGENTA);
                    clientModeSwitch.setEnabled(false);
                    proxyModeSwitch.setTextColor(Color.WHITE);
                    proxyModeSwitch.setChecked(false);
                    proxyModeSwitch.setEnabled(true);

                    audioCodecSelectButton.setEnabled(true);
                    useProxySwitch.setEnabled(true);
                    autoAcceptSwitch.setEnabled(true);
                    dtmfSwitch.setEnabled(true);
                    sendWavSwitch.setEnabled(true);

                    configManager.setUseClient(true);
                    configManager.setIniValue(ConfigManager.SECTION_COMMON, ConfigManager.FIELD_USE_CLIENT, String.valueOf(true));
                    configManager.setProxyMode(false);
                    configManager.setIniValue(ConfigManager.SECTION_COMMON, ConfigManager.FIELD_PROXY_MODE, String.valueOf(false));

                    Toast.makeText(getContext(), "[CLIENT-MODE]", Toast.LENGTH_SHORT).show();
                }
            });

            proxyModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    proxyModeSwitch.setTextColor(Color.MAGENTA);
                    proxyModeSwitch.setEnabled(false);
                    clientModeSwitch.setTextColor(Color.WHITE);
                    clientModeSwitch.setChecked(false);
                    clientModeSwitch.setEnabled(true);

                    audioCodecSelectButton.setEnabled(false);
                    useProxySwitch.setEnabled(false);
                    autoAcceptSwitch.setEnabled(false);
                    dtmfSwitch.setEnabled(false);
                    sendWavSwitch.setEnabled(false);

                    configManager.setProxyMode(true);
                    configManager.setIniValue(ConfigManager.SECTION_COMMON, ConfigManager.FIELD_PROXY_MODE, String.valueOf(true));
                    configManager.setUseClient(false);
                    configManager.setIniValue(ConfigManager.SECTION_COMMON, ConfigManager.FIELD_USE_CLIENT, String.valueOf(false));

                    Toast.makeText(getContext(), "[PROXY-MODE]", Toast.LENGTH_SHORT).show();
                }
            });

            useProxySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (useProxySwitch.isChecked()) {
                    Toast.makeText(getContext(), "[USE-PROXY]", Toast.LENGTH_SHORT).show();
                }

                configManager.setUseProxy(isChecked);
                configManager.setIniValue(ConfigManager.SECTION_COMMON, ConfigManager.FIELD_USE_PROXY, String.valueOf(isChecked));
            });

            autoAcceptSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (autoAcceptSwitch.isChecked()) {
                    Toast.makeText(getContext(), "[AUTO-ACCEPT]", Toast.LENGTH_SHORT).show();
                }

                configManager.setCallAutoAccept(isChecked);
                configManager.setIniValue(ConfigManager.SECTION_COMMON, ConfigManager.FIELD_CALL_AUTO_ACCEPT, String.valueOf(isChecked));
            });

            dtmfSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (dtmfSwitch.isChecked()) {
                    Toast.makeText(getContext(), "[DTMF]", Toast.LENGTH_SHORT).show();
                }

                configManager.setDtmf(isChecked);
                configManager.setIniValue(ConfigManager.SECTION_MEDIA, ConfigManager.FIELD_DTMF, String.valueOf(isChecked));
            });

            sendWavSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (sendWavSwitch.isChecked()) {
                    Toast.makeText(getContext(), "[SEND-WAV]", Toast.LENGTH_SHORT).show();
                }

                configManager.setSendWav(isChecked);
                configManager.setIniValue(ConfigManager.SECTION_MEDIA, ConfigManager.FIELD_SEND_WAV, String.valueOf(isChecked));
            });
            //

            // INPUT FIELDS
            localHostNameInputLayout = rootView.findViewById(R.id.localHostNameInputLayout);
            localHostNameEditText = localHostNameInputLayout.getEditText();
            if (localHostNameEditText != null) {
                localHostNameEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) } );
                localHostNameEditText.setText(configManager.getHostName());
            }
            localHostNameEnterButton = rootView.findViewById(R.id.localHostNameEnterButton);
            localHostNameEnterButton.setOnClickListener(this::localHostNameEnterButtonClicked);
            localHostNameEnterButton.setBackgroundColor(Color.BLACK);

            fromSipIpInputLayout = rootView.findViewById(R.id.localSipIpInputLayout);
            fromSipIpEditText = fromSipIpInputLayout.getEditText();
            if (fromSipIpEditText != null) {
                fromSipIpEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) } );
                fromSipIpEditText.setText(configManager.getFromIp());
            }
            fromSipIpEnterButton = rootView.findViewById(R.id.localSipIpEnterButton);
            fromSipIpEnterButton.setOnClickListener(this::fromSipIpEnterButtonClicked);
            fromSipIpEnterButton.setBackgroundColor(Color.BLACK);

            fromSipPortInputLayout = rootView.findViewById(R.id.localSipPortInputLayout);
            fromSipPortEditText = fromSipPortInputLayout.getEditText();
            if (fromSipPortEditText != null) {
                fromSipPortEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(5) } );
                fromSipPortEditText.setText(String.valueOf(configManager.getFromPort()));
            }
            fromSipPortEnterButton = rootView.findViewById(R.id.localSipPortEnterButton);
            fromSipPortEnterButton.setOnClickListener(this::fromSipPortEnterButtonClicked);
            fromSipPortEnterButton.setBackgroundColor(Color.BLACK);

            mediaIpInputLayout = rootView.findViewById(R.id.localMediaIpInputLayout);
            mediaIpEditText = mediaIpInputLayout.getEditText();
            if (mediaIpEditText != null) {
                mediaIpEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) } );
                mediaIpEditText.setText(configManager.getNettyServerIp());
            }
            mediaIpEnterButton = rootView.findViewById(R.id.localMediaIpEnterButton);
            mediaIpEnterButton.setOnClickListener(this::mediaIpEnterButtonClicked);
            mediaIpEnterButton.setBackgroundColor(Color.BLACK);

            mediaPortInputLayout = rootView.findViewById(R.id.localMediaPortInputLayout);
            mediaPortEditText = mediaPortInputLayout.getEditText();
            if (mediaPortEditText != null) {
                mediaPortEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(5) } );
                mediaPortEditText.setText(String.valueOf(configManager.getNettyServerPort()));
            }
            mediaPortEnterButton = rootView.findViewById(R.id.localMediaPortEnterButton);
            mediaPortEnterButton.setOnClickListener(this::mediaPortEnterButtonClicked);
            mediaPortEnterButton.setBackgroundColor(Color.BLACK);

            recordPathInputLayout = rootView.findViewById(R.id.recordPathInputLayout);
            recordPathEditText = recordPathInputLayout.getEditText();
            if (recordPathEditText != null) {
                recordPathEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(100) } );
                recordPathEditText.setText(configManager.getRecordPath());
            }
            recordPathEnterButton = rootView.findViewById(R.id.recordPathEnterButton);
            recordPathEnterButton.setOnClickListener(this::recordPathEnterButtonClicked);
            recordPathEnterButton.setBackgroundColor(Color.BLACK);
            //

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return rootView;
    }

    ///////////////////////////////////////////////

    public void control(boolean isEnabled) {
        if (isEnabled) { // ENABLE
            if (isClientMode()) {
                clientModeSwitch.setEnabled(false);
                proxyModeSwitch.setEnabled(true);

                audioCodecSelectButton.setEnabled(true);
                useProxySwitch.setEnabled(true);
                autoAcceptSwitch.setEnabled(true);
                dtmfSwitch.setEnabled(true);
                sendWavSwitch.setEnabled(true);
            } else {
                clientModeSwitch.setEnabled(true);
                proxyModeSwitch.setEnabled(false);

                audioCodecSelectButton.setEnabled(false);
                useProxySwitch.setEnabled(false);
                autoAcceptSwitch.setEnabled(false);
                dtmfSwitch.setEnabled(false);
                sendWavSwitch.setEnabled(false);
            }

            localHostNameInputLayout.setEnabled(true);
            fromSipIpInputLayout.setEnabled(true);
            fromSipPortInputLayout.setEnabled(true);
            mediaIpInputLayout.setEnabled(true);
            mediaPortInputLayout.setEnabled(true);
            recordPathInputLayout.setEnabled(true);

            localHostNameEnterButton.setEnabled(true);
            fromSipIpEnterButton.setEnabled(true);
            fromSipPortEnterButton.setEnabled(true);
            mediaIpEnterButton.setEnabled(true);
            mediaPortEnterButton.setEnabled(true);
            recordPathEnterButton.setEnabled(true);
        } else { // DISABLE
            clientModeSwitch.setEnabled(false);
            proxyModeSwitch.setEnabled(false);
            useProxySwitch.setEnabled(false);
            autoAcceptSwitch.setEnabled(false);
            dtmfSwitch.setEnabled(false);
            sendWavSwitch.setEnabled(false);

            localHostNameInputLayout.setEnabled(false);
            fromSipIpInputLayout.setEnabled(false);
            fromSipPortInputLayout.setEnabled(false);
            mediaIpInputLayout.setEnabled(false);
            mediaPortInputLayout.setEnabled(false);
            recordPathInputLayout.setEnabled(false);

            localHostNameEnterButton.setEnabled(false);
            fromSipIpEnterButton.setEnabled(false);
            fromSipPortEnterButton.setEnabled(false);
            mediaIpEnterButton.setEnabled(false);
            mediaPortEnterButton.setEnabled(false);
            recordPathEnterButton.setEnabled(false);
        }
    }

    public boolean isClientMode() {
        return clientModeSwitch.isChecked();
    }

    public boolean isUseProxy() {
        return useProxySwitch.isChecked();
    }

    public boolean isProxyMode() {
        return proxyModeSwitch.isChecked();
    }

    ///////////////////////////////////////////////

    private void localHostNameEnterButtonClicked(View view) {
        String localHostName = localHostNameEditText.getText().toString();

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        configManager.setHostName(localHostName);
        configManager.setIniValue(ConfigManager.SECTION_SIGNAL, ConfigManager.FIELD_HOST_NAME, localHostName);
    }

    private void fromSipIpEnterButtonClicked(View view) {
        String fromSipIp = fromSipIpEditText.getText().toString();

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        configManager.setHostName(fromSipIp);
        configManager.setIniValue(ConfigManager.SECTION_SIGNAL, ConfigManager.FIELD_FROM_IP, fromSipIp);
    }

    private void fromSipPortEnterButtonClicked(View view) {
        String fromSipPort = fromSipPortEditText.getText().toString();

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        configManager.setHostName(fromSipPort);
        configManager.setIniValue(ConfigManager.SECTION_SIGNAL, ConfigManager.FIELD_FROM_PORT, fromSipPort);
    }

    private void mediaIpEnterButtonClicked(View view) {
        String mediaIp = mediaIpEditText.getText().toString();

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        configManager.setHostName(mediaIp);
        configManager.setIniValue(ConfigManager.SECTION_MEDIA, ConfigManager.FIELD_NETTY_SERVER_IP, mediaIp);
    }

    private void mediaPortEnterButtonClicked(View view) {
        String mediaPort = mediaPortEditText.getText().toString();

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        configManager.setHostName(mediaPort);
        configManager.setIniValue(ConfigManager.SECTION_MEDIA, ConfigManager.FIELD_NETTY_SERVER_PORT, mediaPort);
    }

    private void recordPathEnterButtonClicked(View view) {
        String recordPath = recordPathEditText.getText().toString();

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        configManager.setHostName(recordPath);
        configManager.setIniValue(ConfigManager.SECTION_RECORD, ConfigManager.FIELD_RECORD_PATH, recordPath);
    }

    ///////////////////////////////////////////////

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        String selectedCodecName = AUDIO_CODECS[newVal];

        selectedAudioCodec.setText(selectedCodecName);

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        configManager.setPriorityAudioCodec(selectedCodecName);
        configManager.setIniValue(ConfigManager.SECTION_MEDIA, ConfigManager.FIELD_PRIORITY_CODEC, selectedCodecName);

        MediaManager.getInstance().setPriorityCodec(selectedCodecName);


        Logger.d("AUDIO CODEC [%s] is selected. (prev=%s)", AUDIO_CODECS[newVal], AUDIO_CODECS[oldVal] );
    }

    public void showAudioCodecPicker(View view, String title, String subtitle){
        AudioCodecPickerDialog audioCodecPickerDialog = new AudioCodecPickerDialog();

        Bundle bundle = new Bundle(2);
        bundle.putString("title", title);
        bundle.putString("subtitle", subtitle);

        audioCodecPickerDialog.setArguments(bundle);
        audioCodecPickerDialog.setValueChangeListener(this);
        audioCodecPickerDialog.show(masterFragmentActivity.getSupportFragmentManager(), "audio codec picker");
    }

    ///////////////////////////////////////////////

}
