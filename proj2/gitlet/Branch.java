package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.TreeMap;

/**
 * ClassName: Branch
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/23 17:37
 */
import gitlet.Utils;
import gitlet.Commit;

import static gitlet.Utils.join;
import static gitlet.Utils.writeObject;

public class Branch implements Serializable {
    //key小的是前面的，key大的是最近的,用相对大小描述大小关系即可，具体值不重要
    //value表示commit的名字
    TreeMap<Integer, String> commitsList;
    String branchName;

    static final File BRANCH_FOLDER = join(".gitlet","branches");
    public Branch(String branchName) {
        commitsList = new TreeMap<Integer, String>();
        this.branchName = branchName;
    }
    public TreeMap<Integer, String> getCommits() {
        return commitsList;
    }
    public void removeCommits(int index) {
        commitsList.remove(index);
    }
    public void addCommit(String commitName) throws IOException {
        //一定记得检查是否为空！！如果为空调用lastKey()会异常
        if (commitsList.isEmpty()){
            commitsList.put(0, commitName);
        }
        else {
            commitsList.put(commitsList.lastKey() + 1, commitName);
        }

        //判断这个路径是否存在
        File f = join(BRANCH_FOLDER, this.branchName);
        //新加入节点后要覆盖源文件
        writeObject(f,this);
    }
}
