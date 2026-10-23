package gradlelens.dependencylens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FindingsRefreshTest {
    @Test
    fun recognisesChangesThatCanAffectFindings() {
        assertTrue(affectsFindings("/repo/app/build/dependency-lens/js/main/findings.tsv"))
        assertTrue(affectsFindings("/repo/app/build/dependency-lens"))
        assertTrue(affectsFindings("/repo/app/build"))
        assertFalse(affectsFindings("/repo/app/build/classes"))
        assertFalse(affectsFindings("/repo/app/build.gradle.kts"))
    }
}
