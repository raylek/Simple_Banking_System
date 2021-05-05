package banking;

import java.sql.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class BankWithDB {


    Scanner scanner = new Scanner(System.in);
    private boolean terminate = true;
    private String fileName;
    private Set<String>  allAccounts = new HashSet<>();
    static Connection connection  = null;


    public BankWithDB(String fileName) {
        this.fileName = fileName;
        try(Connection conn = connect()) {
            createNewTable();
            addAllNumbersInSet();
            mainMenu();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }

    private Connection connect() {
        // SQLite connection string
        String url = "jdbc:sqlite:" + this.fileName;

        if(connection == null) {
            try {
                connection = DriverManager.getConnection(url);
            } catch (SQLException e) {
//                System.out.println(e.getMessage());
            }
        } else {
            try {
                connection.close();
                connection = DriverManager.getConnection(url);
            } catch (SQLException e) {
//                System.out.println(e.getMessage());
            }
        }
        return connection;
    }

    private void createNewAccount() {
        String number = createAccNumber();
        String pin = createPIN();
        String sql = "INSERT INTO card(number,pin) VALUES(?,?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, number);
            pstmt.setString(2, pin);
            pstmt.executeUpdate();

            System.out.println("Your card has been created\n" +
                    "Your card number:\n" +
                    number + "\n" +
                    "Your card PIN:\n" +
                    pin + "\n");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createNewTable() {
        String sql = "CREATE TABLE IF NOT EXISTS card (\n"
                + "	id integer PRIMARY KEY,\n"
                + "	number text NOT NULL,\n"
                + "	pin text,"
                + " balance INTEGER DEFAULT 0\n"
                + ");";

        try (Statement stmt = connection.createStatement()) {

            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void addAllNumbersInSet() {
        String sql = "SELECT number "
                + "FROM card";

        try (PreparedStatement pstmt  = connection.prepareStatement(sql)){

            ResultSet rs  = pstmt.executeQuery();


            while(rs.next() ){
                allAccounts.add(rs.getString("number"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private int lastDigit(String str) {
        int[] arr = new int[str.length()];
        int sum = 0;
        for (int i = 0; i < str.length(); i++) {
            int preliminary = str.charAt(i) - '0';
            if(i%2 != 0) {
                preliminary *=2;
            }
            if(preliminary > 9) {
                preliminary -= 9;
            }
            arr[i] = preliminary;
        }
        for(int a : arr) {
            sum += a;
        }

        if(sum%10 == 0) {
            return 0;
        } else {
            return 10 - sum%10;
        }
    }

    private String createPIN() {
        Random random = new Random();
        String strPIN = "";
        for (int i = 0; i < 4; i++) {
            strPIN += random.nextInt(10);
        }
        return strPIN;
    }

    private String createAccNumber() {
        while (true) {
            Random random = new Random();
            String result = "400000";
            for (int i = 0; i < 9; i++) {
                result += random.nextInt(10);
            }
            result += lastDigit(result);
            if(!allAccounts.contains(result)) {
                allAccounts.add(result);
                return result;
            }
        }
    }

    private void mainMenu() {
        while(terminate) {
            System.out.println("1. Create an account\n" +
                    "2. Log into account \n" +
                    "0. Exit");

            int option = scanner.nextInt();

            if (option == 0) {
                terminate = false;
            } else if(option == 1) {
                createNewAccount();
            } else if(option == 2) {
                logInAccount();
            }
        }
    }

    private void logInAccount() {
        System.out.println("Enter your card number:");
        String number = scanner.next();
        System.out.println("Enter your PIN:");
        String pin = scanner.next();

        String sql = "SELECT pin, balance "
                + "FROM card WHERE number = ?";

        try (PreparedStatement pstmt  = connection.prepareStatement(sql)){

            pstmt.setString(1,number);
            ResultSet rs  = pstmt.executeQuery();

            if(rs.next() && rs.getString("pin").equals(pin)){
                System.out.println("You have successfully logged in!");
                loggedInAccountMenu(number);
            } else {
                System.out.println("Wrong card number or PIN!");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void loggedInAccountMenu(String number) {
        while (true) {
            System.out.println("1. Balance\n" +
                    "2. Add income\n" +
                    "3. Do transfer\n" +
                    "4. Close account\n" +
                    "5. Log out\n" +
                    "0. Exit");
            int option = scanner.nextInt();

            if (option == 0) {
                terminate = false;
                break;
            } else if(option == 1) {
                System.out.println("Balance: " + getBalance(number));
            } else if(option == 2) {
                addIncome(number);
            } else if(option == 3) {
                doTransfer(number);
            } else if(option == 4) {
                closeAccount(number);
                break;
            } else if(option == 5) {
                break;
            }

        }
    }

    private int getBalance(String number) {
        int balance = 0;


        String sql = "SELECT balance "
                + "FROM card WHERE number = ?";

        try (PreparedStatement pstmt  = connection.prepareStatement(sql)){

            pstmt.setString(1,number);
            ResultSet rs  = pstmt.executeQuery();

            while(rs.next() ){
                balance = rs.getInt("balance");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return balance;
    }

    private void addIncome(String number) {
        System.out.println("Enter income:");
        int balance = scanner.nextInt();
        balance += getBalance(number);



        String sql = "UPDATE card SET balance = ? WHERE number = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);) {
            pstmt.setInt(1, balance);
            pstmt.setString(2, number);
            pstmt.executeUpdate();
            System.out.println("Income was added!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void doTransfer(String number) {
        System.out.println("Transfer\n" +
                "Enter card number:");
        String cardNumber = scanner.next();
        if(!luhnCheck(cardNumber)) {
            System.out.println("Probably you made a mistake in the card number. Please try again!");
            return;
        } else if(!allAccounts.contains(cardNumber)){
            System.out.println("Such a card does not exist.");
            return;
        } else if(cardNumber.equals(number)) {
            System.out.println("You can't transfer money to the same account!");
        }
        System.out.println("Enter how much money you want to transfer:");
        int transferAmount = scanner.nextInt();
        if(transferAmount > getBalance(number)) {
            System.out.println("Not enough money!");
            return;
        }

        int balance = getBalance(cardNumber) + transferAmount;
        String sql = "UPDATE card SET balance = ? WHERE number = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);) {
            pstmt.setInt(1, balance);
            pstmt.setString(2, cardNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        balance = getBalance(number) - transferAmount;
        String sql2 = "UPDATE card SET balance = ? WHERE number = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql2);) {
            pstmt.setInt(1, balance);
            pstmt.setString(2, number);
            pstmt.executeUpdate();
            System.out.println("Success!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


    }

    private void closeAccount(String number) {
//        String url = "jdbc:sqlite:" + this.fileName;
        String sql = "DELETE FROM card WHERE number = ?";

        try (
//                Connection conn = connect();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            // set the corresponding param
            pstmt.setString(1, number);
            // execute the delete statement
            pstmt.executeUpdate();
            System.out.println("The account has been closed!");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean luhnCheck(String str) {


        int[] arr = new int[str.length()];
        int sum = 0;

        for (int i = 0; i < str.length(); i++) {
            int preliminary = str.charAt(i) - '0';
            if(i%2 == 0) {
                preliminary *=2;
            }
            if(preliminary > 9) {
                preliminary -= 9;
            }
            arr[i] = preliminary;
        }
        arr[str.length()-1] = str.charAt(str.length()-1) - '0';
        for(int b : arr) {
            sum += b;
//            System.out.println(sum);
        }


        return sum%10 == 0;
    }





}
