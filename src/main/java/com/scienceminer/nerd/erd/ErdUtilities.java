package com.scienceminer.nerd.erd;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*; 

import org.apache.commons.lang3.StringUtils;
import com.scienceminer.nerd.exceptions.*;


/**
 * Some utilities methods for the ERD challenge.
 * 
 */
public class ErdUtilities {

	// here is the list of Wikipedia categories	considered for the snapshot		
	public static List<String> erdCategories = new ArrayList<String>(
    	Arrays.asList("/architecture/building","/architecture/skyscraper","/architecture/structure",
		"/architecture/house","/architecture/tower",
		"/architecture/venue","/aviation/aircraft_model","/award/award","/book/character","/book/poem_character",
		"/comic_books/character","/comic_strips/character","/cvg/game_character","/fictional_universe/fictional_character",
		"/automotive/model","/automotive/company","/commerce/brand","/comic_books/issue","/comic_books/series","/book/book",
		"/book/magazine","/book/periodical","/book/written_work","/broadcast/broadcast","/broadcast/radio_station",
		"/broadcast/tv_station","/business/consumer_product","/business/product_line","/business/brand",
		"/commerce/consumer_product","/computer/computer","/computer/operating_system","/computer/software",
		"/cvg/computer_videogame","/cvg/developer","/cvg/game_series","/education/university","/film/festival",
		"/film/film","/film/film_series","/internet/website","/internet/website_owner","/location/country",
		"/location/location","/music/musical_group","/organization/organization","/people/deceased_person",
		"/people/person","/sports/pro_athlete","/sports/sports_team","/sports/team","/time/holiday",
		"/time/recurring_event","/tv/network","/tv/tv_network","/tv/programgame_series","/tv/tv_program",
		"/tv/tv_series_season"));
		
	public static List<String> erdHighCategories = new ArrayList<String>(
    	Arrays.asList("/architecture","/aviation","/award","/book","/comic_books","/broadcast",
		"/business","/commerce","/computer","/cvg","/education","/film","/internet","/location",
		"/music","/organization","/people","/sports","/time","/tv"));	

	/**
	 *  Encode annotations in the ERD 2014 format for the short text track
	 */
	public static String encodeAnnotationsShort(List<ErdAnnotationShort> annotations) {
		StringBuilder sb = new StringBuilder();
		if (annotations == null) {
			return null;
		}
		for (ErdAnnotationShort a : annotations) {
			sb.append(a.toTsv()).append('\n');
		}
		return sb.toString();
	}

	/**
	 *  Encode annotations in the ERD 2014 format for the long text track
	 */
	public static String encodeAnnotationsLong(List<ErdAnnotationLong> annotations) {
		StringBuilder sb = new StringBuilder();
		if (annotations == null) {
			return null;
		}
		for (ErdAnnotationLong a : annotations) {
			sb.append(a.toTsv()).append('\n');
		}
		return sb.toString();
	}
}