package fr.membrives.dispotrains;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

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
        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
        ContentResolver.addPeriodicSync(mAccount, AUTHORITY, new Bundle(), 1800);

        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        Log.d("MainActivity", "RequestSync " + mAccount.toString());
        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);

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
                ProgressBar progressBar = ((ProgressBar) findViewById(R.id.loader));
                if (isSyncing) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_about:
                showAbout();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAbout() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        // When linking text, force to always use default color. This works
        // around a pressed color state bug.

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.setCancelable(true);
        builder.setPositiveButton("OK", null);
        builder.create();
        builder.show();
    }
}
