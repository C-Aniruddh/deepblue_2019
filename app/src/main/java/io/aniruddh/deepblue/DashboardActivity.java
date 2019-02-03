package io.aniruddh.deepblue;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.ar.core.CameraIntrinsics;

import io.aniruddh.deepblue.fragments.HomeFragment;
import io.aniruddh.deepblue.fragments.IssueFragment;
import io.aniruddh.deepblue.fragments.ProfileFragment;
import io.aniruddh.deepblue.utils.Tools;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class DashboardActivity extends AppCompatActivity implements HomeFragment.OnFragmentInteractionListener{

    private TabLayout tab_layout;
    private NestedScrollView nested_scroll_view;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/circular-book.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());
        initComponent();


        Fragment temporary_fragment = new HomeFragment();
        loadFragment(temporary_fragment);

        CameraManager manager =
                (CameraManager)getSystemService(CAMERA_SERVICE);

        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics chars
                        = manager.getCameraCharacteristics(cameraId);
                // Do something with the characteristics
                float[] parameters = new float[5];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    parameters = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    for (int i = 0; i < 1; i++){
                        Toast.makeText(getApplicationContext(), String.valueOf(parameters[i]), Toast.LENGTH_SHORT).show();
                    }

                }
            }
        }  catch(CameraAccessException e){
                e.printStackTrace();
        }



    }

    private void initComponent() {
        nested_scroll_view = (NestedScrollView) findViewById(R.id.nested_scroll_view);
        tab_layout = (TabLayout) findViewById(R.id.tab_layout);

        tab_layout.addTab(tab_layout.newTab().setIcon(R.drawable.ic_home), 0);
        tab_layout.addTab(tab_layout.newTab().setIcon(R.drawable.map_plus), 1);
        tab_layout.addTab(tab_layout.newTab().setIcon(R.drawable.car_brake_parking), 2);
        tab_layout.addTab(tab_layout.newTab().setIcon(R.drawable.account_details), 3);

        // set icon color pre-selected
        tab_layout.getTabAt(0).getIcon().setColorFilter(getResources().getColor(R.color.blue_grey_400), PorterDuff.Mode.SRC_IN);
        tab_layout.getTabAt(1).getIcon().setColorFilter(getResources().getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN);
        tab_layout.getTabAt(2).getIcon().setColorFilter(getResources().getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN);
        tab_layout.getTabAt(3).getIcon().setColorFilter(getResources().getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN);

        tab_layout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(getResources().getColor(R.color.blue_grey_400), PorterDuff.Mode.SRC_IN);
                onTabClicked(tab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(getResources().getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabClicked(tab);
            }
        });

        Tools.setSystemBarColor(this, R.color.grey_5);
        Tools.setSystemBarLight(this);
    }

    private void onTabClicked(TabLayout.Tab tab){
        Fragment fragment;
        switch (tab.getPosition()) {
            case 0:
                fragment = new HomeFragment();
                loadFragment(fragment);
                Toast.makeText(getApplicationContext(), "Home", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(getApplicationContext(), "Maps", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(DashboardActivity.this, MapActivity.class);
                startActivity(i);
                break;
            case 2:
                fragment = new IssueFragment();
                loadFragment(fragment);
                Toast.makeText(getApplicationContext(), "Issues", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                fragment = new ProfileFragment();
                // loadFragment(fragment);
                // Toast.makeText(getApplicationContext(), "Profile", Toast.LENGTH_SHORT).show();
                Intent startCalib = new Intent(DashboardActivity.this, CameraCalibration.class);
                startActivity(startCalib);
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_setting, menu);
        Tools.changeMenuIconColor(menu, getResources().getColor(R.color.grey_60));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else {
            Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFragment(Fragment fragment) {
        // load fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }
}