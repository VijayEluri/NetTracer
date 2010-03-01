import java.io.File;
import java.util.Scanner;
import java.io.Serializable;

/**
 * Kapselt einen Scanner und gibt statt einer Zeile direkt die gesplitteten
 * Tokens zurück. Ist auch für das Ausfiltern von Kommentaren zuständig.
 */
public class SceneReader implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	private Scanner in = null;
	private File infile = null;

	/**
	 * Öffnet einen neuen Scanner für diesen Pfad.
	 */
	public SceneReader(String path) throws Exception
	{
		infile = new File(path);
		in = new Scanner(infile);

		// Punkt statt Komma...
		in.useLocale(java.util.Locale.US);
	}

	/**
	 * Gibt das nächste Array mit Tokens zurück oder null, falls die
	 * Datei zuende ist. Beachtet Kommentare.
	 * 
	 * Fängt die Zeile mit #, ; oder // an, gilt sie als auskommentiert.
	 * Ein /* oder """ am Anfang einer Zeile leitet einen Kommentarblock
	 * ein und * / (natürlich ohne Leerzeichen) oder """ am Ende einer
	 * Zeile beendet ihn.
	 */
	public String[] getNextTokens() throws Exception
	{
		boolean inComment = false;

		while (in.hasNextLine())
		{
			String line = in.nextLine().trim();

			// Wenn wir nicht in einem Kommentarbereich sind ...
			if (!inComment)
			{
				// ... und die Zeile nicht mit einzeln auskommentiert
				// ist und nicht mit einem String anfängt, der einen
				// Kommentar einleitet, dann splitte sie und gib die
				// Tokens zurück.
				if (   !line.startsWith("#")  && !line.startsWith(";")
					&& !line.startsWith("//") && !line.equals("")
					&& !line.startsWith("/*") && !line.equals("*/")
					&& !line.startsWith("\"\"\""))
				{
					return line.split(" ");
				}

				// Wird hier ein langer Kommentar eingeleitet?
				if (line.startsWith("/*") || line.startsWith("\"\"\""))
					inComment = true;
			}
			else
			{
				// Ist der Kommentar jetzt zuende?
				if (line.endsWith("*/") || line.endsWith("\"\"\""))
					inComment = false;
			}
		}

		return null;
	}

	/**
	 * Bereitet einen Pfad (z.B. Pfad zu OBJ-File) vor:
	 * - Es wird erwartet, dass in einer Szenen-Datei die Pfade relativ
	 *   zur Position dieser Szenendatei gesetzt werden.
	 * - Es wird der absolute Pfad zur Szenendatei vorangestellt.
	 * - Die Pfade sollten mit Unix-Separatoren geschrieben werden,
	 *   also "/". Für Windows werden die nach "\" umgewandelt.
	 */
	public File getRelativePath(String path)
	{
		path = path.replace("/", File.separator);
		return new File(infile.getParentFile(), path);
	}
}
