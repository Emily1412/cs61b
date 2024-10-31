package gitlet;



import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static gitlet.Commit.*;
import static gitlet.Utils.*;

import static gitlet.Removal.isRmvalEmpty;




/** Represents a gitlet repository.
 *
 *
 *  @author Emily
 */
public class Repository {
    /**
     *
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File PROJECT = CWD;
    public static final File REMOVAL_FOLDER = join(".gitlet/staging_area", "removals");
    public static final File ADDITIONS_FOLDER = join(".gitlet/staging_area", "additions");
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    static final File COMMIT_FOLDER = join(".gitlet", "commits");
    static final File BLOB_FOLDER = join(".gitlet", "blobs");

    static final File HEAD = join(".gitlet", "head.txt");
    static final File BRANCH_FOLDER = join(".gitlet", "branches");
    static final File CURRENT_BRANCH = join(".gitlet", "currentBranch.txt");

    //是整个仓库的head (正处在的地方）
    private static String head;

    static boolean checkGiletFolder() {
        if (!GITLET_DIR.exists()) {
            System.err.println("Not in an initialized Gitlet directory.");
            return false;
        }
        return true;
    }

    static void saveHead(String commitSHA1) {
        File f = HEAD;
        writeContents(f, commitSHA1);
    }

    static void saveCurrBranch(String branchName) {
        File f = CURRENT_BRANCH;
        writeContents(f, branchName);
    }
    static String getHead() {
        File f = HEAD;
        String headCommitSHA1 = readContentsAsString(f);
        return  headCommitSHA1;
    }
    static String getCurrentBranch() {
        File f = CURRENT_BRANCH;
        return readContentsAsString(f);
    }

    public static File findFileWithPrefix(File folder, String prefix) {
        // 列出文件夹中的所有文件
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                // 检查文件名是否以prefix开头
                if (file.getName().startsWith(prefix)) {
                    return file; // 找到匹配的文件
                }
            }
        }
        return null; // 没有找到匹配的文件
    }

    public static String createInitCommit() {
        String msg = "initial commit";
        Instant time = Instant.ofEpochSecond(0);
        Commit initialCommit = new Commit(msg, time, null, null);
        //写入初始commit到文件中
        String sha1Name = initialCommit.saveCommit();
        return sha1Name;
    }
    public static void init() {
        //创建目录结构
        //commits & blobs
        if (GITLET_DIR.exists()) {
            System.err.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
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
        Addition additionArea = new Addition();
        additionArea.saveAdditionArea();
        File removalsFolder = new File(GITLET_DIR, "staging_area/removals");
        removalsFolder.mkdirs();
        Removal removalArea = new Removal();
        removalArea.saveRemovalArea();


        //创建初始的commit
        String sha1Name = createInitCommit();
        //创建头指针
        head = sha1Name;
        saveHead(sha1Name);

        //创建分支结构
        File branchFolder = new File(GITLET_DIR, "branches");
        branchFolder.mkdirs();
        //创建一个主分支
        Branch master = new Branch("master");
        master.addCommit(sha1Name);
        saveCurrBranch("master");
    }

    //将文件添加到暂存区 每次只加一个即可（这和真正的git不同）
    public static void add(String fileName) {
        if (!checkGiletFolder()) {
            return;
        }

        //源文件
        File file = new File(PROJECT, fileName);
        if (!file.exists()) {
            System.err.println("File does not exist.");
            return;
        }

        //先得到这个文件的blob
        Blob blob = new Blob(file);
        //blob需要写入文件
        String blobName = blob.saveBlob();
        File f = new File(BLOB_FOLDER, blobName); //得到这个blob文件
        //得到stagingarea
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        Removal rmval = readObject(rmvalFile, Removal.class);
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        Addition adt = readObject(adtFile, Addition.class);

        //取消已经标记为删除文件的暂存
        if (rmval.ifExists(fileName)) {
            rmval.remove(fileName);
            return;
        }

        //避免不必要的暂存
        if (adt.ifExists(fileName) && adt.sameSHA1(fileName, blobName)) {
            return;
        }

        //如果现在头commit文件已经在追踪这个文件且内容一样直接返回
        TreeMap<String, String> thisComBlobMap = getCommitBlobMap(getHead());

        if (thisComBlobMap != null && thisComBlobMap.containsKey(fileName)) {
            if (blobName.equals(thisComBlobMap.get(fileName))) {
                return;
            }
        }

        //成功暂存，如果同一个文件已经被暂存了，暂存的新文件会覆盖旧文件
        if (adt.ifExists(fileName) && !adt.sameSHA1(fileName, blobName)) {
            adt.addFile(f, fileName);
        } else {
            adt.addFile(f, fileName);
        }

    }
    //检查staging area的文件是否为空
    public static boolean isDirectoryEmpty(File directory) {
        File[] files = directory.listFiles();
        return files == null || files.length == 0;
    }

    //处理commit,msg是信息
    public static void commit(String msg) {
        if (!checkGiletFolder()) {
            return;
        }

        if (isRmvalEmpty() && Addition.isAdditionEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        if (msg.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        //创建一个新的commit with msg & time
        String[] parent;
        head = getHead();
        parent = new String[]{head}; //这里如果有多个parent怎么办 分支那里重新处理吗
        Commit newCommit = new Commit(msg, Instant.now(), parent);

        // 把addition的blob加入到commit blobset里面
        //得到additionArea
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        Addition adt = readObject(adtFile, Addition.class);
        TreeMap<String, String> treeMap = adt.getTreeMap();
        //直接copy
        for (String fileName : treeMap.keySet()) {
            newCommit.addBlob(fileName, treeMap.get(fileName));
        }
        adt.clearAdditionArea();

        //继承父commit中所有的blob 但是blob所追踪文件的名字不能和这个blob相同……
        String first2 = head.substring(0, 2);
        File commitAbbFolder = join(COMMIT_FOLDER, first2);
        File commitFolder = join(commitAbbFolder, head);
        Commit parentCommit = readObject(commitFolder, Commit.class);
        TreeMap<String, String> parentBlobs = parentCommit.getBlobsMap();
        if (parentBlobs != null) {
            //遍历父节点的文件 如果新的里面没有，就加入
            for (String fileName : parentBlobs.keySet()) {
                if (!newCommit.getBlobsMap().containsKey(fileName)) {
                    newCommit.addBlob(fileName, parentBlobs.get(fileName));
                }
            }
        }

        // 把removal里面存在的blob删除，并清空removal
        // 得到rmarea
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        Removal rmval = readObject(rmvalFile, Removal.class);
        TreeMap<String, String> rmvalTreeMap = rmval.getRemovalsFile();

        for (String fileName : rmvalTreeMap.keySet()) {
            newCommit.removeBlob(fileName);
        }
        rmval.clearRemovalArea();
        //commit持久化，移动头指针
        String thisSHA1 = newCommit.saveCommit();
        saveHead(thisSHA1);
        //更新分支 添加新节点
        String curBrachName = getCurrentBranch();
        File branchFile = join(BRANCH_FOLDER, curBrachName);
        Branch curBranch = readObject(branchFile, Branch.class);
        curBranch.addCommit(thisSHA1);
    }

    //fileName是需要被删除的文件
    public static void rm(String fileName) {
        //如果文件被暂存用于新增，`rm` 会将其从暂存区移除。
        //得到staging area 的addition区域
        if (!checkGiletFolder()) {
            return;
        }

        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        Addition adt = readObject(adtFile, Addition.class);
        //SFN是当前工作目录下的这个文件本身（如果存在的话）
        File sfn = join(PROJECT, fileName);
        if (adt.ifExists(fileName)) {
            adt.remove(fileName);
            return;
        }

        //待测试！！！！！！
        //如果文件被当前提交跟踪 (已经 commit)，`rm` 会在暂存区标记其为删除，并从工作目录中删除。
        TreeMap<String, String> headComBlobMap = getCommitBlobMap(getHead());
        if (headComBlobMap != null && headComBlobMap.containsKey(fileName)) {
            File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
            Removal rmval = readObject(rmvalFile, Removal.class);
            rmval.addFile(headComBlobMap.get(fileName), fileName);
            sfn.delete();
        } else {
            System.err.println("No reason to remove the file");
        }


        //如果文件既没有被暂存，也没有被跟踪，命令会返回错误消息：`No reason to remove the file`。
    }

    public static void printlog(Commit thisCom, String thisSHA1) {
        System.out.print("===\n");  // 使用 \n 明确指定换行
        System.out.print("commit " + thisSHA1 + "\n");
        Instant instant = thisCom.getCommitTime();
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("America/Los_Angeles"));
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        String formattedDate = zonedDateTime.format(formatter);
        System.out.print("Date: " + formattedDate + "\n");

        System.out.print(thisCom.getMessage() + "\n");
        System.out.print("\n");  // 确保日志输出的格式正确
    }
    public static void log() {
        if (!checkGiletFolder()) {
            return;
        }

        //从当前这个commit开始从后往前输出信息直到initialCommit
        String headSHA1 = getHead();
        String thisSHA1 = headSHA1;
        String first2 = thisSHA1.substring(0, 2);
        File file = join(COMMIT_FOLDER, first2);
        File f = join(file, thisSHA1);
        Commit thisCom = readObject(f, Commit.class);
        while (!thisCom.getMessage().equals("initial commit")) {
            printlog(thisCom, thisSHA1);
            String[] parents = thisCom.getParentsSHA1();
            String pfirst2 = parents[0].substring(0, 2);
            File pfile = join(COMMIT_FOLDER, pfirst2);
            File nextf = join(pfile, parents[0]);
            thisSHA1 = parents[0];
            thisCom = readObject(nextf, Commit.class);
        }
        printlog(thisCom, thisSHA1);
    }

    public static void globalLog() {
        if (!checkGiletFolder()) {
            return;
        }

        List<String> allCommitNames = getAllCommitNames();
        for (String commitName : allCommitNames) {
            File f = join(COMMIT_FOLDER, commitName.substring(0, 2), commitName);
            Commit thisCom = readObject(f, Commit.class);
            printlog(thisCom, commitName);
        }
    }


    public static void find(String msg) {
        if (!checkGiletFolder()) {
            return;
        }

        List<String> allCommitNames = getAllCommitNames();
        boolean flag = false;
        for (String commitName : allCommitNames) {
            File f = join(COMMIT_FOLDER, commitName.substring(0, 2), commitName);
            Commit thisCom = readObject(f, Commit.class);
            if (thisCom.getMessage().equals(msg)) {
                System.out.println(commitName);
                flag = true;
            }
        }
        if (!flag) {
            System.out.println("Found no commit with that message.");
        }
    }

    //展示所有文件的状态 每个板块按照字典序排序
    public static void status() {
        if (!checkGiletFolder()) {
            return;
        }

        System.out.println("=== Branches ===");
        String thisBranch = getCurrentBranch();
        List<String> branchList = Branch.listBranch();
        for (String branchName : branchList) {
            if (branchName.equals(thisBranch)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        Addition adt = readObject(adtFile, Addition.class);
        String[] stageFiles = adt.allOrderedAdditionFiles();
        if (stageFiles != null) {
            for (String fileName : stageFiles) {
                System.out.println(fileName);
            }
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        Removal rmval = readObject(rmvalFile, Removal.class);
        String[] rmvalFiles = rmval.allOrderedRemovalFiles();
        if (rmvalFiles != null) {
            for (String fileName : rmvalFiles) {
                System.out.println(fileName);
            }
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");
        String headSHA1 = getHead();
        TreeSet<String> utFs = untrackedFileNames(headSHA1);
        if (utFs.size() != 0) {
            for (String fileName : utFs) {
                System.out.println(fileName);
            }
        }

    }

    public static void wantedFileName(TreeMap<String, String> blobNames, String wantedFileName) {
        //blobnameset 反序列化 找这个文件
        if (blobNames != null && blobNames.containsKey(wantedFileName)) {
            File blobFile = join(BLOB_FOLDER, blobNames.get(wantedFileName));
            Blob thisBlob = readObject(blobFile, Blob.class);
            File wantedFile = join(PROJECT, wantedFileName);
            byte[] content = thisBlob.getContent();
            writeContents(wantedFile, content);
            try {
                wantedFile.createNewFile();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("File does not exist in that commit.");
        }

    }

    //切换到当前commit的这个文件
    public static void checkoutFile(String fileName) {
        if (!checkGiletFolder()) {
            return;
        }

        //从当前head里面取出来这个commit
        String headComName = getHead();
        File f = join(COMMIT_FOLDER, headComName.substring(0, 2), headComName);
        Commit headCom = readObject(f, Commit.class);

        //从commit取出这个blobnameset
        TreeMap<String, String> blobNames = headCom.getBlobsMap();

        wantedFileName(blobNames, fileName);

    }

    public static void checkoutFileFromCommit(String commitID, String fileName) {
        if (!checkGiletFolder()) {
            return;
        }

        // 反序列化这个commit
        // 前缀匹配就找到了！！？ 这里有问题唉
        File f = findFileWithPrefix(join(COMMIT_FOLDER, commitID.substring(0, 2)), commitID);
        if (f == null || !f.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit thisCom = readObject(f, Commit.class);

        //从commit取出这个blobnamemap
        TreeMap<String, String> blobNames = thisCom.getBlobsMap();
        wantedFileName(blobNames, fileName);
    }

    public static void checkoutBranch(String branchName) {
        if (!checkGiletFolder()) {
            return;
        }

        File f = join(BRANCH_FOLDER, branchName);
        if (!f.exists()) {
            System.out.println("No such branch exists.");
            return;
        }

        //当前分支的名字
        String curbranchName = getCurrentBranch();
        if (curbranchName.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        //得到目标分支文件表 commit头
        String branCommitName = Branch.getBranchHeadCommit(branchName);
        //目标文件文件列表
        TreeMap<String, String> branchFileNames = getCommitBlobMap(branCommitName);

        //当前前分支中有未跟踪的文件，并且这些文件会被目标分支覆盖 报错
        TreeSet<String> untrackedFileNames = untrackedFileNames(getHead());
        if (branchFileNames != null && untrackedFileNames.size() != 0) {
            for (String fileName : untrackedFileNames) {
                if (branchFileNames.containsKey(fileName)) {
                    System.out.println("There is an untracked "
                            + "file in the way; delete it, "
                            + "or add and commit it first.");

                }
            }
        }
        //将这个最新的commit重现在工作目录中
        checkOutCommit(branCommitName);
        //全部覆盖工作目录中的文件（如果存在相同文件）
        // 并删除当前分支中有跟踪但目标分支中没有的文件。
        TreeMap<String, String> nowCommitFileNames = getCommitBlobMap(getHead());
        if (nowCommitFileNames != null) {
            for (String fileName : nowCommitFileNames.keySet()) {
                if (branchFileNames == null || !branchFileNames.containsKey(fileName)) {
                    File toBeDeleted = join(PROJECT, fileName);
                    toBeDeleted.delete();
                }
            }
        }

        //更改当前branch名字
        saveCurrBranch(branchName);
        saveHead(branCommitName);
    }

    //创建新的branch
    public static void branch(String newBranchName) {
        if (!checkGiletFolder()) {
            return;
        }

        File f = join(BRANCH_FOLDER, newBranchName);
        if (f.exists()) {
            System.out.println("A branch with that name already exists.");
        } else {
            Branch newBranch = new Branch(newBranchName);
            String headComName = getHead();
            //要把这条分支上的所有commit都加入到这个新的branch上面
            File f1 = join(BRANCH_FOLDER, getCurrentBranch());
            Branch curBranch = readObject(f1, Branch.class);
            TreeMap<Integer, String> curBranchMap = curBranch.getCommits();
            newBranch.addCommitMap(curBranchMap);
            newBranch.saveBranch();
        }
    }

    public static void rmBranch(String rmBranchName) {
        if (!checkGiletFolder()) {
            return;
        }

        File f = join(BRANCH_FOLDER, rmBranchName);
        if (!f.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        String cB = getCurrentBranch();
        if (rmBranchName.equals(cB)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        f.delete();
    }

    //调试helper_method 打印工作目录下所有文件
    public static void printFile() {
        List<String> names = plainFilenamesIn(PROJECT);
        for (String name : names) {
            System.out.println(name);
        }
    }


    //这个参数是目标的ID 别和当前headID搞混了
    public static void reset(String commitID) {
        if (!checkGiletFolder()) {
            return;
        }

        //不存在这个commit
        if (!ifExistsCommit(commitID)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        //有未跟踪的文件会被目标Commit所覆盖
        //目标文件的blobmap
        TreeMap<String, String> desCommitBlobMap = getCommitBlobMap(commitID);
        //当前headID
        String headSHA1 = getHead();
        TreeSet<String> untrackedFileNames = untrackedFileNames(headSHA1);
        if (untrackedFileNames.size() != 0) {
            for (String fileName : untrackedFileNames) {
                if (desCommitBlobMap.containsKey(fileName)) {
                    System.out.println("There is an untracked file"
                        + " in the way; delete it, or add and commit it first.");
                    return;
                }
            }
        }
        //删除不在目标commit但是已经追踪了的文件
        TreeMap<String, String> thisCommitMap = getCommitBlobMap(headSHA1);
        for (String fileName : thisCommitMap.keySet()) {
            if (!desCommitBlobMap.containsKey(fileName)) {
                File thisfile = join(PROJECT, fileName);
                thisfile.delete();
            }
        }
        //切换到指定的提交
        checkOutCommit(commitID);
        //移动head
        saveHead(commitID);

        //保存这个分支的reset信息
        String thisBranchName = getCurrentBranch();
        File thisBranchFile = join(BRANCH_FOLDER, thisBranchName);
        Branch thisBranch = readObject(thisBranchFile, Branch.class);
        thisBranch.resetCommit = commitID;
        thisBranch.saveBranch();

        //清空暂存区
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        Removal rmval = readObject(rmvalFile, Removal.class);
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        Addition adt = readObject(adtFile, Addition.class);
        adt.clearAdditionArea();
        rmval.clearRemovalArea();
    }

    //merge 的helper function 用于求当前commit和对应commit的并集
    public static void combine(String commitName) {
        String headComName = getHead();
        TreeMap<String, String> currCommit =
                getCommitBlobMap(headComName);
        TreeMap<String, String> mergeToCom =
                getCommitBlobMap(commitName);
        //merge的那个应该不会为null 当为0的时候要删除所有文件！
        if (mergeToCom.size() == 0) {
            if (currCommit.size() != 0) {
                for (String fileName : currCommit.keySet()) {
                    File thisfile = join(PROJECT, fileName);
                    thisfile.delete();
                }
            }
        }
        for (String fileName : mergeToCom.keySet()) {
            //现在的commit有之前commit的文件
            if (currCommit != null && currCommit.containsKey(fileName)) {
                if (!currCommit.get(fileName).equals(mergeToCom.get(fileName))) {
                    System.out.println("Encountered a merge conflict.");
                    return;
                }
            }
        }
        // 移除目标文件里没有的文件
        for (String fileName : currCommit.keySet()) {
            if (mergeToCom != null && !mergeToCom.containsKey(fileName)) {
                File thisfile = join(PROJECT, fileName);
                thisfile.delete();
            }
        }
    }

    public static boolean dealWithErrMerge(String branchName) {
        if (!Addition.isAdditionEmpty() || !isRmvalEmpty()) {
            System.out.println("You have uncommitted changes.");
            return false;
        }
        File f = join(BRANCH_FOLDER, branchName);
        if (!f.exists()) {
            System.out.println("A branch with that name does not exist.");
            return false;
        }
        if (branchName.equals(getCurrentBranch())) {
            System.out.println("Cannot merge a branch with itself.");
            return false;
        }

        //当前分支的名字
        String currentBranch = getCurrentBranch();
        String curComHead = Branch.getBranchHeadCommit(currentBranch);
        if (ifHasUntrackedFile(curComHead)) {
            System.out.println("There is an "
                    + "untracked file in the way; delete it, "
                    + "or add and commit it first.");
            return false;
        }
        return true;
    }
    public static void merge(String branchName) {
        boolean flag = dealWithErrMerge(branchName);
        if (!flag) {
            return;
        }
        String currentBranch = getCurrentBranch();
        String curComHead = Branch.getBranchHeadCommit(currentBranch);
        String mergedBranHeadCom = Branch.getBranchHeadCommit(branchName);
        File f1 = join(BRANCH_FOLDER, currentBranch);
        File f2 = join(BRANCH_FOLDER, branchName);
        Branch curBranch = readObject(f1, Branch.class);
        Branch mergedBranch = readObject(f2, Branch.class);
        //现在这个branch的commit列表
        TreeMap<Integer, String> curBranComList = curBranch.getCommits();
        // mergeto branch的commit列表
        TreeMap<Integer, String> mergedBranComList = mergedBranch.getCommits();
        // 遍历mergeto branch
        for (Integer key : mergedBranComList.keySet()) {
            String thisValue = mergedBranComList.get(key);
            if (thisValue.equals(curComHead)) {
                //快速前进
                System.out.println("Current branch fast-forwarded.");
                combine(mergedBranHeadCom);
                saveHead(mergedBranHeadCom);
                return;
            }
        }
        //遍历现在branch的commitlist
        for (Integer key : curBranComList.keySet()) {
            String thisValue = curBranComList.get(key);
            if (thisValue.equals(mergedBranHeadCom)) {
                //是祖先！
                System.out.println("Given branch is "
                        + "an ancestor of the current branch.");
                return;
            }
        }
        //找到公共祖先！
        String ancestorCom = "";
        //反向遍历当前branch的commit列表 可能不能这样……需要通过链表来逐一查找每个commit的P列表
        ancestorCom = findLCA(curComHead, mergedBranHeadCom);

        /* for (Map.Entry<Integer, String> entry : curBranComList.descendingMap().entrySet()) {
            String thisCommit = entry.getValue();
            if (Branch.containValue(mergedBranComList, thisCommit)) {
                ancestorCom = thisCommit;
                break;
            }
        }
         */
        // 对当前这三者的文件进行combine curComHead ancestorCom mergedBranHeadCom
        TreeMap<String, String> blobs = mergeHelper(curComHead, ancestorCom, mergedBranHeadCom);
        mergeCommit(branchName, currentBranch, blobs);

    }


    public static void mergeCommit(String branchName,
                                   String currentBranch, TreeMap<String, String> blobs) {
        //创建一个新的commit!
        String msg = "Merged " + branchName + " into " + currentBranch + ".";
        Instant time = Instant.now();
        String mergedBranHeadCom = Branch.getBranchHeadCommit(branchName);
        String[] parent = {getHead(), mergedBranHeadCom};
        Commit newCommit = new Commit(msg, time, blobs, parent);
        //保存！并得到sha1
        String newHead = newCommit.saveCommit();
        //清空暂存区
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        Addition adt = readObject(adtFile, Addition.class);
        adt.clearAdditionArea();
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        Removal rmval = readObject(rmvalFile, Removal.class);
        rmval.clearRemovalArea();
        //要更新当前branch的commit
        File f = join(BRANCH_FOLDER, currentBranch);
        Branch cBranch = readObject(f, Branch.class);
        cBranch.addCommit(newHead);
        saveHead(newHead);
    }
    public static TreeMap<String, String> mergeHelper(String curComHead,
                                   String ancestorCom, String mergedBranHeadCom) {
        //还原commit 找到blobMap
        TreeMap<String, String> currHeadBlobs = getCommitBlobMap(curComHead);
        TreeMap<String, String> ancestorBlobs = getCommitBlobMap(ancestorCom);
        TreeMap<String, String> mergedToBlobs = getCommitBlobMap(mergedBranHeadCom);
        //这三者都有可能是空的……遍历curr
        TreeMap<String, String> clonedMap = new TreeMap<>(currHeadBlobs);
        if (currHeadBlobs != null) {
            for (String fileName : currHeadBlobs.keySet()) {
                String thisBlob = currHeadBlobs.get(fileName);
                if (mergedToBlobs != null && mergedToBlobs.containsKey(fileName)) {
                    String mergedBlob = mergedToBlobs.get(fileName);
                    if (ancestorBlobs != null && ancestorBlobs.containsKey(fileName)) {
                        // 三个都有同名文件的情况
                        String ancestorBlob = ancestorBlobs.get(fileName);
                        //情况1 当前和ansestor一样 mergeto改了
                        if (!mergedBlob.equals(thisBlob) && thisBlob.equals(ancestorBlob)) {
                            Blob.reviveFile(mergedBlob, fileName);
                            clonedMap.put(fileName, mergedBlob);
                            //还要将这个加入addition区域
                            File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
                            Addition adt = readObject(adtFile, Addition.class);
                            adt.addFilebySha1(fileName, mergedBlob);
                        }
                        //情况2 ancestor和mergeto一样 当前的改了
                        if (ancestorBlob.equals(mergedBlob) && !mergedBlob.equals(thisBlob)) {
                            Blob.reviveFile(thisBlob, fileName); //好像也不用..
                        }
                        // 情况8： 冲突了
                        if (!ancestorBlob.equals(thisBlob) && !ancestorBlob.equals(mergedBlob)) {
                            System.out.println("Encountered a merge conflict.");
                            dealConflict(fileName, mergedBlob);
                            //不是冲突就return 还要处理其他文件
                        }
                    }
                } else { // mergeto 没有同名文件
                    if (ancestorBlobs != null && ancestorBlobs.containsKey(fileName)) {
                        String ancestorBlob = ancestorBlobs.get(fileName);
                        if (ancestorBlob.equals(thisBlob)) {
                            //情况3 当前和anscestor一样 mergeto删了
                            File f = join(PROJECT, fileName);
                            f.delete();
                            clonedMap.remove(fileName);
                            // 还要加入removal区域
                            File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
                            Removal rmval = readObject(rmvalFile, Removal.class);
                            rmval.addFile(ancestorBlob, fileName);
                        } else {
                            // 情况9： head做了改动，mergeto没有 发生冲突
                            System.out.println("Encountered a merge conflict.");
                            dealConflict(fileName, null);
                        }
                    } //else 情况4 只有当前有 不增也不减 无事发生（表中最后一列）
                }
            }
        }
        //遍历要merge的文件blob
        if (mergedToBlobs != null) {
            for (String fileName : mergedToBlobs.keySet()) {
                String mergeToBlob = mergedToBlobs.get(fileName);
                if (currHeadBlobs == null || !currHeadBlobs.containsKey(fileName)) {
                    if (ancestorBlobs != null && ancestorBlobs.containsKey(fileName)) {
                        // 情况 5 mergeto和ansestor都有 无事发生 不会复原
                        continue;
                    }
                    if (ancestorBlobs == null || !ancestorBlobs.containsKey(fileName)) {
                        // 情况6 只有mergeto有 要复原
                        Blob.reviveFile(mergeToBlob, fileName);
                        clonedMap.put(fileName, mergeToBlob);
                        // 还要加到addition里面
                        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
                        Addition adt = readObject(adtFile, Addition.class);
                        adt.addFilebySha1(fileName, mergeToBlob);
                    }
                }
            }
        }
        //还有情况7 只有ancestor有 还是无事发生 不用写
        //需要保存这个blobmap吗
        return clonedMap;
    }

    public static void dealConflict(String fileName, String mergedBlob) {
        //重构这个文件
        File f = join(PROJECT, "tem");
        writeContentsAppend(f, "<<<<<<< HEAD\n");
        if (mergedBlob != null) {
            File f2 = join(BLOB_FOLDER, mergedBlob);
            Blob b2 = readObject(f2, Blob.class);
            File f3 = join(PROJECT, fileName);
            String s1 = readContentsAsString(f3);
            byte[] f2b = b2.getContent();
            String s2 = new String(f2b, StandardCharsets.UTF_8);
            writeContentsAppend(f, s1);
            writeContentsAppend(f, "=======\n");
            writeContentsAppend(f, s2);
            dealConflictHelper(f, f3, fileName);
        } else {
            File f3 = join(PROJECT, fileName);
            String s1 = readContentsAsString(f3);
            writeContentsAppend(f, s1);
            writeContentsAppend(f, "=======\n");
            dealConflictHelper(f, f3, fileName);
        }

    }
    public static void dealConflictHelper(File f, File f3, String fileName) {
        writeContentsAppend(f, ">>>>>>>\n");
        String content = readContentsAsString(f);
        // 将读取的内容写入到 fileName 文件中
        writeContents(f3, content);
        //还要加一个暂存区！！！
        Blob b = new Blob(f3);
        String sha1 = b.saveBlob();
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        Addition adt = readObject(adtFile, Addition.class);
        adt.addFilebySha1(fileName, sha1);
        f.delete();
    }
}
