import java.io.Serializable;

/**
 * Bis jetzt wird nicht zwischen Texturen und Materialien unterschieden.
 * 
 * get[XYZ]Color(): Welche Farbe hat dieses Objekt dort?
 * transparency: Wie weit ist es lichtdurchlässig? [0, 1]
 * specularity: Wie sehr spiegelt sich die Umgebung? [0, 1]
 * shininess: Wie sehr sind Lichtquellen sichtbar? [0, 1]
 * shininessSharpness: Wie scharf erscheinen Lichtquellen? [0, 100]
 * roughness: Wie rauh sind Reflektionen bei specularity? [0, 1]
 * roughRays: Wieviele Rays werden für blurry reflections genutzt? [1, inf]
 * cloudiness: Wie verwaschen erscheinen durchscheinende Objekte bei Transparanz? [0, 1]
 * cloudyRays: Wieviele Rays werden für cloudiness genutzt? [1, inf]
 * ior: index of refraction
 */
public abstract class Material implements Serializable
{
	// Nur zur Identifizierung in externen .scn-Files
	public String name;
	
	// Eigenschaften
	public abstract RGBColor getDiffuseColor(Vec3 p);
	public abstract RGBColor getSpecularColor(Vec3 p);
	public abstract RGBColor getTransparentColor(Vec3 p);
	public double transparency;
	public double specularity;
	public double shininess;
	public double shininessSharpness;
	public double roughness;
	public int roughRays = 3;
	public double cloudiness;
	public int cloudyRays = 3;
	public double ior = 1.0;
}
