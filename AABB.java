import java.util.*;
import java.io.Serializable;

public class AABB implements Serializable
{
	// Ortsvektor zum Mittelpunkt
	public Vec3 center;
	
	// Ausdehnungen (kein Ortsvektor!)
	public Vec3 radii;
	
	public Vec3 upperCorner, lowerCorner;
	
	public AABB()
	{
		center = new Vec3();
		radii = new Vec3();
		
		upperCorner = new Vec3();
		lowerCorner = new Vec3();
	}
	
	/**
	 * Eine AABB mit diesen Infos erstellen.
	 */
	public AABB(Vec3 center, Vec3 radii)
	{
		this.center = center;
		this.radii = radii;
		
		upperCorner = center.plus(radii);
		lowerCorner = center.minus(radii);
	}
	
	/**
	 * Eine AABB erstellen, die all diese umfasst.
	 */
	public AABB(ArrayList<AABB> all)
	{
		// Suche Minima und Maxima in allen Richtungen
		
		// TODO: Kosmetik statt +/- 1e100
		Vec3 min = new Vec3(1e100, 1e100, 1e100);
		Vec3 max = new Vec3(-1e100, -1e100, -1e100);
		
		for (AABB a : all)
		{
			Vec3 testPos = a.center.plus(a.radii);
			Vec3 testNeg = a.center.minus(a.radii);
			
			for (int i = 0; i < 3; i++)
			{
				if (testPos.getAxis(i) > max.getAxis(i))
					max.setAxis(i, testPos.getAxis(i));
				
				if (testNeg.getAxis(i) < min.getAxis(i))
					min.setAxis(i, testNeg.getAxis(i));
			}
		}
		
		radii = max.minus(min).times(0.5);
		center = max.plus(min).times(0.5);
		
		upperCorner = center.plus(radii);
		lowerCorner = center.minus(radii);
	}
	
	/**
	 * Eine AABB erstellen, die all diese Primitive umfasst. Dazu intern
	 * eine AABB um alle AABB's dieser Primitive erstellen.
	 * Der Dummy ist nur dazu da, um diese Signatur von der obigen zu
	 * unterscheiden.....
	 */
	public AABB(ArrayList<RenderingPrimitive> set, int dummy)
	{
		ArrayList<AABB> objs = new ArrayList<AABB>();
		for (RenderingPrimitive r : set)
			if (r != null)
				objs.add(r.getAABB());
		
		if (objs.size() > 0)
		{
			AABB allTogether = new AABB(objs);
			
			center = allTogether.center;
			radii = allTogether.radii;
		}
		else
		{
			center = new Vec3();
			radii = new Vec3();
		}
		
		upperCorner = center.plus(radii);
		lowerCorner = center.minus(radii);
	}
	
	/**
	 * Unterteile den Raum, der von dieser AABB gebildet wird, in 8
	 * gleich große Teile.
	 */
	public AABB[] getSpatialSubdivisions()
	{
		AABB[] out = new AABB[8];
		
		Vec3 newRadii = radii.times(0.5);
		int index = 0;
		
		for (int a = -1; a <= 1; a += 2)
		{
			for (int b = -1; b <= 1; b += 2)
			{
				for (int c = -1; c <= 1; c += 2)
				{
					Vec3 newCenter = new Vec3(center);
					newCenter.x += newRadii.times(a).x;
					newCenter.y += newRadii.times(b).y;
					newCenter.z += newRadii.times(c).z;
					
					out[index++] = new AABB(newCenter, newRadii);
				}
			}
		}
		
		return out;
	}
	
