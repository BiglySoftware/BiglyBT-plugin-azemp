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


package com.vuze.mediaplayer.mplayer;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import com.biglybt.core.util.AEThread2;
import com.biglybt.core.util.Constants;
import com.biglybt.pifimpl.local.utils.LocaleUtilitiesImpl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.vuze.mediaplayer.MediaPlaybackState;
import com.vuze.mediaplayer.StateListener;
import com.vuze.mediaplayer.swt.Player;

public class MPlayerTest {
	public static void main(String[] args) throws Exception {
		
		boolean OSX = Constants.isOSX;
		
		File player_binary = new File( OSX?"/Users/parg/Documents/workspace/azemp/vuzeplayer":"C:\\Test\\plus\\plugins\\azemp\\vuzeplayer.exe");
		String test_file	= OSX?"/Users/parg/Downloads/test.mov":"C:\\Downloads\\Work\\transcodes\\Android_4.2.2\\1_Democracy_Now!_2014-09-11_Thursday[V011441414].mp4";
		
		MPlayer.initialise( player_binary );
		
		ResourceBundle bundle = 
			ResourceBundle.getBundle( 
				// "com/azureus/plugins/azemp/skins/skin",
				"com/azureus/plugins/azemp/internat/Messages",
				Locale.getDefault(), Player.class.getClassLoader());
		
		new LocaleUtilitiesImpl(null).integrateLocalisedMessageBundle( bundle );
		
		System.out.println(SWT.getVersion());
		final Display display = new Display();
		final Shell shell = new Shell();
		shell.setLocation(200,200);
		shell.setSize(500,250);
		
		shell.setText("Loading...");
		shell.setLayout(new FillLayout());
		
		//shell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		
		final Player player = new Player(new MPlayer(), shell);

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
		
		player.open( test_file, true );
		
		new AEThread2( "asas" )
		{
			@Override
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
		
		shell.open();
		
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

}
