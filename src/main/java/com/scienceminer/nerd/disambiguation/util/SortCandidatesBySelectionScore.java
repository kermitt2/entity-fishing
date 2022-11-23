package com.scienceminer.nerd.disambiguation.util;

import java.util.*;
import com.scienceminer.nerd.disambiguation.NerdCandidate;

public class SortCandidatesBySelectionScore implements Comparator<NerdCandidate> {
    public int compare(NerdCandidate a, NerdCandidate b) {
        //descending order
        int val = ((int)((b.getSelectionScore() - a.getSelectionScore()) * 1000000));
        if (val == 0) {
            val = ((int)((b.getNerdScore() - a.getNerdScore()) * 1000000));
        }

        if (val == 0) {
            val = ((int)((b.getRelatednessScore() - a.getRelatednessScore()) * 1000000));
        }

        if (val == 0) {
            val = ((int)((b.getProb_c() - a.getProb_c()) * 1000000)); 
        }

        if (val == 0) {
            if (b.getWikipediaExternalRef() != -1)
                val = b.getWikipediaExternalRef() - a.getWikipediaExternalRef();
            else if (b.getWikidataId() != null) 
                val = b.getWikidataId().compareTo(a.getWikidataId());
        }

        return val;
    }


}