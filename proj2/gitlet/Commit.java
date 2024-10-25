package gitlet;

// TODO: any imports you need here
import gitlet.Blob;

import java.io.File;
import gitlet.Utils;

import java.io.Serializable;
import java.time.Instant;

import static gitlet.Utils.*;
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
    private Blob[] blobsList; //顺序似乎不重要？好像是字典序

    private Blob[] parents;
    public Commit(String message, Instant commitTime, Blob[] blobsList, Blob[] parents) {
        this.message = message;
        this.commitTime = commitTime;
        this.blobsList  = blobsList;
        this.parents = parents;
    }
    public String getMessage() {
        return message;
    }
    public Instant getCommitTime() {
        return commitTime;
    }
    public Blob[] getBlobsList() {
        return blobsList;
    }

    public void addBlob(Blob b){
        blobsList[blobsList.length]=b;
    }

    public String saveCommit() {
        //计算哈希值 作为commit的文件名！
        byte[] this_byte = serialize(this);
        String filename = sha1(this_byte);
        File f = join(COMMIT_FOLDER, filename);
        writeObject(f, this);
        return filename;
    }


    /* TODO: fill in the rest of this class. */
}

