package com.vuze.mediaplayer.swt;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

import com.biglybt.core.util.Constants;
import com.biglybt.ui.swt.utils.FontUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.opensubtitles.api.OpenSubtitlesAPI;
import org.opensubtitles.api.Subtitle;

import com.vuze.mediaplayer.BaseMediaPlayer;
import com.vuze.mediaplayer.Language;
import com.vuze.mediaplayer.MediaPlaybackState;
import com.vuze.mediaplayer.PositionListener;
import com.vuze.mediaplayer.StateListener;
import com.vuze.mediaplayer.VolumeListener;

public class FullScreenControls {
	
	

	private static final String PLAYER_INFO_URL = "http://www.vuze.com/emp_info.start";
	
	//Shel alpha
	private static final int TARGET_ALPHA = 90 * 255 / 100;
	private static final int ALPHA_STEP = 20;
	
	//Time labels
	private static final int TIME_LABEL_HEIGHT = 12;
	private static final int TIME_LABEL_WIDTH = 55;
	private static final int CURRENT_TIME_X = 11;
	private static final int REMAINING_TIME_X = 391;
	private static final int TIME_Y = 41;
	
	//Fullscreen Button
	private static final int FULLSCREEN_Y = 10;
	private static final int FULLSCREEN_X = 412;
	
	//Languages Button
	private static final int LANGUAGES_Y = 9;
	private static final int LANGUAGES_X = 379;
	
	//Play/Pause Button
	private static final int PLAY_PAUSE_Y = 5;
	private static final int PLAY_PAUSE_X = 213;
	
	//Mute "Button"
	private static final int VOLUME_0_X = 14;
	private static final int VOLUME_0_Y = 10;
	private static final int VOLUME_0_WIDTH = 20;
	private static final int VOLUME_0_HEIGHT = 20;
	
	//Volume Max "Button"
	private static final int VOLUME_100_X = 90;
	private static final int VOLUME_100_Y = 10;
	private static final int VOLUME_100_WIDTH = 23;
	private static final int VOLUME_100_HEIGHT = 20;
	
	//Time slider
	private static final int TIME_TRACK_X = 65;
	private static final int TIME_TRACK_Y = 44;
	private static final int TIME_SLIDER_MIN_X = 69;
	private static final int TIME_SLIDER_MAX_X = 372;
	private static final int TIME_SLIDER_Y = 42;
	
	//Volume slider
	private static final int VOLUME_TRACK_X = 35;
	private static final int VOLUME_TRACK_Y = 18;
	private static final int VOLUME_SLIDER_MIN_X = 36;
	private static final int VOLUME_SLIDER_MAX_X = 74;
	private static final int VOLUME_SLIDER_Y = 15;
	
	
	Display display;
	Shell parent;
	
	Shell shell;
	
	volatile long 		currentTimeInSecs;
	volatile boolean 	playPauseButton_enabled				= true;
	volatile boolean 	playPauseButton_enabled_buffering	= false;
	volatile float 		seek_max_time			= -1;

	Label currentTime;
	Label remainingTime;
	boolean showRemaining = true;
	
	Canvas background;
	
	Canvas timeLeftTrack;
	Canvas timeSlider;
	
	Canvas volumeLeftTrack;
	Canvas volumeSlider;
	
	Canvas playPauseButton;
	
	
	Canvas fullscreenButton;
	
	Canvas languagesButton;
	
	Menu languagesMenu;
	
	PositionListener positionListener;
	
	float positionX = .5f,positionY = .98f;
	
	
	BaseMediaPlayerFrame	player_frame;
	BaseMediaPlayer			player;
	
	//Assets which require to be disposed
	Image backgroundImage;
	
	Color textColor;
	Font  textFont;
	
	Image timeLeftTrackImage;	
	Image sliderImage;
	Image sliderOnImage;
	
	Image playImage;
	Image playOnImage;
	Image pauseImage;
	Image pauseOnImage;
	
	Image volumeLeftTrackImage;
	Image volumeImage;
	Image volumeOnImage;
	
	Image fullscreenGoImage;
	Image fullscreenGoOnImage;
	Image fullscreenExitImage;
	Image fullscreenExitOnImage;
	
	Image languagesImage;
	Image languagesOnImage;
	
	Image openSubtitlesImage;
	
	//Assets which do not require to be disposed
	
	Listener antiCloseListener;
	
	Rectangle volume0Rectangle;
	Rectangle volume100Rectangle;
	
	//Cached subtitles from OpenSubtitles
	volatile Subtitle[] openSubtitles;
	volatile Subtitle[] openSubtitlesUniques;
	volatile boolean subtitlesLoading = false;
	volatile boolean subtitlesLoaded = false;
	volatile Menu openSubtitlesMenu;
	
