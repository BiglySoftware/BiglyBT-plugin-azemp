/*
 * Created on Nov 6, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package redstone.xmlrpc.serializers;

public class 
VuzeHelper 
{
	/*
	 * Problem with long values being truncated. In this case rather than using the apache extensions to 
	 * use an <i8> tag we convert to string as this is known to actually work OK
	 */
	
	protected static String
	serialise(
		long		value )
	{
		if ( value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE ){
			
			return( "<i4>" + value + "</i4>" );
			
		}else{
			
			return( "<string>" + value + "</string>" );
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		System.out.println( serialise( 45 ));
		System.out.println( serialise( Integer.MAX_VALUE ));
		System.out.println( serialise( (long)Integer.MAX_VALUE + 1));
		System.out.println( serialise( Integer.MIN_VALUE ));
		System.out.println( serialise( (long)Integer.MIN_VALUE - 1));
	}
}
