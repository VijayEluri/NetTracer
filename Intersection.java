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


import java.io.Serializable;

/**
 * Speichert Informationen über einen erfolgreichen Schnitt. Das wird
 * von den Primitiven selbst gesetzt.
 */
public class Intersection implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

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
