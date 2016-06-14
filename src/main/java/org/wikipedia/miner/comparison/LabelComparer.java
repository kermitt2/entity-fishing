package org.wikipedia.miner.comparison;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import org.wikipedia.miner.model.Label;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.CorrelationCalculator;
import org.wikipedia.miner.util.ProgressTracker;

import org.dmilne.weka.wrapper.*;

import weka.classifiers.Classifier;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.meta.Bagging;
import weka.core.Instance;
import weka.core.Utils;

public class LabelComparer {

	private Wikipedia wikipedia ;
	private ArticleComparer articleComparer ;
	
	private enum SenseAttr {
		predictedRelatedness, distanceFromBenchmarkRelatedness, distanceFromTopRelatedness, distanceFromTopPriorProbability, avgPriorProbability, maxPriorProbability, minPriorProbability, avgGenerality, maxGenerality, minGenerality
	}
	
	private enum RelatednessAttr {
		bestSenseRelatedness, maxSenseRelatedness, avgSenseRelatedness, weightedAvgSenseRelatedness, concatenationPriorLinkProbability, concatenationOccurances
	}
	
	private Decider<SenseAttr,Boolean> senseSelector ;
	private Dataset<SenseAttr,Boolean> senseDataset ;
	
	private Decider<RelatednessAttr, Double> relatednessMeasurer ;
	private Dataset<RelatednessAttr,Double> relatednessDataset ;
	
	DecimalFormat df = new DecimalFormat("0.00") ;
	
	public LabelComparer(Wikipedia wikipedia, ArticleComparer articleComparer) throws Exception {
		this.wikipedia = wikipedia ;
		this.articleComparer = articleComparer ;
		
		senseSelector = (Decider<SenseAttr, Boolean>) new DeciderBuilder<SenseAttr>("labelSenseSelector", SenseAttr.class)
			.setDefaultAttributeTypeNumeric()
			.setClassAttributeTypeBoolean("isValid")
			.build();
		
		relatednessMeasurer = (Decider<RelatednessAttr, Double>) new DeciderBuilder<RelatednessAttr>("labelRelatednessMeasurer", RelatednessAttr.class)
			.setDefaultAttributeTypeNumeric()
			.setClassAttributeTypeNumeric("relatedness")
			.build();
		
		if (wikipedia.getConfig().getLabelDisambiguationModel() != null) {
			loadDisambiguationClassifier(wikipedia.getConfig().getLabelDisambiguationModel()) ;
		}
		
		if (wikipedia.getConfig().getLabelComparisonModel() != null)
			loadComparisonClassifier(wikipedia.getConfig().getLabelComparisonModel()) ;
	}
	
	public ComparisonDetails compare(Label labelA, Label labelB) throws Exception {

		if (!senseSelector.isReady())
			throw new Exception("You must train+build a new label sense selection classifier or load an existing one first") ;
		
		if (!relatednessMeasurer.isReady())
			throw new Exception("You must train+build a new label relatedness measuring classifier or load and existing one first") ;

		
		return new ComparisonDetails(labelA, labelB) ;
	}
	
	/**
	 * A convenience function to compare labels without returning details. 
	 * 
	 * @param labelA
	 * @param labelB
	 * @return the semantic relatedness between the two labels
	 * @throws Exception
	 */
	public Double getRelatedness(Label labelA, Label labelB) throws Exception {
		
		ComparisonDetails cmp = compare(labelA, labelB) ;
		return cmp.getLabelRelatedness() ;
	}
	
	
	public void train(ComparisonDataSet dataset, String datasetName) throws Exception {

		senseDataset = senseSelector.createNewDataset() ;
		relatednessDataset = relatednessMeasurer.createNewDataset() ;
		
		ProgressTracker pn = new ProgressTracker(dataset.getItems().size(), "training", LabelComparer.class) ;
		for (ComparisonDataSet.Item item: dataset.getItems()) {

			train(item) ;
			pn.update() ;
		}
		
		//TODO: filter to resolve skewness?
	}
	
	public void saveDisambiguationTrainingData(File file) throws IOException, Exception {
		senseDataset.save(file) ;
	}
	
	public void saveComparisonTrainingData(File file) throws IOException, Exception {
		relatednessDataset.save(file) ;
	}
	
	
	
