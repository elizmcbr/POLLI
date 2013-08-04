/**
 * @author Elizabeth Davis
 * copyright 2012
 * 
 * Interface to minimal pairs
 * Each ConfusionInstance has an original form, an ID, and a set of variations
 * 
 */

package edu.cmu.cs.lti.cleartalk;


import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;

public class ConfusionInstance {
	
	ConfusionInstance(String original, int mp_id, String[] variations) {
		this.original = original;
		this.mp_id = mp_id;
		this.variations = variations;
	}
	
	ConfusionInstance(String original, String mp_id, String[] variations) {
		this.original = original;
		this.mp_id = Integer.parseInt(mp_id);
		this.variations = variations;
	}
	
	private String original; // word from original sentence
	public String getOriginal() {
		return original;
	}
	
	private int mp_id; // min_pair id for matching
	public int getMpId() {
		return mp_id;
	}
	
	private String[] variations; // minimal pairs to original
	public String[] getVariations() {
		return variations;
	}
	
	public int num_vars() {
		return variations.length;
	} // number of variations	
	
	private String varString() {
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < variations.length; i++)
			res.append(variations[i]+" ");
		return res.toString();
	}
	
	public String toString() {
		return "Original: " + this.original + "\n" + "Variations: " + varString();
	}

	/**
	 * Determine if confusion is in a paragraph array
	 */
	private boolean is_in(Paragraph[] paras) {
		for (int i = 0; i < paras.length; i++) {
			if (is_in(paras[i])) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determine if confusion is in a paragraph
	 * @param para
	 * @return
	 */
	private boolean is_in(Paragraph para) {
		if (para == null)
			return false;
		else {
			int min = para.minMP();
			int max = para.maxMP();
			if (min == -1 || max == -1)
				return false;
			else if (mp_id < min)
				return false;
			else if (mp_id > max)
				return false;
			else
				return true;
		}
	}
	
	/**
	 * Determine if confusion is in a sentence
	 * @param sent
	 * @return
	 */
	private boolean is_in(Sentence sent) {
		if (sent == null)
			return false;
		else {
			int min = sent.getMinMP();
			int max = sent.getMaxMP();
			if (min == -1 || max == -1)
				return false;
			if (mp_id < min)
				return false;
			else if (mp_id > max)
				return false;
			else
				return true;			
		}
	}
	
	/**
	 * get the sentence that contains the confusion instance
	 * @param para
	 * @return
	 */
	public Sentence getSentence(Paragraph para) {
		if (para == null)
			return null;
		if (!is_in(para))
			return null;
		Sentence[] sample = para.getSentences();
		if (sample == null)
			return null;
		for (int i = 0; i < sample.length; i++) {
			Sentence s = sample[i];
			if (is_in(s))
				return s;
		}
		return null;		
	}
	
	/** 
	 * get the sentence that contains the confusion instance
	 * @param paras
	 * @return
	 */
	public Sentence getSentence(Paragraph[] paras) {
		if (paras == null)
			return null;
		else if (paras.length == 0)
			return null;
		else if (!is_in(paras)) 
			return null;
		else {
			for (int i = 0; i < paras.length; i++) {
				Paragraph p = paras[i];
				if (is_in(p))
					return getSentence(p);
			}
		}
		return null;
	}
	
	/**
	 * this needs to be a minimal pair in the argument sentence 
	 */
	public int getIndex(Sentence sent) {
		ConfusionInstance[] confs = sent.getMinPairs();
		int[] indices = sent.getConfIndices();
		for (int i = 0; i < confs.length; i++) {
			ConfusionInstance conf = confs[i];
			if (conf.getMpId() == mp_id)
				return indices[i];
		}
		return -1;
	}
	
	public SpannableStringBuilder getConfPhrase(Context ctx, int id) {
		Object styleOrig = (Object) new TextAppearanceSpan(ctx, id);
		SpannableStringBuilder phrase = new 
				SpannableStringBuilder("Listeners might hear ");
		/*SpannableString orig = new SpannableString(original);
		orig.setSpan(styleOrig, 0, orig.length(), 
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		phrase.append(orig);
		phrase.append(" for ");*/
		int varLen = variations.length;
		if (varLen == 1) {
			String v = variations[0];
			SpannableString var = new SpannableString(v);
			Object styleVar = (Object) new TextAppearanceSpan(ctx, id);
			var.setSpan(styleVar, 0, v.length(), 
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			phrase.append(var);
		}
		else if (varLen == 2) {
			String v1 = variations[0];
			String v2 = variations[1];
			SpannableString var1 = new SpannableString(v1);
			Object styleVar1 = (Object) new TextAppearanceSpan(ctx, id);
			var1.setSpan(styleVar1, 0, v1.length(), 
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			SpannableString var2 = new SpannableString(v2);
			Object styleVar2 = (Object) new TextAppearanceSpan(ctx, id);
			var2.setSpan(styleVar2, 0, v2.length(), 
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			phrase.append(var1);
			phrase.append(" or ");
			phrase.append(var2);
		}
		else {
			for (int i = 0; i < variations.length; i++) {
				String v = variations[i];
				SpannableString var = new SpannableString(v);
				Object styleVar = (Object) new TextAppearanceSpan(ctx, id);
				var.setSpan(styleVar, 0, v.length(), 
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				phrase.append(var);
				if (i < variations.length - 1) {
					phrase.append(", or ");
				}			
			}
		}	
		phrase.append(" instead");
		return phrase;
	}
	
}
