package enigma;

import static enigma.EnigmaException.*;

/** Class that represents a rotating rotor in the enigma machine.
 *  @author Nishank Gite
 */
class MovingRotor extends Rotor {

    /** A rotor named NAME whose permutation in its default setting is
     *  PERM, and whose notches are at the positions indicated in NOTCHES.
     *  The Rotor is initally in its 0 setting (first character of its
     *  alphabet).
     */
    MovingRotor(String name, Permutation perm, String notches) {
        super(name, perm);
        rotornotches = notches;
    }

    /** Moving rotors should rotate. **/
    @Override
    boolean rotates() {
        return true;
    }

    /** Advancing the setting of the rotor. **/
    @Override
    void advance() {
        int advance = permutation().wrap(setting() + 1);
        super.set(advance);
    }

    /** This rotor could be at a rotatable notch. **/
    @Override
    boolean atNotch() {
        for (int i = 0; i < notches().length(); i++) {
            if (alphabet().toInt(notches().charAt(i)) == setting()) {
                return true;
            }
        }
        return false;
    }

    /** The notches of the moving rotor. **/
    @Override
    String notches() {
        return rotornotches;
    }

    /** Keeps track of the rotornotches positions. **/
    private String rotornotches;

}
