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

import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.Base32;
import com.biglybt.core.util.Debug;
import com.biglybt.pif.disk.DiskManagerFileInfo;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.ui.UIInstance;
import com.biglybt.pif.ui.UIManager;
import com.biglybt.pif.ui.menus.MenuItem;
import com.biglybt.pif.ui.menus.MenuItemListener;
import com.biglybt.pif.ui.menus.MenuManager;
import com.biglybt.pifimpl.local.PluginCoreUtils;
import com.biglybt.ui.UIFunctionsManager;
import com.biglybt.ui.mdi.MdiEntry;
import com.biglybt.ui.mdi.MdiEntryCreationListener;
import com.biglybt.ui.mdi.MdiEntryDropListener;
import com.biglybt.ui.mdi.MultipleDocumentInterface;
import com.biglybt.ui.swt.FixedURLTransfer.URLType;
import com.biglybt.ui.swt.Messages;
import com.biglybt.ui.swt.UIFunctionsManagerSWT;
import com.biglybt.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.biglybt.ui.swt.pif.UISWTInstance;
import com.biglybt.ui.swt.pif.UISWTView;
import com.biglybt.ui.swt.pif.UISWTViewEvent;
import com.biglybt.ui.swt.pif.UISWTViewEventListener;
import com.biglybt.ui.swt.views.skin.TorrentListViewsUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import com.azureus.plugins.azemp.EmbeddedMediaPlayerPlugin;

public class 
EmbeddedPlayerPluginView 
{
	private EmbeddedMediaPlayerPlugin	plugin;
	
	private String	view_name = MessageText.getString( "azemp.name" );
	private UISWTViewEventListener viewEventListener;
	private MenuItem mainMenuItem;
	
	public 
	EmbeddedPlayerPluginView(
		EmbeddedMediaPlayerPlugin		_plugin,
		UISWTInstance _swt )
	{
		plugin	= _plugin;

		final MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		

			viewEventListener = new UISWTViewEventListener()
			{
				@Override
				public boolean
				eventOccurred(
					UISWTViewEvent event )
				{
					switch( event.getType()){
					
						case UISWTViewEvent.TYPE_CREATE:{
						
							event.getView().setControlType( UISWTView.CONTROLTYPE_SWT );
						
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
						case UISWTViewEvent.TYPE_FOCUSGAINED:{
						
				
							break;
						}
						case UISWTViewEvent.TYPE_DESTROY:{
																			
							break;
						}
					}
					
					return( true );
				}
			};

			mdi.registerEntry(view_name, new MdiEntryCreationListener() {
				@Override
				public MdiEntry createMDiEntry(String id) {
					MdiEntry entry = mdi.createEntryFromEventListener(MultipleDocumentInterface.SIDEBAR_HEADER_DEVICES, viewEventListener, view_name, true, null, null);
					entry.setImageLeftID( "image.sidebar.azemp" );
					
					entry.addListener(
							new MdiEntryDropListener()
							{
								@Override
								public boolean
								mdiEntryDrop(
									MdiEntry 		entry, 
									Object 			payload  )
								{
									handleDrop( payload );
									
									return( true );
								}
							});

					return entry;
				}
			});

			UIManager ui_manager = plugin.getPluginInterface().getUIManager();

			MenuManager menu_manager = ui_manager.getMenuManager();

			mainMenuItem = menu_manager.addMenuItem( MenuManager.MENU_MENUBAR, "azemp.name" );
			mainMenuItem.setDisposeWithUIDetach(UIInstance.UIT_SWT);

			mainMenuItem.addListener( 
					new MenuItemListener()
					{
						@Override
						public void
						selected(
								MenuItem menu, Object target )
						{
							MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
							mdi.showEntryByID(view_name);
						}
					});

			mdi.loadEntryByID(view_name, false, true, null);
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

		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();

		if ( mdi != null ){
			
			MdiEntry mdi_entry = mdi.getEntry( view_name );
			
			if ( mdi_entry != null ){
				
				mdi_entry.close( true );
			}
		}
	}
}
