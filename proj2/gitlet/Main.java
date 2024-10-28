package gitlet;


import static gitlet.Repository.log;



/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
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
                    if (args.length != 2) {
                        System.out.println("Please enter a commit message.");
                    } else {
                        Repository.commit(args[1]);
                    }

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
                        if (!args[1].equals("--")) {
                            System.out.println("Incorrect operands.");
                        } else {
                            // 处理 "checkout -- [file name]" 情况
                            Repository.checkoutFile(args[2]);
                        }
                    } else if (args.length == 4) {
                        if (!args[2].equals("--")) {
                            System.out.println("Incorrect operands.");
                        } else {
                            // 处理 "checkout [commit id] -- [file name]" 情况
                            Repository.checkoutFileFromCommit(args[1], args[3]);
                        }
                    } else if (args.length == 2) {
                        // 处理 "checkout [branch name]" 情况
                        Repository.checkoutBranch(args[1]);
                    }
                    break;
                case "branch":
                    Repository.branch(args[1]);
                    break;
                case "rm-branch":
                    Repository.rmBranch(args[1]);
                    break;
                case "reset":
                    Repository.reset(args[1]);
                    break;
                default:
                    System.out.println("No command with that name exists.");
                    break;
            }
        }
        catch (Exception e) {
            Utils.error(e.getMessage());
        }

    }
}
