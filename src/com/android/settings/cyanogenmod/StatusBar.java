/*
 * Copyright (C) 2012 The CyanogenMod Project
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
import android.preference.CheckBoxPreference;
import android.preference.ColorPickerPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.ExtendedPropertiesUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class StatusBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String STATUS_BAR_CATEGORY_GENERAL = "status_bar_general";
    private static final String STATUS_BAR_CATEGORY_CLOCK = "status_bar_clock";
    private static final String STATUS_BAR_SHOW_AM_PM = "status_bar_show_am_pm";
    private static final String STATUS_BAR_AM_PM_SIZE = "status_bar_am_pm_size";
    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String STATUS_BAR_MAX_NOTIF = "status_bar_max_notifications";
    private static final String STATUS_BAR_CLOCK = "status_bar_show_clock";
    private static final String STATUS_BAR_CENTER_CLOCK = "status_bar_center_clock";
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    private static final String STATUS_BAR_SIGNAL = "status_bar_signal";
    private static final String COMBINED_BAR_AUTO_HIDE = "combined_bar_auto_hide";
    private static final String STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";
    private static final String STATUS_BAR_DONOTDISTURB = "status_bar_donotdisturb";
    private static final String STATUS_BAR_SHOW_WEEKDAY = "status_bar_show_weekday";
    private static final String STATUS_BAR_WEEKDAY_SIZE = "status_bar_weekday_size";
    private static final String STATUS_BAR_SHOW_DAYMONTH = "status_bar_show_daymonth";
    private static final String STATUS_BAR_DAYMONTH_SIZE = "status_bar_daymonth_size";
    private static final String STATUS_BAR_COLOR = "status_bar_color";
    private static final String PREF_LOW_BATTERY_WARNING = "pref_low_battery_warning";
    private static final String PREF_LOW_BATTERY_SOUND = "pref_low_battery_sound";

    private ListPreference mStatusBarAmPmSize;
    private ListPreference mStatusBarBattery;
    private ListPreference mStatusBarMaxNotif;
    private ListPreference mStatusBarCmSignal;
    private ListPreference mStatusBarWeekdaySize;
    private ListPreference mStatusBarDaymonthSize;
    private ColorPickerPreference mStatusBarColor;
    private CheckBoxPreference mStatusBarClock;
    private CheckBoxPreference mStatusBarCenterClock;
    private CheckBoxPreference mStatusBarShowAmPm;
    private CheckBoxPreference mStatusBarShowWeekday;
    private CheckBoxPreference mStatusBarShowDaymonth;
    private CheckBoxPreference mStatusBarBrightnessControl;
    private CheckBoxPreference mCombinedBarAutoHide;
    private CheckBoxPreference mStatusBarNotifCount;
    private CheckBoxPreference mStatusBarDoNotDisturb;
    private PreferenceCategory mPrefCategoryGeneral;
    private PreferenceCategory mPrefCategoryClock;
    private CheckBoxPreference mLowBatteryWarning;
    private CheckBoxPreference mLowBatterySound;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar);

        PreferenceScreen prefSet = getPreferenceScreen();

        mStatusBarClock = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_CLOCK);
        mStatusBarCenterClock = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_CENTER_CLOCK);
        mStatusBarBrightnessControl = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarShowAmPm = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_SHOW_AM_PM);
        mStatusBarAmPmSize = (ListPreference) prefSet.findPreference(STATUS_BAR_AM_PM_SIZE);
        mStatusBarAmPmSize.setEnabled(mStatusBarShowAmPm.isChecked() && mStatusBarClock.isChecked());
        mStatusBarBattery = (ListPreference) prefSet.findPreference(STATUS_BAR_BATTERY);
        mStatusBarShowWeekday = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_SHOW_WEEKDAY);
        mStatusBarWeekdaySize = (ListPreference) prefSet.findPreference(STATUS_BAR_WEEKDAY_SIZE);
        mStatusBarWeekdaySize.setEnabled(mStatusBarShowWeekday.isChecked() && mStatusBarClock.isChecked());
        mStatusBarShowDaymonth = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_SHOW_DAYMONTH);
        mStatusBarDaymonthSize = (ListPreference) prefSet.findPreference(STATUS_BAR_DAYMONTH_SIZE);
        mStatusBarDaymonthSize.setEnabled(mStatusBarShowDaymonth.isChecked() && mStatusBarClock.isChecked());
        mStatusBarMaxNotif = (ListPreference) prefSet.findPreference(STATUS_BAR_MAX_NOTIF);
        mCombinedBarAutoHide = (CheckBoxPreference) prefSet.findPreference(COMBINED_BAR_AUTO_HIDE);
        mStatusBarDoNotDisturb = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_DONOTDISTURB);
        mStatusBarCmSignal = (ListPreference) prefSet.findPreference(STATUS_BAR_SIGNAL);
        mStatusBarColor = (ColorPickerPreference) prefSet.findPreference(STATUS_BAR_COLOR);
        mStatusBarColor.setOnPreferenceChangeListener(this);
        mStatusBarColor.setAlphaSliderEnabled(true);
        mLowBatteryWarning = (CheckBoxPreference) prefSet.findPreference(PREF_LOW_BATTERY_WARNING);
        mLowBatterySound = (CheckBoxPreference) prefSet.findPreference(PREF_LOW_BATTERY_SOUND);

        mStatusBarClock.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_CLOCK, 1) == 1));
        mStatusBarCenterClock.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_CENTER_CLOCK, 0) == 1));
        mStatusBarCenterClock.setEnabled(mStatusBarClock.isChecked());
        mStatusBarShowAmPm.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_AM_PM, 0) == 1));
        mStatusBarShowAmPm.setEnabled(mStatusBarClock.isChecked());
        mStatusBarShowWeekday.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_WEEKDAY, 0) == 1));
        mStatusBarShowWeekday.setEnabled(mStatusBarClock.isChecked());
        mStatusBarShowDaymonth.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_DAYMONTH, 0) == 1));
        mStatusBarShowDaymonth.setEnabled(mStatusBarClock.isChecked());
        mStatusBarBrightnessControl.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0) == 1));
        mStatusBarDoNotDisturb.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_DONOTDISTURB, 0) == 1));
        mLowBatteryWarning.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.POWER_UI_LOW_BATTERY_WARNING, 1) == 1));
        mLowBatterySound.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.POWER_UI_LOW_BATTERY_SOUND, 1) == 1));
        mLowBatterySound.setEnabled(mLowBatteryWarning.isChecked());

        try {
            if (Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                mStatusBarBrightnessControl.setEnabled(false);
                mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
            }
        } catch (SettingNotFoundException e) {
        }

        try {
            if (Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.TIME_12_24) == 24) {
                mStatusBarShowAmPm.setEnabled(false);
                mStatusBarAmPmSize.setEnabled(false);
                mStatusBarShowAmPm.setSummary(R.string.status_bar_am_pm_info);
            }
        } catch (SettingNotFoundException e) {
        }

        int statusBarAmPmSize = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_AM_PM_SIZE, 1);
        mStatusBarAmPmSize.setValue(String.valueOf(statusBarAmPmSize));
        mStatusBarAmPmSize.setSummary(mStatusBarAmPmSize.getEntry());
        mStatusBarAmPmSize.setOnPreferenceChangeListener(this);

        int statusBarWeekdaySize = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_WEEKDAY_SIZE, 1);
        mStatusBarWeekdaySize.setValue(String.valueOf(statusBarWeekdaySize));
        mStatusBarWeekdaySize.setSummary(mStatusBarWeekdaySize.getEntry());
        mStatusBarWeekdaySize.setOnPreferenceChangeListener(this);

        int statusBarDaymonthSize = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_DAYMONTH_SIZE, 1);
        mStatusBarDaymonthSize.setValue(String.valueOf(statusBarDaymonthSize));
        mStatusBarDaymonthSize.setSummary(mStatusBarDaymonthSize.getEntry());
        mStatusBarDaymonthSize.setOnPreferenceChangeListener(this);

        int statusBarBattery = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY, 0);
        mStatusBarBattery.setValue(String.valueOf(statusBarBattery));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        int maxNotIcons = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.MAX_NOTIFICATION_ICONS, 2);
        mStatusBarMaxNotif.setValue(String.valueOf(maxNotIcons));
        mStatusBarMaxNotif.setOnPreferenceChangeListener(this);

        int signalStyle = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_SIGNAL_TEXT, 0);
        mStatusBarCmSignal.setValue(String.valueOf(signalStyle));
        mStatusBarCmSignal.setSummary(mStatusBarCmSignal.getEntry());
        mStatusBarCmSignal.setOnPreferenceChangeListener(this);

        mCombinedBarAutoHide.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.COMBINED_BAR_AUTO_HIDE, 0) == 1));

        mStatusBarNotifCount = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NOTIF_COUNT);
        mStatusBarNotifCount.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_NOTIF_COUNT, 0) == 1));

        mPrefCategoryGeneral = (PreferenceCategory) findPreference(STATUS_BAR_CATEGORY_GENERAL);
        mPrefCategoryClock = (PreferenceCategory) findPreference(STATUS_BAR_CATEGORY_CLOCK);

        updatePreferences(mStatusBarClock.isChecked());

        if (Utils.isTablet()) {
            mPrefCategoryClock.removePreference(mStatusBarCenterClock);
            mPrefCategoryGeneral.removePreference(mStatusBarColor);
            mPrefCategoryGeneral.removePreference(mStatusBarBrightnessControl);
            mPrefCategoryGeneral.removePreference(mStatusBarCmSignal);
        } else {
            mPrefCategoryGeneral.removePreference(mStatusBarMaxNotif);
            mPrefCategoryGeneral.removePreference(mCombinedBarAutoHide);
        }
    }

    public void updatePreferences(boolean value) {
        mStatusBarCenterClock.setEnabled(value);
        mStatusBarShowAmPm.setEnabled(value);
        mStatusBarShowWeekday.setEnabled(value);
        mStatusBarShowDaymonth.setEnabled(value);
        mStatusBarWeekdaySize.setEnabled(value && mStatusBarShowWeekday.isChecked());
        mStatusBarAmPmSize.setEnabled(value && mStatusBarShowAmPm.isChecked());
        mStatusBarDaymonthSize.setEnabled(value && mStatusBarShowDaymonth.isChecked());
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mStatusBarAmPmSize) {
            int statusBarAmPmSize = Integer.valueOf((String) newValue);
            int index = mStatusBarAmPmSize.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_AM_PM_SIZE, statusBarAmPmSize);
            mStatusBarAmPmSize.setSummary(mStatusBarAmPmSize.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarWeekdaySize) {
            int statusBarWeekdaySize = Integer.valueOf((String) newValue);
            int index = mStatusBarWeekdaySize.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_WEEKDAY_SIZE, statusBarWeekdaySize);
            mStatusBarWeekdaySize.setSummary(mStatusBarWeekdaySize.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarDaymonthSize) {
            int statusBarDaymonthSize = Integer.valueOf((String) newValue);
            int index = mStatusBarDaymonthSize.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_DAYMONTH_SIZE, statusBarDaymonthSize);
            mStatusBarDaymonthSize.setSummary(mStatusBarDaymonthSize.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarBattery) {
            int statusBarBattery = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, statusBarBattery);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarMaxNotif) {
            int maxNotIcons = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.MAX_NOTIFICATION_ICONS, maxNotIcons);
            return true;
        } else if (preference == mStatusBarColor) {
            String setting = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_COLOR);
            String[] mColors = (setting == null || setting.equals("") ?
                    ExtendedPropertiesUtils.PARANOID_COLORS_DEFAULTS[ExtendedPropertiesUtils.PARANOID_COLORS_STATBAR] :
                    setting).split(ExtendedPropertiesUtils.PARANOID_STRING_DELIMITER);
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_COLOR, ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue))).substring(1) + "|" + mColors[1] + "|1");
            return true;
         } else if (preference == mStatusBarCmSignal) {
            int signalStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarCmSignal.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_SIGNAL_TEXT, signalStyle);
            mStatusBarCmSignal.setSummary(mStatusBarCmSignal.getEntries()[index]);
            return true;
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mStatusBarClock) {
            value = mStatusBarClock.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CLOCK, value ? 1 : 0);
            updatePreferences(value);
            return true;
        } else if (preference == mStatusBarCenterClock) {
            value = mStatusBarCenterClock.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CENTER_CLOCK, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarShowAmPm) {
            value = mStatusBarShowAmPm.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_AM_PM, value ? 1 : 0);
            mStatusBarAmPmSize.setEnabled(value);
            return true;
        } else if (preference == mStatusBarShowWeekday) {
            value = mStatusBarShowWeekday.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_WEEKDAY, value ? 1 : 0);
            mStatusBarWeekdaySize.setEnabled(value);
            return true;
        } else if (preference == mStatusBarShowDaymonth) {
            value = mStatusBarShowDaymonth.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_DAYMONTH, value ? 1 : 0);
            mStatusBarDaymonthSize.setEnabled(value);
            return true;
        } else if (preference == mStatusBarDoNotDisturb) {
            value = mStatusBarDoNotDisturb.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_DONOTDISTURB, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarBrightnessControl) {
            value = mStatusBarBrightnessControl.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, value ? 1 : 0);
            return true;
        } else if (preference == mCombinedBarAutoHide) {
            value = mCombinedBarAutoHide.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.COMBINED_BAR_AUTO_HIDE, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarNotifCount) {
            value = mStatusBarNotifCount.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_NOTIF_COUNT, value ? 1 : 0);
            return true;
        } else if (preference == mLowBatteryWarning) {
            value = mLowBatteryWarning.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_UI_LOW_BATTERY_WARNING, value ? 1 : 0);
            mLowBatterySound.setEnabled(value);
            return true;
        } else if (preference == mLowBatterySound) {
            value = mLowBatterySound.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_UI_LOW_BATTERY_SOUND, value ? 1 : 0);
            return true;
        }
        return false;
    }
}
