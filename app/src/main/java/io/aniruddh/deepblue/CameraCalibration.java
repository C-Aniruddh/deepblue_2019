package io.aniruddh.deepblue;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraIntrinsics;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;


public class CameraCalibration extends AppCompatActivity {
    private static final String TAG = CameraCalibration.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    ArFragment arFragment;
    ModelRenderable lampPostRenderable;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_camera_calibration);



        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);


        ModelRenderable.builder()
                .setSource(this, Uri.parse("Astronaut.sfb"))
                .build()
                .thenAccept(renderable -> lampPostRenderable = renderable)
                .exceptionally(throwable -> {
                    Toast toast =
                            Toast.makeText(this, "Unable to load any renderable", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return null;
                });

        arFragment.setOnTapArPlaneListener(new BaseArFragment.OnTapArPlaneListener() {
            @Override
            public void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
                if (lampPostRenderable == null){
                    return;
                }
                Anchor anchor = hitResult.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());
                TransformableNode lamp = new TransformableNode(arFragment.getTransformationSystem());
                lamp.setParent(anchorNode);
                lamp.setRenderable(lampPostRenderable);
                lamp.select();

                Camera camera = arFragment.getArSceneView().getArFrame().getCamera();

                CameraIntrinsics ci = camera.getImageIntrinsics();
                int[] dimens = ci.getImageDimensions();
                float[] focals = ci.getFocalLength();
                //String foc = "Focals = {" + String.valueOf(focals[0]) + ", " + String.valueOf(focals[1]) + "}.";
                String foc = "Dimens = {" + String.valueOf(dimens[0]) + ", " + String.valueOf(dimens[1]) + "}.";
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("f_x", focals[0]);
                editor.putFloat("f_y", focals[1]);

                editor.commit();
                Toast.makeText(getApplicationContext(), "Camera Calibrated! ;)", Toast.LENGTH_LONG).show();
            }
        });

    }
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
