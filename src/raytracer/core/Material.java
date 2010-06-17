/*
	Copyright 2008, 2009, 2010  Peter Hofmann

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful, but
	WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package raytracer.core;

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
	private static final long serialVersionUID = 20100301001L;

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
