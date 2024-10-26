package gitlet;

import java.io.IOException;

import static gitlet.Repository.log;
import static gitlet.Repository.rm;


/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {
        // TODO: what if args is empty?
        try {
            String firstArg = args[0];
            switch (firstArg) {
                case "init":
                    Repository.init();
                    break;
                case "add":
                    Repository.add(args[1]);
                    // TODO: handle the `add [filename]` command
                    break;
                case "commit":
                    Repository.commit(args[1]);
                    break;
                case "rm":
                    Repository.rm(args[1]);
                    break;
                case "log":
                    log();
                    break;
                case "global-log":
                    Repository.globalLog();
                    break;
                case "find":
                    Repository.find(args[1]);
                    break;
                case "status":
                    Repository.status();
                    break;
                case "checkout":
                    // 根据不同的参数数量来处理 checkout 命令
                    if (args.length == 3 && args[1].equals("--")) {
                        // 处理 "checkout -- [file name]" 情况
                        Repository.checkoutFile(args[2]);
                    } else if (args.length == 4 && args[2].equals("--")) {
                        // 处理 "checkout [commit id] -- [file name]" 情况
                        Repository.checkoutFileFromCommit(args[1], args[3]);
                    } else if (args.length == 2) {
                        // 处理 "checkout [branch name]" 情况
                        Repository.checkoutBranch(args[1]);
                    }
                    // TODO: FILL THE REST IN
            }
        }
        catch (Exception e) {
            Utils.error(e.getMessage());
        }
    }
}
