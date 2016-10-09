package version_01.util;

import java.util.Vector;

public class ClassScope {
    private static java.lang.reflect.Field LIBRARIES = null;
    static {
        try {
            LIBRARIES = ClassLoader.class.getDeclaredField("loadedLibraryNames");
            LIBRARIES.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }
    public static String[] getLoadedLibraries(final ClassLoader loader) throws IllegalAccessException {
        final Vector<String> libraries = (Vector<String>) LIBRARIES.get(loader);
        return libraries.toArray(new String[] {});
    }
}