	public double testRelatednessPrediction(ComparisonDataSet dataset) throws Exception {

		ArrayList<Double> manualMeasures = new ArrayList<Double>() ;
		ArrayList<Double> autoMeasures = new ArrayList<Double>() ;

		ProgressTracker pt = new ProgressTracker(dataset.getItems().size(), "testing relatedness prediction", LabelComparer.class) ;
		for (ComparisonDataSet.Item item: dataset.getItems()) {

			Label labelA = new Label(wikipedia.getEnvironment(), item.getTermA()) ;
			Label labelB = new Label(wikipedia.getEnvironment(), item.getTermB()) ;
			
			if (!labelA.exists())
				continue ;
			
			if (!labelB.exists())
				continue ;

			Double manual = item.getRelatedness() ;
			Double auto  = 	getRelatedness(labelA, labelB) ;

			if (auto != null) {
				manualMeasures.add(manual) ;
				autoMeasures.add(auto) ;
			}
			
			pt.update()  ;
		}

		return CorrelationCalculator.getCorrelation(manualMeasures, autoMeasures) ;
	}
	
	public Double testDisambiguationAccuracy(ComparisonDataSet dataset) throws Exception {
		
		int totalInterpretations = 0 ;
		int correctInterpretations = 0 ;
		
		ProgressTracker pt = new ProgressTracker(dataset.getItems().size(), "testing disambiguation accuracy", LabelComparer.class) ;
		for (ComparisonDataSet.Item item: dataset.getItems()) {
		
			if (item.getIdA() < 0 || item.getIdB() < 0)
				continue ;
			
			totalInterpretations++ ;
			
			Label labelA = new Label(wikipedia.getEnvironment(), item.getTermA()) ;
			Label labelB = new Label(wikipedia.getEnvironment(), item.getTermB()) ;
			
			
			ComparisonDetails details = this.compare(labelA, labelB) ;
			
			SensePair sp = details.getBestInterpretation() ;
			
			if (sp != null) {
				if (sp.getSenseA().getId() == item.getIdA() && sp.getSenseB().getId() == item.getIdB())
					correctInterpretations ++ ;
			}
			pt.update();
		}
		
		if (totalInterpretations > 0)
			return (double) correctInterpretations/totalInterpretations ;
		else
			return null ;
		
	}
	
	
	public void loadDisambiguationClassifier(File file) throws Exception {
		senseSelector.load(file) ;
	}
	
	public void loadComparisonClassifier(File file) throws Exception {
		relatednessMeasurer.load(file) ;
	}
	
	public void saveDisambiguationClassifier(File file) throws Exception {
		senseSelector.save(file) ;
	}
	
	public void saveComparisonClassifier(File file) throws Exception {
		relatednessMeasurer.save(file) ;
	}
	
	//TODO: saving and loading arff files?
	
	public void buildDefaultClassifiers() throws Exception {
		
		Classifier ssClassifier = new Bagging() ;
		ssClassifier.setOptions(Utils.splitOptions("-P 10 -S 1 -I 10 -W weka.classifiers.trees.J48 -- -U -M 2")) ;
		senseSelector.train(ssClassifier, senseDataset) ;
		
		Classifier rmClassifier = new GaussianProcesses() ;
		relatednessMeasurer.train(rmClassifier, relatednessDataset) ;
	}
	
	
	
	
	private void train(ComparisonDataSet.Item item) throws Exception {
		
		Label labelA = new Label(wikipedia.getEnvironment(), item.getTermA()) ;
		Label labelB = new Label(wikipedia.getEnvironment(), item.getTermB()) ;
		
		
		if (!labelA.exists())
			return ;
		
		if (!labelB.exists())
			return ;
		
		new ComparisonDetails(labelA, labelB, item.getIdA(), item.getIdB(), item.getRelatedness()) ;
	}
	
			
	public class ComparisonDetails {
		
		private Label labelA ;
		private Label labelB ;
		private Label concatenation ;
		
		private Double labelRelatedness ;
		private ArrayList<SensePair> candidateInterpretations = new ArrayList<SensePair>() ;
		
		private double maxSpRelatedness ;
		private double avgSpRelatedness ;
		private double weightedAvgSpRelatedness ;
		
		public Label getLabelA() {
			return labelA;
		}

		public Label getLabelB() {
			return labelB;
		}

		public Double getLabelRelatedness() {
			return labelRelatedness;
		}

		public ArrayList<SensePair> getCandidateInterpretations() {
			return candidateInterpretations;
		}
		
