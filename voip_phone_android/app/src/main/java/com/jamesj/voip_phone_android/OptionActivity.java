package com.jamesj.voip_phone_android;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jamesj.voip_phone_android.config.ConfigManager;
import com.jamesj.voip_phone_android.service.AppInstance;

import java.io.InputStream;

public class OptionActivity extends Fragment {

    private ViewGroup rootView;

    ///////////////////////////////////////////////

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.activity_option, container, false);

        AssetManager assetManager = getResources().getAssets();
        InputStream inputStream;
        try {
            inputStream = assetManager.open("user_conf.ini");

            ConfigManager configManager = new ConfigManager();
            configManager.load(inputStream);
            AppInstance.getInstance().setConfigManager(configManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rootView;
    }

    ///////////////////////////////////////////////

}
