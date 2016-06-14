package org.wikipedia.miner.comparison;

import java.io.*;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;

public class ComparisonDataSet {

	ArrayList<Item> items ;


	public ComparisonDataSet() {
		items = new ArrayList<Item>() ;
	}

	public ComparisonDataSet(File file, int maxRelatedness)  throws IOException {
		items = new ArrayList<Item>() ;

		int articlePairs = 0 ;
		int termPairs = 0 ;

		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		
		String line ;
		while ((line=input.readLine())!= null) {

			if (line.startsWith("termA")) {
				//skip header
				continue ;
			}

			try {
				String[] values = line.split(",") ;

				String termA = values[0] ;

				Integer idA = null ;
				try {
					idA = new Integer(values[1]) ;
				} catch (Exception e) {} ;
				String termB = values[2] ;
				Integer idB = null ;
				try {
					idB = new Integer(values[3]) ;
				} catch (Exception e) {} ;

				Double relatedness = new Double(values[4]) ;

				//make between 0 and 1; its what we are used to.
				relatedness = relatedness/maxRelatedness ;

				if (idA > 0 && idB > 0)
					articlePairs ++ ;
				
				///if (!Character.isUpperCase(termA.charAt(0)))
				//	continue ;
				
				//if (!Character.isUpperCase(termB.charAt(0)))
				//	continue ;

				items.add(new Item(termA, idA, termB, idB, relatedness)) ;
				termPairs ++ ;


			} catch (Exception e) {
				Logger.getLogger(ComparisonDataSet.class).warn("Could not parse line \"" + line + "\"") ;
			}

		
		}
		
		input.close() ;
		
		System.out.println("Article pairs: " + articlePairs) ;
		System.out.println("Term pairs: " + termPairs) ;
	}
	
	public ComparisonDataSet[][] getFolds() {

		ComparisonDataSet[][] folds = new ComparisonDataSet[10][2] ;

		for (int i=0 ; i<10 ; i++) {
			folds[i][0] = new ComparisonDataSet() ;
			folds[i][1] = new ComparisonDataSet() ;
		}

		int index = 0 ;
		for (ComparisonDataSet.Item item:items) {

			for (int i=0 ; i < 10 ; i++) {
				if (index % 10 == i) 
					folds[i][1].addItem(item.getTermA(), item.getIdA(), item.getTermB(), item.getIdB(), item.getRelatedness()) ;
				else
					folds[i][0].addItem(item.getTermA(), item.getIdA(), item.getTermB(), item.getIdB(), item.getRelatedness()) ;
			}

			index++ ;
		}

		return folds ;

	}

	public int size() {
		return items.size();
	}

	public ArrayList<Item> getItems() {
		return items ;
	}

	public void addItem(String termA, Integer idA, String termB, Integer idB, double relatedness) {

		items.add(new Item(termA, idA, termB, idB, relatedness)) ;

	}
	
	public void save(File file) throws Exception {
		
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		
		//BufferedWriter out = new BufferedWriter(new FileWriter(file)) ;
		
		for (Item item:items) {
			
			String msg = item.termA + "," + item.idA + "," + item.termB + "," + item.idB + "," + item.relatedness + "\n" ;
			
			out.write(msg) ;
		}
		
		out.close();
	}


	public class Item {

		private String termA ;
		private int idA ;

		private String termB ;
		private int idB ;

		private double relatedness ;

		public Item(String termA, Integer idA, String termB, Integer idB, double relatedness) {
			this.termA = termA ;
			
			if (idA == null)
				this.idA = -1 ;
			else
				this.idA = idA ;
			
			this.termB = termB ;
			
			if (idB == null)
				this.idB = -1 ;
			else
				this.idB = idB ;
			
			this.relatedness = relatedness ;
		}

		public String getTermA() {
			return termA;
		}

		public int getIdA() {
			return idA;
		}

		public String getTermB() {
			return termB;
		}

		public int getIdB() {
			return idB;
		}

		public double getRelatedness() {
			return relatedness;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder() ;
			
			sb.append(termA) ;
			sb.append(",") ;
			sb.append(idA) ;
			sb.append(",") ;
			sb.append(termB) ;
			sb.append(",") ;
			sb.append(idB) ;
			sb.append(",") ;
			sb.append(relatedness) ;
			
			return sb.toString() ;
		}
	}
}
