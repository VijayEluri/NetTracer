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

package raytracer.net;

import java.io.*;

public class NetConsole extends PrintStream
{
	private PrintStream parent = null;

	public NetConsole(PrintStream parent)
	{
		super(parent, true);
		this.parent = parent;
	}

	@Override
	public void println(String s)
	{
		synchronized (this)
		{
			parent.println(Thread.currentThread() + "> " + s);
		}
	}

	@Override
	public void println()
	{
		synchronized (this)
		{
			parent.println(Thread.currentThread() + "> ");
		}
	}

	@Override
	public void print(String s)
	{
		synchronized (this)
		{
			parent.print(Thread.currentThread() + "> " + s);
		}
	}
}
