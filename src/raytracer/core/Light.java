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
 * Irgendein Licht. Welches genau, ist für die weitere Verarbeitung
 * grundsätzlich egal.
 */
public abstract class Light implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	public Vec3 origin;
	public double intensity = 1.0;
	public double decayRate = 0.1;
	public RGBColor color;

	/**
	 * Gibt die Intensität, die dieses Licht auf diesem Material erzeugt,
	 * zurück.
	 * 
	 * Das Ergebnis wird in li[] geschrieben:
	 * li[0] ist der diffuse Anteil
	 * li[1] ist der "spekulare" Anteil (shininess)
	 */
	public abstract void lighting(Material mat, Vec3 L, Vec3 R, 
									Vec3 N, double[] li);
}
