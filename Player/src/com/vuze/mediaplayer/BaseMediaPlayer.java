package com.vuze.mediaplayer;

import java.util.ArrayList;
import java.util.List;

import org.opensubtitles.api.OpenSubtitlesAPI;
import org.opensubtitles.api.SubtitleMonitor;

public abstract class BaseMediaPlayer implements MediaPlayer,MetaDataListener,StateListener,VolumeListener,PositionListener,TaskListener {

	private List<MetaDataListener> metaDataListeners;
	private List<StateListener> stateListeners;
	private List<VolumeListener> volumeListeners;
	private List<PositionListener> positionListeners;
	private List<TaskListener> taskListeners;
	
	private MediaPlaybackState 	currentState;	
	private int					currentVolume;
	private int					videoWidth;
	private int					videoHeight;
	private int					displayWidth;
	private int					displayHeight;
	private float				currentPositionInSecs;
	private float				durationInSecs;
	private boolean				durationInSecsSet;
	
	private List<Language>		audioTracks;
	private List<Language>		subtitles;
	
	private String				activeAudioTrackId = "0";
	private LanguageSource		activeSubtitleSource = null;
	private String				activeSubtitleId = null;
	
	private String 				openedFile;
	
	private SubtitleMonitor		subtitleMonitor;
	
	protected PlayerPreferences preferences;
	
	public BaseMediaPlayer(PlayerPreferences preferences) {
		this.preferences = preferences;
		
		metaDataListeners = new ArrayList<MetaDataListener>(1);
		stateListeners = new ArrayList<StateListener>(1);
		volumeListeners = new ArrayList<VolumeListener>(1);
		positionListeners = new ArrayList<PositionListener>(1);
		taskListeners = new ArrayList<TaskListener>(1);
		
		initialize();
		
		setMetaDataListener(this);
		setStateListener(this);
		setVolumeListener(this);
		setPositionListener(this);
		
	}

	private void initialize() {
		openedFile = null;
		
		audioTracks = new ArrayList<Language>();
		subtitles = new ArrayList<Language>();
		
		activeAudioTrackId = "0";
		activeSubtitleId = null;
		activeSubtitleSource = null;
				
		durationInSecs 			= 0;
		durationInSecsSet		= false;
		currentPositionInSecs	= 0;
		
		currentState = MediaPlaybackState.Uninitialized;
	}
	
	
	public PlayerPreferences
	getPreferences()
	{
		return( preferences );
	}
	
	@Override
	public void addMetaDataListener(MetaDataListener listener) {
		synchronized (metaDataListeners) {
			metaDataListeners.add(listener);
		}
		
	}

	
	@Override
	public void addStateListener(StateListener listener) {
		synchronized (stateListeners) {
			stateListeners.add(listener);
		}
		
	}

	
	@Override
	public void addVolumeListener(VolumeListener listener) {
		synchronized (volumeListeners) {
			volumeListeners.add(listener);
		}
		
	}
	
	
	@Override
	public void addPositionListener(PositionListener listener) {
		synchronized (positionListeners) {
			positionListeners.add(listener);
		}
		
	}
	
	public abstract void setStateListener(StateListener listener);
	public abstract void setVolumeListener(VolumeListener listener);
	public abstract void setMetaDataListener(MetaDataListener listener);
	public abstract void setPositionListener(PositionListener listener);
	public abstract void doOpen(String fileOrUrl);
	public abstract void doPause();
	public abstract void doResume();
	public abstract void doStop();
	public abstract void doRedraw();
	public abstract void doSeek(float timeInSecs);
	public abstract boolean canSeekAhead();
	public abstract void doSetVolume(int volume);	
	public abstract void doLoadSubtitlesFile(String file,boolean autoPlay);
	public abstract void dispose();
	
	public void doLoadSubtitlesFile(String file) {
		doLoadSubtitlesFile(file, true);
	}
	
	@Override
	public void open(String fileOrUrl) {
		if(currentState == MediaPlaybackState.Uninitialized || currentState == MediaPlaybackState.Stopped) {
			openedFile = fileOrUrl;
			subtitleMonitor = new SubtitleMonitor(fileOrUrl);
			doOpen(fileOrUrl);
		} else {
			doStop();
			initialize();
			openedFile = fileOrUrl;
			subtitleMonitor = new SubtitleMonitor(fileOrUrl);
			doOpen(fileOrUrl);
		}
		
		//Finds subtitles
		String[] subtitles = OpenSubtitlesAPI.getLocalSubtitlesForFile(fileOrUrl);
		for(String subtitle:subtitles) {
			doLoadSubtitlesFile(subtitle,false);
		}
	}

