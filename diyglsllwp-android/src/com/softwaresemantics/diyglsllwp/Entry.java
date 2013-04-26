package com.softwaresemantics.diyglsllwp;

import android.graphics.Bitmap;

public class Entry {

	private String refId;
	private Bitmap bmp;

	private String description;
	
	private boolean selected;

	public Entry(String refId) {
		this.refId = refId;
		this.description = "waiting for download";
	}

	public String getRefId() {
		return refId;
	}

	public void setRefId(String refId) {
		this.refId = refId;
	}

	public Bitmap getBmp() {
		return bmp;
	}

	public void setBmp(Bitmap bmp) {
		this.bmp = bmp;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String toString() {
		return refId;

	}

	public void setSelected(boolean b) {
		selected = b;		
	}

	public boolean isSelected() {
		return selected;
	}

	
	
}
