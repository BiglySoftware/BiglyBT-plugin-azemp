/*
 * Created on Nov 1, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
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


package com.azureus.plugins.azemp.ui.swt.emp;

import java.io.File;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.azureus.plugins.azemp.EmbeddedMediaPlayerPlugin;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.Base32;
import com.biglybt.core.util.Debug;
import com.biglybt.pifimpl.local.PluginCoreUtils;
import com.biglybt.ui.UIFunctionsManager;
import com.biglybt.ui.mdi.MdiEntry;
import com.biglybt.ui.mdi.MultipleDocumentInterface;
import com.biglybt.ui.swt.FixedURLTransfer.URLType;
import com.biglybt.ui.swt.Messages;
import com.biglybt.ui.swt.pif.*;
import com.biglybt.ui.swt.views.skin.TorrentListViewsUtils;

import com.biglybt.pif.disk.DiskManagerFileInfo;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.ui.UIInstance;
import com.biglybt.pif.ui.UIManager;
import com.biglybt.pif.ui.menus.MenuItem;
import com.biglybt.pif.ui.menus.MenuManager;

public class 
EmbeddedPlayerPluginView 
{
	private EmbeddedMediaPlayerPlugin	plugin;
	
	private String	view_name = MessageText.getString( "azemp.name" );
	private MenuItem mainMenuItem;
	
	public 
	EmbeddedPlayerPluginView(
		EmbeddedMediaPlayerPlugin		_plugin,
		UISWTInstance _swt )
	{
		plugin	= _plugin;

		UISWTViewBuilder viewBuilder = _swt.createViewBuilder(view_name,
				ViewEventListener.class).setParentEntryID(
						MultipleDocumentInterface.SIDEBAR_HEADER_DEVICES);
		_swt.registerView(UISWTInstance.VIEW_MAIN, viewBuilder);

			UIManager ui_manager = plugin.getPluginInterface().getUIManager();

			MenuManager menu_manager = ui_manager.getMenuManager();

			mainMenuItem = menu_manager.addMenuItem( MenuManager.MENU_MENUBAR, "azemp.name" );
			mainMenuItem.setDisposeWithUIDetach(UIInstance.UIT_SWT);

		mainMenuItem.addListener((menu, target) -> _swt.openView(
				UISWTInstance.VIEW_MAIN, view_name, null, true));

		
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi != null) {
			mdi.loadEntryByID(view_name, false, true, null);
		}
	}
	
	public void
	destroy()
	{
		try {
  		if (mainMenuItem != null) {
  			mainMenuItem.remove();
  		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class ViewEventListener implements UISWTViewEventListener {

		private EmbeddedMediaPlayerPlugin plugin;

		@Override
		public boolean
		eventOccurred(
			UISWTViewEvent event )
		{
			switch( event.getType()){
			
				case UISWTViewEvent.TYPE_CREATE:{

					UISWTView view = event.getView();
					plugin = (EmbeddedMediaPlayerPlugin) view.getPluginInterface().getPlugin();
					view.setControlType( UISWTView.CONTROLTYPE_SWT );
					
					if (view instanceof MdiEntry) {
						MdiEntry entry = (MdiEntry) view;
						entry.setImageLeftID("image.sidebar.azemp");
						entry.addListener((e, payload) -> {
							handleDrop(payload);
							return true;
						});
					}
				
					break;
				}
				case UISWTViewEvent.TYPE_INITIALIZE:{
												
					Composite composite = (Composite)event.getData();
					
					Composite main = new Composite(composite, SWT.NONE);
					GridLayout layout = new GridLayout();
					layout.numColumns = 1;
					layout.marginHeight = 8;
					layout.marginWidth = 8;
					main.setLayout(layout);
					GridData grid_data = new GridData(GridData.FILL_BOTH );
					main.setLayoutData(grid_data);
					
					 Label label = new Label( main, SWT.WRAP );
					 
					 Messages.setLanguageText( label, "azemp.play.drop");

					Transfer[] transferList = new Transfer[] {
							//HTMLTransfer.getInstance(),
							//URLTransfer.getInstance(),
							FileTransfer.getInstance(),
							TextTransfer.getInstance()
						};

					final DropTarget dropTarget = 
						new DropTarget(
							composite, DND.DROP_DEFAULT	| DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE );
					
					dropTarget.setTransfer(transferList);
					
					dropTarget.addDropListener(
						new DropTargetAdapter()
						{									
							@Override
							public void
							drop(
								DropTargetEvent event) 
							{
								handleDrop( event.data );	
							}
						});
					
					break;
				}
			}
			
			return( true );
		}

		private void
		handleDrop(
				Object		data )
		{
			String	str = null;

			if ( data instanceof String ){

				str = (String)data;

			}else if ( data instanceof String[]){

				String[] temp = (String[])data;

				if ( temp.length > 0 ){

					str = temp[0];
				}
			}else if ( data instanceof URLType){

				str = ((URLType)data).linkURL;
			}

			if ( str != null ){

				if ( str.startsWith( "DownloadManager\n" ) || str.startsWith( "DiskManagerFileInfo\n" )){

					String[]	bits = str.split( "\n" );

					for (int i=1;i<bits.length;i++){

						String	hash_str = bits[i];

						int	pos = hash_str.indexOf(';');

						try{

							if ( pos == -1 ){

								byte[]	 hash = Base32.decode( bits[i] );

								Download dl = plugin.getPluginInterface().getDownloadManager().getDownload( hash );

								if ( dl != null ){

									DiskManagerFileInfo info = dl.getPrimaryFile();

									int file_index = info.getIndex();

									TorrentListViewsUtils.playOrStream( PluginCoreUtils.unwrap( dl ), file_index, info.getLength() == info.getDownloaded(), true );
								}
							}else{

								String[] files = hash_str.split(";");

								if ( files.length > 1 ){

									byte[]	 hash = Base32.decode( files[0].trim());

									Download dl = plugin.getPluginInterface().getDownloadManager().getDownload( hash );

									if ( dl != null ){

										int file_index = Integer.parseInt(files[1].trim());

										DiskManagerFileInfo info = dl.getDiskManagerFileInfo()[file_index];

										TorrentListViewsUtils.playOrStream( PluginCoreUtils.unwrap( dl ), file_index, info.getLength() == info.getDownloaded(), true );
									}
								}
							}
						}catch( Throwable e ){

							Debug.out( "Failed to get download for hash " + bits[1] );
						}
					}
				}else if ( str.startsWith( "TranscodeFile\n" )){

					String[]	bits = str.split( "\n" );

					for (int i=1;i<bits.length;i++){

						File f = new File( bits[i] );

						if ( f.exists() && f.isFile()){

							plugin.open( f );

							break;
						}
					}
				}else if ( str.startsWith( "http:" ) || str.startsWith( "https:" )){

					try{
						plugin.open( new URL( str.trim()));

					}catch( Throwable e ){

						Debug.out( e );
					}

				}else{

					try{
						File	file = new File( str );

						if ( file.exists() && file.isFile()){

							plugin.open( file );
						}
					}catch( Throwable e ){

						Debug.out( e );
					}
				}
			}
		}
	}
}
