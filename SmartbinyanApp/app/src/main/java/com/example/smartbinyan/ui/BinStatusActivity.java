package com.example.smartbinyan.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.example.smartbinyan.R;

import java.util.ArrayList;
import java.util.List;

public class BinStatusActivity extends AppCompatActivity {

    private PieChart pieChart;
    private LineChart lineChart;
    private TextView tvBinTitle, tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bin_status);



        // Receive data from DashboardActivity
        String binName = getIntent().getStringExtra("binName");
        int gasPercent = getIntent().getIntExtra("gasPercent", 0);
        int biodegradablePercent = getIntent().getIntExtra("biodegradablePercent", 40);
        int nonToxicPercent = getIntent().getIntExtra("nonToxicPercent", 35);
        int toxicPercent = getIntent().getIntExtra("toxicPercent", 25);
        String statusText = getIntent().getStringExtra("statusText");

        // Update text views
        tvBinTitle.setText(binName != null ? binName : "Unknown Bin");
        tvStatus.setText(statusText != null ? statusText : "Gas: " + gasPercent + "% • Status: Normal");

        // Setup charts
        setupLineChart(gasPercent);
        setupPieChart(biodegradablePercent, nonToxicPercent, toxicPercent);
    }

    private void setupLineChart(int gasPercent) {
        List<Entry> entries = new ArrayList<>();
        // Simple linear trend from 0 to current gas percent for display
        for (int i = 0; i < 12; i++) {
            float value = gasPercent * i / 12f;
            entries.add(new Entry(i, value));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Gas Levels (ppm approx)");
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setCircleColor(Color.parseColor("#1E88E5"));
        dataSet.setColor(Color.parseColor("#1E88E5"));
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(true);
        lineChart.invalidate(); // refresh chart
    }

    private void setupPieChart(int biodegradable, int nonToxic, int toxic) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(biodegradable, "Biodegradable"));
        entries.add(new PieEntry(nonToxic, "Non-Toxic"));
        entries.add(new PieEntry(toxic, "Toxic"));

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(76, 175, 80));   // Green
        colors.add(Color.rgb(33, 150, 243));  // Blue
        colors.add(Color.rgb(244, 67, 54));   // Red

        PieDataSet dataSet = new PieDataSet(entries, "Waste Categories");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setCenterText("Waste Types");
        pieChart.setCenterTextSize(14f);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.CIRCLE);

        pieChart.invalidate(); // refresh chart
    }
}
