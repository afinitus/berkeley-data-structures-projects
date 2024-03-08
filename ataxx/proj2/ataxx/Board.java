/* Skeleton code copyright (C) 2008, 2022 Paul N. Hilfinger and the
 * Regents of the University of California.  Do not distribute this or any
 * derivative work without permission. */

package ataxx;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Formatter;

import java.util.function.Consumer;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/** An Ataxx board.   The squares are labeled by column (a char value between
 *  'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 *  + 2) or by linearized index, an integer described below.  Values of
 *  the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 *  two layers of border squares, which are always blocked.
 *  This artificial border (which is never actually printed) is a common
 *  trick that allows one to avoid testing for edge conditions.
 *  For example, to look at all the possible moves from a square, sq,
 *  on the normal board (i.e., not in the border region), one can simply
 *  look at all squares within two rows and columns of sq without worrying
 *  about going off the board. Since squares in the border region are
 *  blocked, the normal logic that prevents moving to a blocked square
 *  will apply.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Nishank Gite
 */
class Board {

    /** Number of squares on a side of the board. */
    static final int SIDE = Move.SIDE;

    /** Length of a side + an artificial 2-deep border region.
     * This is unrelated to a move that is an "extend". */
    static final int EXTENDED_SIDE = Move.EXTENDED_SIDE;

    /** Number of consecutive non-extending moves before game ends. */
    static final int JUMP_LIMIT = 25;

    /** Bottom left position. **/
    static final int BL = 24;

    /** Bottom right position. **/
    static final int BR = 30;

    /** Top left position. **/
    static final int TL = 90;

    /** Top right position. **/
    static final int TR = 96;

    /** Max board vertical jump length. **/
    static final int VERT = 66;

    /** Total board length. **/
    static final int TOTAL = 120;

    /** Open squares at start. **/
    static final int OPEN = 49;

    /** A new, cleared board in the initial configuration. */
    Board() {
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        for (int i = BL; i <= BR; i++) {
            for (int j = 0; j <= VERT; j += 11) {
                if (i + j == BL | i + j == BR
                        | i + j == TL | i + j == TR) {
                    continue;
                }
                set(i + j, EMPTY);
            }
        }
        set(BL, BLUE);
        set(BR, RED);
        set(TL, RED);
        set(TR, BLUE);
        for (int i = 0; i <= TOTAL; i++) {
            if (_board[i] == null) {
                set(i, BLOCKED);
            }
        }
        incrPieces(RED, 2);
        incrPieces(BLUE, 2);
        _allMoves = new ArrayList<Move>();
        _nummoves = 0;
        _numJumps = 0;
        _totalOpen = OPEN;
        _undoPieces = new Stack<PieceColor>();
        _undoSquares = new Stack<Integer>();
        setNotifier(NOP);
        clear();
    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        _board = board0._board.clone();
        _whoseMove = board0.whoseMove();
        incrPieces(RED, board0.numPieces(RED));
        incrPieces(BLUE, board0.numPieces(BLUE));
        _winner = board0.getWinner();
        _nummoves = board0.numMoves();
        _numJumps = board0.numJumps();
        _totalOpen = board0.totalOpen();
        _allMoves = (ArrayList<Move>) board0.allMoves();
        _undoPieces = new Stack<PieceColor>();
        _undoSquares = new Stack<Integer>();
        setNotifier(NOP);
    }

    /** Return the linearized index of square COL ROW. */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /** Return the linearized index of the square that is DC columns and DR
     *  rows away from the square with index SQ. */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions and no blocks. */
    void clear() {
        _whoseMove = RED;
        _nummoves = 0;
        _numJumps = 0;
        _totalOpen = OPEN;
        _winner = null;
        for (int i = BL; i <= BR; i++) {
            for (int j = 0; j <= VERT; j += 11) {
                set(i + j, EMPTY);
            }
        }
        set(BL, BLUE);
        set(BR, RED);
        set(TL, RED);
        set(TR, BLUE);
        int red = 2 - numPieces(RED);
        int blue = 2 - numPieces(BLUE);
        incrPieces(RED, red);
        incrPieces(BLUE, blue);
        _allMoves = new ArrayList<Move>();
        _undoPieces = new Stack<PieceColor>();
        _undoSquares = new Stack<Integer>();
        announce();
    }

