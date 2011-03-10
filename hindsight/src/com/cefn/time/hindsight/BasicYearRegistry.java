package com.cefn.time.hindsight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import processing.core.PApplet;

public class BasicYearRegistry implements YearRegistry {
	List<Year> yearList = new ArrayList<Year>();
	Map<Integer, Year> yearMap = new HashMap<Integer, Year>();

	public Year randomYear(Random random) {
		return yearAt((int)Math.floor(random.nextInt(yearList.size())));
	}

	public void add(Year year) {
		yearList.add(year);
		yearMap.put(year.getName(), year);
		Collections.sort(yearList, new Comparator<Year>() {
			public int compare(Year y1, Year y2) {
				Integer i1 = new Integer(y1.getName());
				Integer i2 = new Integer(y2.getName());
				return i1.compareTo(i2);
			}
		});
	}

	public int size() {
		return yearList.size();
	}

	public Year yearAt(int pos) {
		return yearList.get(pos);
	}

	public Year yearCalled(int name) {
		return yearMap.get(name);
	}

	private int indexOf(Year year) {
		return yearList.indexOf(year);
	}

	public int nameAfter(int name, boolean wrap) {
		int newIndex = indexOf(yearCalled(name)) + 1;
		if(newIndex >= yearList.size()){
			if(!wrap){
				return -1;
			}
			else{
				newIndex = newIndex % yearList.size(); 
			}
		}
		return yearAt(newIndex).getName();
	}

	public int nameBefore(int name, boolean wrap) {
		int newIndex = indexOf(yearCalled(name)) - 1;
		if(newIndex < 0){
			if(!wrap){
				return -1;
			}
			else{
				newIndex = (newIndex + yearList.size()) % yearList.size(); 
			}
		}
		return yearAt(newIndex).getName();
	}

}
