package enigma;

import static enigma.EnigmaException.*;

/** Represents a permutation of a range of integers starting at 0 corresponding
 *  to the characters of an alphabet.
 *  @author Nishank Gite
 */
class Permutation {

    /** Set this Permutation to that specified by CYCLES, a string in the
     *  form "(cccc) (cc) ..." where the c's are characters in ALPHABET, which
     *  is interpreted as a permutation in cycle notation.  Characters in the
     *  alphabet that are not included in any cycle map to themselves.
     *  Whitespace is ignored. */
    Permutation(String cycles, Alphabet alphabet) {
        _alphabet = alphabet;
        allcycles = cycles;

    }

    /** Add the cycle c0->c1->...->cm->c0 to the permutation, where CYCLE is
     *  c0c1...cm. */
    private void addCycle(String cycle) {
        allcycles += " " + "(" + cycle + ")";

    }

    /** Return the value of P modulo the size of this permutation. */
    final int wrap(int p) {
        int r = p % size();
        if (r < 0) {
            r += size();
        }
        return r;
    }

    /** Returns the size of the alphabet I permute. */
    int size() {
        return _alphabet.size();
    }

    /** Return the result of applying this permutation to P modulo the
     *  alphabet size. */
    int permute(int p) {
        char letter = permute(_alphabet.toChar(wrap(p)));
        return _alphabet.toInt(letter);
    }

    /** Return the result of applying the inverse of this permutation
     *  to  C modulo the alphabet size. */
    int invert(int c) {
        char letter = invert(_alphabet.toChar(wrap(c)));
        return _alphabet.toInt(letter);
    }

    /** Return the result of applying this permutation to the index of P
     *  in ALPHABET, and converting the result to a character of ALPHABET. */
    char permute(char p) {
        char convert;
        String commacycle = allcycles;
        commacycle = commacycle.replace("(", "");
        commacycle = commacycle.replace(")", "");
        String[] listcycles = commacycle.split("\s");
        for (int i = 0; i < listcycles.length; i++) {
            if (listcycles[i].contains(String.valueOf(p))) {
                for (int k = 0; k < listcycles[i].length(); k++) {
                    if (listcycles[i].charAt(k) == p) {
                        int index = (k + 1) % listcycles[i].length();
                        convert = listcycles[i].charAt(index);
                        return convert;
                    }
                }
            }
        }
        return p;
    }

    /** Return the result of applying the inverse of this permutation to C. */
    char invert(char c) {
        char convert;
        String commacycle = allcycles;
        commacycle = commacycle.replace("(", "");
        commacycle = commacycle.replace(")", "");
        String[] listcycles = commacycle.split("\s");
        for (int i = 0; i < listcycles.length; i++) {
            if (listcycles[i].contains(String.valueOf(c))) {
                for (int k = 0; k < listcycles[i].length(); k++) {
                    if (listcycles[i].charAt(k) == c) {
                        if (k == 0) {
                            int index = listcycles[i].length() - 1;
                            convert = listcycles[i].charAt(index);
                            return convert;
                        }
                        int index = (k - 1) % listcycles[i].length();
                        convert = listcycles[i].charAt(index);
                        return convert;
                    }
                }
            }
        }
        return c;
    }

    /** Return the alphabet used to initialize this Permutation. */
    Alphabet alphabet() {
        return _alphabet;
    }

    /** Return true iff this permutation is a derangement (i.e., a
     *  permutation for which no value maps to itself). */
    boolean derangement() {
        String commacycle = allcycles;
        commacycle = commacycle.replace("(", "");
        commacycle = commacycle.replace(")", "");
        String[] listcycles = commacycle.split("\\s");
        for (int i = 0; i < listcycles.length; i++) {
            if (listcycles[i].length() <= 1) {
                return false;
            }
        }
        return true;
    }

    /** Alphabet of this permutation. */
    private Alphabet _alphabet;

    /** String that holds the cycles. **/
    private String allcycles;
}
