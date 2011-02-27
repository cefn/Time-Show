/**
 * 
 */
package com.cefn.time.hourglass;


class Bowl implements Obstacle{
  Vector centre;
  float radius;
  float hardness = 20f;
  Bowl(Vector centre, float radius){
    this.centre = centre;
    this.radius = radius;
  }
  public Vector calculateRepulsion(Bouncer bouncer){
    Vector diff = this.centre.minus(bouncer.pos);
    float distfromcentre = diff.hypotenuse();
    if(distfromcentre > this.radius){
      float strength = (distfromcentre - radius);
      strength *= strength;
      strength *= hardness;
      return diff.unitVector().times(strength);
    }
    else{
      return StoryboardApp.ZERO;
    }
  } 
}