/**
 * @author Elizabeth Davis
 * copyright 2012
 * Home screen with a blurb
 * 
 */

package edu.cmu.cs.lti.cleartalk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

public class ClearTalkActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // hide title bar
        setContentView(R.layout.main);
        
        Button enter = (Button) findViewById(R.id.go_to_app); // enter button
        enter.setOnClickListener(new OnClickListener() { 
        	public void onClick(View v) {
        		startActivity(new Intent("lti.bicc.cleartalk.INPUT"));
        	}
        });
    }
}