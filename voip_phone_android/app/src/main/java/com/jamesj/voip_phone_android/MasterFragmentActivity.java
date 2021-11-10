package com.jamesj.voip_phone_android;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.tabs.TabLayout;
import com.jamesj.voip_phone_android.media.MediaManager;
import com.jamesj.voip_phone_android.media.codec.amr.AmrManager;
import com.jamesj.voip_phone_android.media.codec.evs.EvsManager;
import com.jamesj.voip_phone_android.media.module.ResourceManager;

public class MasterFragmentActivity extends FragmentActivity {

    ///////////////////////////////////////////////

    private TabLayout tabLayout;

    private PhoneFragment phoneFragment;
    private OptionFragment optionFragment;

    ///////////////////////////////////////////////

    private long lastTimeBackPressed;

    ///////////////////////////////////////////////

    // keyboard down when touched
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View focusView = getCurrentFocus();
        if (focusView != null) {
            Rect rect = new Rect();
            focusView.getGlobalVisibleRect(rect);
            int x = (int) ev.getX(), y = (int) ev.getY();
            if (!rect.contains(x, y)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
                }
                focusView.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        checkPermission();

        optionFragment = new OptionFragment(this);
        phoneFragment = new PhoneFragment();

        getSupportFragmentManager().beginTransaction().add(R.id.container, optionFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.container, phoneFragment).commit();

        getSupportFragmentManager().beginTransaction().show(phoneFragment).commit();
        getSupportFragmentManager().beginTransaction().hide(optionFragment).commit();

        phoneFragment.setOptionActivity(optionFragment);

        tabLayout = findViewById(R.id.tabs);
        tabLayout.addTab(tabLayout.newTab().setText("phone"));
        tabLayout.addTab(tabLayout.newTab().setText("option"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Fragment selected = null;

                if(position == 0) {
                    selected = phoneFragment;
                }
                else if(position == 1) {
                    selected = optionFragment;
                }

                if (selected != null) {
                    if (selected == phoneFragment) {
                        getSupportFragmentManager().beginTransaction().show(phoneFragment).commit();
                        getSupportFragmentManager().beginTransaction().hide(optionFragment).commit();
                    } else if (selected == optionFragment) {
                        getSupportFragmentManager().beginTransaction().show(optionFragment).commit();
                        getSupportFragmentManager().beginTransaction().hide(phoneFragment).commit();
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // SERVICE
        if (MediaManager.getInstance().getPriorityCodec().equals(MediaManager.EVS)) {
            EvsManager.getInstance().init();
        }

        if (MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_NB)
                || MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_WB)) {
            AmrManager.getInstance().init();
        }

        ResourceManager.getInstance().initResource();
        //
    }

    private void checkPermission() {
        // External storage permission
        /*if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }*/

        // Record audio permission
        /*if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
        }*/
    }

    ///////////////////////////////////////////////

    @Override
    public void onBackPressed() {
        /*List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragmentList) {
            if (fragment instanceof OnBackPressedListener) {
                ((OnBackPressedListener) fragment).onBackButtonPressed();
                return;
            }
        }

        // 두 번 클릭시 어플 종료
        if (System.currentTimeMillis() - lastTimeBackPressed < 1500) {
            finish();
            return;
        }
        lastTimeBackPressed = System.currentTimeMillis();
        Toast.makeText(this, "뒤로가기 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();*/

    }
}
