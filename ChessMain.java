import java.util.*;

// Abstract base class for all chess pieces
abstract class Piece {
    protected Color color;
    protected Position position;
    protected boolean hasMoved;

    public Piece(Color color, Position position) {
        this.color = color;
        this.position = position;
        this.hasMoved = false;
    }

    public abstract boolean isValidMove(Position from, Position to, Board board);
    public abstract String getSymbol();
    public Color getColor() { return color; }
    public Position getPosition() { return position; }
    public boolean hasMoved() { return hasMoved; }
    public void setPosition(Position position) { this.position = position; this.hasMoved = true; }

    protected boolean isPathClear(Position from, Position to, Board board) {
        int deltaRow = Integer.compare(to.getRow() - from.getRow(), 0);
        int deltaCol = Integer.compare(to.getCol() - from.getCol(), 0);
        int currentRow = from.getRow() + deltaRow;
        int currentCol = from.getCol() + deltaCol;
        while (currentRow != to.getRow() || currentCol != to.getCol()) {
            if (board.getPiece(new Position(currentRow, currentCol)) != null) return false;
            currentRow += deltaRow;
            currentCol += deltaCol;
        }
        return true;
    }
}

// Enum for piece colors
enum Color {
    WHITE, BLACK;
    public Color opposite() { return this == WHITE ? BLACK : WHITE; }
}

// Position class
class Position {
    private int row, col;
    public Position(int row, int col) { this.row = row; this.col = col; }
    public Position(String notation) {
        if (notation == null || notation.length() != 2) {
             this.row = -1; this.col = -1; return;
        }
        this.col = notation.charAt(0) - 'a';
        this.row = 8 - (notation.charAt(1) - '0');
    }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public boolean isValid() { return row >= 0 && row < 8 && col >= 0 && col < 8; }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position p = (Position)obj;
        return row == p.row && col == p.col;
    }
    @Override
    public String toString() { return "" + (char)('a' + col) + (8 - row); }
}

// Pawn
class Pawn extends Piece {
    public boolean justMovedTwo = false; // For en passant
    public Pawn(Color color, Position pos) { super(color, pos); }

    @Override
    public boolean isValidMove(Position from, Position to, Board board) {
        int dir = color == Color.WHITE ? -1 : 1;
        int rowDiff = to.getRow() - from.getRow();
        int colDiff = to.getCol() - from.getCol();

        // Normal move
        if (colDiff == 0) {
            if (rowDiff == dir && board.getPiece(to) == null) return true;
            if (!hasMoved && rowDiff == 2 * dir) {
                Position mid = new Position(from.getRow() + dir, from.getCol());
                return board.getPiece(to) == null && board.getPiece(mid) == null;
            }
        }
        // Capture
        if (Math.abs(colDiff) == 1 && rowDiff == dir) {
            Piece target = board.getPiece(to);
            if (target != null && target.getColor() != color) return true;
            // En passant
            Piece sidePawn = board.getPiece(new Position(from.getRow(), to.getCol()));
            if (sidePawn instanceof Pawn && sidePawn.getColor() != color && ((Pawn)sidePawn).justMovedTwo)
                return true;
        }
        return false;
    }

    public String getSymbol() {
        return color == Color.WHITE ? "P" : "p";
    }
}

// Rook
class Rook extends Piece {
    public Rook(Color color, Position pos) { super(color, pos); }
    @Override
    public boolean isValidMove(Position from, Position to, Board board) {
        if (from.getRow() != to.getRow() && from.getCol() != to.getCol()) return false;
        return isPathClear(from, to, board);
    }
    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "R" : "r";
    }
}

// Knight
class Knight extends Piece {
    public Knight(Color color, Position pos) { super(color, pos); }
    @Override
    public boolean isValidMove(Position from, Position to, Board board) {
        int r = Math.abs(to.getRow() - from.getRow());
        int c = Math.abs(to.getCol() - from.getCol());
        return (r == 2 && c == 1) || (r == 1 && c == 2);
    }
    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "N" : "n";
    }
}

