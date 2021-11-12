package com.jamesj.voip_phone_android.ui.screen.activity;

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
import com.jamesj.voip_phone_android.R;
import com.jamesj.voip_phone_android.ui.screen.fragment.ContactFragment;
import com.jamesj.voip_phone_android.ui.screen.fragment.OptionFragment;
import com.jamesj.voip_phone_android.ui.screen.fragment.PhoneFragment;
import com.jamesj.voip_phone_android.ui.screen.fragment.base.contact.ContactManagerFragment;

public class MasterFragmentActivity extends FragmentActivity {

    ///////////////////////////////////////////////

    private TabLayout tabLayout;

    private PhoneFragment phoneFragment;
    private OptionFragment optionFragment;
    private ContactFragment contactFragment;

    ///////////////////////////////////////////////

    private long lastTimeBackPressed;
    public static final String CONTACT_ADD_KEY = "contact_add_key";

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
        contactFragment = new ContactFragment();

        getSupportFragmentManager().beginTransaction().add(R.id.container, optionFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.container, phoneFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.container, contactFragment).commit();

        getSupportFragmentManager().beginTransaction().show(phoneFragment).commit();
        getSupportFragmentManager().beginTransaction().hide(optionFragment).commit();
        getSupportFragmentManager().beginTransaction().hide(contactFragment).commit();

        phoneFragment.setOptionActivity(optionFragment);

        tabLayout = findViewById(R.id.tabs);
        tabLayout.addTab(tabLayout.newTab().setText("phone"));
        tabLayout.addTab(tabLayout.newTab().setText("contact"));
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
                    selected = contactFragment;
                } else if (position == 2) {
                    selected = optionFragment;
                }

                if (selected != null) {
                    if (selected == phoneFragment) {
                        getSupportFragmentManager().beginTransaction().show(phoneFragment).commit();
                        getSupportFragmentManager().beginTransaction().hide(optionFragment).commit();
                        hideContactFragment();
                    } else if (selected == optionFragment) {
                        getSupportFragmentManager().beginTransaction().hide(phoneFragment).commit();
                        getSupportFragmentManager().beginTransaction().show(optionFragment).commit();
                        hideContactFragment();
                    } else if (selected == contactFragment) {
                        getSupportFragmentManager().beginTransaction().hide(phoneFragment).commit();
                        getSupportFragmentManager().beginTransaction().hide(optionFragment).commit();
                        hideContactFragment();
                        getSupportFragmentManager().beginTransaction().show(contactFragment).commit();
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
        /*if (MediaManager.getInstance().getPriorityCodec().equals(MediaManager.EVS)) {
            EvsManager.getInstance().init();
        }

        if (MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_NB)
                || MediaManager.getInstance().getPriorityCodec().equals(MediaManager.AMR_WB)) {
            AmrManager.getInstance().init();
        }*/
        //
    }

    private void hideContactFragment() {
        ContactManagerFragment contactManagerFragment = contactFragment.getContactManagerFragment();
        if (contactManagerFragment != null) {
            contactManagerFragment.finish();
        }
        contactFragment.clearContactListView();
        getSupportFragmentManager().beginTransaction().hide(contactFragment).commit();
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
