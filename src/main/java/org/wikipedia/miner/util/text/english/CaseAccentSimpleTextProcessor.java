/*
 *    CaseAccentSimpleTextProcessor.java
 *    Copyright (C) 2013 Angel Conde Manjon, neuw84 at gmail dot com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.wikipedia.miner.util.text.english;

import java.text.Normalizer;
import java.util.regex.Pattern;
import org.wikipedia.miner.util.text.CaseFolder;
import org.wikipedia.miner.util.text.TextProcessor;

/**
 * Class that normalizes,processes case folding and processes plurals using 
 * PlingStemmer from http://mpii.de/yago-naga/javatools 
 * @author Angel Conde Manjon 
 */
    public class CaseAccentSimpleTextProcessor  extends  TextProcessor   {
  
    private final CaseFolder caseFolder=new CaseFolder();
    private final PlingStemmer stemmer= new PlingStemmer();
    private final Pattern pattern =Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    
    @Override
    public String processText(String string) {
        String normalizedText = Normalizer.normalize(string, Normalizer.Form.NFD);
        normalizedText=pattern.matcher(normalizedText).replaceAll("");
        normalizedText=caseFolder.processText(normalizedText);
        normalizedText=stemmer.stem(normalizedText);
        return normalizedText;
    }
//    public static void main(String[] args) throws IOException,Exception {
//        CaseAccentSimpleTextProcessor tex=new CaseAccentSimpleTextProcessor();
//        WikipediaConfiguration conf=new WikipediaConfiguration(new File("/home/angel/wikiminer/configs/wikipedia-en.xml"));
//        WEnvironment.prepareTextProcessor(tex, conf, new File("/tmp/"), true, 4);
//         
//    }   
    }
        