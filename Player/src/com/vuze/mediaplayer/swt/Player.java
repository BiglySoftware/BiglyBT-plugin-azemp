package com.vuze.mediaplayer.swt;

import java.io.File;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.AESemaphore;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.DisplayFormatters;

import com.vuze.mediaplayer.Language;
import com.vuze.mediaplayer.LanguageSource;
import com.vuze.mediaplayer.MediaPlaybackState;
import com.vuze.mediaplayer.MetaDataListener;
import com.vuze.mediaplayer.PlayerPreferences;
import com.vuze.mediaplayer.StateListener;

public class 
Player 
{
	private Composite			parent;
	private Display 			display;
	private PlayerFrame 		playerFrame;
	private BaseMediaPlayerSWT	player;
	
	private EventCatcher		eventCatcher;
	private FullScreenControls 	controls;		
	
	private boolean overControls;
	private boolean isPaused;
	
	private BufferingControls	bufferingControls;
	
	private Thread hideThread;
	
	private long lastMoveTime;
	private Object hideThreadWait = new Object();
	
	private Cursor hiddenCursor;
	private Rectangle controlsSize;
	
	private Listener keyListener;
	private boolean autoResize;
	
	public Player( BaseMediaPlayerSWT	_player, final Composite _parent) {

		player	= _player;
		parent	= _parent;
		display = parent.getDisplay();
		playerFrame = new PlayerFrame(parent, player );
		
		Shell parentShell 		= parent.getShell();
		Shell subParentShell 	= parentShell;
		
		if ( player.isVLC() && Constants.isLinux ){
			
				// for some reason we're not getting events from the player canvas so we have
				// to add our own invisible catcher shell
			eventCatcher = new EventCatcher( playerFrame, parentShell);
			subParentShell = eventCatcher.getShell();
		}
		
		controls = new FullScreenControls(playerFrame, parentShell, subParentShell);
		//playerFrame.setControls(controls.getRealShell());
		controlsSize = controls.getShell().getBounds();
		
		bufferingControls = new BufferingControls(playerFrame, parentShell, subParentShell);
					
		Color white = display.getSystemColor (SWT.COLOR_WHITE);
		Color black = display.getSystemColor (SWT.COLOR_BLACK);
		PaletteData palette = new PaletteData (new RGB [] {white.getRGB(), black.getRGB()});
		ImageData sourceData = new ImageData (16, 16, 1, palette);
		sourceData.transparentPixel = 0;
		hiddenCursor = new Cursor(display, sourceData, 0, 0);
		
		lastMoveTime = System.currentTimeMillis();
		
		hideThread = new Thread("auto hide controls") {
			private boolean	destroyed;
			
			@Override
			public void run() {
				try {
					display.asyncExec(
						new Runnable() 
						{
							@Override
							public void
							run()
							{
								if ( parent.isDisposed()){
									
									synchronized (hideThreadWait){
										
										destroyed = true;
										
										hideThreadWait.notifyAll();
									}
								}else{
									parent.addDisposeListener(
										new DisposeListener()
										{
											@Override
											public void
											widgetDisposed(
												DisposeEvent arg0) 
											{
												synchronized (hideThreadWait) {
													destroyed = true;
													
													hideThreadWait.notifyAll();
												}
											}
											
										});
								}
							}
						});
					
					while(!parent.isDisposed()) {						
						if(overControls || isPaused) {
							synchronized (hideThreadWait) {
								if ( destroyed ){
									return;
								}
								hideThreadWait.wait();
							}
						} else {
							long currentTime = System.currentTimeMillis();
							long delta = currentTime - lastMoveTime;
							long waitFor = 2000 - delta;
	
							if(waitFor > 0) {							
									Thread.sleep(waitFor);							
							} else {
								controls.hide();
								hideCursor();								
								synchronized (hideThreadWait) {
									if ( destroyed ){
										return;
									}
									hideThreadWait.wait();
								}
								
							}
						}
					}
					

				} catch (Exception e) {
					e.printStackTrace();
				}
			
			};
		};
		
		hideThread.setDaemon(true);
		hideThread.start();
		
		player.addMetaDataListener(new MetaDataListener() {
			
			@Override
			public void receivedVideoResolution(final int width, final int height) {
				
			}
			
			@Override
			public void receivedDuration(float durationInSecs) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void receivedDisplayResolution(int width, int height) {
				if(autoResize) {
					setSize( width, height );
				}
			}
			
			@Override
			public void foundSubtitle(Language language) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void foundAudioTrack(Language language) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void activeSubtitleChanged(String subtitleId, LanguageSource source) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void activeAudioTrackChanged(String audioTrackId) {
				// TODO Auto-generated method stub
				
			}
		});
		
		Listener move_listener = 
			new Listener() {
				@Override
				public void handleEvent(Event evt) {
					overControls = false;
					controls.show();
					playerFrame.setCursor(null);
					controls.setCursor(null);
					bufferingControls.setCursor(null);
					if ( eventCatcher != null ){
						eventCatcher.setCursor(null);
					}
					lastMoveTime = System.currentTimeMillis();
					synchronized (hideThreadWait) {
						hideThreadWait.notify();
					}				
				}
			};
		
				// hook into both so that playback without video (i.e. audio)  works as playerFame isn't picking up events in that case
			
		parent.addListener(SWT.MouseMove, move_listener );
		
		if ( eventCatcher != null ){
			
			eventCatcher.addListener(SWT.MouseMove, move_listener );
		}
		
		bufferingControls.addListener(SWT.MouseMove, move_listener );
		
		playerFrame.addListener(SWT.MouseMove, move_listener );
		
		playerFrame.addListener(SWT.MouseEnter, new Listener() {
			
			
			@Override
			public void handleEvent(Event arg0) {
				overControls = false;
				
			}
		});
		
		playerFrame.addListener(SWT.MouseExit, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				Rectangle bounds = parent.getShell().getBounds();
				if(evt.x > 0 && evt.y > 0  && evt.x < bounds.width && evt.y < bounds.height) {
					controls.setFocus();
				}
			}
		});
				
		controls.getShell().addListener(SWT.MouseEnter, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				overControls = true;
				controls.setFocus();
			}
		});
		controls.getShell().addListener(SWT.MouseExit, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if(evt.x <= 0 || evt.y <= 0 || evt.x >= controlsSize.width || evt.y >= controlsSize.height) {
					overControls = false;
				}
			}
		});
		
		player.addStateListener(new StateListener() {
			
			
			@Override
			public void stateChanged(MediaPlaybackState newState) {
				if(newState == MediaPlaybackState.Paused) {
					isPaused = true;
				} else {
					isPaused = false;
				}	
				
				if ( newState == MediaPlaybackState.Failed ){
					
					bufferingControls.setFailed( MessageText.getString( "azemp.failed" ), newState.getDetails());
					
				}else if(newState == MediaPlaybackState.Closed) {
					display.asyncExec(new Runnable() {
						
						@Override
						public void run() {
							if(playerFrame.isDisposed()) return;
							if(playerFrame.getFullscreen()) {
								playerFrame.setFullscreen(false);
							}
							if(player.getDurationInSecs() == 0) {
								MessageBox mb = new MessageBox(parent.getShell(),SWT.OK | SWT.ERROR);
								mb.setText("Error");
								File f = new File(player.getOpenedFile());
								if(!(f.exists() && f.isFile())) {
									mb.setMessage("Video file not found");
								} else {
									mb.setMessage("The video could not be loaded.");
								}
								mb.open();
							}
						}
					});
				}
				
			}
		});
		
		playerFrame.addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				playerFrame.setFullscreen(!playerFrame.getFullscreen());
			}
		});
		
		keyListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				//System.out.println(SWT.ALT + " " + event.stateMask + " '" + event.character + "'");
				switch(event.character) {
					case SWT.ESC :
						if(playerFrame.getFullscreen()) {
							playerFrame.setFullscreen(false);
						}
						break;
					case ' ' :
						player.togglePause();
						break;
					case 'f' :
					case 'F' :
						if((event.stateMask & SWT.COMMAND) != 0) {
							playerFrame.setFullscreen(!playerFrame.getFullscreen());
						}
						break;
					case '\r':
						if((event.stateMask & SWT.ALT) != 0 ) {
							playerFrame.setFullscreen(!playerFrame.getFullscreen());
						}
						break;
					case 'w' :
						if((event.stateMask & SWT.COMMAND) != 0) {
							parent.getShell().close();
						}
						break;
						
				}
			}
		};
		
		if ( player.isVLC()){
			playerFrame.addListener( SWT.KeyDown,keyListener);
			
			if ( eventCatcher != null ){
				eventCatcher.addListener( SWT.KeyDown,keyListener);
			}
		}
		
		parent.addListener(SWT.KeyDown,keyListener);
		controls.getShell().addListener(SWT.KeyDown, keyListener);
		
		parent.getShell().addListener(SWT.Close, new Listener() {
			
			@Override
			public void handleEvent(Event evt) {
				//Handle preferences
				PlayerPreferences preferences = player.getPreferences();
				
				if(preferences != null) {
					Point p = parent.getShell().getLocation();
					preferences.setWindowPosition(p);
					preferences.setVolume(player.getVolume());
				}
			}
		});
		
		parent.getShell().addListener(SWT.Dispose, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if(hiddenCursor != null && !hiddenCursor.isDisposed()) {
					hiddenCursor.dispose();
					
				}
			}
		});
	}
	
	private void hideCursor() {
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				playerFrame.setCursor(hiddenCursor);
				controls.setCursor(hiddenCursor);
				bufferingControls.setCursor(hiddenCursor);
				if ( eventCatcher != null ){
					eventCatcher.setCursor(hiddenCursor);
				}
			}
		});
	}
	
	public void
	setSize(
		final int		width,
		final int		height )
	{
		display.asyncExec(new Runnable() {
			
			@Override
			public void run() {
				if(parent.isDisposed()) return;
				Shell s = parent.getShell();
				Rectangle client = s.getClientArea();
				Rectangle bounds = s.getBounds();
				
				Monitor monitor = s.getMonitor();
				Rectangle monitorBounds = monitor.getClientArea();
				
				//1. Let's compute the target size for the shell
				int targetWidth,targetHeight;
				targetWidth = width + (bounds.width - client.width);
				targetHeight = height + (bounds.height - client.height);
				//We're going to allow for a small shell border (20 px) to get hidden from the screen, so that 1080p content
				//Can fit on a 1920x1200/1080 screen even with a shell border, and we'll move the shell outside the monitor area
				if(targetWidth > monitorBounds.width + 20) {
					targetWidth = monitorBounds.width;
					targetHeight = targetWidth * targetHeight / width;
				}
				if(targetHeight > monitorBounds.height) {
					targetHeight = monitorBounds.height;
					targetWidth = targetWidth * targetHeight / height;
				}
				
				Rectangle currentBounds = s.getBounds();
				
				int targetX,targetY;
				targetX = currentBounds.x;
				targetY = currentBounds.y;
				
				if(targetX + targetWidth > monitorBounds.x + monitorBounds.width) {
					targetX = monitorBounds.width - targetWidth;
					//If we've gone outside the screen, let's only do it by half
					//(assuming left and right shell borders are the same size)
					if(targetX < 0) {
						targetX /= 2;
					}
				}
				
				if(targetY + targetHeight > monitorBounds.y + monitorBounds.height) {
					targetY = monitorBounds.height - targetHeight;
					//If we've gone outside the screen we're doing something wrong ...
					if(targetY < 0) {
						targetY = 0;
					}
				}
				
				Rectangle targetBounds = new Rectangle(targetX,targetY,targetWidth,targetHeight);
				
				resizeParentShell(s,currentBounds,targetBounds);
				
			}
		});	
	}
	
	public void open(String file, boolean stream_mode ) {
		
		bufferingControls.hide();
		if ( !stream_mode ){
			player.clearDurationInSecs();
		}
		controls.prepare();
		controls.setPlayEnabled( true );
		controls.setSeekMaxTime( stream_mode?0:-1 );
		player.open(file);		
	}
	
	public void prepare()
	{
		stop();
		player.clearDurationInSecs();
		controls.prepare();
		controls.setPlayEnabled( false );
		controls.setSeekMaxTime( 0 );
	}
	
	public void stop() {
		player.stop();
	}
	
	public void pause() {
		player.doPause();
		controls.setPlayEnabled( false );
	}
	
	public void resume() {
		bufferingControls.hide();
		controls.setPlayEnabled( true );
		player.doResume();
	}
	
	public boolean
	isActive()
	{
		return( !playerFrame.isDisposed());
	}
	
	
	public void 
	playStats(
		Map<String,Object>		map )
	{		
		Long	file_size 	= (Long)map.get( "file_size" );
		Long	cont_done	= (Long)map.get( "cont_done" );
		Long	duration	= (Long)map.get( "duration" );
		
		Long	min_secs	= (Long)map.get( "buffer_min" );

		Integer buffer_secs	= (Integer)map.get( "buffer_secs" );
		
		if ( file_size == null || file_size == 0 || cont_done == null || duration == null || min_secs == null || buffer_secs == null ){
			
			return;
		}
		
		float max;
		
		if ((long)cont_done == (long)file_size ){
			
			max = duration;
			
		}else{
						
			float existing_max = controls.getSeekMaxTime();
			
			// System.out.println( map );
			
			min_secs += 10;
			
			if ( buffer_secs >= min_secs ){
				
				// we're playing and have enough buffer so the max seek must at least be where we currently
				// are
								
				max = controls.getCurrentTimeSecs();
			
				float	estimated_max_time_pos = cont_done * duration / ( file_size * 1000 ) - min_secs;

				float	extra_secs = estimated_max_time_pos - max;
				
				if ( extra_secs > 0 ){
					
					max = max + extra_secs*9/10;
				}
				
				if ( existing_max >= max ){
					
					max = existing_max;
				}
			}else{
								
				max = controls.getCurrentTimeSecs();
				
				if ( max - existing_max > min_secs ){

						// keep at least min-secs behind current
					
					max = max - min_secs;
					
				}else{
				
					// not enough buffer, stick with where we were

					max = existing_max;
				}
			}
			
			if ( max > duration ){
				
				max = duration;
			}
		}
		
		controls.setSeekMaxTime( max );
	}
	
	public void 
	buffering(
		Map<String,Object>		map )
	{
		int	state = (Integer)map.get( "state" );
		
		// System.out.println( "buffering: " + map );

		bufferingControls.show();
		controls.setPlayEnabled( false );
		
		String	line1;
		String	line2;
		String	line3	= "";
		
		Long	elapsed = (Long)map.get( "dl_time" );
		Long	dl_rate	= (Long)map.get( "dl_rate" );
		Long	dl_size = (Long)map.get( "dl_size" );
		
		Long	stream_rate = (Long)map.get( "stream_rate" );
		
		long elapsed_sec = elapsed==null?0:(elapsed/1000);
		
		if ( dl_rate != null && dl_size != null ){
			
			line3 	= 
				MessageText.getString( 
					stream_rate==null?"azemp.play_in.rate1":"azemp.play_in.rate2", 
					new String[]{ 
						trim(DisplayFormatters.formatByteCountToKiBEtcPerSec( dl_rate )),
						trim(DisplayFormatters.formatByteCountToKiBEtc( dl_size )),
						stream_rate==null?"":trim(DisplayFormatters.formatByteCountToKiBEtcPerSec( stream_rate ))
					});
		}
		
		if ( state == 1 ){
			
			line1	= MessageText.getString( "azemp.analysing_md" );
			line2	= (String)map.get( "msg" );
			
			if ( line2 == null ){
				
				line2 = "";
			}
			
		}else if ( state == 2 ){
			
			Integer i_preview = (Integer)map.get( "preview" );
		
			boolean	preview = i_preview != null && i_preview == 1;

			Integer i_eta = (Integer)map.get( "eta" );
					
			int 	eta = i_eta==null?0:i_eta;
		
			line1	= MessageText.getString( "azemp.buffering" );
			
			if ( eta > 30*60 && elapsed_sec < 60 ){

				line2 	= MessageText.getString( preview?"azemp.preview_in.calc":"azemp.play_in.calc" );
				
			}else if ( eta > 60*60 ){
			
				line2 	= MessageText.getString( preview?"azemp.preview_in.ages":"azemp.play_in.ages" );

			}else{
				
				if ( eta < 0 ){
					
					eta = 0;
				}
				
				String time_str = Utils.getFormatedTime(eta,true);
				
				if ( time_str.startsWith( "0:00:" )){
					
					time_str = time_str.substring( 3 );
				}
				
				line2 	= MessageText.getString( preview?"azemp.preview_in.time":"azemp.play_in.time", new String[]{ time_str });
			}
		}else{
			
			line1	= MessageText.getString( "azemp.failed" );
			line2	= (String)map.get( "msg" );
			
			if ( line2 == null ){
				
				line2 = "";
			}		
		}
		
		bufferingControls.updateText( line1, line2, line3 );
	}
	
	private String
	trim(
		String	str )
	{
		int pos = str.indexOf('.');
		
		if ( pos != -1 ){
			
			String s = str.substring( 0, pos );
			
			while( str.charAt(pos) != ' ' ){
				
				pos++;
			}
			
			return( s + ' ' + str.substring( pos ));
		}
		
		return( str );
	}
	
	private void resizeParentShell(final Shell s,final Rectangle currentBounds,
			final Rectangle targetBounds) {
		
		if(Utils.isMacOSX()) {
			player.pause();
			//Animate on OSX ...
			Thread t = new Thread("Player Resizer") {
				@Override
				public void run() {
					if(!display.isDisposed()) {
						display.asyncExec(new Runnable() {
							
							@Override
							public void run() {
								//playerFrame.setVisible(false); // breaks playback
							}
						});
					}
					
					//We'll resize within 1s
					try {
						final AESemaphore sem = new AESemaphore("resize wait");
						//final long startTime = System.currentTimeMillis();
						//final float runTime = 3000;
						float time = 0;
						do {
							long currentTime = System.currentTimeMillis();
							//t = (float)currentTime / runTime;
							time += 0.06;
							float t = (float) Math.pow(time,0.30);
							if(t > 1f) {
								t = 1f;
							}
							float one_minus_t = 1 - t;
							final int x = (int) (targetBounds.x * t + one_minus_t * currentBounds.x);
							final int y = (int) (targetBounds.y * t + one_minus_t * currentBounds.y);
							final int width = (int) (targetBounds.width * t + one_minus_t * currentBounds.width);
							final int height = (int) (targetBounds.height * t + one_minus_t * currentBounds.height);
							//System.out.println(x+","+y+","+width+","+height);
							if(!display.isDisposed()) {
								display.asyncExec(new Runnable() {
									
									@Override
									public void run() {
										//System.out.println(">" + x+","+y+","+width+","+height);
										if(!s.isDisposed()) {
											s.setBounds(x,y,width,height);
										}
										sem.release();
									}
								});
								
							}
							sem.reserve();
							long timeTaken = System.currentTimeMillis() - currentTime;
							long sleepTime = 25 - timeTaken;
							if(sleepTime > 25) sleepTime = 25;
							if(sleepTime > 0 ) {
								Thread.sleep(sleepTime);
							}
						} while( time < 1f);
					} catch(Exception e) {
					}
					if(!display.isDisposed()) {
						display.asyncExec(new Runnable() {
							
							@Override
							public void run() {
								if(!playerFrame.isDisposed()) {
									//playerFrame.setVisible(true); breaks playback 
									player.play();
								}
							}
						});
					}
					
				}
			};
			t.setDaemon(true);
			t.start();
			
		} else {
			s.setBounds(targetBounds.x,targetBounds.y,targetBounds.width,targetBounds.height);
		}
	}
	
	public void setAutoResize(boolean b) {
		autoResize = b;
		
	}
	
	public void addStateListener(StateListener listener) {
		player.addStateListener(listener);
	}
	
	public void
	setDurationInSeconds(
		float	secs )
	{
		player.setDurationInSecs( secs );
	}
	
	public float
	getDurationInSeconds()
	{
		return( player.getDurationInSecs());
	}
}
