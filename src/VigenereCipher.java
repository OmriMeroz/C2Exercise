//based on vigenere-cipher - https://www.geeksforgeeks.org/dsa/vigenere-cipher/

public class VigenereCipher {
    
    private final String keyword;

    public VigenereCipher(String keyword) {
        this.keyword = keyword.toUpperCase();
    }

    public String expandPerCommand(String cmd) {
        if (keyword.length() >= cmd.length()) {
            return keyword;
        }

        StringBuilder sb = new StringBuilder(keyword);

        for (int i = 0; sb.length() < cmd.length();i++) {
            sb.append(keyword.charAt(i%keyword.length()));
        }

        return sb.toString();
    }

    public String encrypt(String cmd) {
        cmd = cmd.toUpperCase();
        String key = expandPerCommand(cmd);
        StringBuilder res = new StringBuilder();

        for (int i =0;i<cmd.length();i++) {
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

    public String decrypt(String cmd) {
        cmd = cmd.toUpperCase();
        String key = expandPerCommand(cmd);
        StringBuilder res = new StringBuilder();

        for (int i =0;i<cmd.length();i++) {
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
