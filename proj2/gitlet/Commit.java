package gitlet;

// TODO: any imports you need here

import java.io.File;

import java.io.Serializable;
import java.time.Instant;

import static gitlet.Utils.*;
import java.util.HashSet;
import java.util.SplittableRandom;
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
    private static HashSet<String> blobsSet; //顺序似乎不重要？好像是字典序

    private String[] parentsSHA1;

    public Commit(){
        message = "";
        commitTime = Instant.now();
        blobsSet = new HashSet<>();
        parentsSHA1 = new String[2];
    }

    public Commit(String message, Instant commitTime, HashSet<String> blobsSet, String[] parents) {
        this.message = message;
        this.commitTime = commitTime;
        if (blobsSet == null) {
            blobsSet = new HashSet<>();
        }
        else {
            this.blobsSet  = blobsSet;
        }
        this.parentsSHA1 = parents;
    }

    public Commit(String message, Instant commitTime, String[] parents) {
        this.message = message;
        this.commitTime = commitTime;
        this.parentsSHA1 = parents;
        blobsSet = new HashSet<>();
    }
    public String getMessage() {
        return message;
    }
    public Instant getCommitTime() {
        return commitTime;
    }
    public HashSet<String> getBlobsSet() {
        return blobsSet;
    }
    public String[] getParentsSHA1() {
        return parentsSHA1;
    }

    public  static String[] allBlobString(){
        String[] BlobString = new String[blobsSet.size()];
        int i = 0;
        for (String item : blobsSet){
            BlobString[i++] = item;
        }
        return BlobString;
    }
    public void addBlob(String blobName){
        blobsSet.add(blobName);
    }

    public String saveCommit() {
        //计算哈希值 作为commit的文件名！
        byte[] this_byte = serialize(this);
        String filename = sha1(this_byte);
        File f = join(COMMIT_FOLDER, filename);
        writeObject(f, this);
        return filename;
    }

    public void removeBlob(String blobName){
        blobsSet.remove(blobName);
    }

    public boolean ifExistsBlob(String blobName){
        //一定要注意空指针访问问题
        if (blobsSet == null){
            return false;
        }
        return blobsSet.contains(blobName);
    }


    /* TODO: fill in the rest of this class. */
}

