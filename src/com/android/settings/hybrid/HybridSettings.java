package com.android.settings.hybrid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.ExtendedPropertiesUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class HybridSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private PreferenceScreen mDpiScreen;
    private PreferenceScreen mAppsDpi;
    private CheckBoxPreference mAutoBackup;
    private Preference mBackup;
    private Preference mRestore;

    private Context mContext;

    private int mNavbarHeightProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        addPreferencesFromResource(R.xml.hybrid_settings);

        mDpiScreen = (PreferenceScreen) findPreference("system_dpi");

        mAppsDpi = (PreferenceScreen) findPreference("apps_dpi");

        mAutoBackup = (CheckBoxPreference) findPreference("dpi_groups_auto_backup");
        mBackup = findPreference("dpi_groups_backup");
        mRestore = findPreference("dpi_groups_restore");

        boolean isAutoBackup = mContext.getSharedPreferences(Applications.PREFS_NAME, 0)
                .getBoolean(Applications.PROPERTY_AUTO_BACKUP, false);

        mAutoBackup.setChecked(isAutoBackup);

        mRestore.setEnabled(Applications.backupExists());

        updateSummaries();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mBackup) {
            Applications.backup(mContext);
        } else if (preference == mRestore) {
            Applications.restore(mContext);
            Utils.reboot(mContext);
        } else if (preference == mAutoBackup) {
            SharedPreferences settings = mContext.getSharedPreferences(
                    Applications.PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(Applications.PROPERTY_AUTO_BACKUP,
                    ((CheckBoxPreference) preference).isChecked());
            editor.commit();
        }
        updateSummaries();
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if ("ui_mode".equals(key)) {
            String layout = (String) objValue;
            Applications.addSystemLayout(mContext, layout);
        } else if ("apps_ui_mode".equals(key)) {
            String layout = (String) objValue;
            Applications.addAppsLayout(mContext, layout);
        }

        updateSummaries();
        return true;
    }

    private void updateSummaries() {
        int dpi = ExtendedPropertiesUtils
                .getActualProperty("com.android.systemui.dpi");
        mDpiScreen.setSummary(getResources().getString(
                R.string.system_dpi_summary)
                + " " + dpi);

        dpi = ExtendedPropertiesUtils
                .getActualProperty(ExtendedPropertiesUtils.BEERBONG_PREFIX + "user_default_dpi");
        if (dpi == 0) {
            dpi = ExtendedPropertiesUtils.getActualProperty("com.android.systemui.dpi");
        }
        mAppsDpi.setSummary(getResources().getString(
                R.string.apps_dpi_summary)
                + " " + dpi);

        mRestore.setEnabled(Applications.backupExists());
    }
}