package com.scienceminer.nerd.disambiguation.util;

import java.util.*;
import com.scienceminer.nerd.disambiguation.NerdEntity;

public class SortEntitiesByNerdScore implements Comparator<NerdEntity> {
    public int compare(NerdEntity a, NerdEntity b) {
        // if we have offsets
        int bStart = b.getOffsetStart();
        int bEnd = b.getOffsetEnd();

        int aStart = a.getOffsetStart();
        int aEnd = a.getOffsetEnd();

        if ( (aStart == bStart) && (aEnd == bEnd) ) {
            Double bScore = new Double(b.getNerdScore());
            Double aScore = new Double(a.getNerdScore());

            if ((bScore != 0.0) && (aScore != 0.0) && (!bScore.equals(aScore)))
                return aScore.compareTo(bScore);
            else {
                aScore = new Double(a.getProb_c());
                bScore = new Double(b.getProb_c());
                if (aScore != bScore)
                    return aScore.compareTo(bScore);
                else 
                    return a.getSource().getName().compareTo(b.getSource().getName());
            } 
        } else if (aStart != bStart) {
            return aStart - bStart;
        } else if (aEnd != bEnd) {
            return aEnd - bEnd;
        } else {
            return 0;
        }
    }
}

    