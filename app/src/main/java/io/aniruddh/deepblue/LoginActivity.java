package io.aniruddh.deepblue;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Camera;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.ason.Ason;
import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Form;
import com.afollestad.bridge.Request;
import com.afollestad.bridge.Response;

import io.aniruddh.deepblue.R;
import io.aniruddh.deepblue.utils.Tools;

public class LoginActivity extends AppCompatActivity {

    private View parent_view;
    private Button loginButton;
    private TextInputEditText usernameEdit;
    private TextInputEditText passwordEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        parent_view = findViewById(android.R.id.content);


        Tools.setSystemBarColor(this, android.R.color.white);
        Tools.setSystemBarLight(this);


        usernameEdit = (TextInputEditText) findViewById(R.id.usernameEdit);
        passwordEdit = (TextInputEditText) findViewById(R.id.passwordEdit);

        ((View) findViewById(R.id.forgot_password)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(parent_view, "Forgot Password", Snackbar.LENGTH_SHORT).show();
            }
        });
        ((View) findViewById(R.id.sign_up)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(parent_view, "Sign Up", Snackbar.LENGTH_SHORT).show();
                Intent calibCamera = new Intent(LoginActivity.this, CameraCalibration.class);
                startActivity(calibCamera);
            }
        });



        loginButton = (Button) findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Editable usernameEditable = usernameEdit.getText();
                Editable passwordEditable = passwordEdit.getText();
                String username = usernameEditable.toString();
                String password = passwordEditable.toString();

                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                String login_endpoint = Constants.SERVER_API + "login";

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


                Float f_x = prefs.getFloat("f_x", 0.89115971f);
                Float f_y = prefs.getFloat("f_y", 1.18821287f);

                String f_x_str = String.valueOf(f_x);
                String f_y_str = String.valueOf(f_y);

                Form form = new Form()
                        .add("username", username)
                        .add("password", password)
                        .add("f_x", f_x_str)
                        .add("f_y", f_y_str);
                try {
                    Request request = Bridge
                            .post(login_endpoint)
                            .body(form)
                            .request();

                    Response response = request.response();
                    Ason ason = new Ason(String.valueOf(response.asAsonObject()));

                    String status = ason.get("login");
                    if (status != null) {
                        if (status.contentEquals("successful")){
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("logged", true);
                            editor.commit();
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (BridgeException e) {
                    e.printStackTrace();
                }



            }
        });
    }
}
