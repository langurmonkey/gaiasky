/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.utils;

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
