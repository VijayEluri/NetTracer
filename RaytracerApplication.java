/**
 * Hauptanwendung.
 */
public class RaytracerApplication
{
	public static void main(String[] args)
	{
		Scene theScene = new Scene();
		if (args.length != 0)
		{
			if (theScene.loadEnvironment(args[0]))
			{
				theScene.render();
			}
		}
		else
		{
			System.out.println("Gib die zu rendernde Szene als Parameter an.");
		}
	}
}
