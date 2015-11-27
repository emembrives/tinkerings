package fr.membrives.dispotrains;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.util.Log;

/**
 * Activity that listens to changes in a ContentResolver.
 */
abstract public class ListeningActivity extends ListActivity implements SyncStatusObserver {
    public static final String AUTHORITY = "fr.membrives.dispotrains";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "fr.membrives.dispotrains";
    // The account name
    public static final String ACCOUNT = "Dispotrains";
    private static final String TAG = "f.m.d.ListeningActivity";
    // Dispotrains account for synchronization.
    protected Account mAccount;
    private Object mContentProviderHandle;

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
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

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        // Create the dummy account
        mAccount = CreateSyncAccount(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ContentResolver.removeStatusChangeListener(mContentProviderHandle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mContentProviderHandle = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this);
        onStatusChanged(0);
    }

    public void onStatusChanged(int which) {
        updateIsSyncing(ContentResolver.isSyncActive(mAccount, AUTHORITY));
    }

    /**
     * Runs on background thread.
     *
     * @param isSyncing whether the syncing is in progress
     */
    abstract protected void updateIsSyncing(final boolean isSyncing);

}
