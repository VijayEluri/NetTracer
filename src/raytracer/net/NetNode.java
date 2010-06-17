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

import raytracer.core.*;
import java.io.*;
import java.net.*;

public class NetNode
{
	public static int threads = Runtime.getRuntime().availableProcessors();

	public static void main(String[] args) throws Exception
	{
		String host = "localhost";
		int port = 7431;

		System.setOut(new NetConsole(System.out));
		System.setErr(new NetConsole(System.err));

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-h"))
			{
				host = args[i + 1];
				i++;
			}
			if (args[i].equals("-p"))
			{
				port = new Integer(args[i + 1]);
				i++;
			}
			if (args[i].equals("-t"))
			{
				threads = new Integer(args[i + 1]);
				i++;
			}
		}

		ServerSocket serv = new ServerSocket(port, 0,
				InetAddress.getByName(host));

		System.out.println("Node bereit.");

		while (true)
		{
			Socket cli = serv.accept();
			new Handler(cli).start();
		}
	}

	/**
	 * Hier wird gerechnet.
	 */
	private static class Handler extends Thread
	{
		public Socket s = null;
		public Scene theScene = null;
		public int rowsDone = 0;

		public Handler(Socket s)
		{
			this.s = s;
		}

		public void handle() throws Exception
		{
			ObjectOutputStream oos
				= new ObjectOutputStream(s.getOutputStream());

			System.out.println("Sende meine Version.");
			oos.writeInt(NetCodes.VERSION);
			oos.flush();
			System.out.println("Fertig.");

			ObjectInputStream ois
				= new ObjectInputStream(s.getInputStream());

			int firstCmd = ois.readInt();
			if (firstCmd == NetCodes.QUERY_THREADS)
			{
				System.out.println("Gebe Threadzahl bekannt.");
				oos.writeInt(threads);
				oos.flush();
				return;
			}

			System.out.println("Warte auf Szene...");
			theScene = (Scene)ois.readObject();
			System.out.println("Habe Szene.");

			System.out.println("Bereite Szene vor...");
			if (!theScene.prepareScene())
			{
				System.out.println("Konnte Szene nicht vorbereiten.");
				oos.writeInt(NetCodes.QUIT);
				oos.flush();
				return;
			}
			System.out.println("Szene bereit.");

			boolean run = true;
			int state = 0;
			while (run)
			{
				switch (state)
				{
					case 0:
						System.out.println("Frage nach Arbeit...");
						oos.writeInt(NetCodes.REQUEST_JOB);
						oos.flush();

						int yOff = ois.readInt();

						if (yOff == -1)
						{
							System.out.println("Gegenseite sagt: Bitte warten.");
							Thread.sleep(1000);
						}
						else if (yOff == -2)
						{
							System.out.println("Gegenseite sagt: Fertig.");
							state = -1;
							theScene.pixels = null;
							theScene.criticalPixels = null;
						}
						else
						{
							int rows = ois.readInt();
							int toid = ois.readInt();
							int type = ois.readInt();
							int allocd = ois.readInt();

							System.out.println("Habe Job: " + yOff + ", " + rows
									+ ", " + type);

							int reply = -1;
							if (type == 0)
							{
								// Primäre Strahlen.
								if (!theScene.renderPartialPrimary(yOff, rows))
								{
									System.out.println("Fehler beim Rendern.");
									state = -1;
								}
								else
								{
									reply = NetCodes.JOB_COMPLETED;
								}
							}
							else if (type == 1)
							{
								// Antialiasing. Dafür brauche ich das Array der
								// kritischen Pixel.
								if (theScene.criticalPixels == null)
								{
									System.out.println("Benötige kritische Pixel.");
									oos.writeInt(NetCodes.REQUEST_CRITICAL);
									oos.flush();

									theScene.criticalPixels = (boolean[][])ois.readObject();
									System.out.println("Habe kritische Pixel erhalten.");
								}

								if (!theScene.renderPartialAntiAlias(yOff, rows))
								{
									System.out.println("Fehler beim Rendern.");
									state = -1;
								}
								else
								{
									reply = NetCodes.JOB_COMPLETED_AA;
								}
							}

							if (reply != -1)
							{
								System.out.println("Job fertig, sende zurück...");
								oos.writeInt(reply);
								oos.writeInt(yOff);
								oos.writeInt(toid);
								oos.writeInt(allocd);
								oos.writeObject(theScene.pixels);
								oos.flush();
								System.out.println("Job gesendet.");
								state = 0;

								// FIXME: So ein OutputStream cached einiges. Wenn er
								// also die neuen Pixel beim nächsten Mal verschicken
								// soll, dann muss das Objekt auch neu erzeugt werden.
								// Erzwinge dies.
								theScene.pixels = null;

								// Speicher sparen. In der Realität überprüft, das
								// bringt wirklich etwas.
								oos.reset();
								System.gc();

								rowsDone += rows;
							}
						}
						break;

					default:
						oos.writeInt(NetCodes.QUIT);
						oos.flush();
						run = false;
				}
			}
		}

		@Override
		public void run()
		{
			try
			{
				handle();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (s != null)
				{
					try
					{
						s.shutdownInput();
						s.shutdownOutput();
						s.close();
					}
					catch (Exception e) {} // ignore
				}
			}

			System.out.println("Thread zuende. "
					+ rowsDone + " Zeilen bearbeitet.");
		}
	}
}
