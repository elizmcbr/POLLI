/**
 * @author Elizabeth Davis
 * copyright 2012
 * 
 * Gets input from the user regarding input text and native language, then
 * allows user to send that to the server for analysis. Input can be either
 * raw text or imported file.
 */

package edu.cmu.cs.lti.cleartalk;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ClearTalkInputActivity extends Activity {
	
	private String pref_name = "ClearTalkPrefs";
	private PreferencesLibrary prefLib;
	
	private static final String INPUT_TEXT_KEY = "inputtext"; // text in box
	private static final String IMPORT_FILE_KEY = "importfile"; // file name
	private static final String LANG_KEY = "lang"; // chosen language
	private static final String OUTPUT_KEY = "outputtext"; // xml output 
	private static final String ORIENT_KEY = "orientation"; // setting
	private static final String INPUT_TYPE_KEY = "inputtype"; // file or text?
	private static final String PATH_KEY = "path"; // path for file traversal
	
	private static final int TEXT_INPUT = 0; // to input text
	private static final int FILE_INPUT = 1; // to import a file
	
	private static final int HELP = 0; // for help button
	private static final int LANGUAGE = 1; // for selecting a language
	
	private String path = Environment.getExternalStorageDirectory().getPath();
	
	/*
	 * File comparison - directories come first, then files, alphabetically
	 */
	private class FileListCompare implements Comparator<File> {
		public int compare(File f1, File f2) {
			String s1 = f1.getName();
			String s2 = f2.getName();
			if (f1.isDirectory()) {
				if (f2.isDirectory())
					return s1.compareToIgnoreCase(s2);
				else return -1;
			}
			else if (f2.isDirectory())
					return 1;
			else return s1.compareToIgnoreCase(s2);
		}
		
		public boolean equals(File f1, File f2) {
			return ((f1.isDirectory() == f2.isDirectory()) && 
				(f1.getName().equals(f2.getName())));
		}
		
	}
	
	/* language list */
	private CharSequence[] languages = { "Arabic", "Chinese", "French", 
										 "German", "Hebrew", "Hindi", 
										 "Japanese", "Korean", "Marathi", 
										 "Portuguese", "Russian", "Spanish", 
										 "Tamil", "Telugu", "Thai", "Other" };
	
	/* determine if there is only whitespace */
	private boolean empty(String s) {
		return (s.trim().length() == 0);
	}
	
	private void setListClick(ListView fileListView, 
			final FileListAdapter adapter) {
		
		/* File Box and Traversal Interface */
		final EditText file_box = (EditText) findViewById(R.id.the_file);
		final TextView current_path = (TextView)
				findViewById(R.id.file_traverse_current);
		final TextView nothing_here = (TextView) 
				findViewById(R.id.file_traverse_nothing_here);
		final LinearLayout traversal_container = 
				(LinearLayout) findViewById(R.id.file_traverse_container);
		
		fileListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				adapter.clear();
				TextView textView = (TextView) 
						view.findViewById(R.id.label);
				String filePath = textView.getText().toString();
				
				/* Get nav path */
				String navPath = current_path.getText().toString();	
				File parentPath = new File(navPath);
				File thisPath = new File(parentPath, filePath);
				String newPath = thisPath.getAbsolutePath();
				adapter.setParentPath(newPath);
				
				/* Go into folder */
				if (thisPath.isDirectory()) {
					
					/* Set nav bar */
					save_path(newPath);
					current_path.setText(newPath);
					
					ArrayList<CharSequence> newFiles = 
							loadFileList(newPath);
					
					/* What to show */
					if (newFiles.size() > 0) {
						nothing_here.setVisibility(View.GONE);
						for (int i = 0; i < newFiles.size(); i++) {
							CharSequence fileName = newFiles.get(i);
							adapter.add(fileName);
						}
					}
					else {
						nothing_here.setVisibility(View.VISIBLE);
					}
					
				}
				/* Select that file */
				else {
					save_filename(newPath);
					file_box.setText(newPath);
					traversal_container.setVisibility(View.GONE);
				}
				
			}
			
		});
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); // hide title bar
		setContentView(R.layout.cleartalkinputactivity);	
		
		this.prefLib = new PreferencesLibrary(this, pref_name);
	
		/* Changes saved */
		String input_text = load_txt();
		String input_file = load_filename();
		
		/* Buttons */
		Button help = (Button) findViewById(R.id.input_help);		
		Button select_lang = (Button) findViewById(R.id.select_lang);
		Button submit = (Button) findViewById(R.id.submit_input);
		Button output = (Button) findViewById(R.id.cached_output);
		Button textIn = (Button) findViewById(R.id.text_in);
		Button fileIn = (Button) findViewById(R.id.file_in);
		Button back = (Button) findViewById(R.id.file_traverse_back);
		
		/* File Box and Traversal Interface */
		final EditText file_box = (EditText) findViewById(R.id.the_file);
		final TextView current_path = (TextView)
				findViewById(R.id.file_traverse_current);
		final TextView nothing_here = (TextView) 
				findViewById(R.id.file_traverse_nothing_here);
		final LinearLayout traversal_close_bar = 
				(LinearLayout) findViewById(R.id.file_traverse_bottom_bar);
		final LinearLayout traversal_container = 
				(LinearLayout) findViewById(R.id.file_traverse_container);
		
		/* Title Box */
		final TextView title = (TextView) findViewById(R.id.input_title);
		
		/* Edit Box */
		final EditText edit_box = (EditText) findViewById(R.id.the_text);
	
		/* Which input type to show */
		int inputType = load_inputType();
		if (inputType == TEXT_INPUT) {
			edit_box.setVisibility(View.VISIBLE);
			edit_box.setText(input_text);
			title.setText(R.string.text_in_title);
		}
		else {
			file_box.setVisibility(View.VISIBLE);
			file_box.setText(input_file);
			title.setText(R.string.file_in_title);
		}
		
		/* Make file bar appear if clicked */
		fileIn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				String curPath = load_path();				
				
				/* Hide text box, show file traversal portion and file box */
				edit_box.setVisibility(View.GONE);
				traversal_container.setVisibility(View.VISIBLE);
				current_path.setText(curPath);
				file_box.setVisibility(View.VISIBLE);
				file_box.setText(load_filename());
				save_inputType(FILE_INPUT);
				title.setText(R.string.file_in_title);
				
				
				/* Populate file list */
				ArrayList<CharSequence> fileList = loadFileList(curPath);
				
				if (fileList.size() > 0) {
					nothing_here.setVisibility(View.GONE);
					
					ListView fileListView = (ListView) 
							findViewById(R.id.file_list);
					final ArrayAdapter<CharSequence> adapter = new 
							FileListAdapter(ClearTalkInputActivity.this, 
									fileList, curPath);
					fileListView.setAdapter(adapter);
					setListClick(fileListView, (FileListAdapter) adapter);
				
				}
				else {
					nothing_here.setVisibility(View.VISIBLE);
				}
				
			}
		});
		
		/* Navigation back button */
		back.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				
				/* Fix the nav bar */
				String curPath = current_path.getText().toString();
				File curFile = new File(curPath);
				File parent = curFile.getParentFile();
				if (parent == null || !parent.exists()) {
					parent = curFile;
				}
				String parentPath = parent.getAbsolutePath();
	
				current_path.setText(parentPath);
				save_path(parentPath);
				
				/* Reset the list elements */
				ArrayList<CharSequence> files = loadFileList(parentPath);
				if (files.size() > 0) {
					nothing_here.setVisibility(View.GONE);	
					
					ListView fileListView = (ListView) 
							findViewById(R.id.file_list);
					final ArrayAdapter<CharSequence> adapter = new 
							FileListAdapter(ClearTalkInputActivity.this,
									files, parentPath);
					fileListView.setAdapter(adapter);	
					setListClick(fileListView, (FileListAdapter) adapter);
				}
				else {
					nothing_here.setVisibility(View.VISIBLE);
				}
				
			}
			
		});
		
		/* Make file importer disappear if clicked */
		traversal_close_bar.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				traversal_container.setVisibility(View.GONE);				
			}
		});
		
		
		/* Make text box appear if clicked */
		textIn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				edit_box.setVisibility(View.VISIBLE);
				edit_box.setText(load_txt());
				file_box.setVisibility(View.GONE);
				traversal_container.setVisibility(View.GONE);
				save_inputType(TEXT_INPUT);
				title.setText(R.string.text_in_title);
			}
		});
		
		
		/* Help button dialog pop-up */
		help.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(HELP);				
			}			
		});
		
		/* Language selection dialog pop-up */
		select_lang.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(LANGUAGE);				
			}
		});
		
		/* Submit text - right now checks for text and a language submitted */
		submit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				save_txt();
				save_filename();
				String the_text = edit_box.getText().toString();
				String fileName = load_filename();
				boolean textMode = (file_box.getVisibility() == View.GONE);
				int lang = load_lang();
				String message;
				if (lang < 0) {
					if (textMode) {
						if (empty(the_text)) {
							message = "Please enter some text and select a " +
									"language";
						}
						else {
							message = "Please select a language";
						}
					}
					else {
						if (fileName.length() == 0) {
							message = "Please select a file to import and a " +
									"language";
						}
						else {
							message = "Please select a language";
						}
					}
					Toast.makeText(getBaseContext(), message, 
							Toast.LENGTH_SHORT).show();
				}
				else {
					String language = languages[lang].toString().toLowerCase();
					if (textMode) {
						if (empty(the_text)) {
							message = "Please enter some text into the box";
							Toast.makeText(getBaseContext(), message, 
									Toast.LENGTH_SHORT).show();
						}
						else {
							disableAutoOrient();
							Progressor showme = new Progressor(
									ClearTalkInputActivity.this);
							showme.execute(the_text, language);							
						}
					}
					else {
						if (fileName.length() == 0) {
							message = "Please select a file to import";
							Toast.makeText(getBaseContext(), message, 
									Toast.LENGTH_SHORT).show();
						}
						else {
							
							FileInputStream fin;
							try {
								
								fin = new FileInputStream(new File(fileName));
								StringBuilder textBuilder = new StringBuilder();
								int ch;
								while ((ch = fin.read()) != -1) {
									textBuilder.append((char) ch);
								}
								String text = textBuilder.toString();
								
								disableAutoOrient();
								Progressor showme = new Progressor(
										ClearTalkInputActivity.this);
								showme.execute(text, language);
							}
							catch (FileNotFoundException e) {
								message = "Could not find file. Please " +
										"select a file to import";
								Toast.makeText(getBaseContext(), message, 
										Toast.LENGTH_SHORT).show();
								Log.e("file", "couldn't find file");
								enableAutoOrient();
							}
							catch (Exception e) {
								message = "Problem reading the file";
								Toast.makeText(getBaseContext(), message,
										Toast.LENGTH_SHORT).show();
								Log.e("read", "read error");
								enableAutoOrient();
							}
						}
					}
				}
			}
		});
		
		/* Goes right to previously saved output */
		output.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				save_txt();
				startActivity(new Intent ("lti.bicc.cleartalk.OUTPUT"));
			}
		});
		
	}
	
	@Override
	public void onPause() {
		super.onPause();	
		save_txt();
		save_inputType(load_inputType());
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		/* Title Box */
		final TextView title = (TextView) findViewById(R.id.input_title);
		
		/* Text input box */
		final EditText edit_box = (EditText) findViewById(R.id.the_text);
		edit_box.setText(load_txt());
		
		/* File box and Traversal Interface */
		final EditText file_box = (EditText) findViewById(R.id.the_file);
		final LinearLayout traversal_container = 
				(LinearLayout) findViewById(R.id.file_traverse_container);
			
		enableAutoOrient();
		
		/* Which input type to show */
		int inputType = load_inputType();
		if (inputType == TEXT_INPUT) {
			edit_box.setVisibility(View.VISIBLE);
			String the_text = load_txt();
			edit_box.setText(the_text);
			file_box.setVisibility(View.GONE);
			traversal_container.setVisibility(View.GONE);
			title.setText(R.string.text_in_title);
		}
		else {
			edit_box.setVisibility(View.GONE);
			file_box.setVisibility(View.VISIBLE);
			String filename = load_filename();
			file_box.setText(filename);
			title.setText(R.string.file_in_title);
		}
		save_inputType(inputType);
	}
	

	/** 
	 * Update orientation based on set preferences
	 * @param activity
	 */
	public void updateOrientationConfiguration(Activity activity) {
	    if (prefLib.load_bool(ORIENT_KEY)) {
	        activity.setRequestedOrientation(
	        		ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	    } else {
	        if (activity.getResources().getConfiguration().orientation == 
	        		Configuration.ORIENTATION_LANDSCAPE) {
	            activity.setRequestedOrientation(
	            		ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	        }
	        if (activity.getResources().getConfiguration().orientation == 
	        		Configuration.ORIENTATION_PORTRAIT) {
	            activity.setRequestedOrientation(
	            		ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	        }
	    }
	}
	
	/** 
	 * enable auto-orientation
	 */
	public void enableAutoOrient() {
		prefLib.save_bool(ORIENT_KEY, true);
		updateOrientationConfiguration(ClearTalkInputActivity.this);
	}
	
	/** 
	 * disable auto-orientation
	 */
	public void disableAutoOrient() {
		prefLib.save_bool(ORIENT_KEY, false);
		updateOrientationConfiguration(ClearTalkInputActivity.this);
	}
	
	/**
	 * Save the chosen file
	 * @param filename
	 */
	private void save_filename() {
		EditText file_box = (EditText) findViewById(R.id.the_file);
		String the_file = file_box.getText().toString();
		prefLib.save_str(IMPORT_FILE_KEY, the_file);		
	}
	
	/**
	 * Save filename as the chosen file
	 * @param filename
	 */
	private void save_filename(String filename) {
		prefLib.save_str(IMPORT_FILE_KEY, filename);		
	}
	
	/**
	 * Save what's in the text box on the input screen 
	 */
	private void save_txt() {
		EditText edit_box = (EditText) findViewById(R.id.the_text); 
		String the_text = edit_box.getText().toString();
		prefLib.save_str(INPUT_TEXT_KEY, the_text);	
	}
	
	/**
	 * Save the path for file traversal
	 */
	private void save_path(String path) {
		prefLib.save_str(PATH_KEY, path);		
	}
	
	/**
	 * Save which input type should be showing all the time
	 * @param type
	 */
	private void save_inputType(int type) {
		switch (type) {
		case TEXT_INPUT:
			prefLib.save_int(INPUT_TYPE_KEY, TEXT_INPUT);
			return;
			
		case FILE_INPUT:
			prefLib.save_int(INPUT_TYPE_KEY, FILE_INPUT);
			return;
		}
		prefLib.save_int(INPUT_TYPE_KEY, TEXT_INPUT);
	}
	
	/**
	 * Load chosen file otherwise return ""
	 */
	private String load_filename() {
		String filename = prefLib.load_str(IMPORT_FILE_KEY);
		File file = new File(filename);
		if (file.exists() && file.isFile()) {
			return filename;
		}
		else {
			return "";
		}
	}

	
	/**
	 * load saved path from file traversal
	 */
	private String load_path() {
		String savedPath = prefLib.load_str(PATH_KEY);
		File saved = new File(savedPath);
		if (saved.exists() && saved.isDirectory()) {
			//return path;
			return savedPath;
		}
		else {
			return path;
		}
	}
	
	/** 
	 * Load the text for the text box
	 * @return
	 */
	private String load_txt() { 
		return prefLib.load_str(INPUT_TEXT_KEY);
	}
	
	/**
	 * load the index for the language from the languages array
	 * @return
	 */
	private int load_lang() {
		return prefLib.load_int(LANG_KEY);
	}
	
	/**
	 * Get either TEXT_INPUT or FILE_INPUT according to what was saved
	 * @return
	 */
	private int load_inputType() {
		int type = prefLib.load_int(INPUT_TYPE_KEY);
		if (type < 0) {
			return TEXT_INPUT;
		}
		else {
			return type;
		}
	}
		
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case HELP:
			return new AlertDialog.Builder(this)
			.setTitle("How to Use ClearTalk")
			.setMessage(R.string.help_input)
			.setNeutralButton("OK", null)
			.create();
			
		case LANGUAGE:
			return new AlertDialog.Builder(this)
			.setTitle(R.string.select_lang)
			.setNeutralButton("OK", null)
			.setSingleChoiceItems(languages, prefLib.load_int(LANG_KEY), new 
				DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						prefLib.save_int(LANG_KEY, which);						
					}
				})
			.create();
			
		}
		return null;
	}
	
    /**
     *  Initialize file variables 
     *  @param path : the parent folder
     */
	private ArrayList<CharSequence> loadFileList(String path) {
		File base = new File(path);
		ArrayList<CharSequence> result;
		try {
			base.mkdirs();
			if(base.exists()) {
				// List out only files that end in .txt or are directories
				FilenameFilter filter = new FilenameFilter() {
					public boolean accept(File dir, String filename){
						File sel = new File(dir, filename);
						boolean isOkay = hasCorrectType(filename) || 
								sel.isDirectory();
						return isOkay;
					}
				};
				File[] fileList = base.listFiles(filter); 
				Arrays.sort(fileList, new FileListCompare());
				int numFiles = (fileList == null) ? 0 : fileList.length;
				CharSequence[] formatted = mapPathAndStyle(fileList);
				result = new ArrayList<CharSequence>(numFiles);
				for (int i = 0; i < numFiles; i++) {
					result.add(formatted[i]);
				}
			}
			else {
				result = new ArrayList<CharSequence>(0);
			}
			return result;
		}
		catch(SecurityException e) {
			Log.e("Files", "unable to write on the sd card " + e.toString());
			return new ArrayList<CharSequence>(0);
		}	
	}
	
	/**
	 *  Get entire path from a file list, apply style if directory
	 */
 	private CharSequence[] mapPathAndStyle(File[] fileList) {
		if (fileList == null) {
			return new CharSequence[0];
		}
		else {
			CharSequence[] result = new CharSequence[fileList.length];
			for (int i = 0; i < fileList.length; i++) {
				File file = fileList[i];
				String absPath = file.getName();
				if (file.isDirectory()) {
					/* Style directories */
					SpannableString folderPath = new SpannableString(absPath);
					applyStyle(folderPath, R.style.folder);
					result[i] = folderPath;
				}
				else {
					SpannableString filePath = new SpannableString(absPath);
					applyStyle(filePath, R.style.file);
					result[i] = filePath;
				}
			}
			return result;
		}
	}
	
 	/**
 	 * Apply a style to a string based on the resource id
 	 * @param str
 	 * @param id
 	 */
 	private void applyStyle(SpannableString str, int id) {
 		if (str != null) {
	 		TextAppearanceSpan style = new TextAppearanceSpan(
	 				ClearTalkInputActivity.this, id);
	 		int len = str.length();
	 		str.setSpan((Object) style, 0, len, 
	 				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 		}
 	}
 	
	/**
	 * Determine if the filename has the right type
	 * @param filename
	 * @return
	 */
	private boolean hasCorrectType(CharSequence filename) {
		String[] ftypes = { ".txt" }; 
		if (filename == null) {
			return false;
		}
		String fname = filename.toString();
		for (int i = 0; i < ftypes.length; i++) {
			String type = ftypes[i];
			if (fname.toLowerCase().endsWith(type))
				return true;
		}
		return false;
	}
	
	/*
	 * File traversing interface
	 */
	private class FileListAdapter extends ArrayAdapter<CharSequence> {
		private final Context context; // context we're in
		private String parentPath; // path where we are
		private ArrayList<CharSequence> values; // Files and folders
		
		public FileListAdapter(Context context, ArrayList<CharSequence> values, 
				String parentPath) {
			super(context, R.layout.rowlayout, values);
			this.context = context;
			this.values = values;
			this.parentPath = parentPath;
		}
		
		public void setParentPath(String newPath) {
			this.parentPath = newPath;
		}
		
		/*
		 * Get the view associated with a file or a folder (visual element)
		 * (non-Javadoc)
		 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
			TextView textView = (TextView) rowView.findViewById(R.id.label);
			ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
			CharSequence text = values.get(position);
			textView.setText(text);
			
			File parentDir = new File(parentPath);
			File absPath = new File(parentDir, text.toString());			
			
			if (absPath.isDirectory()) {
				imageView.setImageResource(R.drawable.dir);
			}
			else if (absPath.isFile()) {
				imageView.setImageResource(R.drawable.file);
			}
			else {
				imageView.setImageResource(R.drawable.blank);
			}
			return rowView;			
		}
		
	}
	
	/*
	 * Show a loading bar while we're sending the data to the server and 
	 * analyzing the text
	 */
	private class Progressor extends AsyncTask<String, Void, Boolean> {
		ProgressDialog dialog;
		
		Progressor(Activity activity) {
			super();
			this.activity = activity;
		}
		
		Activity activity;
		
		/*
		 * Show the dialog
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		protected void onPreExecute() {
			dialog = ProgressDialog.show(ClearTalkInputActivity.this, 
					"Processing", "Analyzing Text", true, false);
		}
		
		/* 
		 * Actually sending the data to the server
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#Background(Params[])
		 */
		protected Boolean doInBackground(String... strings) {
			String the_text = strings[0];
			String language = strings[1];
			Socket socket = null; 
			DataOutputStream dos = null;
			DataInputStream dis = null;
			Boolean result;
			
			try {
				socket = new Socket("128.2.208.83", 42804); // Socket and port 
				dos = new DataOutputStream(socket.getOutputStream());
				dis = new DataInputStream(socket.getInputStream());
				
				String toSend = language+"****"+the_text;
				byte[] out = toSend.getBytes("UTF-8");
				int outLen = out.length; // number of bytes we want to send
				
				// Check that the server read our int correctly
				dos.writeInt(outLen);
				if (dis.readInt() != outLen) throw new IOException();
				
				// Write the bytes to the server
				dos.write(out, 0, outLen);
				String in;
				StringBuffer sb_in = new StringBuffer();
				
				// Find out how many bytes we're expecting back
				int count = dis.readInt();
				dos.writeInt(count);
				
				// Read that many bytes
				byte[] received = new byte[count];
				int bytesReceived = 0;
				int bytesThisTime = 0;
				while (-1 < bytesReceived && bytesReceived < count) {
					bytesThisTime = dis.read(received, 0, count);
					if (bytesThisTime <= 0) break;
					
					bytesReceived += bytesThisTime;
					String bytesToString = new String(received, 0, 
							bytesThisTime, "UTF-8");
					sb_in.append(bytesToString);
					received = new byte[count];
					
				}
				in = sb_in.toString();
				prefLib.save_str(OUTPUT_KEY, in);
				result = true;				
			} catch (UnknownHostException e) {
				e.printStackTrace();
				result = false;
			} catch (IOException e) {
				e.printStackTrace();
				result = false;
			} catch (Exception e) {
				e.printStackTrace();
				result = false;
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if (dos != null) {
					try {
						dos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if (dis != null) {
					try {
						dis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} 
			}
			return result;
		}
		
		/*
		 * Done processing - go to next screen
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		protected void onPostExecute(Boolean result) {	
			if (dialog.isShowing())
				dialog.dismiss();
			enableAutoOrient();
			if (result)
				startActivity(new Intent ("lti.bicc.cleartalk.OUTPUT"));
			else
				Toast.makeText(activity, "Connection Error", 
						Toast.LENGTH_SHORT).show();
				
		}
		
	}
}

