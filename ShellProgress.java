import java.io.Serializable;

/**
 * Selben Mechanismus wie in der GUI benutzen, aber nur an der Shell
 * kurz ansagen, wie weit er ist.
 */
public class ShellProgress implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	public static final int barlen = 25;
	public static final int margin = 15;

	public ShellProgress(final Scene tr, final int height)
	{
		Thread t = new Thread()
		{
			public void run()
			{
				int lastdone = -1;
				long start = System.currentTimeMillis();

				while (true)
				{
					synchronized (tr.repaintQueue)
					{
						while (tr.repaintQueue.isEmpty())
						{
							try
							{
								tr.repaintQueue.wait();
							}
							catch (Exception e)
							{
								System.err.println("* Progress bar thread died.");
								return;
							}
						}

						tr.repaintQueue.removeFirst();
					}

					// Kurz mal piep sagen, es geht noch weiter.
					if (tr.tpos != null)
					{
						int maxpos = -1;
						int[] positions = tr.tpos.get();
						for (int i = 0; i < positions.length; i++)
							if (positions[i] > maxpos)
								maxpos = positions[i];

						double frac = (double)maxpos / (double)height;
						int done = (int)(frac * barlen);
						int todo = barlen - done;

						// Sorgt dafür, dass garantiert keine Ausgabe nach der
						// "Fertig"-Zeile erfolgt...
						synchronized (tr.running)
						{
							if (tr.running)
							{
								// Zeichne die Progress bar.
								//
								// Wichtiger Trick: CR am Ende der Bar schreiben, denn das
								// bewirkt, dass folgende Zeilen die Bar überschreiben --
								// das betrifft auch Zeilen, die nicht von dieser Klasse
								// geschrieben werden, also insbesondere die
								// "Fertig..."-Zeile.
								if (done != lastdone)
								{
									System.out.print("[");
									for (int i = 0; i < done; i++)
										System.out.print("#");
									for (int i = 0; i < todo; i++)
										System.out.print("-");
									System.out.print("]");

									// Zeit linear abschätzen.
									long now = System.currentTimeMillis();
									long diff = now - start;
									long total = (long)((double)diff / frac);
									long s = total - diff;

									System.out.print(" ");

									s /= 1000;

									if (s > 0)
									{
										long h = s / 3600;
										s -= 3600 * h;

										long m = s / 60;
										s -= 60 * m;

										System.out.print(
													(h < 10 ? "0" : "") + h + ":" +
													(m < 10 ? "0" : "") + m + ":" +
													(s < 10 ? "0" : "") + s
												);
									}

									System.out.print("\r");
									lastdone = done;
								}
							}
						}
					}
				}
			}
		};
		t.start();
	}

	public static void clearLine()
	{
		for (int i = 0; i < barlen + margin; i++)
			System.out.print(" ");
		System.out.print("\r");
	}
}
