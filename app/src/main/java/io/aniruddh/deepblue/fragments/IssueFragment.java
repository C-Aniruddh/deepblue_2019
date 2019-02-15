package io.aniruddh.deepblue.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.JsonArrayRequest;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import io.aniruddh.deepblue.ManualHelp;
import io.aniruddh.deepblue.SingleIssueActivity;
import io.aniruddh.deepblue.Constants;
import io.aniruddh.deepblue.SubmitActivity;
import io.aniruddh.deepblue.network.NetworkClass;
import io.aniruddh.deepblue.R;
import io.aniruddh.deepblue.models.Issue;

import static com.android.volley.VolleyLog.TAG;



public class IssueFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView recyclerView;
    private List<Issue> issueList;
    private StoreAdapter mAdapter;

    private FloatingActionButton new_issue;

    public IssueFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment IssueFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static IssueFragment newInstance(String param1, String param2) {
        IssueFragment fragment = new IssueFragment();
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
        View view = inflater.inflate(R.layout.fragment_issue, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        issueList = new ArrayList<>();
        mAdapter = new StoreAdapter(getActivity(), issueList);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(2), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        recyclerView.setNestedScrollingEnabled(false);

        new_issue = (FloatingActionButton) view.findViewById(R.id.new_issue);
        new_issue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent submitIssue = new Intent(getActivity(), ManualHelp.class);
                startActivity(submitIssue);
            }
        });

        fetchStoreItems();

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event

    private void fetchStoreItems() {
        final String URL = Constants.SERVER_API + "issues";
        JsonArrayRequest request = new JsonArrayRequest(URL,
                response -> {
                    if (response == null) {
                        Toast.makeText(getActivity(), "Couldn't fetch the issue List! Please try again.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<Issue> items = new Gson().fromJson(response.toString(), new TypeToken<List<Issue>>() {
                    }.getType());

                    issueList.clear();
                    issueList.addAll(items);

                    // refreshing recycler view
                    mAdapter.notifyDataSetChanged();
                },

                error -> {
                    // error in getting json
                    Log.e(TAG, "Error: " + error.getMessage());
                    Toast.makeText(getActivity(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });

        NetworkClass.getInstance().addToRequestQueue(request);
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    public interface ItemClickListener {
        void onClick(View view, int position, boolean isLongClick);
    }

    class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.MyViewHolder> {
        private Context context;
        private List<Issue> issueList;

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            public TextView title, locality;
            public ImageView thumbnail;
            private ItemClickListener clickListener;

            public MyViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.issue_title);
                locality = view.findViewById(R.id.locality);
                thumbnail = view.findViewById(R.id.thumbnail);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                clickListener.onClick(view, getPosition(), false);
            }

            public void setClickListener(ItemClickListener itemClickListener) {
                this.clickListener = itemClickListener;
            }
        }


        public StoreAdapter(Context context, List<Issue> issueList) {
            this.context = context;
            this.issueList = issueList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.issue_single, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, final int position) {
            final Issue issue = issueList.get(position);
            holder.title.setText(issue.getCategory());
            holder.locality.setText(issue.getLocality());

            String[] arr = issue.getImage_one_full().split("/");
            String filename = arr[arr.length - 1];

            String final_url = Constants.CDN_IMAGES + filename;

            Glide.with(context)
                    .load(final_url)
                    .into(holder.thumbnail);

            holder.setClickListener((view, position1, isLongClick) -> {
                /*String name = ((TextView) recyclerView.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.delegate_name)).getText().toString();
                Log.d("TAG", String.valueOf(delegateList.get(position)));*/
                Toast.makeText(getContext(), issue.getTitle(), Toast.LENGTH_SHORT).show();
                Intent details = new Intent(getContext(), SingleIssueActivity.class);
                details.putExtra("issue", issue);
                startActivity(details);

                // String issue_identifier = issue.getIssue_id();
            });
        }

        @Override
        public int getItemCount() {
            return issueList.size();
        }
    }
}

