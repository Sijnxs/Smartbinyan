package com.example.smartbinyan.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.smartbinyan.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

public class BinStatusActivity extends AppCompatActivity {

    private TextView tvStatus;
    private ExpandableListView expandableListViewToxic, expandableListViewNonToxic, expandableListViewFood;

    private FirebaseFirestore firestore;

    // Stores the list of image/time data for each category
    private List<Map<String, String>> toxicList = new ArrayList<>();
    private List<Map<String, String>> nonToxicList = new ArrayList<>();
    private List<Map<String, String>> foodList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bin_status);

        tvStatus = findViewById(R.id.tvStatus);
        expandableListViewToxic = findViewById(R.id.expandableListViewToxic);
        expandableListViewNonToxic = findViewById(R.id.expandableListViewNonToxic);
        expandableListViewFood = findViewById(R.id.expandableListViewFood);

        firestore = FirebaseFirestore.getInstance();

        // Listen to Firestore collection in real time
        firestore.collection("trash_history")
                // Filter: Only include items where 'is_trash' is true ("if the firebase said so")
                .whereEqualTo("is_trash", true)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            tvStatus.setText("Error loading trash data");
                            return;
                        }

                        // Clear all lists for fresh data
                        toxicList.clear();
                        nonToxicList.clear();
                        foodList.clear();

                        if (snapshot != null) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                // Fetch image URL and type (removing name)
                                String imageUrl = doc.getString("imageUrl");
                                String type = doc.getString("type");
                                Object timestampObj = doc.get("timestamp");

                                // Continue only if essential fields are present
                                if (imageUrl == null || type == null || imageUrl.isEmpty()) continue;

                                String formattedTime = "";
                                if (timestampObj != null) {
                                    if (timestampObj instanceof Number) {
                                        formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                                Locale.getDefault()).format(new Date(((Number) timestampObj).longValue()));
                                    } else if (timestampObj instanceof Timestamp) {
                                        formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                                Locale.getDefault()).format(((Timestamp) timestampObj).toDate());
                                    }
                                }

                                Map<String, String> entry = new HashMap<>();
                                entry.put("imageUrl", imageUrl);
                                entry.put("formattedTime", formattedTime);

                                switch (type.toLowerCase()) {
                                    case "toxic":
                                        toxicList.add(entry);
                                        break;
                                    case "non-toxic":
                                        nonToxicList.add(entry);
                                        break;
                                    case "food":
                                        foodList.add(entry);
                                        break;
                                }
                            }
                        }

                        // Setup lists
                        setupExpandableList(expandableListViewToxic, toxicList, "Toxic Waste");
                        setupExpandableList(expandableListViewNonToxic, nonToxicList, "Non-Toxic Waste");
                        setupExpandableList(expandableListViewFood, foodList, "Food Waste");

                        // Auto-expand all lists (to "automatically go there")
                        if (expandableListViewToxic != null) expandableListViewToxic.expandGroup(0);
                        if (expandableListViewNonToxic != null) expandableListViewNonToxic.expandGroup(0);
                        if (expandableListViewFood != null) expandableListViewFood.expandGroup(0);

                        tvStatus.setText("Last updated: " +
                                new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
                    }
                });
    }

    private void setupExpandableList(ExpandableListView listView, List<Map<String, String>> data, String groupTitle) {
        if (listView == null) return; // Prevent crash if a list view is missing

        ArrayList<String> group = new ArrayList<>();
        // MODIFICATION: Append the count to the group title
        String newGroupTitle = groupTitle + " (Count: " + data.size() + ")";
        group.add(newGroupTitle);

        listView.setAdapter(new ImageHistoryAdapter(this, group, data));
    }

    private class ImageHistoryAdapter extends BaseExpandableListAdapter {
        private Context context;
        private List<String> groupList;
        // **MODIFIED:** Removed unused 'childData' field.
        private List<Map<String, String>> data; // Holds the image/time data

        public ImageHistoryAdapter(Context context, List<String> groupList, List<Map<String, String>> data) {
            this.context = context;
            this.groupList = groupList;
            this.data = data;
        }

        @Override public int getGroupCount() { return groupList.size(); }
        @Override public int getChildrenCount(int groupPosition) { return data.size(); }
        @Override public Object getGroup(int groupPosition) { return groupList.get(groupPosition); }
        @Override public Object getChild(int groupPosition, int childPosition) { return data.get(childPosition); }
        @Override public long getGroupId(int groupPosition) { return groupPosition; }
        @Override public long getChildId(int groupPosition, int childPosition) { return childPosition; }
        @Override public boolean hasStableIds() { return false; }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            TextView textView = new TextView(context);
            textView.setText(groupList.get(groupPosition));
            textView.setPadding(70, 40, 40, 40);
            textView.setTextSize(18);
            textView.setTextColor(0xFF1B5E20);
            return textView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            // Using the correct inflation method: parent, false
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate( R.layout.list_item_image_trash, parent, false);
            }

            ImageView imageView = convertView.findViewById(R.id.imageViewTrash);
            TextView textViewTime = convertView.findViewById(R.id.textViewTimestamp);

            // --- MODIFICATION START ---
            // 1. Get the time data
            Map<String, String> item = data.get(childPosition);
            String formattedTime = item.get("formattedTime");

            // 2. Hide the ImageView as requested
            if (imageView != null) {
                imageView.setVisibility(View.GONE);
                // Clear any potential image loading to save resources
                Glide.with(context).clear(imageView);
            }

            // 3. Display only the timestamp history
            textViewTime.setText("Logged at: " + formattedTime);
            // --- MODIFICATION END ---

            return convertView;
        }
        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) { return false; }
    }
}