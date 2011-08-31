package com.msi.wake;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.msi.wake.R;
import com.msi.wake.OnBootReceiver;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class EditPreferences extends PreferenceActivity {
	private SharedPreferences prefs = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onResume() {
		super.onResume();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(onChange);
	}

	@Override
	public void onPause() {
		prefs.unregisterOnSharedPreferenceChangeListener(onChange);
		super.onPause();
	}

	SharedPreferences.OnSharedPreferenceChangeListener onChange = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key) {
			boolean enabled = prefs.getBoolean("wake_enabled", false);
			if ("wake_enabled".equals(key)) {
				//Keep app from being killed if wakeup is enabled. This has not been tested
				int flag = (enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
						: PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
				ComponentName component = new ComponentName(EditPreferences.this, OnBootReceiver.class);
				getPackageManager().setComponentEnabledSetting(component, flag,	PackageManager.DONT_KILL_APP);
			}

			//Always cancel
			OnBootReceiver.cancelAlarm(EditPreferences.this);
			if (enabled) {
				//Schedule alarm with new settings
				OnBootReceiver.setAlarm(EditPreferences.this);
			} 
			//Put device in airplane mode if Wake is enabled
			AlarmReceiver.enableHotSpot(EditPreferences.this, false);
			AlarmReceiver.setAirplaneMode(EditPreferences.this, enabled);
			
			Wake.logger("Wake " + (enabled ? "enabled" : "disabled") + " in settings", true);
		}
	};
}