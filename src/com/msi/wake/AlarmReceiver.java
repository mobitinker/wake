package com.msi.wake;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
	private static final String OUR_SSID = "SWEETSpot";
	private static final int WIFI_AP_STATE_DISABLING = 0;
	private static final int WIFI_AP_STATE_DISABLED = 1;
	private static final int WIFI_AP_STATE_ENABLING = 2;
	private static final int WIFI_AP_STATE_ENABLED = 3;
	private static final int WIFI_AP_STATE_FAILED = 4;	

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Uri u = intent.getData();
			String cmd = u.toString();
			Wake.logger("Command: " + cmd, true);
			if (cmd.equals("Wake")) {
				setAirplaneMode(context, false);
				enableHotSpot(context, true);
			} else if (cmd.equals("Sleep")) {
				//Need to disable hotspot before going to airplane mode
				enableHotSpot(context, false);
				setAirplaneMode(context, true);
			} else if (cmd.equals("Toggle")) {
				boolean isAM = Settings.System.getInt(
						context.getContentResolver(),
						Settings.System.AIRPLANE_MODE_ON, 0) != 0;
				if (isAM) {
					setAirplaneMode(context, false);
					enableHotSpot(context, true);
				} else {
					setAirplaneMode(context, true);
					enableHotSpot(context, false);
				}
			}
		} catch (Exception e) {
			Toast.makeText(
					context,
					"There was an error somewhere, but we still received an alarm",
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();

		}
	}

	/***
	 * Set up hotspot using hidden method. Note this could change so this app
	 * should not be put in stores.
	 */
	public static void enableHotSpot(Context context, Boolean enabled) {
		
		boolean isAM = Settings.System.getInt(context.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0) != 0;
		if (isAM) {
			Wake.logger("Bad! Trying to enable hotspot In airplane mode!", true);
			return;
		}

		//Undocumented API ahead
		WifiManager wifi = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		Method[] wmMethods = wifi.getClass().getDeclaredMethods();
		for (Method method : wmMethods) {
			if (method.getName().equals("setWifiApEnabled")) {
				WifiConfiguration netConfig = new WifiConfiguration();
				netConfig.SSID = OUR_SSID;
				// Set as open
				netConfig.allowedAuthAlgorithms
						.set(WifiConfiguration.AuthAlgorithm.OPEN);

				try {
					method.invoke(wifi, netConfig, enabled);
					Wake.logger("Hotspot " + (enabled ? "on" : "off"), true);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					Wake.logger(e.getMessage(), true);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					Wake.logger(e.getMessage(), true);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					Wake.logger(e.getMessage(), true);
				} catch (Exception e) {
					Wake.logger(e.getMessage(), true);
				}
			}
		}
	}


	/***
	 * Set up hotspot using hidden method. Note this could change so this app
	 * should not be put in stores.
	 */
	public static Boolean isHotSpot(Context context) {
		//Undocumented API ahead
		int state = WIFI_AP_STATE_DISABLED;
		WifiManager wifi = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		Method[] wmMethods = wifi.getClass().getDeclaredMethods();
		for (Method method : wmMethods) {
			if (method.getName().equals("getWifiApState")) {
				try {
					state = (Integer)method.invoke(wifi);
					Wake.logger("Hotspot state: " + state, false);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					Wake.logger(e.getMessage(), true);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					Wake.logger(e.getMessage(), true);
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					Wake.logger(e.getMessage(), true);
				} catch (Exception e) {
					Wake.logger(e.getMessage(), true);
				}
			}
		}
		return (state == WIFI_AP_STATE_ENABLED);
	}


	/**
	 * Sets/unsets Airplane Mode, if needed. Broadcasts change
	 * 
	 * @param status
	 *            True = set airplane mode on
	 */
	public static void setAirplaneMode(Context context, boolean status) {

		boolean isAM = Settings.System.getInt(context.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0) != 0;
		
		String radios = Settings.System.getString(context.getContentResolver(),
				Settings.System.AIRPLANE_MODE_RADIOS);
		
		//This line is reporting all radios affected but annunciator does not seem to think so. Does not show airplane
		Wake.logger("Airplane mode is: " + isAM + " changing to " + status + " For radios: " + radios, false);
		
		// It appears Airplane mode should only be toggled. Don't reset to
		// current state.
		if (isAM && !status) {
			Settings.System.putInt(context.getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, 0);
			Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			intent.putExtra("state", status);
			context.sendBroadcast(intent);
			return;
		}
		if (!isAM && status) {
			Settings.System.putInt(context.getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, 1);
			Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			intent.putExtra("state", status);
			context.sendBroadcast(intent);
			return;
		}
	}
}
