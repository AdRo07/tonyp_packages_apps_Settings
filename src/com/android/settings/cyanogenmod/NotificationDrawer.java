/*
 * Copyright (C) 2012 The CyanogenMod project
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

import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NotificationDrawer extends SettingsPreferenceFragment  implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "NotificationDrawer";

    private static final String UI_COLLAPSE_BEHAVIOUR = "notification_drawer_collapse_on_dismiss";
    private static final String VOLUME_SLIDER_MODE = "volume_slider_input_mode";

    private ListPreference mCollapseOnDismiss;
    
    private ListPreference mVolumeSliderMode;
    
    private int mVSliderMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.notification_drawer);
        PreferenceScreen prefScreen = getPreferenceScreen();

        // Notification drawer
        int collapseBehaviour = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_COLLAPSE_ON_DISMISS,
                Settings.System.STATUS_BAR_COLLAPSE_IF_NO_CLEARABLE);
        mCollapseOnDismiss = (ListPreference) findPreference(UI_COLLAPSE_BEHAVIOUR);
        mCollapseOnDismiss.setValue(String.valueOf(collapseBehaviour));
        mCollapseOnDismiss.setOnPreferenceChangeListener(this);
        updateCollapseBehaviourSummary(collapseBehaviour);
        
        mVSliderMode = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.SHOW_VOLUME_SLIDER, -1, UserHandle.USER_CURRENT);
        
        mVolumeSliderMode = (ListPreference)findPreference(VOLUME_SLIDER_MODE);
        mVolumeSliderMode.setValueIndex(mVSliderMode);
        mVolumeSliderMode.setOnPreferenceChangeListener(this);
        updateStreamSummary(mVSliderMode);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mCollapseOnDismiss) {
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_COLLAPSE_ON_DISMISS, value);
            updateCollapseBehaviourSummary(value);
            return true;
        } else if (preference == mVolumeSliderMode) {
            int value = Integer.valueOf((String) objValue);
            mVSliderMode = value;
            mVolumeSliderMode.setSummary(mVolumeSliderMode.getValue());
            Settings.System.putInt(getContentResolver(),
                    Settings.System.VOLUME_SLIDER_INPUT_MODE, value);
            updateStreamSummary(mVSliderMode);
            return true;
        }

        return false;
    }

    private void updateCollapseBehaviourSummary(int setting) {
        String[] summaries = getResources().getStringArray(
                R.array.notification_drawer_collapse_on_dismiss_summaries);
        mCollapseOnDismiss.setSummary(summaries[setting]);
    }
    
    private void updateStreamSummary(int setting) {
        String[] summaries = getResources().getStringArray(
                R.array.stream_volume_slider_entries);
        mVolumeSliderMode.setSummary(summaries[setting]);
    }
}