		public SensePair getBestInterpretation() {
			
			if (candidateInterpretations.size() > 0)
				return candidateInterpretations.get(0) ;
			else
				return null ;
		}
		
		/**
		 * Constructs details for item where correct disambiguation and relatedness are already known (training)
		 * 
		 * @param labelA
		 * @param labelB
		 * @throws Exception 
		 * @throws Exception 
		 */
		private ComparisonDetails(Label labelA, Label labelB, int artA, int artB, double relatedness) throws Exception {
			
			init(labelA, labelB, artA, artB, relatedness) ;
		}
		
		private ComparisonDetails(Label labelA, Label labelB) throws Exception {
			
			init(labelA, labelB, null, null, null) ;
			
		}
	
	
		private void init(Label labelA, Label labelB, Integer senseIdA, Integer senseIdB, Double relatedness) throws Exception {
			
			this.labelA = labelA ;
			this.labelB = labelB ;
			concatenation = new Label(wikipedia.getEnvironment(), labelA.getText() + " " + labelB.getText()) ;
			
			double benchmarkRelatedness = 0 ;
			double spacer = 0.5 ;
			
			double topPriorProbability = 0 ;
			double topRelatedness = 0 ;
			
			for (Label.Sense senseA:labelA.getSenses()) {
				
				if (senseA.getPriorProbability() < wikipedia.getConfig().getMinSenseProbability())
					break ;
				
				for (Label.Sense senseB:labelB.getSenses()) {
					
					if (senseB.getPriorProbability() < wikipedia.getConfig().getMinSenseProbability())
						break ;
					
					SensePair sp = new SensePair(senseA, senseB) ;
					
					if (sp.getSenseRelatedness() > benchmarkRelatedness + (benchmarkRelatedness*spacer)) {
						//this sets a new benchmark
						benchmarkRelatedness = sp.getSenseRelatedness() ;
						candidateInterpretations.clear();
						candidateInterpretations.add(sp); 
						topPriorProbability = sp.avgPriorProbability ;
						topRelatedness = sp.senseRelatedness ;
						
					} else if (sp.getSenseRelatedness() > benchmarkRelatedness - (benchmarkRelatedness*spacer)) {
						//this is close enough to benchmark to be considered
						candidateInterpretations.add(sp);
						
						if (sp.avgPriorProbability > topPriorProbability) 
							topPriorProbability = sp.avgPriorProbability ;
						
						if (sp.senseRelatedness > topRelatedness)
							topRelatedness = sp.senseRelatedness ;
					}
				}
			}
			
			double totalSpRelatedness = 0 ;
			double totalWeightedSpRelatedness = 0 ;
			double totalWeight = 0 ;
			int spCount = 0 ;
			
			for (SensePair sp:candidateInterpretations) {
				
				
				sp.setDistanceFromBenchmarkRelatedness(benchmarkRelatedness-sp.getSenseRelatedness()) ;
				sp.setDistanceFromTopRelatedness(topRelatedness-sp.getSenseRelatedness()) ;
				sp.setDistanceFromTopPriorProbability(topPriorProbability-sp.avgPriorProbability) ;
				
				if (senseIdA != null && senseIdB != null) {
					//this is a training instance, where correct senses are known
					if (senseIdA > 0 && senseIdB >0) {
						if (sp.getSenseA().getId() == senseIdA && sp.getSenseB().getId() == senseIdB) {
							sp.setIsValid(true) ;
						} else {
							sp.setIsValid(false) ;
						}
						
						Instance i = sp.getInstance() ;
						
						//weighting training instances
						// - training instances for terms that aren't closely related to each other don't matter much
						// - because it doesn't really matter how they are interpreted
						double weight =  sp.getSenseRelatedness() ;
						
						
						// - negative instances that are close to the relatedness of the correct interpretation shouldn't matter much either
						if (!sp.isValid) {
							double distToActualRelatedness = Math.abs(relatedness-sp.getSenseRelatedness()) ;
							weight = (weight + distToActualRelatedness) / 2 ;
						}
						
						i.setWeight(weight) ;
						senseDataset.add(i) ;
					}
					
				} else {
					//correct senses must be predicted
					sp.predictIsValid() ;
				}
		
				if (sp.getSenseRelatedness() > maxSpRelatedness)
					maxSpRelatedness = sp.getSenseRelatedness() ;
				
				//System.out.println(" - " + sp) ;				
				
				totalSpRelatedness += sp.getSenseRelatedness() ;
				totalWeightedSpRelatedness += (sp.avgPriorProbability * sp.getSenseRelatedness()) ;
				totalWeight += sp.avgPriorProbability ;
				spCount++ ;
			}
						
			Collections.sort(candidateInterpretations) ;	
			
			if (spCount > 0) {
				avgSpRelatedness = totalSpRelatedness/spCount ;
				weightedAvgSpRelatedness = totalWeightedSpRelatedness/totalWeight ;
			} else {
				avgSpRelatedness = 0 ;
				weightedAvgSpRelatedness = 0 ;
			}
			
			if (relatedness != null) {
				//this is a training instance, where relatedness is known
				labelRelatedness = relatedness ;
				relatednessDataset.add(getInstance()) ;
			} else {
				//relatedness must be predicted
				labelRelatedness = relatednessMeasurer.getDecision(getInstance()) ;
			}
			
			//System.out.println(this) ;
			
		}
		
