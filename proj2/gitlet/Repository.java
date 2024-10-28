package gitlet;



import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static gitlet.Commit.*;
import static gitlet.Utils.*;
import static gitlet.addition.isAdditionEmpty;
import static gitlet.removal.isRmvalEmpty;


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
    public static final File PROJECT = CWD;
    public static final File REMOVAL_FOLDER = join(".gitlet/staging_area","removals");
    public static final File ADDITIONS_FOLDER = join(".gitlet/staging_area","additions");
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    static final File COMMIT_FOLDER = join(".gitlet","commits");
    static final File BLOB_FOLDER = join(".gitlet","blobs");

    static final File HEAD = join(".gitlet","head.txt");
    static final File BRANCH_FOLDER = join(".gitlet","branches");
    static final File CURRENT_BRANCH = join(".gitlet","currentBranch.txt");

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
    static void saveHead(String commitSHA1){
        File f = HEAD;
        writeContents(f,commitSHA1);
    }

    static void saveCurrBranch(String branchName){
        File f = CURRENT_BRANCH;
        writeContents(f,branchName);
    }
    static String getHead(){
        File f = HEAD;
        String headCommitSHA1 = readContentsAsString(f);
        return  headCommitSHA1;
    }
    static String getCurrentBranch(){
        File f = CURRENT_BRANCH;
        return readContentsAsString(f);
    }
    public static void init() {
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
        addition additionArea = new addition();
        additionArea.saveAdditionArea();
        File removalsFolder = new File(GITLET_DIR, "staging_area/removals");
        removalsFolder.mkdirs();
        removal removalArea = new removal();
        removalArea.saveRemovalArea();

        //创建初始commit
        String msg = "initial commit";
        Instant time = Instant.ofEpochSecond(0);
        Commit initialCommit = new Commit(msg, time,null, null);
        //写入初始commit到文件中
        String sha1Name = initialCommit.saveCommit();
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
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }

        //源文件
        File file = new File(PROJECT, fileName);
        if(!file.exists()){
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
        removal rmval = readObject(rmvalFile, removal.class);
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        addition adt = readObject(adtFile, addition.class);

        //取消已经标记为删除文件的暂存
        if (rmval.ifExists(fileName)){
            rmval.remove(fileName);
            return;
        }

        //避免不必要的暂存
        if (adt.ifExists(fileName) && adt.sameSHA1(fileName, blobName)){
            return;
        }

        //如果现在头commit文件已经在追踪这个文件且内容一样直接返回
        TreeMap<String, String> thisComBlobMap = getCommitBlobMap(getHead());

        if (thisComBlobMap != null && thisComBlobMap.containsKey(fileName)){
            if (blobName.equals(thisComBlobMap.get(fileName))){
                return;
            }
        }

        //成功暂存，如果同一个文件已经被暂存了，暂存的新文件会覆盖旧文件
        if (adt.ifExists(fileName) && !adt.sameSHA1(fileName, blobName)){
            adt.addFile(f,fileName);
        }
        else {
            adt.addFile(f,fileName);
        }

    }
    //检查staging area的文件是否为空
    public static boolean isDirectoryEmpty(File directory) {
        File[] files = directory.listFiles();
        return files == null || files.length == 0;
    }

    //处理commit,msg是信息
    public static void commit(String msg) {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        if (isRmvalEmpty() && isAdditionEmpty()){
            System.out.println("No changes added to the commit.");
            return;
        }
        //创建一个新的commit with msg & time
        String[] parent;
        String head = getHead();
        parent = new String[]{head}; //这里如果有多个parent怎么办 分支那里重新处理吗
        Commit newCommit = new Commit(msg, Instant.now(), parent);

        // 把addition的blob加入到commit blobset里面
        //得到additionArea
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        addition adt = readObject(adtFile, addition.class);
        TreeMap<String,String> treeMap = adt.getTreeMap();
        //直接copy
        for (String fileName : treeMap.keySet()){
            newCommit.addBlob(fileName, treeMap.get(fileName));
        }
        adt.clearAdditionArea();

        //继承父commit中所有的blob 但是blob所追踪文件的名字不能和这个blob相同……
        String first2 = head.substring(0, 2);
        File commitAbbFolder = join(COMMIT_FOLDER, first2);
        File commitFolder = join(commitAbbFolder, head);
        Commit parentCommit = readObject(commitFolder, Commit.class);
        TreeMap<String,String> parentBlobs = parentCommit.getBlobsMap();
        if (parentBlobs != null){
            for (String fileName : parentBlobs.keySet()){
                if (!newCommit.getBlobsMap().containsKey(fileName)){
                    newCommit.addBlob(fileName, parentBlobs.get(fileName));
                }
            }
        }

        // 把removal里面存在的blob删除，并清空removal
        // 得到rmarea
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        removal rmval = readObject(rmvalFile, removal.class);
        TreeMap<String,String> rmvalTreeMap = rmval.getRemovalsFile();

        for (String fileName : rmvalTreeMap.keySet()){
            newCommit.removeBlob(fileName);
        }
        rmval.clearRemovalArea();

        //commit持久化，移动头指针
       String ThisSHA1 = newCommit.saveCommit();
       saveHead(ThisSHA1);
       //更新分支 添加新节点
       String curBrachName = getCurrentBranch();
       File branchFile = join(BRANCH_FOLDER, curBrachName);
       Branch curBranch = readObject(branchFile, Branch.class);
       curBranch.addCommit(ThisSHA1);
    }

    //fileName是需要被删除的文件
    public static void rm(String fileName) {
        //如果文件被暂存用于新增，`rm` 会将其从暂存区移除。
        //得到staging area 的addition区域
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        addition adt = readObject(adtFile, addition.class);
        //SFN是当前工作目录下的这个文件本身（如果存在的话）
        File SFN = join(PROJECT, fileName);
        if (adt.ifExists(fileName)){
            adt.remove(fileName);
            adt.saveAdditionArea();
            SFN.delete(); //是否需要？
            return;
        }

        //待测试！！！！！！
        //如果文件被当前提交跟踪 (已经 commit)，`rm` 会在暂存区标记其为删除，并从工作目录中删除。
        File head = HEAD;
        TreeMap<String, String> headComBlobMap = getCommitBlobMap(getHead());
        if (headComBlobMap.containsKey(fileName)){
            File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
            removal rmval = readObject(rmvalFile, removal.class);
            rmval.addFile(headComBlobMap.get(fileName), fileName);
            SFN.delete();
        }

        /*String headCommit = readContentsAsString(head);
        String first2 = headCommit.substring(0, 2);
        File thisCommitFilef = join(COMMIT_FOLDER, first2);
        File thisCommitFile = join(thisCommitFilef, headCommit);
        //这个是当前commit
        Commit thisCommit = readObject(thisCommitFile, Commit.class);
         */
        else {
            System.err.println("No reason to remove the file");
        }


        //如果文件既没有被暂存，也没有被跟踪，命令会返回错误消息：`No reason to remove the file`。
    }

    public static void printlog(Commit thisCom, String thisSHA1){
        System.out.print("===\n");  // 使用 \n 明确指定换行
        System.out.print("commit " + thisSHA1 + "\n");
        Instant instant = thisCom.getCommitTime();
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("America/Los_Angeles"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        String formattedDate = zonedDateTime.format(formatter);
        System.out.print("Date: " + formattedDate + "\n");

        System.out.print(thisCom.getMessage() + "\n");
        System.out.print("\n");  // 确保日志输出的格式正确
    }
    public static void log() {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        //从当前这个commit开始从后往前输出信息直到initialCommit
        String headSHA1 = getHead();
        String thisSHA1 = headSHA1;
        String first2 = thisSHA1.substring(0,2);
        File file = join(COMMIT_FOLDER, first2);
        File f = join(file, thisSHA1);
        Commit thisCom = readObject(f, Commit.class);
        while (!thisCom.getMessage().equals("initial commit")){
            printlog(thisCom, thisSHA1);
            String[] parents = thisCom.getParentsSHA1();
            String pfirst2 = parents[0].substring(0,2);
            File pfile = join(COMMIT_FOLDER, pfirst2);
            File nextf = join(pfile, parents[0]);
            thisSHA1 = parents[0];
            thisCom = readObject(nextf, Commit.class);
        }
        printlog(thisCom, thisSHA1);
    }

    public static void globalLog() {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        List<String> allCommitNames = getAllCommitNames();
        for (String commitName : allCommitNames) {
            File f = join(COMMIT_FOLDER, commitName.substring(0,2), commitName);
            Commit thisCom = readObject(f, Commit.class);
            printlog(thisCom,commitName);
        }
    }


    public static void find(String msg) {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        List<String> allCommitNames = getAllCommitNames();
        for (String commitName : allCommitNames) {
            File f = join(COMMIT_FOLDER, commitName.substring(0,2), commitName);
            Commit thisCom = readObject(f, Commit.class);
            if (thisCom.getMessage().equals(msg)){
                System.out.println(commitName);
            }
        }
    }

    //展示所有文件的状态 每个板块按照字典序排序
    public static void status() {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        System.out.println("=== Branches ===");
        String thisBranch = getCurrentBranch();
        List<String> BranchList = Branch.listBranch();
        for (String branchName : BranchList){
            if (branchName.equals(thisBranch)){
                System.out.println("*" + branchName);
            }
            else System.out.println(branchName);
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        addition adt = readObject(adtFile, addition.class);
        String[] StageFiles = adt.allOrderedAdditionFiles();
        if (StageFiles != null){
            for (String fileName : StageFiles){
                System.out.println(fileName);
            }
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        removal rmval = readObject(rmvalFile, removal.class);
        String[] RmvalFiles = rmval.allOrderedRemovalFiles();
        if (RmvalFiles != null) {
            for (String fileName : RmvalFiles){
                System.out.println(fileName);
            }
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");

        System.out.println("=== Untracked Files ===");
        String headSHA1 = getHead();
        TreeSet<String> UtFs = UntrackedFileNames(headSHA1);
        if (UtFs != null){
            for (String fileName : UtFs){
                System.out.println(fileName);
            }
        }

    }

    public static void WantedFileName(TreeMap<String, String> blobNames, String WantedFileName) {
        //blobnameset 反序列化 找这个文件
        if (blobNames != null && blobNames.containsKey(WantedFileName)) {
            File blobFile = join(BLOB_FOLDER, blobNames.get(WantedFileName));
            Blob thisBlob = readObject(blobFile, Blob.class);
            File wantedFile = join(PROJECT, WantedFileName);
            byte[] content = thisBlob.getContent();
            writeContents(wantedFile, content);
            try {
                wantedFile.createNewFile();
            } catch (IOException e) {

            }
        }
        else{
            System.out.println("File does not exist in that commit.");
        }

    }
    public static void checkoutFile(String fileName)  {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        //从当前head里面取出来这个commit
        String headComName = getHead();
        File f = join(COMMIT_FOLDER, headComName.substring(0,2),headComName);
        Commit headCom = readObject(f, Commit.class);

        //从commit取出这个blobnameset
        TreeMap<String, String> blobNames = headCom.getBlobsMap();

        WantedFileName(blobNames, fileName);

    }

    public static void checkoutFileFromCommit(String commitID, String fileName) {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        // 反序列化这个commit
        File f = join(COMMIT_FOLDER, commitID.substring(0,2), commitID);
        if (!f.exists()){
            System.out.println("No commit with that id exists.");
        }
        Commit thisCom = readObject(f, Commit.class);

        //从commit取出这个blobnamemap
        TreeMap<String, String> blobNames = thisCom.getBlobsMap();
        WantedFileName(blobNames, fileName);
    }

    public static void checkoutBranch(String branchName) {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        File f = join(BRANCH_FOLDER, branchName);
        if (!f.exists()){
            System.out.println("No such branch exists.");
            return;
        }
        //更改当前branch名字
        String curbranchName = getCurrentBranch();
        if (curbranchName.equals(branchName)){
            System.out.println("No need to checkout the current branch.");
            return;
        }

        //当前前分支中有未跟踪的文件，并且这些文件会被目标分支覆盖 报错
        // 如何确定当前分支的未跟踪文件
        //对象化当前branch最新的commit

        //将这个最新的commit重现在工作目录中

        //全部覆盖工作目录中的文件（如果存在相同文件）
        // 并删除当前分支中有跟踪但目标分支中没有的文件。

    }

    //创建新的branch
    public static void branch(String newBranchName) {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        File f = join(BRANCH_FOLDER, newBranchName);
        if (f.exists()){
            System.out.println("A branch with that name already exists.");
        }
        else {
            Branch newBranch = new Branch(newBranchName);
            newBranch.addCommit(getHead());
        }
    }

    public static void rmBranch(String rmBranchName) {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        File f = join(BRANCH_FOLDER, rmBranchName);
        if (!f.exists()){
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (rmBranchName.equals(getCurrentBranch())){
            System.out.println("Cannot remove the current branch.");
            return;
        }
        f.delete();
    }

    //调试helper_method 打印工作目录下所有文件
    public static void printFile(){
        List<String> NAMES = plainFilenamesIn(PROJECT);
        for (String name : NAMES){
            System.out.println(name);
        }
    }


    //这个参数是目标的ID 别和当前headID搞混了
    public static void reset(String CommitID) {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        //不存在这个commit
        if (!ifExistsCommit(CommitID)){
            System.out.println("No commit with that id exists.");
            return;
        }
        //有未跟踪的文件会被目标Commit所覆盖
        TreeMap<String, String> desCommitBlobMap = getCommitBlobMap(CommitID);
        //当前headID
        String headSHA1 = getHead();
        TreeSet<String> UtkedFileNames = UntrackedFileNames(headSHA1);
        if (UtkedFileNames != null){
            for (String fileName : UtkedFileNames){
                if (desCommitBlobMap.containsKey(fileName)){
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    return;
                }
            }
        }
        //删除不在目标commit但是已经追踪了的文件
        TreeMap<String, String> thisCommitMap = getCommitBlobMap(headSHA1);
        for (String fileName : thisCommitMap.keySet()){
            if (!desCommitBlobMap.containsKey(fileName)){
                File thisfile = join(PROJECT, fileName);
                thisfile.delete();
            }
        }
        //切换到指定的提交
        checkoutFile(CommitID);
        //移动head
        saveHead(CommitID);
        //清空暂存区
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        removal rmval = readObject(rmvalFile, removal.class);
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        addition adt = readObject(adtFile, addition.class);
        adt.clearAdditionArea();
        rmval.clearRemovalArea();
    }
}
