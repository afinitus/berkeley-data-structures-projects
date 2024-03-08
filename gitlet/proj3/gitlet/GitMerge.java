package gitlet;

/** Merge class.
 * @author Nishank Gite **/

public class GitMerge extends GitCommit {
    /** Creates a new commit/merge instance.
     * @param message The commit message.
     * @param time The time when the commit was made.
     * @param head The file path to the commit. **/
    GitMerge(String message, long time, String head) {
        super(message, time);
        parentid = head;
    }

    /** Returns the id of the parent.
     * @return parent_id. **/
    public String mergeparentid() {
        return parentid;
    }

    /** Prints the log for our merging instance.
     * @return the parent id is returned. **/
    @Override
    String log() {
        System.out.println("===");
        System.out.println("commit " + commithash());
        System.out.println("Merge: " + commitparent().substring(0, 7)
                + " " + parentid.substring(0, 7));
        System.out.println("Date: " + timeString());
        System.out.println(commitmsg());
        System.out.println();
        return commitparent();
    }

    /** Once parent is merged this gives the id. **/
    private String parentid;
}
