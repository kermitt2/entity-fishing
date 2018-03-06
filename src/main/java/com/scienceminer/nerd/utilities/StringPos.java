package com.scienceminer.nerd.utilities;

import org.grobid.core.layout.LayoutToken;

import java.util.List;

/**
 * Store a string and a position index.
 */

public class StringPos implements Comparable<StringPos> {
    private String string = null;

    private int offsetStart = -1;

    public List<LayoutToken> layoutTokens = null;

    public StringPos() {

    }

    public StringPos(String string, int offsetStart) {
        this.string = string;
        this.offsetStart = offsetStart;
    }

    public StringPos(String string, int offsetStart, List<LayoutToken> layoutTokens) {
        this(string, offsetStart);
        setLayoutTokens(layoutTokens);
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public int getOffsetStart() {
        return offsetStart;
    }

    public void setOffsetStart(int offsetStart) {
        this.offsetStart = offsetStart;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    @Override
    public String toString() {
        return "'" + string + "'\t" + offsetStart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringPos)) return false;

        StringPos stringPos = (StringPos) o;

        if (getOffsetStart() != stringPos.getOffsetStart()) return false;
        return getString().equals(stringPos.getString());
    }

    @Override
    public int hashCode() {
        int result = getString() != null ? getString().hashCode() : 0;
        result = 31 * result + getOffsetStart();
        return result;
    }

    @Override
    public int compareTo(StringPos b) {
        return Integer.compare(getOffsetStart(), b.getOffsetStart());
    }
}