/*
 * Created on Mar 16, 2016
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


import java.awt.Canvas;
import java.awt.Frame;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JFrame;

import com.biglybt.core.util.Constants;
import com.biglybt.pifimpl.local.utils.LocaleUtilitiesImpl;
import com.biglybt.ui.swt.mainwindow.Colors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibX11;
import uk.co.caprica.vlcj.binding.internal.libvlc_instance_t;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.DefaultMediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.DefaultEmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.linux.LinuxVideoSurfaceAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.mac.MacVideoSurfaceAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.windows.WindowsVideoSurfaceAdapter;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.version.LibVlcVersion;

import com.vuze.mediaplayer.MediaPlaybackState;
import com.vuze.mediaplayer.StateListener;
import com.vuze.mediaplayer.swt.Player;
import com.vuze.swt.MPlayerRendererCanvas;




public class 
VLCPlayerTest 
{
	public static void 
	main(
		String[] args) 
	
		throws Exception 
	{
		
		boolean OSX = Constants.isOSX;
		
		final String test_file;
		
		if ( Constants.isWindows ){
			
			//test_file = "C:\\temp\\count_audio.mp4";
			test_file = "C:\\fark\\A_glimpse_of_the_future_through_an_augmented_reali[V005546181].mp4";
			
		}else if ( Constants.isOSX ){
			
			//test_file = "/Users/parg/Downloads/test.mov";
			test_file = "/Users/vuze/Downloads/test.mp4";
			
		}else{
			
			test_file = "/home/paul/Vuze Downloads/sintel.mp4";
		}
			
		boolean	use_vuze_player	= true;
		
		if ( use_vuze_player ){
			ResourceBundle bundle = 
				ResourceBundle.getBundle( 
					// "com/azureus/plugins/azemp/skins/skin",
					"com/azureus/plugins/azemp/internat/Messages",
					Locale.getDefault(), VLCPlayerTest.class.getClassLoader());
			
			new LocaleUtilitiesImpl(null).integrateLocalisedMessageBundle( bundle );
			
			System.out.println(SWT.getVersion());
			final Display display = new Display();
			final Shell shell = new Shell();
					
			shell.setLayout( new FillLayout());
					
			final Player player = new Player(new VLCPlayer(), shell);
	
			player.setAutoResize(true);
	
			player.addStateListener(new StateListener() {
				
				@Override
				public void stateChanged(MediaPlaybackState newState) {
					if(newState == MediaPlaybackState.Closed) {
						shell.getDisplay().asyncExec(new Runnable() {
							
							@Override
							public void run() {
								shell.close();
								
							}
						});					
					}
				}
			});
			
			//shell.setFullScreen(true);
					
			/*
			new AEThread2( "asas" )
			{
				public void
				run()
				{
					Map	map = new HashMap();
					
					map.put( "state", 2 );
					map.put( "dl_rate", 100L );
					map.put( "dl_size", 1000000L );
					map.put( "stream_rate", 200L );
					
					int		eta 	= 10;
					long	elapsed = 100;
					
					while( true ){
						
						map.put( "eta", eta-- );
						map.put( "dl_time", (elapsed++)*1000 );
						
						player.buffering( map );
						
						try{
							
							Thread.sleep(1000);
							
						}catch( Throwable e ){
						}
					}
				}
			}.start();
			*/
			
			shell.open();
			
			boolean stream_mode = false;
			
			player.open( test_file, stream_mode );
			
			while(!shell.isDisposed()) {
				if(!display.readAndDispatch()) {
					display.sleep();
				}
			}
			
		}else{
			
			boolean use_swt = true;
			boolean	swt_awt	= false;
			
			if ( Constants.isWindows || Constants.isOSX ){
				
			}else{
			
					System.out.println( "XInitThreads: " + LibX11.INSTANCE.XInitThreads());
			}
		

			if ( use_swt ){
				
				if ( swt_awt ){
					System.out.println("SWT version: " + SWT.getVersion());
					Display display = new Display();
					Shell  shell = new Shell(display, SWT.SHELL_TRIM );        
					shell.setLayout(new FillLayout());
					shell.setSize(800, 600);  
					shell.setText( "SWT Frame" );
					
					Composite  videoPanel = new Composite (shell, SWT.EMBEDDED);
	
					Frame videoFrame = SWT_AWT.new_Frame(videoPanel);    
					
					Canvas videoSurfaceCanvas = new Canvas();
	
					videoSurfaceCanvas.setBackground(java.awt.Color.black);
					videoFrame.add(videoSurfaceCanvas);
	
					new NativeDiscovery().discover();
					
					System.out.println( "Version: " + LibVlcVersion.getVersion());
					
					MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory("--no-video-title-show");
					
					EmbeddedMediaPlayer mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer();
	
					CanvasVideoSurface videoSurface = mediaPlayerFactory.newVideoSurface(videoSurfaceCanvas);
					mediaPlayer.setVideoSurface(videoSurface);
	
					shell.open();
					
					mediaPlayer.playMedia( test_file );         
	
					while(!shell.isDisposed()) {
						if(!display.readAndDispatch()) {
							display.sleep();
						}
					}
					
					mediaPlayer.release();
					mediaPlayerFactory.release();
					display.dispose();   
					
				}else{
					System.out.println("SWT version: " + SWT.getVersion());
					Display display = new Display();
					Shell  shell = new Shell(display, SWT.SHELL_TRIM );        
					shell.setLayout(new FillLayout());
					shell.setSize(800, 600);  
					shell.setText( "SWT Frame" );

					new NativeDiscovery().discover();
					
					System.out.println( "Version: " + LibVlcVersion.getVersion());

					Composite videoSurface = new Composite(shell, SWT.EMBEDDED | SWT.NO_BACKGROUND);
					//videoSurface.setLayout(gridLayout);
					//videoSurface.setLayoutData(gridData);
					//videoSurface.setBackground(black);

					LibVlc libvlc = LibVlc.INSTANCE;
					libvlc_instance_t instance = libvlc.libvlc_new(0, null);

					SwtEmbeddedMediaPlayer mediaPlayer = new SwtEmbeddedMediaPlayer(libvlc, instance);
					mediaPlayer.setVideoSurface(new CompositeVideoSurface(videoSurface, getVideoSurfaceAdapter()));

					shell.open();

					mediaPlayer.playMedia( test_file );

					while (!shell.isDisposed()) {
						if (!display.readAndDispatch()) {
							display.sleep();
						}
					}

					display.dispose();
				}
			}else{
				
				Display display = new Display();
				Shell  shell = new Shell(display, SWT.SHELL_TRIM );        
				shell.setLayout(new FillLayout());
				shell.setSize(800, 600);  
				shell.setText( "SWT Frame" );

				shell.open();
				
				new Thread()
				{
					@Override
					public void
					run()
					{
						try{
							System.out.println( "Using JFrame" );
							
							JFrame videoFrame = new JFrame( "JFrame" );
							videoFrame.setSize(800, 600 );
											
							Canvas videoSurfaceCanvas = new Canvas();
				
							videoSurfaceCanvas.setBackground(java.awt.Color.black);
							videoFrame.add(videoSurfaceCanvas);
				
							if ( Constants.isWindows || Constants.isOSX ){
								
							}else{
							
								System.out.println( "XInitThreads: " + LibX11.INSTANCE.XInitThreads());
							}
						
							new NativeDiscovery().discover();
							
							System.out.println( "Version: " + LibVlcVersion.getVersion());
							
							MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory("--no-video-title-show");
							
							EmbeddedMediaPlayer mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer();
				
							CanvasVideoSurface videoSurface = mediaPlayerFactory.newVideoSurface(videoSurfaceCanvas);
							mediaPlayer.setVideoSurface(videoSurface);
					
							videoFrame.setVisible( true );
											
							mediaPlayer.playMedia( test_file );
							
							while( true ){
								
								Thread.sleep(1000);
								
								if ( false ){
									break;
								}
							}
							
							mediaPlayer.release();
							mediaPlayerFactory.release();
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
				}.start();
	
				while(!shell.isDisposed()) {
					if(!display.readAndDispatch()) {
						display.sleep();
					}
				}
			}
		}
	}
	
	   private static VideoSurfaceAdapter getVideoSurfaceAdapter() {
	        VideoSurfaceAdapter videoSurfaceAdapter;
	        if(RuntimeUtil.isNix()) {
	            videoSurfaceAdapter = new LinuxVideoSurfaceAdapter();
	        }
	        else if(RuntimeUtil.isWindows()) {
	            videoSurfaceAdapter = new WindowsVideoSurfaceAdapter();
	        }
	        else if(RuntimeUtil.isMac()) {
	            videoSurfaceAdapter = new MacVideoSurfaceAdapter();
	        }
	        else {
	            throw new RuntimeException("Unable to create a media player - failed to detect a supported operating system");
	        }
	        return videoSurfaceAdapter;
	    }
	static class SwtEmbeddedMediaPlayer extends DefaultEmbeddedMediaPlayer {

	    private CompositeVideoSurface videoSurface;

	    public SwtEmbeddedMediaPlayer(LibVlc libvlc, libvlc_instance_t instance) {
	        super(libvlc, instance);
	    }

	    public void setVideoSurface(CompositeVideoSurface videoSurface) {
	        this.videoSurface = videoSurface;
	    }

	    @Override
	    public void attachVideoSurface() {
	        videoSurface.attach(libvlc, this);
	    }
	}
	
	static class CompositeVideoSurface extends VideoSurface {

	    private final 	Composite composite;
	    private final   MPlayerRendererCanvas canvas;
	    
	    public CompositeVideoSurface(Composite composite, VideoSurfaceAdapter videoSurfaceAdapter) {
	        super(videoSurfaceAdapter);
	        
	        composite.setLayout( new GridLayout());
	        
	        canvas = new MPlayerRendererCanvas(composite,SWT.NONE);
	        
	        canvas.setLayoutData( new GridData(  GridData.FILL_BOTH ));
	        
	        canvas.setForeground( Colors.red );
	        this.composite = composite;
	    }

	    @Override
	    public void attach(LibVlc libvlc, MediaPlayer mediaPlayer) {
	    	
	    	try{
	    		if ( Constants.isOSX ){
	    			
	    			String[] opts = canvas.getExtraMplayerOptions();
	    			
	    			System.out.println( opts[0] );
	    			
	    			Field f_impl = canvas.getClass().getDeclaredField( "impl" );
	    			
	    			f_impl.setAccessible( true );
	    			
	    			Object impl = f_impl.get( canvas );
	    			
		    		Field f_handle = impl.getClass().getDeclaredField( "id" );
			    	
		    		f_handle.setAccessible( true );
		    		
		    		long componentId = f_handle.getLong( impl );
		    			    		
		    		videoSurfaceAdapter.attach(libvlc, mediaPlayer, componentId);
	    		}else{
		    		Field f_handle = composite.getClass().getField( ( Constants.isWindows || Constants.isOSX )? "handle":"embeddedHandle" );
		    	
		    		f_handle.setAccessible( true );
		    		
		    		long componentId = f_handle.getLong( composite );
		    			    		
		    		videoSurfaceAdapter.attach(libvlc, mediaPlayer, componentId);
	    		}
	    	}catch( Throwable e ){
	    		
	    		e.printStackTrace();
	    	}
	    }
	}
}