    /** Return the winner, if there is one yet, and otherwise null.  Returns
     *  EMPTY in the case of a draw, which can happen as a result of there
     *  having been MAX_JUMPS consecutive jumps without intervening extends,
     *  or if neither player can move and both have the same number of pieces.*/
    PieceColor getWinner() {
        if (numPieces(RED) == 0 & numPieces(BLUE) > 0) {
            _winner = BLUE;
        } else if (numPieces(BLUE) == 0 & numPieces(RED) > 0) {
            _winner = RED;
        }
        if (!canMove(RED) & !canMove(BLUE)) {
            if (numPieces(RED) > numPieces(BLUE)) {
                _winner = RED;
            } else if (numPieces(BLUE) > numPieces(RED)) {
                _winner = BLUE;
            } else {
                _winner = EMPTY;
            }
        } else if (_numJumps >= JUMP_LIMIT) {
            if (numPieces(RED) > numPieces(BLUE)) {
                _winner = RED;
            } else if (numPieces(BLUE) > numPieces(RED)) {
                _winner = BLUE;
            } else {
                _winner = EMPTY;
            }
        }
        return _winner;
    }

    /** Return number of red pieces on the board. */
    int redPieces() {
        return numPieces(RED);
    }

    /** Return number of blue pieces on the board. */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /** Return number of COLOR pieces on the board. */
    int numPieces(PieceColor color) {
        return _numPieces[color.ordinal()];
    }

    /** Increment numPieces(COLOR) by K. */
    private void incrPieces(PieceColor color, int k) {
        _numPieces[color.ordinal()] += k;
    }

    /** The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     *  '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     *  BLOCKED.  Returns the same value as get(index(C, R)). */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /** Return the current contents of square with linearized index SQ. */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'g', and
     *  '1' <= R <= '7'. This operation is undoable. */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /** Set square with linearized index SQ to V.  This operation is
     *  undoable. */
    private void set(int sq, PieceColor v) {
        addUndo(sq);
        _board[sq] = v;
    }

    /** Set square at C R to V (not undoable). This is used for changing
     * contents of the board without updating the undo stacks. */
    private void unrecordedSet(char c, char r, PieceColor v) {
        _board[index(c, r)] = v;
    }

