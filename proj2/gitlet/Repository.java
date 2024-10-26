package gitlet;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

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
    public static final File PROJECT = CWD;
    public static final File REMOVAL_FOLDER = join(".gitlet/staging_area","removals");
    public static final File ADDITIONS_FOLDER = join(".gitlet/staging_area","additions");
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    static final File COMMIT_FOLDER = join(".gitlet","commits");
    static final File BLOB_FOLDER = join(".gitlet","blobs");
    static final File FILENAME_FOLDER = join(".gitlet/staging_area","fileNames");

    static final File HEAD = join(".gitlet","head.txt");

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

        //成功暂存，如果同一个文件已经被暂存了，暂存的新文件会覆盖旧文件
        if (adt.ifExists(fileName) && !adt.sameSHA1(fileName, blobName)){
            adt.addFile(f,fileName);
            return;
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
        //创建一个新的commit with msg & time
        String[] parent;
        String head = getHead();
        parent = new String[]{head}; //这里如果有多个parent怎么办 分支那里重新处理吗
        Commit newCommit = new Commit(msg, Instant.now(), parent);

        // 把addition的blob加入到commit blobset里面
        //得到additionArea
        File adtFile = join(ADDITIONS_FOLDER, "additionTreeMap");
        addition adt = readObject(adtFile, addition.class);
        String[] fileNames = adt.allAdditionFilesSHA1();
        for (String fileName : fileNames){
            newCommit.addBlob(fileName);
        }
        adt.clearAdditionArea();
        //继承父commit中所有的blob
       /* File commitFolder = join(COMMIT_FOLDER, parent);
        Commit parentCommit = readObject(commitFolder, Commit.class);
        String parentSha1[] = parentCommit.allBlobString();
        if (parentSha1 != null){
            for (String fileName : parentSha1){
                newCommit.addBlob(fileName); //设置成set很好的解决了重复性问题
            }
        }
*/
        // 把removal里面存在的blob删除，并清空removal
        // 得到rmarea
        File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
        removal rmval = readObject(rmvalFile, removal.class);
        String[] rmFileNames = rmval.allRemovalFilesSHA1();
        for (String fileName : rmFileNames){
            newCommit.removeBlob(fileName);
        }
        rmval.clearRemovalArea();

        //commit持久化，移动头指针
       String ThisSHA1 = newCommit.saveCommit();
       saveHead(ThisSHA1);
    }

    //fileName是需要被删除的文件
    public static void rm(String fileName) {
        //如果文件被暂存用于新增，`rm` 会将其从暂存区移除。
        //得到staging area 的addition区域
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
        String headCommit = readContentsAsString(head);
        File thisCommitFile = join(COMMIT_FOLDER, headCommit);
        //这个是当前commit
        Commit thisCommit = readObject(thisCommitFile, Commit.class);

        //得到要删除文件的sha1
        String rmFileSHA1 = Blob.getSHA1ByFile(SFN);
        if (thisCommit.ifExistsBlob(rmFileSHA1)){
            thisCommit.removeBlob(rmFileSHA1);
            //得到staging area 的removalfile
            File rmvalFile = join(REMOVAL_FOLDER, "removalTreeMap");
            removal rmval = readObject(rmvalFile, removal.class);
            rmval.addFile(rmFileSHA1,fileName);
            SFN.delete();
        }
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
        File f = join(COMMIT_FOLDER, thisSHA1);
        Commit thisCom = readObject(f, Commit.class);
        while (!thisCom.getMessage().equals("initial commit")){
            printlog(thisCom, thisSHA1);
            String[] parents = thisCom.getParentsSHA1();

            File nextf = join(COMMIT_FOLDER, parents[0]);
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
        List<String> CommitFileNames = plainFilenamesIn(COMMIT_FOLDER);
        for (String fileName : CommitFileNames){
            File f = join(COMMIT_FOLDER, fileName);
            Commit thisCom = readObject(f, Commit.class);
            printlog(thisCom,fileName);
        }
    }


    public static void find(String msg) {
        if (!checkFolder()){
            System.err.println("There is no .Gitlet folder.");
            return;
        }
        List<String> CommitFileNames = plainFilenamesIn(COMMIT_FOLDER);
        for (String fileName : CommitFileNames){
            File f = join(COMMIT_FOLDER, fileName);
            Commit thisCom = readObject(f, Commit.class);
            if (thisCom.getMessage().equals(msg)){
                System.out.println(fileName);
            }
        }
    }

    //展示所有文件的状态 每个板块按照字典序排序
    public static void status() {
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

    }

    public static void WantedFileName(HashSet<String> blobNames, String WantedFileName) {
        //blobnameset 反序列化 找这个文件
        for (String blobName : blobNames){
            File blobFile = join(BLOB_FOLDER, blobName);
            Blob thisBlob = readObject(blobFile, Blob.class);

            if (thisBlob.getFileName().equals(WantedFileName)){
                //找到了就还原
                File wantedFile = join(PROJECT, WantedFileName);
                byte[] content = thisBlob.getContent();
                Utils.writeContents(wantedFile,content);
                try {
                    wantedFile.createNewFile();
                }
                catch (IOException e){

                }
                return;
            }
            System.out.println("File does not exist in that commit.");
        }
    }
    public static void checkoutFile(String fileName)  {
        //从当前head里面取出来这个commit
        String headComName = getHead();
        File f = join(COMMIT_FOLDER, headComName);
        Commit headCom = readObject(f, Commit.class);

        //从commit取出这个blobnameset
        HashSet<String> blobNames = headCom.getBlobsSet();

        WantedFileName(blobNames, fileName);

    }

    public static void checkoutFileFromCommit(String commitID, String fileName) {
        // 反序列化这个commit
        File f = join(COMMIT_FOLDER, commitID);
        if (!f.exists()){
            System.out.println("No commit with that id exists.");
        }
        Commit thisCom = readObject(f, Commit.class);

        //从commit取出这个blobnameset
        HashSet<String> blobNames = thisCom.getBlobsSet();
        WantedFileName(blobNames, fileName);
    }

    public static void checkoutBranch(String arg) {
    }
}
