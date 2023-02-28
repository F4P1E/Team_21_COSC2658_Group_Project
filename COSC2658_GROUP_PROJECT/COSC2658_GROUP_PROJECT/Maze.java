// IMPORTANT:
// You can set Robot "debug" to true to see robot's memory output

// IMPORTANT:
// Also update Robot's constructor
// According to the comment

class Robot {
    // These are shortcuts to make writing the code faster.
    // Example: Instead of writing "UP", just write U
    static final String U = "UP";
    static final String D = "DOWN";
    static final String L = "LEFT";
    static final String R = "RIGHT";
    static final String t = "true";
    static final String f = "false";
    static final char v = 'v';
    static final char w = '■';
    static final char d = 'd';
    // End shortcuts
    Maze maze; // Maze unique to this robot
    char[][] memory; // Array of positions [X][Y] that the robot has visited
    int X; // Current X position (in the robot's memory)
    int Y; // Current Y position (in the robot's memory)
    String prevDirection; // The direction the robot last tried to move to
    boolean isLastDirectionSuccessful; // If the last direction lead to an empty spot
    boolean isBacktracking; // If the robot is backtracking away from a dead-end
    Stack<String> allMoves; // Stack of all moves starting from origin
    boolean win; // Check if the robot has reached the goal
    static final String[] directionPriority = new String[]{U, L, D, R};
    // The robot will try to move in this priority (Up > Left > Down > Right) until it finds a new path or a goal
    boolean debug; // Show robot's memory

    // Constructor
    public Robot() {
        this.maze = new Maze();

        this.memory = new char[61][61];
        // At least the size of the map (ex: map 10x10 -> memory (20 + 1) x (20 + 1))
        // "+1" is so that the index will equal the current positions. Prevent array overflow
        this.X = 30; // Middle of the memory array
        this.Y = 30; // Middle of the memory array

        this.prevDirection = null;
        this.isLastDirectionSuccessful = false;
        this.isBacktracking = false;
        this.allMoves = new Stack<>();
        this.win = false;
        this.debug = true;

        memory[X][Y] = v; // Mark starting position as already visited
    }

    public void navigate() {

        // Move the robot until it win
        do {

            // If the robot is at a dead-end or backtracking
            if (isDeadEnd() || isBacktracking) {
                // And no new path is available
                if (findNewPath() == null) {
                    backtrack(); // Then backtrack
                } else {
                    // If a path is available
                    // Try going to that path
                    tryMove(findNewPath());
                }
                // Skip the rest of the loop
                continue;
            }

            // If not backtrack or not at dead-end
            // And last direction was successful
            // And next position is unvisited
            if (isLastDirectionSuccessful
                    && isNextPositionUnvisited(prevDirection)) {
                // Try moving to next position
                tryMove(prevDirection);
            } else {
                // If last direction was unsuccessful
                // For each possible direction (up, left, down, right)
                for (String direction : directionPriority) {
                    // If it's not the last failed position
                    // And next position is unvisited
                    if (!direction.equals(prevDirection)
                            && isNextPositionUnvisited(direction)) {
                        // Try moving to next position
                        tryMove(direction);
                        // Break direction testing loop
                        break;
                    }
                }
            }
        } while (!win);

        if (debug) showMemory();// Show the memory once won
    }

    boolean isNextPositionUnvisited(String direction) {
        return getNextPositionFromMemory(direction) != w
                && getNextPositionFromMemory(direction) != v;
    }

    // Try moving the robot in the specified direction
    // Could move successfully
    // Or hit a wall
    void tryMove(String direction) {
        String mazeResponse = maze.go(direction); // Ask Maze to move robot and get response

        switch (mazeResponse) {
            // If move is successful
            // Mark it in memory that it has been visited
            case t -> {
                updateMemoryVisited(direction); // Also update the current position in robot's memory
                isBacktracking = false; // Since a new path is found, the robot is no longer backtracking
            }
            // Most likely the robot has hit a wall, so update its memory to where the wall was
            case f -> updateMemoryWall(direction);
            // Set win and end
            case "win" -> win = true;
        }
    }

