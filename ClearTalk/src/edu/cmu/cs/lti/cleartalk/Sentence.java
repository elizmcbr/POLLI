/** 
 * @author Elizabeth
 * 
 * Interface to sentences 
 * Invariants:
 * words joined together by " " would be original
 * length == words.length
 * num_min_pairs == conf_indices.length == min_pairs.length
 */

package edu.cmu.cs.lti.cleartalk;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;

public class Sentence {
	
	Sentence (String original, String[] words, int[] conf_indices, 
			ConfusionInstance[] min_pairs) {
		this.original = original;
		this.words = words;
		this.conf_indices = conf_indices;
		this.min_pairs = min_pairs;
		
		if (min_pairs == null || min_pairs.length == 0)
			this.min_mp_id = -1;
		else
			this.min_mp_id = min_pairs[0].getMpId();
		
		if (min_pairs == null || min_pairs.length == 0)
			this.max_mp_id = -1;
		else
			this.max_mp_id = min_pairs[min_pairs.length-1].getMpId();
	}

	Sentence(String[] words, int[] conf_indices, 
			ConfusionInstance[] min_pairs) {
		this.words = words;
		this.conf_indices = conf_indices;
		this.min_pairs = min_pairs;
		this.original = makeOriginal(words);
		
		if (min_pairs == null || min_pairs.length == 0)
			this.min_mp_id = -1;
		else
			this.min_mp_id = min_pairs[0].getMpId();
		
		if (min_pairs == null || min_pairs.length == 0)
			this.max_mp_id = -1;
		else
			this.max_mp_id = min_pairs[min_pairs.length-1].getMpId();
		
	}
	
	private int min_mp_id;
	private int max_mp_id;
	
	private String original; // Sentence in string form
	public String getOriginal() {
		return original;
	}
	
	private String[] words; //Sentence separated into words
	public String[] getWords() {
		return words;
	}
	
	public int length() {
		return words.length;
	} // Number of words in the sentence
	
	public int num_mp() {
		if (conf_indices != null)
			return conf_indices.length;
		else 
			return 0;
	} // Number of minimal pairs contained in sentence
	
	private int[] conf_indices; // Indices of min pairs
	public int[] getConfIndices() {
		return conf_indices;
	}
	
	private ConfusionInstance[] min_pairs; // Actual minimal pairs occurring
	public ConfusionInstance[] getMinPairs() {
		return min_pairs;
	}
	
	public int getMinMP() {
		return min_mp_id;
	}
	
	public int getMaxMP() {
		return max_mp_id;
	}
	
	/**
	 * Get the sentence as a SpannableStringBuilder with the MPs styled for 
	 * all of the minimal pairs
	 * @param style - style to apply
	 * @return
	 */
	public SpannableStringBuilder getStyledOriginal(Object style) {
		return styleWords(words, style, conf_indices);
	}
	
	/**
	 * Get the sentence as a SpannableStringBuilder with the MPs styled for
	 * all indices in indices[]
	 * @param style
	 * @param indices
	 * @return
	 */
	public SpannableStringBuilder getStyledOriginal(Object style, int[] indices) {
		return styleWords(words, style, indices);		
	}

	/**
	 * Get sentence as a SpannableStringBuilder with the MP at index styled
	 * @param style
	 * @param index
	 * @return
	 */
	public SpannableStringBuilder getStyledOriginal(Object style, int index) {
		int[] indices = { index };
		return styleWords(words, style, indices);		
	}
	
	/**
	 * Make the sentence where we know there are no MPs
	 * @param words - words in the sentence
	 * @param spaces - booleans to indicate spaces between words
	 * @return SpannableStringBuilder containing original sentence
	 */
	private SpannableStringBuilder styleNoMP(String[] words, boolean[] spaces) {
		SpannableStringBuilder original = new SpannableStringBuilder("");
		for (int i = 0; i < words.length; i++) {
			original.append(words[i]);
			if (i < spaces.length && spaces[i])
				original.append(" ");
		}
		return original;
	}
	
	/**
	 * Make the sentence where we know there are MPs
	 * @param words - words in the sentence
	 * @param spaces - booleans to indicate spaces between words
	 * @param style - style to apply to the MPs
	 * @param indices - indices that are MPs
	 * @return
	 */
	private SpannableStringBuilder styleMP(String[] words, boolean[] spaces, 
			Object style, int[] indices) {
		SpannableStringBuilder original = new SpannableStringBuilder("");
		int indicesIdx = 0;
		for (int i = 0; i < words.length; i++) {
			boolean isMP = false;
			if (indicesIdx < indices.length && i == indices[indicesIdx]) {
				isMP = true;
				indicesIdx++;
			}
			SpannableString word = new SpannableString(words[i]);
			if (isMP) {
				word.setSpan(style, 0, words[i].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			original.append(word);
			if (i < spaces.length && spaces[i]) 
				original.append(" ");	
		}
		
		return original;
	}
	
	/**
	 * Apply style to words in a sentence
	 * @param words
	 * @param style
	 * @param indices
	 * @return
	 */
	private SpannableStringBuilder styleWords(String[] words, Object style,
			int[] indices) {
		int numWords = words.length;

		boolean[] spaces = new boolean[numWords-1];
		for (int i = 1; i < numWords; i++) {
			String now = words[i];
			if (now.startsWith(",") || now.startsWith(";") || 
				now.startsWith(":") || now.startsWith(".")) {
				spaces[i-1] = false;				
			}
			else if (now.startsWith("-") && now.length() > 1) {
				spaces[i-1] = false;
			}
			else {
				spaces[i-1] = true;
			}
		}
		
		if (indices == null || indices.length == 0)
			return styleNoMP(words, spaces);	
		else
			return styleMP(words, spaces, style, indices);
		
	}
	
	/**
	 * Take the word array and make a string out of it
	 */
	private String makeOriginal(String[] words) {
		return styleWords(words, null, null).toString();
	}
	
	
}
