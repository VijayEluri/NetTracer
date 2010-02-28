import java.io.Serializable;

/**
 * Speichert Informationen über einen erfolgreichen Schnitt. Das wird
 * von den Primitiven selbst gesetzt.
 */
public class Intersection implements Serializable
{
	public RenderingPrimitive winner;
	public Vec3 at;
	public Vec3 normal;
	public double distance;
	
	public Material mat;
	public RGBColor diffuseColor;
	public RGBColor specularColor;
	public RGBColor transparentColor;
	
	public Intersection(RenderingPrimitive winner, Vec3 at, Vec3 normal,
						double distance, Material mat, RGBColor diffuseColor,
						RGBColor specularColor, RGBColor transparentColor)
	{
		this.winner = winner;
		this.at = at;
		this.normal = normal;
		this.distance = distance;
		
		this.mat = mat;
		this.diffuseColor = diffuseColor;
		this.specularColor = specularColor;
		this.transparentColor = transparentColor;
	}
}
