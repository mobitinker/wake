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

/**
 * Handles setting alarms and starting app on boot.
 * @author Mark Murphy
 *
 */
public class OnBootReceiver extends BroadcastReceiver {

	/**
	 * Sets alarms to awaken and go back to sleep, based on shared preferences
	 * @param context
	 */
	public static void setAlarm(Context context) {
		AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Boolean isTestMode = prefs.getBoolean("test_mode", false);
		
		Long wakeTime, sleepTime;
		
		if (!isTestMode) {
			//Normal case. Set alarm to occur at same time, every 24 hours
			Calendar cal = Calendar.getInstance();
			String time = prefs.getString("wake_time", "00:00");
			cal.set(Calendar.HOUR_OF_DAY, TimePreference.getHour(time));
			cal.set(Calendar.MINUTE, TimePreference.getMinute(time));
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			//Set to tomorrow if needed
			if (cal.getTimeInMillis() < System.currentTimeMillis()) {
				cal.add(Calendar.DAY_OF_YEAR, 1);
			}

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
			//This is a testing set up with more frequent alarms. Not meant for typical use. May be removed
			//some day.
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

	/**
	 * Returns value of wake_enabled shared preference
	 * @param context
	 * @return
	 */
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

	/**
	 * Finds and returns pending intents. Used to cancel alarms 
	 * @param context
	 * @param data
	 * @return
	 */
	public static PendingIntent getPendingIntent(Context context, String data) {
		Intent i = new Intent(context, AlarmReceiver.class);
		Uri u = Uri.parse(data);
		//data indicates the action to perform
		i.setData(u);
		return (PendingIntent.getBroadcast(context, 0, i, 0));
	}

	@Override
	/**
	 * Handler for device's boot message. Puts phone to sleep and starts main activity
	 */
	public void onReceive(Context context, Intent intent) {
		try {
			Wake.logger("Start up on boot", true);
	
			if (wakeEnabled(context)) {
				Wake.logger("Setting alarms", false);
				setAlarm(context);

				//Default to sleeping
				AlarmReceiver.enableHotSpot(context, false);
				AlarmReceiver.setAirplaneMode(context, true);

				//Start up app. Note: Android may shut it down later
				Intent startupIntent = new Intent(context, Wake.class); 
				startupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(startupIntent);	
			}
		}
		catch (Exception e) {
			Wake.logger("Exception on boot: " + e.getMessage(), false);
		}
	}
}