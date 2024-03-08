package gitlet;

import java.util.TreeMap;
import java.util.ArrayList;
import java.io.IOException;
import java.io.Serializable;
import java.io.File;

/** Staging Area of Gitlet.
 * @author Nishank Gite **/

public class StagedFile implements Serializable, Dumpable {
    /** Creates a new Staging Area.
     * @param directory where we save files. **/

    StagedFile(File directory) {
        dir = directory;
        pos = Utils.join(directory, "stage");
        headpos = Utils.join(directory, "HEAD");
        try {
            if (headpos.length() != 0 && headpos.isFile()) {
                cpParent();
            }
            if (pos.length() != 0 && pos.isFile()) {
                cpState();
            } else {
                pos.createNewFile();
            }
        } catch (IOException e) {
            return;
        }
    }

    /** Adds file to the staging area.
     * @param blob some blob or file to be added. **/
    public void stagefile(FileTrail blob) {
        if (rmfiles.contains(blob.blobname())) {
            rmfiles.remove(blob.blobname());
        }
        if (filenames != null && filenames.containsKey(blob.blobname())) {
            String ourblobhash = filenames.get(blob.blobname()).blobhashcode();
            filenames.remove(blob.blobname());
            blobs.remove(ourblobhash);
            blobs.put(blob.blobhashcode(), blob);
            filenames.put(blob.blobname(), blob);
            filetrails.put(blob.blobname(), blob);
        } else if (stgarea != null && stgarea.blobs.
                containsKey(blob.blobhashcode())) {
            if (blobs.containsKey(blob.blobhashcode())) {
                blobs.remove(blob.blobhashcode());
                filenames.remove(blob.blobname());
            }
        } else {
            blobs.put(blob.blobhashcode(), blob);
            filenames.put(blob.blobname(), blob);
            filetrails.put(blob.blobname(), blob);
        }
    }

    /** Removes file from the staging area.
     * @param remove file to be removed. **/
    public void rmfile(File remove) {
        boolean gone = false;
        if (filenames.containsKey(remove.getName())) {
            String commhash = filenames.get(remove.getName()).blobhashcode();
            filenames.remove(remove.getName());
            blobs.remove(commhash);
            filetrails.remove(remove.getName());
            gone = true;
        }
        if (stgarea != null && stgarea.trackedfiles().
                containsKey(remove.getName())) {
            filetrails.remove(remove.getName());
            rmfiles.add(remove.getName());
            Utils.restrictedDelete(remove);
            gone = true;
        }
        if (!gone) {
            throw new GitletException("No reason to remove the file.");
        }
    }

    /** Copy current commit to staging area. **/
    private void cpState() {
        StagedFile stgfl = Utils.readObject(pos, StagedFile.class);
        blobs.putAll(stgfl.blobs);
        filenames.putAll(stgfl.filenames);
        rmfiles.addAll(stgfl.rmfiles());
        filetrails.putAll(stgfl.trackedfiles());
        for (String file : rmfiles) {
            if (filetrails.containsKey(file)) {
                filetrails.remove(file);
            }
        }

    }

    /** Copy commit parent to staging area. **/
    private void cpParent() {
        String parid = Utils.readContentsAsString(headpos);
        GitCommit par = Utils.readObject(Utils.join(dir,
                "commits", parid), GitCommit.class);
        stgarea = par.commitstagearea();
        if (stgarea != null) {
            filetrails.putAll(stgarea.trackedfiles());
        }

        if (stgarea != null && stgarea.rmfiles.size() > 0) {
            for (String rmfile : stgarea.rmfiles) {
                if (stgareasize() > 0) {
                    FileTrail fltr = filenames.get(rmfile);
                    filenames.remove(fltr.blobname());
                    blobs.remove(fltr.blobhashcode());
                }
            }
        }
    }

    /** Returns the staging area.
     * @return gives staging area. **/
    StagedFile stgarea() {
        return stgarea;
    }

    /** Gives the size of our staging area.
     * @return gives stage area size. **/
    int stgareasize() {
        return Math.max(Math.max(filenames.size(),
                blobs.size()), rmfiles.size());
    }

    /** Gives the directory to the staging area.
     * @return gives staging area directory. **/
    File stgareapath() {
        return pos;
    }

    /** Gives all files to removed/ have been in this staging area.
     * @return gives files to be removed from staging area and removed ones. **/
    ArrayList<String> rmfiles() {
        return rmfiles;
    }

    /** Gives our treemap containing all the blobs in our current staging area.
     * @return gives all our blobs. **/
    TreeMap<String, FileTrail> blobs() {
        return blobs;
    }

    /** Gives us all the files that are being actively tracked.
     * @return  gives our tracked files. **/
    TreeMap<String, FileTrail> trackedfiles() {
        return filetrails;
    }

    /** Returns our gitlet directory we are working through.
     * @return gives our directory. **/
    File dir() {
        return dir;
    }

    /** Returns the path to the head.
     * @return gives directory to head. **/
    File headpos() {
        return headpos;
    }

    /** Returns all names of files in the staging area.
     * @return gives file names. **/
    TreeMap<String, FileTrail> filenames() {
        return filenames;
    }

    @Override
    public void dump() {
        if (filenames == null && blobs == null) {
            System.out.println("No blobs added");
        } else {
            System.out.println("Blob Names :");
            System.out.println(filenames.toString());
            System.out.println("Blob Tree Map: ");
            System.out.println(blobs.toString());
            System.out.println();
        }
    }

    /** The Staging area for our prior commits. **/
    private StagedFile stgarea;

    /** Treemap of all current staging area filenames. **/
    private TreeMap<String, FileTrail> filenames = new TreeMap<>();

    /** Treemap of all Staging area blobs.  **/
    private TreeMap<String, FileTrail> blobs = new TreeMap<>();

    /** A file that contains our reference to the working git directory. **/
    private File dir;

    /** Files to be removed upon our next commit. **/
    private ArrayList<String> rmfiles = new ArrayList<>();

    /** All files being trailed in position. **/
    private TreeMap<String, FileTrail> filetrails = new TreeMap<>();

    /** Pathway reference to our staging area. **/
    private File pos;

    /** The path to the HEAD. **/
    private File headpos;
}
