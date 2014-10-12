package fr.membrives.dispotrains;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import fr.membrives.dispotrains.adapters.LineAdapter;
import fr.membrives.dispotrains.data.Station;

public class StationListActivity extends Activity {
    private DataSource mDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_list);
        // Create the dummy account
        mDataSource = new DataSource(this);
        mDataSource.open();

        List<Station> lines = new ArrayList<Station>(mDataSource.getAllLines());
        Collections.sort(lines);
        setListAdapter(new LineAdapter(this, lines));
    }
}
