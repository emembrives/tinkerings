package fr.membrives.dispotrains;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import fr.membrives.dispotrains.adapters.StationAdapter;
import fr.membrives.dispotrains.data.Line;
import fr.membrives.dispotrains.data.Station;

public class StationListActivity extends ListActivity {
    private DataSource mDataSource;
    private Line line;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.station_list_activity);
        // Create the dummy account
        mDataSource = new DataSource(this);
        mDataSource.open();

        getActionBar().setDisplayHomeAsUpEnabled(true);
        line = (Line) getIntent().getExtras().getParcelable("line");
    }

    @Override
    protected void onResume() {
        super.onResume();

        getActionBar().setTitle(line.getNetwork() + " " + line.getId());

        List<Station> stations = new ArrayList<Station>(mDataSource.getStationsPerLine(line));
        Collections.sort(stations);
        setListAdapter(new StationAdapter(this, stations));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Station item = (Station) getListAdapter().getItem(position);
        Intent appInfo = new Intent(StationListActivity.this, StationDetailActivity.class);
        appInfo.putExtra("line", line);
        appInfo.putExtra("station", item);
        startActivity(appInfo);
    }
}
