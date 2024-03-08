package enigma;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;


import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import jdk.jshell.spi.ExecutionControl;
import ucb.util.CommandArgs;

import static enigma.EnigmaException.*;

/** Enigma simulator.
 *  @author Nishank Gite
 */
public final class Main {

    /** Process a sequence of encryptions and decryptions, as
     *  specified by ARGS, where 1 <= ARGS.length <= 3.
     *  ARGS[0] is the name of a configuration file.
     *  ARGS[1] is optional; when present, it names an input file
     *  containing messages.  Otherwise, input comes from the standard
     *  input.  ARGS[2] is optional; when present, it names an output
     *  file for processed messages.  Otherwise, output goes to the
     *  standard output. Exits normally if there are no errors in the input;
     *  otherwise with code 1. */
    public static void main(String... args) {
        try {
            CommandArgs options =
                new CommandArgs("--verbose --=(.*){1,3}", args);
            if (!options.ok()) {
                throw error("Usage: java enigma.Main [--verbose] "
                            + "[INPUT [OUTPUT]]");
            }

            _verbose = options.contains("--verbose");
            new Main(options.get("--")).process();
            return;
        } catch (EnigmaException excp) {
            System.err.printf("Error: %s%n", excp.getMessage());
        }
        System.exit(1);
    }

    /** Open the necessary files for non-option arguments ARGS (see comment
      *  on main). */
    Main(List<String> args) {
        _config = getInput(args.get(0));

        if (args.size() > 1) {
            _input = getInput(args.get(1));
        } else {
            _input = new Scanner(System.in);
        }

        if (args.size() > 2) {
            _output = getOutput(args.get(2));
        } else {
            _output = System.out;
        }
    }

    /** Return a Scanner reading from the file named NAME. */
    private Scanner getInput(String name) {
        try {
            return new Scanner(new File(name));
        } catch (IOException excp) {
            throw error("could not open %s", name);
        }
    }

    /** Return a PrintStream writing to the file named NAME. */
    private PrintStream getOutput(String name) {
        try {
            return new PrintStream(new File(name));
        } catch (IOException excp) {
            throw error("could not open %s", name);
        }
    }

    /** Configure an Enigma machine from the contents of configuration
     *  file _config and apply it to the messages in _input, sending the
     *  results to _output. */
    private void process() {
        Machine enigmamachine = readConfig();
        String settings = _input.nextLine();
        if (settings.charAt(0) == '*') {
            setUp(enigmamachine, settings);
            while (_input.hasNextLine()) {
                String input = _input.nextLine();
                if (input.contains("*")) {
                    setUp(enigmamachine, input);
                } else if (input.isEmpty()) {
                    _output.println();
                } else {
                    input = input.replace(" ", "");
                    input = enigmamachine.convert(input);
                    printMessageLine(input);
                }
            }
        } else {
            throw new EnigmaException("First line is settings!");
        }

    }

    /** Return an Enigma machine configured from the contents of configuration
     *  file _config. */
    private Machine readConfig() {
        try {
            possiblerotors = new ArrayList<Rotor>();
            String machalpha = _config.nextLine();
            if (machalpha.contains("-+[]{}|*&^%$#@+)(`~")) {
                throw new EnigmaException("Illegal Symbols!");
            }
            _alphabet = new Alphabet(machalpha);
            totalrotors = _config.nextInt();
            totalpawls = _config.nextInt();
            _config.nextLine();
            while (_config.hasNext()) {
                possiblerotors.add(readRotor());
            }
            return new Machine(_alphabet, totalrotors,
                    totalpawls, possiblerotors);

        } catch (NoSuchElementException excp) {
            throw error("configuration file truncated");
        }
    }

    /** Return a rotor, reading its description from _config. */
    private Rotor readRotor() {
        try {
            String notchpos = "";
            String rotorname = _config.next();
            String make = _config.next();
            while (_config.hasNext("(\\(\\S+\\))+")) {
                notchpos += _config.next().replaceAll(
                        "\\)\\(", ") (") + " ";
            }
            Permutation perm = new Permutation(notchpos, _alphabet);
            String rotornotches = "";
            if (make.charAt(0) == 'M') {
                if (make.length() <= 1) {
                    throw new EnigmaException("Moving rotor needs notches!");
                } else {
                    for (int i = 1; i < make.length(); i++) {
                        rotornotches += make.charAt(i);
                    }
                    return new MovingRotor(rotorname, perm, rotornotches);
                }
            } else if (make.charAt(0) == 'N') {
                if (make.length() != 1) {
                    throw new EnigmaException("FixedRotor shouldn't"
                            + " have a notch!");
                } else {
                    return new FixedRotor(rotorname, perm);
                }
            } else if (make.charAt(0) == 'R') {
                if (make.length() != 1) {
                    throw new EnigmaException("Reflector shouldn't"
                            + " have a notch!");
                } else {
                    return new Reflector(rotorname, perm);
                }
            } else {
                throw new EnigmaException("Not a rotor!");
            }

        } catch (NoSuchElementException excp) {
            throw error("bad rotor description");
        }
    }

    /** Set M according to the specification given on SETTINGS,
     *  which must have the format specified in the assignment. */
    private void setUp(Machine M, String settings) {
        String[] rotors = new String[M.numRotors()];
        Scanner settingscan = new Scanner(settings);
        if (settings.charAt(0) == '*') {
            settingscan.next();
            for (int i = 0; i < M.numRotors(); i += 1) {
                rotors[i] = settingscan.next();
            }
            M.insertRotors(rotors);
            String allcycles = "";
            String input = settingscan.next();
            if (input.length() == M.numRotors() - 1) {
                M.setRotors(input);
                while (settingscan.hasNext("\\(\\w+\\)")) {
                    allcycles += settingscan.next() + " ";
                }
            }
            if (settingscan.hasNext()) {
                throw new EnigmaException("Settings are wrong!");
            } else {
                M.setPlugboard(new Permutation(allcycles, _alphabet));
            }

        } else {
            throw new EnigmaException("Setting start is wrong!");
        }


    }

    /** Return true iff verbose option specified. */
    static boolean verbose() {
        return _verbose;
    }

    /** Print MSG in groups of five (except that the last group may
     *  have fewer letters). */
    private void printMessageLine(String msg) {
        String processed = "";
        for (int i = 0; i < msg.length(); i++) {
            processed += msg.charAt(i);
            if (processed.length() % 5 == 0) {
                if (i == msg.length() - 1) {
                    _output.print(processed);
                    _output.println();
                } else {
                    _output.print(processed + " ");
                }
                processed = "";
            } else if (i == msg.length() - 1) {
                _output.print(processed);
                _output.println();
            }
        }
    }

    /** Alphabet used in this machine. */
    private Alphabet _alphabet;

    /** Source of input messages. */
    private Scanner _input;

    /** Source of machine configuration. */
    private Scanner _config;

    /** File for encoded/decoded messages. */
    private PrintStream _output;

    /** True if --verbose specified. */
    private static boolean _verbose;

    /** Total number of rotors. **/
    private int totalrotors;

    /** Total number of pawls. **/
    private int totalpawls;

    /** All possible usable rotors. **/
    private ArrayList<Rotor> possiblerotors;

    /** Keeps track of the _config. **/
    private String inputtrack;
}
