package android.util; 

public class Log {
    public static int e(String tag, String msg, Throwable error) {
        System.out.println("ERROR: " + tag + ": " + error);
        return 0;
    }
}