    // Update the robot memory so it knows
    // 1. New current position
    // 2. Mark the new current position as visited
    void updateMemoryVisited(String direction) {
        // Update current position in memory based on direction moved
        switch (direction) {
            case U -> Y--;
            case D -> Y++;
            case L -> X--;
            case R -> X++;
        }
        memory[X][Y] = v; // Marked new position as visited
        allMoves.push(direction); // Add direction to stack of all directions
        prevDirection = direction; // So robot can continue moving in this direction if successful
        isLastDirectionSuccessful = true; // So robot can continue moving in this direction if successful
    }

    // Update the robot memory so it knows
    // 1. It has NOT been moved
    // 2. Where there's a wall, so the robot can avoid moving into it again
    void updateMemoryWall(String direction) {
        switch (direction) {
            case U -> memory[X][Y - 1] = Robot.w;
            case D -> memory[X][Y + 1] = Robot.w;
            case L -> memory[X - 1][Y] = Robot.w;
            case R -> memory[X + 1][Y] = Robot.w;
            default -> throw new RuntimeException("Invalid direction");
        }
        isLastDirectionSuccessful = false; // Prevent the robot from keep trying a failed direction
    }

    // Return true if all adjacent position (in robot's memory) is not an empty position
    // (like a wall, visited, or dead-end)
    boolean isDeadEnd() {
        return getNextPositionFromMemory(U) != '\0'
                && getNextPositionFromMemory(L) != '\0'
                && getNextPositionFromMemory(D) != '\0'
                && getNextPositionFromMemory(R) != '\0';
    }

    // Based on list of allMoves, move the robot backward
    // Set it as "backtracking"
    // During "backtracking", the robot will try to find a new path by going back and try unvisited position
    void backtrack() {
        String oppositePrevMove = oppositeOf(allMoves.peek()); // Get opposite direction of the direction used to get here, so it can move backward
        isBacktracking = true; // Enable "backtracking"
        maze.go(oppositePrevMove); // Ask Maze to move robot back. Will always work since the robot has been there before
        updateMemoryDeadEnd(oppositePrevMove); // Mark current position as a dead-end
    }

    // Used during backtracking to find new unvisited positions
    // by testing and returning first available new position
    // from the robot's memory
    // Does not move robot
    String findNewPath() {
        for (String direction :
                directionPriority) {
            if (getNextPositionFromMemory(direction) == '\0') return direction;
        }
        return null;
    }

    // Used during backtracking
    // Update the robot's memory, so it knows
    // 1. Current position is a dead-end
    // 2. Remove most recent move (as it has already move backward)
    void updateMemoryDeadEnd(String direction) {
        memory[X][Y] = d;
        switch (direction) {
            case U -> Y--;
            case D -> Y++;
            case L -> X--;
            case R -> X++;
        }
        allMoves.pop();
    }

    // Check the robot's memory for what is in the next position based on current position
    char getNextPositionFromMemory(String direction) {
        switch (direction) {
            case U -> {
                return memory[X][Y - 1];
            }
            case D -> {
                return memory[X][Y + 1];
            }
            case L -> {
                return memory[X - 1][Y];
            }
            case R -> {
                return memory[X + 1][Y];
            }
            default -> throw new RuntimeException("Invalid direction");
        }
    }

    // Used during backtracking to get the opposite direction to move the robot to
    static String oppositeOf(String direction) {
        switch (direction) {
            case U -> {
                return D;
            }
            case L -> {
                return R;
            }
            case D -> {
                return U;
            }
            case R -> {
                return L;
            }
            default -> throw new RuntimeException("Invalid direction");
        }
    }

    // Display the robot's memory
    // Including its memory of the map
    // and steps that it has taken (backtracked steps are removed and not counted)
    void showMemory() {
        System.out.println("\n----- Robot memory -----\n");


        // Map
        System.out.println("Map");
        System.out.print("\t");
        for (int i = 0; i < memory.length; i++) {
            System.out.print(i + "\t");
        }

        System.out.println();

        for (int i = 0; i < memory.length; i++) {
            System.out.print(i + "\t");
            for (char[] chars : memory) {
                if (chars[i] != '\0') {
                    System.out.print(chars[i] + "\t");
                } else {
                    System.out.print("∙\t");
                }
            }
            System.out.println();
        }

        // allMoves
        System.out.println("\nRetrace to origin");
        allMoves.retraceAllMoves();
    }

