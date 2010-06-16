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


public class Utils
{
	public static String formatMillis(long millis)
	{
		millis /= 1000;
		long h = millis / 3600;
		millis -= 3600 * h;

		long m = millis / 60;
		millis -= 60 * m;

		return "" +
				(h < 10 ? "0" : "") + h + ":" +
				(m < 10 ? "0" : "") + m + ":" +
				(millis < 10 ? "0" : "") + millis
				;
	}
}
