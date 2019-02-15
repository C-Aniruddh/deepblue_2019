package io.aniruddh.deepblue.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.ason.Ason;
import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Request;
import com.afollestad.bridge.Response;

import io.aniruddh.deepblue.Constants;
import io.aniruddh.deepblue.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView fullName;
    private TextView emailID;
    private TextView usernameView;
    private TextView issues;
    private TextView references;


    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        fullName = (TextView) view.findViewById(R.id.full_name);
        emailID = (TextView) view.findViewById(R.id.emailView);
        usernameView = (TextView) view.findViewById(R.id.usernameView);

        issues = (TextView) view.findViewById(R.id.userIssues);
        references = (TextView) view.findViewById(R.id.userReferences);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String username = prefs.getString("username", "aniruddh");

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        String api_endpoint = Constants.SERVER_API + "profile/" + username;
        try {
            Request request = Bridge
                    .get(api_endpoint)
                    .request();
            Response response = request.response();
            Ason data = response.asAsonObject();

            String user_email = data.getString("email");
            String user_issues = data.getString("issues");
            String user_references = data.getString("references");
            String user_fullname = data.getString("fullname");
            usernameView.setText(username);
            emailID.setText(user_email);
            issues.setText(user_issues);
            references.setText(user_references);
            fullName.setText(user_fullname);


        } catch (BridgeException e) {
            e.printStackTrace();
        }

        return view;
    }

}