    // Linked list implementation of Stack
// Taken from wk04 example code (ok to take from)
    static class Stack<T> {
        static class Node<T> {
            T data;
            Node<T> next;

            public Node(T data) {
                this.data = data;
                this.next = null;
            }

            @Override
            public String toString() {
                return data +
                        " <- " + next;
            }
        }

        private int size;
        private Node<T> head;

        public Stack() {
            size = 0;
            head = null;
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public void push(T item) {
            Node<T> node = new Node<>(item);
            if (!isEmpty()) {
                node.next = head;
            }
            head = node;
            size++;
        }

        public void pop() {
            if (isEmpty()) {
                return;
            }
            head = head.next;
            size--;
        }

        public T peek() {
            if (isEmpty()) {
                return null;
            }
            return head.data;
        }

        public void retraceAllMoves() {
            if (!isEmpty()) {
                System.out.println(head);
            }
        }
    }
}



// Maze class taken from example code
public class Maze {
    int rows;
    int cols;
    String[] map;
    int robotRow;
    int robotCol;
    int steps;
    boolean debug;

    // Maze input
    // Test maze here
    public Maze() {
        rows = 30;
        cols = 30;
        map = new String[rows];
        map[0]="..............................";
        map[1]=". ...    .              X .  .";
        map[2]=".    .           .          ..";
        map[3]=". .   .         .          . .";
        map[4]=".              .      .      .";
        map[5]=".. . .  .   .. ..  .   .     .";
        map[6]=".       .   . .. .     ..  . .";
        map[7]=".. ..    .    .           . ..";
        map[8]=".     . ..         . .       .";
        map[9]="..         .        .     .  .";
        map[10]=".              .  ..         .";
        map[11]=".            .          .    .";
        map[12]=".         .      .  . ..  .  .";
        map[13]=".        .        .          .";
        map[14]=".   .               .  ..    .";
        map[15]=".    . .       .  .  .      ..";
        map[16]=".      .     .        .      .";
        map[17]=".                         ....";
        map[18]=".   .              ..    . . .";
        map[19]="..      .   .                .";
        map[20]=".        ..   .    . . .     .";
        map[21]=".                            .";
        map[22]=". ..                         .";
        map[23]=".  .                  ...   ..";
        map[24]=".           .         .   .. .";
        map[25]="..      . .  .. ..         . .";
        map[26]=".  .       .              .. .";
        map[27]="..       ..           . . .  .";
        map[28]=".                            .";
        map[29]="..............................";



        robotRow = 8;
        robotCol = 5;
        steps = 0;

        // Change to "true" to see Maze's view of the robot (this is different to the robot's memory)
        debug = false;
    }

    public String go(String direction) {
        if (!direction.equals("UP") &&
                !direction.equals("DOWN") &&
                !direction.equals("LEFT") &&
                !direction.equals("RIGHT")) {
            // invalid direction
            steps++;
            return "false";
        }

        int currentRow = robotRow;
        int currentCol = robotCol;

        if (debug) {
            System.out.print(steps + "\t|\t");
            System.out.print(currentRow + ", " + currentCol);
        }

        if (direction.equals("UP")) {

            if (debug) {
                System.out.print("\t ↑");
            }

            currentRow--;
        } else if (direction.equals("DOWN")) {

            if (debug) {
                System.out.print("\t ↓");
            }

            currentRow++;
        } else if (direction.equals("LEFT")) {

            if (debug) {
                System.out.print("\t ←");
            }

            currentCol--;
        } else {

            if (debug) {
                System.out.print("\t →");
            }

            currentCol++;
        }

        // check the next position
        if (map[currentRow].charAt(currentCol) == 'X') {
            // Exit gate
            steps++;
            System.out.println("\nSteps to reach the Exit gate " + steps);
            return "win";
        } else if (map[currentRow].charAt(currentCol) == '.') {
            // Wall
            steps++;

            if (debug) {
                System.out.print("✕ Wall at ");
                System.out.println(currentRow + ", " + currentCol);
            }

            return "false";
        } else {
            // Space => update robot location
            steps++;
            robotRow = currentRow;
            robotCol = currentCol;

            if (debug) {
                System.out.println("\t" + currentRow + ", " + currentCol);
            }

            return "true";
        }
    }

    // Main program
    public static void main(String[] args) {
        (new Robot()).navigate();
    }
}
