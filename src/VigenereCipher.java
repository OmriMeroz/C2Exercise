// Basic Vigenere cipher implementation used for simple command-level encryption.
// Only alphabetic characters are shifted; all other characters remain unchanged.
// The keyword is repeated/expanded to match the length of the input command.

//based on vigenere-cipher - https://www.geeksforgeeks.org/dsa/vigenere-cipher/

public class VigenereCipher {

    private final String keyword;

    public VigenereCipher(String keyword) {
        this.keyword = keyword;
    }

    // Expands the keyword so that its length matches the command length.
    // This ensures a matching key character for each character in the command.
    public String expandPerCommand(String cmd) {
        if (keyword.length() >= cmd.length()) {
            return keyword;
        }

        StringBuilder sb = new StringBuilder(keyword);

        for (int i = 0; sb.length() < cmd.length(); i++) {
            sb.append(keyword.charAt(i % keyword.length()));
        }

        return sb.toString();
    }

    // Encrypts the input command using the Vigenere cipher rules.
    // Works separately for uppercase and lowercase letters.
    // Non-alphabet characters are copied as-is without modification.
    public String encrypt(String cmd) {
        String key = expandPerCommand(cmd);
        StringBuilder res = new StringBuilder();

        for (int i = 0; i < cmd.length(); i++) {
            char cmdChar = cmd.charAt(i);
            char keyChar = key.charAt(i);
            if (Character.isUpperCase(cmdChar)) {
                int x = (cmdChar - 'A' + (keyChar - 'A')) % 26;
                res.append((char) (x + 'A'));
            } else if (Character.isLowerCase(cmdChar)) {
                int x = (cmdChar - 'a' + (keyChar - 'a')) % 26;
                res.append((char) (x + 'a'));
            } else {
                res.append(cmdChar);
            }
        }

        return res.toString();

    }

    // Decrypts text previously encrypted with the same Vigenere key.
    // Performs the inverse shift operation for both upper/lowercase letters.
    public String decrypt(String cmd) {
        String key = expandPerCommand(cmd);
        StringBuilder res = new StringBuilder();

        for (int i = 0; i < cmd.length(); i++) {
            char cmdChar = cmd.charAt(i);
            char keyChar = key.charAt(i);
            if (Character.isUpperCase(cmdChar)) {
                int x = (cmdChar - 'A' - (keyChar - 'A') + 26) % 26;
                res.append((char) (x + 'A'));
            } else if (Character.isLowerCase(cmdChar)) {
                int x = (cmdChar - 'a' - (keyChar - 'a') + 26) % 26;
                res.append((char) (x + 'a'));
            } else {
                res.append(cmdChar);
            }
        }

        return res.toString();
    }

}
