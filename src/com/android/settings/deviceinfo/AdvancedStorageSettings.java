/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.deviceinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.io.File;

/**
 * Advanced storage settings.
 */
public class AdvancedStorageSettings extends SettingsPreferenceFragment {

    private static final String TAG = "AdvancedStorageSettings";

    private static final String KEY_PRIMARY_STORAGE_WARNING = "as_primary_storage_warning";
    private static final String KEY_SYS_SWAP = "as_sys_swap";
    private static final String KEY_ENV_SWAP = "as_env_swap";
    private static final String ENVVOLD_SWITCH_PERSIST_PROP = "persist.sys.env.switchexternal";
    private static final String SYSVOLD_SWITCH_PERSIST_PROP = "persist.sys.vold.switchexternal";
    private static final String VOLD_SWITCHABLEPAIR_PROP = "persist.sys.vold.switchablepair";
    private static boolean mSwitchablePairFound = false;
    private static boolean mPrimaryEmulated = Environment.isExternalStorageEmulated();
    private Preference mPrimaryStorageWarning;
    private CheckBoxPreference mSysSwap;
    private CheckBoxPreference mEnvSwap;

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.advanced_storage_settings);
        root = getPreferenceScreen();

        mPrimaryStorageWarning = (Preference)root.findPreference(KEY_PRIMARY_STORAGE_WARNING);
        mSysSwap = (CheckBoxPreference)root.findPreference(KEY_SYS_SWAP);
        mEnvSwap = (CheckBoxPreference)root.findPreference(KEY_ENV_SWAP);
        updateToggles();

        return root;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mSwitchablePairFound = setSwitchablePair();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
    }

    private boolean setSwitchablePair() {
        String primaryPath = System.getenv("EXTERNAL_STORAGE");
        String[] secondaryPaths = System.getenv("SECONDARY_STORAGE").split(":");
        boolean[] sdxFound = { false, false };

        if (TextUtils.equals(primaryPath, "/storage/sdcard0")) {
            sdxFound[0] = true;
        } else {
            Log.e(TAG,"/storage/sdcard0 was not found");
            Log.e(TAG,"System vold swap unavailable");
        }

        for (String secondaryPath : secondaryPaths) {
            if (TextUtils.equals(secondaryPath, "/storage/sdcard1")) {
                sdxFound[1] = true;
            }
        }

        if (!sdxFound[1]) {
            Log.e(TAG,"/storage/sdcard1 was not found");
            Log.e(TAG,"System vold swap unavailable");
            Log.e(TAG,"Environment primary swap unavailable");

            return false;
        }

        if (sdxFound[0] && sdxFound[1]) {
            SystemProperties.set(VOLD_SWITCHABLEPAIR_PROP, "sdcard0,sdcard1");
            Log.i(TAG, "System property set: " + VOLD_SWITCHABLEPAIR_PROP
                    + "=sdcard0,sdcard1");
        }

        return true;
    }

    private void updateToggles() {
        mPrimaryStorageWarning.setEnabled(false);
        if (!SystemProperties.get(SYSVOLD_SWITCH_PERSIST_PROP).equals("1") ||
                !SystemProperties.get(ENVVOLD_SWITCH_PERSIST_PROP).equals("1")) {
            removePreference(KEY_PRIMARY_STORAGE_WARNING);
        }

        if (mSwitchablePairFound && !mPrimaryEmulated) {
            if (SystemProperties.get(SYSVOLD_SWITCH_PERSIST_PROP).equals("1") &&
                    SystemProperties.get(ENVVOLD_SWITCH_PERSIST_PROP).equals("1")) {
                // This case should not be encountered if system properties are
                // set via this Settings menu
                mEnvSwap.setSummary(R.string.as_env_swap_summary);
                mEnvSwap.setChecked(true);
                mEnvSwap.setEnabled(true);
                mSysSwap.setSummary(R.string.as_sys_swap_summary);
                mSysSwap.setChecked(true);
                mSysSwap.setEnabled(true);
            } else if (SystemProperties.get(SYSVOLD_SWITCH_PERSIST_PROP).equals("1")) {
                mEnvSwap.setSummary(R.string.as_env_swap_sysenabled);
                mEnvSwap.setChecked(false);
                mEnvSwap.setEnabled(false);
                mSysSwap.setSummary(R.string.as_sys_swap_summary);
                mSysSwap.setChecked(true);
                mSysSwap.setEnabled(true);
            } else if (SystemProperties.get(ENVVOLD_SWITCH_PERSIST_PROP).equals("1")) {
                mEnvSwap.setSummary(R.string.as_env_swap_summary);
                mEnvSwap.setChecked(true);
                mEnvSwap.setEnabled(true);
                mSysSwap.setSummary(R.string.as_sys_swap_envenabled);
                mSysSwap.setChecked(false);
                mSysSwap.setEnabled(false);
            } else {
                mEnvSwap.setSummary(R.string.as_env_swap_summary);
                mEnvSwap.setChecked(false);
                mEnvSwap.setEnabled(true);
                mSysSwap.setSummary(R.string.as_sys_swap_summary);
                mSysSwap.setChecked(false);
                mSysSwap.setEnabled(true);
            }
        } else if (mSwitchablePairFound && mPrimaryEmulated) {
            mSysSwap.setSummary(R.string.as_sys_swap_noemulated);
            mSysSwap.setChecked(false);
            mSysSwap.setEnabled(false);
            if (SystemProperties.get(ENVVOLD_SWITCH_PERSIST_PROP).equals("1")) {
                mEnvSwap.setChecked(true);
                mEnvSwap.setEnabled(true);
            } else {
                mEnvSwap.setChecked(false);
                mEnvSwap.setEnabled(true);
            }
        } else {
            mEnvSwap.setChecked(false);
            mEnvSwap.setSummary(R.string.as_swap_unavailable);
            mEnvSwap.setEnabled(false);
            mSysSwap.setChecked(false);
            mSysSwap.setSummary(R.string.as_swap_unavailable);
            mSysSwap.setEnabled(false);
        }
    }

    private void showRebootPrompt() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.as_swap_reboot_prompt_title)
                .setMessage(R.string.as_swap_reboot_prompt_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        pm.reboot(null);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .create();

        dialog.show();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        // Don't allow any changes to take effect as the USB host will be disconnected, killing
        // the monkeys
        if (Utils.isMonkeyRunning()) {
            return true;
        }

        if (preference == mSysSwap) {
            SystemProperties.set(SYSVOLD_SWITCH_PERSIST_PROP,
                mSysSwap.isChecked() ? "1" : "0");
            Log.i(TAG, "System property set: " + SYSVOLD_SWITCH_PERSIST_PROP
                  + "=" + SystemProperties.get(SYSVOLD_SWITCH_PERSIST_PROP));
        } else if (preference == mEnvSwap) {
            SystemProperties.set(ENVVOLD_SWITCH_PERSIST_PROP,
                mEnvSwap.isChecked() ? "1" : "0");
            Log.i(TAG, "System property set: " + ENVVOLD_SWITCH_PERSIST_PROP
                  + "=" + SystemProperties.get(ENVVOLD_SWITCH_PERSIST_PROP));
        }
        updateToggles();
        showRebootPrompt();
        return true;
    }
}
