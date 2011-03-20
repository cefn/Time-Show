package com.cefn.time.hindsight;

import java.io.File;
import java.util.List;

interface Year {
	int getName();
	List<File> getVideoFiles();
	void setLoadNext(int loadNext);
	int getLoadNext();
	void incrementLoadNext();
}
