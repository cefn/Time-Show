package com.cefn.time;

import codeanticode.gsvideo.GSMovie;

public interface Tunable {
		
		void incrementFrequency();
		void decrementFrequency();
		GSMovie getMovie();
		float getMovieOpacity();
		double getMovieVolume();
		double getNoiseVolume();
			
}
