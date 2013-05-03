/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 * 
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
