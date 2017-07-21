package org.opensubtitles.api;

public class SubtitleMonitor {
	
	String videoFile;	 
	float videoDuration;
	
	String subtitleFile;
	
	float playedFor;
	float lastPlayerPosition;
	
	boolean alreadyReported;
	
	public SubtitleMonitor(String videoFile) {
		this.videoFile = videoFile;
		alreadyReported = false;
	}
	
	public void setVideoDuration(float duration) {
		this.videoDuration = duration;
	}
	
	public void subtitleSet(String subtitleFile) {
		this.subtitleFile = subtitleFile;
		playedFor = 0;
	}
	
	public synchronized void playerPosition(float time) {
		if(alreadyReported) return;
		if(subtitleFile == null) return;
		
		float delta = time - lastPlayerPosition;
		if(delta > 0 && delta < 5) {
			playedFor += delta;
			if(playedFor > .8 * videoDuration) {
				reportSubtitle();
			}
		}
		lastPlayerPosition = time;
	}
	
	private void reportSubtitle() {
		if(alreadyReported) return;
		alreadyReported = true;
		final String subtitleFileName  = subtitleFile;
		final String videoFileName = videoFile;
		Thread uploader = new Thread("Sutitle uploader") {
			@Override
			public void run() {
				OpenSubtitlesAPI.upload(subtitleFileName, videoFileName);
			};
		};
		uploader.setDaemon(true);
		uploader.start();
		
	}
	
	

}
