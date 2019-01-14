/*
 * Created on Mar 17, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.vuze.mediaplayer.vlc;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import com.biglybt.core.util.*;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.mainwindow.Colors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibX11;
import uk.co.caprica.vlcj.binding.internal.libvlc_instance_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.AudioTrackInfo;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.TextTrackInfo;
import uk.co.caprica.vlcj.player.TrackDescription;
import uk.co.caprica.vlcj.player.TrackInfo;
import uk.co.caprica.vlcj.player.TrackType;
import uk.co.caprica.vlcj.player.VideoTrackInfo;
import uk.co.caprica.vlcj.player.embedded.DefaultEmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.DefaultFullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.FullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.linux.LinuxVideoSurfaceAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.mac.MacVideoSurfaceAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.windows.WindowsVideoSurfaceAdapter;
import uk.co.caprica.vlcj.player.embedded.windows.Win32FullScreenStrategy;

import com.vuze.mediaplayer.Language;
import com.vuze.mediaplayer.LanguageSource;
import com.vuze.mediaplayer.MediaPlaybackState;
import com.vuze.mediaplayer.MetaDataListener;
import com.vuze.mediaplayer.PlayerPreferences;
import com.vuze.mediaplayer.PositionListener;
import com.vuze.mediaplayer.StateListener;
import com.vuze.mediaplayer.VolumeListener;
import com.vuze.mediaplayer.swt.AspectRatioData;
import com.vuze.mediaplayer.swt.BaseMediaPlayerSWT;
import com.vuze.mediaplayer.swt.PlayerFrame;
import com.vuze.mediaplayer.swt.RenderFrame;
import com.vuze.swt.MPlayerRendererCanvas;

public class 
VLCPlayer 
	extends BaseMediaPlayerSWT 
{
	private static boolean				initialised = false;
	private static MediaPlayerFactory 	mpFactory;

	private PlayerFrame		player_frame;
	private RenderFrame		render_frame;

	
	private EmbeddedMediaPlayer 		vlcPlayer;
	
	private MetaDataListener 	metaDataListener;
	private StateListener 		stateListener;
	private VolumeListener 		volumeListener;
	private PositionListener 	positionListener;

	private static AsyncDispatcher dispatcher 	= new AsyncDispatcher( "VLC" );
	private static LinkedList<Job>		jobs		= new LinkedList<Job>();
	private static int					job_depth;
		
	private volatile boolean	 disposed = false;
			
	public static boolean
	initialise()
	{
		try{
			final boolean[] result = { false };
					
			addJob(
				new Job( "initialise" )
				{
					@Override
					public void
					exec()
					{
						if ( !initialised ){
							
							initialised = true;
							
							if ( !( Constants.isWindows || Constants.isOSX )){
							
								LibX11.INSTANCE.XInitThreads();
							}
						}
					
						if ( mpFactory == null ){
							
							if ( new NativeDiscovery().discover()){
															
								mpFactory = new MediaPlayerFactory();
							}
						}
						
						synchronized( result ){
							
							result[0] = mpFactory != null;
						}
					}
				}, true );
						
			synchronized( result ){
				
				return( result[0] );
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( false );
		}
	}
	
	public static void
	unload()
	{
		addJob(
			new Job( "unload" )
			{
				@Override
				public void
				exec()
				{
					if ( mpFactory != null ){
						
						mpFactory.release();
						
						mpFactory = null;
					}
				}
			}, true );
	}
	
	
	public
	VLCPlayer()
	{
		this( null );
	}
	
	public 
	VLCPlayer(
		PlayerPreferences preferences) 
	{
		super(preferences);
					
		initialise();
	}
	
	@Override
	public boolean isVLC() {
		return( true );
	}
		
	@Override
	public RenderFrame
	createRenderFrame(
		final PlayerFrame		_player_frame,
		final Composite 		parent )
	{
		player_frame	= _player_frame;
		
		final AtomicReference<Listener>	swt_mouse_double_click_listener =  new AtomicReference<Listener>( null );
		final AtomicReference<Listener>	swt_mouse_move_listener 		=  new AtomicReference<Listener>( null );
		final AtomicReference<Listener>	swt_key_down_listener	 		=  new AtomicReference<Listener>( null );

		final Composite videoComposite = new org.eclipse.swt.widgets.Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		
		boolean SWT_CANVAS = true;
		
		final Composite[] glue_composite = { null };
		
		if ( SWT_CANVAS ){
		
			addJob(
				new Job( "createPlayer" )
				{
					@Override
					public void
					exec()
					{
						Composite glue = createPlayer( null, null, videoComposite );
						
						synchronized( glue_composite ){
						
							glue_composite[0] = glue;
						}
					}
				}, true );
		
		}else{
			
			final java.awt.Frame videoFrame = SWT_AWT.new_Frame( videoComposite );
			
			/* JFrame test version
			final Composite videoComposite = new org.eclipse.swt.widgets.Composite(parent, SWT.NONE);
			
			final java.awt.Frame videoFrame = new JFrame( "JFrame" );		
			
			videoFrame.setSize( 800, 400 );
			
			videoFrame.setVisible( true );
			*/
						
			addJob(
				new Job( "createPlayer" )
				{
					@Override
					public void
					exec()
					{
						java.awt.Canvas playerCanvas = new java.awt.Canvas();
						
						playerCanvas.setBackground(java.awt.Color.black);
	
						playerCanvas.addMouseListener(
								new java.awt.event.MouseAdapter()
								{
									@Override
									public void 
									mouseClicked(java.awt.event.MouseEvent e) {
										if ( e.getClickCount() == 2 && !e.isConsumed()){
							
											e.consume();
											
											final Listener l = swt_mouse_double_click_listener.get();
											
											if ( l != null ){
											
												final Event ev = new Event();
												
												ev.type 	= SWT.MouseDoubleClick;
												ev.widget	= videoComposite;
												ev.x		= e.getX(); 
												ev.y		= e.getY(); 
												
												Utils.execSWTThread(
													new Runnable()
													{
														@Override
														public void
														run()
														{
															l.handleEvent( ev );
														}
													});
												
											}
										}
									}
								});
	
							playerCanvas.addMouseMotionListener(
								new java.awt.event.MouseMotionListener() {
									
									@Override
									public void mouseMoved(java.awt.event.MouseEvent e) {
										final Listener l = swt_mouse_move_listener.get();
										
										if ( l != null ){
										
											final Event ev = new Event();
											
											ev.type 	= SWT.MouseMove;
											ev.widget	= videoComposite;
											ev.x		= e.getX(); 
											ev.y		= e.getY(); 
											
											Utils.execSWTThread(
												new Runnable()
												{
													@Override
													public void
													run()
													{
														l.handleEvent( ev );
													}
												});
											
										}
									}
									
									@Override
									public void mouseDragged(java.awt.event.MouseEvent e) {
									}
								});
							
							playerCanvas.addKeyListener(
								new java.awt.event.KeyAdapter()
								{
									@Override
									public void 
									keyPressed(java.awt.event.KeyEvent e){
										
										/*
										e.consume();
										
										final Listener l = swt_key_down_listener.get();
										
										if ( l != null ){
										
											final Event ev = new Event();
											
											ev.type 		= SWT.KeyDown;
											ev.widget		= videoComposite;
											ev.character	= e.getKeyChar();
											
											Utils.execSWTThread(
												new Runnable()
												{
													public void
													run()
													{
														l.handleEvent( ev );
													}
												});
											
										}
										*/
									}
								});
							
							
						videoFrame.add( playerCanvas );
							
						createPlayer( videoFrame, playerCanvas, null );
					}
				}, true );
		}	
		
		final Composite glue;
		
		synchronized( glue_composite ){
			
			Composite temp = glue_composite[0];
			
			if ( temp == videoComposite ){
				
				glue = null;
				
			}else{
				
				glue = temp;
			}
		}
		
		render_frame = 
			new RenderFrame()
			{
				@Override
				public String[]
				getExtraOptions()
				{
					return( new String[0]);
				}
				
				@Override
				public void
				setVisible(
					boolean	vis )
				{
					videoComposite.setVisible( vis );
					
					if ( glue != null ){
						glue.setVisible( vis );
					}
				}
				
				@Override
				public void
				setBackground(
					Color	c )
				{
					videoComposite.setBackground( c );
					
					if ( glue != null ){
						glue.setBackground( c );
					}
				}
				
				@Override
				public void
				setCursor(
					Cursor	c )
				{
					videoComposite.setCursor( c );
					
					if ( glue != null ){
						glue.setCursor( c );
					}
				}
				
				@Override
				public boolean
				isDisposed()
				{
					return( videoComposite.isDisposed());
				}
				
				@Override
				public void
				setLayoutData(
					AspectRatioData	ld )
				{
					videoComposite.setLayoutData( ld );
				}
				
				@Override
				public void
				addListener(
					int			eventType,
					Listener	listener )
				{
					videoComposite.addListener(eventType, listener);
					
					if ( glue != null ){
						glue.addListener(eventType, listener);
					}
					
					if ( eventType == SWT.MouseMove ){
						
						swt_mouse_move_listener.set( listener );
						
					}else if ( eventType == SWT.MouseDoubleClick ){
						
						swt_mouse_double_click_listener.set( listener );
						
					}else if ( eventType == SWT.KeyDown ){
						
						swt_key_down_listener.set( listener );
					}
				}
			};
		
		return( render_frame );
	}
	
	private Composite
	createPlayer(
		java.awt.Frame		awt_videoFrame,
		java.awt.Canvas		awt_playerCanvas,
		final Composite		swt_composite )
	{	
		Composite glue = null;
		
		if ( vlcPlayer != null ){
			
			vlcPlayer.stop();
			vlcPlayer.release();

			vlcPlayer = null;
		}
		
		if ( !initialise()){
			
			Debug.out( "VLC initialisation failed" );
			
			return( null );
		}		
		
		VideoSurfaceAdapter 	vsa;

		if ( Constants.isWindows ){
					
			vsa = new WindowsVideoSurfaceAdapter();
			
		}else if ( Constants.isOSX ){
						
			vsa = new MacVideoSurfaceAdapter();
		}else{
						
			vsa = new LinuxVideoSurfaceAdapter();
		}
			
		if ( swt_composite != null ){
			
			final MPlayerRendererCanvas mplayer_canvas;
			final Composite				general_canvas;
			
			if ( Constants.isOSX ){
	        
				swt_composite.setLayout( new FillLayout());
	        
				mplayer_canvas = new MPlayerRendererCanvas( swt_composite, SWT.NONE );
	        				
				glue = mplayer_canvas;
				
				general_canvas = null;
				
			}else if ( Constants.isWindows ){
				
				swt_composite.setLayout( new FillLayout());
				
				general_canvas = new Canvas( swt_composite, SWT.NONE );
				
				general_canvas.setBackground( Colors.black );
				
				glue = general_canvas;
				
				mplayer_canvas = null;
				
			}else{
				
					// linux requires direct attaching to the embedded composite.
				
				general_canvas = swt_composite;
				mplayer_canvas = null;
			}
			
			final VideoSurface vs = 
				new VideoSurface( vsa )
				{
				    @Override
				    public void attach(LibVlc libvlc, MediaPlayer mediaPlayer) {
				    	
				    	try{
				    		if ( mplayer_canvas == null ){
				    			
					    		Field f_handle = general_canvas.getClass().getField( Constants.isWindows?"handle":"embeddedHandle" );
					    	
					    		f_handle.setAccessible( true );
					    		
					    		long componentId = f_handle.getLong( general_canvas );
					    			    		
					    		videoSurfaceAdapter.attach( libvlc, mediaPlayer, componentId );
					    		
				    		}else{
				    			
				    			Field f_impl = mplayer_canvas.getClass().getDeclaredField( "impl" );
				    			
				    			f_impl.setAccessible( true );
				    			
				    			Object impl = f_impl.get( mplayer_canvas );
				    			
					    		Field f_handle = impl.getClass().getDeclaredField( "id" );
						    	
					    		f_handle.setAccessible( true );
					    		
					    		long componentId = f_handle.getLong( impl );
					    			    		
					    		videoSurfaceAdapter.attach( libvlc, mediaPlayer, componentId );
				    		}
				    		
				    	}catch( Throwable e ){
				    		
				    		Debug.out( e );
				    	}
				    }
				};					
			
			LibVlc libvlc = LibVlc.INSTANCE;
			
			libvlc_instance_t instance = libvlc.libvlc_new(0, null);

			EmbeddedMediaPlayer swtPlayer = 
				new DefaultEmbeddedMediaPlayer( libvlc, instance )
				{
					@Override
				    public void 
				    attachVideoSurface()
					{
				        vs.attach( libvlc, this );
				    }
				};
			
			vlcPlayer = swtPlayer;

		}else{
			
			FullScreenStrategy 		fss;

			if ( Constants.isWindows ){
				
				fss = new Win32FullScreenStrategy( awt_videoFrame );
								
			}else if ( Constants.isOSX ){
				
				fss = new DefaultFullScreenStrategy( awt_videoFrame );
				
			}else{
				
				fss = new DefaultFullScreenStrategy( awt_videoFrame );	
			}
			
			EmbeddedMediaPlayer embeddedPlayer = mpFactory.newEmbeddedMediaPlayer( fss );
		
			vlcPlayer = embeddedPlayer;
					
			CanvasVideoSurface cvs = new CanvasVideoSurface( awt_playerCanvas, vsa );
	
			embeddedPlayer.setVideoSurface(cvs);
		}
		
		vlcPlayer.setEnableMouseInputHandling(false);
		vlcPlayer.setEnableKeyInputHandling(false);

		vlcPlayer.addMediaPlayerEventListener(
			new MediaPlayerEventListener() {
				
				@Override
				public void volumeChanged(MediaPlayer player, float arg1) {
				}
				
				@Override
				public void
				videoOutput(
					final 				MediaPlayer player, 
					int 				arg1 ) 
				{					
					List<TrackInfo> tracks = player.getTrackInfo( TrackType.VIDEO );
					
					if ( tracks.size() > 0 ){
						
						VideoTrackInfo track = (VideoTrackInfo)tracks.get(0);
						
						final int height 	= track.height();
						final int width		= track.width();

						final float duration = player.getMediaMeta().getLength()/1000.0f;
						
						addJob(
							new Job( "reportVideoOutput( " + width + "/" + height + ": " + duration + " )" )
							{
								@Override
								public void
								exec()
								{	
									player_frame.setAspectRatio( ((float)width)/height );
									
									if ( metaDataListener != null ){
										
										metaDataListener.receivedDisplayResolution( width, height );
										metaDataListener.receivedVideoResolution( width, height );
									
										metaDataListener.receivedDuration( duration );
									}
									
									int	volume = player.getVolume();
									
									if ( preferences != null ){
										
										float seekTo = preferences.getPositionForFile(getOpenedFile()) - 2f;
										
										if ( seekTo > 0 && seekTo < 0.99 * duration && seekTo < duration - 20f ){
											
											doSeek( seekTo );
										}
										
										int	stored_volume = preferences.getVolume();
										
										if ( stored_volume != volume ){
							
											setVolume( stored_volume );
											
											volume = -1;	// don't re-report
										}
									}
									
									if ( volume >= 0 ){
									
										reportVolume( volume, true );
									}
									
									List<TrackInfo> text_tracks = player.getTrackInfo(TrackType.TEXT);
									
									Map<Integer,String>	spu_map = new HashMap<Integer, String>();
									
									List<TrackDescription>	spu_descs = player.getSpuDescriptions();
									
									for ( TrackDescription td: spu_descs ){
										
										spu_map.put( td.id(), td.description());
									}
									
									for ( TrackInfo ti: text_tracks ){
										
										TextTrackInfo info = (TextTrackInfo)ti;
										
										Language subtitle = new Language( LanguageSource.STREAM, "" + info.id());
										
										subtitle.setLanguage( info.language());
										
										Locale locale = subtitle.getLanguage();
																				
										String desc = spu_map.get( info.id());
										
										if ( desc != null ){
											
											subtitle.setName( desc );
											
										}else{
											
											String name = locale==null?info.language():locale.toString();

											subtitle.setName( name );
										}
																			
										if ( metaDataListener != null ){
										
											metaDataListener.foundSubtitle( subtitle );	
										}
									}
									
									player.setSpu( -1 );
									
									reportAudioTracks( player );
								}
							});
					}
				}
				
				@Override
				public void titleChanged(MediaPlayer arg0, int arg1) {
				}
				
				@Override
				public void timeChanged(MediaPlayer player, long arg1) {
					reportPosition( player.getTime()/1000f );
				}
				
				@Override
				public void subItemPlayed(MediaPlayer arg0, int arg1) {
				}
				
				@Override
				public void subItemFinished(MediaPlayer arg0, int arg1) {
				}
				
				@Override
				public void stopped(MediaPlayer arg0) {
					reportNewState(MediaPlaybackState.Stopped);
				}
				
				@Override
				public void snapshotTaken(MediaPlayer arg0, String arg1) {
				}
				
				@Override
				public void seekableChanged(MediaPlayer arg0, int arg1) {
				}
				
				@Override
				public void scrambledChanged(MediaPlayer arg0, int arg1) {
				}
				
				@Override
				public void positionChanged(MediaPlayer arg0, float arg1) {
				}
				
				@Override
				public void
				playing(
					MediaPlayer player) 
				{
					reportNewState(MediaPlaybackState.Playing);
				}
				
				@Override
				public void paused(MediaPlayer arg0) {
					reportNewState(MediaPlaybackState.Paused);
				}
				
				@Override
				public void pausableChanged(MediaPlayer arg0, int arg1) {
				}
				
				@Override
				public void opening(MediaPlayer arg0) {
					reportNewState(MediaPlaybackState.Opening);
				}
				
				@Override
				public void newMedia(MediaPlayer player) {
				}
				
				@Override
				public void muted(MediaPlayer arg0, boolean arg1) {
				}
				
				@Override
				public void mediaSubItemTreeAdded(MediaPlayer arg0, libvlc_media_t arg1) {
				}
				
				@Override
				public void mediaSubItemAdded(MediaPlayer arg0, libvlc_media_t arg1) {
				}
				
				@Override
				public void mediaStateChanged(MediaPlayer arg0, int arg1) {
				}
				
				@Override
				public void mediaParsedChanged(MediaPlayer arg0, int arg1) {
				}
				
				@Override
				public void mediaParsedStatus(MediaPlayer arg0, int arg1){
				}
				
				@Override
				public void mediaPlayerReady(MediaPlayer arg0){
				}
				
				@Override
				public void mediaMetaChanged(MediaPlayer arg0, int arg1) {
				}
				
				@Override
				public void mediaFreed(MediaPlayer arg0) {
				}
				
				@Override
				public void mediaDurationChanged(MediaPlayer arg0, long arg1) {
				}
				
				@Override
				public void mediaChanged(MediaPlayer arg0, libvlc_media_t arg1, String arg2) {
				}
				
				@Override
				public void lengthChanged(MediaPlayer arg0, long arg1) {
				}
				
				@Override
				public void forward(MediaPlayer arg0) {
				}
				
				@Override
				public void finished(MediaPlayer arg0) {
					reportNewState(MediaPlaybackState.Stopped);
				}
				
				@Override
				public void error(MediaPlayer arg0) {
					reportNewState(MediaPlaybackState.Failed);
				}
				
				@Override
				public void endOfSubItems(MediaPlayer arg0) {
				}
				
				@Override
				public void elementaryStreamSelected(MediaPlayer arg0, int arg1, int arg2) {
				}
				
				@Override
				public void elementaryStreamDeleted(MediaPlayer arg0, int arg1, int arg2) {
				}
				
				@Override
				public void elementaryStreamAdded(MediaPlayer arg0, int arg1, int arg2) {
				}
				
				@Override
				public void corked(MediaPlayer arg0, boolean arg1) {
				}
				
				@Override
				public void chapterChanged(MediaPlayer arg0, int arg1) {
				}
				
				@Override
				public void buffering(MediaPlayer arg0, float arg1) {
				}
				
				@Override
				public void backward(MediaPlayer arg0) {
				}
				
				@Override
				public void audioDeviceChanged(MediaPlayer arg0, String arg1) {
				}
			});
		
		return( glue );
	}
	
	private void
	reportAudioTracks(
		MediaPlayer	player )
	{
		List<TrackInfo> audio_tracks = player.getTrackInfo( TrackType.AUDIO );

		if ( audio_tracks.size() > 1 ){
			
			List<TrackDescription> descs = player.getAudioDescriptions();
			
			for ( TrackInfo ti: audio_tracks ){
				
				AudioTrackInfo	info = (AudioTrackInfo)ti;
						
				Language audio_track = new Language( LanguageSource.STREAM, "" + info.id());
				
				String language = info.language();
				
				if ( language == null || language.length() == 0 ){
					
					audio_track.setLanguage( "" );
					
				}else{
					
					audio_track.setLanguage( language );
				}
																	
				if ( metaDataListener != null ){
					
					metaDataListener.foundAudioTrack( audio_track );	
				}
			}
		}	
	}
	
	@Override
	public void
	setStateListener(
		StateListener listener)
	{
		stateListener	= listener;
	}
	
	private void 
	reportNewState(
		final MediaPlaybackState state ) 
	{
		addJob(
			new Job( "reportNewState( " + state + " )")
			{
				@Override
				public void
				exec()
				{
					if ( stateListener != null ){
						
						if ( state == MediaPlaybackState.Stopped ){
							
							stateListener.stateChanged( MediaPlaybackState.Paused );
						}
						
						stateListener.stateChanged( state );
					}
				}
			});

	}
	
	@Override
	public void
	setVolumeListener(
		VolumeListener listener)
	{
		volumeListener	= listener;
	}
	
	private void 
	reportVolume(
		final int 		volume,
		final boolean	initial_value )
	{
		addJob(
			new Job(  "reportVolume( " + volume + " )" )
			{
				@Override
				public void
				exec()
				{
					if ( !initial_value && preferences != null ){
						
						preferences.setVolume( volume );
					}
					
					if ( volumeListener != null ){
						
						volumeListener.volumeChanged( volume );
					}
				}
			});
	}
	
	@Override
	public void
	setMetaDataListener(
		MetaDataListener listener)
	{
		metaDataListener	= listener;
	}
	
	private void 
	reportSubtitleChanged(
		final String 			subtitleId,
		final LanguageSource 	source )
	{
		addJob(
			new Job( "reportSubtitleChanged( " + subtitleId + "/" + source + " )")
			{
				@Override
				public void
				exec()
				{
					if ( metaDataListener != null ){
						
						metaDataListener.activeSubtitleChanged( subtitleId, source );
					}
				}
			});
	}

	private void 
	reportAudioTrackChanged(
		final String 			audioId )
	{
		addJob(
			new Job( "activeAudioTrackChanged( " + audioId + " )")
			{
				@Override
				public void
				exec()
				{
					if ( metaDataListener != null ){
						
						metaDataListener.activeAudioTrackChanged( audioId );
					}
				}
			});
	}

	
	@Override
	public void
	setPositionListener(
		PositionListener listener)
	{
		positionListener	= listener;
	}
	
	private void 
	reportPosition(
		final float position ) 
	{
		addJob(
			new Job( "reportPosition( " + position + " )")
			{
				@Override
				public void
				exec()
				{
					if ( positionListener != null ){
						
						positionListener.positionChanged( position );
					}
				}
			});
	}
	
	@Override
	public void
	doOpen(
		final String fileOrUrl )
	{
		addJob(
			new Job( "doOpen( " + fileOrUrl + " )" )
			{
				@Override
				public void
				exec()
				{
					vlcPlayer.prepareMedia( fileOrUrl );
					vlcPlayer.parseMedia();
					
					vlcPlayer.play();
					
					reportNewState( MediaPlaybackState.Playing );
					
						// no video tracks means we get no decent event to trigger initial actions :(
					
					List<TrackInfo> video_tracks = vlcPlayer.getTrackInfo( TrackType.VIDEO );
					
					if ( video_tracks.size() == 0 ){
						
						final float duration = vlcPlayer.getMediaMeta().getLength()/1000.0f;
						
						addJob(
							new Job( "reportAudioOutput( " + duration + " )" )
							{
								@Override
								public void
								exec()
								{
									if ( metaDataListener != null ){
																			
										metaDataListener.receivedDuration( duration );
									}
									
									int	volume = vlcPlayer.getVolume();
									
									if ( preferences != null ){
										
										float seekTo = preferences.getPositionForFile(getOpenedFile()) - 2f;
										
										if ( seekTo > 0 && seekTo < 0.99 * duration && seekTo < duration - 20f ){
											
											doSeek( seekTo );
										}
										
										int	stored_volume = preferences.getVolume();
										
										if ( stored_volume != volume ){
							
											setVolume( stored_volume );
											
											volume = -1;	// don't re-report
										}
									}
									
									if ( volume >= 0 ){
									
										reportVolume( volume, true );
									}
									
									reportAudioTracks( vlcPlayer );
									
								}
							});
					}
				}
			});
	}
	
	@Override
	public void
	doPause()
	{
		addJob(
			new Job("doPause()")
			{
				@Override
				public void
				exec()
				{
					vlcPlayer.setPause( true );
					
					reportNewState( MediaPlaybackState.Paused );
				}
			});	
	}
	
	@Override
	public void
	doResume()
	{
		addJob(
			new Job("doResume()")
			{
				@Override
				public void
				exec()
				{
					vlcPlayer.setPause( false );
					
					reportNewState( MediaPlaybackState.Playing );
				}
			});
	}
	
	@Override
	public void
	doStop()
	{
		addJob(
			new Job("doStop()")
			{
				@Override
				public void
				exec()
				{
					if ( preferences != null ){
						
						preferences.setPositionForFile( getOpenedFile(), getPositionInSecs());
					}
					
					vlcPlayer.stop();	
					
					reportNewState( MediaPlaybackState.Stopped );
				}
			});	
	}
	
	@Override
	public void
	doRedraw()
	{
		
	}
	
	@Override
	public boolean 
	canSeekAhead()
	{	
		return true;
	}
	
	@Override
	public void
	doSeek(
		final float timeInSecs)
	{
		addJob(
			new Job("doSeek( " + timeInSecs + " )")
			{
				@Override
				public void
				exec()
				{
					float position = vlcPlayer.getTime()/1000.0f;
					
					float diff = timeInSecs - position;
					
					vlcPlayer.skip((long)( diff*1000 ));
					
					reportPosition( timeInSecs );
				}
			});	
		

	}
	
	@Override
	public void
	doSetVolume(
		final int volume)
	{
		addJob(
			new Job("doSetVolume( " + volume + ")")
			{
				@Override
				public void
				exec()
				{
					vlcPlayer.setVolume( volume );
					
					reportVolume( volume, false );	
				}
			});	
	}	
	
	@Override
	public void
	doLoadSubtitlesFile(
		final String 		file,
		boolean 			autoPlay )
	{
		addJob(
			new Job("doLoadSubtitlesFile( " + file + ")")
			{
				@Override
				public void
				exec()
				{
					vlcPlayer.setSubTitleFile( file );
				}
			});		
	}
	
	@Override
	public void
	setAudioTrack(
		final Language language) 
	{
		addJob(
			new Job("setAudioTrack( " + ( language == null?"null":language.getId()) + ")")
			{
				@Override
				public void
				exec()
				{
					int id = language==null?-1:Integer.parseInt( language.getId());
					
					vlcPlayer.setAudioTrack(id);
					
					reportAudioTrackChanged( String.valueOf( id ));
				}
			});	
	}
	
	@Override
	public void
	setSubtitles(
		final Language language) 
	{	
		addJob(
			new Job("setSubtitles( " + ( language == null?"null":language.getId()) + ")")
			{
				@Override
				public void
				exec()
				{
					int id = language==null?-1:Integer.parseInt( language.getId());
					
					vlcPlayer.setSpu( id );
					
					if ( language == null ){
						
						reportSubtitleChanged( "-1", null );
						
					}else{
					
						reportSubtitleChanged( language.getId(), language.getSource());
					}
				}
			});	
	}
	
	@Override
	public void
	mute(
		final boolean on) 
	{
		addJob(
			new Job("doMute( " + on + ")")
			{
				@Override
				public void
				exec()
				{
					vlcPlayer.mute( on );
				}
			});	
	}
	
	@Override
	public void
	showMessage(
		String message, int duration) 
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void
	dispose()
	{
		addJob(
			new Job("doDispose()")
			{
				@Override
				public void
				exec()
				{
					if ( vlcPlayer != null ){
						
						vlcPlayer.stop();
						vlcPlayer.release();
	
						vlcPlayer = null;
					}
				}
			});
	}
	
	private static final boolean	JOBS_RUN_ON_SWING_THREAD = false;
	
	private static void
	addJob(
		Job	job )
	{
		addJob( job, false );
	}
	
	private static void
	addJob(
		Job			job,
		boolean		blocking )
	{
		boolean	run_now = false;
		
		synchronized( jobs ){
		
			if ( blocking ){
				
				if ( job_depth > 0 ){
					
					run_now = true;
					
				}else{
					
					if ( JOBS_RUN_ON_SWING_THREAD && SwingUtilities.isEventDispatchThread()){
						
						run_now	= true;
						
					}else if ( (!JOBS_RUN_ON_SWING_THREAD) &&  Utils.isSWTThread()){
						
						run_now = true;
					}
				}
			}
		}
		
		if ( run_now ){
			
			try{
				//System.out.println( job.toString() + " (immediate)" );
				
				job.run();
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
			
			return;
		}
		
		synchronized( jobs ){
			
			jobs.addLast( job );
		}
	
		dispatcher.dispatch(
			new AERunnable() {
				
				@Override
				
				public void 
				runSupport()
				{
					try{
						synchronized( jobs ){
							
							job_depth++;
						}
					
						while( true ){
							
							Job job;
							
							synchronized( jobs ){
							
								if ( jobs.isEmpty()){
									
									return;
								}
								
								job = jobs.removeFirst();
							}
							
							try{
								//System.out.println( job.toString());
								
								job.run();
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					}finally{
						
						synchronized( jobs ){
							
							job_depth--;
						}
					}
				}
			});
		
		if ( blocking ){
			
			job.waitFor();
		}
	}
	
	private static abstract class
	Job
		implements Runnable
	{
		private String		name;
		
		private AESemaphore sem = new AESemaphore( "VLC:job" );
		
		private
		Job(
			String		_name )
		{
			name	= _name;
		}
		
		@Override
		public void
		run()
		{
			if ( JOBS_RUN_ON_SWING_THREAD ){
				
				if ( !SwingUtilities.isEventDispatchThread()){
				
					try{
						SwingUtilities.invokeAndWait( this );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}else{
				
					try{
						exec();
						
					}finally{
						
						sem.releaseForever();
					}
				}
			}else{
			
				Utils.execSWTThread(
					new Runnable()
					{
						@Override
						public void
						run()
						{
							try{
								exec();
								
							}finally{
								
								sem.releaseForever();
							}
						}
					});
				
				/*
				try{
					exec();
					
				}finally{
					
					sem.releaseForever();
				}
				*/
			}
		}
		
		protected abstract void
		exec();
		
		private void
		waitFor()
		{
			sem.reserve();
		}
		
		public String
		toString()
		{
			return( name );
		}
	};	
}