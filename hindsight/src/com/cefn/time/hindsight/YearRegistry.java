package com.cefn.time.hindsight;

import java.util.Random;

interface YearRegistry{
	void add(Year year);
	int size();
	Year yearAt(int pos);
	Year yearCalled(int name);
	Year randomYear(Random random);
	int nameBefore(int name, boolean wrap);
	int nameAfter(int name, boolean wrap);
}