		@Override
		public String toString() {
			
			try {
				Instance i = getInstance() ;
				
				StringBuffer sb = new StringBuffer() ;
				sb.append(labelA.getText()) ;
				sb.append(" vs. ") ;
				sb.append(labelB.getText()) ;
				
				sb.append(" br: " + df.format(i.value(RelatednessAttr.bestSenseRelatedness.ordinal()))) ;
				sb.append(" wr: " + df.format(i.value(RelatednessAttr.weightedAvgSenseRelatedness.ordinal()))) ;
				
				sb.append(" cpp: " + df.format(i.value(RelatednessAttr.concatenationPriorLinkProbability.ordinal()))) ;
				
				return sb.toString() ;
			} catch (Exception e)  {
				return e.getMessage() ;
			}
			
			
		}
		
		
		protected Instance getInstance() throws ClassMissingException, AttributeMissingException {
			
			InstanceBuilder<RelatednessAttr,Double> ib = relatednessMeasurer.getInstanceBuilder() ;
			
			if (candidateInterpretations.size() > 0)
				ib.setAttribute(RelatednessAttr.bestSenseRelatedness, candidateInterpretations.get(0).getSenseRelatedness()) ;
			else
				ib.setAttribute(RelatednessAttr.bestSenseRelatedness, Instance.missingValue()) ;
			
			ib.setAttribute(RelatednessAttr.maxSenseRelatedness, maxSpRelatedness) ;
			ib.setAttribute(RelatednessAttr.avgSenseRelatedness, avgSpRelatedness) ;
			ib.setAttribute(RelatednessAttr.weightedAvgSenseRelatedness, weightedAvgSpRelatedness) ;
			ib.setAttribute(RelatednessAttr.concatenationPriorLinkProbability, concatenation.getLinkProbability()) ;
			ib.setAttribute(RelatednessAttr.concatenationOccurances, Math.log(concatenation.getOccCount()+1)) ;
			
			if (labelRelatedness != null)
				ib.setClassAttribute(labelRelatedness) ;
			
			return ib.build() ;
		}
	}
	
	public class SensePair implements Comparable<SensePair> {
		
		private Label.Sense senseA ;
		private Label.Sense senseB ;
		
		private Double avgPriorProbability ;
		private Double maxPriorProbability ;
		private Double minPriorProbability ;
		
		private Double avgGenerality ;
		private Double maxGenerality ;
		private Double minGenerality ;
		
		
		private Double distanceFromBenchmarkRelatedness ;
		private Double distanceFromTopRelatedness ;
		private Double distanceFromTopPriorProbability ;
		
		private Double senseRelatedness ;
		
		private Boolean isValid = null ;
		private Double disambiguationConfidence = null ;
		
		private SensePair(Label.Sense senseA, Label.Sense senseB) throws Exception {
			init(senseA, senseB) ;
		}
		
		private void setIsValid(boolean valid) {
			
			isValid = valid ;
			if (isValid)
				disambiguationConfidence = 1.0 ;
			else
				disambiguationConfidence = 0.0 ;
			
		}
		
		private void predictIsValid() throws ClassMissingException, AttributeMissingException, Exception {
			disambiguationConfidence = senseSelector.getDecisionDistribution(getInstance()).get(true) ;
			isValid = (disambiguationConfidence > 0.5) ;
		}
		
		private void setDistanceFromBenchmarkRelatedness(double distance) {
			distanceFromBenchmarkRelatedness = distance ;
		}
		