	@Override
	public String getOpenedFile() {
		return openedFile;
	}
	
	@Override
	public void loadSubtitlesFile(String file) {
		doLoadSubtitlesFile(file);
	}
	
	@Override
	public void pause() {
		if(currentState == MediaPlaybackState.Playing) {
			doPause();
		}		
	}

	@Override
	public void play() {
		if(currentState == MediaPlaybackState.Paused) {
			doResume();
		}
		
	}
	
	@Override
	public void togglePause() {
		if(currentState == MediaPlaybackState.Paused) {
			doResume();
		} else
		if(currentState == MediaPlaybackState.Playing) {
			doPause();
		}
	}

	@Override
	public void removeMetaDataListener(MetaDataListener listener) {
		synchronized (metaDataListeners) {
			while(metaDataListeners.contains(listener)) {
				metaDataListeners.remove(listener);
			}
		}
		
	}

	@Override
	public void removeStateListener(StateListener listener) {
		synchronized (stateListeners) {
			while(stateListeners.contains(listener)) {
				stateListeners.remove(listener);
			}
		}
		
	}

	
	@Override
	public void removeVolumeListener(VolumeListener listener) {
		synchronized (volumeListeners) {
			while(volumeListeners.contains(listener)) {
				volumeListeners.remove(listener);
			}
		}
		
	}
	
	
	@Override
	public void removePositionListener(PositionListener listener) {
		synchronized (positionListeners) {
			while(positionListeners.contains(listener)) {
				positionListeners.remove(listener);
			}
		}
		
	}
	
