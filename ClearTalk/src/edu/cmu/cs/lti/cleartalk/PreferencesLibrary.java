/**
 * 
 * @author Elizabeth Davis
 * copyright 2012
 * 
 * Some common functions used to deal with Preferences within an activity
 *
 */

package edu.cmu.cs.lti.cleartalk;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;


public class PreferencesLibrary {
	
	PreferencesLibrary(Activity activity, String pref_name) {
		this.activity = activity;
		this.pref_name = pref_name;
	}
	
	private String pref_name;
	private Activity activity;
		
	/**
	 * Save a string to the shared preferences list 
	 */
	public void save_str(String key, String to_save) {
		SharedPreferences prefs = activity.getSharedPreferences(pref_name, 
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, to_save);
		editor.commit();
	}
	
	/**
	 * Save a boolean to the shared preferences list
	 */
	public void save_bool(String key, boolean to_save) {
		SharedPreferences prefs = activity.getSharedPreferences(pref_name,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(key, to_save);
		editor.commit();
	}
	
	
	/**
	 * Save an int to the shared preferences list 
	 */
	public void save_int(String key, int to_save) {
		SharedPreferences prefs = activity.getSharedPreferences(pref_name, 
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(key, to_save);
		editor.commit();
	}
	
	/**
	 * Load a string from the shared preferences list 
	 */
	public String load_str(String key) {
		SharedPreferences prefs = activity.getSharedPreferences(pref_name, 
				Context.MODE_PRIVATE);
		return prefs.getString(key,  "");
	}
	
	/**
	 * Load a boolean from the shared preferences list
	 * Default is true
	 */
	public boolean load_bool(String key) {
		SharedPreferences prefs = activity.getSharedPreferences(pref_name,
				Context.MODE_PRIVATE);
		return prefs.getBoolean(key, true);
	}
	
	/**
	 * Load an int from the shared preferences list 
	 */
	public int load_int(String key) {
		SharedPreferences prefs = activity.getSharedPreferences(pref_name, 
				Context.MODE_PRIVATE);
		return prefs.getInt(key, -1);
	}

	

}
