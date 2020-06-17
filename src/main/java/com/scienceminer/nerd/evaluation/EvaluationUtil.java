package com.scienceminer.nerd.evaluation;

import java.util.*;
import java.io.*;
import java.text.*;

import org.grobid.core.utilities.TextUtilities;
import org.grobid.trainer.evaluation.LabelStat;

import com.scienceminer.nerd.exceptions.*;
import com.scienceminer.nerd.disambiguation.NerdEntity;
import com.scienceminer.nerd.utilities.*;
import com.scienceminer.nerd.training.*;
import com.scienceminer.nerd.kb.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluationUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationUtil.class);

	public static LabelStat evaluate(ArticleTrainingSample testSet, List<LabelStat> stats) throws Exception {	
		//DecimalFormat format = new DecimalFormat("#0.00");

		double accumulatedRecall = 0.0;
		double accumulatedPrecision = 0.0;
		double accumulatedF1Score = 0.0;

		double lowerPrecision = 1.0;
		double lowerRecall = 1.0;
		double lowerF1Score = 1.0;
		
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

/*System.out.println(localStats.toString());
System.out.println("local recall: " + localStats.getRecall());
System.out.println("local precision: " + localStats.getPrecision());
System.out.println("local f1: " + localStats.getF1Score());*/

			lowerPrecision = Math.min(lowerPrecision, localStats.getPrecision());
			lowerRecall = Math.min(lowerRecall, localStats.getRecall());
			
			if (localStats.getPrecision() == 1.0) 
				perfectPrecision++;
			if (localStats.getRecall() == 1.0)
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

		builder.append("\n\n-- Macro-average --\n");
		builder.append("precision: ").append(TextUtilities.formatFourDecimals(accumulatedPrecision / testSet.size())).append("\n");
		builder.append("recall: ").append(TextUtilities.formatFourDecimals(accumulatedRecall / testSet.size())).append("\n");
		builder.append("f1-score: ").append(TextUtilities.formatFourDecimals(accumulatedF1Score / testSet.size())).append("\n\n");

		builder.append("-- Micro-average --\n");
		builder.append("precision: ").append(TextUtilities.formatFourDecimals(globalStats.getPrecision())).append("\n");
		builder.append("recall: ").append(TextUtilities.formatFourDecimals(globalStats.getRecall())).append("\n");
		builder.append("f1-score: ").append(TextUtilities.formatFourDecimals(globalStats.getF1Score())).append("\n\n");		

		builder.append("lower precision in evaluation set: ").append(TextUtilities.formatFourDecimals(lowerPrecision)).append("\n");
		builder.append("lower recall in evalution set : ").append(TextUtilities.formatFourDecimals(lowerRecall)).append("\n");
		builder
			.append("perfect precision in evaluation set: ")
			.append(perfectPrecision)
			.append(" / ")
			.append(testSet.size())
			.append("\n");
		builder
			.append("perfect recall in evaluation set: ")
			.append(perfectRecall)
			.append(" / ")
			.append(testSet.size())
			.append("\n");
		
		System.out.println(builder.toString());

		return globalStats;
	}

}