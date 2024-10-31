package gitlet;



import java.io.File;


import java.io.Serializable;
import java.time.Instant;

import static gitlet.Utils.*;

import java.util.*;


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
    private static final long serialVersionUID = 1L;
    private Instant commitTime;
    private TreeMap<String, String> blobsmap;

    private String[] parentsSHA1;

    public Commit() {
        message = "";
        commitTime = Instant.now();
        blobsmap = new TreeMap<>();
        parentsSHA1 = new String[2];
    }

    public Commit(String message, Instant commitTime,
                  TreeMap<String, String> blobsmap, String[] parents) {
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

    public static Commit getwantedCommit(String prefix) {
        String first2 = prefix.substring(0, 2);
        File dir = join(COMMIT_FOLDER, first2);

        if (dir.isDirectory()) {
            File[] matchingFiles = dir.listFiles((dir1, name) -> {
                return name.startsWith(prefix); // 检查文件名是否以prefix开头
            });

            // 如果找到了匹配的文件，读取第一个并返回
            if (matchingFiles != null && matchingFiles.length > 0) {
                File matchingFile = matchingFiles[0]; // 假设只返回第一个
                return readObject(matchingFile, Commit.class); // 读取并返回该Commit对象
            }
        }
        return null; // 如果没有找到匹配的commit，返回null
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
    public static boolean ifHasUntrackedFile(String commitName) {
        //得到当前工作目录中的所有文件
        List<String> allNames = getAllWorkNames();

        TreeMap<String, String> blobsmap = getCommitBlobMap(commitName);

        //得到暂存区
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        Addition adt = readObject(adtFile, Addition.class);
        TreeMap<String, String> addTreeMap = adt.getTreeMap();
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        Removal rmval = readObject(rmvalFile, Removal.class);
        TreeMap<String, String> rmvalTreeMap = rmval.getRemovalsFile();
        //和当前这个commit里的blobmap进行比较
        if (allNames != null) {
            for (String fileName : allNames) {
                if (!blobsmap.containsKey(fileName)) {
                    if (!rmvalTreeMap.containsKey(fileName) && !addTreeMap.containsKey(fileName)) {
                        return true; //说明当前commit和stagingarea里面没有这个工作目录里的文件
                    }
                }
            }
        }
        return false; //当前工作目录为空或者所有文件都已经被追踪了
    }

    public static TreeMap<String, String> getCommitBlobMap(String commitName) {
        // 得到当前commit
        File f = join(COMMIT_FOLDER, commitName.substring(0, 2), commitName);
        Commit thisCommit = readObject(f, Commit.class);
        TreeMap<String, String> blobsmap = thisCommit.getBlobsMap();
        return blobsmap;
    }

    public static void checkOutCommit(String commitName) {
        TreeMap<String, String> thisCommitBlobMap = getCommitBlobMap(commitName);
        if (thisCommitBlobMap == null) {
            return;
        }
        for (String fileName : thisCommitBlobMap.keySet()) {
            String thisBlobName = thisCommitBlobMap.get(fileName);
            //逐个把blob恢复到工作目录中
            Blob.reviveFile(thisBlobName, fileName);
        }
    }
    public static TreeSet<String> untrackedFileNames(String thisCommit) {
        //得到工作目录文件
        List<String> allNames = getAllWorkNames();
        //得到这个commit的blobMap
        TreeMap<String, String> blobsmap = getCommitBlobMap(thisCommit);
        //初始化结果集
        TreeSet<String> untrackedFileNames = new TreeSet<>();

        //得到暂存区
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        Addition adt = readObject(adtFile, Addition.class);
        TreeMap<String, String> addTreeMap = adt.getTreeMap();
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        Removal rmval = readObject(rmvalFile, Removal.class);
        TreeMap<String, String> rmvalTreeMap = rmval.getRemovalsFile();

        //如果有工作目录中的文件不在这个commit里，就加入到结果集中
        for (String fileName : allNames) {
            if (blobsmap == null || !blobsmap.containsKey(fileName)) {
                if (!rmvalTreeMap.containsKey(fileName) && !addTreeMap.containsKey(fileName)) {
                    untrackedFileNames.add(fileName);
                }
            }
        }
        return untrackedFileNames;
    }

    /**
     * 找到两个commit最近的公共祖先的SHA1
     * @param sha11 第一个Commit对象的SHA1值
     * @param sha12 第二个Commit对象的SHA1值
     * @return 最近的公共祖先的SHA1字符串
     */
    public static String findLCA(String sha11, String sha12) {
        // 先通过SHA1找到对应的commit对象
        Commit commit1 = getwantedCommit(sha11);
        Commit commit2 = getwantedCommit(sha12);

        if (commit1 == null || commit2 == null) {
            return null; // 如果其中一个commit不存在，直接返回null
        }

        // 用来存放bfs队列
        Queue<Commit> queue1 = new LinkedList<>();
        Queue<Commit> queue2 = new LinkedList<>();

        // 用来记录访问过的节点
        Set<String> visited1 = new HashSet<>();
        Set<String> visited2 = new HashSet<>();

        // 开始从两个commit分别进行bfs
        queue1.add(commit1);
        queue2.add(commit2);
        visited1.add(sha11);  // 直接使用传入的 SHA1 作为唯一标识
        visited2.add(sha12);

        while (!queue1.isEmpty() || !queue2.isEmpty()) {
            // 从commit1开始搜索
            if (!queue1.isEmpty()) {
                Commit current1 = queue1.poll();
                for (String parentSha1 : current1.getParentsSHA1()) {
                    if (visited2.contains(parentSha1)) {
                        return parentSha1;  // 找到公共祖先，直接返回SHA1
                    }
                    if (!visited1.contains(parentSha1)) {
                        visited1.add(parentSha1);
                        queue1.add(getCommitBySha1(parentSha1));
                    }
                }
            }

            // 从commit2开始搜索
            if (!queue2.isEmpty()) {
                Commit current2 = queue2.poll();
                for (String parentSha1 : current2.getParentsSHA1()) {
                    if (visited1.contains(parentSha1)) {
                        return parentSha1;  // 找到公共祖先，直接返回SHA1
                    }
                    if (!visited2.contains(parentSha1)) {
                        visited2.add(parentSha1);
                        queue2.add(getCommitBySha1(parentSha1));
                    }
                }
            }
        }
        return null; // 没有找到公共祖先
    }




    /**
     * 根据SHA1获取对应的Commit对象
     * @param sha1 Commit的SHA1值
     * @return 对应的Commit对象
     */
    public static Commit getCommitBySha1(String sha1) {
        // 从 COMMIT_FOLDER 中找到相应 SHA1 的 commit 文件并返回
        File commitFile = join(COMMIT_FOLDER, sha1);
        if (commitFile.exists()) {
            return readObject(commitFile, Commit.class);  // 假设存在此方法来反序列化
        }
        return null;
    }

}

