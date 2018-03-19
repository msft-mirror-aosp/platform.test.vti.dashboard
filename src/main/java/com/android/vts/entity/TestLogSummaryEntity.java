/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.vts.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

import static com.googlecode.objectify.ObjectifyService.ofy;

/** Entity Class for saving Test Log Summary */
@Cache
@Entity
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
public class TestLogSummaryEntity {

    /** Test Log Summary id */
    @Id Long id;

    /** Test Log Summary branch information */
    @Index @Getter @Setter String branch;

    /** Test Log Summary build target information */
    @Index @Getter @Setter String target;

    /** Test Log Summary build ID information */
    @Index @Getter @Setter String build_id;

    /** Test Log Summary system finrgerprint information */
    @Getter @Setter String system_fingerprint;

    /** Test Log Summary vendor fingerprint information */
    @Getter @Setter String vendor_fingerprint;

    /** Test Log Summary test count for success information */
    @Index @Getter @Setter int pass;

    /** Test Log Summary test count for failure information */
    @Index @Getter @Setter int fail;

    /** When this record was created or updated */
    @Index @Getter Date updated;

    /** Construction function for TestLogSummaryEntity Class */
    public TestLogSummaryEntity(
            Long id,
            String branch,
            String target,
            String build_id,
            String system_fingerprint,
            String vendor_fingerprint,
            int pass,
            int fail) {
        this.id = id;
        this.branch = branch;
        this.target = target;
        this.build_id = build_id;
        this.system_fingerprint = system_fingerprint;
        this.vendor_fingerprint = vendor_fingerprint;
        this.pass = pass;
        this.fail = fail;
    }

    /** Saving function for the instance of this class */
    private void save() {
        this.updated = new Date();
        ofy().defer().save().entity(this);
    }
}
