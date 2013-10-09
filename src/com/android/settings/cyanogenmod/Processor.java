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
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.util.Log;
import com.android.settings.CMDProcessor;

//
// CPU Related Settings
//
public class Processor extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String FREQ_CUR_PREF = "pref_cpu_freq_cur";
    public static final String SCALE_CUR_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    public static final String FREQINFO_CUR_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq";
    private static String FREQ_CUR_FILE = SCALE_CUR_FILE;
    public static final String GOV_PREF = "pref_cpu_gov";
    public static final String GOV_LIST_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";
    public static final String GOV_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    public static final String FREQ_MIN_PREF = "pref_cpu_freq_min";
    public static final String FREQ_MAX_PREF = "pref_cpu_freq_max";
    public static final String FREQ_LIST_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies";
	public static final String SC_AH_FILE = "/sys/kernel/auto_hotplug/auto_hotplug_enabled";
	public static final String SC_AH = "pref_cpu_second_core_auto_hotplug";
    public static final String SCM_FILE = "/sys/kernel/auto_hotplug/single_core_mode";
    public static final String SCM = "pref_cpu_single_core_mode";
    public static final String SCHED_PS = "pref_cpu_sched_power_savings";
    public static final String SCHED_PS_FILE = "/sys/devices/system/cpu/sched_mc_power_savings";
    public static String FREQ_MAX_FILE = null;
    public static String FREQ_MIN_FILE = null;
    public static final String SOB_PREF = "pref_cpu_set_on_boot";
    public static final String TEGRA_MAX_FREQ_PATH = "/sys/module/cpu_tegra/parameters/cpu_user_cap";
    public static final String NUM_OF_CPUS_PATH = "/sys/devices/system/cpu/present";

    public static final String[] SCHED_PS_MODES = new String[]{
        "No power saving load balance",
        "Fill one thread/core/package first for long running threads",
        "Also bias task wakeups to semi-idle cpu package for power savings"
    };
    public static final String[] SCHED_PS_MODES_SHORT = new String[]{
        "No load balance",
        "Mid load balance",
        "Max load balance"
    };

    protected static boolean freqCapFilesInitialized = false;

    private static final String TAG = "CPUSettings";

    private String mGovernorFormat;
    private String mMinFrequencyFormat;
    private String mMaxFrequencyFormat;

    private Preference mCurFrequencyPref;
    private CheckBoxPreference mAutoHotplugPref;
    private CheckBoxPreference mSingleCorePref;
    private ListPreference mGovernorPref;
    private ListPreference mMinFrequencyPref;
    private ListPreference mMaxFrequencyPref;
    private ListPreference mSchedPSPref;

    private class CurCPUThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(500);
                    final String curFreq = Utils.fileReadOneLine(FREQ_CUR_FILE);
                    if (curFreq != null)
                        mCurCPUHandler.sendMessage(mCurCPUHandler.obtainMessage(0, curFreq));
                }
            } catch (InterruptedException e) {
            }
        }
    };

    private CurCPUThread mCurCPUThread = new CurCPUThread();

    private Handler mCurCPUHandler = new Handler() {
        public void handleMessage(Message msg) {
            mCurFrequencyPref.setSummary(toMHz((String) msg.obj));
        }
    };

    private void initFreqCapFiles()
    {
        if (freqCapFilesInitialized) return;
        FREQ_MAX_FILE = getString(R.string.max_cpu_freq_file);
        FREQ_MIN_FILE = getString(R.string.min_cpu_freq_file);
        freqCapFilesInitialized = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initFreqCapFiles();

        mGovernorFormat = getString(R.string.cpu_governors_summary);
        mMinFrequencyFormat = getString(R.string.cpu_min_freq_summary);
        mMaxFrequencyFormat = getString(R.string.cpu_max_freq_summary);

        String[] availableFrequencies = new String[0];
        String[] availableGovernors = new String[0];
        String[] frequencies;
        String availableGovernorsLine;
        String availableFrequenciesLine;
        String temp;

        addPreferencesFromResource(R.xml.processor_settings);

        PreferenceScreen prefScreen = getPreferenceScreen();

        mGovernorPref = (ListPreference) prefScreen.findPreference(GOV_PREF);
        mCurFrequencyPref = (Preference) prefScreen.findPreference(FREQ_CUR_PREF);
        mMinFrequencyPref = (ListPreference) prefScreen.findPreference(FREQ_MIN_PREF);
        mMaxFrequencyPref = (ListPreference) prefScreen.findPreference(FREQ_MAX_PREF);
        mSchedPSPref = (ListPreference) prefScreen.findPreference(SCHED_PS);
        mAutoHotplugPref = (CheckBoxPreference) prefScreen.findPreference(SC_AH);
        mSingleCorePref = (CheckBoxPreference) prefScreen.findPreference(SCM);

        /* Governor
        Some systems might not use governors */
        if (!Utils.fileExists(GOV_LIST_FILE) || !Utils.fileExists(GOV_FILE) || (temp = Utils.fileReadOneLine(GOV_FILE)) == null || (availableGovernorsLine = Utils.fileReadOneLine(GOV_LIST_FILE)) == null) {
            prefScreen.removePreference(mGovernorPref);

        } else {
            availableGovernors = availableGovernorsLine.split(" ");

            mGovernorPref.setEntryValues(availableGovernors);
            mGovernorPref.setEntries(availableGovernors);
            mGovernorPref.setValue(temp);
            mGovernorPref.setSummary(String.format(mGovernorFormat, temp));
            mGovernorPref.setOnPreferenceChangeListener(this);
        }

        // Disable the min/max list if we dont have a list file
        if (!Utils.fileExists(FREQ_LIST_FILE) || (availableFrequenciesLine = Utils.fileReadOneLine(FREQ_LIST_FILE)) == null) {
            mMinFrequencyPref.setEnabled(false);
            mMaxFrequencyPref.setEnabled(false);

        } else {
            availableFrequencies = availableFrequenciesLine.split(" ");

            frequencies = new String[availableFrequencies.length];
            for (int i = 0; i < frequencies.length; i++) {
                frequencies[i] = toMHz(availableFrequencies[i]);
            }

            // Min frequency
            if (!Utils.fileExists(FREQ_MIN_FILE) || (temp = Utils.fileReadOneLine(FREQ_MIN_FILE)) == null) {
                mMinFrequencyPref.setEnabled(false);
            } else {
                mMinFrequencyPref.setEntryValues(availableFrequencies);
                mMinFrequencyPref.setEntries(frequencies);
                mMinFrequencyPref.setValue(temp);
                mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat, toMHz(temp)));
                mMinFrequencyPref.setOnPreferenceChangeListener(this);
            }

            // Max frequency
            if (!Utils.fileExists(FREQ_MAX_FILE) || (temp = Utils.fileReadOneLine(FREQ_MAX_FILE)) == null) {
                mMaxFrequencyPref.setEnabled(false);
            } else {
        	    mMaxFrequencyPref.setEntryValues(availableFrequencies);
                mMaxFrequencyPref.setEntries(frequencies);
                mMaxFrequencyPref.setValue(temp);
                mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat, toMHz(temp)));
                mMaxFrequencyPref.setOnPreferenceChangeListener(this);
            }
        }

        // Cur frequency
        if (!Utils.fileExists(FREQ_CUR_FILE)) {
            FREQ_CUR_FILE = FREQINFO_CUR_FILE;
        }

        if (!Utils.fileExists(FREQ_CUR_FILE) || (temp = Utils.fileReadOneLine(FREQ_CUR_FILE)) == null) {
            mCurFrequencyPref.setEnabled(false);
        } else {
            mCurFrequencyPref.setSummary(toMHz(temp));

            mCurCPUThread.start();
        }

        boolean autoHotplugActive = false;
		//Second Core Auto Hotplug 
        if (!Utils.fileExists(SC_AH_FILE) || (temp = Utils.fileReadOneLine(SC_AH_FILE)) == null) {
            prefScreen.removePreference(mAutoHotplugPref);
        } else {
            mAutoHotplugPref.setChecked(temp.equals("1") ? true : false);
            mAutoHotplugPref.setOnPreferenceChangeListener(this);
        }

        //Single Core Mode 
        if (!Utils.fileExists(SCM_FILE) || (temp = Utils.fileReadOneLine(SCM_FILE)) == null) {
            prefScreen.removePreference(mSingleCorePref);
        } else {
            mSingleCorePref.setChecked(temp.equals("1") ? true : false);
            mSingleCorePref.setOnPreferenceChangeListener(this);
        }

        //Sched power savings
        if (!Utils.fileExists(SCHED_PS_FILE) || (temp = Utils.fileReadOneLine(SCHED_PS_FILE)) == null) {
            prefScreen.removePreference(mSchedPSPref);
        } else {
            try {
                mSchedPSPref.setEntryValues(new String[]{"0", "1", "2"});
                mSchedPSPref.setEntries(SCHED_PS_MODES_SHORT);
                mSchedPSPref.setValue(temp);
                mSchedPSPref.setSummary(SCHED_PS_MODES[Integer.valueOf(temp)]);
                mSchedPSPref.setOnPreferenceChangeListener(this);
            } catch (Exception ex){
                prefScreen.removePreference(mSchedPSPref);
            }
        }
    }

    @Override
    public void onResume() {
        String temp;

        super.onResume();

        initFreqCapFiles();

        if (Utils.fileExists(FREQ_MIN_FILE) && (temp = Utils.fileReadOneLine(FREQ_MIN_FILE)) != null) {
            mMinFrequencyPref.setValue(temp);
            mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat, toMHz(temp)));
        }

        if (Utils.fileExists(FREQ_MAX_FILE) && (temp = Utils.fileReadOneLine(FREQ_MAX_FILE)) != null) {
            mMaxFrequencyPref.setValue(temp);
            mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat, toMHz(temp)));
        }

        if (Utils.fileExists(GOV_FILE) && (temp = Utils.fileReadOneLine(GOV_FILE)) != null) {
            mGovernorPref.setSummary(String.format(mGovernorFormat, temp));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCurCPUThread.interrupt();
        try {
            mCurCPUThread.join();
        } catch (InterruptedException e) {
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        initFreqCapFiles();

        String fname = "";
        boolean success = false;
        boolean mIsTegra = Utils.fileExists(Processor.TEGRA_MAX_FREQ_PATH);

        if (newValue != null) {
            if (preference == mGovernorPref) {
                fname = GOV_FILE;
            } else if (preference == mMinFrequencyPref) {
                fname = FREQ_MIN_FILE;
            } else if (preference == mMaxFrequencyPref) {
                fname = FREQ_MAX_FILE;
            } else if (preference == mAutoHotplugPref) {
                fname = SC_AH_FILE;
                newValue = newValue.toString().equals("true") ? "1" : "0";
            } else if (preference == mSingleCorePref) {
                fname = SCM_FILE;
                newValue = newValue.toString().equals("true") ? "1" : "0";
            } else if (preference == mSchedPSPref) {
                fname = SCHED_PS_FILE;
            }

            Process process = null;
            for (int i = 0; i < getNumOfCpus(); i++) {
                try {
                    new CMDProcessor().su.runWaitFor("busybox echo "
                            + (String) newValue + " > "
                            + fname.replace("cpu0", "cpu" + i));
                    success = true;
                } catch (Exception ex) {
                    Log.d(TAG, "Applying " + fname.replace("cpu0", "cpu" + i) + " failed!");
                    success = false;
                }
            }
            if (success) {
                if (preference == mGovernorPref) {
                    mGovernorPref.setSummary(String.format(mGovernorFormat, (String) newValue));
                } else if (preference == mMinFrequencyPref) {
                    mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat,
                            toMHz((String) newValue)));
                } else if (preference == mMaxFrequencyPref) {
                    if (mIsTegra) {
                        Utils.fileWriteOneLine(Processor.TEGRA_MAX_FREQ_PATH, (String) newValue);
                    }
                    mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat,
                            toMHz((String) newValue)));
                } else if (preference == mSchedPSPref) {
                    int pos = Integer.valueOf((String)newValue);
                    mSchedPSPref.setSummary(SCHED_PS_MODES[pos] + "\n[" + SCHED_PS_MODES_SHORT[pos] + "]");
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private String toMHz(String mhzString) {
        return new StringBuilder().append(Integer.valueOf(mhzString) / 1000).append(" MHz")
                .toString();
    }

    public static int getNumOfCpus() {
        int numOfCpu = 1;
        String numOfCpus = Utils.fileReadOneLine(NUM_OF_CPUS_PATH);
        String[] cpuCount = numOfCpus.split("-");
        if (cpuCount.length > 1) {
            try {
                int cpuStart = Integer.parseInt(cpuCount[0]);
                int cpuEnd = Integer.parseInt(cpuCount[1]);

                numOfCpu = cpuEnd - cpuStart + 1;

                if (numOfCpu < 0)
                    numOfCpu = 1;
            } catch (NumberFormatException ex) {
                numOfCpu = 1;
            }
        }
        return numOfCpu;
    }
}
