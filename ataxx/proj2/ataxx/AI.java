/* Skeleton code copyright (C) 2008, 2022 Paul N. Hilfinger and the
 * Regents of the University of California.  Do not distribute this or any
 * derivative work without permission. */

package ataxx;

import java.util.Random;

import java.util.ArrayList;

import static ataxx.PieceColor.*;
import static java.lang.Math.min;
import static java.lang.Math.max;

/** A Player that computes its own moves.
 *  @author Nishank Gite
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 4;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. SEED is used to initialize
     *  a random-number generator for use in move computations.  Identical
     *  seeds produce identical behaviour. */
    AI(Game game, PieceColor myColor, long seed) {
        super(game, myColor);
        _random = new Random(seed);
    }

    @Override
    boolean isAuto() {
        return true;
    }

    @Override
    String getMove() {
        if (!getBoard().canMove(myColor())) {
            game().reportMove(Move.pass(), myColor());
            return "-";
        }
        Main.startTiming();
        Move move = findMove();
        Main.endTiming();
        game().reportMove(move, myColor());
        return move.toString();
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(getBoard());
        _lastFoundMove = null;
        if (myColor() == RED) {
            minMax(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            minMax(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** The move found by the last call to the findMove method
     *  above. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board board, int depth, boolean saveMove, int sense,
                       int alpha, int beta) {
        /* We use WINNING_VALUE + depth as the winning value so as to favor
         * wins that happen sooner rather than later (depth is larger the
         * fewer moves have been made. */
        if (depth == 0 || board.getWinner() != null) {
            return staticScore(board, WINNING_VALUE + depth);
        }
        Move best = null;
        int bestScore = 0;
        int max = alpha;
        int min = beta;
        ArrayList<Move> legalmoves = new ArrayList<>();
        for (char c = 'a'; c <= 'g'; c++) {
            for (char r = '1'; r <= '7'; r++) {
                if (board.get(c, r).equals(myColor())) {
                    for (int i = -2; i <= 2; i++) {
                        for (int j = -2; j <= 2; j++) {
                            Move possible = Move.move(c, r,
                                    (char) (c + i), (char) (r + j));
                            if (board.legalMove(possible)) {
                                legalmoves.add(possible);
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < legalmoves.size(); i++) {
            board.makeMove(legalmoves.get(i));
            int movevalue = minMax(board, depth - 1,
                    saveMove, -1 * sense, alpha, beta);
            if (movevalue > alpha & sense == 1) {
                best = legalmoves.get(i);
                max = movevalue;
            } else if (movevalue < beta & sense == -1) {
                best = legalmoves.get(i);
                min = movevalue;
            }
            if (max == WINNING_VALUE | min == -WINNING_VALUE) {
                break;
            }
            board.undo();
            if (max >= min) {
                break;
            }
        }
        if (sense == 1) {
            bestScore = max;
        } else if (sense == -1) {
            bestScore = min;
        }
        if (saveMove) {
            _lastFoundMove = best;
            if (best == null) {
                _lastFoundMove = Move.pass();
            }
        }
        return bestScore;
    }

    /** Return a heuristic value for BOARD.  This value is +- WINNINGVALUE in
     *  won positions, and 0 for ties. */
    private int staticScore(Board board, int winningValue) {
        PieceColor winner = board.getWinner();
        if (winner != null) {
            return switch (winner) {
            case RED -> winningValue;
            case BLUE -> -winningValue;
            default -> 0;
            };
        }
        int tracker = 0;
        for (int i = BL; i <= BR; i++) {
            for (int j = 0; j <= VERT; j += 11) {
                if (board.get(i + j) == RED) {
                    tracker += 1;
                } else if (board.get(i + j) == BLUE) {
                    tracker -= 1;
                }
            }
        }
        return tracker;
    }

    /** Pseudo-random number generator for move computation. */
    private Random _random = new Random();

    /** Bottom left position. **/
    static final int BL = 24;

    /** Bottom right position. **/
    static final int BR = 30;

    /** Max board vertical jump length. **/
    static final int VERT = 66;
}
