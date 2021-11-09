package com.jamesj.voip_phone_android;

import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class MasterActivity extends FragmentActivity {

    ///////////////////////////////////////////////

    private TabLayout tabLayout;

    private MainActivity mainActivity;
    private OptionActivity optionActivity;

    ///////////////////////////////////////////////

    private long lastTimeBackPressed;

    ///////////////////////////////////////////////

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        optionActivity = new OptionActivity();
        mainActivity = new MainActivity();

        getSupportFragmentManager().beginTransaction().add(R.id.container, optionActivity).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.container, mainActivity).commit();

        getSupportFragmentManager().beginTransaction().show(mainActivity).commit();
        getSupportFragmentManager().beginTransaction().hide(optionActivity).commit();

        tabLayout = findViewById(R.id.tabs);
        tabLayout.addTab(tabLayout.newTab().setText("phone"));
        tabLayout.addTab(tabLayout.newTab().setText("option"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Fragment selected = null;

                if(position == 0) {
                    selected = mainActivity;
                }
                else if(position == 1) {
                    selected = optionActivity;
                }

                if (selected != null) {
                    if (selected == mainActivity) {
                        getSupportFragmentManager().beginTransaction().show(mainActivity).commit();
                        getSupportFragmentManager().beginTransaction().hide(optionActivity).commit();
                    } else if (selected == optionActivity) {
                        getSupportFragmentManager().beginTransaction().show(optionActivity).commit();
                        getSupportFragmentManager().beginTransaction().hide(mainActivity).commit();
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
    }

    ///////////////////////////////////////////////

    @Override
    public void onBackPressed() {

        //프래그먼트 onBackPressedListener사용
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragmentList) {
            if (fragment instanceof OnBackPressedListener) {
                ((OnBackPressedListener) fragment).onBackButtonPressed();
                return;
            }
        }

        //두 번 클릭시 어플 종료
        if (System.currentTimeMillis() - lastTimeBackPressed < 1500) {
            finish();
            return;
        }
        lastTimeBackPressed = System.currentTimeMillis();
        Toast.makeText(this, "'뒤로' 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();

    }
}
