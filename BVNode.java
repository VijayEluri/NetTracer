import java.util.*;
import java.io.Serializable;

/**
 * Ein Knoten im Tree.
 * 
 * Idee des TreeTraversal (wird aus Scene.java gesteuert):
 * 
 * - Eintrittspunkt in Root finden + Epsilon
 * - Bis Schnitt gefunden oder Baum zuende:
 * 		 - Absteigen bis zum Kind, das diesen Punkt enthält
 * 		 - Intersection-Tests mit den Objekten in diesem Kind
 * 		   ggf. abbrechen, falls Intersection gefunden
 * 		 - Austrittspunkt aus diesem Kind + Epsilon
 * 		 - Aufsteigen, solange Box den Punkt nicht enthält
 * 		 - Wenn bei Wurzel angekommen und die enthält Punkt nicht, dann raus
 * 
 * Angelehnt an den TreeTraversal aus Art of Illusion:
 * http://aoi.sf.net
 */
public class BVNode implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	public static final int THRESHOLD = 10;
	public static final int MAXDEPTH = 12;
	public static final int NODETHRESHOLD = 5;
	public static final double traversalEpsilon = 1e-6;
	
	public AABB space;
	public RenderingPrimitive[] prims;
	public BVNode parent = null;
	public BVNode[] children = new BVNode[8];
	
	/**
	 * Erstelle einen Knoten im Baum um diese Menge herum, die sich im
	 * angegebenen Raum befindet. Wird zu Beginn auf die gesamte
	 * Primitivmenge mit ihrer gemeinsamen AABB angewandt, um den
	 * kompletten Baum zu erstellen.
	 */
	public BVNode(BVNode parent, ArrayList<RenderingPrimitive> set, AABB space, int maxdepth)
	{
		// Merke dir deine eigenen Primitive und deinen Raum
		this.prims = set.toArray(new RenderingPrimitive[0]);
		this.space = space;
		this.parent = parent;
		
		// Wenn du noch zu viele Primitive enthältst, dann unterteile
		// dich weiter.
		if (maxdepth > 0 && prims.length > THRESHOLD)
		{
			// Unterteile Raum in 8 gleiche Teile
			AABB[] subdivs = space.getSpatialSubdivisions();
			
			// Für jedes Raumsegment: Füge ein Kind hinzu, das die
			// Primitive dort enthält.
			for (int s = 0; s < 8; s++)
			{
				AABB oneSpace = subdivs[s];
				
				ArrayList<RenderingPrimitive> primsHere = new ArrayList<RenderingPrimitive>();
				
				for (RenderingPrimitive oneObject : prims)
				{
					if (oneObject.getAABB().intersects(oneSpace))
					{
						primsHere.add(oneObject);
					}
				}
				
				// Füge also dieses Kind mit dieser Menge an Primitiven
				// hinzu. Es gibt also immer Kinder-Objekte, auch wenn
				// diese vielleicht keine Primitive enthalten. Das macht
				// den TreeTraversal einfacher.
				children[s] = new BVNode(this, primsHere, oneSpace, maxdepth - 1);
			}
			
			// Markiere dich als Knoten und nicht als Blatt.
			prims = null;
		}
	}
	
	/**
	 * Suche genau das Blatt, das diesen Punkt enthält.
	 */
	public BVNode findChild(Vec3 p)
	{
		// Vergleiche einfach den Punkt mit den Mittelpunkten der
		// jeweiligen Nodes. Es ist anderweitig sichergestellt, dass
		// der Punkt auch tatsächlich *in* der fraglichen Node liegt.
		// (Rein nach diesem Test könnte er auch außerhalb liegen...)
		
		BVNode current = this;
		while (current.prims == null)
		{
			if (p.x < current.space.center.x)
			{
				if (p.y < current.space.center.y)
				{
					if (p.z < current.space.center.z)
					{
						current = current.children[0];
					}
					else
					{
						current = current.children[1];
					}
				}
				else
				{
					if (p.z < current.space.center.z)
					{
						current = current.children[2];
					}
					else
					{
						current = current.children[3];
					}
				}
			}
			else
			{
				if (p.y < current.space.center.y)
				{
					if (p.z < current.space.center.z)
					{
						current = current.children[4];
					}
					else
					{
						current = current.children[5];
					}
				}
				else
				{
					if (p.z < current.space.center.z)
					{
						current = current.children[6];
					}
					else
					{
						current = current.children[7];
					}
				}
			}
		}
		
		return current;
	}
	
	/**
	 * Steige so weit im Tree auf, bis eine Node gefunden wurde, die
	 * den übergebenen Punkt enthält.
	 */
	public BVNode findNextParent(Vec3 nextPos)
	{
		// Ich bin schon die Root-Node, hier geht es nicht weiter.
		if (this.parent == null)
			return null;
		
		// Gehe so lange nach oben, bis der Punkt enthalten ist.
		BVNode current = this.parent;
		while (!current.space.contains(nextPos))
		{
			current = current.parent;
			
			// Wenn das hier zutrifft, ist die Root-Node erreicht und
			// diese enthält den Punkt nicht. Dann liegt der Punkt
			// nicht mehr im Tree.
			if (current == null)
			{
				return null;
			}
		}
		
		return current;
	}
	/**
	 * Zählt einfach nur die Knoten.
	 */
	public int getNumNodes()
	{
		int out = 1;
		for (BVNode child : children)
			if (child != null)
				out += child.getNumNodes();
		
		return out;
	}
}