	public FullScreenControls(BaseMediaPlayerFrame player_frame,Shell parent, Shell shell_parent) {
		
		this.player_frame 	= player_frame;
		this.player			= player_frame.getMediaPlayer();
		this.parent = parent;
		
		display = parent.getDisplay();
		
		backgroundImage = loadImage("controls/images/fc_background.png");
		sliderImage = loadImage("controls/images/fc_slider.png");
		sliderOnImage = loadImage("controls/images/fc_slider_on.png");
		playImage = loadImage("controls/images/fc_play.png");
		playOnImage = loadImage("controls/images/fc_play_on.png");
		pauseImage = loadImage("controls/images/fc_pause.png");
		pauseOnImage = loadImage("controls/images/fc_pause_on.png");
		volumeImage = loadImage("controls/images/fc_volume.png");
		volumeOnImage = loadImage("controls/images/fc_volume_on.png");
		fullscreenGoImage = loadImage("controls/images/fc_go.png");
		fullscreenGoOnImage = loadImage("controls/images/fc_go_on.png");
		fullscreenExitImage = loadImage("controls/images/fc_exit.png");
		fullscreenExitOnImage = loadImage("controls/images/fc_exit_on.png");
		timeLeftTrackImage = loadImage("controls/images/fc_slider_filled.png");
		volumeLeftTrackImage = loadImage("controls/images/fc_volume_filled.png");
		languagesImage = loadImage("controls/images/fc_languages.png");
		languagesOnImage = loadImage("controls/images/fc_languages_on.png");
		openSubtitlesImage = loadImage("controls/images/opensubtitles_icon.png");
		
		textColor = new Color(display, 152,178,200);
		
		GC gc = new GC(parent);
		textFont = FontUtils.getFontWithHeight(parent.getFont(), Constants.isOSX?10:12 ,SWT.BOLD);
		gc.dispose();
		
		shell = new Shell(shell_parent,SWT.NO_TRIM);
		shell.setBackgroundMode(SWT.INHERIT_FORCE);
		
		
		currentTime = new Label(shell,SWT.NONE);
		remainingTime = new Label(shell,SWT.NONE);
		timeSlider = new Canvas(shell,SWT.NONE);
		volumeSlider = new Canvas(shell,SWT.NONE);
		playPauseButton = new Canvas(shell,SWT.NONE);
		fullscreenButton = new Canvas(shell,SWT.NONE);
		volumeLeftTrack = new Canvas(shell,SWT.NONE);	
		timeLeftTrack = new Canvas(shell,SWT.NONE);
		languagesButton = new Canvas(shell,SWT.NONE);
		languagesMenu = new Menu(shell,SWT.POP_UP);
		
		buildLanguagesMenu();
		
		languagesButton.setLocation(LANGUAGES_X,LANGUAGES_Y);
		languagesButton.setBackgroundImage(languagesImage);
		languagesButton.setSize(languagesImage.getBounds().width,languagesImage.getBounds().height);
		
		volumeLeftTrack.setLocation(VOLUME_TRACK_X, VOLUME_TRACK_Y);
		volumeLeftTrack.setBackgroundImage(volumeLeftTrackImage);
		
		timeLeftTrack.setLocation(TIME_TRACK_X, TIME_TRACK_Y);
		timeLeftTrack.setBackgroundImage(timeLeftTrackImage);
		
		currentTime.setForeground(textColor);
		currentTime.setFont(textFont);
		currentTime.setLocation(CURRENT_TIME_X, TIME_Y);
		currentTime.setSize(TIME_LABEL_WIDTH,TIME_LABEL_HEIGHT);
		
		remainingTime.setForeground(textColor);
		remainingTime.setFont(textFont);
		remainingTime.setLocation(REMAINING_TIME_X,TIME_Y);
		remainingTime.setSize(TIME_LABEL_WIDTH,TIME_LABEL_HEIGHT);
		
		timeSlider.setBackgroundImage(sliderImage);
		timeSlider.setSize(sliderImage.getBounds().width,sliderImage.getBounds().height);
		setTimeSliderPosition(new Point(TIME_SLIDER_MIN_X, TIME_SLIDER_Y));
		timeSliderRectangle = new Rectangle(TIME_SLIDER_MIN_X, TIME_SLIDER_Y, TIME_SLIDER_MAX_X-TIME_SLIDER_MIN_X, sliderImage.getBounds().height);
		
		volumeSlider.setBackgroundImage(volumeImage);
		volumeSlider.setSize(volumeImage.getBounds().width,volumeImage.getBounds().height);
		setVolumeSliderLocation(new Point(VOLUME_SLIDER_MIN_X, VOLUME_SLIDER_Y));
		volumeSliderRectangle = new Rectangle(VOLUME_SLIDER_MIN_X, VOLUME_SLIDER_Y, VOLUME_SLIDER_MAX_X-VOLUME_SLIDER_MIN_X+volumeImage.getBounds().width, volumeImage.getBounds().height);
		
		volume0Rectangle = new Rectangle(VOLUME_0_X, VOLUME_0_Y, VOLUME_0_WIDTH, VOLUME_0_HEIGHT);
		volume100Rectangle = new Rectangle(VOLUME_100_X, VOLUME_100_Y, VOLUME_100_WIDTH, VOLUME_100_HEIGHT);
				
		playPauseButton.setBackgroundImage(pauseImage);
		playPauseButton.setSize(pauseImage.getBounds().width,pauseImage.getBounds().height);
		playPauseButton.setLocation(PLAY_PAUSE_X, PLAY_PAUSE_Y);
		
		fullscreenButton.setBackgroundImage(fullscreenGoImage);
		fullscreenButton.setSize(fullscreenGoImage.getBounds().width,fullscreenGoImage.getBounds().height);
		fullscreenButton.setLocation(FULLSCREEN_X,FULLSCREEN_Y);
		
		setShellSizeShapeAndBackground();
		
		addFullScreenListeners();
		
		addPlayPauseListeners();
		
		addLanguagesListeners();
		
		positionShell();
		
		addParentListeners();
		
		addShellMoveListener();
		
		addTimeSliderListener();
		
		addVolumeSliderListener();
		
		addPlayerPositionListener();
		
		addDisposeListener();
		
		animateTransition.setDaemon(true);
		animateTransition.start();
		
		shell.open();
	}
	
