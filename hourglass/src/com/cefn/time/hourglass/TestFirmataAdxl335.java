package com.cefn.time.hourglass;

import cc.arduino.Arduino;
import processing.core.PApplet;

public class TestFirmataAdxl335 extends PApplet{

	Arduino arduino;
	
	int groundpin = 18;             // analog input pin 4 -- ground
	int powerpin = 19;              // analog input pin 5 -- voltage

	public void setup(){

		//configure arduino
		arduino = new Arduino(this, Arduino.list()[0], 57600);
		
		delay(4000);

		//arduino.pinMode(15, Arduino.ANALOG);
		//arduino.pinMode(16, Arduino.ANALOG);
		//arduino.pinMode(17, Arduino.ANALOG);
				
	}
	
	@Override
	public void draw() {
		if((frameCount % 32) == 0){
			arduino.pinMode(groundpin, Arduino.OUTPUT);
			arduino.pinMode(powerpin, Arduino.OUTPUT);
			arduino.digitalWrite(groundpin, Arduino.LOW);
			arduino.digitalWrite(powerpin, Arduino.HIGH);
			
			int xPin=3,yPin=2,zPin=1;
			//int xPin=17,yPin=16,zPin=15;
			System.out.print(arduino.analogRead(xPin) + " " + arduino.analogRead(yPin) + " " + arduino.analogRead(zPin));
			System.out.println();						
		}
	}
	
}
