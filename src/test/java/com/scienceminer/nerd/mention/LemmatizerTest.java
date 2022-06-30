package com.scienceminer.nerd.mention;

import com.scienceminer.nerd.disambiguation.NerdContext;
import com.scienceminer.nerd.service.NerdQuery;
import com.scienceminer.nerd.utilities.StringPos;
import com.scienceminer.nerd.utilities.Utilities;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class ProcessTextTest {
    private Lemmatizer lemmatizer = null;

    @Before
    public void setUp() throws Exception {
        lemmatizer = Lemmatizer.getInstance();
    }

    @Test
    public void testLemmatizerAr() {
        String input = "سوريا العثمانية أو سوريا في العهد العثماني تشير إلى سوريا تحت حكم الدولة العثمانية، وقد دامت هذه الفترة سحابة أربعة قرون منذ أن سحق السلطان سليم الأول جيش المماليك في معركة مرج دابق شمال حلب يوم 24 أغسطس 1516، ومنها ملك مدن البلاد سلمًا وعلى رأسها دمشق في 26 سبتمبر 1516، وحتى انسحاب العثمانيين منها في أعقاب الثورة العربية الكبرى والحرب العالمية الأولى في أكتوبر 1918. في بداية عهدهم، أبقى العثمانيون بلاد الشام ضمن تقسيم إداري واحد، وحتى مع تتالي تعقيد التقسيم الإداري ظلّت الإيالات والولايات تشمل مناطق جغرافيّة هي اليوم بمعظمها تتبع مختلف دول بلاد الشام، أي سوريا ولبنان وفلسطين والأردن، إضافة إلى قسم ضمته تركيا على دفعتين. لذلك فإن الحديث عن سوريا العثمانية يشمل في عديد من المفاصل جميع دول الشام الحاليّة. عرفت البلاد خلال القرنين السادس عشر والسابع عشر ازدهارًا اقتصاديًا وسكانيًا، وساهم في ذلك كون قوافل الحج تجتمع في دمشق لتنطلق إلى الحجاز، وأغلب قوافل التجارة البرية نحو الخليج العربي والعراق تمر من حلب. استمر الوضع الاقتصادي خلال عهد ولاة آل العظم في القرن الثامن عشر جيدًا، لكن عهد الفوضى والحروب الأهلية بين الولاة ساد في ذلك الحين، فضلاً عن النزعات الاستقلالية أمثال ظاهر العمر وأحمد باشا الجزار وفخر الدين المعني الثاني، إلى جانب إرهاق الشعب بالضرائب وهجمات البدو وانعدام الأمن وجور بعض الإقطاع المحليّ. في عام 1831 دخلت البلاد في حكم محمد علي باشا. كان حكمه فيها حكمًا إصلاحيًا من نواحي الإدارة والاقتصاد والتعليم، إلا أن سياسة التجنيد الإجباري التي انتهجها أدت إلى تململ السوريين من حكمه، وقيام ثورات شعبية متتالية ضده بين عامي 1833 و1837، وقد استطاع السلطان عبد المجيد الأول بدعم عسكري من روسيا القيصرية وبريطانيا والنمسا استعادة بلاد الشام في عام 1840. ";

        


    }



}

