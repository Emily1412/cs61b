package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import gitlet.Blob;
import gitlet.Utils;

import static gitlet.Utils.*;

/**
 * ClassName: addition
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/23 21:41
 */
public class addition implements Serializable {
    TreeMap<String, String> additionFiles = new TreeMap<>();

    //创建示例对象 需要保存的！
    public addition(){
        additionFiles = new TreeMap<>();
    }

    static final File ADDITIONS_FOLDER = join(".gitlet/staging_area","additions");

    //此处需判断文件名一致，且内容一致
     boolean ifExists(String fileName) {
         if (additionFiles == null){
             return false;
         }
         else return additionFiles.containsKey(fileName);
    }

    boolean sameSHA1(String fileName, String compareSHA1) {
         if (additionFiles != null && additionFiles.containsKey(fileName)){
             return compareSHA1.equals(additionFiles.get(fileName));
         }
         return false;
    }
    //判断文件的内容一不一样


    public void remove(String filename) {
        if (additionFiles != null){
            additionFiles.remove(filename);
        }
        saveAdditionArea();
    }

    //这里一定要记得第二个传整个文件名！！
    public void addFile(File f, String name) {
        //从f这里取哈希名，从name这里取文件名  好反直觉。。
        if (f != null){
            //文件名是主键，哈希值是值
            additionFiles.put(name, f.getName());
        }
        saveAdditionArea();
    }


    public String[] allAdditionFilesSHA1(){
        if (additionFiles != null){
            String[] blobFileNames = new String[additionFiles.size()];
            int i = 0;
            for (String fileName : additionFiles.keySet()){
                blobFileNames[i++] = additionFiles.get(fileName);
            }
            return blobFileNames;
        }
        return null;
    }

    public String[] allOrderedAdditionFiles(){
        String[] blobFileNames = new String[additionFiles.size()];
        int i = 0;
        if (additionFiles != null){
            for (String fileName : additionFiles.keySet()){
                blobFileNames[i++] = fileName;
            }
        }
        return blobFileNames; //treemap已经按照字典序排好了！
    }

    public void clearAdditionArea() {
        additionFiles.clear();
        saveAdditionArea();
    }

    public void saveAdditionArea() {
        File f = join(ADDITIONS_FOLDER,"additionTreeMap");
        writeObject(f, this);
    }

}