	public void
	prepare()
	{
		currentTimeInSecs					= 0;
		playPauseButton_enabled				= true;
		playPauseButton_enabled_buffering	= false;
		seek_max_time						= -1;
		
		updateSliderPosition( TIME_SLIDER_MIN_X );
		updateTimeDisplay( 0 );
	}
	
	public void
	setPlayEnabled( 
		boolean		enabled,
		boolean		is_buffering )
	{
		playPauseButton_enabled 			= enabled;
		playPauseButton_enabled_buffering	= is_buffering;
	}
	
	private void buildLanguagesMenu() {
		final Listener audioSelectionListener = new Listener() {

			@Override
			public void handleEvent(Event evt) {
				try {
					Language language = (Language) evt.widget.getData();
					if(language != null) {
						player.setAudioTrack(language);
					}								
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		};
		
		final Listener subtitleSelectionListener = new Listener() {
			
			
			@Override
			public void handleEvent(Event evt) {
				try {
					//Apparently, the Selection event is fired when the selection
					//changes, so we need to make sure we're not changing the subtitles
					//to the one that got disabled.
					MenuItem item =(MenuItem) evt.widget;
					if(item.getSelection()) {
						Language language = (Language) evt.widget.getData();
						player.setSubtitles(language);									
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		};
		
		languagesMenu.addListener(SWT.Show, new Listener() {
			
			
			@Override
			public void handleEvent(Event arg0) {
				MenuItem[] menuItems = languagesMenu.getItems();
				for (int i=0; i<menuItems.length; i++) {
					menuItems [i].dispose ();
				}
				
				
				
				Language[] subtitles = player.getSubtitles();
				MenuItem subtitlesItem = new MenuItem(languagesMenu,SWT.CASCADE);
				subtitlesItem.setText("Subtitles");
				Menu subtitlesMenu = new Menu(languagesMenu);
				subtitlesItem.setMenu(subtitlesMenu);
				if(subtitles != null && subtitles.length > 0) {		
					MenuItem itemOff = new MenuItem(subtitlesMenu, SWT.RADIO);
					itemOff.setText("Off");
					itemOff.addListener(SWT.Selection, subtitleSelectionListener);
					if(player.getActiveSubtitle() == null) {
						itemOff.setSelection(true);
					}
					
					for(Language l : subtitles) {						
						MenuItem item = new MenuItem(subtitlesMenu, SWT.RADIO);
						String label = "";
						if(l.getLanguage() != null) {
							label = l.getLanguage().getDisplayLanguage();
						} else {
							if(l.getId() != null) {
								label = "Undefined ";
							}
						}
						if(l.getName() != null) {
							label += " (" + l.getName() + ")";
						}
						item.setText(label);
						item.setData(l);
						
						Language currentSubtitle = player.getActiveSubtitle();
						if(l.getId() != null && l.getId().equals(player.getActiveSubtitleId()) && (currentSubtitle == null || l.getSource() == currentSubtitle.getSource())) {
							item.setSelection(true);
						}
						item.addListener(SWT.Selection, subtitleSelectionListener);
					}
					
					new MenuItem(subtitlesMenu,SWT.SEPARATOR);
			
				}
				
				Language[] audioTracks = player.getAudioTracks();
				if(audioTracks != null && audioTracks.length > 0) {
					MenuItem audio = new MenuItem(languagesMenu,SWT.CASCADE);
					audio.setText("Audio");
					if(audioTracks.length == 1) {
						audio.setEnabled(false);
					}
					Menu audioMenu = new Menu(languagesMenu);
					audio.setMenu(audioMenu);
					
					int	track_num = 0;
					
					for(Language l : audioTracks) {	
						track_num++;
						MenuItem item = new MenuItem(audioMenu, SWT.RADIO);
						String label = "";
						if(l.getLanguage() != null) {
							label = l.getLanguage().getDisplayLanguage();
						} else {
							label = "Track " + track_num;
						}
						if(l.getName() != null) {
							label += " (" + l.getName() + ")";
						}
						item.setText(label);
						if(l.getId() != null && l.getId().equals(player.getActiveAudioTrackId()) ) {
							item.setSelection(true);
						}
						item.setData(l);
						item.addListener(SWT.Selection, audioSelectionListener);
					}
					
							
				}
				
				MenuItem loadFromFile = new MenuItem(subtitlesMenu,SWT.PUSH);
				loadFromFile.setText("Open from File...");
				loadFromFile.addListener(SWT.Selection, new Listener() {
					
					@Override
					public void handleEvent(Event evt) {
						wasPlayerPaused = player.getCurrentState() == MediaPlaybackState.Paused;
						isTimeSliding = true;
						if(!wasPlayerPaused) {
							player.pause();
						}
						
						FileDialog fd = new FileDialog(shell);
						try {
							File f = new File(player.getOpenedFile());
							if(f.exists()) {
								fd.setFilterPath(f.getParent());
							}
						} catch(Exception e) {
							
						}
						String result = fd.open();
						if(!wasPlayerPaused) {
							player.play();
						}
						isTimeSliding = false;
						if(result != null) {
							player.loadSubtitlesFile(result);
						}
					}
				});
				new MenuItem(subtitlesMenu,SWT.SEPARATOR);
				final MenuItem loadFromOpenSubtitles = new MenuItem(subtitlesMenu,SWT.CASCADE);
				loadFromOpenSubtitles.setText("Open from OpenSubtitles.org");
				loadFromOpenSubtitles.setImage(openSubtitlesImage);
				openSubtitlesMenu = new Menu(subtitlesMenu);
				loadFromOpenSubtitles.setMenu(openSubtitlesMenu);
				if(subtitlesLoaded) {
					populateSubtitles();
				} else {
					MenuItem loading = new MenuItem(openSubtitlesMenu,SWT.PUSH);
					loading.setText("Loading from OpenSubtitles.org ...");
					loading.setEnabled(false);
					openSubtitlesMenu.addListener(SWT.Show, new Listener() {
						@Override
						public void handleEvent(Event arg0) {
							if(!subtitlesLoading && !subtitlesLoaded) {
								subtitlesLoading = true;
								Thread t = new Thread("OpenSubtitles lookup") {
									@Override
									public void run() {
										openSubtitles = OpenSubtitlesAPI.getSubtitlesForFile(player.getOpenedFile());
										openSubtitlesUniques = Subtitle.removeDuplicates(openSubtitles);
										Arrays.sort(openSubtitles);
										Arrays.sort(openSubtitlesUniques);
										subtitlesLoaded = true;
										display.asyncExec(new Runnable() {
											@Override
											public void run() {
												populateSubtitles();
												
											}

											
										});
									}
								};
								t.setDaemon(true);
								t.start();
							}
						}
					});
				}
				
				new MenuItem(languagesMenu,SWT.SEPARATOR);
				MenuItem infoItem = new MenuItem(languagesMenu,SWT.PUSH);
				infoItem.setText("More Info");
				infoItem.addListener(SWT.Selection, new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {
						Program.launch(PLAYER_INFO_URL);
					}
				});
				
				
			}
			
		});
		
	}
	
	private void populateSubtitles() {
		if(!subtitlesLoaded || openSubtitles == null) return;
		if(openSubtitlesMenu == null || openSubtitlesMenu.isDisposed()) return;
		MenuItem[] menuItems = openSubtitlesMenu.getItems();
		for (int i=0; i<menuItems.length; i++) {
			menuItems [i].dispose ();
		}
		
		Listener openSubListener = new Listener() {
			@Override
			public void handleEvent(Event evt) {
				final Subtitle sub = (Subtitle) evt.widget.getData("sub");
				if(sub == null) return;
				player.showMessage("Loading " + sub.getLocale().getDisplayLanguage() + " subtitles...", 20000);
				Thread downloader = new Thread("Subtitle Downloader") {
					@Override
					public void run() {
						String subTitleFileName = OpenSubtitlesAPI.getSubtitleFileName(player.getOpenedFile(), sub);
						if(OpenSubtitlesAPI.downloadSubtitle(subTitleFileName, sub)) {
							player.showMessage("", 0);
							player.loadSubtitlesFile(subTitleFileName);
						} else {
							player.showMessage("Failed to load " + sub.getLocale().getDisplayLanguage() + " subtitles.", 3000);
						}
					};
				};
				downloader.setDaemon(true);
				downloader.start();
			}
		};
		
		for(Subtitle sub:openSubtitlesUniques) {
			MenuItem item = new MenuItem(openSubtitlesMenu,SWT.PUSH);
			item.setData("sub",sub);
			item.setText(sub.getLocale().getDisplayLanguage());
			item.addListener(SWT.Selection, openSubListener);
		}
		
		if(openSubtitlesUniques.length != openSubtitles.length) {
			new MenuItem(openSubtitlesMenu,SWT.SEPARATOR);
			MenuItem showAll = new MenuItem(openSubtitlesMenu,SWT.CASCADE);
			showAll.setText("Show All");
			Menu menuAll = new Menu(openSubtitlesMenu);
			showAll.setMenu(menuAll);
			
			for(Subtitle sub:openSubtitles) {
				MenuItem item = new MenuItem(menuAll,SWT.PUSH);
				item.setData("sub",sub);
				item.setText(sub.getLocale().getDisplayLanguage() + " (" + sub.getNbDownloads() + " downloads)");
				item.addListener(SWT.Selection, openSubListener);
			}
			
		}
		
		if(openSubtitles.length == 0) {
			MenuItem item = new MenuItem(openSubtitlesMenu,SWT.PUSH);
			item.setText("None available");
			item.setEnabled(false);
		}
	}


	private void addDisposeListener() {
		shell.addListener(SWT.Dispose, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				dispose(backgroundImage);
				
				dispose( textColor);
				dispose(  textFont);
				
				dispose( sliderImage);
				dispose( sliderOnImage);
				
				dispose( playImage);
				dispose( playOnImage);
				dispose( pauseImage);
				dispose( pauseOnImage);
				
				dispose( volumeImage);
				dispose( volumeOnImage);
				
				dispose( fullscreenGoImage);
				dispose( fullscreenGoOnImage);
				dispose( fullscreenExitImage);
				dispose( fullscreenExitOnImage);
				
				dispose(timeLeftTrackImage);
				dispose(volumeLeftTrackImage);
				
				dispose(openSubtitlesImage);
				
				dispose(languagesImage);
				dispose(languagesOnImage);
				
				synchronized(animationStart) {
					stopAlphaThread = true;
					
					animationStart.notifyAll();
				}
				
			}
		});		
	}
	
	private void dispose(Image img) {
		if(img != null && ! img.isDisposed()) {
			img.dispose();			
		}
	}
	
	private void dispose(Color color) {
		if(color != null && ! color.isDisposed()) {
			color.dispose();			
		}
	}
	
	private void dispose(Font font) {
		if(font != null && ! font.isDisposed()) {
			font.dispose();			
		}
	}


	private void addFullScreenListeners() {	
		fullscreenButton.addListener(SWT.MouseDown, new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				if(player_frame.getFullscreen()) {
					fullscreenButton.setBackgroundImage(fullscreenExitOnImage);
				} else {
					fullscreenButton.setBackgroundImage(fullscreenGoOnImage);
				}
					
			}
		});
		fullscreenButton.addListener(SWT.MouseUp, new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				player_frame.setFullscreen(!player_frame.getFullscreen());
					
			}
		});
		
		
	}


	private void addPlayPauseListeners() {
		player.addStateListener(new StateListener() {
			@Override
			public void stateChanged(MediaPlaybackState newState) {
				if(newState == MediaPlaybackState.Opening) {
					reinitializePlayer();
				} else
				if(!isTimeSliding) {
					if(newState == MediaPlaybackState.Paused) {
						updatePlayPauseImage(playImage);
					} else
					if(newState == MediaPlaybackState.Playing) {
						updatePlayPauseImage(pauseImage);
					}
				}
			}

		});
		
		playPauseButton.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if ( !playPauseButton_enabled ){
					return;
				}
				if(player.getCurrentState() == MediaPlaybackState.Paused) {
					updatePlayPauseImage(playOnImage);
				} else
				if(player.getCurrentState() == MediaPlaybackState.Playing) {
					updatePlayPauseImage(pauseOnImage);
				}				
			}
		});
		
		playPauseButton.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if ( !playPauseButton_enabled ){
					return;
				}
				if(player.getCurrentState() == MediaPlaybackState.Paused) {
					player.play();
				} else
				if(player.getCurrentState() == MediaPlaybackState.Playing) {
					player.pause();
				}				
			}
		});
		
	}
	
	private void reinitializePlayer() {
		subtitlesLoaded = false;
		subtitlesLoading = false;
		openSubtitles = null;
		openSubtitlesUniques = null;
		
	}
	
	private void addLanguagesListeners() {
				
		languagesButton.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				updateLanguagesImage(languagesOnImage);			
			}
		});
		
		languagesButton.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				updateLanguagesImage(languagesImage);	
				if(evt.x >= 0 && evt.y >= 0 && evt.x <= languagesImage.getBounds().width && evt.y <= languagesImage.getBounds().height) {
					languagesMenu.setVisible(true);
				}
			}
		});
		
	}

	private void updateCanvasImage(final Canvas control,final Image img) {
		if(display.isDisposed()) return;
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if(control.isDisposed()) return;
				if(control.getBackgroundImage() != img) {
					control.setBackgroundImage(img);
				}
			}
		});
		
	}

	protected void updatePlayPauseImage(final Image img) {
		updateCanvasImage(playPauseButton, img);
	}
	
	protected void updateLanguagesImage(final Image img) {
		updateCanvasImage(languagesButton, img);
	}
	
	protected void updateFullscreenImage(final Image img) {
		updateCanvasImage(fullscreenButton, img);
	}


	private int sliderSeekOffsetX;
	private boolean wasPlayerPaused;
	
	private boolean
	timeSliderEnabled()
	{
		if ( playPauseButton_enabled ){
			
			return( true );
		}
		
		if ( player.canSeekAhead()){
			
			if ( playPauseButton_enabled_buffering ){
				
				return( true );
			}
		}
		
		return( false );
	}
	
	private void addTimeSliderListener() 
	{
		timeSlider.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if ( !timeSliderEnabled() ){
					return;
				}
				if ( isTimeSliding )return;
				timeSlider.setBackgroundImage(sliderOnImage);
				isTimeSliding = true;
				sliderSeekOffsetX = evt.x;
				wasPlayerPaused = player.getCurrentState() == MediaPlaybackState.Paused;
				player.pause();
				player.mute( true );
			}
		});
		
		timeSlider.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if ( !timeSliderEnabled() ){
					return;
				}
				timeSlider.setBackgroundImage(sliderImage);
				seekFromSlider(timeSlider.getLocation().x);
				isTimeSliding = false;
				player.mute( false );
				if(!wasPlayerPaused) {
					player.play();
				}
				
			}
		});
		timeSlider.addListener(SWT.MouseMove, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if ( !timeSliderEnabled() ){
					return;
				}
				if(!isTimeSliding) return;
				Point p = shell.toControl(timeSlider.toDisplay(evt.x, evt.y));
				p.x -= sliderSeekOffsetX;
				if(p.x < TIME_SLIDER_MIN_X) {
					p.x = TIME_SLIDER_MIN_X;
				}
				if(p.x > TIME_SLIDER_MAX_X) {
					p.x = TIME_SLIDER_MAX_X;
				}
				
				p.y = TIME_SLIDER_Y;
				
				setTimeSliderPosition(p);
				float seekToTime = getTimeFromSliderX(p.x);
				updateTimeDisplay(seekToTime);
				
				if ( seek_max_time == -1 ){
				
					player.seek(seekToTime);
				}
			}
		});
		
	}
	
	private void setTimeSliderPosition(Point p) {
		timeSlider.setLocation(p);
		Point size = new Point(p.x,timeLeftTrackImage.getBounds().height);
		size.x -= TIME_TRACK_X;
		
		if ( seek_max_time >= 0 ){
			int seek_max_pos = TIME_SLIDER_MIN_X + (int)((TIME_SLIDER_MAX_X-TIME_SLIDER_MIN_X) * seek_max_time / player.getDurationInSecs());
			size.x = seek_max_pos - TIME_TRACK_X;
			if ( size.x > timeLeftTrackImage.getBounds().width ){
				size.x = timeLeftTrackImage.getBounds().width;
			}
		}
		timeLeftTrack.setSize(size);
		timeLeftTrack.redraw();
	}
	
	private int volumeSliderSeekOffsetX;
	
	private void addVolumeSliderListener() {
		volumeSlider.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				volumeSlider.setBackgroundImage(volumeOnImage);
				isVolumeSliding = true;
				volumeSliderSeekOffsetX = evt.x;
			}
		});
		
		volumeSlider.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				volumeSlider.setBackgroundImage(volumeImage);
				isVolumeSliding = false;				
			}
		});
		volumeSlider.addListener(SWT.MouseMove, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if(!isVolumeSliding) return;
				Point p = shell.toControl(volumeSlider.toDisplay(evt.x, evt.y));
				p.x -= volumeSliderSeekOffsetX;
				if(p.x < VOLUME_SLIDER_MIN_X) {
					p.x = VOLUME_SLIDER_MIN_X;
				}
				if(p.x > VOLUME_SLIDER_MAX_X) {
					p.x = VOLUME_SLIDER_MAX_X;
				}
				
				p.y = VOLUME_SLIDER_Y;
				
				setVolumeSliderLocation(p);
				
				int newVolume = getVolumeFromSliderX(p.x);
				player.setVolume(newVolume);
			}

			
		});
		
		player.addVolumeListener(new VolumeListener() {
			
			@Override
			public void volumeChanged(int newVolume) {
				if(!isVolumeSliding) {
					int sliderPosition = VOLUME_SLIDER_MIN_X + (int) ((VOLUME_SLIDER_MAX_X-VOLUME_SLIDER_MIN_X) * newVolume / 100);
					updateVolumeSliderPosition(sliderPosition);
				}
			}
		});
		
	}
	
	private void setVolumeSliderLocation(Point p) {
		volumeSlider.setLocation(p);
		Point size = new Point(p.x,volumeLeftTrackImage.getBounds().height);
		size.x -= VOLUME_TRACK_X;
		volumeLeftTrack.setSize(size);
	}
	
	private int getVolumeFromSliderX(int x) {
		 return 100 * (x - VOLUME_SLIDER_MIN_X) / (VOLUME_SLIDER_MAX_X - VOLUME_SLIDER_MIN_X);
	}


	private void addPlayerPositionListener() {
		
		positionListener = new PositionListener() {
			
			
			@Override
			public void positionChanged(float currentTimeInSecs) {
				updateTimeDisplay(currentTimeInSecs);

				if(!isTimeSliding) {
					int sliderPosition = TIME_SLIDER_MIN_X + (int) ((TIME_SLIDER_MAX_X-TIME_SLIDER_MIN_X) * currentTimeInSecs / player.getDurationInSecs());
					
					updateSliderPosition(sliderPosition);
				}
			}
		};
		
		remainingTime.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				showRemaining = ! showRemaining;
				positionListener.positionChanged(player.getPositionInSecs());
			}
		});
		
		player.addPositionListener(positionListener);		
	}
	
	private void updateSliderPosition(final int sliderPosition) {
		if(!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				
				@Override
				public void run() {
					if(timeSlider.isDisposed()) return;
					Point p = new Point(sliderPosition, TIME_SLIDER_Y);
					setTimeSliderPosition(p);
				}
			});
		}		
	}
	
	private void updateVolumeSliderPosition(final int sliderPosition) {
		if(!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				
				@Override
				public void run() {
					if(volumeSlider.isDisposed()) return;
					setVolumeSliderLocation(new Point(sliderPosition, VOLUME_SLIDER_Y));
				}
			});
		}		
	}
	
	private void updateTimeDisplay(final float _currentTimeInSecs) {
		if(!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if(shell.isDisposed()) return;
					final String currentTimeStr = Utils.getFormatedTime(_currentTimeInSecs,true);
					final String remainingTimeStr;
					if(showRemaining) {
						remainingTimeStr = " " + Utils.getFormatedTime(player.getDurationInSecs(),true);
					} else {
						remainingTimeStr = "-" + Utils.getFormatedTime(player.getDurationInSecs() - _currentTimeInSecs,true);
					}
					currentTimeInSecs = (long)_currentTimeInSecs;
					currentTime.setText(currentTimeStr);
					remainingTime.setText(remainingTimeStr);
				}
			});
		}
	}


	private boolean isTimeSliding;
	private Rectangle timeSliderRectangle;
	private boolean isVolumeSliding;
	private Rectangle volumeSliderRectangle;
	
	private boolean moving = false;
	private int moveStartX,moveStartY;
	private int moveStartPositionX,moveStartPositionY;	
	private int minX,minY,maxX,maxY;
	
	private void addShellMoveListener() {
		background.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				//We need to hide it manually because OSX doesn't
				//if(!languagesMenu.isDisposed()) languagesMenu.setVisible(false);
				
				//Let's no move the window if the user clicks on one of the sliders
				if(timeSliderRectangle.contains(evt.x,evt.y)) {
					return;
				}
				if(volumeSliderRectangle.contains(evt.x,evt.y)) {
					return;
				}
				if(volume0Rectangle.contains(evt.x,evt.y)) {
					return;
				}				
				if(volume100Rectangle.contains(evt.x,evt.y)) {
					return;
				}
				moving = true;
				
				moveStartX = evt.x;
				moveStartY = evt.y;
				
				Point real = shell.toDisplay(moveStartX, moveStartY);
				moveStartX = real.x;
				moveStartY = real.y;
				
				Point min = parent.toDisplay(0,0);
				minX = min.x;
				minY = min.y;
				
				Point max = parent.toDisplay(parent.getClientArea().width,parent.getClientArea().height);
				maxX = max.x - shell.getBounds().width;
				maxY = max.y - shell.getBounds().height;
				
				moveStartPositionX = shell.getLocation().x;
				moveStartPositionY = shell.getLocation().y;
			}
		});
		
		background.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if(timeSliderRectangle.contains(evt.x,evt.y)) {
					seekFromSlider(evt.x- sliderImage.getBounds().width/2);	
					return;
				}
				if(volumeSliderRectangle.contains(evt.x,evt.y)) {
					changeVolumeFromSlider(evt.x);
					return;
				}
				if(volume0Rectangle.contains(evt.x,evt.y)) {
					changeVolumeFromSlider(VOLUME_SLIDER_MIN_X);
					return;
				}				
				if(volume100Rectangle.contains(evt.x,evt.y)) {
					changeVolumeFromSlider(VOLUME_SLIDER_MAX_X);
					return;
				}
				moving = false;
				
				Point position = shell.getLocation();
				Point parentPosition = parent.toDisplay(0,0);
				Rectangle bounds = shell.getBounds();
				Rectangle parentBounds = parent.getClientArea();
				
				positionX = (float) (position.x - parentPosition.x) / (parentBounds.width - bounds.width);
				positionY = (float) (position.y - parentPosition.y)/ (parentBounds.height - bounds.height);
				
			}
		});
		
		timeLeftTrack.addListener(SWT.MouseUp, new Listener() {
			
			@Override
			public void handleEvent(Event evt) {
				Point p = background.toControl(timeLeftTrack.toDisplay(evt.x,evt.y));
				if(timeSliderRectangle.contains(p.x,p.y)) {
					seekFromSlider(p.x- sliderImage.getBounds().width/2);					
				}
			}
		});
		
		volumeLeftTrack.addListener(SWT.MouseUp, new Listener() {
			
			@Override
			public void handleEvent(Event evt) {
				Point p = background.toControl(volumeLeftTrack.toDisplay(evt.x,evt.y));
				if(volumeSliderRectangle.contains(p.x,p.y)) {
					changeVolumeFromSlider(p.x- volumeImage.getBounds().width/2);				
				}
			}
		});
		
		background.addMouseMoveListener(new MouseMoveListener() {
			
			
			@Override
			public void mouseMove(MouseEvent evt) {
				if(!moving) return;
				
				int x = evt.x;
				int y = evt.y;
				
				Point real = shell.toDisplay(x, y);
				x = real.x;
				y = real.y;
				
				int deltaX = x - moveStartX;
				int deltaY = y - moveStartY;
				
				int newX = moveStartPositionX+deltaX;
				int newY = moveStartPositionY+deltaY;
				
				if(newX < minX) {
					newX = minX;
				}
				if(newX > maxX) {
					newX = maxX;
				}
				if(newY < minY) {
					newY = minY;
				}
				if(newY > maxY) {
					newY = maxY;
				}
				
				shell.setLocation(newX,newY);
				
			}
		});
		
		
		
	}

	private void seekFromSlider(int x) {	
		if ( !timeSliderEnabled()){
			return;
		}
		boolean paused = player.getCurrentState() == MediaPlaybackState.Paused;
		
		if( paused ){
			player.mute( true );
		}
		player.seek(getTimeFromSliderX(x));
		
		if ( paused ){
			player.mute( false );
		}
	}
	
	private void changeVolumeFromSlider(int x) {			
		player.setVolume(getVolumeFromSliderX(x));
	}
	
	public void
	setSeekMaxTime(
		float	_t )
	{
		if ( _t == seek_max_time ){
			
			return;
		}
				
		seek_max_time = _t;
		
		if(!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				
				@Override
				public void run() {
					if(timeSlider.isDisposed()) return;
					Point p = new Point(timeSlider.getLocation().x, TIME_SLIDER_Y);
					setTimeSliderPosition(p);
				}
			});
		}		
	}
	
	public float
	getSeekMaxTime()
	{
		return( seek_max_time );
	}
	
	public long
	getCurrentTimeSecs()
	{
		return( currentTimeInSecs );
	}
	
	private float getTimeFromSliderX(int x) {
		float time = player.getDurationInSecs() * (x - timeSliderRectangle.x) / (timeSliderRectangle.width);
		
		if ( !player.canSeekAhead()){
			
			if ( seek_max_time >= 0 ){
			
				time = Math.min( time, seek_max_time );
			}
		}
				
		return( time );
	}


	private void addParentListeners() {
		
		Listener repositionListener = new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				positionShell();
			}
		};
		
		parent.addListener(SWT.Move, repositionListener);
		parent.addListener(SWT.Resize, repositionListener);

		Point minimumSize = shell.getSize();
		
		Rectangle clientArea = parent.getClientArea();
		Rectangle shellArea = parent.getBounds();
		
		minimumSize.x += shellArea.width - clientArea.width;
		minimumSize.y += shellArea.height - clientArea.height;
		
		parent.setMinimumSize(minimumSize);
		
		Listener fullScreenListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				//Looks like the full-screen state isn't correct yet
				//at the time this event is fired on windows,
				//so let's async process it ...
				if(display.isDisposed()) return;
				display.asyncExec(new Runnable() {
					
					@Override
					public void run() {
						if(!player_frame.isDisposed() && player_frame.getFullscreen()) {
							updateFullscreenImage(fullscreenExitImage);
						} else {
							updateFullscreenImage(fullscreenGoImage);
						}
					}
				});
				
			}
		};
		
		parent.addListener(SWT.Resize, fullScreenListener);
		
	}

	private void positionShell() {
		
		Rectangle parentBounds = parent.getClientArea();
		Point start = parent.toDisplay(0,0);
		Rectangle shellBounds = shell.getBounds();
		
		int y = (int) ( (parentBounds.height - shellBounds.height)*positionY );
		int x = (int) ( (parentBounds.width - shellBounds.width)*positionX );
		
		shell.setLocation(start.x+x, start.y+y);
		
		Point location = display.getCursorLocation();
		location = parent.toControl(location);
		Rectangle bounds = shell.getBounds();
		if(!bounds.contains(location)) {
			Event evt = new Event();
			evt.x = location.x;
			evt.y = location.y;
			for(Listener listener : player_frame.getListeners(SWT.MouseMove)) {
				listener.handleEvent(evt);
			}
		}
		
	}

	private void setShellSizeShapeAndBackground() {
		shell.setSize(backgroundImage.getBounds().width,backgroundImage.getBounds().height);
		shell.setBackgroundImage(backgroundImage);
		
		background = new Canvas(shell,SWT.NONE);
		background.setSize(backgroundImage.getBounds().width,backgroundImage.getBounds().height);
		background.setLocation(0,0);
		background.setBackgroundImage(backgroundImage);
		
		Region region = new Region();
		final ImageData imageData = backgroundImage.getImageData();
		if (imageData.alphaData != null) {
			Rectangle pixel = new Rectangle(0, 0, 1, 1);
			for (int y = 0; y < imageData.height; y++) {
				for (int x = 0; x < imageData.width; x++) {
					if (imageData.getAlpha(x, y) >= 10) {
						pixel.x = imageData.x + x;
						pixel.y = imageData.y + y;
						region.add(pixel);
					} 
				}
			}
		}
		shell.setRegion(region);
		
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
		
		shell.setAlpha(TARGET_ALPHA);
		
	}

	private Image loadImage(String resource) {
		InputStream is = this.getClass().getResourceAsStream(resource);
		if(is != null) {
			ImageData data = new ImageData(is);
			Image img = new Image(display,data);
			return img;
		}
		return null;
	}
	
	public Composite getShell() {
		return background;
	}
	
	
	private Object animationStart = new Object();
	private boolean isHiding;
	private boolean isShowing;
	private int currentAlpha = TARGET_ALPHA;
	private boolean stopAlphaThread = false;
	private Thread	animateTransition = new Thread("Player Controls Alpha Animation") {
		@Override
		public void run() {
			while(!stopAlphaThread && !shell.isDisposed()) {
				if(isHiding) {
					if(currentAlpha > 0) {
						if(currentAlpha >= ALPHA_STEP) {
							currentAlpha -= ALPHA_STEP;
						} else {
							currentAlpha = 0;
						}
									
						setAlpha(currentAlpha);
					} else {
						isHiding = false;
					}
				}
				if(isShowing) {
					if(currentAlpha < TARGET_ALPHA) {	
						if(currentAlpha <= TARGET_ALPHA - ALPHA_STEP) {
							currentAlpha += ALPHA_STEP;
						} else {
							currentAlpha = TARGET_ALPHA;
						}
						setAlpha(currentAlpha);
					} else {
						isShowing = false;
					}
				}
				
				try {
					if(isShowing || isHiding) {
						Thread.sleep(10);
					} else {
						synchronized (animationStart) {
							
							if ( stopAlphaThread ){
								return;
							}
							
							animationStart.wait();
						}						
					}
				} catch (Exception e) {
					// TODO: handle exception
				}								
			}
		};
	};
	
	private void setAlpha(final int alpha) {
		if(display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {

			@Override
			public void run() {
				if(shell == null || shell.isDisposed()) {
					return;
				}
				shell.setAlpha(alpha);
				if(alpha == 0 && shell.isVisible()) {
					shell.setVisible(false);
				}
				if(alpha > 0 && ! shell.isVisible()) {
					shell.setVisible(true);
				}
			}
		});
	}
	
	public void hide() {
		if(isHiding) return;
		if(isShowing) {
			isShowing = false;
		}
		isHiding = true;
		synchronized (animationStart) {
			animationStart.notify();
		}					
	}
	
	public void show() {
		if(isShowing) return;
		if(isHiding) {
			isHiding = false;
		}
		isShowing = true;
		synchronized (animationStart) {
			animationStart.notify();
		}
	}
	
	public void setFocus() {
		if(!shell.isDisposed()) {
			shell.setActive();
		}
	}
	
	public void close() {
		if ( !shell.isDisposed()){
			shell.removeListener(SWT.Close, antiCloseListener);
			shell.close();
		}
	}
	
	public void setCursor(Cursor cursor) {
		if(!shell.isDisposed()) shell.setCursor(cursor);
		if(!background.isDisposed()) background.setCursor(cursor);
		if(!fullscreenButton.isDisposed()) fullscreenButton.setCursor(cursor);
	}


}
