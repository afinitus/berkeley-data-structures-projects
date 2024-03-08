package enigma;

import java.util.ArrayList;
import java.util.Collection;

/** Class that represents a complete enigma machine.
 *  @author Nishank Gite
 */
class Machine {

    /** A new Enigma machine with alphabet ALPHA, 1 < NUMROTORS rotor slots,
     *  and 0 <= PAWLS < NUMROTORS pawls.  ALLROTORS contains all the
     *  available rotors. */
    Machine(Alphabet alpha, int numRotors, int pawls,
            Collection<Rotor> allRotors) {
        _alphabet = alpha;
        totalrotors = numRotors;
        totalpawls = pawls;
        possiblerotors = allRotors;
    }

    /** Return the number of rotor slots I have. */
    int numRotors() {
        return totalrotors;
    }

    /** Return the number pawls (and thus rotating rotors) I have. */
    int numPawls() {
        return totalpawls;
    }

    /** Return Rotor #K, where Rotor #0 is the reflector, and Rotor
     *  #(numRotors()-1) is the fast Rotor.  Modifying this Rotor has
     *  undefined results. */
    Rotor getRotor(int k) {
        return machinerotors.get(k);
    }

    Alphabet alphabet() {
        return _alphabet;
    }

    /** Set my rotor slots to the rotors named ROTORS from my set of
     *  available rotors (ROTORS[0] names the reflector).
     *  Initially, all rotors are set at their 0 setting. */
    void insertRotors(String[] rotors) {
        machinerotors = new ArrayList<>();
        rotorchecker = new ArrayList<String>();
        for (int i = 0; i < rotors.length; i++) {
            for (Rotor r : possiblerotors) {
                String tracker = rotors[i];
                if (tracker.equals(r.name())) {
                    r.set(0);
                    machinerotors.add(r);
                    if (rotorchecker.contains(tracker)) {
                        throw new EnigmaException("No rotor repeats!");
                    } else {
                        rotorchecker.add(tracker);
                    }
                }
            }
        }
        if (!machinerotors.get(0).reflecting()) {
            throw new EnigmaException("First rotor isn't a reflector!");
        }
        if (machinerotors.size() != totalrotors) {
            throw new EnigmaException("Wrong total rotors inputted!");
        }

    }

    /** Set my rotors according to SETTING, which must be a string of
     *  numRotors()-1 characters in my alphabet. The first letter refers
     *  to the leftmost rotor setting (not counting the reflector).  */
    void setRotors(String setting) {
        if (setting.length() != numRotors() - 1) {
            throw new EnigmaException("Settings length wrong!");
        }
        for (int i = 1; i < machinerotors.size(); i += 1) {
            if (i <= totalrotors - totalpawls - 1) {
                if (machinerotors.get(i).rotates()
                        | machinerotors.get(i).reflecting()) {
                    throw new EnigmaException("Rotor isn't fixed!");
                }
                if (_alphabet.contains(setting.charAt(i - 1))) {
                    machinerotors.get(i).set(setting.charAt(i - 1));
                } else {
                    throw new EnigmaException("Setting not in alphabet!");
                }
            }
            if (i >= totalrotors - totalpawls) {
                if (machinerotors.get(i).rotates()) {
                    if (_alphabet.contains(setting.charAt(i - 1))) {
                        machinerotors.get(i).set(setting.charAt(i - 1));
                    } else {
                        throw new EnigmaException("Setting characters "
                                + "not in alphabet!");
                    }
                } else {
                    throw new EnigmaException("Rotors should rotate!");
                }
            }
        }

    }

    /** Return the current plugboard's permutation. */
    Permutation plugboard() {
        return _plugboard;
    }

    /** Set the plugboard to PLUGBOARD. */
    void setPlugboard(Permutation plugboard) {
        _plugboard = plugboard;
    }

    /** Returns the result of converting the input character C (as an
     *  index in the range 0..alphabet size - 1), after first advancing
     *  the machine. */
    int convert(int c) {
        advanceRotors();
        if (Main.verbose()) {
            System.err.printf("[");
            for (int r = 1; r < numRotors(); r += 1) {
                System.err.printf("%c",
                        alphabet().toChar(getRotor(r).setting()));
            }
            System.err.printf("] %c -> ", alphabet().toChar(c));
        }
        c = plugboard().permute(c);
        if (Main.verbose()) {
            System.err.printf("%c -> ", alphabet().toChar(c));
        }
        c = applyRotors(c);
        c = plugboard().permute(c);
        if (Main.verbose()) {
            System.err.printf("%c%n", alphabet().toChar(c));
        }
        return c;
    }

    /** Advance all rotors to their next position. */
    private void advanceRotors() {
        boolean notchtracker = false;
        for (int i = 1; i < totalpawls + 1; i++) {
            Rotor current = machinerotors.get(machinerotors.size() - i);
            if (i == 1) {
                if (current.atNotch()) {
                    notchtracker = true;
                }
                current.advance();
            } else if (notchtracker) {
                notchtracker = current.atNotch();
                current.advance();
            } else if (current.atNotch() && i != totalpawls) {
                notchtracker = true;
                current.advance();
            }
        }
    }

    /** Return the result of applying the rotors to the character C (as an
     *  index in the range 0..alphabet size - 1). */
    private int applyRotors(int c) {
        int input = c % _alphabet.size();
        for (int i = machinerotors.size() - 1; i >= 0; i--) {
            Rotor current = machinerotors.get(i);
            input = current.convertForward(input);
        }
        for (int i = 1; i < machinerotors.size(); i++) {
            Rotor current = machinerotors.get(i);
            input = current.convertBackward(input);
        }
        return input;
    }

    /** Returns the encoding/decoding of MSG, updating the state of
     *  the rotors accordingly. */
    String convert(String msg) {
        String converted = "";
        for (int i = 0; i < msg.length(); i++) {
            int index = convert(_alphabet.toInt(msg.charAt(i)));
            converted += _alphabet.toChar(index);
        }
        return converted;
    }

    /** Common alphabet of my rotors. */
    private final Alphabet _alphabet;

    /** Total number of rotors. **/
    private int totalrotors;

    /** Total number of pawls. **/
    private int totalpawls;

    /** The collection of all the available rotors. **/
    private Collection<Rotor> possiblerotors;

    /** Array of the rotors in our machine. **/
    private ArrayList<Rotor> machinerotors;

    /** Checks rotor repetition. **/
    private ArrayList<String> rotorchecker;

    /** The plugboard for the machine. **/
    private Permutation _plugboard;


}
