package com.cefn.time;

interface YearRegistry{
	void add(Year year);
	int size();
	Year yearAt(int pos);
	Year yearCalled(int name);
	Year randomYear();
	int nameBefore(int name);
	int nameAfter(int name);
}
