package com.vuze.mediaplayer;

import org.eclipse.swt.graphics.Point;

public interface PlayerPreferences {
	
	public void setVolume(int volume);
	public int getVolume();
	
	public void setWindowPosition(Point p);
	public Point getWindowPosition();
	
	public void setPositionForFile(String file,float position);
	public float getPositionForFile(String file);

}
