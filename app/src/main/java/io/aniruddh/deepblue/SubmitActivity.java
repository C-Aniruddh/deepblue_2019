package io.aniruddh.deepblue;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Callback;
import com.afollestad.bridge.MultipartForm;
import com.afollestad.bridge.Pipe;
import com.afollestad.bridge.ProgressCallback;
import com.afollestad.bridge.Request;
import com.afollestad.bridge.Response;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;

import io.aniruddh.deepblue.fragments.IssueSubmittedFragment;
import io.aniruddh.deepblue.utils.Tools;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class SubmitActivity extends AppCompatActivity {


    private ImageButton addImages;
    private ImageView image_one;
    private ImageView image_two;

    private TextView imageOneStatus;
    private TextView imageTwoStatus;

    private Button addImagesButton;

    private EditText issueCategory;
    private EditText issueDescription;

    private FloatingActionButton submitButton;

    private ProgressDialog progressDoalog;

    private int requestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit);
        initToolbar();

        addImages = (ImageButton) findViewById(R.id.add_images);
        image_one = (ImageView) findViewById(R.id.image_one);
        image_two = (ImageView) findViewById(R.id.image_two);

        imageOneStatus = (TextView) findViewById(R.id.imageOneStatus);
        imageTwoStatus = (TextView) findViewById(R.id.imageTwoStatus);

        addImagesButton = (Button) findViewById(R.id.button_add_images);

        submitButton = (FloatingActionButton) findViewById(R.id.submit_issue);

        issueCategory = (EditText) findViewById(R.id.editCategory);
        issueDescription = (EditText) findViewById(R.id.editDescription);


        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/circular-book.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());

        addImagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startCamera = new Intent(SubmitActivity.this, CameraActivity.class);
                startActivityForResult(startCamera, requestCode);
            }
        });

        addImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startCamera = new Intent(SubmitActivity.this, CameraActivity.class);
                startActivityForResult(startCamera, requestCode);
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                String username = prefs.getString("username", "aniruddh");
                String image_onePath = prefs.getString("photo_one", "none");
                String image_twoPath = prefs.getString("photo_two", "none");

                File imgFileOne = new File(image_onePath);
                File imgFileTwo = new File(image_twoPath);

                Editable issueCat = issueCategory.getText();
                String issueCate = issueCat.toString();

                Editable issueDes = issueDescription.getText();
                String issueDesc = issueDes.toString();

                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                MultipartForm form = null;

                try {
                    form = new MultipartForm()
                            .add("image_one", String.valueOf((new File(imgFileOne.getName()))), Pipe.forFile(new File(imgFileOne.getAbsolutePath())))
                            .add("image_two", String.valueOf((new File(imgFileTwo.getName()))), Pipe.forFile(new File(imgFileTwo.getAbsolutePath())))
                            .add("project_category", String.valueOf(issueCate))
                            .add("description", String.valueOf(issueDesc))
                            .add("username", String.valueOf(username));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String api_endpoint = Constants.SERVER_API + "issues/create";

                progressDoalog = new ProgressDialog(SubmitActivity.this);
                progressDoalog.setMax(100);
                progressDoalog.setMessage("We're now working on the images you have submitted!");
                progressDoalog.setTitle("Uploading issue!");
                progressDoalog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDoalog.show();

                Bridge
                        .post(api_endpoint)
                        .cancellable(false)
                        .body(form)
                        .connectTimeout(5000)
                        .readTimeout(500000)
                        .uploadProgress(new ProgressCallback() {
                            @Override
                            public void progress(Request request, int current, int total, int percent) {
                                //dialog.setProgress(percent);
                                progressDoalog.setProgress(percent);
                            }
                        })
                        .request(new Callback() {
                            @Override
                            public void response(Request request, Response response, BridgeException e) {
                                // Use response
                                progressDoalog.dismiss();
                                showDialogPaymentSuccess();

                                Snackbar.make(getWindow().getDecorView().getRootView(), "Uploaded", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        })
                        .response();

            }
        });
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close);
        toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.grey_60), PorterDuff.Mode.SRC_ATOP);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Tools.setSystemBarColor(this, R.color.grey_5);
        Tools.setSystemBarLight(this);
    }

    private void showDialogPaymentSuccess() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        IssueSubmittedFragment newFragment = new IssueSubmittedFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(android.R.id.content, newFragment).addToBackStack(null).commit();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String returnedResult = data.getData().toString();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            String image_onePath = prefs.getString("photo_one", "none");
            String image_twoPath = prefs.getString("photo_two", "none");
            Toast.makeText(getApplicationContext(), image_onePath, Toast.LENGTH_SHORT).show();


            File imgFileOne = new File(image_onePath);
            Uri imageOneUri = Uri.fromFile(imgFileOne);

            Glide.with(this)
                    .load(imageOneUri)
                    .into(image_one);

            File imgFileTwo = new File(image_twoPath);
            Uri imageTwoUri = Uri.fromFile(imgFileTwo);

            Glide.with(this)
                    .load(imageTwoUri)
                    .into(image_two);

            imageOneStatus.setText("Image Added");
            imageTwoStatus.setText("Image Added");

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_setting, menu);
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

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }
}
