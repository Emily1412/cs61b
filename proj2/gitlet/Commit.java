package gitlet;



import java.io.File;


import java.io.Serializable;
import java.time.Instant;

import static gitlet.Utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;


/** Represents a gitlet commit object.
 *
 *  does at a high level.
 *
 *  @author Emily
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    static final File COMMIT_FOLDER = join(".gitlet", "commits");
    static final File ADDITIONS_FOLDER = join(".gitlet/staging_area", "additions");
    static final File REMOVAL_FOLDER = join(".gitlet/staging_area", "removals");
    public static final File PROJECT = new File(System.getProperty("user.dir"));
    private String message;
    private Instant commitTime;
    private TreeMap<String, String> blobsmap;

    private String[] parentsSHA1;

    public Commit() {
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
        } else {
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
    public  String[] allBlobString() {
        if (blobsmap == null) {
            return null;
        }
        String[] blobString = new String[blobsmap.size()];
        int i = 0;
        for (String item : blobsmap.keySet()) {
            blobString[i++] = blobsmap.get(item);
        }
        return blobString;
    }
    public void addBlob(String fileName, String blobname) {
        blobsmap.put(fileName, blobname);
    }

    public String saveCommit() {
        //计算哈希值 作为commit的文件名！
        byte[] thisByte = serialize(this);
        String filename = sha1(thisByte);
        String first2 = filename.substring(0, 2);
        File fileFolder = new File(COMMIT_FOLDER, first2);
        fileFolder.mkdirs();
        File f = join(fileFolder, filename);
        writeObject(f, this);
        return filename;
    }

    public void removeBlob(String fileName) {
        if (blobsmap != null) {
            blobsmap.remove(fileName);
        }
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

    public static List<String> getAllCommitNames() {
        List<String> allCommitNames = new ArrayList<>();
        File[] commitFolders = COMMIT_FOLDER.listFiles();
        if (commitFolders != null) {
            //枚举所有的内层folder
            for (File folder : commitFolders) {
                if (folder.isDirectory()) {
                    // 列出子文件夹中的普通文件
                    List<String> thisFolderNames = plainFilenamesIn(folder);
                    if (thisFolderNames != null) {
                        for (String fileName : thisFolderNames) {
                            // 将文件名添加到列表
                            allCommitNames.add(fileName);
                        }
                    }
                }
            }
        }
        return allCommitNames;
    }

    public static boolean ifExistsCommit(String filename) {
        List<String> allNames = getAllCommitNames();
        if (allNames == null) {
            return false;
        }
        return allNames.contains(filename);
    }

    public static List<String> getAllWorkNames() {
        List<String> allNames = new ArrayList<>(Utils.plainFilenamesIn(PROJECT));
        allNames.remove("Makefile");
        allNames.remove("pom.xml");
        return allNames;
    }

    //未被跟踪的文件指的是 既不在stageing area也不再头commitblob里的文件
    public static boolean ifHasUntrackedFile(String CommitName) {
        //得到当前工作目录中的所有文件
        List<String> AllNames = getAllWorkNames();

        TreeMap<String, String> blobsmap = getCommitBlobMap(CommitName);

        //得到暂存区
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        addition adt = readObject(adtFile, addition.class);
        TreeMap<String,String> addTreeMap = adt.getTreeMap();
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        Removal rmval = readObject(rmvalFile, Removal.class);
        TreeMap<String,String> rmvalTreeMap = rmval.getRemovalsFile();
        //和当前这个commit里的blobmap进行比较
        if (AllNames != null){
            for (String fileName : AllNames) {
                if (!blobsmap.containsKey(fileName)) {
                    if (!rmvalTreeMap.containsKey(fileName) && !addTreeMap.containsKey(fileName)) {
                        return true; //说明当前commit和stagingarea里面没有这个工作目录里的文件
                    }
                }
            }
        }
        return false; //当前工作目录为空或者所有文件都已经被追踪了
    }

    public static TreeMap<String, String> getCommitBlobMap(String CommitName) {
        // 得到当前commit
        File f = join(COMMIT_FOLDER, CommitName.substring(0, 2),CommitName);
        Commit thisCommit = readObject(f, Commit.class);
        TreeMap<String, String> blobsmap = thisCommit.getBlobsMap();
        return blobsmap;
    }

    public static void checkOutCommit(String commitName) {
        TreeMap<String, String> thisCommitBlobMap = getCommitBlobMap(commitName);
        if (thisCommitBlobMap == null){
            return;
        }
        for (String fileName : thisCommitBlobMap.keySet()) {
            String thisBlobName = thisCommitBlobMap.get(fileName);
            //逐个把blob恢复到工作目录中
            Blob.reviveFile(thisBlobName, fileName);
        }
    }
    public static TreeSet<String> UntrackedFileNames(String thisCommit) {
        //得到工作目录文件
        List<String> AllNames = getAllWorkNames();
        //得到这个commit的blobMap
        TreeMap<String, String> blobsmap = getCommitBlobMap(thisCommit);
        //初始化结果集
        TreeSet<String> untrackedFileNames = new TreeSet<>();

        //得到暂存区
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        addition adt = readObject(adtFile, addition.class);
        TreeMap<String,String> addTreeMap = adt.getTreeMap();
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        Removal rmval = readObject(rmvalFile, Removal.class);
        TreeMap<String,String> rmvalTreeMap = rmval.getRemovalsFile();

        //如果有工作目录中的文件不在这个commit里，就加入到结果集中
        for (String fileName : AllNames) {
            if (blobsmap == null || !blobsmap.containsKey(fileName)) {
                if (!rmvalTreeMap.containsKey(fileName) && !addTreeMap.containsKey(fileName)) {
                    untrackedFileNames.add(fileName);
                }
            }
        }
        return untrackedFileNames;
    }

}

