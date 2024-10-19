package capers;

import java.io.File;
import java.io.Serializable;
import static capers.Utils.*;

/** Represents a dog that can be serialized.
 * @author Emily
*/
//记得实现序列化接口
public class Dog implements Serializable{

    /** Folder that dogs live in. */
    static final File DOG_FOLDER = join(".capers","dogs"); // TODO (hint: look at the `join`
                                         //      function in Utils)

    /** Age of dog. */
    private int age;
    /** Breed of dog. */
    private String breed;
    /** Name of dog. */
    private String name;

    /**
     * Creates a dog object with the specified parameters.
     * @param name Name of dog
     * @param breed Breed of dog
     * @param age Age of dog
     */
    public Dog(String name, String breed, int age) {
        this.age = age;
        this.breed = breed;
        this.name = name;
    }

    /**
     * Reads in and deserializes a dog from a file with name NAME in DOG_FOLDER.
     *
     * @param name Name of dog to load
     * @return Dog read from file
     */
    //通过文件中的字节流读取狗
    public static Dog fromFile(String name) {
        // TODO (hint: look at the Utils file)
        File f = join(DOG_FOLDER,name);
        Dog d = readObject(f, Dog.class);
        return d;
    }

    /**
     * Increases a dog's age and celebrates!
     */
    public void haveBirthday() {
        age += 1;
        System.out.println(toString());
        System.out.println("Happy birthday! Woof! Woof!");
    }

    /**
     * Saves a dog to a file for future use.
     */
    //把狗变成字节流存到文件里 文件里是狗数组 所以在这个地方创建文件？
    public void saveDog() {
        // TODO (hint: don't forget dog names are unique)
        //序列化对象 在这里不用，util里的writeObject已经替我们封装好了
        File f = join(DOG_FOLDER, name);
        //存到文件里
        writeObject(f,this);
    }

    @Override
    public String toString() {
        return String.format(
            "Woof! My name is %s and I am a %s! I am %d years old! Woof!",
            name, breed, age);
    }

}
