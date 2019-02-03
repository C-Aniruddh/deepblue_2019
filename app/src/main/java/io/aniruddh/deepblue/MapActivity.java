package io.aniruddh.deepblue;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.javiersantos.materialstyleddialogs.enums.Style;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import io.aniruddh.deepblue.R;
import io.aniruddh.deepblue.utils.Tools;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class MapActivity extends AppCompatActivity {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/circular-book.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());

        initMapFragment();
        Tools.setSystemBarColor(this, R.color.blue_grey_600);
    }

    private void initMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMap -> {
            mMap = Tools.configActivityMaps(googleMap);
            MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(19.067909, 72.866284)).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher));
            mMap.addMarker(markerOptions);
            mMap.moveCamera(zoomingLocation());
            mMap.setOnMarkerClickListener(marker -> {
                try {
                    mMap.animateCamera(zoomingLocation());
                } catch (Exception e) {
                }
                return true;
            });
        });
    }

    private CameraUpdate zoomingLocation() {
        return CameraUpdateFactory.newLatLngZoom(new LatLng(19.078359, 72.955731), 10);
    }


    public void clickAction(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.map_button:
                Toast.makeText(getApplicationContext(), "Map Clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.list_button:
                Toast.makeText(getApplicationContext(), "List Clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.add_button:
                new MaterialStyledDialog.Builder(this)
                        .setTitle("Create New Issue")
                        .setDescription("Choose the suitable method!")
                        .setScrollable(true)
                        .setStyle(Style.HEADER_WITH_TITLE)
                        .setPositiveText("Autonomous Mode")
                        .onPositive((dialog, which) -> {
                            new MaterialStyledDialog.Builder(this)
                                    .setTitle("Autonomous Mode")
                                    .setDescription(R.string.autonomous_desc)
                                    .setStyle(Style.HEADER_WITH_TITLE)
                                    .setCancelable(true)
                                    .setPositiveText("START")
                                    .onPositive((dialog1, which1) -> {
                                        Intent startAutonomous = new Intent(MapActivity.this, DetectorActivity.class);
                                        startActivity(startAutonomous);
                                    })
                                    .setNeutralText("Learn More")
                                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            Toast.makeText(getApplicationContext(), "Learn More clicked", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .show();
                        })
                        .setNeutralText("Manual Mode")
                        .onNeutral((dialog, which) -> {
                            new MaterialStyledDialog.Builder(this)
                                    .setTitle("Manual Mode")
                                    .setDescription(R.string.autonomous_desc)
                                    .setCancelable(true)
                                    .setStyle(Style.HEADER_WITH_TITLE)
                                    .setPositiveText("START")
                                    .onPositive((dialog1, which1) -> {
                                        Intent startAutonomous = new Intent(MapActivity.this, DetectorActivity.class);
                                        startActivity(startAutonomous);
                                    })
                                    .setNeutralText("Learn More")
                                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            Toast.makeText(getApplicationContext(), "Learn More clicked", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .show();
                        })
                        .setScrollable(true, 10)
                        .show();
                Toast.makeText(getApplicationContext(), "Add Clicked", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }
}
