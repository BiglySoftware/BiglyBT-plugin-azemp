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

package com.azureus.plugins.azemp;

import java.io.File;
import java.net.URL;
import java.util.*;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.Debug;
import com.biglybt.core.util.UrlUtils;
import com.biglybt.pif.PluginException;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.UnloadablePlugin;
import com.biglybt.pif.ui.UIInstance;
import com.biglybt.pif.ui.UIManager;
import com.biglybt.pif.ui.UIManagerListener;
import com.biglybt.pif.ui.config.*;
import com.biglybt.pif.ui.model.BasicPluginConfigModel;
import com.biglybt.pif.utils.PowerManagementListener;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.pif.UISWTInstance;

import com.biglybt.ui.UIFunctions;
import com.biglybt.ui.UIFunctionsManager;
import com.biglybt.ui.UIFunctionsUserPrompter;
import com.biglybt.ui.swt.skin.*;
import com.azureus.plugins.azemp.ui.swt.emp.EmbeddedPlayerPluginView;
import com.azureus.plugins.azemp.ui.swt.emp.EmbeddedPlayerWindowSWT;
import com.vuze.mediaplayer.PlayerPreferences;
import com.vuze.mediaplayer.mplayer.MPlayer;
import com.vuze.mediaplayer.vlc.VLCPlayer;

/**
 * @author TuxPaper
 * @created Sep 4, 2007
 *
 */
