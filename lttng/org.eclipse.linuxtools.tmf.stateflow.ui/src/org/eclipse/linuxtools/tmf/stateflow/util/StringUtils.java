package org.eclipse.linuxtools.tmf.stateflow.util;

/**
 * static string utility class
 * TODO add apache commons lang to dependencies and get rid of this
 * @author Aaron Spear
 *
 */
public class StringUtils {
	
	
	public static String join( Object[] parts, String separator) {
		StringBuilder sb = new StringBuilder();		
		for (Object p: parts) {
			if (p != null) {
				if (sb.length() != 0) {
					sb.append(separator);
				}
				sb.append(p);
			}
		}
		return sb.toString();		
	}
	
	private StringUtils() {		
	}
}