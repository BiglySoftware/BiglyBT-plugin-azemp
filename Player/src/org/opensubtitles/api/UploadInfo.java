package org.opensubtitles.api;

public class UploadInfo {
	
	boolean shouldUpload = false;
	String imdbId  = null;
	
	public boolean isShouldUpload() {
		return shouldUpload;
	}
	public void setShouldUpload(boolean shouldUpload) {
		this.shouldUpload = shouldUpload;
	}
	public String getImdbId() {
		return imdbId;
	}
	public void setImdbId(String imdbId) {
		this.imdbId = imdbId;
	}
	
	

}
