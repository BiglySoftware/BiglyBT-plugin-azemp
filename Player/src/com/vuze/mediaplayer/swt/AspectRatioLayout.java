package com.vuze.mediaplayer.swt;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

public class AspectRatioLayout extends Layout {
	
	int count;
	
	public AspectRatioLayout() {		
	}
	
	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		return new Point(wHint,hHint);
	}
	
	@Override
	protected void layout(Composite composite, boolean flushCache) {
		Rectangle bounds = composite.getBounds();
		for(Control c : composite.getChildren()) {
			if(c != null && !c.isDisposed()) {
				Object layoutData = c.getLayoutData();
				if(layoutData != null && layoutData instanceof AspectRatioData) {
					AspectRatioData data = (AspectRatioData) layoutData;
					
					float aspectRatio = data.aspectRatio;
					
					if(bounds.height > 0 && aspectRatio > 0) {
					
						float containerAspectRatio = (float)bounds.width / (float)bounds.height;
						
						if(containerAspectRatio >= aspectRatio) {
							int x,y,w,h;
							y = 0;
							h = bounds.height;
							w = (int) (bounds.height * aspectRatio);
							x = (bounds.width - w) / 2;
							c.setLocation(new Point(x,y));
							c.setSize(new Point(w,h));
						} else {
							int x,y,w,h;
							x = 0;
							w = bounds.width;
							h = (int) (bounds.width / aspectRatio);
							y = (bounds.height - h) / 2;
							c.setLocation(new Point(x,y));
							c.setSize(new Point(w,h));
						}
					}
					
				}
			}
		}
		
	}
	
	@Override
	protected boolean flushCache(Control arg0) {
		// TODO Auto-generated method stub
		return super.flushCache(arg0);
	}

}
