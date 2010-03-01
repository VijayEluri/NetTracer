import java.util.*;
import java.net.*;
import java.io.*;

public class NetMaster
{
	public static Scene theScene = null;
	public static RGBColor[][] pixels = null;

	public static int bunchsize = 1;
	public static short[] tokens = null;

	public static short free = 0;
	public static short target = 2;

	public static void loadScene(String path)
	{
		theScene = new Scene();
		if (!theScene.loadScene(path))
		{
			System.err.println("Konnte Szene nicht laden.");
			System.exit(1);
		}

		// Lokalen Buffer erzeugen, der aber nur null's enthält. Die
		// Daten kommen von den Clients, wir brauchen hier keine
		// fertigen schwarzen Pixel.
		pixels = new RGBColor[theScene.set.sizeY][];
		for (int y = 0; y < theScene.set.sizeY; y++)
			pixels[y] = new RGBColor[theScene.set.sizeX];

		// Tokens. Ein Token meint <bunchsize> Zeilen.
		tokens = new short[
				(int)Math.ceil(theScene.set.sizeY / (double)bunchsize)];
	}

	/**
	 * 0 : offset
	 * 1 : rows
	 * 2 : token-index
	 * 3 : type (0 = regular, 1 = antialiasing)
	 * 4 : alloc'd
	 */
	public static int[] getFreeJob(int desiredTokens, int lasttype)
	{
		int startToken = -1;
		int type = -1;
		int allocd = 0;
		synchronized (tokens)
		{
			// Primärer Job oder Antialiasing?
			if (target == 2)
				type = 0;
			else
				type = 1;

			// Wenn der Tokentyp, den dieser Client zuletzt bearbeitet hat,
			// nicht mehr mit dem aktuellen Typ übereinstimmt, dann gib
			// maximal ein Token zurück.
			if (lasttype != type)
				desiredTokens = 1;

			// Versuche, <desiredTokens> Stück freie Token zu bekommen, die
			// aneinander liegen.
			for (int i = 0; i < tokens.length; i++)
			{
				if (tokens[i] == free)
				{
					startToken = i;

					// Hier fangen wir an. Solange das aktuell betrachtete Token
					// noch frei ist und wir noch mehr wollen, reservieren wir.
					while (i < tokens.length
							&& tokens[i] == free
							&& allocd < desiredTokens)
					{
						tokens[i]++;
						allocd++;
						i++;
					}

					// Okay, raus aus der for-Schleife.
					break;
				}
			}

			// Nichts mehr frei?
			if (allocd == 0)
				return null;
		}

		// Von Tokens in Pixelgrenzen umrechnen.
		int yOff = startToken * bunchsize;
		int rows = bunchsize * allocd;

		if (yOff + rows >= theScene.set.sizeY)
			rows = theScene.set.sizeY - yOff;

		return new int[] {yOff, rows, startToken, type, allocd};
	}

	public static short getCurrentTarget()
	{
		synchronized (tokens)
		{
			return target;
		}
	}

	public static void setCompleted(int start, int num)
	{
		synchronized (tokens)
		{
			// Markiere alle als fertig.
			for (int i = start; i < tokens.length && i - start < num; i++)
				tokens[i]++;

			// Status ausgeben. Wir hätten gerne <targetBoxes> viele Boxes,
			// die jeweils den Status in einem bestimmten Bereich anzeigen.
			int targetBoxes = 10;
			int border = tokens.length / targetBoxes;
			int bunchDone = 0;
			int bunchTota = 0;
			String outstr = "";
			for (int i = 0; i < tokens.length; i++)
			{
				if (tokens[i] == target)
					bunchDone++;
				bunchTota++;

				if (((i + 1) % border) == 0)
				{
					int perc = (int)(bunchDone / (double)bunchTota * 100.0);
					outstr += "[";
					if (perc < 10)
						outstr += "0" + perc;
					else if (perc == 100)
						outstr += "++";
					else
						outstr += "" + perc;
					outstr += "]";

					bunchDone = 0;
					bunchTota = 0;
				}
			}
			if (bunchTota > 0)
			{
				int perc = (int)(bunchDone / (double)bunchTota * 100.0);
				outstr += "[";
				if (perc < 10)
					outstr += "0" + perc;
				else if (perc == 100)
					outstr += "++";
				else
					outstr += "" + perc;
				outstr += "]";
			}
			System.out.println(outstr);

			// Schau nach, ob wir jetzt fertig sind.
			for (int i = 0; i < tokens.length; i++)
				if (tokens[i] != target)
					return;

			// Phase beendet. Speichere Pixel in Szene.
			theScene.pixels = pixels;

			// Wir sind mit den primären Strahlen fertig. Jetzt AA?
			if (target == 2 && theScene.set.AARays > 0)
			{
				free = 3;
				target = 5;

				// Alle Tokens wieder auf "frei" setzen.
				for (int i = 0; i < tokens.length; i++)
					tokens[i] = free;
			}
		}
	}

