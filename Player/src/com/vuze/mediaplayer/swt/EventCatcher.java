package com.vuze.mediaplayer.swt;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.eclipse.swt.widgets.Shell;

public class 
EventCatcher 
{		
	private Display 		display;
	private Shell 			parent;
	
	private Shell 			shell;
	
	private PlayerFrame 	player;
		
	private Listener antiCloseListener;
		
	
	public 
	EventCatcher(
		PlayerFrame player,
		Shell parent) 
	{
		
		this.player = player;
		this.parent = parent;
		
		display = parent.getDisplay();
			
		shell = new Shell(parent,SWT.NO_TRIM);
		shell.setBackgroundMode(SWT.INHERIT_FORCE);
		
		setShellSizeShapeAndBackground();
				
		positionShell();
		
		addParentListeners();
		
		addDisposeListener();
						
		shell.open();
	}
	
	private void addDisposeListener() {
		shell.addListener(SWT.Dispose, new Listener() {
			@Override
			public void handleEvent(Event arg0) {

			}
		});		
	}
	
	protected Shell
	getShell()
	{
		return( shell );
	}
	
	protected void
	addListener(
		int			eventType,
		Listener	listener )
	{
		shell.addListener(eventType, listener);
	}

	private void addParentListeners() {
		
		Listener repositionListener = new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				setShellSizeShapeAndBackground();
				positionShell();
			}
		};
		
		parent.addListener(SWT.Move, repositionListener);
		parent.addListener(SWT.Resize, repositionListener);
	}

	private void positionShell() {
		
		Point start = parent.toDisplay(0,0);
		
		shell.setLocation(start.x, start.y);
		
		Point location = display.getCursorLocation();
		location = parent.toControl(location);
		Rectangle bounds = shell.getBounds();
		if(!bounds.contains(location)) {
			Event evt = new Event();
			evt.x = location.x;
			evt.y = location.y;
			for(Listener listener : player.getListeners(SWT.MouseMove)) {
				listener.handleEvent(evt);
			}
		}	
	}

	private void setShellSizeShapeAndBackground() {
		Rectangle parentBounds = parent.getClientArea();

		shell.setSize(parentBounds.width,parentBounds.height);
		shell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
				
		antiCloseListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				event.doit = false;
			}
		};
		
		parent.addListener(SWT.Close, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				close();
			}
		});
		shell.addListener(SWT.Close, antiCloseListener);
		
		shell.setAlpha( 0 );
	}
	
	public void close() {
		if(!shell.isDisposed()){
			shell.removeListener(SWT.Close, antiCloseListener);
			shell.close();
		}
	}
	
	public void setCursor(Cursor cursor) {
		if(!shell.isDisposed()) shell.setCursor(cursor);
	}
}
