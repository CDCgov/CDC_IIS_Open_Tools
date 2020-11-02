package com.ainq.izgateway.extract.annotations;

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
    EventType[] when() default {};
    EventType[] whenv1() default {};
    boolean versioned() default false;
}
