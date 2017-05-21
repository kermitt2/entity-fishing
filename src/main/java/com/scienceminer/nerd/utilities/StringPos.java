package com.scienceminer.nerd.utilities;

import java.util.Comparator;

/**
 * Store a string and a position index. 
 */

public class StringPos  implements Comparable<StringPos> {
    public String string = null;
    public int pos = -1;

    public String toString() {
        return "" + string + "\t" + pos;
    }

    @Override
    public int compareTo(StringPos b) {
        return pos < b.pos ? -1 : pos == b.pos ? 0 : 1;
    }
}