/**
 * 
 */
package com.cefn.time.hourglass;


class Funnel extends Bowl{
  Funnel(Vector centre, float radius){
    super(centre,radius);
  }

  public Vector calculateRepulsion(Bouncer bouncer){
    Vector diff = bouncer.pos.minus(centre);
    float hypotenuse = diff.hypotenuse();
	Vector repulsion = super.calculateRepulsion(bouncer);
	float xCentreOffset = bouncer.pos.x - centre.x;
	float yCentreOffset = bouncer.pos.y - centre.y;
	float orbit = App.abs(yCentreOffset);
	if(orbit > radius){ //apply to bouncers at the bottom of the bowl
		float xAcc = - (App.G * xCentreOffset/bouncer.width); //funnel mouth
		float yAcc = (App.abs(xCentreOffset) < bouncer.width) ? 0 : repulsion.y; //bowl sides
		return new Vector(xAcc, yAcc); //things can drop through the middle (no vertical force, but are channelled)
	}
	else{
		return repulsion;
	}		
  }
}