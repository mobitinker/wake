package com.msi.wake;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import com.msi.wake.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Wake extends Activity {
	
	public static final String TAG = "Wake";
	public static final String LOGNEW = "wakelog.txt";
	public static final String LOGOLD = "wakelog.old";
	public static final Integer LogSize = 3000;
	
	private Context m_Context;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_Context = this.getApplicationContext();        
        setContentView(R.layout.main);
        
        //Set buttons
        final Button btnSettings = (Button)this.findViewById(R.id.settings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
    		    startActivity(new Intent(Wake.this, EditPreferences.class));
        	}
        });
        final Button btnAirplane = (Button)this.findViewById(R.id.airplane);
        btnAirplane.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		boolean isAM = Settings.System.getInt(m_Context.getContentResolver(),
        				Settings.System.AIRPLANE_MODE_ON, 0) != 0;
       			AlarmReceiver.setAirplaneMode(m_Context, !isAM);
        	}
        });
        final Button btnHotspot = (Button)this.findViewById(R.id.hotspot);
        btnHotspot.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		//Toggle hotspot state
        		AlarmReceiver.enableHotSpot(m_Context, !AlarmReceiver.isHotSpot(m_Context));
        	}
        });
        final Button btnLog = (Button)this.findViewById(R.id.log);
        btnLog.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
    		    startActivity(new Intent(Wake.this, ViewLog.class));
        	}
        });

    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	updateUI();

    	//Set up to listen for when screen is turned off. To test, use hw button to turn off screen
        //IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        //registerReceiver(mScreenOffListener, filter);
        logger("onResume", false);
    }

    @Override
    public void onPause() {
    	super.onPause();
        logger("onPause", false);
    }

    @Override
    public void onDestroy() {
        logger("onDestroy", false);
    	super.onDestroy();
    }

    /* TODO remove
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(this).inflate(R.menu.option, menu);
		return (super.onCreateOptionsMenu(menu));
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId()==R.id.prefs) {
		    startActivity(new Intent(this, EditPreferences.class));
		    return(true);
    	}
    	return(super.onOptionsItemSelected(item));
    }
	*/
    
    /**
     * Updates the main screen with current values
     */
    private void updateUI() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(m_Context);
        TextView t = (TextView)this.findViewById(R.id.enabled);
        t.setText(prefs.getBoolean("wake_enabled", false) ? "Yes" : "No");

        t = (TextView)this.findViewById(R.id.testMode);
        t.setText(prefs.getBoolean("test_mode", false) ? "Yes" : "No");

        t = (TextView)this.findViewById(R.id.wakeTime);
        t.setText(prefs.getString("wake_time", "00:00"));

        t = (TextView)this.findViewById(R.id.duration);
        t.setText(prefs.getString("duration", "300"));
    }

    /* Example of setting wake lock. May be necessary if phone needs to wake to be hotspot
    private Handler wakeUp = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);
            // Wake up phone
            Log.i(TAG, "Wake up the phone");
            
            //Above message fires but phone does not unlock nor redisplay screen. Must need to
            //do something more below.
            
            PowerManager pm = (PowerManager) Wake.this.getSystemService(Context.POWER_SERVICE);
            
            // This may not do what we need
            //long l = SystemClock.uptimeMillis();
            //pm(l, false);	//false will bring the screen back as bright as it was, true - will dim it
            //
            
            //This also does not seem to work to bring back screen
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
            wl.acquire();
            //wl.release();
          
        }
    };
    */
    
	/**
	 * Log a message to file on sdcard. Keep wakelog.txt and wakelog.old. Moves .txt to .old once .txt is
	 * longer than 10k.
	 * @param msg
	 */
	public static void logger(String msg, Boolean toFile) {
		// Note that messages logged to file on boot may fail since SD card may not be read
		try {
			if (toFile) {
				File log = new File(Environment.getExternalStorageDirectory(), LOGNEW);
				try {
					Long l = log.length();
					if (l > LogSize) {
						File oldLog = new File(Environment.getExternalStorageDirectory(), LOGOLD);
						if (oldLog.exists()) {
							oldLog.delete();
						}
						log.renameTo(oldLog);
						log.createNewFile();
					}
					String s;
					//Write a message to /mnt/sdcard/wakelog.txt
					BufferedWriter out = new BufferedWriter(new FileWriter(log.getAbsolutePath(), log.exists()));
					s = new Date().toString() + ", " + msg + "\r\n";
					out.write(s);
					Log.i(TAG, s);
					out.close();
				} catch (IOException e) {
					Log.e(TAG, "Exception appending to log file", e);
				}
			} 
		}
		catch (Exception e) {
			Log.i(TAG, e.getMessage());
			e.printStackTrace();
		}
		Log.i(TAG, msg);
	}

}


