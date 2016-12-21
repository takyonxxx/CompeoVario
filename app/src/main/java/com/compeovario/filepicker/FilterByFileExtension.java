/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.compeovario.filepicker;

import java.io.File;
import java.io.FileFilter;
import com.compeovario.MyActivity;

public class FilterByFileExtension implements FileFilter {
	private final String extension;
	MyActivity fc;

	public FilterByFileExtension(String extension) {
		this.extension = extension;
	}

	@Override
	public boolean accept(File file) {
		// accept only readable fileslayout
		if (file.canRead()) {
			if (file.isDirectory()) {
				// accept all directories
				return true;
			} else{		
				 String[] result = extension.split(";");
				 for(String asset: result){
					  if (file.isFile() && file.getName().endsWith(asset)) {
						return true;
						}	
					 }				
			}
		}
		return false;
	}
}
