package gitlet;

// TODO: any imports you need here

import java.io.File;

import java.io.FilenameFilter;
import java.io.Serializable;
import java.time.Instant;

import static gitlet.Utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
// TODO: You'll likely use this in this class

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Emily
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    static final File COMMIT_FOLDER = join(".gitlet","commits");
    private String message;
    private Instant commitTime;
    private TreeMap<String, String> blobsmap;

    private String[] parentsSHA1;

    public Commit(){
        message = "";
        commitTime = Instant.now();
        blobsmap = new TreeMap<>();
        parentsSHA1 = new String[2];
    }

    public Commit(String message, Instant commitTime, TreeMap<String, String> blobsmap, String[] parents) {
        this.message = message;
        this.commitTime = commitTime;
        if (blobsmap == null) {
            blobsmap = new TreeMap<>();
        }
        else {
            this.blobsmap  = blobsmap;
        }
        this.parentsSHA1 = parents;
    }

    public Commit(String message, Instant commitTime, String[] parents) {
        this.message = message;
        this.commitTime = commitTime;
        this.parentsSHA1 = parents;
        blobsmap = new TreeMap<>();
    }
    public String getMessage() {
        return message;
    }
    public Instant getCommitTime() {
        return commitTime;
    }
    public TreeMap<String, String> getBlobsMap() {
        return blobsmap;
    }
    public String[] getParentsSHA1() {
        return parentsSHA1;
    }

    //得到所有blob的哈希值
    public  String[] allBlobString(){
        if (blobsmap == null) {
            return null;
        }
        String[] BlobString = new String[blobsmap.size()];
        int i = 0;
        for (String item : blobsmap.keySet()){
            BlobString[i++] = blobsmap.get(item);
        }
        return BlobString;
    }
    public void addBlob(String FileName,String blobname){
        blobsmap.put(FileName,blobname);
    }

    public String saveCommit() {
        //计算哈希值 作为commit的文件名！
        byte[] this_byte = serialize(this);
        String filename = sha1(this_byte);
        String first2 = filename.substring(0, 2);
        File fileFolder = new File(COMMIT_FOLDER, first2);
        fileFolder.mkdirs();
        File f = join(fileFolder, filename);
        writeObject(f, this);
        return filename;
    }

    public void removeBlob(String FileName){
        if (blobsmap != null){
            blobsmap.remove(FileName);
        }
    }

    public boolean ifExistsBlob(String FileName){
        //一定要注意空指针访问问题
        if (blobsmap == null){
            return false;
        }
        return blobsmap.containsKey(FileName);
    }


    public static List<String> getwantedCommit(String prefix) {
        String first2 = prefix.substring(0, 2);
        File dir = join(COMMIT_FOLDER, first2);
        List<String> matchingCommit = new ArrayList<>();

        if (dir.isDirectory()) {
            File[] matchingFiles = dir.listFiles((dir1, name) -> {
                return name.startsWith(prefix);  // 检查文件名是否以prefix开头
            });
            if (matchingFiles != null) {
                for (File file : matchingFiles) {
                    // 添加文件全称
                    matchingCommit.add(file.getName());
                }

            }
        }
        return matchingCommit;
    }
    /* TODO: fill in the rest of this class. */
}

