package fr.membrives.dispotrains;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import fr.membrives.dispotrains.adapters.LineAdapter;
import fr.membrives.dispotrains.data.Line;

public class MainActivity extends ListActivity {
    private static final String TAG = "f.m.d.MainActivity";
    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "fr.membrives.dispotrains";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "fr.membrives.dispotrains";
    // The account name
    public static final String ACCOUNT = "Dispotrains";
    // Instance fields
    private Account mAccount;

    private DataSource mDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Create the dummy account
        mAccount = CreateSyncAccount(this);
        // Turn on automatic syncing for the default account and authority
        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
        ContentResolver.addPeriodicSync(mAccount, AUTHORITY, new Bundle(), 1800);
        ContentResolver.requestSync(mAccount, AUTHORITY, new Bundle());

        mDataSource = new DataSource(this);
        mDataSource.open();

        List<Line> lines = new ArrayList<Line>(mDataSource.getAllLines());
        Collections.sort(lines);
        setListAdapter(new LineAdapter(this, lines));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Line item = (Line) getListAdapter().getItem(position);
        Intent appInfo = new Intent(MainActivity.this, StationListActivity.class);
        appInfo.putExtra("line", item);
        startActivity(appInfo);
    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context
     *            The application context
     */
    public static Account CreateSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data If successful, return the
         * Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            Log.d(TAG, "Account added.");
        } else {
            Log.d(TAG, "Account already present.");
        }
        return newAccount;
    }
}
