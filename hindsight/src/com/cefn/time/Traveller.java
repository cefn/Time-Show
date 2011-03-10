package com.cefn.time;

public interface Traveller {
	
	void setYearName(int year);
	int getYearName();
	
	boolean addListener(Listener l);
	boolean removeListener(Listener l);
	
	interface Listener{
		void yearChanged(Traveller traveller);
	}

}
