package io.aniruddh.deepblue.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.airbnb.lottie.LottieAnimationView;

import io.aniruddh.deepblue.CameraCalibration;
import io.aniruddh.deepblue.R;

public class SlideFive extends Fragment {
    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private int layoutResId;

    public SlideFive() {
    }

    public static SlideFive newInstance(int layoutResId) {
        SlideFive sampleSlide = new SlideFive();

        Bundle bundleArgs = new Bundle();
        bundleArgs.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        sampleSlide.setArguments(bundleArgs);

        return sampleSlide;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID))
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.slide_five, container, false);
        LottieAnimationView animationView = (LottieAnimationView) view.findViewById(R.id.slideFiveAnimation);
        animationView.setAnimation("animations/smile.json");
        animationView.loop(true);
        animationView.playAnimation();

        return view;
    }

}