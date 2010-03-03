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
