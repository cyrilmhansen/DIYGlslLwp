/*******************************************************************************
 * Cyril M. Hansen 2013
 * 
 * Licences :
 * Creative Commons Attribution-ShareAlike 3.0
 * Creative Commons Attribution - Partage dans les Mêmes Conditions 3.0 France
 * 
 * http://creativecommons.org/licenses/by-sa/3.0
 * http://creativecommons.org/licenses/by-sa/3.0/fr/
 * 
 * Sources :
 * https://github.com/cyrilmhansen/DIYGlslLwp
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

/**
 * 
 * @author cmh
 *
 */
public class ShaderProgram {
	
	private String fragShaderText;
	private String vertShaderText;
	
	
	public String getFragShaderText() {
		return fragShaderText;
	}
	public void setFragShaderText(String fragShaderText) {
		this.fragShaderText = fragShaderText;
	}
	public String getVertShaderText() {
		return vertShaderText;
	}
	public void setVertShaderText(String vertShaderText) {
		this.vertShaderText = vertShaderText;
	}
	

}
