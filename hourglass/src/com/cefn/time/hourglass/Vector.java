/**
 * 
 */
package com.cefn.time.hourglass;

public class Vector {
  final float x;
  final float y;
  public Vector(float x, float y){
    this.x = x;
    this.y = y;
  }
  public String toString(){
	  return "[" + x + "," + y + "]";
  }	  
  Vector plus(Vector toadd){
    return new Vector(this.x + toadd.x, this.y + toadd.y);
  }
  Vector minus(Vector totake){
    return new Vector(this.x - totake.x, this.y - totake.y);
  }
  Vector times(Vector totimes){
    return new Vector(this.x * totimes.x, this.y * totimes.y);
  }
  Vector times(float by){
    return new Vector(this.x * by, this.y * by);
  }
  float squareofhypotenuse(){
    return (x * x) + (y * y);
  }
  float hypotenuse(){
    return StoryboardApp.sqrt(this.squareofhypotenuse());
  }
  Vector unitVector(){
    return this.times(1.0f/this.hypotenuse());
  }
  
  public boolean equals(Object other){
	  if(other == this){
		  return true;
	  }
	  else if(other instanceof Vector){
		  Vector otherVector = Vector.class.cast(other);
		  return x == otherVector.x && y == otherVector.y;		  
	  }
	  else{
		  return false;
	  }
  }
  
  static Vector vectorWithAngle(float angle){ //0 is down the screen
	  return new Vector(App.sin(angle), App.cos(angle));
  }
}