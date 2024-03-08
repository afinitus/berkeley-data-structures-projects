package gitlet;

import java.io.Serializable;
import java.io.File;
import java.io.IOException;

/** Our Initial Repo and initial commit.
 * @author Nishank Gite **/

public class InitializeRepoAndCommit extends GitCommit implements Serializable {

    /** Our constructor for our repo.
     * @param msg the commit message for our first commit.
     * @param tm The time this commit took place. **/
    InitializeRepoAndCommit(String msg, long tm) {
        setMsg(msg);
        setTime(tm);
    }

    /** Does our first commit.
     * @param head the path to go to the head. **/
    public void firstcommit(File head) {
        try {
            head.createNewFile();
        } catch (IOException e) {
            return;
        }
        setCommitHash();
        Utils.writeContents(head, commithash());
    }

    @Override
    String log() {
        System.out.println("===");
        System.out.println("commit " + commithash());
        System.out.println("Date: " + timeString());
        System.out.println(commitmsg());
        return null;
    }
}
