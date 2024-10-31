package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

/**
 * ClassName: Branch
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/23 17:37
 */import static gitlet.Utils.*;

public class Branch implements Serializable {
    //key小的是前面的，key大的是最近的,用相对大小描述大小关系即可，具体值不重要
    //value表示commit的名字
    TreeMap<Integer, String> commitsList;
    //最后面的就是头结点
    String branchName;
    private static final long serialVersionUID = 1L;
    String resetCommit;
    static boolean containValue(TreeMap<Integer, String> tm, String value) {
        if (tm.size() == 0) {
            return false;
        }
        for (Integer i : tm.keySet()) {
            if (tm.get(i).equals(value)) {
                return true;
            }
        }
        return false;
    }
    static final File BRANCH_FOLDER = join(".gitlet", "branches");
    public Branch(String branchName) {
        commitsList = new TreeMap<Integer, String>();
        this.branchName = branchName;
        resetCommit = "";
    }
    public TreeMap<Integer, String> getCommits() {
        return commitsList;
    }
    public void removeCommits(int index) {
        commitsList.remove(index);
        saveBranch();
    }
    public void removeByName(String commitName) {
        for (int i : commitsList.keySet()) {
            if (commitName.equals(commitsList.get(i))) {
                commitsList.remove(i);
            }
        }
        saveBranch();
    }
    public void  addCommitMap(TreeMap<Integer, String> commits) {
        commitsList.putAll(commits);
    }
    public void addCommit(String commitName) {
        //一定记得检查是否为空！！如果为空调用lastKey()会异常
        if (commitsList.isEmpty()) {
            commitsList.put(0, commitName);
        } else {
            commitsList.put(commitsList.lastKey() + 1, commitName);
        }

        saveBranch();
    }

    public void saveBranch() {
        File f = join(BRANCH_FOLDER, this.branchName);
        //新加入节点后要覆盖源文件
        writeObject(f, this);
    }
    public static List<String> listBranch() {
        File f = BRANCH_FOLDER;

        List<String> branchList = Utils.plainFilenamesIn(f);
        return  branchList;
    }

    public static String getBranchHeadCommit(String branchName) {
        File f = join(BRANCH_FOLDER, branchName);
        Branch b = readObject(f, Branch.class);
        //需要判断这个分支是不是空的
        if (b.commitsList.isEmpty()) {
            return null;
        } else {
            //注意在java里字符串为空和为null是不一样的
            //比较字符串一定要用equal
            if (!b.resetCommit.equals("")) {
                return b.resetCommit;
            }
            String commitName = b.commitsList.get(b.commitsList.lastKey());
            return commitName;
        }

    }
}
