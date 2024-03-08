package gitlet;

import java.io.Serializable;
import java.io.File;

/** Tracks files/blobs.
 * @author Nishank Gite  **/

public class FileTrail implements Serializable {
    /** Makes the basic git unit of blob and stores info accordingly.
     * @param mkblob the blob or file to be made.
     * @param dir the Directory in which we save this. **/
    FileTrail(File mkblob, File dir) {
        blobdir = dir;
        if (mkblob.isFile()) {
            name = mkblob.getName();
            file = mkblob;
            str = Utils.readContentsAsString(mkblob);
            hash = Utils.sha1(Utils.serialize(this));
        } else {
            throw new GitletException("File doesn't exist.");
        }
        if (!Utils.join(blobdir, hash).exists()) {
            Utils.writeObject(Utils.join(blobdir, hash), this);
        }
    }

    /** return file name of the blob.
     * @return  _name of the file. **/
    String blobname() {
        return name;
    }

    /** Returns the hash for our blob.
     * @return the blob's hashcode. */
    String blobhashcode() {
        return hash;
    }

    /** This returns the contents of our blob.
     * @return String containing file info. **/
    String blobstring() {
        return str;
    }

    /** File we are now changing to become the basic git unit of blob. **/
    private File file;

    /** Our blob hashcode. **/
    private String hash;

    /** The directory in which we save the blob. **/
    private File blobdir;

    /**  The name we saved this blob as. **/
    private String name;

    /** The String contents of our blob. **/
    private String str;
}
