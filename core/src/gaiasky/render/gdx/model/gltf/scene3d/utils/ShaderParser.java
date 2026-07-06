/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class ShaderParser {
	private final static String includeBefore = "#include <";
	private final static String includeAfter = ">";
	
	public static String parse(FileHandle file){
		String content = file.readString();
		String[] lines = content.split("\n");
		String result = "";
		for(String line : lines){
			String cleanLine = line.trim();
			
			if(cleanLine.startsWith(includeBefore)){
				int end = cleanLine.indexOf(includeAfter, includeBefore.length());
				if(end < 0) throw new GdxRuntimeException("malformed include: " + cleanLine);
				String path = cleanLine.substring(includeBefore.length(), end);
				FileHandle subFile = file.sibling(path);
				result += "\n//////// " + path + "\n";
				result += parse(subFile);
			}else{
				result += line + "\n";
			}
		}
		return result;
	}
	
}
