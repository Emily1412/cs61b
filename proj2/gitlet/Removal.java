package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;

import static gitlet.Utils.*;

/**
 * ClassName: removal
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/23 21:41
 */
public class Removal implements Serializable {
    static final File REMOVAL_FOLDER = join(".gitlet/staging_area", "removals");

    TreeMap<String, String> removalsFile;
    public Removal() {
        removalsFile = new TreeMap<>();
    }
    public boolean ifExists(String fileName) {
        if (removalsFile == null) {
            return false;
        } else {
            return removalsFile.containsKey(fileName);
        }
    }

    public void remove(String fileName) {
        if (removalsFile != null){
            removalsFile.remove(fileName);
        }
        saveRemovalArea();
    }
    public void addFile(File f, String name) {
        //从f这里取哈希名，从name这里取文件名  好反直觉。。
        if (f != null) {
            removalsFile.put(name, f.getName());
        }
        saveRemovalArea();
    }

    public void addFile(String SHA1, String name) {
        //从f这里取哈希名，从name这里取文件名  好反直觉。。
        if (SHA1 != null && removalsFile != null) {
            removalsFile.put(name, SHA1);
        }
        saveRemovalArea();

    }

    public void clearRemovalArea() {
        removalsFile.clear();
        saveRemovalArea();
    }

    public static boolean isRmvalEmpty() {
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        Removal rmval = readObject(rmvalFile, Removal.class);
        TreeMap<String,String> treeMap = rmval.getRemovalsFile();
        if (treeMap.size() == 0) {
            return true;
        }
        return false;
    }
    public TreeMap<String, String> getRemovalsFile() {
        return removalsFile;
    }

    public String[] allRemovalFilesSHA1() {
        if (removalsFile != null){
            String[] fileNames = new String[removalsFile.size()];
            int i = 0;
            for (String fileName : removalsFile.keySet()) {
                fileNames[i++] = removalsFile.get(fileName);
            }
            return fileNames;
        }
        return null;
    }

    public String[] allOrderedRemovalFiles() {
        String[] blobFileNames = new String[removalsFile.size()];
        int i = 0;
        if (removalsFile != null) {
            for (String fileName : removalsFile.keySet()) {
                blobFileNames[i++] = fileName;
            }
        }
        return blobFileNames; //treemap已经按照字典序排好了！
    }
    public void  saveRemovalArea() {
        File f = join(REMOVAL_FOLDER, "removalTreeMap");
        writeObject(f, this);
    }
}
