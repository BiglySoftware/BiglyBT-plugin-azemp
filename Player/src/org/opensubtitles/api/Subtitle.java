package org.opensubtitles.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.swt.internal.image.GIFFileFormat;

public class Subtitle implements Comparable<Subtitle>{
	
	private String downloadLink;
	private String fileExtension;
	private Locale locale;
	
	private float  score;
	private int	   nbDownloads;

	public Subtitle(String downloadLink,String isoCode,String fileExtension) {
		this.downloadLink = downloadLink;
		this.locale = new Locale(isoCode);
		this.fileExtension = fileExtension;
	}

	public String getDownloadLink() {
		return downloadLink;
	}

	public Locale getLocale() {
		return locale;
	}
	
	public String toString() {
		return locale.getDisplayLanguage() + " : " + downloadLink;
 	}

	public String getFileExtenstion() {
		return fileExtension;
	}
	
	public Float getScore() {
		return score;
	}

	public void setScore(String score) {
		try {
			this.score = Float.parseFloat(score);
		} catch (Exception e) {
		}
	}
	
	public void setScore(Float score) {
		this.score = score;
	}
	
	public int getNbDownloads() {
		return nbDownloads;
	}

	public void setNbDownloads(int nbDownloads) {
		this.nbDownloads = nbDownloads;
	}
	
	public void setNbDownloads(String nbDownloads) {
		try {
			this.nbDownloads = Integer.parseInt(nbDownloads);
		} catch (Exception e) {
			
		}
	}
	
	@Override
	public int compareTo(Subtitle o) {
		String language = locale.getDisplayLanguage();
		String otherlanguage = o.getLocale().getDisplayLanguage();
		int comp = language.compareTo(otherlanguage);
		if(comp == 0) {
			return getNbDownloads() - o.getNbDownloads();
		}
		return comp;
	}
	
	public static Subtitle[] removeDuplicates(Subtitle[] list) {
		Map<String,Subtitle> uniques = new HashMap<String,Subtitle>(list.length);
		
		for(Subtitle subtitle : list) {
			String key = subtitle.getLocale().getLanguage();
			Subtitle alreadyThere = uniques.get(key);
			if(alreadyThere == null || alreadyThere.getNbDownloads() < subtitle.getNbDownloads()) {
				uniques.put(key, subtitle);
			}
		}
		
		List<Subtitle> results = new ArrayList<Subtitle>(uniques.keySet().size());
		for(String key : uniques.keySet()) {
			results.add(uniques.get(key));
		}
		
		return results.toArray(new Subtitle[results.size()]);
	}

}
