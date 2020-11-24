package com.ainq.izgateway.extract.validation;

import java.lang.reflect.Field;
import java.util.Locale;

import com.ainq.izgateway.extract.CVRSExtract;
import com.opencsv.bean.BeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.opencsv.exceptions.CsvValidationException;

/**
 * A simple implementation of BeanField used to support Validation operations
 *
 * @author Keith W. Boone
 */
public class ValidatorBeanField implements BeanField<CVRSExtract, String> {
    private Field field;
    public ValidatorBeanField(Field f) {
        field = f;
    }
    @Override
    public Class<?> getType() {
        // TODO Auto-generated method stub
        return CVRSExtract.class;
    }

    @Override
    public void setType(Class<?> type) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setField(Field field) {
        // TODO Auto-generated method stub

    }

    @Override
    public Field getField() {
        // TODO Auto-generated method stub
        return field;
    }

    @Override
    public boolean isRequired() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setRequired(boolean required) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFieldValue(Object bean, String value, String header) throws CsvDataTypeMismatchException,
        CsvRequiredFieldEmptyException, CsvConstraintViolationException, CsvValidationException {
        // TODO Auto-generated method stub

    }

    @Override
    public Object getFieldValue(Object bean) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object[] indexAndSplitMultivaluedField(Object value, String index) throws CsvDataTypeMismatchException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] write(Object bean, String index)
        throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setErrorLocale(Locale errorLocale) {
        // TODO Auto-generated method stub

    }

    @Override
    public Locale getErrorLocale() {
        return Locale.getDefault();
    }

}