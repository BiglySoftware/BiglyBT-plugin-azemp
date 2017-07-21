package com.azureus.plugins.azemp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import com.biglybt.pif.PluginConfig;
import com.biglybt.pif.PluginInterface;
import org.eclipse.swt.graphics.Point;

import com.vuze.mediaplayer.PlayerPreferences;

public class VuzePlayerPreferences implements PlayerPreferences {

	private PluginConfig config;
	private Properties videoFileProperties;
	private File videoFilePropertieFile;
	
	@Override
	public float getPositionForFile(String file) {
		try {
			String position = videoFileProperties.getProperty("h" + file.hashCode());
			if(position != null) {
				return Float.parseFloat(position);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0f;
	}
	
	@Override
	public int getVolume() {
		
		int volume = config.getPluginIntParameter("volume",75);
		//System.out.println("get>" + volume);
		return volume;
	}
	
	@Override
	public void setVolume(int volume) {
		//System.out.println("set>" + volume);
		config.setPluginParameter("volume", volume);
	}
	
	@Override
	public Point getWindowPosition() {
		Point p = new Point(100,100);
		p.x = config.getPluginIntParameter("window_x",100);
		p.y = config.getPluginIntParameter("window_y",100);
		//System.out.println("get>" + p);
		return p;
	}
	
	@Override
	public void setPositionForFile(String file, float position) {
		try {
			videoFileProperties.setProperty("h" + file.hashCode(),"" + position);
			videoFileProperties.store(new FileOutputStream(videoFilePropertieFile),null);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	@Override
	public void setWindowPosition(Point p) {
		//System.out.println("set>" + p);
		config.setPluginParameter("window_x", p.x);
		config.setPluginParameter("window_y", p.y);
	}
	
	public VuzePlayerPreferences(PluginInterface pi) {
		config = pi.getPluginconfig();
		videoFileProperties = new Properties();
		videoFilePropertieFile = config.getPluginUserFile("videofiles.properties");
		try {
			videoFileProperties.load(new FileInputStream(videoFilePropertieFile));
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
}
