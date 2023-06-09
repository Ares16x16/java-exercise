import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/*
 * This class creates a server of the game
 */
public class TicTacToeServer {

	 /*
     * this is the main method connect to the client and start the game
     */
    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(8901);
        System.out.println("Server is Running");
        try {
            while (true) {
                Game game = new Game();
                Game.Player playerX = game.new Player(listener.accept(), 'X');
                Game.Player playerO = game.new Player(listener.accept(), 'O');
                playerX.setOpponent(playerO);
                playerO.setOpponent(playerX);
                game.currentPlayer = playerX;
                playerX.start();
                playerO.start();
            }
        } finally {
            listener.close();
        }
    }
}

/*
 * This class is the game, handling the game mechanics 
 */
class Game {
    private Player[] board = {
        null, null, null,
        null, null, null,
        null, null, null};

    Player currentPlayer;


    /*
     * This method check whether winner appears
     */
    public boolean hasWinner() {
        return
            (board[0] != null && board[0] == board[1] && board[0] == board[2])
          ||(board[3] != null && board[3] == board[4] && board[3] == board[5])
          ||(board[6] != null && board[6] == board[7] && board[6] == board[8])
          ||(board[0] != null && board[0] == board[3] && board[0] == board[6])
          ||(board[1] != null && board[1] == board[4] && board[1] == board[7])
          ||(board[2] != null && board[2] == board[5] && board[2] == board[8])
          ||(board[0] != null && board[0] == board[4] && board[0] == board[8])
          ||(board[2] != null && board[2] == board[4] && board[2] == board[6]);
    }

    /*
     * This method check whether all squares are occupied
     */
    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                return false;
            }
        }
        return true;
    }
    
    /*
     * This method check whether the player's move is legal
     */
    public synchronized boolean legalMove(int location, Player player) {
        if (player == currentPlayer && board[location] == null) {
            board[location] = currentPlayer;
            currentPlayer = currentPlayer.opponent;
            currentPlayer.otherPlayerMoved(location);
            return true;
        }
        return false;
    }
    
    /*
     * This class sent commands to client
     */
    class Player extends Thread {
        char mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;
        
        /*
         * This method starts the game by sending signal to client
         */
        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Enter your player name");
            } catch (IOException e) {
                System.out.println("Player disconnected: " + e);
            }
        }
        
        /*
         * This method set the opponent player
         */
        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        /*
         * This method send whether the other player moved and check if game is ended
         */
        public void otherPlayerMoved(int location) {
            output.println("OPPONENT_MOVED " + location);
            if (hasWinner()) {
            	 output.println("DEFEAT");
            	 
            }else if (boardFilledUp()){
            	output.println("TIE");
            }else {
            	output.println("");
            }
        }
    
        /*
         * This method send signal to client
         */
        public void run() {
            try {
                if (mark == 'X') {
                    output.println("MESSAGE Your move");
                }

                while (true) {
                    String command = input.readLine();
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(5));
                        if (legalMove(location, this)) {
                            output.println("VALID_MOVE");
                            if (hasWinner()){
                            	output.println("VICTORY");
                            }else if(boardFilledUp()){
                            	output.println("TIE");
                            }else {
                            	output.println("");
                            }
                        } else {
                            output.println("MESSAGE Invalid move");
                        }

                    } else if (command.startsWith("QUIT")) {
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Player died: " + e);
                JFrame popUpMsg = new JFrame();
                JOptionPane.showMessageDialog(popUpMsg, "Game Ends. One of the player left.");
                output.println("CLOSE");
            } finally {
                try {socket.close();} catch (IOException e) {}
            }
        }
    }
}