package com.android.vts.entity;

import com.android.vts.util.ObjectifyTestBase;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.factory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CodeCoverageEntityTest extends ObjectifyTestBase {

    @Test
    public void saveTest() {
        factory().register(CodeCoverageEntity.class);

        Key testParentKey = Key.create(TestEntity.class, "test1");
        Key testRunParentKey = Key.create(testParentKey, TestRunEntity.class, 1);

        CodeCoverageEntity codeCoverageEntity = new CodeCoverageEntity(testRunParentKey, 1000, 3500);
        codeCoverageEntity.save();

        assertEquals(codeCoverageEntity.getCoveredLineCount(), 1000);
        assertEquals(codeCoverageEntity.getTotalLineCount(), 3500);
    }

    @Test
    public void getUrlSafeKeyTest() {
        factory().register(CodeCoverageEntity.class);

        Key testParentKey = Key.create(TestEntity.class, "test1");
        Key testRunParentKey = Key.create(testParentKey, TestRunEntity.class, 1);

        CodeCoverageEntity codeCoverageEntity = new CodeCoverageEntity(testRunParentKey, 1000, 3500);
        codeCoverageEntity.save();

        String urlKey =
                "kind%3A+%22Test%22%0A++name%3A+%22test1%22%0A%7D%0Apath+%7B%0A++kind%3A+%22TestRun%22%0A++id%3A+1%0A%7D%0Apath+%7B%0A++kind%3A+%22CodeCoverage%22%0A++id%3A+1%0A%7D%0A";
        assertTrue(codeCoverageEntity.getUrlSafeKey().endsWith(urlKey));
    }

}
