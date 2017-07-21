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


package com.vuze.mediaplayer.swt;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Listener;

public interface 
RenderFrame 
{
	public String[]
	getExtraOptions();
	
	public void
	setVisible(
		boolean	vis );
	
	public void
	setBackground(
		Color	c );
	
	public void
	setCursor(
		Cursor	c );
	
	public boolean
	isDisposed();
	
	public void
	setLayoutData(
		AspectRatioData	ld );
	
	public void
	addListener(
		int			type,
		Listener	listener );
}
