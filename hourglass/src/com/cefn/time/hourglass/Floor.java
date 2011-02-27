/**
 * 
 */
package com.cefn.time.hourglass;


class Floor implements Obstacle{
  float y;
  float hardness;
  boolean up ;
  Floor(float y, float hardness, boolean up){
    this.y = y;
    this.hardness = hardness;
    this.up = up;
  }
  public Vector calculateRepulsion(Bouncer other){
    float ydiff = other.pos.y - this.y;
    if((up && ydiff > 0) || (!up && ydiff < 0)){
      return new Vector(0, hardness * ydiff * (up?-1.0f:1.0f));
    }
    else{
      return new Vector(0,0);
    }
  }
}