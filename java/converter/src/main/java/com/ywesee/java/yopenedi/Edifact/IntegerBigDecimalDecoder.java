package com.ywesee.java.yopenedi.Edifact;

import org.milyn.edisax.model.internal.Delimiters;
import org.milyn.javabean.DataDecodeException;
import org.milyn.javabean.decoders.DABigDecimalDecoder;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class IntegerBigDecimalDecoder extends DABigDecimalDecoder {

    @Override
    public String encode(Object object) throws DataDecodeException {
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.applyPattern("#0");
        return decimalFormat.format(object);
    }

    @Override
    public String encode(Object object, Delimiters interchangeDelimiters) throws DataDecodeException {
        DecimalFormat decimalFormat = new DecimalFormat();

        DecimalFormatSymbols dfs = decimalFormat.getDecimalFormatSymbols();
        decimalFormat.applyPattern("#0");
        if (interchangeDelimiters != null) {
            dfs.setDecimalSeparator(interchangeDelimiters.getDecimalSeparator().charAt(0));
        }

        decimalFormat.setDecimalFormatSymbols(dfs);
        decimalFormat.setParseBigDecimal(true);

        return decimalFormat.format(object);
    }
}
