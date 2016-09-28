package version_mati_01.util;

import java.io.File;

public class DirResourcesFilesPathUtil {

    /**
     * Represent the FILE_SYSTEM_SEPARATOR
     */
    public  static final String FILE_SYSTEM_SEPARATOR = System.getProperty("file.separator");

    /**
     * Get the path to resources file system folder
     * @return String path to file folder
     **/
    public static String getExternalStorageDirectory() {

        String path = System.getProperty("user.home").concat(FILE_SYSTEM_SEPARATOR).concat(".iop-node");
        File dir = new File(path);
        return dir.getAbsolutePath().concat(FILE_SYSTEM_SEPARATOR);
    }

    /**
     * Es el mismo que arriba más un path que se le agrega.
     * ejemplo: .concat(FILE_SYSTEM_SEPARATOR).concat("node").concat(FILE_SYSTEM_SEPARATOR).concat("resources")
     * @param pathToAdd
     * @return
     */
    public static String getExternalStoregaDirectory(String pathToAdd){
        String path = System.getProperty("user.home").concat(FILE_SYSTEM_SEPARATOR).concat(".mati-iop-node")+pathToAdd;
        File dir = new File(path);
        return dir.getAbsolutePath().concat(FILE_SYSTEM_SEPARATOR);
    }

    /**
     * Create a new files path in the default path with the name
     * pass as parameter and ended with de FILE_SYSTEM_SEPARATOR
     *
     * @param name
     * @return String
     */
    public static String createNewFilesPath(String name){
        String path = null;
        if (name!=null || (!name.equals("")))
            path = getExternalStorageDirectory().concat(name).concat(FILE_SYSTEM_SEPARATOR);
        else path = getExternalStorageDirectory().concat(FILE_SYSTEM_SEPARATOR);
        return path;
    }

}