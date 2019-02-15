package io.aniruddh.deepblue;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import io.aniruddh.deepblue.representation.Quaternion;
import io.aniruddh.deepblue.utils.Tools;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class CameraActivity extends AppCompatActivity {


    private CameraView cameraView;
    private TextView debugData;
    private FloatingActionButton takePicture;

    private OrientationProvider orientationProvider;
    private Quaternion quaternion = new Quaternion();

    private FusedLocationProviderClient mFusedLocationClient;

    public static final String TAG = "Maps";

    private int photo_id;

    Float angle;
    Float x;
    Float y;
    Float z;

    Double lat = 19.111240;
    Double lon = 73.016365;

    private int random_issue_number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);

        photo_id = 1;

        // GPSTracker gpsTracker = new GPSTracker(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/circular-book.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());

        cameraView = (CameraView) findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);

        Random ran = new Random();
        random_issue_number = ran.nextInt(5000) + 1000;

        takePicture = (FloatingActionButton) findViewById(R.id.take_picture);

        Tools.setSystemBarTransparent(this);

        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.capturePicture();
            }
        });

        SensorChecker checker = new HardwareChecker((SensorManager) getSystemService(SENSOR_SERVICE));
        if (!checker.IsGyroscopeAvailable()) {
            // If a gyroscope is unavailable, display a warning.
            displayHardwareMissingWarning();
        }

        LottieAnimationView animationView = (LottieAnimationView) findViewById(R.id.cameraAnim);
        animationView.setAnimation("animations/left.json");
        animationView.loop(true);
        animationView.playAnimation();

        orientationProvider = new OrientationSensor1Provider((SensorManager) getSystemService(SENSOR_SERVICE));

        Location currentLocation = null;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                        }
                    }
                });

        String loc = String.valueOf(lat) + ", " + String.valueOf(lon);
        Toast.makeText(getApplicationContext(), loc, Toast.LENGTH_SHORT).show();


        cameraView.setLocation(lat, lon);
        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                // Create a bitmap or a file...
                // CameraUtils will read EXIF orientation for you, in a worker thread.
                super.onPictureTaken(picture);
                new SavePhotoTask().execute(picture);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();


                if (photo_id == 1) {
                    //cameraView.capturePicture();
                    animationView.setVisibility(View.VISIBLE);
                    String filename = "Pothole_" + photo_id + "_" + random_issue_number + ".jpg";
                    photo_id = photo_id + 1;
                    File imagesFolder = new File(Environment.getExternalStorageDirectory(), "CSP");
                    String full_path = imagesFolder.getAbsolutePath() + '/' + filename;
                    editor.putString("photo_one", full_path);
                    editor.commit();
                } else {
                    String filename = "Pothole_" + photo_id + "_" + random_issue_number + ".jpg";
                    File imagesFolder = new File(Environment.getExternalStorageDirectory(), "CSP");
                    String full_path = imagesFolder.getAbsolutePath() + '/' + filename;
                    editor.putString("photo_two", full_path);
                    editor.commit();
                    animationView.setAnimation("animations/tick.json");
                    animationView.loop(false);
                    animationView.playAnimation();
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent data = new Intent();
                            String text = "Pictures Taken";
                            data.setData(Uri.parse(text));
                            setResult(RESULT_OK, data);
                            finish();
                            finish();
                        }
                    }, 1000);
                }

            }
        });


    }

    private void getDeviceLocation() {
        //get user location for map activity
        Log.d(TAG, "Get Present Location");


    }

    private void displayHardwareMissingWarning() {
        AlertDialog ad = new AlertDialog.Builder(this).create();
        ad.setCancelable(false); // This blocks the 'BACK' button
        ad.setTitle(getResources().getString(R.string.gyroscope_missing));
        ad.setMessage(getResources().getString(R.string.gyroscope_missing_message));
        ad.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.show();
    }


    class SavePhotoTask extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]... jpeg) {
            String filename = "Pothole_" + photo_id + "_" + random_issue_number + ".jpg";

            File imagesFolder = new File(Environment.getExternalStorageDirectory(), "CSP");
            imagesFolder.mkdirs();
            File photo = new File(imagesFolder, filename);
            if (photo.exists()) {
                photo.delete();
            }
            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());
                fos.write(jpeg[0]);
                fos.close();
            } catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }
            return (null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.destroy();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }
}
