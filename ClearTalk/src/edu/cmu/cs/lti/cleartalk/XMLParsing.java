/**
 * @author Elizabeth Davis
 * Copyright 2012
 * 
 * A class to parse the XML that encodes the BICC analysis
 * Each instance has the following attributes:
 * Paragraphs[] paragraphs
 * ConfusionInstance[] confusions
 *
 */

package edu.cmu.cs.lti.cleartalk;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

public class XMLParsing {
	
	XMLParsing(String s) {
		Document dom = parseXmlString(s);
		parseCTDocument(dom);
	}
	
	private Paragraph[] paragraphs;	
	public Paragraph[] getParagraphs() {
		return this.paragraphs;
	}
	
	private ConfusionInstance[] confusions;
	public ConfusionInstance[] getConfusions() {
		return this.confusions;
	}	
	
	/** 
	 * Turn the document into a string that can be parsed
	 */
	private Document parseXmlString(String s){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(new InputSource(new StringReader(s)));
			return dom;
		} catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch(SAXException se) {
			se.printStackTrace();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}
	
	/**
	 * parse BICC xml document, set Paragraphs and Confusions for this instance
	 */
	private void parseCTDocument(Document dom) {
		//get the root element
		Element docEle = dom.getDocumentElement();

		//get a node list of  elements
		NodeList paras = docEle.getElementsByTagName("para");
		NodeList confIs = docEle.getElementsByTagName("confInstance");
		
		int numMPs = (confIs != null) ? confIs.getLength() : 0;
		int numParas = (paras != null) ? paras.getLength() : 0;
		
		// ConfusionInstance objects in cis will be indexed by mp-id
		ConfusionInstance[] cis = new ConfusionInstance[numMPs];
		Paragraph[] paragraphs = new Paragraph[numParas];		
		
		// Get all the ConfusionInstance objects encoded in the xml
		for (int i = 0; i < numMPs; i++) {
			Element conf_inst = (Element) confIs.item(i);
			cis[i] = getConfInst(conf_inst);				
		}
		// Set the instance variable
		this.confusions = cis;
		
		// Get all the Paragrah objects encoded in the xml
		for (int i = 0; i < numParas; i++) {
			Element para = (Element) paras.item(i);
			if (para == null) {
				Log.d("para", "this element is null");
			}
			int num_sents = getIntAttr(para, "sents");
			int num_spaces = getIntAttr(para, "seps");
			
			Sentence[] sentences = new Sentence[num_sents];
			NodeList sents = para.getChildNodes();
			for (int j = 0; j < num_sents; j++) {
				Element sent = (Element) sents.item(j);
				sentences[j] = getSentence(sent, cis);
			}
			paragraphs[i] = new Paragraph(sentences, num_spaces);				
		}
		// Set the instance variable
		this.paragraphs = paragraphs;
		
	}
	
	/**
	 * Take confusion instance element, read in values, return 
	 * ConfusionInstance object
	 */
	private ConfusionInstance getConfInst(Element confEl) {
		
		String original = getTextValue(confEl, "conf0");
		String mp_id = confEl.getAttribute("mp-id");
		NodeList confs = confEl.getChildNodes();
		
		int numConf = (confs != null) ? confs.getLength() : 0;
		
		String[] vars = (numConf > 0) ? new String[numConf-1] : new String[0];
		for (int i = 1; i < numConf; i++) {
			vars[i-1] = getTextValue(confEl, "conf"+i);			
		}
		
		return new ConfusionInstance(original, mp_id, vars);	
		
	}

	/**
	 * Takes in sentence element and the ConfusionInstance array and create a 
	 * Sentence instance with the minimal pairs included 
	 */
	private Sentence getSentence(Element sentEl, ConfusionInstance[] cis) {
		String[] words;
		ConfusionInstance[] confs;
		int[] mpIdx; // The mp-ids associated with the internal confusions
		int[] mpIds; // The indices that each confusion occurs in
		ArrayList<String> wordAL; // The words in the sentence
		
		// We'll get a combination of plain text & marked up minimal pair nodes
		NodeList sentNodes = sentEl.getChildNodes();
		NodeList mpNodes = sentEl.getElementsByTagName("mp");
		int mpLen = (mpNodes != null) ? mpNodes.getLength() : 0;
		int numFrags = (sentNodes != null) ? sentNodes.getLength() : 0;
		
		
		mpIds = new int[mpLen];
		mpIdx = new int[mpLen];
		wordAL = new ArrayList<String>(numFrags); 
		
		int mpi = 0;
		
		// Get the words in the sentence
		for (int i = 0; i < numFrags; i++) {
			boolean mp = false; // Haven't seen mp label yet
			Node piece = sentNodes.item(i);
			String name = piece.getNodeName();
			if (name.equals("mp")) {
				mp = true;
				int id = getIntAttr((Element) piece, "mp-id");
				mpIds[mpi] = id;
				mpIdx[mpi] = wordAL.size();
				mpi++;
			}

			// Separate the words inside the fragment
			NodeList frags = ((Element) piece).getChildNodes();
			StringBuilder fragment = new StringBuilder();
			for (int j = 0; j < frags.getLength(); j++) {
				fragment.append(frags.item(j).getNodeValue());
			}
			String frag = fragment.toString();
			getSpacing(frag, wordAL, mp);
		}
		words = wordAL.toArray(new String[0]);	
		confs = getConfs(mpIds, cis);
		
		return new Sentence(words, mpIdx, confs);		
	}
	
	/**
	 * Take id indexing array and master array, filling out the indexed 
	 * ConfusionInstances
	 */
	private ConfusionInstance[] getConfs(int[] mpIds, ConfusionInstance[] cis) {
		int numConfs = mpIds.length;
		ConfusionInstance[] confs = new ConfusionInstance[numConfs];
		for (int i = 0; i < numConfs; i++) {
			confs[i] = cis[mpIds[i]];
		}
		return confs;
	}
	
	/**
	 * Take the "sentence" and split it around " " characters.
	 */
	private void getSpacing(String frag, ArrayList<String> al, boolean mp) {
		String[] spaces;
		if (mp) {
			spaces = new String[1];
			spaces[0] = frag;
		}
		else
			spaces = frag.split(" ");
		for (String s : spaces) {
			if (s.length() > 0) {
				al.add(s);
			}
		}
	}
	
	/**
	 * Take an xml element and tag name, look for tag, get text content
	 * getTextValue(<employee><name>John</name></employee>, "name") ==> John
	 */
	private String getTextValue(Element ele, String tagName) {
		String textVal = null;		
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			NodeList frags = el.getChildNodes();
			StringBuilder fragment = new StringBuilder();
			for (int i = 0; i < frags.getLength(); i++) {
				fragment.append(frags.item(i).getNodeValue());
			}
			textVal = fragment.toString();
		}
		return textVal;
	}

	/**
	 * Take an xml element and attribute name, get the value stored for the
	 * attribute, and parse it as an int.
	 * getIntAttr(<employee age="50"><name>Joe</name></employee>, "age") ==> 50
	 * 
	 */
	private int getIntAttr(Element ele, String attrName) {
		String attr = ele.getAttribute(attrName);
		return Integer.parseInt(attr);
	}	
	
}