	@Override
	public synchronized void seek(float timeInSecs) {
		if(timeInSecs < 0) timeInSecs = 0;
		if(timeInSecs > durationInSecs) timeInSecs = durationInSecs;
		
		if(currentState == MediaPlaybackState.Playing || currentState == MediaPlaybackState.Paused) {			
			doSeek(timeInSecs);
		}
		
	}

	
	@Override
	public void setVolume(int volume) {
		if(currentState == MediaPlaybackState.Playing || currentState == MediaPlaybackState.Paused) {
			doSetVolume(volume);
		}
	}
	
	
	@Override
	public void receivedDisplayResolution(int width, int height) {
		displayWidth = width;
		displayHeight = height;
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.receivedDisplayResolution(width,height);
			}
		}
	}
	
	
	@Override
	public void receivedDuration(float durationInSecs) {
		durationInSecsSet = true;
		this.durationInSecs = durationInSecs;
		if(subtitleMonitor != null) {
			subtitleMonitor.setVideoDuration(durationInSecs);
		}
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.receivedDuration(durationInSecs);
			}
		}
	}
	
	
	@Override
	public void receivedVideoResolution(int width, int height) {
		videoWidth = width;
		videoHeight = height;
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.receivedVideoResolution(width, height);
			}
		}
	}
	
	
	@Override
	public void foundAudioTrack(Language language) {
		synchronized (audioTracks) {
			audioTracks.add(language);
		}		
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.foundAudioTrack(language);
			}
		}
	}
	
	
	@Override
	public void foundSubtitle(Language language) {
		synchronized (subtitles) {
			subtitles.add(language);
		}
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.foundSubtitle(language);
			}
		}
	}
	
	
	@Override
	public void activeAudioTrackChanged(String audioTrackId) {
		activeAudioTrackId = audioTrackId;
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.activeAudioTrackChanged(audioTrackId);
			}
		}
	}
	
	
	@Override
	public void activeSubtitleChanged(String subtitleId, LanguageSource source) {
		//System.out.println(subtitleId + " " + source);
		activeSubtitleId = subtitleId;
		activeSubtitleSource = source;
		if(subtitleMonitor != null) {
			Language subtitle = getSubtitleByIdAndSource(subtitleId,source);
			if(subtitle != null && subtitle.source == LanguageSource.FILE) {
				subtitleMonitor.subtitleSet(subtitle.getSourceInfo());
			} else {
				subtitleMonitor.subtitleSet(null);
			}
		}
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.activeSubtitleChanged(subtitleId,source);
			}
		}
	}
	
	
	public Language getSubtitleByIdAndSource(String subtitleId,LanguageSource source) {
		if(subtitleId == null) return null;
		synchronized (subtitles) {
			for(Language l : subtitles) {
				if(l.id != null && l.id.equals(subtitleId) && l.source == source) {
					return l;
				}
			}
			return null;
		}
		
	}




	@Override
	public void stateChanged(MediaPlaybackState newState) {
		currentState = newState;
		synchronized (stateListeners) {
			for(StateListener listener : stateListeners) {
				listener.stateChanged( newState);
			}
		}
		
		
	}
	
	
	@Override
	public void volumeChanged(int newVolume) {
		currentVolume = newVolume;
		synchronized (volumeListeners) {
			for(VolumeListener listener : volumeListeners) {
				listener.volumeChanged(newVolume);
			}			
		}		
	}
	
	
	@Override
	public void positionChanged(float currentTimeInSecs) {
		if(currentPositionInSecs != currentTimeInSecs) {
			if(subtitleMonitor != null) {
				subtitleMonitor.playerPosition(currentTimeInSecs);
			}
			currentPositionInSecs = currentTimeInSecs;
			synchronized (positionListeners) {
				for(PositionListener listener : positionListeners) {
					listener.positionChanged(currentTimeInSecs);
				}			
			}
		}
	}
	
	
	@Override
	public void addTaskListener(TaskListener listener) {
		synchronized (taskListeners) {
			taskListeners.add(listener);
		}		
	}
	
	
	@Override
	public void removeTaskListener(TaskListener listener) {
		synchronized (taskListeners) {
			while(taskListeners.contains(listener)) {
				taskListeners.remove(listener);
			}
		}
		
	}
	
	
	@Override
	public void taskStarted(String taskName) {
		synchronized (taskListeners) {
			for(TaskListener listener : taskListeners) {
				listener.taskStarted(taskName);
			}
		}
		
	}
	
	
	@Override
	public void taskProgress(String taskName, int percent) {
		synchronized (taskListeners) {
			for(TaskListener listener : taskListeners) {
				listener.taskProgress(taskName,percent);
			}
		}
		
	}
	
	
	@Override
	public void taskEnded(String taskName) {
		synchronized (taskListeners) {
			for(TaskListener listener : taskListeners) {
				listener.taskEnded(taskName);
			}
		}
	}
	
	
	
	
	@Override
	public void stop() {
		if(currentState == MediaPlaybackState.Playing || currentState == MediaPlaybackState.Paused) {
			doStop();
		}		
	}



	@Override
	public MediaPlaybackState getCurrentState() {
		return currentState;
	}



	@Override
	public int getVolume() {
		return currentVolume;
	}



	@Override
	public int getVideoWidth() {
		return videoWidth;
	}



	@Override
	public int getVideoHeight() {
		return videoHeight;
	}



	@Override
	public int getDisplayWidth() {
		return displayWidth;
	}



	@Override
	public int getDisplayHeight() {
		return displayHeight;
	}


	@Override
	public float getPositionInSecs() {
		return currentPositionInSecs;
	}

	public void
	clearDurationInSecs()
	{
		durationInSecs 		= 0;
		durationInSecsSet	= false;
	}
	
	public void
	setDurationInSecs(
		float		secs )
	{
		// this is a hint in case we don't receive a real value, which sometimes happens during streaming :(
		
		if ( !durationInSecsSet ){
			
			durationInSecs = secs;
		}
	}
	
	@Override
	public float getDurationInSecs() {
		return durationInSecs;
	}
	
	
	@Override
	public Language[] getAudioTracks() {
		List<Language> result = new ArrayList<Language>();
		synchronized (audioTracks) {			
			for(Language language : audioTracks) {
				result.add(language);			
			}			
		}
		return result.toArray(new Language[result.size()]);
	}
	
	
	@Override
	public Language[] getSubtitles() {
		List<Language> result = new ArrayList<Language>();		
		synchronized (subtitles) {			
			for(Language language : subtitles) {
				result.add(language);			
			}			
		}
		return result.toArray(new Language[result.size()]);
	}
	
	
	@Override
	public String getActiveAudioTrackId() {
		return activeAudioTrackId;
	}
	
	
	@Override
	public String getActiveSubtitleId() {
		return activeSubtitleId;
	}
	
	public Language getActiveSubtitle() {
		return getSubtitleByIdAndSource(activeSubtitleId,activeSubtitleSource);
	}

}
