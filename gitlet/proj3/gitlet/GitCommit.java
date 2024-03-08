package gitlet;

import java.text.SimpleDateFormat;
import java.io.Serializable;
import java.util.Date;
import java.io.File;

/** Commit class.
 * @author Nishank Gite **/

public class GitCommit implements Dumpable, Serializable {
    /** Default constructor. **/
    public GitCommit() {
    }

    /** Constructs new Commit with given message and time.
     * @param messg the -m message portion of the commit.
     * @param tm the time of the commit. **/
    public GitCommit(String messg, long tm) {
        setMsg(messg);
        setTime(tm);
    }

    /** Performs the commit operation.
     * @param stagearea the place where everything is being staged.
     * @param branch the branch where we commit to. **/
    void gitcommit(StagedFile stagearea, String branch) {
        br = branch;
        stgarea = stagearea;
        File path = Utils.join(stgarea.dir(),
                "branches", branch);
        dir = Utils.join(stgarea.dir(), "commits");
        stgarea.stgareapath().delete();
        parent = Utils.readContentsAsString(path);
        setCommitHash();
        Utils.writeContents(stgarea.headpos(), hash);
    }

    /** Gives commit time.
     * @return gives commit time. **/
    long committime() {
        return _time;
    }

    /** Set the time when this commit took place.
     * @param time time of commit. **/
    void setTime(long time) {
        _time = time;
    }

    /** Returns the time in the correct string fashion.
     * @return correctly formatted time in string format. **/
    public String timeString() {
        SimpleDateFormat dateform =
                new SimpleDateFormat("E MMM dd HH:mm:ss yyyy Z");
        return dateform.format(new Date(committime()));
    }

    /** Gives commit message.
     * @return gives commit msg. **/
    String commitmsg() {
        return msg;
    }

    /** Set the message for this commit.
     * @param message message -m for the commit **/
    void setMsg(String message) {
        msg = message;
    }

    /** Gives commit id of parent.
     * @return gives commit id of parent. **/
    String commitparent() {
        return parent;
    }

    /** Return the hash for this commit.
     * @return gives hash of this commit. **/
    String commithash() {
        return hash;
    }

    /** Set the hash id for this commit. **/
    void setCommitHash() {
        hash = Utils.sha1(Utils.serialize(this));
    }

    /** Returns the staging area for this commit.
     * @return the stage area.  **/
    StagedFile commitstagearea() {
        return stgarea;
    }

    /** Prints out the log for our commit.
     * @return returns the parent for the id check. **/
    String log() {
        System.out.println("===");
        System.out.println("commit " + hash);
        System.out.println("Date: " + timeString());
        System.out.println(msg);
        System.out.println();
        return parent;
    }

    /** Adds the commit into a specific file directory.
     * @param dircomm the directory to save commit into. **/
    void commitadd(File dircomm) {
        Utils.writeObject(Utils.join(dircomm, hash), this);
    }

    @Override
    public void dump() {
        System.out.println(msg + "at " + timeString());
        System.out.println("HEAD was at" + parent);
        System.out.println("NEW HEAD is" + hash + ", "
                + Utils.readContentsAsString(stgarea.headpos()));
        System.out.println("+++++");
    }

    /** The message for the commit. **/
    private String msg;

    /** Time of the commit. **/
    private long _time;

    /** Hash of the commit id. **/
    private String hash;

    /** Directory of which to push the commit into. **/
    private File dir;

    /** The place where we stage all commits. **/
    private StagedFile stgarea;

    /**  The corresponding branch of the commit. **/
    private String br = "master";

    /** The parent of this commit. **/
    private String parent = null;






}
