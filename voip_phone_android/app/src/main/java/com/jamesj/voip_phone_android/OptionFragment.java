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
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.service.AppInstance;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class OptionFragment extends Fragment {

    private ViewGroup rootView;

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

    public ViewGroup getRootView() {
        return rootView;
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

                proxyModeSwitch.setChecked(false);
                useProxySwitch.setEnabled(true);
                autoAcceptSwitch.setEnabled(true);
                dtmfSwitch.setEnabled(true);
                sendWavSwitch.setEnabled(true);
            } else if (configManager.isProxyMode()) {
                proxyModeSwitch.setTextColor(Color.MAGENTA);
                proxyModeSwitch.setChecked(true);
                proxyModeSwitch.setEnabled(false);

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
                    proxyModeSwitch.setTextColor(Color.BLACK);
                    proxyModeSwitch.setChecked(false);
                    proxyModeSwitch.setEnabled(true);
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
                    clientModeSwitch.setTextColor(Color.BLACK);
                    clientModeSwitch.setChecked(false);
                    clientModeSwitch.setEnabled(true);
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

            fromSipIpInputLayout = rootView.findViewById(R.id.localSipIpInputLayout);
            fromSipIpEditText = fromSipIpInputLayout.getEditText();
            if (fromSipIpEditText != null) {
                fromSipIpEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) } );
                fromSipIpEditText.setText(configManager.getFromIp());
            }
            fromSipIpEnterButton = rootView.findViewById(R.id.localSipIpEnterButton);
            fromSipIpEnterButton.setOnClickListener(this::fromSipIpEnterButtonClicked);

            fromSipPortInputLayout = rootView.findViewById(R.id.localSipPortInputLayout);
            fromSipPortEditText = fromSipPortInputLayout.getEditText();
            if (fromSipPortEditText != null) {
                fromSipPortEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(5) } );
                fromSipPortEditText.setText(String.valueOf(configManager.getFromPort()));
            }
            fromSipPortEnterButton = rootView.findViewById(R.id.localSipPortEnterButton);
            fromSipPortEnterButton.setOnClickListener(this::fromSipPortEnterButtonClicked);

            mediaIpInputLayout = rootView.findViewById(R.id.localMediaIpInputLayout);
            mediaIpEditText = mediaIpInputLayout.getEditText();
            if (mediaIpEditText != null) {
                mediaIpEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) } );
                mediaIpEditText.setText(configManager.getNettyServerIp());
            }
            mediaIpEnterButton = rootView.findViewById(R.id.localMediaIpEnterButton);
            mediaIpEnterButton.setOnClickListener(this::mediaIpEnterButtonClicked);

            mediaPortInputLayout = rootView.findViewById(R.id.localMediaPortInputLayout);
            mediaPortEditText = mediaPortInputLayout.getEditText();
            if (mediaPortEditText != null) {
                mediaPortEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(5) } );
                mediaPortEditText.setText(String.valueOf(configManager.getNettyServerPort()));
            }
            mediaPortEnterButton = rootView.findViewById(R.id.localMediaPortEnterButton);
            mediaPortEnterButton.setOnClickListener(this::mediaPortEnterButtonClicked);

            recordPathInputLayout = rootView.findViewById(R.id.recordPathInputLayout);
            recordPathEditText = recordPathInputLayout.getEditText();
            if (recordPathEditText != null) {
                recordPathEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(100) } );
                recordPathEditText.setText(configManager.getRecordPath());
            }
            recordPathEnterButton = rootView.findViewById(R.id.recordPathEnterButton);
            recordPathEnterButton.setOnClickListener(this::recordPathEnterButtonClicked);
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
                useProxySwitch.setEnabled(true);
                autoAcceptSwitch.setEnabled(true);
                dtmfSwitch.setEnabled(true);
                sendWavSwitch.setEnabled(true);
            } else {
                clientModeSwitch.setEnabled(true);
                proxyModeSwitch.setEnabled(false);
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

}
