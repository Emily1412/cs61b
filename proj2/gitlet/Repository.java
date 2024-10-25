package gitlet;

import java.io.File;
import java.io.IOException;
import java.time.Instant;



import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File PROJECT = new File(CWD, "gitlet");
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    static final File BLOB_FOLDER = join(".gitlet","blobs");
    static final File FILENAME_FOLDER = join(".gitlet/staging_area","fileNames");

    //是整个仓库的head (正处在的地方）
    private static String head;

    /* TODO: fill in the rest of this class. */
    static boolean checkFolder(){
        File f = new File(CWD, ".gitlet");
        if (f.exists() && f.isDirectory()) {
            return true;
        }
        return false;
    }
    public static void init() throws IOException {
        //创建目录结构
        //commits & blobs
        if(GITLET_DIR.exists()){
            System.err.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        GITLET_DIR.mkdirs();
        File commitFolder = new File(GITLET_DIR, "commits");
        File blobFolder = new File(GITLET_DIR, "blobs");
        commitFolder.mkdirs();
        blobFolder.mkdirs();
        //staging area
        File additionsFolder = new File(GITLET_DIR, "staging_area/additions");
        additionsFolder.mkdirs();
        File removalsFolder = new File(GITLET_DIR, "staging_area/removals");
        removalsFolder.mkdirs();
        File fileNames = new File(GITLET_DIR, "staging_area/fileNames");
        fileNames.mkdirs();
        //创建初始commit
        String msg = "initial commit";
        Instant time = Instant.ofEpochSecond(0);
        Commit initialCommit = new Commit(msg, time,null, null);
        //写入初始commit到文件中
        String sha1Name = initialCommit.saveCommit();
        //创建头指针
        head = sha1Name;
        //创建分支结构
        File branchFolder = new File(GITLET_DIR, "branches");
        branchFolder.mkdirs();
        //创建一个主分支
        Branch master = new Branch("master");
        master.addCommit(sha1Name);
    }

    //将文件添加到暂存区 每次只加一个即可（这和真正的git不同）
    public static void add(String fileName) throws IOException {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }

        //源文件
        File file = new File(PROJECT, fileName);
        if(!file.exists()){
            System.err.println("File does not exist.");
        }

        //先得到这个文件的blob
        Blob blob = new Blob(file);
        //blob需要写入文件
        String blobName = blob.saveBlob();
        File f = new File(BLOB_FOLDER, blobName);
        //取消已经标记为删除文件的暂存
        if (removal.ifExists(f)){
            removal.remove(f);
            return;
        }

        //避免不必要的暂存
        if (addition.ifExists(f)){
            return;
        }


        // 自定义方法：检查目录是否为空


        //成功暂存，如果同一个文件已经被暂存了，暂存的新文件会覆盖旧文件
        if (isDirectoryEmpty(FILENAME_FOLDER)){
            addition.addFile(f,fileName);
            return;
        }
        else {
            File SFN = join(FILENAME_FOLDER, fileName + ".txt");
            if (SFN.exists()){
                //说明暂存区已经有这个文件的blob了
                //直接覆盖 SFN是之前存的文件
                String thisSHA1 = readContentsAsString(SFN);
                addition.remove(thisSHA1); //没有remove
                addition.addFile(f,fileName);
                return;
            }
        }
        addition.addFile(f,blobName);
    }
    //检查staging area的文件是否为空
    public static boolean isDirectoryEmpty(File directory) {
        File[] files = directory.listFiles();
        return files == null || files.length == 0;
    }

}
