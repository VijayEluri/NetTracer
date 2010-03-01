/**
 * Das muss ein zu zeichnendes Primitiv beherrschen
 */
public interface RenderingPrimitive
{
	/**
	 * Führe mit diesem Strahl einen Schnitttest mit dir selbst durch
	 */
	public Intersection intersectionTest(Ray r);

	/**
	 * Gib mir deine AABB
	 */
	public AABB getAABB();
}
