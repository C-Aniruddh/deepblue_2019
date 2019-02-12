package io.aniruddh.deepblue;

import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Form;
import com.afollestad.bridge.Request;

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

                Form form = new Form()
                        .add("email", username)
                        .add("password", password);
                try {
                    Request request = Bridge
                            .post(login_endpoint)
                            .body(form)
                            .request();
                } catch (BridgeException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}
