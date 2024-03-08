package gitlet;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.File;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Nishank Gite **/

public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                throw new GitletException("Please enter a command.");
            } else {
                callmethod(args);
            }
        } catch (GitletException gitletErr) {
            System.err.print(gitletErr.getMessage());
            System.exit(0);
        }
    }

    /** Function that will call the method on args.
     * @param args The function calls after git. **/
    private static void callmethod(String[] args) {
        switch (args[0]) {
        case "add":
            gitletadd(args);
            break;
        case "add-remote":
            gitletaddremote(args);
            break;
        case "commit":
            gitletcommit(args);
            break;
        case "merge":
            gitletmerge(args);
            break;
        case "rm":
            gitletrm(args);
            break;
        case "rm-branch":
            gitletrmbranch(args);
            break;
        case "rm-remote":
            gitletrmremote(args);
            break;
        case "init":
            gitletinit(args);
            break;
        case "log":
            gitletlog(args);
            break;
        case "global-log":
            gitletgloballog(args);
            break;
        case "status":
            gitletstatus(args);
            break;
        case "checkout":
            gitletcheckout(args);
            break;
        case "branch":
            gitletbranch(args);
            break;
        case "find":
            gitletfinder(args);
            break;
        case "reset":
            gitletreset(args);
            break;
        default:
            throw new GitletException("No command with that name exists.");
        }
    }

    /** Adding file to be staged for commit in repository.
     * @param args file to be added. **/
    public static void gitletadd(String[] args) throws GitletException {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        } else {
            File adding = Utils.join(CURRWORKDIR, args[1]);
            if (!adding.isFile()) {
                throw new GitletException("File does not exist.");
            } else {
                StagedFile stgdfl = new StagedFile(gitdirectory);
                stgdfl.stagefile(new FileTrail(adding, allblobs));
                if (stgdfl.stgareapath().isFile()) {
                    Utils.writeObject(stgdfl.stgareapath(), stgdfl);
                }
            }
        }
    }

    /** Adds args to our remote.
     * @param args files to add. **/
    public static void gitletaddremote(String[] args) {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else {
            if (!remotesubdir.exists()) {
                remotesubdir.mkdir();
            }
            File remfl = Utils.join(remotesubdir, args[1]);
            if (remfl.exists()) {
                throw new GitletException("A remote with that "
                        + "name already exists.");
            } else {
                try {
                    remfl.createNewFile();
                } catch (IOException dummy) {
                    return;
                }
                String remfilepos = args[2].replace('/', File.separatorChar);
                Utils.writeContents(remfl, remfilepos);
            }
        }
    }

    /** Adds ancestors to our head commit.
     * @param commitheader the head commit we will add to.
     * @param ancestor the ancestor we add. **/
    private static void gitletaddancestor(GitCommit commitheader,
                        HashMap<String, GitCommit> ancestor) {
        if (commitheader instanceof InitializeRepoAndCommit) {
            ancestor.put(commitheader.commithash(), commitheader);
            return;
        } else if (commitheader instanceof GitMerge) {
            ancestor.put(commitheader.commithash(), commitheader);
            gitletaddancestor(Utils.readObject(Utils.join
                    (committracker, commitheader.commitparent()),
                    GitCommit.class), ancestor);
            gitletaddancestor(Utils.readObject(Utils.join
                            (committracker, ((GitMerge) commitheader).
                            mergeparentid()), GitCommit.class), ancestor);
        } else {
            ancestor.put(commitheader.commithash(), commitheader);
            gitletaddancestor(Utils.readObject(Utils.join
                    (committracker, commitheader.commitparent()),
                    GitCommit.class), ancestor);
            return;
        }
    }

    /** Committing all added files.
     * @param args the committed message and operand. **/
    public static void gitletcommit(String[] args) throws GitletException {
        StagedFile stgdfl = new StagedFile(gitdirectory);
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized"
                    + " Gitlet directory.");
        } else if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        } else if (args[1].length() == 0) {
            throw new GitletException("Please enter a commit message.");
        } else if (stgdfl.stgareasize() == 0
            && stgdfl.rmfiles().size() == 0) {
            throw new GitletException("No changes added to the commit.");
        } else {
            GitCommit crcommit = new GitCommit(args[1],
                    System.currentTimeMillis());
            String crbranchinfo = Utils.readContentsAsString(currentbranchpos);
            crcommit.gitcommit(stgdfl, crbranchinfo);
            crcommit.commitadd(committracker);
            File newbradded = Utils.join(allbranches, crbranchinfo);
            Utils.writeContents(newbradded, crcommit.commithash());
            stgdfl.stgareapath().delete();
        }
    }

    /** Merges branch to another according to args.
     * @param args the branches to merge. **/
    public static void gitletmerge(String[] args) throws GitletException {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        } else if (args[1].equals(Utils.
                readContentsAsString(currentbranchpos))) {
            throw new GitletException("Cannot merge a "
                        + "branch with itself.");
        } else if (!Utils.join(allbranches, args[1]).exists()) {
            throw new GitletException("A branch with that "
                    + "name does not exist.");
        } else {
            gitletmergehelper(args);
        }
    }

    /** Helps merge some branch to the head of the branch using args.
     * @param args the args info on the merging files.
     * @param error if there is an error.
     * @param mergingbrhead basically eh head of the
     *                      branch for which we will merge.
     * @param editedb the branch after it has been edited by the merge helper.
     * @param editedh the ehad after it has been edited by the merge helper.
     * @param stgdfl the staged file from the merge. **/
    public static void gitletmergerhelper(HashMap<String, FileTrail> editedb,
                                     HashMap<String, FileTrail> editedh,
                                     String mergingbrhead, StagedFile stgdfl,
                                     boolean error, String[] args) {
        Set<String> alledited = new HashSet<>(editedh.keySet());
        alledited.retainAll(editedb.keySet());
        for (String flnm : alledited) {
            String out = "<<<<<<< HEAD\n";
            if (editedh.get(flnm) != null
                    && editedb.get(flnm) != null && !editedh
                    .get(flnm).equals(editedb.get(flnm))) {
                out += editedh.get(flnm).blobstring();
                out += "=======\n";
                out += editedb.get(flnm).blobstring();
                out += ">>>>>>>";
            } else if (editedh.get(flnm) == null
                    && editedb.get(flnm) == null) {
                out = "<<<<<<< HEAD\n";
            } else if (editedh.get(flnm) == null) {
                out += "=======\n";
                out += editedb.get(flnm).blobstring();
                out += ">>>>>>>";
            } else if (editedb.get(flnm) == null) {
                out += editedh.get(flnm).blobstring();
                out += "=======\n";
                out += ">>>>>>>";
            }
            if (!out.equals("<<<<<<< HEAD\n")) {
                error = true;
                try {
                    File currentfile = Utils.join(CURRWORKDIR, flnm);
                    if (!currentfile.exists()) {
                        currentfile.createNewFile();
                    }
                    Utils.writeContents(currentfile, out);
                    gitletadd(new String[]{"add", currentfile.getName()});
                    stgdfl = new StagedFile(gitdirectory);
                } catch (IOException dummy) {
                    return;
                }
            }
        }

        GitMerge merging = new GitMerge("Merged " + args[1]
                + " into " + Utils.readContentsAsString(currentbranchpos)
                + ".", System.currentTimeMillis(), mergingbrhead);
        String crbranchinfo = Utils.readContentsAsString(currentbranchpos);
        merging.gitcommit(stgdfl, crbranchinfo);
        merging.commitadd(committracker);
        File newbradded = Utils.join(allbranches, crbranchinfo);
        Utils.writeContents(newbradded, merging.commithash());
        stgdfl.stgareapath().delete();
        if (error) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** A helper for the merge.
     * @param args merge file info. **/
    public static void gitletmergehelper(String[] args) {
        boolean err = false;
        StagedFile stgdfl = new StagedFile(gitdirectory);
        if (stgdfl.stgareasize() != 0) {
            throw new GitletException("You have uncommitted changes.");
        }
        String currbrhead = Utils.readContentsAsString(Utils.join(
                allbranches, Utils.readContentsAsString(currentbranchpos)));
        String mergingbrhead = Utils.readContentsAsString(
                Utils.join(allbranches, args[1]));
        String flinfo = gitletsplitpointlocator(mergingbrhead, currbrhead);
        if (flinfo.equals("checkout")) {
            gitletcheckout(new String[]{"checkout", args[1]});
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        GitCommit newfl = Utils.readObject(Utils.join
                (committracker, flinfo), GitCommit.class);
        StagedFile stgdflbranch = Utils.readObject(
                Utils.join(committracker, mergingbrhead),
                GitCommit.class).commitstagearea();
        StagedFile stgdflhead = Utils.readObject(
                Utils.join(committracker, currbrhead),
                GitCommit.class).commitstagearea();
        StagedFile stgdflsplit = newfl.commitstagearea();
        HashMap<String, FileTrail> editedb =
                gitleteditedfiles(stgdflsplit, stgdflbranch);
        HashMap<String, FileTrail> editedh =
                gitleteditedfiles(stgdflsplit, stgdflhead);
        HashMap<String, File> gitdirinfo = new HashMap<>();
        for (File fl : CURRWORKDIR.listFiles()) {
            gitdirinfo.put(fl.getName(), fl);
        }
        for (String flnm : editedb.keySet()) {
            if (!editedh.containsKey(flnm)) {
                if (!stgdfl.trackedfiles().containsKey(flnm)
                        && gitdirinfo.containsKey(flnm)) {
                    throw new GitletException("There is an untracked "
                            + "file in the way; delete it, "
                            + "or add and commit it first.");
                }
            }
        }
        for (String flnm : editedb.keySet()) {
            if (editedb.get(flnm) != null
                    && !editedh.containsKey(flnm)) {
                gitletcheckout(new String[]{"checkout",
                    mergingbrhead, "--", flnm});
                gitletadd(new String[]{"add", flnm});
                stgdfl = new StagedFile(gitdirectory);
            } else if (editedb.get(flnm) == null
                    && !editedh.containsKey(flnm)) {
                gitletrm(new String[]{"rm", flnm});
                stgdfl = new StagedFile(gitdirectory);
            }
        }
        gitletmergerhelper(editedb, editedh, mergingbrhead, stgdfl, err, args);
    }

    /** Removes args files from the repository.
     * @param args the files to be removed. **/
    public static void gitletrm(String[] args) {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        } else {
            StagedFile stgdfl = new StagedFile(gitdirectory);
            File rmfl = Utils.join(CURRWORKDIR, args[1]);
            stgdfl.rmfile(rmfl);
            if (stgdfl.stgareapath().isFile()) {
                Utils.writeObject(stgdfl.stgareapath(), stgdfl);
            }
        }
    }

    /** Removes args branch.
     * @param args the branch info. **/
    public static void gitletrmbranch(String[] args) {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        } else if (!Utils.join(allbranches, args[1]).exists()) {
            throw new GitletException("A branch with that "
                    + "name does not exist.");
        } else if (args[1].equals(Utils.readContentsAsString(
                currentbranchpos))) {
            throw new GitletException("Cannot remove the "
                    + "current branch.");
        } else {
            Utils.join(allbranches, args[1]).delete();
        }
    }

    /** Removes args from our remote.
     * @param args the remote file. **/
    public static void gitletrmremote(String[] args) {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else {
            File remfile = Utils.join(remotesubdir, args[1]);
            if (!remfile.exists()) {
                throw new GitletException("A remote with "
                        + "that name does not exist.");
            } else {
                remfile.delete();
            }
        }
    }

    /** Initialize a new Gitlet repository.
     * @param args the arg init for creating a repo. **/
    public static void gitletinit(String[] args) throws GitletException {
        if (gitdirectory.exists()) {
            throw new GitletException("A Gitlet version-control "
                    + "system already exists in the current directory.");
        } else if (args.length != 1) {
            throw new GitletException("Incorrect operands.");
        } else {
            gitdirectory.mkdir();
            committracker.mkdir();
            allblobs.mkdir();
            allbranches.mkdir();
            InitializeRepoAndCommit initialrepo =
                    new InitializeRepoAndCommit("initial commit", 0);
            initialrepo.firstcommit(origmastHEAD);
            initialrepo.commitadd(committracker);
            File origin = Utils.join(allbranches, "master");
            try {
                currentbranchpos.createNewFile();
                origin.createNewFile();
                Utils.writeContents(currentbranchpos, "master");
                Utils.writeContents(origin, initialrepo.commithash());
            } catch (IOException e) {
                return;
            }
        }
    }

    /** Prints out the logs for the branches in our repository.
     * @param args log call info. **/
    public static void gitletlog(String[] args) {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else if (args.length != 1) {
            throw new GitletException("Incorrect operands.");
        } else {
            File position = Utils.join(allbranches, Utils.readContentsAsString(
                    currentbranchpos));
            String latestcommitid = Utils.readContentsAsString(position);
            while (latestcommitid != null) {
                GitCommit latestcommit = Utils.readObject(
                        Utils.join(committracker, latestcommitid),
                        GitCommit.class);
                latestcommitid = latestcommit.log();
            }
        }
    }

    /** Prints our logs for all of the commits.
     * @param args all the log call info. **/
    public static void gitletgloballog(String[] args) {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else if (args.length != 1) {
            throw new GitletException("Incorrect operands.");
        } else {
            for (File f : committracker.listFiles()) {
                GitCommit trackercommit = Utils.readObject(f, GitCommit.class);
                trackercommit.log();
            }
        }
    }

    /** Basically resets our file or repo to a particular args commit.
     * @param args the commit/file info. **/
    public static void gitletcheckout(String[] args) throws GitletException {
        if (!gitdirectory.exists()) {
            throw new GitletException("A Gitlet "
                    + "version-control system already "
                    + "exists in the current directory.");
        }
        if (args[1].equals("--")) {
            try {
                gitletcheckoutheadfile(args[2]);
            } catch (IOException dummy) {
                return;
            }
        }  else if (args.length == 2) {
            gitletcheckoutbranch(args[1]);
        } else if (args[2].equals("--")) {
            try {
                gitletcheckoutcommitfile(args[1], args[3]);
            } catch (IOException dummy) {
                return;
            }
        } else {
            throw new GitletException("Incorrect operands.");
        }
    }

    /** Checks out a file from out commit based off the id and name.
     * @param commid commit id.
     * @param filenm the file name. **/
    private static void gitletcheckoutcommitfile(String commid,
                        String filenm) throws IOException {
        if (commid.length() < DEFAULTIDLEN) {
            for (File fl : committracker.listFiles()) {
                if (fl.getName().contains(commid)) {
                    commid = fl.getName();
                    break;
                }
            }
        }
        File tracker = Utils.join(committracker, commid);
        if (!tracker.exists()) {
            throw new GitletException("No commit with that id exists.");
        }
        GitCommit check = Utils.readObject(tracker, GitCommit.class);
        File analyzefile = Utils.join(CURRWORKDIR, filenm);
        if (check.commitstagearea().filenames().containsKey(
                analyzefile.getName())) {
            FileTrail analyzeblob = check.commitstagearea().filenames()
                    .get(analyzefile.getName());
            String fileinfo = analyzeblob.blobstring();
            if (!analyzefile.exists()) {
                analyzefile.createNewFile();
            }
            Utils.writeContents(analyzefile, fileinfo);
        } else {
            throw new GitletException("File does not exist in that commit.");
        }
    }

    /** Checks out the args file.
     * @param filenm the file name. **/
    private static void gitletcheckoutheadfile(String filenm)
            throws IOException {
        String origmast = Utils.readContentsAsString(origmastHEAD);
        GitCommit headcommit = Utils.readObject(
                Utils.join(committracker, origmast), GitCommit.class);
        if (headcommit.commitstagearea() != null
                && headcommit.commitstagearea().filenames().containsKey(
                Utils.join(CURRWORKDIR, filenm).getName())) {
            FileTrail analyzeblob = headcommit.commitstagearea().
                    filenames().get(Utils.join(CURRWORKDIR,
                            filenm).getName());
            String fileinfo = analyzeblob.blobstring();
            if (!Utils.join(CURRWORKDIR, filenm).exists()) {
                Utils.join(CURRWORKDIR, filenm).createNewFile();
            }
            Utils.writeContents(Utils.join(CURRWORKDIR, filenm), fileinfo);
        } else {
            throw new GitletException("File does not exist in that commit.");
        }
    }

    /** Checkout a particular branch according to the arg.
     * @param position the position of the branch. **/
    private static void gitletcheckoutbranch(String position) {
        if (!Utils.join(allbranches, position).exists()) {
            throw new GitletException("No such branch exists.");
        } else if (position.equals(
                Utils.readContentsAsString(currentbranchpos))) {
            throw new GitletException("No need to checkout the "
                    + "current branch.");
        }
        GitCommit commitheader = Utils.readObject(Utils.join(
                committracker, Utils.readContentsAsString(origmastHEAD)),
                GitCommit.class);
        File headbrpos = Utils.join(allbranches, position);
        String headbrhash = Utils.readContentsAsString(headbrpos);
        GitCommit branchheader = Utils.readObject(Utils.join(
                committracker, headbrhash), GitCommit.class);
        if (commitheader.commitstagearea() != null) {
            for (String nm : commitheader.commitstagearea()
                    .trackedfiles().keySet()) {
                File rm = Utils.join(CURRWORKDIR, nm);
                if (branchheader.commitstagearea() == null) {
                    Utils.restrictedDelete(rm);
                } else if (!branchheader.commitstagearea().trackedfiles().
                        keySet().contains(nm)) {
                    Utils.restrictedDelete(rm);
                }
            }
        }
        if (branchheader.commitstagearea() != null) {
            for (String key : branchheader.commitstagearea().
                    trackedfiles().keySet()) {
                File saving = Utils.join(CURRWORKDIR, key);
                if (saving.exists()) {
                    if (commitheader.commitstagearea() == null
                            || !commitheader.commitstagearea().trackedfiles().
                            containsKey(key)) {
                        throw new GitletException("There is an untracked file"
                                + " in the way; delete it, "
                                + "or add and commit it first.");
                    }
                } else {
                    try {
                        saving.createNewFile();
                    } catch (IOException dummy) {
                        return;
                    }
                }
                Utils.writeContents(saving,
                        branchheader.commitstagearea().trackedfiles().
                                get(key).blobstring());
            }
        }
        new StagedFile(gitdirectory).stgareapath().delete();
        Utils.writeContents(currentbranchpos, position);
        Utils.writeContents(origmastHEAD, Utils.readContentsAsString(
                Utils.join(allbranches, position)));
    }

    /** Makes a new branch with args.
     * @param args the branch specifics. **/
    public static void gitletbranch(String[] args) {
        if (!gitdirectory.exists()) {
            throw new GitletException(
                    "Not in an initialized Gitlet directory.");
        } else if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        } else if (Utils.join(allbranches, args[1]).exists()) {
            throw new GitletException("A branch with that name "
                    + "already exists.");
        } else {
            File branchheader = Utils.join(allbranches, args[1]);
            try {
                branchheader.createNewFile();
                Utils.writeContents(branchheader,
                        Utils.readContentsAsString(origmastHEAD));
            } catch (IOException io) {
                return;
            }
        }
    }

    /** Find the commit of args.
     * @param args the commit. **/
    public static void gitletfinder(String[] args) {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        } else {
            boolean located = false;
            for (File f : committracker.listFiles()) {
                GitCommit commitchecker = Utils.readObject(f, GitCommit.class);
                if (commitchecker.commitmsg().equals(args[1])) {
                    System.out.println(commitchecker.commithash());
                    located = true;
                }
            }
            if (!located) {
                throw new GitletException("Found no commit "
                        + "with that message.");
            }
        }
    }

    /** Find the split point of 2 branch hashes.
     * @param prevbr  branch hash of a given/prior branch.
     * @param currbr branch hash of our current branch.
     * @return gives string of the splitpoint. **/
    private static String gitletsplitpointlocator(
            String prevbr, String currbr) {
        HashMap<String, GitCommit> prcom = new HashMap<>();
        HashMap<String, GitCommit> crcom = new HashMap<>();
        GitCommit prhd = Utils.
                readObject(Utils.join(committracker, prevbr),
                        GitCommit.class);
        GitCommit crhd = Utils.
                readObject(Utils.join(committracker, currbr),
                        GitCommit.class);
        prcom.put(prhd.commithash(), prhd);
        crcom.put(crhd.commithash(), crhd);
        gitletaddancestor(prhd, prcom);
        gitletaddancestor(crhd, crcom);
        Set<String> cousin =
                new HashSet<String>(prcom.keySet());
        cousin.retainAll(crcom.keySet());
        Set<String> copyofsplt = new HashSet<>(cousin);
        for (String cousinHash : copyofsplt) {
            GitCommit cousincomm = crcom.get(cousinHash);
            if (cousin.contains(cousincomm.commitparent())) {
                GitCommit rmcomm =
                        crcom.get(cousincomm.commitparent());
                while (cousin.contains(rmcomm.commitparent())) {
                    cousin.remove(rmcomm.commithash());
                    rmcomm = crcom.get(rmcomm.commitparent());
                }
                cousin.remove(rmcomm.commithash());
            }
        }
        if (cousin.size() > 1) {
            int cousdist = Integer.MAX_VALUE;
            String tmp = "";
            for (String key : cousin) {
                GitCommit commofhead =
                        Utils.readObject(Utils
                                .join(committracker, currbr), GitCommit.class);
                int dist = gitletdistance(commofhead, key);
                if (cousdist == Integer.MAX_VALUE && dist == cousdist) {
                    tmp = key;
                } else if (Math.min(dist, cousdist) == dist) {
                    cousdist = dist;
                    tmp = key;
                }
            }
            cousin = new HashSet<>();
            cousin.add(tmp);
        }
        if (cousin.contains(prevbr)) {
            System.out.println("Given branch is "
                    + "an ancestor of the current branch.");
            System.exit(0);
        } else if (cousin.contains(currbr)) {
            return "checkout";
        } else {
            List<String> comms = new ArrayList<String>();
            comms.addAll(cousin);
            return comms.get(0);
        }
        return null;
    }

    /** Resets our whole repository to args commit id.
     * @param args the commit id. **/
    public static void gitletreset(String[] args) {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        } else {
            String commitid = args[1];
            File committedfile = Utils.join(committracker, commitid);
            if (!committedfile.exists()) {
                throw new GitletException("No commit with that id exists.");
            }
            GitCommit repoheader = Utils.readObject(Utils.join(
                            committracker, Utils.readContentsAsString(
                                    origmastHEAD)), GitCommit.class);
            GitCommit commitheader = Utils.readObject(Utils.join(
                    committracker, commitid), GitCommit.class);
            if (commitheader.commitstagearea() != null) {
                for (String key : commitheader.commitstagearea().
                        trackedfiles().keySet()) {
                    File tracking = Utils.join(CURRWORKDIR, key);
                    if (tracking.exists()) {
                        if (repoheader.commitstagearea() == null
                                || !repoheader.commitstagearea().
                                trackedfiles().containsKey(key)) {
                            throw new GitletException("There is an untracked"
                                    + " file in the way; delete it, or add and"
                                    + " commit it first.");
                        }
                    } else {
                        try {
                            tracking.createNewFile();
                        } catch (IOException dummy) {
                            return;
                        }
                    }
                    Utils.writeContents(tracking, commitheader
                            .commitstagearea().trackedfiles()
                            .get(key).blobstring());
                }
            }
            if (repoheader.commitstagearea() != null) {
                for (String key : repoheader.commitstagearea()
                        .filenames().keySet()) {
                    File rmfile = Utils.join(CURRWORKDIR, key);
                    if (commitheader.commitstagearea() == null) {
                        Utils.restrictedDelete(rmfile);
                    } else if (!commitheader.commitstagearea().filenames().
                            keySet().contains(key)) {
                        Utils.restrictedDelete(rmfile);
                    }
                }
            }
            new StagedFile(gitdirectory).stgareapath().delete();
            Utils.writeContents(Utils.join(allbranches,
                    Utils.readContentsAsString(currentbranchpos)), commitid);
            Utils.writeContents(origmastHEAD, commitid);
        }

    }

    /** Gets the distance from our head commit to some branch point.
     * @param commitheader the head commit.
     * @param brhash the branch posititon.
     * @return gives distance to our branch from head. **/
    private static int gitletdistance(GitCommit commitheader, String brhash) {
        if (commitheader.commithash().equals(brhash)) {
            return 0;
        } else if (commitheader instanceof InitializeRepoAndCommit) {
            return Integer.MAX_VALUE;
        } else if (commitheader instanceof GitMerge) {
            GitCommit commparent = Utils.readObject
                    (Utils.join(committracker, commitheader.
                            commitparent()), GitCommit.class);
            GitCommit mergeparent = Utils.readObject
                    (Utils.join(committracker, ((GitMerge)
                            commitheader).mergeparentid()), GitCommit.class);
            int totaldistance = Math.min(gitletdistance
                    (commparent, brhash), gitletdistance(mergeparent, brhash));
            if (totaldistance == Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            } else {
                return 1 + totaldistance;
            }
        } else {
            int totaldistance = gitletdistance
                    (Utils.readObject(Utils.join
                            (committracker, commitheader
                                    .commitparent()), GitCommit.class), brhash);
            if (totaldistance == Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            } else {
                return 1 + totaldistance;
            }
        }
    }

    /** Gets modified files according ot the branch and the split-point.
     * @param branchposition input branch.
     * @param splitposition the splitpoint.
     * @return gives mashmap of the modified files. **/
    private static HashMap<String, FileTrail> gitleteditedfiles(
            StagedFile splitposition, StagedFile branchposition) {
        HashMap<String, FileTrail> edited = new HashMap<>();
        if (splitposition != null) {
            for (String br : branchposition.trackedfiles().keySet()) {
                if (splitposition.trackedfiles().containsKey(br)) {
                    if (!splitposition.trackedfiles().get(br)
                            .blobhashcode()
                            .equals(branchposition.trackedfiles()
                                    .get(br).blobhashcode())) {
                        edited.put(br,
                                branchposition.trackedfiles().get(br));
                    }
                } else {
                    edited.put(br,
                            branchposition.trackedfiles().get(br));
                }
            }
            for (String b : splitposition.trackedfiles().keySet()) {
                if (branchposition.rmfiles().contains(b)) {
                    edited.put(b, null);
                }
            }
        } else {
            for (String tr : branchposition.trackedfiles().keySet()) {
                edited.put(tr,
                        branchposition.trackedfiles().get(tr));
            }
            for (String rm : branchposition.rmfiles()) {
                edited.put(rm, null);
            }
        }
        return edited;
    }

    /** Print out the status of our Gitlet repository.
     * @param args status call info. **/
    public static void gitletstatus(String[] args) {
        if (!gitdirectory.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        } else if (args.length != 1) {
            throw new GitletException("Incorrect operands.");
        } else {
            String brpos = Utils.readContentsAsString(currentbranchpos);
            System.out.println("=== Branches ===");
            System.out.println("*" + brpos);
            for (File brfile : allbranches.listFiles()) {
                if (!brfile.getName().equals(brpos)) {
                    System.out.println(brfile.getName());
                }
            }
            System.out.println();
            System.out.println("=== Staged Files ===");
            gitletstatushelper(args);
            System.out.println();
        }
    }

    /** The helper method for GitletStatus.
     * @param args status call info. **/
    private static void gitletstatushelper(String[] args) {
        StagedFile stgfl = new StagedFile(gitdirectory);
        for (String blobk : stgfl.filenames().keySet()) {
            System.out.println(blobk);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String rmfl: stgfl.rmfiles()) {
            System.out.println(rmfl);
        }
        System.out.println();
        HashMap<String, File> gitdirinfo = new HashMap<>();
        for (File cwdfile : CURRWORKDIR.listFiles()) {
            if (cwdfile.isFile()) {
                gitdirinfo.put(cwdfile.getName(), cwdfile);
            }
        }
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String flnm : gitdirinfo.keySet()) {
            String fileinfo = Utils.
                    readContentsAsString(gitdirinfo.get(flnm));
            if (stgfl.trackedfiles().containsKey(flnm)) {
                if (!stgfl.trackedfiles().
                        get(flnm).blobstring().equals(fileinfo)) {
                    System.out.println(flnm + " (modified)");
                }
            } else if (stgfl.filenames().containsKey(flnm)
                    && !stgfl.filenames().get(flnm)
                    .blobstring().equals(fileinfo)) {
                System.out.println(flnm + " (modified)");
            }
        }
        for (String flnm : stgfl.filenames().keySet()) {
            if (!gitdirinfo.containsKey(flnm)) {
                System.out.println(flnm + " (deleted)");
            }
        }
        for (String flnm : stgfl.trackedfiles().keySet()) {
            if (!gitdirinfo.containsKey(flnm)
                    && !stgfl.rmfiles().contains(flnm)) {
                System.out.println(flnm + " (deleted)");
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String flnm : gitdirinfo.keySet()) {
            if (!stgfl.trackedfiles().containsKey(flnm)
                    && !stgfl.filenames().containsKey(flnm)) {
                System.out.println(flnm);
            }
        }
    }

    /** The default length of a commit id. **/
    static final int DEFAULTIDLEN = 40;

    /** Our current working directory. **/
    static final File CURRWORKDIR = new File(".");

    /** Our .gitlet subdirectory in our current working directory. **/
    private static File gitdirectory = Utils.join(CURRWORKDIR, ".gitlet");

    /** The head reference file (like origin master). **/
    private static File origmastHEAD = Utils.join(gitdirectory, "HEAD");

    /** Basically the file to track commits. **/
    private static File committracker = Utils.join(gitdirectory, "commits");

    /** Our remote directory within the subdirectory. **/
    private static File remotesubdir = Utils.join(gitdirectory, "remotes");

    /** File basically holding blobs. **/
    private static File allblobs = Utils.join(gitdirectory, "blobs");

    /** Just the current branch you are in (should change accordingly). **/
    private static File currentbranchpos = Utils.join(gitdirectory,
            "current-branch");

    /** File that hold all the data on branches. **/
    private static File allbranches = Utils.join(gitdirectory, "branches");
}
