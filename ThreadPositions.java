import java.io.Serializable;

/**
 * Threadsichere Klasse, die Informationen über die "aktuellen" Positionen
 * der einzelnen Renderthreads verwaltet. Da die Renderthreads aber
 * nicht auf eventuelle Zuschauer warten, kann es sein, dass sie bereits
 * schon ein Stück weiter sind. Um nicht noch mehr Zeit an den Output
 * zu verschwenden, wird das auch so bleiben.
 */
public class ThreadPositions implements Serializable
{
	private static final long serialVersionUID = 20100301001L;

	private int[] pos = null;
	
	public ThreadPositions(int num)
	{
		pos = new int[num];
		reset();
	}
	
	synchronized public void reset()
	{
		for (int i = 0; i < pos.length; i++)
			pos[i] = -1;
	}
	
	synchronized public void update(int which, int val)
	{
		if (which >= 0 && which < pos.length)
			pos[which] = val;
	}
	
	synchronized public int[] get()
	{
		int[] out = new int[pos.length];
		for (int i = 0; i < pos.length; i++)
			out[i] = pos[i];
		return out;
	}
}