    /** Set square at linearized index SQ to V (not undoable). This is used
     * for changing contents of the board without updating the undo stacks. */
    private void unrecordedSet(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /** Return true iff MOVE is legal on the current board. */
    boolean legalMove(Move move) {
        if (move == null) {
            return false;
        }
        if (move.isPass() & !canMove(whoseMove())) {
            return true;
        }
        if (_board[move.fromIndex()] != whoseMove()) {
            return false;
        }
        if (_board[move.toIndex()] == EMPTY) {
            int difference = move.toIndex() - move.fromIndex();
            if (difference < 0) {
                difference *= -1;
            }
            for (int i  = 0; i < possiblemoves.length; i++) {
                if (possiblemoves[i] == difference) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return true iff C0 R0 - C1 R1 is legal on the current board. */
    boolean legalMove(char c0, char r0, char c1, char r1) {
        return legalMove(Move.move(c0, r0, c1, r1));
    }

    /** Return true iff player WHO can move, ignoring whether it is
     *  that player's move and whether the game is over. */
    boolean canMove(PieceColor who) {
        if (numPieces(who) == 0) {
            return false;
        }
        for (int i = BL; i <= BR; i++) {
            for (int j = 0; j <= VERT; j += 11) {
                if (_board[i + j] == who) {
                    for (int k = 0; k < possiblemoves.length; k++) {
                        if (_board[i + j + possiblemoves[k]] == EMPTY) {
                            return true;
                        }
                        if (_board[i + j - possiblemoves[k]] == EMPTY) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if the game is over. */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Return total number of moves and passes since the last
     *  clear or the creation of the board. */
    int numMoves() {
        return _nummoves;
    }

    /** Return number of non-pass moves made in the current game since the
     *  last extend move added a piece to the board (or since the
     *  start of the game). Used to detect end-of-game. */
    int numJumps() {
        return _numJumps;
    }

    /** Assuming MOVE has the format "-" or "C0R0-C1R1", make the denoted
     *  move ("-" means "pass"). */
    void makeMove(String move) {
        if (move.equals("-")) {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(move.charAt(0), move.charAt(1), move.charAt(3),
                               move.charAt(4)));
        }
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));
        }
    }

    /** Make the MOVE on this Board, assuming it is legal. */
    void makeMove(Move move) {
        if (!legalMove(move)) {
            throw error("Illegal move: %s", move);
        }
        if (move.isPass()) {
            pass();
            return;
        }
        _allMoves.add(move);
        startUndo();
        PieceColor opponent = _whoseMove.opposite();
        if (move.isJump()) {
            set(move.toIndex(), _whoseMove);
            set(move.fromIndex(), EMPTY);
            for (int i = move.toIndex() - 12;
                 i < move.toIndex() + 11; i += 11) {
                for (int j = 0; j < 3; j++) {
                    if (_board[i + j] == opponent) {
                        set(i + j, _whoseMove);
                        incrPieces(_whoseMove, 1);
                        incrPieces(opponent, -1);
                    }
                }
            }
            _nummoves += 1;
            _numJumps += 1;
        } else {
            set(move.toIndex(), _whoseMove);
            incrPieces(_whoseMove, 1);
            for (int i = move.toIndex() - 12;
                 i < move.toIndex() + 11; i += 11) {
                for (int j = 0; j < 3; j++) {
                    if (_board[i + j] == opponent) {
                        set(i + j, _whoseMove);
                        incrPieces(_whoseMove, 1);
                        incrPieces(opponent, -1);
                    }
                }
            }
            _nummoves += 1;
            _numJumps = 0;
        }
        _whoseMove = opponent;
        announce();
    }

    /** Update to indicate that the current player passes, assuming it
     *  is legal to do so. Passing is undoable. */
    void pass() {
        assert !canMove(_whoseMove);
        _allMoves.add(Move.pass());
        _nummoves += 1;
        _numJumps += 1;
        startUndo();
        _whoseMove = _whoseMove.opposite();
        announce();
    }

    /** Undo the last move. */
    void undo() {
        int temp;
        PieceColor color;
        while (_undoSquares.peek() != null) {
            temp = _undoSquares.pop();
            color = _undoPieces.pop();
            incrPieces(_board[temp], -1);
            incrPieces(color, 1);
            _board[temp] = color;
        }
        _undoSquares.pop();
        _undoPieces.pop();
        _nummoves -= 1;
        if (_allMoves.get(_allMoves.size() - 1).isJump()
                | _allMoves.get(_allMoves.size() - 1).isPass()) {
            _numJumps -= 1;
        }

        _whoseMove = _whoseMove.opposite();
        _allMoves.remove(_allMoves.size() - 1);
        _winner = null;
        announce();
    }

    /** Indicate beginning of a move in the undo stack. See the
     * _undoSquares and _undoPieces instance variable comments for
     * details on how the beginning of moves are marked. */
    private void startUndo() {
        _undoSquares.push(null);
        _undoPieces.push(null);
    }

    /** Add an undo action for changing SQ on current board. */
    private void addUndo(int sq) {
        if (_board[sq] == null) {
            return;
        }
        _undoSquares.push(sq);
        _undoPieces.push(_board[sq]);
    }

    /** Return true iff it is legal to place a block at C R. */
    boolean legalBlock(char c, char r) {
        for (int i  = 0; i < 7; i++) {
            if (columns[i] == c) {
                for (int j  = 0; j < 7; j++) {
                    if (_nummoves == 0 & rows[j] == r
                            & _board[BL + i + j * EXTENDED_SIDE] == EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Return true iff it is legal to place a block at CR. */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Set a block on the square C R and its reflections across the middle
     *  row and/or column, if that square is unoccupied and not
     *  in one of the corners. Has no effect if any of the squares is
     *  already occupied by a block.  It is an error to place a block on a
     *  piece. */
    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("illegal block placement");
        }
        int position0 = BL;
        int position1 = BL;
        int position2 = BL;
        int position3 = BL;
        int reflectcol = 0;
        int reflectrow = 0;
        for (int i = 0; i < 7; i++) {
            if (columns[i] == c) {
                position0 += i;
                position1 += i;
                reflectcol = 6 - i;
            }
            if (rows[i] == r) {
                position0 += EXTENDED_SIDE * i;
                position2 += EXTENDED_SIDE * i;
                reflectrow = 6 - i;
            }
        }
        position1 += EXTENDED_SIDE * reflectrow;
        position2 += reflectcol;
        position3 += EXTENDED_SIDE * reflectrow + reflectcol;
        set(position0, BLOCKED);
        set(position1, BLOCKED);
        set(position2, BLOCKED);
        set(position3, BLOCKED);
        _totalOpen -= 4;
        if (c == 'd' && r == '4') {
            _totalOpen += 3;
        } else if (c == 'd') {
            _totalOpen += 2;
        } else if (r == '4') {
            _totalOpen += 2;
        }
        if (!canMove(RED) && !canMove(BLUE)) {
            _winner = EMPTY;
        }
        announce();
    }

    /** Place a block at CR. */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Return total number of unblocked squares. */
    int totalOpen() {
        return _totalOpen;
    }

    /** Return a list of all moves made since the last clear (or start of
     *  game). */
    List<Move> allMoves() {
        return _allMoves;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        }
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board)
                && numPieces(RED) == other.redPieces()
                && numPieces(BLUE) == other.bluePieces()
                && _allMoves.equals(other.allMoves())
                && _whoseMove == other.whoseMove()
                && _nummoves == other.numMoves()
                && _numJumps == other.numJumps();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /** Return a text depiction of the board.  If LEGEND, supply row and
     *  column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        for (char r = '7'; r >= '1'; r -= 1) {
            if (legend) {
                out.format("%c", r);
            }
            out.format(" ");
            for (char c = 'a'; c <= 'g'; c += 1) {
                switch (get(c, r)) {
                case RED:
                    out.format(" r");
                    break;
                case BLUE:
                    out.format(" b");
                    break;
                case BLOCKED:
                    out.format(" X");
                    break;
                case EMPTY:
                    out.format(" -");
                    break;
                default:
                    break;
                }
            }
            out.format("%n");
        }
        if (legend) {
            out.format("   a b c d e f g");
        }
        return out.toString();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** Use _notifier.accept(this) to announce changes to this board. */
    private Consumer<Board> _notifier;

    /** For reasons of efficiency in copying the board,
     *  we use a 1D array to represent it, using the usual access
     *  algorithm: row r, column c => index(r, c).
     *
     *  Next, instead of using a 7x7 board, we use an 11x11 board in
     *  which the outer two rows and columns are blocks, and
     *  row 2, column 2 actually represents row 0, column 0
     *  of the real board.  As a result of this trick, there is no
     *  need to special-case being near the edge: we don't move
     *  off the edge because it looks blocked.
     *
     *  Using characters as indices, it follows that if 'a' <= c <= 'g'
     *  and '1' <= r <= '7', then row r, column c of the board corresponds
     *  to _board[(c -'a' + 2) + 11 (r - '1' + 2) ]. */
    private final PieceColor[] _board;

    /** Player that is next to move. */
    private PieceColor _whoseMove;

    /** Number of consecutive non-extending moves since the
     *  last clear or the beginning of the game. */
    private int _numJumps;

    /** Total number of unblocked squares. */
    private int _totalOpen;

    /** Number of blue and red pieces, indexed by the ordinal positions of
     *  enumerals BLUE and RED. */
    private int[] _numPieces = new int[BLUE.ordinal() + 1];

    /** Set to winner when game ends (EMPTY if tie).  Otherwise is null. */
    private PieceColor _winner;

    /** List of all (non-undone) moves since the last clear or beginning of
     *  the game. */
    private ArrayList<Move> _allMoves;

    /* The undo stack. We keep a stack of squares that have changed and
     * their previous contents.  Any given move may involve several such
     * changes, so we mark the start of the changes for each move (including
     * passes) with a null. */

    /** Stack of linearized indices of squares that have been modified and
     *  not undone. Nulls mark the beginnings of full moves. */
    private Stack<Integer> _undoSquares;
    /** Stack of pieces formally at corresponding squares in _UNDOSQUARES. */
    private Stack<PieceColor> _undoPieces;

    /** All the possible to and from distances in absolute value. **/
    private int[] possiblemoves = new int[]{1, 2, 9,
        10, 11, 12, 13, 20, 21, 22, 23, 24};

    /** All the column titles. **/
    private static char[] columns = new char[]{'a', 'b',
        'c', 'd', 'e', 'f', 'g'};

    /** All the row titles. **/
    private static char[] rows = new char[]{'1', '2', '3', '4', '5', '6', '7'};

    /** Total number of moves done since the start. **/
    private int _nummoves;
}
