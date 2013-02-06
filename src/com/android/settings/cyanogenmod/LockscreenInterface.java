/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.view.IWindowManager;
import android.view.Window;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.notificationlight.ColorPickerView;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "LockscreenInterface";
    private static final int LOCKSCREEN_BACKGROUND = 1024;
    public static final String KEY_WEATHER_PREF = "lockscreen_weather";
    public static final String KEY_CALENDAR_PREF = "lockscreen_calendar";
    public static final String KEY_BACKGROUND_PREF = "lockscreen_background";
    public static final String KEY_SEE_TRHOUGH_PREF = "lockscreen_see_through";
    public static final String KEY_STYLE_PREF = "lockscreen_style";
    private static final int LOCK_STYLE_JB = 0;
    private static final int LOCK_STYLE_OP4 = 5;  
    private static final String KEY_ALWAYS_BATTERY_PREF = "lockscreen_battery_status";
    private static final String KEY_CLOCK_ALIGN = "lockscreen_clock_align";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";

    public static final String KEY_CIRCLES_LOCK_BG_COLOR = "circles_lock_bg_color";
    public static final String KEY_CIRCLES_LOCK_RING_COLOR = "circles_lock_ring_color";
    public static final String KEY_CIRCLES_LOCK_HALO_COLOR = "circles_lock_halo_color";
    public static final String KEY_CIRCLES_LOCK_WAVE_COLOR = "circles_lock_wave_color";


    public static final String KEY_LOCKSCREEN_TARGETS = "lockscreen_targets";
    public static final String KEY_OPTIMUS_COLOR = "optimus_color";

    private ListPreference mCustomBackground;
    private CheckBoxPreference mSeeThrough;
    private ListPreference mStylePref;
    private Preference mWeatherPref;
    private Preference mCalendarPref;

    private Preference mLockBgColor;
    private Preference mLockRingColor;
    private Preference mLockHaloColor;
    private Preference mLockWaveColor;

    private ListPreference mBatteryStatus;
    private ListPreference mClockAlign;
    private PreferenceScreen mLockscreenButtons;
    private Activity mActivity;
    ContentResolver mResolver;

    private File wallpaperImage;
    private File wallpaperTemporary;
    private boolean mIsScreenLarge;
    private boolean mUseJbLockscreen;
    private boolean mUseOp4Lockscreen;
    private int mLockscreenStyle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mResolver = mActivity.getContentResolver();

        addPreferencesFromResource(R.xml.lockscreen_interface_settings);
        mWeatherPref = (Preference) findPreference(KEY_WEATHER_PREF);
        mCalendarPref = (Preference) findPreference(KEY_CALENDAR_PREF);

        mCustomBackground = (ListPreference) findPreference(KEY_BACKGROUND_PREF);
        mCustomBackground.setOnPreferenceChangeListener(this);

        mSeeThrough = (CheckBoxPreference) findPreference(KEY_SEE_TRHOUGH_PREF);
        mSeeThrough.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_SEE_THROUGH, 0) == 1));

        mStylePref = (ListPreference) findPreference(KEY_STYLE_PREF);
        mStylePref.setOnPreferenceChangeListener(this);

        mLockBgColor = (Preference) findPreference(KEY_CIRCLES_LOCK_BG_COLOR);
        mLockRingColor = (Preference) findPreference(KEY_CIRCLES_LOCK_RING_COLOR);
        mLockHaloColor = (Preference) findPreference(KEY_CIRCLES_LOCK_HALO_COLOR);
        mLockWaveColor = (Preference) findPreference(KEY_CIRCLES_LOCK_WAVE_COLOR);

        wallpaperImage = new File(mActivity.getFilesDir()+"/lockwallpaper");
        wallpaperTemporary = new File(mActivity.getCacheDir()+"/lockwallpaper.tmp");

        mBatteryStatus = (ListPreference) findPreference(KEY_ALWAYS_BATTERY_PREF);
        mBatteryStatus.setOnPreferenceChangeListener(this);

        mIsScreenLarge = Utils.isTablet();

        mClockAlign = (ListPreference) findPreference(KEY_CLOCK_ALIGN);
        mClockAlign.setOnPreferenceChangeListener(this);

        mLockscreenButtons = (PreferenceScreen) findPreference(KEY_LOCKSCREEN_BUTTONS);
        IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            if(!wm.hasHardwareKeys()){
                getPreferenceScreen().removePreference(mLockscreenButtons);
            }
        } catch (RemoteException ex) {
            // too bad, so sad, oh mom, oh dad
        }

	
	check_lockscreentarget();
	check_optimus();


        updateCustomBackgroundSummary();
    }

	private void check_lockscreentarget() {
            mLockscreenStyle = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_STYLE, 0);
            mUseJbLockscreen = (mLockscreenStyle == LOCK_STYLE_JB);
            if (!mUseJbLockscreen) {
                Preference lockTargets = findPreference(KEY_LOCKSCREEN_TARGETS);
                if (lockTargets != null) {
                    getPreferenceScreen().removePreference(lockTargets);
		}
	    }
	}

	private void check_optimus() {
            mLockscreenStyle = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_STYLE, 0);
            mUseOp4Lockscreen = (mLockscreenStyle == LOCK_STYLE_OP4);
            if (!mUseOp4Lockscreen) {
                Preference optimusColor = findPreference(KEY_OPTIMUS_COLOR);
                if (optimusColor != null) {
                    getPreferenceScreen().removePreference(optimusColor);
		}
	    }
	}

    private void updateCustomBackgroundSummary() {
        int resId;
        String value = Settings.System.getString(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND);
        if (value == null) {
            resId = R.string.lockscreen_background_default_wallpaper;
            mCustomBackground.setValueIndex(2);
        } else if (value.isEmpty()) {
            resId = R.string.lockscreen_background_custom_image;
            mCustomBackground.setValueIndex(1);
            mSeeThrough.setEnabled(false);
        } else {
            resId = R.string.lockscreen_background_color_fill;
            mCustomBackground.setValueIndex(0);
        }
        mCustomBackground.setSummary(getResources().getString(resId));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateState() {
        int resId;

        // Set the weather description text
        if (mWeatherPref != null) {
            boolean weatherEnabled = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_WEATHER, 0) == 1;
            if (weatherEnabled) {
                mWeatherPref.setSummary(R.string.lockscreen_weather_enabled);
            } else {
                mWeatherPref.setSummary(R.string.lockscreen_weather_summary);
            }
        }

        // Set the calendar description text
        if (mCalendarPref != null) {
            boolean weatherEnabled = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_CALENDAR, 0) == 1;
            if (weatherEnabled) {
                mCalendarPref.setSummary(R.string.lockscreen_calendar_enabled);
            } else {
                mCalendarPref.setSummary(R.string.lockscreen_calendar_summary);
            }
        }

        // Set the battery status description text
        if (mBatteryStatus != null) {
            boolean batteryStatusAlwaysOn = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, 0) == 1;
            if (batteryStatusAlwaysOn) {
                mBatteryStatus.setValueIndex(1);
            } else {
                mBatteryStatus.setValueIndex(0);
            }
            mBatteryStatus.setSummary(mBatteryStatus.getEntry());
        }

        // Set the clock align value
        if (mClockAlign != null) {
            int clockAlign = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_CLOCK_ALIGN, 2);
            mClockAlign.setValue(String.valueOf(clockAlign));
            mClockAlign.setSummary(mClockAlign.getEntries()[clockAlign]);
        }

        // Set the style value
        if (mStylePref != null) {
            int stylePref = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_STYLE, 0);
            mStylePref.setValue(String.valueOf(stylePref));
            mStylePref.setSummary(mStylePref.getEntries()[stylePref]);
	    check_lockscreentarget();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOCKSCREEN_BACKGROUND) {
            if (resultCode == Activity.RESULT_OK) {
                if (wallpaperTemporary.exists()) {
                    wallpaperTemporary.renameTo(wallpaperImage);
                }

                wallpaperImage.setReadOnly();
                Toast.makeText(mActivity, getResources().getString(R.string.
                        lockscreen_background_result_successful), Toast.LENGTH_LONG).show();
                Settings.System.putString(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND,"");
                updateCustomBackgroundSummary();
            } else {
                if (wallpaperTemporary.exists()) {
                    wallpaperTemporary.delete();
                }

                Toast.makeText(mActivity, getResources().getString(R.string.
                        lockscreen_background_result_not_successful), Toast.LENGTH_LONG).show();
                mSeeThrough.setEnabled(true);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSeeThrough) {
            int value = mSeeThrough.isChecked() ? 1 : 0;
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, value);
            return true;
        } else if (preference == mLockBgColor) {
            ColorPickerDialog cp = new ColorPickerDialog(getActivity(),
                    mCirclesBgColorListener, Settings.System.getInt(getActivity()
                    .getApplicationContext()
                    .getContentResolver(), Settings.System.CIRCLES_LOCK_BG_COLOR, 0xD2000000));
            cp.setDefaultColor(0xD2000000);
            cp.show();
            return true;
        } else if (preference == mLockRingColor) {
            ColorPickerDialog cp = new ColorPickerDialog(getActivity(),
                    mCirclesRingColorListener, Settings.System.getInt(getActivity()
                    .getApplicationContext()
                    .getContentResolver(), Settings.System.CIRCLES_LOCK_RING_COLOR, 0xFFFFFFFF));
            cp.setDefaultColor(0xFFFFFFFF);
            cp.show();
            return true;
        } else if (preference == mLockHaloColor) {
            ColorPickerDialog cp = new ColorPickerDialog(getActivity(),
                    mCirclesHaloColorListener, Settings.System.getInt(getActivity()
                    .getApplicationContext()
                    .getContentResolver(), Settings.System.CIRCLES_LOCK_HALO_COLOR, 0xFFFFFFFF));
            cp.setDefaultColor(0xFFFFFFFF);
            cp.show();
            return true;
        } else if (preference == mLockWaveColor) {
            ColorPickerDialog cp = new ColorPickerDialog(getActivity(),
                    mCirclesWaveColorListener, Settings.System.getInt(getActivity()
                    .getApplicationContext()
                    .getContentResolver(), Settings.System.CIRCLES_LOCK_WAVE_COLOR, 0xD2FFFFFF));
            cp.setDefaultColor(0xD2FFFFFF);
            cp.show();
            return true;
        }


        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mCustomBackground) {
            int indexOf = mCustomBackground.findIndexOfValue(objValue.toString());
            mSeeThrough.setEnabled(indexOf != 1);
            switch (indexOf) {
                //Displays color dialog when user has chosen color fill
                case 0:
                    final ColorPickerView colorView = new ColorPickerView(mActivity);
                    int currentColor = Settings.System.getInt(getContentResolver(),
                            Settings.System.LOCKSCREEN_BACKGROUND, -1);
                    if (currentColor != -1) {
                        colorView.setColor(currentColor);
                    }
                    colorView.setAlphaSliderVisible(true);
                    new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.lockscreen_custom_background_dialog_title)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_BACKGROUND, colorView.getColor());
                            updateCustomBackgroundSummary();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).setView(colorView).show();
                    return false;
                //Launches intent for user to select an image/crop it to set as background
                case 1:
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                    intent.setType("image/*");
                    intent.putExtra("crop", "true");
                    intent.putExtra("scale", true);
                    intent.putExtra("scaleUpIfNeeded", false);
                    intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
                    Display display = mActivity.getWindowManager().getDefaultDisplay();
                    int width = display.getWidth();
                    int height = display.getHeight();
                    Rect rect = new Rect();
                    Window window = mActivity.getWindow();
                    window.getDecorView().getWindowVisibleDisplayFrame(rect);
                    int statusBarHeight = rect.top;
                    int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
                    int titleBarHeight = contentViewTop - statusBarHeight;
                    // Lock screen for tablets visible section are different in landscape/portrait,
                    // image need to be cropped correctly, like wallpaper setup for scrolling in background in home screen
                    // other wise it does not scale correctly
                    if (mIsScreenLarge) {
                        width = mActivity.getWallpaperDesiredMinimumWidth();
                        height = mActivity.getWallpaperDesiredMinimumHeight();
                        float spotlightX = (float) display.getWidth() / width;
                        float spotlightY = (float) display.getHeight() / height;
                        intent.putExtra("aspectX", width);
                        intent.putExtra("aspectY", height);
                        intent.putExtra("outputX", width);
                        intent.putExtra("outputY", height);
                        intent.putExtra("spotlightX", spotlightX);
                        intent.putExtra("spotlightY", spotlightY);

                    } else {
                        boolean isPortrait = getResources().getConfiguration().orientation ==
                            Configuration.ORIENTATION_PORTRAIT;
                        intent.putExtra("aspectX", isPortrait ? width : height - titleBarHeight);
                        intent.putExtra("aspectY", isPortrait ? height - titleBarHeight : width);
                    }
                    try {
                        wallpaperTemporary.createNewFile();
                        wallpaperTemporary.setWritable(true, false);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(wallpaperTemporary));
                        intent.putExtra("return-data", false);
                        mActivity.startActivityFromFragment(this, intent, LOCKSCREEN_BACKGROUND);
                    } catch (IOException e) {
                    } catch (ActivityNotFoundException e) {
                    }
                    return false;
                //Sets background color to default
                case 2:
                    Settings.System.putString(getContentResolver(),
                            Settings.System.LOCKSCREEN_BACKGROUND, null);
                    updateCustomBackgroundSummary();
                    break;
            }
        } else if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        } else if (preference == mClockAlign) {
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_ALIGN, value);
            mClockAlign.setSummary(mClockAlign.getEntries()[value]);
            return true;

        } else if (preference == mStylePref) {
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_STYLE, value);
            mStylePref.setSummary(mStylePref.getEntries()[value]);
            return true;
        }
        return false;
    }
    ColorPickerDialog.OnColorChangedListener mCirclesBgColorListener =
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.CIRCLES_LOCK_BG_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
    ColorPickerDialog.OnColorChangedListener mCirclesRingColorListener =
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.CIRCLES_LOCK_RING_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
    ColorPickerDialog.OnColorChangedListener mCirclesHaloColorListener =
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.CIRCLES_LOCK_HALO_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
    ColorPickerDialog.OnColorChangedListener mCirclesWaveColorListener =
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.CIRCLES_LOCK_WAVE_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
}
