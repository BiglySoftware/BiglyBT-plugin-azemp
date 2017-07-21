/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.azureus.plugins.azemp.ui.swt.emp;

import java.io.File;
import java.net.URL;
import java.util.Map;


import com.biglybt.core.download.DownloadManager;
import com.biglybt.core.torrent.TOTorrent;
import com.biglybt.core.util.*;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.disk.DiskManagerFileInfo;
import com.biglybt.pif.download.Download;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.components.shell.ShellManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

import com.azureus.plugins.azemp.*;
import com.vuze.mediaplayer.MediaPlaybackState;
import com.vuze.mediaplayer.PlayerPreferences;
import com.vuze.mediaplayer.StateListener;
import com.vuze.mediaplayer.mplayer.MPlayer;
import com.vuze.mediaplayer.swt.Player;
import com.vuze.mediaplayer.vlc.VLCPlayer;

/**
 * @author TuxPaper
 * @created Aug 6, 2007
 *
 *	TODO: Put all on new thread, call SWT thread when needed
 */
public class EmbeddedPlayerWindowSWT
{
	public static PluginInterface pluginInterface;
	
	private static EmbeddedPlayerWindowSWT alreadyRunningWin = null;

	private static AEMonitor alreadyRunningWin_mon = new AEMonitor(
			"alreadyRunningWin");

	
		/////////////////////////////////////////////////////////////////////////

	public static boolean
	isPlayerActive()
	{
		try {
			alreadyRunningWin_mon.enter();

			if ( alreadyRunningWin == null ){
				
				return( false );
			}
			
			return( alreadyRunningWin.isActive());
			
		} finally {
			alreadyRunningWin_mon.exit();
		}
	}
	
	private static void
	checkMPlayer()
		throws Exception
	{
		if (!Constants.isWindows && !Constants.isOSX) {
			boolean	use_mplayer = EmbeddedMediaPlayerPlugin.useMPlayer();
			if ( use_mplayer ){
				throw new Exception("EMP Windows and OSX Only");
			}
		}
	}
	public static EmbeddedPlayerWindowSWT openWindow(DownloadManager dm)
			throws Throwable {
		return openWindow(dm, dm.getTorrent(), null);
	}

	public static EmbeddedPlayerWindowSWT openWindow(DownloadManager dm, int file_index )
		throws Throwable {
		return openWindow(dm, file_index, dm.getTorrent(), null);
	}
	
	public static EmbeddedPlayerWindowSWT openWindow(DownloadManager dm,
	  TOTorrent torrent, String runfile) throws Throwable {
		return( openWindow(dm,-1,torrent,runfile ));
	}
	
	private static EmbeddedPlayerWindowSWT openWindow(DownloadManager dm, int file_index,
			TOTorrent torrent, String runfile) throws Throwable {
		checkMPlayer();

		try {
			alreadyRunningWin_mon.enter();
			try {
				if (alreadyRunningWin == null) {
					alreadyRunningWin = new EmbeddedPlayerWindowSWT();
				}
				
			} finally {
				alreadyRunningWin_mon.exit();
			}

			alreadyRunningWin.init(dm, file_index, torrent, runfile);
			return alreadyRunningWin;

		} catch (Throwable t) {
			throw t;
		}
	}

	public static EmbeddedPlayerWindowSWT openWindow( File file, String name ) throws Throwable {
		checkMPlayer();

		try {
			alreadyRunningWin_mon.enter();
			try {
				if (alreadyRunningWin == null) {
					alreadyRunningWin = new EmbeddedPlayerWindowSWT();
				}
				
			} finally {
				alreadyRunningWin_mon.exit();
			}

			alreadyRunningWin.init( file, name );
			return alreadyRunningWin;

		} catch (Throwable t) {
			throw t;
		}
	}
	
	
	
	public static EmbeddedPlayerWindowSWT openWindow( URL url, String name ) throws Throwable {
		checkMPlayer();

		try {
			alreadyRunningWin_mon.enter();
			try {
				if (alreadyRunningWin == null) {
					alreadyRunningWin = new EmbeddedPlayerWindowSWT();
				}
				
			} finally {
				alreadyRunningWin_mon.exit();
			}

			alreadyRunningWin.init( url, name );
			return alreadyRunningWin;

		} catch (Throwable t) {
			throw t;
		}
	}
	
	
	// Streaming APIs
	
