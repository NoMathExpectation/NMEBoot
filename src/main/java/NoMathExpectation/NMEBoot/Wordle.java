package NoMathExpectation.NMEBoot;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public final class Wordle {
    public static class CharCorrectivePair {
        enum Corrective {
            NONE("\uD83D\uDCD8"), WRONG("\uD83D\uDCD5"), WRONG_POS("\uD83D\uDCD9"), CORRECT("\uD83D\uDCD7");

            public final String icon;

            Corrective(String c) {
                icon = c;
            }

            @Override
            public String toString() {
                return icon;
            }
        }

        public final char c;
        public final Corrective corrective;

        CharCorrectivePair(char c, @NotNull Corrective corrective) {
            this.c = c;
            this.corrective = corrective;
        }
    }

    private static List<String> wordlist;
    private static int length;
    private int tries;
    private String word;
    private Set<Character> wordContainsChar;
    private int triesRemain;
    private boolean hard;
    private final Set<Character> noAppearLetters = new HashSet<>();
    private Map<Integer, Set<Character>> noPositionedAppearLetters = new HashMap<>();
    private char[] positionedAppearLetters;
    private boolean passed = false;
    private CharCorrectivePair[][] wordle;


    @Contract(pure = true)
    private static boolean wordValidate(@NotNull String word) {
        return word.matches("[a-zA-Z]+");
    }

    public static void setWordList(@NotNull File f) {
        if (!f.isFile()) {
            throw new IllegalArgumentException("未找到文件");
        }
        wordlist = new ArrayList<>();
        try (Scanner sc = new Scanner(f)) {
            if (!sc.hasNext()) {
                throw new IllegalArgumentException("文件是空的");
            }
            String first = sc.next();
            if (!wordValidate(first)) {
                throw new IllegalArgumentException("\"" + first + "\"不是一个单词");
            }
            length = first.length();
            wordlist.add(first);
            while (sc.hasNext()) {
                String s = sc.next();
                if (!wordValidate(s)) {
                    throw new IllegalArgumentException("\"" + s + "\"不是一个单词");
                }
                if (s.length() != length) {
                    throw new IllegalArgumentException("\"" + s + "\"单词长度不符");
                }
                wordlist.add(s);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("未找到文件", e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("文件格式不符", e);
        }
    }

    public void setTries(int tries) {
        if (!isEnded()) {
            throw new IllegalStateException("运行时不能改变尝试次数");
        }
        if (tries < 1) {
            throw new IllegalArgumentException("尝试次数不能小于1");
        }
        this.tries = tries;
    }

    public int getTries() {
        return tries;
    }

    public Wordle(int tries) {
        if (wordlist == null) {
            throw new IllegalStateException("词库未初始化");
        }
        this.tries = tries;
    }

    public void generate(boolean hard) {
        word = wordlist.get(new Random().nextInt(wordlist.size()));
        triesRemain = tries;
        this.hard = hard;

        noAppearLetters.clear();
        noPositionedAppearLetters.clear();
        positionedAppearLetters = new char[length];

        wordle = new CharCorrectivePair[tries][length];
        CharCorrectivePair[] row = new CharCorrectivePair[length];
        for (int i = 0; i < tries; i++) {
            wordle[i] = new CharCorrectivePair[length];
            for (int j = 0; j < length; j++) {
                wordle[i][j] = new CharCorrectivePair(' ', CharCorrectivePair.Corrective.NONE);
            }
        }

        wordContainsChar = new HashSet<>();
        for (int i = 0; i < word.length(); i++) {
            wordContainsChar.add(word.charAt(i));
        }

        passed = false;
    }

    @NotNull
    public String getAnswer() {
        return word;
    }

    @NotNull
    public String getRowsString() {
        StringBuilder block = new StringBuilder();
        for (CharCorrectivePair[] row : wordle) {
            StringBuilder icons = new StringBuilder();
            StringBuilder word = new StringBuilder();
            for (CharCorrectivePair ccp : row) {
                icons.append(ccp.corrective);
                word.append(ccp.c);
            }
            block.append(icons)
                    .append("  ")
                    .append(word)
                    .append("\n");
        }
        return block.toString();
    }

    @Contract(pure = true)
    public static boolean hasWord(@NotNull String word) {
        return wordlist.contains(word.toLowerCase(Locale.ROOT));
    }

    public synchronized boolean validate(@NotNull String wordToBeValidated) throws IllegalArgumentException {
        if (passed) {
            return true;
        }
        if (triesRemain <= 0) {
            return false;
        }

        wordToBeValidated = wordToBeValidated.toLowerCase(Locale.ROOT);
        if (wordToBeValidated.length() != length || !wordValidate(wordToBeValidated)) {
            throw new IllegalArgumentException("单词格式有误");
        }
        if (!hasWord(wordToBeValidated)) {
            throw new IllegalArgumentException("词库里没有此单词");
        }

        if (hard) {
            for (int i = 0; i < word.length(); i++) {
                char w = wordToBeValidated.charAt(i);
                if (positionedAppearLetters[i] != '\u0000' && positionedAppearLetters[i] != w) {
                    throw new IllegalArgumentException("未使用所有提示");
                }
                if (noAppearLetters.contains(w)) {
                    throw new IllegalArgumentException("未使用所有提示");
                }
                if (noPositionedAppearLetters.computeIfAbsent(i, x -> new HashSet<>()).contains(w)) {
                    throw new IllegalArgumentException("未使用所有提示");
                }
            }
        }

        int correctTimes = 0;
        for (int i = 0; i < word.length(); i++) {
            char w = wordToBeValidated.charAt(i);
            if (word.charAt(i) == w) {
                wordle[tries - triesRemain][i] = new CharCorrectivePair(w, CharCorrectivePair.Corrective.CORRECT);
                positionedAppearLetters[i] = w;
                correctTimes++;
            } else if (wordContainsChar.contains(w)) {
                wordle[tries - triesRemain][i] = new CharCorrectivePair(w, CharCorrectivePair.Corrective.WRONG_POS);
                noPositionedAppearLetters.computeIfAbsent(i, x -> new HashSet<>()).add(w);
            } else {
                wordle[tries - triesRemain][i] = new CharCorrectivePair(w, CharCorrectivePair.Corrective.WRONG);
                noAppearLetters.add(w);
            }
        }

        triesRemain--;
        passed = correctTimes == word.length();
        return correctTimes == word.length();
    }

    public boolean isHardMode() {
        return hard;
    }

    public int getTriesRemain() {
        return triesRemain;
    }

    public boolean isPassed() {
        return passed;
    }

    public void end() {
        triesRemain = 0;
    }

    public boolean isEnded() {
        return passed || triesRemain <= 0;
    }
}
