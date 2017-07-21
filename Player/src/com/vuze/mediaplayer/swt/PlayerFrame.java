package com.vuze.mediaplayer.swt;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.vuze.mediaplayer.*;




public class PlayerFrame  implements BaseMediaPlayerFrame {
	
	private Display display;
	private Composite parent;
	private BaseMediaPlayerSWT	player;
	private Composite outerPlayerFrame;
	private RenderFrame	 rendererFrame;
	
	AspectRatioData aspectRatioData;
	
	int displayWidth = 0;
	int displayHeight = 0;
	
	//Shell fullscreenShell;
	
	public PlayerFrame(Composite _parent, BaseMediaPlayerSWT _player) {

		display = Display.getCurrent();
		
		parent = _parent;
		player	= _player;
		
		/*
		fullscreenShell = new Shell(display,SWT.NO_TRIM | SWT.ON_TOP);
		fullscreenShell.setLocation(0,0);
		fullscreenShell.setLayout(new FillLayout());
		*/

		outerPlayerFrame = new Composite(parent, SWT.NONE);
		outerPlayerFrame.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		//outerPlayerFrame.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		
		outerPlayerFrame.setLayout(new AspectRatioLayout());		
		rendererFrame = player.createRenderFrame( this, outerPlayerFrame );
		rendererFrame.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		//rendererFrame.setForeground(display.getSystemColor(SWT.COLOR_RED));
		
		aspectRatioData = new AspectRatioData();
		rendererFrame.setLayoutData(aspectRatioData);
		
		outerPlayerFrame.addDisposeListener(new DisposeListener() {
			
			
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				player.dispose();
			}
		});

		if(Utils.isWindows()) {
			outerPlayerFrame.addListener(SWT.Resize, new Listener() {
				
				@Override
				public void handleEvent(Event evt) {
					if(player.getCurrentState() == MediaPlaybackState.Paused) {
						player.doRedraw();
					}
				}
			});
		}

	}
	
	@Override
	public BaseMediaPlayer getMediaPlayer() {
		return player;
	}	
	
	public void addListener(int event,Listener listener) {
		outerPlayerFrame.addListener(event, listener);
		rendererFrame.addListener(event, listener);
		//fullscreenShell.addListener(event, listener);
	}
	
	@Override
	public Listener[] getListeners(int event) {
		return outerPlayerFrame.getListeners(event);
	}
	
	public void setLayoutData(Object layoutData) {
		outerPlayerFrame.setLayoutData(layoutData);
	}
	
	public void setData(Object data) {
		outerPlayerFrame.setData(data);
	}
	
	public void setData(String key,Object data) {
		outerPlayerFrame.setData(key,data);
	}
	
	public Object getData() {
		return outerPlayerFrame.getData();
	}
	
	public Object getData(String key) {
		return outerPlayerFrame.getData(key);
	}
	
	public Composite getComposite() {
		return outerPlayerFrame;
	}
	
	
	public String[] getExtraOptions() {
		return rendererFrame.getExtraOptions();
	}
	
	
	public void setAspectRatio(float aspectRatio) {		
		if(aspectRatioData != null) {
			aspectRatioData.aspectRatio = aspectRatio;
		}
		display.asyncExec(new Runnable() {
			
			
			@Override
			public void run() {
				if(outerPlayerFrame != null && ! outerPlayerFrame.isDisposed()) {
					outerPlayerFrame.layout(true);
				}
			}
		});
		
	}

	public void setCursor(Cursor hiddenCursor) {
		if(outerPlayerFrame != null && !outerPlayerFrame.isDisposed()) {
			outerPlayerFrame.setCursor(hiddenCursor);
		}
		if(rendererFrame != null && !rendererFrame.isDisposed()) {
			rendererFrame.setCursor(hiddenCursor);
		}
		if(blackOutShells != null) {
			for(Shell s : blackOutShells) {
				if(s != null && !s.isDisposed()) {
					s.setCursor(hiddenCursor);
				}
			}
		}
	}
	
	public void setVisible(boolean visible) {
		rendererFrame.setVisible(visible);
	}
	
	private Shell[] blackOutShells;
	
	@Override
	public void setFullscreen(boolean fullscreen) {
		Monitor fullScreenMonitor = outerPlayerFrame.getShell().getMonitor();
		if(fullscreen) {
			Monitor[] monitors = display.getMonitors();
			blackOutShells = new Shell[monitors.length -1];
			int i = 0;
			for(Monitor monitor : monitors) {
				if(monitor.getBounds().equals(fullScreenMonitor.getBounds())) continue;
				final Shell s = new Shell(display,SWT.NO_TRIM | SWT.ON_TOP);
				s.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
				s.setBounds(monitor.getBounds());
				s.addListener(SWT.Close, new Listener() {
					
					@Override
					public void handleEvent(Event event) {
						event.doit = false;
					}
				});
				Listener[] listeners = outerPlayerFrame.getListeners(SWT.MouseMove);
				for(Listener listener : listeners) {
					s.addListener(SWT.MouseMove, listener);
				}
				listeners = outerPlayerFrame.getListeners(SWT.MouseDoubleClick);
				for(Listener listener : listeners) {
					s.addListener(SWT.MouseDoubleClick, listener);
				}
				s.setAlpha(0);
				s.addListener(SWT.MouseDown, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						s.dispose();
					}
				});
				s.open();
				blackOutShells[i++] = s;
			}
			Thread alphaInc = new Thread() {
				@Override
				public void run() {
					try {
						int alpha = 0;
						while(alpha < 255) {
							alpha += 15;
							if(alpha > 255) {
								alpha = 255;
							}
							
							final int alphaSet = alpha;
							if(display.isDisposed()) return;
							
							display.asyncExec(new Runnable() {
								
								@Override
								public void run() {
									if(blackOutShells == null) return;
									
									for(Shell s : blackOutShells) {
										if(s != null && !s.isDisposed()) {
											s.setAlpha(alphaSet);
										}
									}
								}
							});
							
							Thread.sleep(20);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				};
			};
			alphaInc.setDaemon(true);
			alphaInc.start();
		} else if(blackOutShells != null) {
			for(Shell s : blackOutShells) {
				if(s != null && !s.isDisposed()) {
					s.dispose();
				}
			}
			blackOutShells = null;
		}
		
		if(Utils.isWindows()) {
			if(fullscreen) {
				
				oldSize = outerPlayerFrame.getShell().getBounds();
				
				if(display.getPrimaryMonitor().equals(fullScreenMonitor)) {
					Rectangle bounds = fullScreenMonitor.getBounds();
					outerPlayerFrame.getShell().setBounds(bounds);
				}
				
				outerPlayerFrame.getShell().setFullScreen(true);
			} else {
				outerPlayerFrame.getShell().setFullScreen(false);
				outerPlayerFrame.getShell().setBounds(oldSize);
			}
		} else {
			outerPlayerFrame.getShell().setActive();
			outerPlayerFrame.getShell().setFullScreen(fullscreen);
		}
		
	}
	
	Rectangle oldSize;
	
	@Override
	public boolean getFullscreen() {
		/*if(Utils.isWindows()) {
			return fullscreenShell.isVisible();
		} else {*/
			return outerPlayerFrame.getShell().getFullScreen();
		//}
	}
	
	@Override
	public boolean isDisposed() {
		return outerPlayerFrame.isDisposed();
	}


	/*Shell controls;
	public void setControls(Shell shell) {
		controls = shell;
	}*/
}