	public static EmbeddedPlayerWindowSWT 
	prepareWindow( 
		String 		name ) 
		throws Throwable 
	{
		checkMPlayer();

		alreadyRunningWin_mon.enter();
		
		try{
			if ( alreadyRunningWin == null ){
				
				alreadyRunningWin = new EmbeddedPlayerWindowSWT();
			}
		}finally{
			
			alreadyRunningWin_mon.exit();
		}

		alreadyRunningWin.prepare( name );
		
		return alreadyRunningWin;
	}
	
	public static File getPrimaryFile(Download d) {
		Debug.out( "This should never be called!!!!" );
		long size = d.getTorrent().getSize();
		DiskManagerFileInfo[] infos = d.getDiskManagerFileInfo();
		for(int i = 0; i < infos.length ; i++) {
			if(infos[i].getLength() > 90*size / 100l) {
				return infos[i].getFile( true );
			}
		}
		return null;
	}
	
	/////////////////////////////////////////////////////////////////////////
	
	
	private Shell shell;
	private Player player;
	
	private AESemaphore init_sem = new AESemaphore( "EMP:init" );
	
	public EmbeddedPlayerWindowSWT() {
		Utils.execSWTThread(new AERunnable() {
			
			@Override
			public void runSupport() {
				try {
					shell = new Shell();
					ShellManager.sharedManager().addWindow(shell);//ShellFactory.createShell((Display)null);
					Utils.setShellIcon(shell);
					shell.setLayout(new FillLayout());
					
					shell.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_BLACK));
					
					PlayerPreferences preferences = EmbeddedMediaPlayerPlugin.getPlayerPreferences();
					
					
					if(preferences != null) {
						shell.setLocation(preferences.getWindowPosition());
					} else {
						shell.setLocation(100,100);
					}
					
					shell.setSize(600,400);
					
					shell.setText("Loading...");
					
					shell.open();
					
					boolean	use_mplayer = EmbeddedMediaPlayerPlugin.useMPlayer();
					
					if ( !use_mplayer ){
						
						if ( !VLCPlayer.initialise()){
							
							EmbeddedMediaPlayerPlugin.reportVLCProblem();
							
							use_mplayer = true;
						}
					}
					
					if ( use_mplayer ){
					
						player = new Player(new MPlayer( preferences), shell );
						
					}else{
						
						player = new Player(new VLCPlayer( preferences), shell );

					}
					
					player.setAutoResize(true);
					
					player.addStateListener(new StateListener() {
						
						boolean	failed = false;
						
						@Override
						public void stateChanged(MediaPlaybackState newState) {
							
							if ( 	newState == MediaPlaybackState.Uninitialized || 
									newState == MediaPlaybackState.Opening ){
								
								failed = false;
								
							}else if ( newState == MediaPlaybackState.Failed ){
								
								failed = true;
								
							}else if ( newState == MediaPlaybackState.Closed ) {
								
								if ( !( failed || player.getDurationInSeconds() == 0 )){
									shell.getDisplay().asyncExec(new Runnable() {
										
										@Override
										public void run() {
											shell.close();
											
										}
									});
								}
							}
						}
					});
					
					shell.addListener(SWT.Close, new Listener() {
						@Override
						public void handleEvent(Event arg0) {
							if ( player != null ){
								player.stop();
							}
							alreadyRunningWin_mon.enter();
							try {
								alreadyRunningWin = null;
							} finally {
								alreadyRunningWin_mon.exit();
							}
						}
					});
					
				} catch( Throwable e ){
					
					e.printStackTrace();
					
				}finally{
					
					init_sem.releaseForever();
				}
			}
		});
		
	}

	
	public void init(final DownloadManager dm,final int file_index,final TOTorrent torrent,
			String initialRunfile) throws Throwable {
			Utils.execSWTThreadLater(0, new AERunnable() {
			
			@Override
			public void runSupport() {
				try {
					if ( shell.isDisposed()){
						
						return;
					}
					
					Download download = pluginInterface.getDownloadManager().getDownload(torrent.getHash());
					if(download != null) {
						File toPlay = file_index==-1?getPrimaryFile(download):download.getDiskManagerFileInfo( file_index ).getFile( true );
						if(toPlay != null) {
							shell.setText(toPlay.getName());
							player.open(toPlay.getAbsolutePath(), false);
							shell.setActive();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			});
	}
	
	public void init( final File file, final String name ) throws Throwable {
		Utils.execSWTThreadLater(0, new AERunnable() {
		
		@Override
		public void runSupport() {
			try {
				if ( shell.isDisposed()){
					
					return;
				}
				
				shell.setText( name );
				player.open( file.getAbsolutePath(), false );
				shell.setActive();
		
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		});
	}
	
	public void init( final URL url, final String name ) throws Throwable {
			Utils.execSWTThreadLater(0, new AERunnable() {
			
			@Override
			public void runSupport() {
				try {
					if ( shell.isDisposed()){
						
						return;
					}
					
					shell.setText( name );
					player.open( url.toExternalForm(), false );
					shell.setActive();
			
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			});
	}
	
	
	
	public void 
	prepare( 
		final String name ) 
	
		throws Throwable
	{
		Utils.execSWTThreadLater(
			0, new AERunnable() 
			{		
				@Override
				public void runSupport() {
					try{
						if ( shell.isDisposed()){
							
							return;
						}
						
						shell.setText( name );
							
						player.prepare();
						
						shell.setActive();
				
					}catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
	}
	
	public void
	setMetaData(
		final Map<String,Object>		map )
	{		
		Utils.execSWTThreadLater(
				0, 
				new AERunnable() 
				{
					@Override
					public void
					runSupport() 
					{
						if ( shell.isDisposed()){
							
							return;
						}
						
						try {
							player.setSize(((Long)map.get( "width")).intValue(), ((Long)map.get( "height" )).intValue());
							
							player.setDurationInSeconds(((Long)map.get( "duration" )).floatValue()/1000);
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				});
	}
	
	public void 
	startPlayback( 
		final URL url )
	
		throws Throwable
	{
		Utils.execSWTThreadLater(
			0, new AERunnable() 
			{		
				@Override
				public void runSupport() {
					
					if ( shell.isDisposed()){
						
						return;
					}
					
					try{	
						String	target = url.toExternalForm();

						PlayerPreferences preferences = EmbeddedMediaPlayerPlugin.getPlayerPreferences();

						if ( preferences != null ){
							
							preferences.setPositionForFile( target, 0 );
						}
						
						player.open( url.toExternalForm(), true );
						
						shell.setActive();
				
					}catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
	}
	
	public void
	pausePlayback()
	{
		Utils.execSWTThreadLater(
			0, 
			new AERunnable() 
			{
				@Override
				public void
				runSupport() 
				{
					if ( shell.isDisposed()){
						
						return;
					}
					
					try {
						player.pause();
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			});
	}
	
	public void
	resumePlayback()
	{
		Utils.execSWTThreadLater(
			0, 
			new AERunnable() 
			{
				@Override
				public void
				runSupport() 
				{
					if ( shell.isDisposed()){
						
						return;
					}
					
					try {
						player.resume();
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			});
	}
	
	public void
	bufferingPlayback(
		final Map<String,Object>		map )
	{
		Utils.execSWTThreadLater(
			0, 
			new AERunnable() 
			{
				@Override
				public void
				runSupport() 
				{
					if ( shell.isDisposed()){
						
						return;
					}
					
					try{
						player.buffering( map );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			});
	}
	
	public void
	playStats(
		final Map<String,Object>		map )
	{
		Utils.execSWTThreadLater(
			0, 
			new AERunnable() 
			{
				@Override
				public void
				runSupport() 
				{
					if ( shell.isDisposed()){
						
						return;
					}
					
					try{
						player.playStats( map );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			});
	}
	
	public boolean
	isActive()
	{
		if ( shell.isDisposed()){
			
			return( false );
		}	
		
		if ( player == null ){
			
			init_sem.reserve(2*60*1000);
			
			if ( player == null ){
			
				return( false );
			}
		}
		
		return( player.isActive());
	}
}