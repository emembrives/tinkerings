package fr.membrives.dispotrains;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.membrives.dispotrains.adapters.LineAdapter;
import fr.membrives.dispotrains.data.Line;

public class MainActivity extends ListeningActivity {

    private DataSource mDataSource;
    volatile private List<Line> mLines;
    private LineAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Turn on automatic syncing for the default account and authority
        ((SwipeRefreshLayout) findViewById(R.id.swipe_refresh)).setOnRefreshListener(this);
        doRefresh();

        mDataSource = new DataSource(this);

        mLines = new ArrayList<Line>(mDataSource.getAllLines());
        Collections.sort(mLines);
        mAdapter = new LineAdapter(this, mLines);
        setListAdapter(mAdapter);
    }

    @Override
    protected void updateIsSyncing(final boolean isSyncing) {
        final List<Line> lines = new ArrayList<Line>(mDataSource.getAllLines());
        Collections.sort(lines);
        runOnUiThread(new Runnable() {
            public void run() {
                mLines.clear();
                mLines.addAll(lines);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Line item = (Line) getListAdapter().getItem(position);
        Intent appInfo = new Intent(MainActivity.this, StationListActivity.class);
        appInfo.putExtra("line", item);
        startActivity(appInfo);
    }
}
