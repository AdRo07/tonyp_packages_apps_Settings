/*
 * Copyright (C) 2012 Slimroms Project
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

import android.app.AlertDialog;
import android.app.INotificationManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.Spannable;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import android.widget.EditText;

public class UserInterfaceSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    public static final String TAG = "UserInterface";

    private static final String MISC_SETTINGS = "misc";
    private static final String KEY_RECENTS_RAM_BAR = "recents_ram_bar";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    private static final String KEY_HALO_STATE = "halo_state";
    private static final String KEY_HALO_HIDE = "halo_hide";
    private static final String KEY_HALO_REVERSED = "halo_reversed";
    private static final String KEY_HALO_SIZE = "halo_size";
    private static final String KEY_HALO_PAUSE = "halo_pause";

    private PreferenceCategory mMisc;
    private Preference mRamBar;
    private Preference mCustomLabel;
    private ListPreference mLowBatteryWarning;
    private ListPreference mHaloState;
    private ListPreference mHaloSize;
    private CheckBoxPreference mHaloHide;
    private CheckBoxPreference mHaloReversed;
    private CheckBoxPreference mHaloPause;

    private String mCustomLabelText = null;
    private Context mContext;
    private INotificationManager mNotificationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.user_interface_settings);

        PreferenceScreen prefs = getPreferenceScreen();
        mContext = getActivity();

        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));

        mHaloState = (ListPreference) prefs.findPreference(KEY_HALO_STATE);
        mHaloState.setValue(String.valueOf((isHaloPolicyBlack() ? "1" : "0")));
        mHaloState.setOnPreferenceChangeListener(this);

        mHaloHide = (CheckBoxPreference) prefs.findPreference(KEY_HALO_HIDE);
        mHaloHide.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_HIDE, 0) == 1);

        mHaloReversed = (CheckBoxPreference) prefs.findPreference(KEY_HALO_REVERSED);
        mHaloReversed.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_REVERSED, 1) == 1);

        mHaloSize = (ListPreference) prefs.findPreference(KEY_HALO_SIZE);
        try {
            float haloSize = Settings.System.getFloat(mContext.getContentResolver(),
                    Settings.System.HALO_SIZE, 1.0f);
            mHaloSize.setValue(String.valueOf(haloSize));  
        } catch(Exception ex) {
            // So what
        }
        mHaloSize.setOnPreferenceChangeListener(this);                

        int isLowRAM = (ActivityManager.isLargeRAM()) ? 0 : 1;
        mHaloPause = (CheckBoxPreference) prefs.findPreference(KEY_HALO_PAUSE);
        mHaloPause.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_PAUSE, isLowRAM) == 1);

        mMisc = (PreferenceCategory) prefs.findPreference(MISC_SETTINGS);

        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        mCustomLabel.setOnPreferenceClickListener(mCustomLabelClicked);

        updateCustomLabelTextSummary();

        mRamBar = findPreference(KEY_RECENTS_RAM_BAR);
        mRamBar.setOnPreferenceChangeListener(this);
        updateRamBar();

        mLowBatteryWarning = (ListPreference) findPreference(KEY_LOW_BATTERY_WARNING_POLICY);
        mLowBatteryWarning.setOnPreferenceChangeListener(this);
        int lowBatteryWarning = Settings.System.getInt(getActivity().getContentResolver(),
                                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY, 0);
        mLowBatteryWarning.setValue(String.valueOf(lowBatteryWarning));
        mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());
    }

    private boolean isHaloPolicyBlack() {
        try {
            return mNotificationManager.isHaloPolicyBlack();
        } catch (android.os.RemoteException ex) {
                // System dead
        }
        return true;
    }

    private void updateRamBar() {
        int ramBarMode = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.RECENTS_RAM_BAR_MODE, 0);
        if (ramBarMode != 0)
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_enabled));
        else
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_disabled));
    }

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRamBar();
    }

    @Override
    public void onPause() {
        super.onResume();
        updateRamBar();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLowBatteryWarning) {
            int lowBatteryWarning = Integer.valueOf((String) newValue);
            int index = mLowBatteryWarning.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY,
                    lowBatteryWarning);
            mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntries()[index]);
            return true;
        } else if (preference == mHaloState) {
            boolean state = Integer.valueOf((String) newValue) == 1;
            try {
                mNotificationManager.setHaloPolicyBlack(state);
            } catch (android.os.RemoteException ex) {
                // System dead
            }          
            return true;
        } else if (preference == mHaloSize) {
            float haloSize = Float.valueOf((String) newValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.HALO_SIZE, haloSize);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mHaloHide) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_HIDE, mHaloHide.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloReversed) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_REVERSED, mHaloReversed.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloPause) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_PAUSE, mHaloPause.isChecked()
                    ? 1 : 0);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public OnPreferenceClickListener mCustomLabelClicked = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);
            alert.setPositiveButton(getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                    Intent i = new Intent();
                    i.setAction("com.android.settings.LABEL_CHANGED");
                    getActivity().sendBroadcast(i);
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
            return true;
        }
    };

}
