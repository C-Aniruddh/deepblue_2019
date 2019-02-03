package io.aniruddh.deepblue;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.klinker.android.sliding.MultiShrinkScroller;
import com.klinker.android.sliding.SlidingActivity;

import io.aniruddh.deepblue.models.Issue;
import io.aniruddh.deepblue.tensorflow.Constants;
import io.aniruddh.deepblue.utils.Tools;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class SingleIssueActivity extends SlidingActivity {

    Issue issue;

    private GoogleMap mMap;

    private TextView issueAddress;
    private TextView issueDescription;
    private TextView issueCreator;

    private ImageView issueImage;
    private ImageView issueImageTwo;

    @Override
    public void init(Bundle savedInstanceState) {

        setPrimaryColors(
                getResources().getColor(R.color.blue_grey_600),
                getResources().getColor(R.color.blue_grey_800)
        );

        setContent(R.layout.activity_single_issue);


        issue = (Issue) getIntent().getSerializableExtra("issue");
        setTitle(issue.getCategory());

        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/circular-book.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());


        setUserImage(getImageURL(issue.getImage_one_full()));

        initMapFragment();

        issueAddress = (TextView) findViewById(R.id.issue_address);
        issueDescription = (TextView) findViewById(R.id.issue_description);
        issueCreator = (TextView) findViewById(R.id.issue_creator);

        issueImage = (ImageView) findViewById(R.id.issue_image);
        issueImageTwo = (ImageView) findViewById(R.id.issue_image_two);


        issueDescription.setText(issue.getDescription());
        issueAddress.setText(issue.getLocality());
        issueCreator.setText(issue.getUploaded_by());

        String image_one_url = getImageURL(issue.getImage_one_full());
        String image_two_url = getImageURL(issue.getImage_two_full());

        Glide.with(SingleIssueActivity.this)
                .load(image_one_url)
                .into(issueImage);

        Glide.with(SingleIssueActivity.this)
                .load(image_two_url)
                .into(issueImageTwo);
    }

    @Override
    protected void configureScroller(MultiShrinkScroller scroller) {
        super.configureScroller(scroller);
        scroller.setIntermediateHeaderHeightRatio(1);
    }

    public void setUserImage(String url) {
        Glide.with(this)
                .load(url)
                .into((ImageView) findViewById(R.id.photo));
    }

    String getImageURL(String in_string){
        String[] arr = in_string.split("/");
        String filename = arr[arr.length - 1];

        return Constants.CDN_IMAGES + filename;
    }

    private void initMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = Tools.configActivityMaps(googleMap);
                MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(issue.getLat(), issue.getLon()));
                mMap.addMarker(markerOptions);
                mMap.moveCamera(zoomingLocation());
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        try {
                            mMap.animateCamera(zoomingLocation());
                        } catch (Exception e) {
                        }
                        return true;
                    }
                });
            }
        });
    }

    private CameraUpdate zoomingLocation() {
        return CameraUpdateFactory.newLatLngZoom(new LatLng(19.078359, 72.955731), 10);
    }

    public void viewComments(View v){

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }


}
