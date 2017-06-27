package com.scienceminer.nerd.evaluation;

import java.util.*;
import java.io.*;
import java.text.*;

import org.grobid.core.utilities.TextUtilities;
import org.grobid.trainer.LabelStat;

import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.utilities.NerdConfig;
import com.scienceminer.nerd.training.*;
import com.scienceminer.nerd.kb.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class EvaluationUtil {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationUtil.class);

	public static LabelStat evaluate(ArticleTrainingSample testSet, List<LabelStat> stats) throws Exception {	
		DecimalFormat format = new DecimalFormat("#0.00");

		double accumulatedRecall = 0.0;
		double accumulatedPrecision = 0.0;
		double accumulatedF1Score = 0.0;

		double lowerPrecision = 1;
		double lowerRecall = 1;
		double lowerF1Score = 1;
		
		int perfectRecall = 0;
		int perfectPrecision = 0;
		
		LabelStat globalStats = new LabelStat();
		int i = 0; 
		for (Article article : testSet.getSample()) {
			if (i == testSet.size())
				break;
			LabelStat localStats = stats.get(i);
			
			globalStats.incrementObserved(localStats.getObserved());
			globalStats.incrementExpected(localStats.getExpected());
			globalStats.incrementFalsePositive(localStats.getFalsePositive());
			globalStats.incrementFalseNegative(localStats.getFalseNegative());

			accumulatedRecall += localStats.getRecall();
			accumulatedPrecision += localStats.getPrecision();
			accumulatedF1Score += localStats.getF1Score();

System.out.println(localStats.toString());
System.out.println("local recall: " + localStats.getRecall());
System.out.println("local precision: " + localStats.getPrecision());
System.out.println("local f1: " + localStats.getF1Score());

			lowerPrecision = Math.min(lowerPrecision, localStats.getPrecision());
			lowerRecall = Math.min(lowerRecall, localStats.getRecall());
			
			if (localStats.getPrecision() == 1.0) 
				perfectPrecision++;
			if (localStats.getRecall() == 1) 
				perfectRecall++;

			i++;
		}

		double microAveragePrecision = 0.0;
		double microAverageRecall = 0.0;
		double microAverageF1Score = 0.0;

		double macroAveragePrecision = 0.0;
		double macroAverageRecall = 0.0;
		double macroAverageF1Score = 0.0;

		StringBuilder builder = new StringBuilder();

		builder.append("\nEvaluation on " + testSet.size() + " articles ");

		builder.append("-- Macro-average --\n");
		builder.append("precision: ").append(format.format(accumulatedPrecision / testSet.size())).append("\n");
		builder.append("recall: ").append(format.format(accumulatedRecall / testSet.size())).append("\n");
		builder.append("f1-score: ").append(format.format(accumulatedF1Score / testSet.size())).append("\n\n");

		builder.append("-- Micro-average --\n");
		builder.append("precision: ").append(format.format(globalStats.getPrecision())).append("\n");
		builder.append("recall: ").append(format.format(globalStats.getRecall())).append("\n");
		builder.append("f1-score: ").append(format.format(globalStats.getF1Score())).append("\n\n");		

		builder.append("lower precision in evaluation set: ").append(format.format(lowerPrecision)).append("\n");
		builder.append("lower recall in evalution set : ").append(format.format(lowerRecall)).append("\n");
		builder
			.append("perfect precision in evaluation set: ")
			.append(format.format((double)perfectPrecision / 100))
			.append("\n");
		builder
			.append("perfect recall in evaluation set: ")
			.append(format.format((double)perfectPrecision / 100))
			.append("\n");
		
		System.out.println(builder.toString());

		return globalStats;
	}

}