public class EmbeddedMediaPlayerPlugin
	implements UnloadablePlugin, PowerManagementListener
{
	public static final int CONFIG_DEFAULT_DL_LIM_MAX	= 1536;
	public static final int CONFIG_DEFAULT_DL_LIM_EXTRA	= 512;
	
	private static final boolean	CAN_USE_VLC;
	private static final boolean	MUST_USE_VLC;

	static{ 
		if ( Constants.isOSX ){
			
				// recent VLC only 64-bit, no legacy 1.6 support just because
			
			CAN_USE_VLC = Constants.isJava7OrHigher && Constants.is64Bit;
			
		}else if ( Constants.isWindows ){
			
				// Windows ok as long as java 1.6+
			
			CAN_USE_VLC = Constants.JAVA_VERSION.startsWith( "1.6" ) || Constants.isJava7OrHigher;
			
		}else{
			
				// Linux ok as long as java 1.6+
			
			CAN_USE_VLC = Constants.JAVA_VERSION.startsWith( "1.6" ) || Constants.isJava7OrHigher;
		}
		
			// no MPlayer on Linux
		
		MUST_USE_VLC	= !(Constants.isWindows || Constants.isOSX);
	}
	
	
	private static final boolean	CONFIG_DEFAULT_USE_VLC = MUST_USE_VLC?true:false;
	
	static{
		COConfigurationManager.setIntDefault( "Plugin.azemp.azemp.config.dl_lim_max", CONFIG_DEFAULT_DL_LIM_MAX );
		COConfigurationManager.setIntDefault( "Plugin.azemp.azemp.config.dl_lim_extra", CONFIG_DEFAULT_DL_LIM_EXTRA );	
	}
	
	public static final String ID = "azemp";

	public static final String MSG_PREFIX = "v3.emp";

	public static final String CFG_VIDEO_OUT = "video.out";
	
	private static VuzePlayerPreferences preferences;

	
	public static PlayerPreferences getPlayerPreferences() {
		return preferences;
	}
	
	private static boolean use_vlc	= false;
	
	public static boolean useMPlayer(){
		
		return( !use_vlc );
	}
	
	public static void
	reportVLCProblem()
	{
		UIFunctions uif = UIFunctionsManager.getUIFunctions();
		
		if ( uif == null ){
							
			return;
		}	
		
		String title = MessageText.getString("azemp.vlc.test.fail.title");
		
		String text = MessageText.getString("azemp.vlc.test.fail.text", new String[]{ Constants.is64Bit?"64":"32" } );
		
		UIFunctionsUserPrompter prompter = uif.getUserPrompter(title, text, new String[]{
			MessageText.getString("Button.ok"),
		}, 0);

		prompter.setAutoCloseInMS(0);
		
		prompter.open(null);

	}
	
	private PluginInterface pluginInterface;

	private BasicPluginConfigModel	config;
	private BooleanParameter 		prevent_sleep_param;

	private EmbeddedPlayerPluginView	plugin_view;
	
	@Override
	public void unload()
			throws PluginException {
		//EmbeddedPlayerWindowSWT.killAll();
		
		if ( config != null ){
			
			config.destroy();
		}
		
		if ( plugin_view != null ){
			
			plugin_view.destroy();
		}
		
		VLCPlayer.unload();
	}
	
	@Override
	public void initialize(PluginInterface _pluginInterface)
			throws PluginException {
		pluginInterface = _pluginInterface;
		EmbeddedPlayerWindowSWT.pluginInterface = pluginInterface;
		
		preferences = new VuzePlayerPreferences(pluginInterface);
		
		if (pluginInterface.getUtilities().isOSX()) {
			
			String binaryPath = pluginInterface.getPluginDirectoryName();
			File binary = new File(binaryPath,"vuzeplayer");
			MPlayer.initialise(binary);

			Properties props = pluginInterface.getPluginProperties();
			props.setProperty("plugin.unload.disabled", "true");
		}
		
		if(pluginInterface.getUtilities().isWindows()){
			
			String binaryPath = pluginInterface.getPluginDirectoryName();
			
				// problem here with the mplayer/config file residing in virtual store on vista/win 7
				// and breaking the latest mplayer that won't run with the file there
			
			try{
				if ( Constants.isWindowsVistaOrHigher ){
					
					String local_app = System.getenv( "LOCALAPPDATA" );
					
					if ( local_app != null ){
						
						File virtual_store = new File( local_app, "VirtualStore" );
						
						if ( virtual_store.exists()){
							
							File prog_dir = new File( pluginInterface.getUtilities().getProgramDir());
							
							File parent_prog_dir = prog_dir.getParentFile();
							
							if ( parent_prog_dir != null ){
								
								File vs_prog_dir = new File( virtual_store, parent_prog_dir.getName());
								
								vs_prog_dir = new File( vs_prog_dir, prog_dir.getName());
								
								if ( vs_prog_dir.exists()){
																	
									File bad_file = new File( vs_prog_dir, "plugins" );
									
									bad_file = new File( bad_file, "azemp" );
									
									bad_file = new File( bad_file, "mplayer" );
									
									bad_file = new File( bad_file, "config" );
									
									if ( bad_file.exists()){
										
										Debug.out( "EMP: Renaming obsolete config file " + bad_file.getAbsolutePath());
	
										File bad_boy = new File( bad_file.getParentFile(), "config.bad" );
										
										bad_boy.delete();
										
										bad_file.renameTo( bad_boy );
									}
								}
							}
						}
					}
				}
			}catch( Throwable e ){
				
				Debug.out(e );
			}
			
			File binary = new File(binaryPath,"vuzeplayer.exe");
			MPlayer.initialise(binary);
		}
		
		UIManager ui_manager = pluginInterface.getUIManager();
		
		config = ui_manager.createBasicPluginConfigModel( "azemp.name" );

		boolean supports_sleep = pluginInterface.getUtilities().supportsPowerStateControl( PowerManagementListener.ST_SLEEP );
		
		if ( supports_sleep ){
			
				// prevent sleep
			
			prevent_sleep_param = config.addBooleanParameter2("azemp.prevent_sleep", "azemp.prevent_sleep", true);
		}
		
			// used by core atm...
		
		config.addIntParameter2( "azemp.config.dl_lim_max", 	"azemp.config.dl_lim_max", CONFIG_DEFAULT_DL_LIM_MAX, 0, Integer.MAX_VALUE );
		config.addIntParameter2( "azemp.config.dl_lim_extra", 	"azemp.config.dl_lim_extra", CONFIG_DEFAULT_DL_LIM_EXTRA, 0, Integer.MAX_VALUE );
		
		if ( CAN_USE_VLC ){
			
			HyperlinkParameter param_info = config.addHyperlinkParameter2( "azemp.config.vlc.info", pluginInterface.getUtilities().getLocaleUtilities().getLocalisedMessageText( "azemp.config.vlc.info.url" ));
			
			final BooleanParameter param_use_vlc = config.addBooleanParameter2( "azemp.config.vlc.use_vlc", "azemp.config.vlc.use_vlc", CONFIG_DEFAULT_USE_VLC );

			if ( MUST_USE_VLC ){
				
				param_use_vlc.setValue( true );
				
				param_use_vlc.setEnabled( false );
			}
			
			use_vlc = param_use_vlc.getValue();
			
			param_use_vlc.addListener(
					new ParameterListener()
					{
						@Override
						public void
						parameterChanged(
							Parameter param) 
						{
							use_vlc = param_use_vlc.getValue();
						};
					});
			
			ActionParameter param_test_vlc = config.addActionParameter2( "azemp.config.vlc.test_vlc", "azemp.config.vlc.test_vlc_button" );
			
			param_test_vlc.addListener(
				new ParameterListener()
				{
					@Override
					public void
					parameterChanged(
						Parameter param) 
					{
						UIFunctions uif = UIFunctionsManager.getUIFunctions();
						
						if ( uif == null ){
											
							return;
						}
						
						if ( VLCPlayer.initialise()){
							
							String title = MessageText.getString("azemp.vlc.test.ok.title");
							
							String text = MessageText.getString("azemp.vlc.test.ok.text" );
							
							UIFunctionsUserPrompter prompter = uif.getUserPrompter(title, text, new String[]{
								MessageText.getString("Button.ok"),
							}, 0);
			
							prompter.setAutoCloseInMS(0);
							
							prompter.open(null);
						}else{
							
							reportVLCProblem();
						}
						
					};
				});
			
			param_use_vlc.addEnabledOnSelection( param_test_vlc );
			
			config.createGroup(
				"azemp.config.vlc.group",
				new Parameter[]{ param_info, param_use_vlc, param_test_vlc } );
		}
		
		ui_manager.addUIListener(
				new UIManagerListener()
				{
					@Override
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							String skin_path = "com/azureus/plugins/azemp/skins/";
							
							UISWTInstance	swt = (UISWTInstance)instance;
							
							SWTSkin skin = SWTSkinFactory.getInstance();
							
							ClassLoader loader = EmbeddedMediaPlayerPlugin.class.getClassLoader();
							
							SWTSkinProperties skinProperties = skin.getSkinProperties();
							
							try {
								ResourceBundle subBundle = ResourceBundle.getBundle(skin_path + "skin",
										Locale.getDefault(), loader);
								
								skinProperties.addResourceBundle(subBundle, skin_path, loader);
								
							}catch( MissingResourceException mre ){
								Debug.out(mre);
							}

							SWTSkinPropertiesImpl imageProps = new SWTSkinPropertiesImpl( loader,skin_path, "skinImages" );
								
							skin.getImageLoader(skinProperties).addSkinProperties(imageProps);
							
							plugin_view = new EmbeddedPlayerPluginView( EmbeddedMediaPlayerPlugin.this, swt );
						}
					}
					
					@Override
					public void
					UIDetached(
						UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							if (plugin_view != null) {
								plugin_view.destroy();
								plugin_view = null;
							}
						}

					}
				});
		
		if ( supports_sleep ){
			
			pluginInterface.getUtilities().addPowerManagementListener( this );
		}
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( pluginInterface );
	}
	
	public void
	open(
		File		file )
	{
		try{
			EmbeddedPlayerWindowSWT.openWindow(file, file.getName());
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	public void
	open(
		URL		url )
	{
		try{
			String url_str = url.toExternalForm();
			
			String dn = "&azcddn=";
			
			int pos = url_str.indexOf( dn );
			
			if ( pos != -1 ){
				
				url_str = url_str.substring( pos + dn.length());
				
				pos = url_str.indexOf( '&' );
				
				if ( pos != -1 ){
					
					url_str = url_str.substring( 0, pos );
				}
			
				url_str = UrlUtils.decode( url_str );
			}
			
			EmbeddedPlayerWindowSWT.openWindow(url, url_str );
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	public void registerExternalPlayer(String player) {
		//Do nothing but kept here for backward compatibility
	}
	
	public void unRegisterExternalPlayer(String player) {
		//Do nothing but kept here for backward compatibility
	}
	
	@Override
	public String
	getPowerName()
	{
		return( "Embedded Media Player" );
	}
	
	@Override
	public boolean
	requestPowerStateChange(
		int		new_state,
		Object	data )
	{
		if ( prevent_sleep_param != null && prevent_sleep_param.getValue()){
			
			if ( EmbeddedPlayerWindowSWT.isPlayerActive()){
				
				return( false );
			}
		}
		
		return( true );
	}

	@Override
	public void
	informPowerStateChange(
		int		new_state,
		Object	data )
	{
	}
}