// Bishop
class Bishop extends Piece {
    public Bishop(Color color, Position pos) { super(color, pos); }
    @Override
    public boolean isValidMove(Position from, Position to, Board board) {
        int r = Math.abs(to.getRow() - from.getRow());
        int c = Math.abs(to.getCol() - from.getCol());
        if (r != c) return false;
        return isPathClear(from, to, board);
    }
    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "B" : "b";
    }
}

// Queen
class Queen extends Piece {
    public Queen(Color color, Position pos) { super(color, pos); }
    @Override
    public boolean isValidMove(Position from, Position to, Board board) {
        int r = Math.abs(to.getRow() - from.getRow());
        int c = Math.abs(to.getCol() - from.getCol());
        boolean rookMove = from.getRow() == to.getRow() || from.getCol() == to.getCol();
        boolean bishopMove = r == c;
        if (!rookMove && !bishopMove) return false;
        return isPathClear(from, to, board);
    }
    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "Q" : "q";
    }
}

// King
class King extends Piece {
    public King(Color color, Position pos) { super(color, pos); }

    @Override
    public boolean isValidMove(Position from, Position to, Board board) {
        int rDiff = Math.abs(to.getRow() - from.getRow());
        int cDiff = Math.abs(to.getCol() - from.getCol());
        
        // Normal one-step move
        if (rDiff <= 1 && cDiff <= 1) {
            return true;
        }

        // Castling
        if (!hasMoved && rDiff == 0 && cDiff == 2) {
            int row = from.getRow();
            // Determine if it's kingside or queenside based on destination column
            int rookCol = (to.getCol() > from.getCol()) ? 7 : 0;
            Position rookPos = new Position(row, rookCol);
            Piece rook = board.getPiece(rookPos);

            if (rook instanceof Rook && !rook.hasMoved()) {
                // Check if the path between king and rook is clear
                int step = (rookCol > from.getCol()) ? 1 : -1;
                for (int i = from.getCol() + step; i != rookCol; i += step) {
                    if (board.getPiece(new Position(row, i)) != null) {
                        return false; // Path is blocked
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "K" : "k";
    }
}

// Board
class Board {
    private Piece[][] board = new Piece[8][8];
    private Move lastMove = null;

    // ANSI escape codes
    public static final String RESET = "\u001B[0m";
    public static final String WHITE_PIECE = "\u001B[97m";   // bright white
    public static final String BLACK_PIECE = "\u001B[33m";   // yellow
    public static final String BG_LIGHT = "\u001B[47m";      // light square
    public static final String BG_DARK = "\u001B[40m";       // dark square
    public static final String BG_HIGHLIGHT = "\u001B[42m";  // green
    public static final String BG_LAST_MOVE = "\u001B[44m";  // blue

    public Board() { initializeBoard(); }

    private void initializeBoard() {
        for (int col = 0; col < 8; col++) {
            board[1][col] = new Pawn(Color.BLACK, new Position(1, col));
            board[6][col] = new Pawn(Color.WHITE, new Position(6, col));
        }
        Piece[] blacks = {
            new Rook(Color.BLACK,new Position(0,0)), new Knight(Color.BLACK,new Position(0,1)),
            new Bishop(Color.BLACK,new Position(0,2)), new Queen(Color.BLACK,new Position(0,3)),
            new King(Color.BLACK,new Position(0,4)), new Bishop(Color.BLACK,new Position(0,5)),
            new Knight(Color.BLACK,new Position(0,6)), new Rook(Color.BLACK,new Position(0,7))
        };
        Piece[] whites = {
            new Rook(Color.WHITE,new Position(7,0)), new Knight(Color.WHITE,new Position(7,1)),
            new Bishop(Color.WHITE,new Position(7,2)), new Queen(Color.WHITE,new Position(7,3)),
            new King(Color.WHITE,new Position(7,4)), new Bishop(Color.WHITE,new Position(7,5)),
            new Knight(Color.WHITE,new Position(7,6)), new Rook(Color.WHITE,new Position(7,7))
        };
        for (int i=0;i<8;i++){ board[0][i]=blacks[i]; board[7][i]=whites[i]; }
    }

    public Piece getPiece(Position p){ return p.isValid()? board[p.getRow()][p.getCol()] : null; }
    public void setPiece(Position p, Piece piece){
        if(!p.isValid()) return;
        board[p.getRow()][p.getCol()] = piece;
        if(piece!=null) piece.setPosition(p);
    }
    public void removePiece(Position p){ if(p.isValid()) board[p.getRow()][p.getCol()]=null; }
    public void setLastMove(Move m){ lastMove = m; }

    // In Board.java

    public void displayBoard() {
        System.out.println("   a  b  c  d  e  f  g  h");
        System.out.println(" --------------------------");
        for (int r = 0; r < 8; r++) {
            System.out.print((8 - r) + " |");
            for (int c = 0; c < 8; c++) {
                Position currentPos = new Position(r, c);
                Piece p = board[r][c];

                // Choose background (checkerboard)
                boolean lightSquare = (r + c) % 2 == 0;
                String bg = lightSquare ? BG_LIGHT : BG_DARK;

                // Highlight last move
                if (lastMove != null && (currentPos.equals(lastMove.getFrom()) || currentPos.equals(lastMove.getTo()))) {
                    bg = BG_HIGHLIGHT;
                }

                // Piece color
                String pieceColor = (p == null) ? "" :
                    (p.getColor() == Color.WHITE ? WHITE_PIECE : BLACK_PIECE);

                String symbol = (p == null) ? "." : p.getSymbol();

                System.out.print(bg + pieceColor + " " + symbol + " " + RESET);
            }
            System.out.println("| " + (8 - r));
        }
        System.out.println(" --------------------------");
        System.out.println("   a  b  c  d  e  f  g  h");
    }


    public boolean isInCheck(Color color){
        Position kingPos = findKing(color);
        if(kingPos==null) return false;
        for(int r=0;r<8;r++) {
            for(int c=0;c<8;c++){
                Piece p = board[r][c];
                if(p!=null && p.getColor() != color) {
                    if (p.isValidMove(p.getPosition(), kingPos, this)) {
                         // Special check for pawn captures as isValidMove is different
                        if (p instanceof Pawn) {
                             if (Math.abs(p.getPosition().getCol() - kingPos.getCol()) == 1) {
                                return true;
                             }
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Position findKing(Color color){
        for(int r=0;r<8;r++)
            for(int c=0;c<8;c++){
                Piece p = board[r][c];
                if(p instanceof King && p.getColor()==color) return p.getPosition();
            }
        return null;
    }

    public List<Move> getAllLegalMoves(Color color){
        List<Move> moves = new ArrayList<>();
        for(int r=0;r<8;r++){
            for(int c=0;c<8;c++){
                Piece p = board[r][c];
                if(p!=null && p.getColor()==color){
                    Position from = p.getPosition();
                    for(int r2=0;r2<8;r2++){
                        for(int c2=0;c2<8;c2++){
                            Position to = new Position(r2,c2);
                            Piece captured = getPiece(to);
                            if (captured != null && captured.getColor() == color) continue;

                            if(p.isValidMove(from, to, this)){
                                boolean wasMoved = p.hasMoved();
                                setPiece(to,p); removePiece(from);
                                if(!isInCheck(color)) {
                                    moves.add(new Move(from,to));
                                }
                                // Undo move
                                setPiece(from,p); setPiece(to,captured);
                                p.hasMoved = wasMoved;
                            }
                        }
                    }
                }
            }
        }
        return moves;
    }
}

// Move
class Move {
    private Position from,to; private Piece captured;
    public Move(Position from,Position to){ this.from=from; this.to=to;}
    public Position getFrom(){return from;} public Position getTo(){return to;}
    public Piece getCapturedPiece(){return captured;}
    public void setCapturedPiece(Piece p){captured=p;}
    @Override public String toString(){ return from+" to "+to; }
}

// Player
class Player {
    private String name; private Color color;
    public Player(String name, Color color){ this.name=name; this.color=color;}
    public String getName(){return name;} public Color getColor(){return color;}
}

// ChessGame
class ChessGame {
    private Board board;
    private Player white, black, current;
    private List<Move> moveHistory;
    private boolean gameOver;

    public ChessGame(String whiteName,String blackName){
        board=new Board();
        white=new Player(whiteName,Color.WHITE);
        black=new Player(blackName,Color.BLACK);
        current=white; moveHistory=new ArrayList<>();
        gameOver=false;
    }

    public boolean makeMove(String fromNotation, String toNotation){
        if(gameOver){System.out.println("Game over!"); return false;}
        Position from=new Position(fromNotation);
        Position to=new Position(toNotation);
        if(!from.isValid()||!to.isValid()){System.out.println("Invalid position!"); return false;}
        Piece p=board.getPiece(from);
        if(p==null){System.out.println("No piece at "+fromNotation); return false;}
        if(p.getColor()!=current.getColor()){System.out.println("Not your piece!"); return false;}

        // Check for special castling rules before isValidMove for piece
        if (p instanceof King && Math.abs(from.getCol() - to.getCol()) == 2) {
            if (!handleCastlingValidation(p, from, to)) {
                return false;
            }
        }

        if(!p.isValidMove(from,to,board)){System.out.println("Invalid move for "+p.getClass().getSimpleName()); return false;}

        Piece captured = board.getPiece(to);
        boolean wasPieceMoved = p.hasMoved();
        boolean wasCapturedPieceMoved = (captured != null) && captured.hasMoved();

        // Handle special moves execution
        if(p instanceof Pawn && Math.abs(from.getCol()-to.getCol())==1 && captured==null){
            Position ep = new Position(from.getRow(), to.getCol());
            captured = board.getPiece(ep); board.removePiece(ep);
        } else if (p instanceof King && Math.abs(from.getCol()-to.getCol())==2){
            handleCastlingExecution(from, to);
        }

        // Make the move
        Move move = new Move(from,to);
        move.setCapturedPiece(captured);
        board.removePiece(from);
        board.setPiece(to,p);

        // Check if the move puts the king in check
        if(board.isInCheck(current.getColor())){
            System.out.println("Invalid move: You would be in check!");
            // Undo move
            board.setPiece(from,p);
            p.hasMoved = wasPieceMoved;
            board.setPiece(to,captured);
            if (captured != null) {
                captured.hasMoved = wasCapturedPieceMoved;
            }
            return false;
        }

        // Handle post-move logic if move is legal
        handlePawnPromotion(p, to);
        updateEnPassantStatus(p, from, to);

        moveHistory.add(move);
        board.setLastMove(move);
        switchPlayer();
        checkGameOver();
        return true;
    }

    private boolean handleCastlingValidation(Piece king, Position from, Position to) {
        if (board.isInCheck(current.getColor())) {
            System.out.println("Invalid move: Cannot castle while in check!");
            return false;
        }
        int step = (to.getCol() > from.getCol()) ? 1 : -1;
        for (int c = from.getCol() + step; c != to.getCol() + step; c += step) {
            Position pathPos = new Position(from.getRow(), c);
            Piece originalPiece = board.getPiece(pathPos); // Should be null
            board.removePiece(from);
            board.setPiece(pathPos, king);
            if (board.isInCheck(current.getColor())) {
                board.setPiece(from, king); board.setPiece(pathPos, originalPiece);
                System.out.println("Invalid move: Cannot castle through or into an attacked square!");
                return false;
            }
            board.setPiece(from, king); board.setPiece(pathPos, originalPiece);
        }
        return true;
    }

    private void handleCastlingExecution(Position from, Position to) {
        int row = from.getRow();
        if(to.getCol()==6){ // king side
            Piece rook = board.getPiece(new Position(row,7));
            board.removePiece(new Position(row,7));
            board.setPiece(new Position(row,5),rook);
        } else { // queen side
            Piece rook = board.getPiece(new Position(row,0));
            board.removePiece(new Position(row,0));
            board.setPiece(new Position(row,3),rook);
        }
    }
    
    private void handlePawnPromotion(Piece p, Position to) {
        if (p instanceof Pawn && (to.getRow() == 0 || to.getRow() == 7)) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Pawn promotion! Choose piece (Q, R, B, N): ");
            String choice = scanner.nextLine().trim().toUpperCase();

            Piece promoted;
            switch (choice) {
                case "R": 
                    promoted = new Rook(p.getColor(), to);
                    break;
                case "B": 
                    promoted = new Bishop(p.getColor(), to);
                    break;
                case "N": 
                    promoted = new Knight(p.getColor(), to);
                    break;
                default: // default to Queen if input invalid
                    promoted = new Queen(p.getColor(), to);
                    break;
            }
            board.setPiece(to, promoted);
            System.out.println("Pawn promoted to " + promoted.getClass().getSimpleName() + "!");
        }
    }


    private void updateEnPassantStatus(Piece p, Position from, Position to) {
        for(int r=0;r<8;r++)
            for(int c=0;c<8;c++) {
                Piece piece = board.getPiece(new Position(r,c));
                if(piece instanceof Pawn)
                    ((Pawn)piece).justMovedTwo=false;
            }
        if(p instanceof Pawn && Math.abs(from.getRow()-to.getRow())==2)
            ((Pawn)p).justMovedTwo=true;
    }

    private void switchPlayer(){ current = (current==white) ? black:white; }
    
    private void checkGameOver(){
        boolean inCheck=board.isInCheck(current.getColor());
        List<Move> legal=board.getAllLegalMoves(current.getColor());
        if(legal.isEmpty()){
            gameOver=true;
            if(inCheck) System.out.println("Checkmate! "+ current.getColor().opposite() +" wins!");
            else System.out.println("Stalemate! Draw!");
        } else if (inCheck) {
            System.out.println(current.getName() + " is in check!");
        }
    }

    public void displayBoard(){ board.displayBoard(); }
    public Player getCurrentPlayer(){ return current; }
    public boolean isGameOver(){ return gameOver; }
    public void displayMoveHistory(){
        System.out.println("\nMove History:");
        for(int i=0;i<moveHistory.size();i++) System.out.println((i+1)+". "+moveHistory.get(i));
    }

    public void showLegalMoves(String square){
        Position from=new Position(square);
        if (!from.isValid()) { System.out.println("Invalid square."); return; }
        Piece p=board.getPiece(from);
        if(p==null){System.out.println("No piece at "+square); return;}
        System.out.println("Legal moves for "+p.getClass().getSimpleName()+" at "+square+":");
        List<Move> allMoves = board.getAllLegalMoves(p.getColor());
        boolean found = false;
        for(Move m: allMoves) {
            if(m.getFrom().equals(from)) {
                System.out.print(m.getTo()+" ");
                found = true;
            }
        }
        if (!found) System.out.print("None");
        System.out.println();
    }
}

// Main
public class ChessMain {
    public static void main(String[] args){
        ChessGame game=new ChessGame("Player 1 (White)","Player 2 (Black)");
        Scanner scanner=new Scanner(System.in);
        System.out.println("Chess Game Started!");
        System.out.println("Enter moves like: e2 e4");
        System.out.println("Commands: 'quit', 'history', 'legal e2'");

        while(!game.isGameOver()){
            game.displayBoard();
            System.out.print("\n"+game.getCurrentPlayer().getName()+"'s turn: ");
            String input=scanner.nextLine().trim().toLowerCase();
            if(input.equals("quit")) break;
            else if(input.equals("history")){ game.displayMoveHistory(); continue; }
            else if(input.startsWith("legal ")){
                if (input.length() > 6) {
                    game.showLegalMoves(input.substring(6).trim());
                } else {
                    System.out.println("Please specify a square (e.g., 'legal e2').");
                }
                continue;
            }

            String[] parts=input.split(" ");
            if(parts.length==2) {
                if(!game.makeMove(parts[0],parts[1])) {
                    System.out.println("Move failed, please try again.");
                }
            }
            else System.out.println("Invalid input! Please use the format 'e2 e4'.");
        }
        scanner.close();
        System.out.println("Game Over!");
    }
}