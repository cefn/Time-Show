package com.cefn.time.hindsight;

import java.io.File;
import java.util.List;

class BasicYear implements Year{
	int loadNext = 0;
	int name;
	List<File> videoFiles;
	BasicYear(int name, List<File> videoFiles){
		this.name = name;
		this.videoFiles = videoFiles;
	}
	public int getName(){ return name; }
	public List<File> getVideoFiles(){return videoFiles; }
	public int getLoadNext(){
		return loadNext;
	}
	public void setLoadNext(int loadNext){
		this.loadNext = loadNext;
	}
	public void incrementLoadNext(){
		loadNext++;
	}
}