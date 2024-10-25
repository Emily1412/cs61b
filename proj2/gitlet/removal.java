package gitlet;

import java.io.File;
import java.io.IOException;

import static gitlet.Utils.join;

/**
 * ClassName: removal
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/23 21:41
 */
public class removal {
    static final File REMOVAL_FOLDER = join(".gitlet/staging_area","removals");
    public static boolean ifExists(File f) {
        File fileToCheck = new File(REMOVAL_FOLDER, f.getName());
        if (fileToCheck.exists()) {
            return true;
        }
        return false;
    }

    public static void remove(File f) throws IOException {
        if (ifExists(f)) {
            File fileToDelete = new File(REMOVAL_FOLDER, f.getName());
            if (!fileToDelete.delete()){
                System.out.println("Could not delete " + f.getName());
            }
        }
    }
    public static void addFile(File f) throws IOException {
        File fileToAdd = new File(REMOVAL_FOLDER, f.getName()); //只保存blob的名字就好
        if (!ifExists(fileToAdd)) {
            fileToAdd.createNewFile();
        }
        else {
            System.out.println("File already exists");
        }
    }

    public static void clearRemovalArea() {
        File[] f = REMOVAL_FOLDER.listFiles();
        if (f != null) {
            for (File file : f) {
                if (file.isFile()){
                    file.delete();
                }
            }
        }
    }

    public static String[] allRemovalFiles(){
        String[] fileNames = new String[REMOVAL_FOLDER.listFiles().length];
        File[] files = REMOVAL_FOLDER.listFiles();
        int i = 0;
        for (File file : files) {
            fileNames[i++] = file.getName();
        }
        return fileNames;
    }

}
