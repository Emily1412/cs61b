package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static gitlet.Utils.join;
import static gitlet.Utils.readObject;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import gitlet.Blob;
import gitlet.Utils;
/**
 * ClassName: addition
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/23 21:41
 */
public class addition {

    static final File ADDITIONS_FOLDER = join(".gitlet/staging_area","additions");
    static final File FILENAME_FOLDER = join(".gitlet/staging_area","fileNames");
    //此处需判断文件名一致，且内容一致
    static boolean ifExists(File f) {
        File fileToCheck = new File(ADDITIONS_FOLDER, f.getName());
        if (fileToCheck.exists()) {
            return true;
        }
        return false;
    }

    static boolean sameSAH1(File f, String s1) {
        File fileToCheck = new File(ADDITIONS_FOLDER, f.getName());
        if(fileToCheck.exists()) {
            Blob b = readObject(fileToCheck, Blob.class);
            if (b.getSHA1().equals(s1)){
                return true;
            }
        }
        return false;
    }

    public static void remove(String filename) throws IOException {
        File f = new File(ADDITIONS_FOLDER, filename);
        if (ifExists(f)) {
            if (!f.delete()){
                System.out.println("Could not delete " + f.getName());
            }
        }
    }

    public static void addFile(File f, String name) throws IOException {
        File fileToAdd = new File(ADDITIONS_FOLDER, f.getName()); //只保存blob的名字就好
        if (!ifExists(fileToAdd)) {
            fileToAdd.createNewFile();
        }
       else {
           System.out.println("File already exists");
        }
       File fileToAdd2 = join(FILENAME_FOLDER, name + ".txt");
       fileToAdd2.createNewFile();
       Utils.writeContents(fileToAdd2,f.getName());
    }


}


