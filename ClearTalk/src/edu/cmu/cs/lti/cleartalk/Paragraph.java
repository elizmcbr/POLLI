/**
 * @author Elizabeth Davis
 * copyright 2012
 * 
 * Interface to paragraphs
 * 
 * Invariants:
 * sentences.length == num_sents
 */

package edu.cmu.cs.lti.cleartalk;

public class Paragraph {
	
	Paragraph (Sentence[] sentences, int num_spaces) {
		this.sentences = sentences;
		this.num_spaces = num_spaces;
		
		/* Set min mp */
		if (sentences == null || sentences.length == 0)
			this.minMP = -1;
		else {
			int min = -1;
			int i = 0;
			while (min < 0 && i < sentences.length) {
				min = sentences[i].getMinMP();
				i++;
			}
			this.minMP = min;
		}	
		
		/* Set max mp */
		if (sentences == null || sentences.length == 0)
			this.maxMP = -1;
		else {
			int max = -1;
			int i = sentences.length - 1;
			while (max < 0 && i > -1) {
				max = sentences[i].getMaxMP();
				i--;
			}
			this.maxMP = max;
		}
			
	}
	
	Paragraph (Sentence[] sentences, String num_spaces) {
		this.sentences = sentences;
		this.num_spaces = Integer.parseInt(num_spaces);
		
		/* Set min mp */
		if (sentences == null || sentences.length == 0)
			this.minMP = -1;
		else {
			int min = -1;
			int i = 0;
			while (min < 0 && i < sentences.length) {
				min = sentences[i].getMinMP();
				i++;
			}
			this.minMP = min;
		}	
		
		/* Set max mp */
		if (sentences == null || sentences.length == 0)
			this.maxMP = -1;
		else {
			int max = -1;
			int i = sentences.length - 1;
			while (max < 0 && i > -1) {
				max = sentences[i].getMaxMP();
				i--;
			}
			this.maxMP = max;
		}
	}
	
	private int minMP;
	private int maxMP;
	
	/**
	 * @return first minpair id
	 */
	public int minMP() {
		return this.minMP;
	}
	
	/**
	 * @return last minpair id
	 */
	public int maxMP() {
		return this.maxMP;
	}
	
	private Sentence[] sentences; // Sentences in paragraph
	public Sentence[] getSentences() {
		return sentences;
	}
	
	public int num_sents() {
		return sentences.length;
	} // Number of sentences in paragraph
	
	private int num_spaces; // Number of spaces after paragraph
	public int getNumSpaces() {
		return num_spaces;
	}
	
	public String originalForm() {
		StringBuilder para = new StringBuilder();
		for (Sentence s : sentences) {
			para.append(s.getOriginal()+" ");
		}
		for (int i = 0; i < num_spaces; i++)
			para.append("\n");
		return para.toString();
	}

}