		private void setDistanceFromTopRelatedness(double distance) {
			distanceFromTopRelatedness = distance ;
		}
		
		private void setDistanceFromTopPriorProbability(double distance) {
			distanceFromTopPriorProbability = distance ;
		}
		

		
		
		private void init(Label.Sense senseA, Label.Sense senseB) throws Exception {
			
			this.senseA = senseA ;
			this.senseB = senseB ;
			
			maxPriorProbability = Math.max(senseA.getPriorProbability(), senseB.getPriorProbability()) ;
			minPriorProbability = Math.min(senseA.getPriorProbability(), senseB.getPriorProbability()) ;
			avgPriorProbability = (maxPriorProbability+minPriorProbability)/2 ;
			
			if (senseA.getGenerality() != null && senseB.getGenerality() != null) {
				maxGenerality = (double)Math.max(senseA.getGenerality(), senseB.getGenerality()) ;
				minGenerality = (double)Math.min(senseA.getGenerality(), senseB.getGenerality()) ;
				avgGenerality = (maxGenerality+minGenerality)/2 ;
			}
			
			
			senseRelatedness = articleComparer.getRelatedness(senseA, senseB) ;
		}
		
		
		public Double getDisambiguationConfidence() {
			return disambiguationConfidence ;
		}


		public Label.Sense getSenseA() {
			return senseA;
		}


		public Label.Sense getSenseB() {
			return senseB;
		}


		public Double getSenseRelatedness() {
			return senseRelatedness;
		}
		
		protected Instance getInstance() throws ClassMissingException, AttributeMissingException {
			
			InstanceBuilder<SenseAttr,Boolean> ib = senseSelector.getInstanceBuilder() ;
			
			ib.setAttribute(SenseAttr.predictedRelatedness, senseRelatedness) ;
			ib.setAttribute(SenseAttr.avgPriorProbability, avgPriorProbability) ;
			ib.setAttribute(SenseAttr.maxPriorProbability, maxPriorProbability) ;
			ib.setAttribute(SenseAttr.minPriorProbability, minPriorProbability) ;
			
			ib.setAttribute(SenseAttr.avgGenerality, avgGenerality) ;
			ib.setAttribute(SenseAttr.maxGenerality, maxGenerality) ;
			ib.setAttribute(SenseAttr.minGenerality, minGenerality) ;
			
			ib.setAttribute(SenseAttr.distanceFromBenchmarkRelatedness, distanceFromBenchmarkRelatedness) ;
			ib.setAttribute(SenseAttr.distanceFromTopRelatedness, distanceFromTopRelatedness) ;
			ib.setAttribute(SenseAttr.distanceFromTopPriorProbability, distanceFromTopPriorProbability) ;
			
			if (disambiguationConfidence != null)
				ib.setClassAttribute(isValid) ;
			
			return ib.build() ;
		}

		public int compareTo(SensePair sp) {
			
			int cmp = 0 ;
			
			
			
			if (disambiguationConfidence != null && sp.disambiguationConfidence != null) {
				cmp = sp.disambiguationConfidence.compareTo(disambiguationConfidence) ;
				if (cmp != 0)
					return cmp ;
			}
			
			cmp = sp.avgPriorProbability.compareTo(avgPriorProbability) ;
			if (cmp != 0)
				return cmp ;
			
			cmp = senseA.compareTo(sp.senseA) ;
			if (cmp != 0)
				return cmp ;
			
			cmp = senseB.compareTo(sp.senseB) ;
			return cmp ;
		}
		
		@Override
		public String toString() {
			
			DecimalFormat df = new DecimalFormat("0.00") ;
			
			StringBuffer sb = new StringBuffer() ;
			sb.append(senseA) ;
			sb.append(" vs. ") ;
			sb.append(senseB) ;
			
			if (disambiguationConfidence != null) 
				sb.append(" dc:" + df.format(disambiguationConfidence)) ;
			else
				sb.append(" dc:null") ;
			
			
			sb.append(" r:" + df.format(senseRelatedness)) ;
			sb.append(" pp:" + df.format(avgPriorProbability)) ;
			
			sb.append(" distR:" + df.format(this.distanceFromTopRelatedness)) ;
			sb.append(" distPP: " + df.format(this.distanceFromTopPriorProbability)) ;
			
			return sb.toString() ;
		}
	}
}
