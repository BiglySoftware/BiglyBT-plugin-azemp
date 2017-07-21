package com.vuze.swt.windows;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.eclipse.swt.widgets.Canvas;

import com.vuze.swt.MPlayerRendererInterface;

public class MPlayerRendererCanvasWindows implements MPlayerRendererInterface {
	
	long id;
	
	public MPlayerRendererCanvasWindows(Canvas canvas) {
		try {
			Class clazz = canvas.getClass();
			Field fId = clazz.getField("handle");
			
				// Integer on 32 bit win, Long on 64 bit...
			
			Number num = (Number)fId.get(canvas);
			
			if ( num instanceof Integer ){
				
				id = num.longValue()&0x00000000ffffffffL;
				
			}else{
				
				id = num.longValue();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String[] getExtraMplayerOptions() {
		
			// when 'id' is negative mplayer fails to find the window - not easily
			// reproducible but guessing that we need to treat as unsigned 32 bit quantity
		
		String s_id;
		
		if ( id >= 0 ){
			
			s_id = String.valueOf( id );
			
		}else{
		
			byte[] bytes = ByteBuffer.allocate(8).putLong(id).array();

			s_id = new BigInteger(1, bytes).toString();
		}
		
		return new String[] {"-vo","direct3d","-wid" , s_id };
	}

}