	public static int getThreadsForClient(String host, int port)
	{
		Socket s = null;
		int numthreads = -1;
		try
		{
			s = new Socket(host, port);
			System.out.println("Frage " + host + ":" + port + " nach Threads.");

			ObjectInputStream ois =
				new ObjectInputStream(s.getInputStream());

			if (ois.readInt() >= NetCodes.VERSION)
			{
				System.out.println("Version ok.");

				ObjectOutputStream oos =
					new ObjectOutputStream(s.getOutputStream());

				oos.writeInt(NetCodes.QUERY_THREADS);
				oos.flush();
				System.out.println("Warte auf Antwort...");
				numthreads = ois.readInt();
				System.out.println("Node kann " + numthreads + " Threads.");
			}
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

		return numthreads;
	}

	public static ArrayList<Thread> spawnHandlersFor(String host, int port)
		throws Exception
	{
		int numthreads = getThreadsForClient(host, port);
		ArrayList<Thread> spawned = new ArrayList<Thread>();

		for (int i = 0; i < numthreads; i++)
		{
			Handler h = new Handler();
			h.host = host;
			h.port = port;

			h.start();
			spawned.add(h);
		}

		return spawned;
	}

	public static synchronized void findCriticalPixels()
	{
		if (theScene.criticalPixels == null)
		{
			// Array bauen.
			theScene.criticalPixels = new boolean[theScene.set.sizeY][];
			for (int y = 0; y < theScene.set.sizeY; y++)
				theScene.criticalPixels[y] = new boolean[theScene.set.sizeX];

			// Array füllen, nutze vorhandenen Code.
			theScene.renderPhase(1, -1);
		}
	}

	public static void main(String[] args) throws Exception
	{
		long t_start = System.currentTimeMillis();

		if (args.length != 2)
		{
			System.err.println("Parameter: <Szene> <Zieldatei>");
			System.exit(1);
		}

		String scenePath = args[0];
		String targetPath = args[1];

		System.setOut(new NetConsole(System.out));
		System.setErr(new NetConsole(System.err));

		loadScene(scenePath);

		System.out.println("Lese Nodes von STDIN, beende mit EOF:");
		ArrayList<String> hosts = new ArrayList<String>();
		ArrayList<Integer> ports = new ArrayList<Integer>();
		Scanner sin = new Scanner(System.in);
		while (sin.hasNext())
		{
			String line = sin.nextLine();
			String[] split = line.split(":");
			hosts.add(split[0]);
			ports.add(new Integer(split[1]));
		}

		// Rendern lassen.
		ArrayList<Thread> running = new ArrayList<Thread>();
		for (int i = 0; i < hosts.size(); i++)
		{
			running.addAll(spawnHandlersFor(hosts.get(i), ports.get(i)));
		}

		// Warten, bis die alle fertig sind.
		for (Thread t : running)
			t.join();

		// Bild schreiben.
		TIFFWriter.writeRGBImage(theScene, new File(targetPath));

		long t_end = System.currentTimeMillis();
		long s = t_end - t_start;
		s /= 1000;
		long h = s / 3600;
		s -= 3600 * h;

		long m = s / 60;
		s -= 60 * m;

		System.out.println("Verstrichene Gesamtzeit: " +
				(h < 10 ? "0" : "") + h + ":" +
				(m < 10 ? "0" : "") + m + ":" +
				(s < 10 ? "0" : "") + s
				);
	}

	/**
	 * Hiermit wird eine Node bedient.
	 */
	private static class Handler extends Thread
	{
		public String host = null;
		public int port = -1;

		public Socket s = null;

		public void handle() throws Exception
		{
			System.out.println("Verbinde zu: " + host + ":" + port);
			s = new Socket(host, port);
			System.out.println("Verbunden mit: " + host + ":" + port);

			System.out.println("Teste Version...");
			ObjectInputStream ois =
				new ObjectInputStream(s.getInputStream());

			if (ois.readInt() < NetCodes.VERSION)
				return;
			System.out.println("Version ok.");

			ObjectOutputStream oos =
				new ObjectOutputStream(s.getOutputStream());

			// NOOP senden, also *nicht* Threads abfragen.
			oos.writeInt(NetCodes.NOOP);

			System.out.println("Sende Szene...");
			oos.writeObject(theScene);
			oos.flush();
			System.out.println("Szene gesendet.");

			int yOff, toid, allocd;
			RGBColor[][] px;
			boolean run = true;
			long t_start = 0, t_end = 0;
			int jobsize = 1, lastjobsize = 1;
			int lasttype = -1;
			while (run)
			{
				int cmd = ois.readInt();
				switch (cmd)
				{
					case NetCodes.REQUEST_JOB:
						System.out.println("Jobanfrage.");

						// Schätze linear ab, wieviele Tokens es bräuchte, damit
						// dieser Job eine vernünftige Zeit braucht.
						if (t_start != 0 && t_end != 0)
						{
							double millidiff = (double)(t_end - t_start);

							jobsize = (int)((lastjobsize / millidiff) * 5000);

							if (jobsize < 1)
								jobsize = 1;
							else if (jobsize > 20)
								jobsize = 20;
						}

						// getFreeJob() wird der letzte Typ mitgegeben, damit dieses
						// dafür sorgen kann, dass nicht zu viele Tokens allokiert
						// werden. Bei dem Wechsel auf AA kann die Performance
						// grundlegend anders sein.
						int[] jobInfo = getFreeJob(jobsize, lasttype);
						if (jobInfo == null)
						{
							if (getCurrentTarget() == 2 && theScene.set.AARays > 0)
							{
								// Im Moment ist nichts frei, aber es kommt noch
								// Antialiasing. Bitte warten.
								System.out.println("Muss warten.");
								oos.writeInt(-1);
							}
							else
							{
								// Du kannst gehen, es ist gar nichts mehr da.
								System.out.println("Keine Jobs mehr vorhanden.");
								oos.writeInt(-2);
							}
						}
						else
						{
							// Merke dir, ob dieser Job regular oder antialiasing war.
							// Merke dir auch, wie groß er tatsächlich war.
							lasttype = jobInfo[3];
							lastjobsize = jobInfo[4];

							System.out.println("Sende jobInfo, Tokens: " + lastjobsize);
							for (int i = 0; i < jobInfo.length; i++)
							{
								oos.writeInt(jobInfo[i]);
							}

							t_start = System.currentTimeMillis();
						}
						oos.flush();
						break;

					case NetCodes.REQUEST_CRITICAL:
						System.out.println("Anfrage für kritische Pixel.");
						findCriticalPixels();
						oos.writeObject(theScene.criticalPixels);
						oos.flush();
						break;

					case NetCodes.JOB_COMPLETED:
						System.out.println("Empfange Ergebnis.");
						yOff = ois.readInt();
						toid = ois.readInt();
						allocd = ois.readInt();

						px = (RGBColor[][])ois.readObject();

						// Ergebnis im Ziel einhängen.
						for (int y = 0; y < px.length; y++)
						{
							pixels[y + yOff] = px[y];
						}

						System.out.println("Ergebnis erhalten, Token fertig.");
						setCompleted(toid, allocd);
						t_end = System.currentTimeMillis();
						break;

					case NetCodes.JOB_COMPLETED_AA:
						System.out.println("Empfange AA-Ergebnis.");
						yOff = ois.readInt();
						toid = ois.readInt();
						allocd = ois.readInt();

						px = (RGBColor[][])ois.readObject();
						System.out.println("AA-Ergebnis erhalten. Addiere.");

						// Ergebnis im Ziel dazuaddieren. Nur kritische Pixel.
						for (int y = 0; y < px.length; y++)
							for (int x = 0; x < px[0].length; x++)
								if (theScene.criticalPixels[y + yOff][x])
									pixels[y + yOff][x].addAllSamples(px[y][x]);

						System.out.println("Addition fertig, Token fertig.");
						setCompleted(toid, allocd);
						t_end = System.currentTimeMillis();
						break;

					case NetCodes.QUIT:
					default:
						System.out.println("Beende.");
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
		}
	}

	/**
	 * Interface für irgendeine Konsole, könnte auch GUI sein.
	 */
	private static interface Console
	{
		public void println(String s);
	}
}

// vim: set ts=2 sw=2 :
