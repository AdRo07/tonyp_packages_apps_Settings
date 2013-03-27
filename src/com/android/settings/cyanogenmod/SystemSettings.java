/*
 * Copyright (C) 2012 CyanogenMod
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class SystemSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_NOTIFICATION_DRAWER_TOGGLES = "notification_drawer_toggles";
    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";

    private ListPreference mFontSizePref;
    private PreferenceScreen mStatusbarToggles;
    private PreferenceScreen mNavigationBar;
    private PreferenceScreen mHardwareKeys;
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_BATTERY_LIGHT = "battery_light";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String KEY_NAVIGATION_RING = "navigation_ring";
    private static final String KEY_NAVIGATION_BAR_CATEGORY = "navigation_bar_category";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_STATUS_BAR = "status_bar";
    private static final String KEY_QUICK_SETTINGS = "quick_settings_panel";
    private static final String KEY_NOTIFICATION_DRAWER = "notification_drawer";
    private static final String KEY_POWER_MENU = "power_menu";
    private static final String KEY_PIE_CONTROL = "pie_control";

    private final Configuration mCurConfig = new Configuration();
    private PreferenceScreen mNotificationPulse;
    private PreferenceScreen mBatteryPulse;
    private PreferenceScreen mPieControl;
    private boolean mIsPrimary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_settings);

        mFontSizePref = (ListPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);

        mStatusbarToggles = (PreferenceScreen) findPreference(KEY_NOTIFICATION_DRAWER_TOGGLES);
        mNavigationBar = (PreferenceScreen) findPreference(KEY_NAVIGATION_BAR);
        mHardwareKeys = (PreferenceScreen) findPreference(KEY_HARDWARE_KEYS);
        
        IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            if(!wm.hasHardwareKeys())
                getPreferenceScreen().removePreference(mHardwareKeys);
        } catch (RemoteException ex) {
            // no WindowManager ?, and we're still alive?
        }
    }

    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }

    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // mark the appropriate item in the preferences list
        int index = floatToIndex(mCurConfig.fontScale);
        pref.setValueIndex(index);
        // Pie controls
        mPieControl = (PreferenceScreen) findPreference(KEY_PIE_CONTROL);
    }

	    @Override
    public void onResume() {
        super.onResume();
        // All users
        if (mNotificationPulse != null) {
            updateLightPulseDescription();
        }
        if (mPieControl != null) {
            updatePieControlDescription();
        }
        // Primary user only
        if (mIsPrimary && mBatteryPulse != null) {
            updateBatteryPulseDescription();
        }
    }
    @Override
    public void onPause() {
        super.onPause();
    }

    private void updatePieControlDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_CONTROLS, 0) == 1) {
            mPieControl.setSummary(getString(R.string.pie_control_enabled));
        } else {
            mPieControl.setSummary(getString(R.string.pie_control_disabled));
        }
    }

    private void updateState() {
        readFontSizePreference(mFontSizePref);
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }

        return true;
    }
}
