package com.vuze.swt;

import java.lang.reflect.Constructor;

import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.biglybt.core.util.Constants;

public class MPlayerRendererCanvas extends Canvas implements MPlayerRendererInterface {
	
	MPlayerRendererInterface impl;
	
	public MPlayerRendererCanvas(Composite parent,int style) {
		super(parent,style);
		
		//We need different 
		String osName = System.getProperty("os.name").toLowerCase();
		if(osName == null) return;
		if(osName.startsWith("windows")) {
			//Use reflection to be safe;
			try {
				Class clazz = Class.forName("com.vuze.swt.windows.MPlayerRendererCanvasWindows");
				Constructor constructor = clazz.getConstructor(Canvas.class);
				impl = (MPlayerRendererInterface) constructor.newInstance(this);

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(osName.startsWith("mac os x")){
			//Use reflection to be safe;
			try {
				Class clazz = Class.forName("com.vuze.swt.osx.MPlayerRendererCanvasOSX" + (Constants.is64Bit ? "64" : ""));
				Constructor constructor = clazz.getConstructor(Canvas.class);
				impl = (MPlayerRendererInterface) constructor.newInstance(this);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public String[] getExtraMplayerOptions() {
		if(impl != null) {
			return impl.getExtraMplayerOptions();
		}
		
		return new String[0];
	}
	

}
