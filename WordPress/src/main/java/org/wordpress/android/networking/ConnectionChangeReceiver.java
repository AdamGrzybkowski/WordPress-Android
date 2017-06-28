package org.wordpress.android.networking;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;

import de.greenrobot.event.EventBus;

/*
 * global network connection change receiver - declared in the manifest to monitor
 * android.net.conn.CONNECTIVITY_CHANGE
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {
    private static boolean mIsFirstReceive = true;
    private static boolean mWasConnected = true;
    private static boolean mIsEnabled = false; // this value must be synchronized with the ConnectionChangeReceiver
                                               // state in our AndroidManifest
    private static ConnectionChangeReceiver sInstance;

    public static class ConnectionChangeEvent {
        private final boolean mIsConnected;
        public ConnectionChangeEvent(boolean isConnected) {
            mIsConnected = isConnected;
        }
        public boolean isConnected() {
            return mIsConnected;
        }
    }

    /*
     * note that onReceive occurs when anything about the connection has changed, not just
     * when the connection has been lost or restated, so it can happen quite often when the
     * user is on the move. for this reason we only fire the event the first time onReceive
     * is called, and afterwards only when we know connection availability has changed
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = NetworkUtils.isNetworkAvailable(context);
        boolean initialStickyBroadcast = isInitialStickyBroadcast();
        if (mIsFirstReceive || isConnected != mWasConnected || !initialStickyBroadcast) {
            postConnectionChangeEvent(isConnected);
        }
    }

    private static void postConnectionChangeEvent(boolean isConnected) {
        AppLog.i(T.UTILS, "Connection status changed, isConnected=" + isConnected);
        mWasConnected = isConnected;
        mIsFirstReceive = false;
        EventBus.getDefault().post(new ConnectionChangeEvent(isConnected));
    }

    public synchronized static void setEnabled(Context context, boolean enabled) {
        if (mIsEnabled == enabled) {
            return;
        }
        forceSetEnabled(context, enabled);
    }

    public synchronized static void forceSetEnabled(Context context, boolean enabled) {
        mIsEnabled = enabled;
        AppLog.i(T.UTILS, "ConnectionChangeReceiver.setEnabled " + enabled);
        int flag = (enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        ComponentName component = new ComponentName(context, ConnectionChangeReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(component, flag, PackageManager.DONT_KILL_APP);
        int  status = pm.getComponentEnabledSetting(component);
        if (mIsEnabled) {
            postConnectionChangeEvent(NetworkUtils.isNetworkAvailable(context));
        }
    }

    public static ConnectionChangeReceiver getInstance(){
        if (sInstance == null) {
            sInstance = new ConnectionChangeReceiver();
        }
        return sInstance;
    }
}