	/**
	 * Schneidet diese Box eine andere?
	 */
	public boolean intersects(AABB box)
	{
		// Schau dir die 3 Raumachsen an. Betrachte in dieser Richtung
		// alleine den Abstand beider Boxmittelpunkte, sowie die Summe
		// ihrer Radien. Ist der Mittelpunktsabstand für eine Dimension
		// größer als die Radiensumme? Dann *können* sie sich nicht
		// schneiden.
		
		for (int i = 0; i < 3; i++)
		{
			double projc = Math.abs(center.getAxis(i) - box.center.getAxis(i));
			double projl = radii.getAxis(i) + box.radii.getAxis(i);
			
			if (projc > projl)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Führt einen Strahl-AABB-Schnitttest durch und setzt ggf. gleich
	 * die Strahl-Parameter tIn und tOut, an denen der Strahl diese Box
	 * betritt bzw. wieder verlässt.
	 * 
	 * out[0] ist Eintritt, out[1] ist Austritt.
	 */
	public boolean alphaEntryExit(Ray r, double[] out)
	{
		// Smits method, siehe auch:
		// http://cag.csail.mit.edu/~amy/papers/box-jgt.pdf
		
		double tmin, tmax, tymin, tymax, tzmin, tzmax;
		
		// Suche je nach Richtung des Strahls den Schnittparameter des
		// Strahls mit der Box in X-Richtung: Für Schnitt mit vorderer
		// Begrenzungsebene und hinterer Ebene.
		if (r.reciDir.x >= 0.0)
		{
			tmin = (lowerCorner.x - r.origin.x) * r.reciDir.x;
			tmax = (upperCorner.x - r.origin.x) * r.reciDir.x;
		}
		else
		{
			tmin = (upperCorner.x - r.origin.x) * r.reciDir.x;
			tmax = (lowerCorner.x - r.origin.x) * r.reciDir.x;
		}
		
		// Dasselbe in Y-Richtung für die untere bzw. obere Ebene.
		if (r.reciDir.y >= 0.0)
		{
			tymin = (lowerCorner.y - r.origin.y) * r.reciDir.y;
			tymax = (upperCorner.y - r.origin.y) * r.reciDir.y;
		}
		else
		{
			tymin = (upperCorner.y - r.origin.y) * r.reciDir.y;
			tymax = (lowerCorner.y - r.origin.y) * r.reciDir.y;
		}
		
		// Ist eines hiervon wahr, dann läuft der Strahl links bzw. rechts
		// an der Box vorbei und kann sie nicht mehr schneiden.
		// (Einfach mal aufmalen ...)
		if ((tmin > tymax) || (tymin > tmax))
			return false;
		
		// Okay, wir sind noch da. Das heißt, wenn man nur X- und Y-
		// Richtung betrachtet, dann könnte der Strahl die Box schneiden.
		// Jetzt muss noch überprüft werden, ob das auch in Z-Richtung
		// zutrifft oder ob der Strahl vor bzw. hinter der Box vorbei-
		// läuft.
		
		// Suche dir die beiden Parameter, die den Strahl am nächsten an
		// die Box heranführen (nur in X-/Y-Richtung). Dadurch wird aus
		// der Betrachtung eine der beiden Achsen "fallengelassen", was
		// es dann erlaubt, die Sichtweise zu kippen und denselben Test
		// für die Z-Ebene durchzuführen.
		if (tymin > tmin)
			tmin = tymin;
		if (tymax < tmax)
			tmax = tymax;
		
		// Suche jetzt die Schnittparameter für die begrenzenden Ebenen
		// in Z-Richtung.
		if (r.reciDir.z >= 0.0)
		{
			tzmin = (lowerCorner.z - r.origin.z) * r.reciDir.z;
			tzmax = (upperCorner.z - r.origin.z) * r.reciDir.z;
		}
		else
		{
			tzmin = (upperCorner.z - r.origin.z) * r.reciDir.z;
			tzmax = (lowerCorner.z - r.origin.z) * r.reciDir.z;
		}
		
		// Noch einmal den Test von oben, aber diesmal Z-Richtung mit
		// kombinierter X-/Y-Richtung.
		if ((tmin > tzmax) || (tzmin > tmax))
			return false;
		
		// Führe jetzt die Parameter wieder am nächsten an die Box heran,
		// also suche wie oben das Maximum der Eintrittsparameter und das
		// Minimum der Austrittsparameter. Dadurch entstehen die finalen
		// Schnittparameter tmin und tmax.
		if (tzmin > tmin)
			tmin = tzmin;
		if (tzmax < tmax)
			tmax = tzmax;
		
		// Wenn tmin < 0, dann war der Strahlursprung schon innerhalb
		// der Box.
		if (tmin < 0.0)
			out[0] = 0.0;
		else
			out[0] = tmin;
		
		out[1] = tmax;
		
		// Zuletzt noch sicherstellen, dass die Schnittparameter auch
		// im positiven Bereich liegen. Sonst würde die Box in negativer
		// Strahlrichtung geschnitten werden.
		return (tmax > 0.0);
	}
	
	/**
	 * Für's TreeTraversal: Unter der Voraussetzung, dass der Strahl die
	 * Box tatsächlich valide schneidet, berechne nur den Parameter tmax,
	 * an dem der Strahl die Box wieder verlässt. Kein Schnitttest!
	 * 
	 * Zurechtgestutzte, unkommentierte Version der vorherigen Methode.
	 */
	public double alphaExit(Ray r)
	{
		double tmax, tymax, tzmax;
		
		if (r.reciDir.x >= 0.0)
			tmax = (upperCorner.x - r.origin.x) * r.reciDir.x;
		else
			tmax = (lowerCorner.x - r.origin.x) * r.reciDir.x;
		
		if (r.reciDir.y >= 0.0)
			tymax = (upperCorner.y - r.origin.y) * r.reciDir.y;
		else
			tymax = (lowerCorner.y - r.origin.y) * r.reciDir.y;
		
		if (tymax < tmax)
			tmax = tymax;
		
		if (r.reciDir.z >= 0.0)
			tzmax = (upperCorner.z - r.origin.z) * r.reciDir.z;
		else
			tzmax = (lowerCorner.z - r.origin.z) * r.reciDir.z;
		
		return (tzmax < tmax ? tzmax : tmax);
	}
	
	/**
	 * Enthält diese Box den angegebenen Punkt?
	 */
	public boolean contains(Vec3 p)
	{
		if (   p.x < lowerCorner.x || p.x > upperCorner.x
			|| p.y < lowerCorner.y || p.y > upperCorner.y
			|| p.z < lowerCorner.z || p.z > upperCorner.z)
			return false;
		
		return true;
	}
	
	public String toString()
	{
		return "Center: " + center + ", Radii: " + radii;
	}
}
