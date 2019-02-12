package io.aniruddh.deepblue;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import io.aniruddh.deepblue.models.Issue;
import io.aniruddh.deepblue.utils.Tools;

public class IssueActivity extends AppCompatActivity {

    private GoogleMap mMap;
    private BottomSheetBehavior bottomSheetBehavior;

    private TextView issueCategory;
    private TextView issueLocality;
    private TextView issueAddress;
    private TextView issueDescription;

    private ImageView issueImage;
    private ImageView issueImageTwo;

    Issue issue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue);

        issue = (Issue) getIntent().getSerializableExtra("issue");

        initMapFragment();
        // initIssueDetails();
        initComponent();
        Toast.makeText(this, "Swipe up bottom sheet", Toast.LENGTH_SHORT).show();
    }


    private void initComponent() {
        // get the bottom sheet view
        LinearLayout llBottomSheet = (LinearLayout) findViewById(R.id.bottom_sheet);

        // init the bottom sheet behavior
        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);

        // change the state of the bottom sheet
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // set callback for changes
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        ((FloatingActionButton) findViewById(R.id.fab_directions)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                try {
                    mMap.animateCamera(zoomingLocation());
                } catch (Exception e) {
                }
            }
        });

        issueCategory = (TextView) findViewById(R.id.issue_category);
        issueLocality = (TextView) findViewById(R.id.issue_locality);
        issueAddress = (TextView) findViewById(R.id.issue_address);
        issueDescription = (TextView) findViewById(R.id.issue_description);

        issueImage = (ImageView) findViewById(R.id.issue_image);
        issueImageTwo = (ImageView) findViewById(R.id.issue_image_two);

        issueCategory.setText(issue.getCategory());
        issueLocality.setText(issue.getLocality());
        issueAddress.setText(issue.getLocality());
        issueDescription.setText(issue.getDescription());

        String image_one_url = getImageURL(issue.getImage_one_full());
        String image_two_url = getImageURL(issue.getImage_two_full());

        Glide.with(IssueActivity.this)
                .load(image_one_url)
                .into(issueImage);

        Glide.with(IssueActivity.this)
                .load(image_two_url)
                .into(issueImageTwo);
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
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
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

}
