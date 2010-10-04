package com.cefn.time;

import java.util.ArrayList;
import java.util.List;


public class AbstractTraveller implements Traveller{

	private int year;
	private List<Listener> listeners = new ArrayList<Listener>();
	public int getYearName() {
		return year;
	}
	public void setYearName(int year) {
		this.year = year;
		for(Listener l:listeners){
			l.yearChanged(this);
		}
	}
	public boolean addListener(Listener l) {
		return listeners.add(l);
	}
	public boolean removeListener(Listener l) {
		return listeners.remove(l);
	}
	
}
