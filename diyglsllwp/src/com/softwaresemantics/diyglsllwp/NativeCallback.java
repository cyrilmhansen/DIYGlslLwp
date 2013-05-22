package com.softwaresemantics.diyglsllwp;

public interface NativeCallback {
	public void onRequirementFailure(String msg);
	public void onResumeGDX();
	public void notifyCompilationEnd();
	public void notifyCompilation();
}
