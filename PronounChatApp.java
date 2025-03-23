import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.Random;

public class PronounChatApp {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JComboBox<String> pronounBox;
    private Random random = new Random();
    private boolean isDarkMode = false;
    private Connection connection;

    // Constructor
    public PronounChatApp() {
        connectToDatabase();
        createTables();

        frame = new JFrame("Pronoun-Friendly Chat");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Top Panel
        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(173, 216, 230));
        topPanel.add(new JLabel("Set Your Pronouns: "));

        String[] pronouns = {"he/him", "she/her", "they/them", "xe/xem", "ze/zir", "other"};
        pronounBox = new JComboBox<>(pronouns);
        topPanel.add(pronounBox);

        JButton savePronounButton = new JButton("💾 Save Pronoun");
        savePronounButton.addActionListener(e -> savePronoun());
        topPanel.add(savePronounButton);

        JButton themeToggle = new JButton("🌙");
        themeToggle.addActionListener(e -> toggleTheme());
        topPanel.add(themeToggle);

        frame.add(topPanel, BorderLayout.NORTH);

        // Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatArea.setBackground(Color.WHITE);
        chatArea.setForeground(Color.BLACK);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(240, 240, 240));

        messageField = new JTextField();
        messageField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        bottomPanel.add(messageField, BorderLayout.CENTER);

        JButton sendButton = new JButton("📩 Send");
        sendButton.addActionListener(e -> sendMessage());
        bottomPanel.add(sendButton, BorderLayout.EAST);

        // Clear Chat Button
        JButton clearChatButton = new JButton("🗑 Clear Chat");
        clearChatButton.addActionListener(e -> clearChat());
        bottomPanel.add(clearChatButton, BorderLayout.WEST);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        loadChatHistory();
        frame.setVisible(true);
    }

    // Database Connection
    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:chat.db");
            System.out.println("Connected to Database.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Create Tables
    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, pronoun TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS chat_history (id INTEGER PRIMARY KEY AUTOINCREMENT, message TEXT, sender TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Save User Pronoun
    private void savePronoun() {
        String pronoun = (String) pronounBox.getSelectedItem();
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO users (pronoun) VALUES (?)")) {
            stmt.setString(1, pronoun);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Pronoun saved!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Save Chat Message to Database and File
    private void saveChatMessage(String sender, String message) {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO chat_history (message, sender) VALUES (?, ?)")) {
            stmt.setString(1, message);
            stmt.setString(2, sender);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Also save to a file
        try (FileWriter writer = new FileWriter("chat_log.txt", true)) {
            writer.write(sender + ": " + message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load Chat History
    private void loadChatHistory() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sender, message FROM chat_history")) {
            while (rs.next()) {
                chatArea.append(rs.getString("sender") + ": " + rs.getString("message") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Clear Chat History
    private void clearChat() {
        chatArea.setText(""); // Clear the chat window

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM chat_history"); // Clear the database
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Clear the file log
        try (FileWriter writer = new FileWriter("chat_log.txt", false)) {
            writer.write(""); // Overwrite with an empty string
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Dark Mode Toggle
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        frame.getContentPane().setBackground(isDarkMode ? Color.DARK_GRAY : Color.WHITE);
        chatArea.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        chatArea.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
    }

    // Message Handling
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            chatArea.append("You: " + message + "\n");
            saveChatMessage("You", message);

            String botResponse = getBotResponse(message);
            chatArea.append("Bot: " + botResponse + "\n");
            saveChatMessage("Bot", botResponse);
            messageField.setText("");
        }
    }

    // Bot Responses
    private String getBotResponse(String message) {
        String lowerMessage = message.toLowerCase();
        return switch (lowerMessage) {
            case "hello", "hi" -> "Hey bestie! ✨";
            case "how are you?" -> "Living my best digital life! 💖";
            case "/joke" -> getJoke();
            case "/quote" -> getQuote();
            case "/fact" -> getFact();
            default -> "Love that! Tell me more. 💖";
        };
    }

    private String getJoke() {
        String[] jokes = {
                "Why did the pronoun break up with the verb? It needed some space!",
                "Non-binary people are like stars – they shine no matter what. 🌟",
                "Why did the LGBTQ+ AI go to therapy? Too many processing issues! 🤖"
        };
        return jokes[random.nextInt(jokes.length)];
    }

    private String getQuote() {
        String[] quotes = {
                "“Love yourself first and everything else falls into line.” - RuPaul",
                "“Visibility is important because it’s how change begins.” - Laverne Cox",
                "“Be proud of who you are, and not ashamed of how someone else sees you.” - Unknown"
        };
        return quotes[random.nextInt(quotes.length)];
    }

    private String getFact() {
        String[] facts = {
                "🏳️‍🌈 The first Pride was a riot – led by Black and Latinx trans women like Marsha P. Johnson!",
                "In 2022, over 30 countries recognized same-sex marriage. Progress! ❤️",
                "The term ‘non-binary’ has been used since at least the 1990s!"
        };
        return facts[random.nextInt(facts.length)];
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PronounChatApp::new);
    }
}

