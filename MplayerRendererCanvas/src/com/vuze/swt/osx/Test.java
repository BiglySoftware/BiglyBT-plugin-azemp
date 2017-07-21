package com.vuze.swt.osx;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Test {
	
	public static void main(String[] args) {
		
		System.out.println(System.getProperty("os.name"));
		
		Display display = new Display();
		Shell shell = new Shell();
		
		shell.setLayout(new FillLayout());
		
		//GLData data = new GLData();
		Canvas canvas = new Canvas(shell,SWT.NONE);
		
		MPlayerRendererCanvasOSX64 renderer = new MPlayerRendererCanvasOSX64(canvas);
		
		System.out.println(renderer.getExtraMplayerOptions()[1]);
		
		shell.open();
		
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

}
