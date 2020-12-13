package com.ainq.izgateway.extract.validation;
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
import java.util.Set;

/**
 * This interface is used to mark a validator as being suppressible,
 * and provides the basic interface to tell the validator what errors
 * to suppress or determine which errors are being suppressed.
 *
 * @author Keith W. Boone
 *
 */
public interface Suppressible {
    /**
     * This method will be called after the validator is constructed
     * before any parameters are set to indicate which errors should
     * be suppressed during validation.  This enables validators to
     * have dynamic behavior based on system configuration.
     *
     * @param suppressed    A list of error codes to be suppressed.
     */
    void setSuppressed(Set<String> suppressed);

    /**
     * This method may be used by a Validator to determine which
     * errors should be suppressed.
     *
     * @return  The list of error codes that should be suppressed.
     */
    Set<String> getSuppressed();
}
