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


/**
 * Hauptanwendung.
 */
public class RaytracerApplication
{
	public static void main(String[] args)
	{
		int i = 0;
		Scene theScene = new Scene();
		String headlessTarget = null;

		if (args.length == 0)
		{
			System.err.println("Gib die zu rendernde Szene als Parameter an.");
			System.exit(1);
		}

		if (args[i].equals("-h"))
		{
			i++;
			headlessTarget = args[i];
			i++;
		}

		if (!theScene.loadScene(args[i]))
		{
			System.err.println("Konnte Szene nicht laden.");
			System.exit(1);
		}

		if (headlessTarget != null)
		{
			if (!theScene.initHeadlessTarget(headlessTarget))
			{
				System.err.println("Kann nicht headless rendern.");
				System.exit(1);
			}
		}

		if (!theScene.prepareScene())
		{
			System.err.println("Konnte Szene nicht vorbereiten.");
			System.exit(1);
		}

		if (!theScene.initPixbufs())
		{
			System.err.println("Konnte Pixelbuffer nicht erstellen.");
			System.exit(1);
		}

		if (!theScene.initUI())
		{
			System.err.println("Konnte User-Interface nicht erstellen.");
			System.exit(1);
		}

		theScene.renderAll();
	}
}
