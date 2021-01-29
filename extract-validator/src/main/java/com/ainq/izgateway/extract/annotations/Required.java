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
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate when an item is Required, Optional, or Required If Known, or should
 * not be sent for a specific event.
 *
 * Usage:
 * \@Requirement(value=RequirementType.REQUIRED, when=EventType.VACCINATION)
 * \@Requirement(value=RequirementType.DO_NOT_SEND, when=EventType.MISSED_APPOINTMENT)
 * @author Keith W. Boone
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface Required {
//    EventType[] when() default {};
//    EventType[] whenv1() default {};
//    boolean versioned() default false;
}
