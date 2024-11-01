package gitlet;


import java.io.File;
import java.io.Serializable;


import static gitlet.Utils.*;

/**
 * ClassName: Blob
 *
 * @author Emily
 * @version 1.0
 * @Create 2024/10/23 10:36
 */
public class Blob implements Serializable {

    //保存文件快照的位置和名字
    String fileName;
    // String thisSHA1;  作为名字

    //此类存储时的名字是确定文件唯一性的ID属性
    static final int NUM = 10;
    static final File BLOB_FOLDER = join(".gitlet", "blobs");

    public static final File PROJECT = new File(System.getProperty("user.dir"));
    private byte[] content;
    //以字节码形式保存文件内容，适用于各种类型的文件

    //不用生成单独的哈希字段了，因为无益于确定文件的唯一性，哈希码直接作为每个blob的名字即可
    public Blob(File file) {
        this.fileName = file.getName();
        this.content = readContents(file);  // 读取文件内容
    }
    private Blob() {
        fileName = "defualt.txt";
        final File cwd = new File(System.getProperty("user.dir"));
        File f = join(cwd, ".gitlet", "Blobs");
        f.mkdirs();
        content = new byte[NUM];
    }

    public String saveBlob() {
        byte[] thisByte = serialize(this);
        String blobFileName = sha1(thisByte); //哈希编码作为blob的名字
        File f = join(BLOB_FOLDER, blobFileName);
        writeObject(f, this);
        return blobFileName;
    }


    public byte[] getContent() {
        return content;
    }

    //判断两个文件的内容一不一样
    public String getSHA1() {
        return Utils.sha1(content);
    }

    public String getFileName() {
        return fileName;
    }

    public static void reviveFile(String blobName, String fileName) {
        File f = new File(BLOB_FOLDER, blobName);
        Blob thisBlob = readObject(f, Blob.class);
        byte[] thisByte = thisBlob.getContent();
        File pjFile = join(PROJECT, fileName);
        byte[] content = thisBlob.getContent();
        writeContents(pjFile, content); //是否已经保存？

    }
    public static String getSHA1ByFile(File file) {
        Blob b = new Blob(file);
        byte[] thisByte = serialize(b);
        String bolbSha1 = sha1(thisByte);
        return bolbSha1;
    }

}
