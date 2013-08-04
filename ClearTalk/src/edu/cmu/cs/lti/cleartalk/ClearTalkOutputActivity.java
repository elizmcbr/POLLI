/**
 * @author Elizabeth Davis
 * copyright 2012
 * 
 * Screen that displays feedback from analyzed text
 * Content is all dynamically generated, based on parsing xml string gotten
 * through server interaction, using XMLParsing instance.
 * ExpandableListAdapter is used to create list of confusing words - list 
 * elements have one child for each sentence the word appears in, accompanied
 * by the alternative words.
 * 
 */

package edu.cmu.cs.lti.cleartalk;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;


public class ClearTalkOutputActivity extends Activity {

	private String pref_name = "ClearTalkPrefs"; // preference list name
	private PreferencesLibrary prefLib;
	
	private static final String OUTPUT_KEY = "outputtext";
	private static final String LANG_KEY = "lang"; // chosen language
	
	/* language list */
	private CharSequence[] languages = { "Arabic", "Chinese", "French", 
										 "German", "Hebrew", "Hindi", 
										 "Japanese", "Korean", "Marathi", 
										 "Portuguese", "Russian", "Spanish", 
										 "Tamil", "Telugu", "Thai", "Other" };
	
	
	private static final int HINTS = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.prefLib = new PreferencesLibrary(this, pref_name);
		String language = (String) languages[prefLib.load_int(LANG_KEY)];
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);      
		setContentView(R.layout.cleartalkoutputactivity);
		
		TextView outputTitle = (TextView) findViewById(R.id.output_title);
		TextView outputDesc = (TextView) findViewById(R.id.output_desc);		
		
		Button hints = (Button) findViewById(R.id.output_hint);
		Button edit = (Button) findViewById(R.id.edit_input);
		
		/* Hints button dialog pop-up */
		hints.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(HINTS);
			}
		});
		
		// Back to the input screen
		edit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent("lti.bicc.cleartalk.INPUT"));
			}
		});
	
		
		/* Nothing saved */
		String xml = prefLib.load_str(OUTPUT_KEY);
		if (xml.equals("")) {
			outputTitle.setText(R.string.output_title_empty);
			outputDesc.setText(R.string.output_description_empty);
		}
		/* Something went wrong in receiving data */
		else if (xml.indexOf("</content>") < 0) {
			outputTitle.setText(R.string.output_title_error);
			outputDesc.setText(R.string.output_description_error);
		}
		/* Everything should be fine */
		else {
		
			XMLParsing parse = new XMLParsing(xml);
			
			Paragraph[] paras = parse.getParagraphs();
			ConfusionInstance[] confs = parse.getConfusions();
			
			if (confs == null || confs.length == 0) {
				outputTitle.setText(R.string.output_title_empty);
				outputDesc.setText(R.string.output_description_empty);
			} 
			else {
				if (language.equalsIgnoreCase("Other")) {
					outputTitle.setText(R.string.output_title_other);
				}
				else { // It was a language we have support for
					String title = "For native speakers of " + language +
								   ", these words may cause confusion if " +
								   "mispronounced";
					outputTitle.setText(title);
				}
				outputDesc.setText(R.string.output_description_nonempty);
			}
			
			ExpandableListAdapter adapter;
			ExpandableListView mpList = 
					(ExpandableListView) findViewById(R.id.mp_list);
			//adapter = new MPExpandableListAdapter(paras, confs);
			adapter = new GroupedMPExpListAdapter(paras, confs);
			mpList.setAdapter(adapter);
			
			mpList.setOnGroupClickListener(new OnGroupClickListener() {
				public boolean onGroupClick(ExpandableListView arg0, View arg1,
					int groupPosition, long arg3) {
					return false;
				}
			});
			
			mpList.setOnChildClickListener(new 
				ExpandableListView.OnChildClickListener() {
				public boolean onChildClick(ExpandableListView parent,
						View v, int groupPosition, int childPosition,
						long id) {
					return false;
				}
			});			
			
		}
		
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		
		// What to do when the HINTS button is pressed
		case HINTS:
			return new AlertDialog.Builder(this)
			.setTitle("Using Output")
			.setMessage(R.string.hints_output)
			.setNeutralButton("OK", null)
			.create();
			
		}
		return null;
	}
	

	/* 
	 * Creates the list of minimal pairs and the sentences they occur in. 
	 * This implementation groups together occurrences of the same word,
	 * not distinguishing between plurals
	 */
	public class GroupedMPExpListAdapter extends BaseExpandableListAdapter {
		
		private ArrayList<SpannableString> distinct_mps; // mps grouped together
		private ArrayList<ArrayList<SpannableStringBuilder>> mp_groups; // sentence groups 
		
		// Test if two words should go in the list together
		boolean same_word(String s1, String s2) {
			String longer = (s1.length() > s2.length()) ? s1 : s2;
			String shorter = (s1.length() > s2.length()) ? s2 : s1;
		
			// Check for length
			int longlen = longer.length();
			int shortlen = shorter.length();
			
			if (longlen - shortlen > 2)
				return false;
			
			// If they're actually the same word
			if (longlen - shortlen == 0) {
				if (longer.equalsIgnoreCase(shorter)) return true;

			}
			
			// If one is a plural of the other.
			if (longlen - shortlen == 1) {
				if ((longer.charAt(longlen - 1) == 's') && 
					(longer.substring(0, longlen-1).equalsIgnoreCase(shorter)))
					return true;
				else return false;
			}
			
			// More plural checking
			if (longlen - shortlen == 2) {
				if ((longer.charAt(longlen - 1) == 's') &&
					(longer.charAt(longlen - 2) == 'e') &&
					(longer.substring(0, longlen-2).equalsIgnoreCase(shorter)))
					return true;
				else return false;
			}
			
			return false;
				
		}
		
		// Inserts a minimal pair appropriately into the list of min pairs
		// and adds sentence to list
		void insert_mp(String confusion, SpannableString conf_styled,
				ArrayList<String> distinct, 
				ArrayList<SpannableString> distinct_mps, 
				SpannableStringBuilder item,
				ArrayList<ArrayList<SpannableStringBuilder>> mp_groups) {
			
			ArrayList<SpannableStringBuilder> sents;
			
			
			for (int i = 0; i < distinct.size(); i++) {
				String word = distinct.get(i);
				if (same_word(confusion, word)) {
					sents = mp_groups.get(i);
					sents.add(item);
					return;
				}
			}
			
			sents = new ArrayList<SpannableStringBuilder>();
			sents.add(item);
			distinct.add(confusion);
			distinct_mps.add(conf_styled);
			mp_groups.add(sents);
			return;
			
		}
		
		GroupedMPExpListAdapter(Paragraph[] paras, ConfusionInstance[] confs) {

			ArrayList<SpannableString> distinct_mps;
			ArrayList<ArrayList<SpannableStringBuilder>> mp_groups;
			
			/* If there are no confusions */
			if (confs == null || confs.length == 0) {
				distinct_mps = null;
				mp_groups = null;
			}
			else {
				distinct_mps = new ArrayList<SpannableString>();
				ArrayList<String> distinct = new ArrayList<String>();
				
				mp_groups = new ArrayList<ArrayList<SpannableStringBuilder>>();
				
				/* Get the styled sentence for each confusion */
				for (int i = 0; i < confs.length; i++) {				
					ConfusionInstance conf = confs[i];
					Sentence sent = conf.getSentence(paras);
					int idx = conf.getIndex(sent);				
					
					SpannableString confStyled = new SpannableString(conf.getOriginal());
					confStyled.setSpan((Object) new TextAppearanceSpan(
							ClearTalkOutputActivity.this, 
							R.style.min_pair_parent), 0, confStyled.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					
					// Make a new style object for this confusion
					Object style = (Object) new TextAppearanceSpan(
							ClearTalkOutputActivity.this, R.style.min_pair);
					SpannableStringBuilder item = new SpannableStringBuilder();
					item.append(sent.getStyledOriginal(style, idx));
					item.append("\n\n");
					item.append(conf.getConfPhrase(
							ClearTalkOutputActivity.this, R.style.min_pair));
					
					String confusion = conf.getOriginal().toLowerCase();
					
					insert_mp(confusion, confStyled, distinct, distinct_mps,
							item, mp_groups);
					
				}
				
				// apply style to the minimal pairs 
				for (int i = 0; i < distinct_mps.size(); i++) {
					Object style = (Object) new TextAppearanceSpan(
							ClearTalkOutputActivity.this, R.style.min_pair);
					SpannableString conf = distinct_mps.get(i);
					conf.setSpan(style, 0, conf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					int count = mp_groups.get(i).size();
					if (count > 1) {
						SpannableStringBuilder mp_and_count = new SpannableStringBuilder(distinct_mps.get(i));
						mp_and_count.append(" ("+Integer.toString(mp_groups.get(i).size())+")");
						SpannableString new_text = new SpannableString(mp_and_count);
						distinct_mps.set(i, new_text);	
					}
				}
			}	
				
			this.mp_groups = mp_groups;
			this.distinct_mps = distinct_mps;

		}
		
		/**
		 * get the original sentence associated with the minimal pair index
		 */
		public Object getChild(int groupPosition, int childPosition) {
			if (distinct_mps == null)
				return null;
			else
				return mp_groups.get(groupPosition).get(childPosition);
		}
		
		/**
		 * find out which original sentence it was based on the index...
		 */
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}
		
		/**
		 * get number of sentences for a minimal pair
		 */
		public int getChildrenCount(int groupPosition) {
			if (distinct_mps == null)
				return 0;
			else 
				return mp_groups.get(groupPosition).size();
		}

		/**
		 * get generic view
		 */
		public TextView getGenericView() {
			//Layout parameters for the ExpandableListView
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT, 
				ViewGroup.LayoutParams.WRAP_CONTENT);
			
			TextView tv = new TextView(ClearTalkOutputActivity.this);
			tv.setLayoutParams(lp);
			// Center text vertically
			tv.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
			tv.setTextColor(getResources().getColor(R.color.black));
			//Set text starting position (LTRB)
			return tv;
		}
		
		/**
		 * get the view associated with a particular sentence
		 */
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			TextView tv = getGenericView();
			tv.setText((SpannableStringBuilder) 
					getChild(groupPosition,childPosition));
			tv.setPadding(16, 18, 16, 18);
			tv.setClickable(false);
			return tv;
		}

		/**
		 * get the conf_instance at that position
		 */
		public Object getGroup(int groupPosition) {
			if (distinct_mps == null)
				return null;
			else
				return distinct_mps.get(groupPosition);
		}

		/**
		 * get the number of min_pairs
		 */
		public int getGroupCount() {
			if (distinct_mps == null)
				return 0;
			else
				return distinct_mps.size();
		}

		/** 
		 * get the index for a conf_instance 
		 */
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView tv = getGenericView();
			tv.setText((SpannableString) getGroup(groupPosition));
			tv.setPadding(64, 18, 0, 18);
			return tv;
		}

		/**
		 * Indicates whether the child and group IDs are stable across changes 
		 * to the underlying data.
		 */
		public boolean hasStableIds() {
			return true;
		}

		/**
		 * determine if sentence is selectable
		 */
		public boolean isChildSelectable(int groupPosition, 
				int childPosition) {
			if (distinct_mps == null)
				return false;
			else if (mp_groups == null)
				return false;
			else if (mp_groups.get(groupPosition).get(childPosition) == null)
				return false;
			return true;
		}
		
	}

	/*
	 * Basic expandable list adapter - shows every word separately
	 * NOT USED
	 */
	public class MPExpandableListAdapter extends BaseExpandableListAdapter {
		
		private SpannableString[] conf_strings; // confusions in their String form
		private Sentence[] sentences; // Sentences correspond to each min pair
		private SpannableStringBuilder[][] styled_plus_mp; // sents, min pairs
		
		MPExpandableListAdapter(Paragraph[] paras, ConfusionInstance[] confs) {
				
			int[] conf_idxs;			
			Sentence[] conf_sents;
			SpannableString[] conf_origs;
			SpannableStringBuilder[][] styled_plus_mp;
			
			if (confs == null || confs.length == 0) {
				conf_sents = null;
				conf_origs = null;
				conf_idxs = null;
				styled_plus_mp = null;
			}
			else {
				conf_sents = new Sentence[confs.length];
				conf_origs = new SpannableString[confs.length];
				conf_idxs = new int[confs.length];
				styled_plus_mp = new SpannableStringBuilder[confs.length][1];
				for (int i = 0; i < confs.length; i++) {
					ConfusionInstance conf = confs[i];
					Sentence sent = conf.getSentence(paras);
					int idx = conf.getIndex(sent);				
					conf_sents[i] = sent;
					
					SpannableString confStyled = new SpannableString(conf.getOriginal());
					confStyled.setSpan((Object) new TextAppearanceSpan(
							ClearTalkOutputActivity.this, 
							R.style.min_pair_parent), 0, confStyled.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					conf_origs[i] = confStyled;
					conf_idxs[i] = idx;
					Object style = (Object) new TextAppearanceSpan(
							ClearTalkOutputActivity.this, R.style.min_pair);
					SpannableStringBuilder item = new SpannableStringBuilder();
					item.append(sent.getStyledOriginal(style, idx));
					item.append("\n\n");
					item.append(conf.getConfPhrase(
							ClearTalkOutputActivity.this, R.style.min_pair));
					styled_plus_mp[i][0] = item;
				}
			}	
				
			this.conf_strings = conf_origs;
			this.sentences = conf_sents;
			this.styled_plus_mp = styled_plus_mp;

		}
		
		/**
		 * get the original sentence associated with the minimal pair index
		 */
		public Object getChild(int groupPosition, int childPosition) {
			if (styled_plus_mp == null)
				return null;
			else
				return styled_plus_mp[groupPosition][childPosition];
		}
		
		/**
		 * find out which original sentence it was based on the index...
		 */
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}
		
		/**
		 * get number of sentences for a minimal pair
		 */
		public int getChildrenCount(int groupPosition) {
			if (styled_plus_mp == null)
				return 0;
			else 
				return styled_plus_mp[groupPosition].length;
		}

		/**
		 * get generic view
		 */
		public TextView getGenericView() {
			//Layout parameters for the ExpandableListView
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT, 
				ViewGroup.LayoutParams.WRAP_CONTENT);
			
			TextView tv = new TextView(ClearTalkOutputActivity.this);
			tv.setLayoutParams(lp);
			// Center text vertically
			tv.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
			tv.setTextColor(getResources().getColor(R.color.black));
			//Set text starting position (LTRB)
			return tv;
		}
		
		/**
		 * get the view associated with a particular sentence
		 */
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			TextView tv = getGenericView();
			tv.setText((SpannableStringBuilder) 
					getChild(groupPosition,childPosition));
			tv.setPadding(16, 18, 16, 18);
			tv.setClickable(false);
			return tv;
		}

		/**
		 * get the conf_instance at that position
		 */
		public Object getGroup(int groupPosition) {
			if (conf_strings == null)
				return null;
			else 
				return conf_strings[groupPosition];
		}

		/**
		 * get the number of min_pairs
		 */
		public int getGroupCount() {
			if (conf_strings == null)
				return 0;
			else
				return conf_strings.length;
		}

		/** 
		 * get the index for a conf_instance 
		 */
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView tv = getGenericView();
			tv.setText((SpannableString) getGroup(groupPosition));
			tv.setPadding(64, 18, 0, 18);
			return tv;
		}

		/**
		 * Indicates whether the child and group IDs are stable across changes 
		 * to the underlying data.
		 */
		public boolean hasStableIds() {
			return true;
		}

		/**
		 * determine if sentence is selectable
		 */
		public boolean isChildSelectable(int groupPosition, 
				int childPosition) {
			if (conf_strings == null)
				return false;
			else if (styled_plus_mp == null)
				return false;
			else if (styled_plus_mp[groupPosition][childPosition] == null)
				return false;
			return true;
		}
		
	}

}

