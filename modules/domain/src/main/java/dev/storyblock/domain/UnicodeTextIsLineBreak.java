package dev.storyblock.domain;



final class UnicodeTextIsLineBreak {
    static boolean isLineBreak(String grapheme) {
        return grapheme.equals("\n") || grapheme.equals("\r") || grapheme.equals("\r\n");
    }
}
