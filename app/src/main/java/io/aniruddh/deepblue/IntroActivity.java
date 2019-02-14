package io.aniruddh.deepblue;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.View;

import com.github.paolorotolo.appintro.AppIntro;

import io.aniruddh.deepblue.fragments.SlideFive;
import io.aniruddh.deepblue.fragments.SlideFour;
import io.aniruddh.deepblue.fragments.SlideOne;
import io.aniruddh.deepblue.fragments.SlideThree;
import io.aniruddh.deepblue.fragments.SlideTwo;

public class IntroActivity extends AppIntro {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        addSlide(SlideOne.newInstance(R.layout.slide_one));
        addSlide(SlideTwo.newInstance(R.layout.slide_two));
        addSlide(SlideThree.newInstance(R.layout.slide_three));
        addSlide(SlideFour.newInstance(R.layout.slide_four));
        addSlide(SlideFive.newInstance(R.layout.slide_five));

        askForPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_CONTACTS}, 2);
        askForPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 3);


        //setSeparatorColor(Color.parseColor("#2196F3"));
    }


    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("first_time", false);
        editor.commit();

        Intent login = new Intent(IntroActivity.this, LoginActivity.class);
        startActivity(login);
        finish();
    }

}
