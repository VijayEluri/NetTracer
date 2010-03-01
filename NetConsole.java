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
