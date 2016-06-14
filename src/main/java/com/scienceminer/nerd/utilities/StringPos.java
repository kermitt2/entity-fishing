package com.scienceminer.nerd.utilities;

/**
 * Store a string and a position index. 
 */

public class StringPos {
    public String string = null;
    public int pos = -1;

    public String toString() {
        return "" + string + "\t" + pos;
    }
}