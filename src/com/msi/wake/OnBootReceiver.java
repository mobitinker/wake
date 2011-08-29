package com.msi.wake;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {

	public static void setAlarm(Context context) {
		AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Calendar cal = Calendar.getInstance();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		String time = prefs.getString("wake_time", "00:00");
		cal.set(Calendar.HOUR_OF_DAY, TimePreference.getHour(time));
		cal.set(Calendar.MINUTE, TimePreference.getMinute(time));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		//Set to tomorrow if needed
		if (cal.getTimeInMillis() < System.currentTimeMillis()) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		Boolean isTestMode = prefs.getBoolean("test_mode", false);
		Long wakeTime, sleepTime;
		
		if (!isTestMode) {
				
			//Set alarm to wake and turn on hotspot
			wakeTime = cal.getTimeInMillis();
			mgr.setRepeating(AlarmManager.RTC_WAKEUP, wakeTime,
					AlarmManager.INTERVAL_DAY, getPendingIntent(context, "Wake"));
	
			//Now set alarm to go back to sleep after waking
			Integer secondsToStayAwake = Integer.parseInt(prefs.getString("duration", "300"));
			cal.add(Calendar.SECOND, secondsToStayAwake);
			sleepTime = cal.getTimeInMillis(); 
			mgr.setRepeating(AlarmManager.RTC_WAKEUP, sleepTime,
					AlarmManager.INTERVAL_DAY, getPendingIntent(context, "Sleep"));
			Wake.logger("Set alarms. Normal. Wake: " + wakeTime.toString() + " Sleep: " + sleepTime.toString(), true);
		}
		else
		{			
			//Set alarm to start in 2 min and continue every five minutes
			wakeTime = System.currentTimeMillis() + 2 * 60 * 1000;
			mgr.setRepeating(AlarmManager.RTC_WAKEUP, wakeTime,
					5 * 60 * 1000, getPendingIntent(context, "Wake"));
	
			//Now set alarm to go back to sleep after waking
			sleepTime = wakeTime + 1 * 60 * 1000; 
			mgr.setRepeating(AlarmManager.RTC_WAKEUP, sleepTime,
					5 * 60 * 1000, getPendingIntent(context, "Sleep"));
			Wake.logger("Set alarms. Test. Wake: " + wakeTime.toString() + " Sleep: " + sleepTime.toString(), true);
		}
	}

	public static Boolean wakeEnabled(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean("wake_enabled", false);
	}
	
	/**
	 * Cancel repeating alarm to turn on hotspot and any pending stop alarm
	 * @param context
	 */
	public static void cancelAlarm(Context context) {
		AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		mgr.cancel(getPendingIntent(context, "Wake"));
		mgr.cancel(getPendingIntent(context, "Sleep"));
	}

	public static PendingIntent getPendingIntent(Context context, String data) {
		Intent i = new Intent(context, AlarmReceiver.class);
		Uri u = Uri.parse(data);
		//data indicates the action to perform
		i.setData(u);
		return (PendingIntent.getBroadcast(context, 0, i, 0));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Wake.logger("Start up on boot", true);
	
			if (wakeEnabled(context)) {
				Wake.logger("Setting alarms", false);
				setAlarm(context);

				//Default to sleeping
				AlarmReceiver.enableHotSpot(context, false);
				AlarmReceiver.setAirplaneMode(context, false);

				//Start up app. Note: Android may shut it down later
				Intent startupIntent = new Intent(context, Wake.class); // substitute with your launcher class
				startupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(startupIntent);	

			}
		}
		catch (Exception e) {
			Wake.logger("Exception on boot: " + e.getMessage(), true);
		}
	}
}