package com.ainq.izgateway.extract.annotations;
/*
 * Copyright 2020 Audiacious Inquiry, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
import java.lang.annotation.*;

import com.opencsv.bean.validators.StringValidator;

/**
 * Duplicates the interface of PreassignmentValidator but runs during Bean Validation
 * to support conditional validation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldValidator {

    /**
     * Returns the validator that will validate the string.
     *
     * @return The class of the validator that will validate the bean field
     * string value
     */
    Class<? extends StringValidator> validator();

    /**
     * This is used to store additional information needed by the
     * {@link StringValidator}.
     * This could, for example, contain a regular expression that will be
     * applied to the data.
     *
     * @return Parameter string required by the {@link StringValidator}
     */
    String paramString() default "";

    /**
     * This is the list of versions to which the constraint applies
     *
     * @return The list of versions to which this validator applies.
     */
    String[] versions() default "";

    /**
     * The maximum length of the field
     *
     * @return The maximum length of the field.
     */
    int maxLength() default 255;
}

