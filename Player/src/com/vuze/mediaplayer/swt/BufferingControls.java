package com.vuze.mediaplayer.swt;

import java.io.InputStream;

import com.biglybt.core.util.AEThread2;
import com.biglybt.ui.swt.shells.GCStringPrinter;
import com.biglybt.ui.swt.utils.FontUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.eclipse.swt.widgets.Shell;


public class 
BufferingControls 
{	
		//Shell alpha
	
	private static final int TARGET_ALPHA = 80 * 255 / 100;
	private static final int ALPHA_STEP = 20;

	private static final int LINE1_HEIGHT = 14;
	private static final int LINE2_HEIGHT = 12;
	private static final int LINE3_HEIGHT = 11;

	
	Display display;
	Shell parent;
	
	Shell shell;
	
	Canvas background;
	
		
	float positionX = 0.50f;
	float positionY = 0.25f;
	
	
	PlayerFrame player;
	
	boolean			visible;
	
	private boolean	failed_state;
	
	private volatile String	line1_text = "";
	private volatile String	line2_text = "";
	private volatile String	line3_text = "";
	
	private volatile float	spinner_angle;
	private volatile int	showHideCount;
	
	//Assets which require to be disposed
	Image 	backgroundImage;
	Image	spinner;
	
	Color textColor;
	Font  text1Font;
	Font  text2Font;
	Font  text3Font;
	
	
	//Assets which do not require to be disposed
	
	Listener antiCloseListener;
		
	private Object animationStart = new Object();
	private boolean isHiding;
	private boolean isShowing;
	private int currentAlpha = TARGET_ALPHA;
	private boolean stopAlphaThread = false;

	
	public 
	BufferingControls(
		PlayerFrame 	player,
		Shell 			parent,
		Shell 			shell_parent )
	{
		
		this.player = player;
		this.parent = parent;
		
		display = parent.getDisplay();
		
		backgroundImage = loadImage("controls/images/bc_background.png");
		
		spinner = loadImage("controls/images/bc_spinner.png");
		
		textColor = new Color(display, 152,178,200);
		FontData[] fData = parent.getFont().getFontData();
		for(FontData data : fData) {
			data.setStyle(SWT.BOLD);
			data.setHeight(Utils.isMacOSX() ? 10 : 8);
		}
		GC gc = new GC(parent);
		text1Font = FontUtils.getFontWithHeight(parent.getFont(),gc , LINE1_HEIGHT ,SWT.BOLD);
		text2Font = FontUtils.getFontWithHeight(parent.getFont(),gc , LINE2_HEIGHT ,SWT.BOLD);
		text3Font = FontUtils.getFontWithHeight(parent.getFont(),gc , LINE3_HEIGHT ,SWT.NORMAL);
		gc.dispose();
		
		shell = new Shell(shell_parent,SWT.NO_TRIM);
		shell.setBackgroundMode(SWT.INHERIT_FORCE);
		
		setShellSizeShapeAndBackground();
				
		positionShell();
		
		addParentListeners();
		
		addDisposeListener();
				
		currentAlpha	= 0;
		isHiding		= true;
		
		animateTransition.setDaemon(true);
		animateTransition.start();
		
		shell.open();
	}
	
	public void 
	updateText(
		String		line1,
		String		line2,
		String		line3 )
	{
		if ( failed_state ){
			
			return;
		}
		
		line1_text	= line1;
		line2_text 	= line2;
		line3_text	= line3;
	
		if (!display.isDisposed()){
			display.asyncExec(new Runnable(){
				@Override
				public void run() {
					if(background.isDisposed()) return;
					
					background.redraw();
				}
			});
		}
	}




	private void addDisposeListener() {
		shell.addListener(SWT.Dispose, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				dispose(backgroundImage);
				dispose( spinner );

				dispose( textColor );
				dispose( text1Font );
				dispose( text2Font );
				dispose( text3Font );
			
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
			for(Listener listener : player.getListeners(SWT.MouseMove)) {
				listener.handleEvent(evt);
			}
		}
		
	}

	private void setShellSizeShapeAndBackground() {
		shell.setSize(backgroundImage.getBounds().width,backgroundImage.getBounds().height);
		shell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		shell.setBackgroundImage(backgroundImage);
		
		background = new Canvas(shell,SWT.DOUBLE_BUFFERED);
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

		background.addPaintListener(
			new PaintListener()
			{
				@Override
				public void
				paintControl(
					PaintEvent e) 
				{
					GC	gc = e.gc;
										
					Rectangle bounds = background.getBounds();
							
					int	all_width 	= bounds.width;
					int all_height	= bounds.height;
					
					Rectangle spinner_bounds = spinner.getBounds();
					
					int	spinner_x = spinner_bounds.width / 2;
					int	spinner_y = spinner_bounds.height / 2;
					
					Transform trans = new Transform( gc.getDevice());
								
					trans.translate( all_width/2, all_height/2 );
					
					trans.rotate( spinner_angle );

					gc.setTransform( trans );
					
					gc.drawImage( spinner, -spinner_x, -spinner_y );
					
					trans.dispose();
					
					gc.setTransform( null );
										
					gc.setForeground( textColor );
					
						// line 1
					
					gc.setFont( text1Font );

					GCStringPrinter sp = new GCStringPrinter(gc, line1_text, bounds, false, false, SWT.NONE);
					
					sp.calculateMetrics();
					
					Point text_size = sp.getCalculatedSize();

					int	text_y = ( bounds.height - spinner_bounds.height ) / 4 - text_size.y/2;
					
					Rectangle text_rect = 
						new Rectangle( (all_width - text_size.x )/2, text_y,  text_size.x, text_size.y );
	
					sp.printString( gc, text_rect, 0 );

						// line 2
					
					gc.setFont( text2Font );

					sp = new GCStringPrinter(gc, line2_text, bounds, false, false, SWT.NONE);
					
					sp.calculateMetrics();
					
					text_size = sp.getCalculatedSize();

					text_y = ( bounds.height + spinner_bounds.height ) / 2;
					
					text_y = text_y + (bounds.height - text_y )/2 - text_size.y/2 - 10;
					
					text_rect = 
						new Rectangle( (all_width - text_size.x )/2, text_y,  text_size.x, text_size.y );
	
					sp.printString( gc, text_rect, 0 );

						// line 3
					
					if ( line1_text.length() > 0 ){
						
						gc.setFont( text3Font );

						sp = new GCStringPrinter(gc, line3_text, bounds, false, false, SWT.NONE);
						
						sp.calculateMetrics();
						
						text_size = sp.getCalculatedSize();

						text_y = ( bounds.height + spinner_bounds.height ) / 2 + 40 - 30;
						
						text_y = text_y + (bounds.height - text_y )/2 - text_size.y/2;
						
						text_rect = 
							new Rectangle( (all_width - text_size.x )/2, text_y,  text_size.x, text_size.y );
		
						sp.printString( gc, text_rect, 0 );
					}
				}
			});
				
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
		
		shell.setAlpha( 0 );
		
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
	
	public void
	setFailed(
		String	line1,
		String	line2 )
	{
		failed_state = true;
		
		line1_text 	= line1==null?"":line1;
		line2_text	= line2==null?"":line2;
		
		int pos = line2_text.indexOf('\n');
		
		if ( pos == -1 ){
		
			line3_text	= "";
			
		}else{
			
			line3_text	= line2_text.substring( pos+1 ).trim();
			line2_text	= line2_text.substring( 0, pos ).trim();
		}
		
		show();
	}
	
	public void 
	hide() 
	{
		failed_state = false;
		
		synchronized (animationStart) {
			if(isHiding) return;
			if(isShowing) {
				isShowing = false;
			}
			isHiding = true;
			if ( visible ){
				visible = false;
				showHideCount++;	
			}
			
			animationStart.notify();
		}					
	}
	
	public void 
	show() 
	{
		synchronized (animationStart) {
			if(isShowing) return;
			if(isHiding) {
				isHiding = false;
			}
			isShowing = true;
			if ( !visible ){
				visible = true;
			
				final int sc = ++showHideCount;
				new AEThread2( "spinner" )
				{
					@Override
					public void
					run()
					{
						final int	angles = 15;
						
						int spinner_index = 0;
						while( showHideCount == sc ){
							try{
								Thread.sleep(100);
							}catch( Throwable e ){
								break;
							}
							
							spinner_index++;
							
							spinner_angle = (float)( 360*(spinner_index%angles)/angles);
							
							if ( display.isDisposed() || shell.isDisposed()){
								break;
							}
							display.asyncExec(
								new Runnable() 
								{
									@Override
									public void
									run()
									{
										if ( !shell.isDisposed()){
											
											background.redraw();
										}
									}
								});
						}
					}
				}.start();
			}
			
			animationStart.notify();
		}
	}
	
	protected void
	addListener(
		int			eventType,
		Listener	listener )
	{
		if ( !background.isDisposed()){
			background.addListener(eventType, listener);
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
	}


}
