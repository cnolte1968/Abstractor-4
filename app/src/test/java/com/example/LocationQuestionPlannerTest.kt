package com.example

import com.example.domain.engine.location.DataSourceType
import com.example.domain.engine.location.LocationQuestionPlanner
import com.example.domain.engine.location.QuestionCategory
import org.junit.Assert.*
import org.junit.Test

class LocationQuestionPlannerTest {

    @Test
    fun test1_Stosszeiten() {
        val plan = LocationQuestionPlanner.planExecution("Wann ist hier am meisten los?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.STOSSZEITEN, plan!!.primaryCategory)
    }

    @Test
    fun test2_ZugangBergsehenswuerdigkeit() {
        val plan = LocationQuestionPlanner.planExecution("Wie ist der Weg zum Berg?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.ZUGANG_MOBILITAET, plan!!.primaryCategory)
    }

    @Test
    fun test3_Barrierefreiheit() {
        val plan = LocationQuestionPlanner.planExecution("Gibt es hier einen Aufzug für Rollstühle?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.BARRIEREFREIHEIT, plan!!.primaryCategory)
    }

    @Test
    fun test4_Parken() {
        val plan = LocationQuestionPlanner.planExecution("Wo kann ich mein Auto parken?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.PARKEN, plan!!.primaryCategory)
    }

    @Test
    fun test5_Atmosphaere() {
        val plan = LocationQuestionPlanner.planExecution("Lohnt es sich bei Regen zu fotografieren?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.ATMOSPHAERE_AUSSTATTUNG, plan!!.primaryCategory)
    }

    @Test
    fun test6_FamilienUndKinderwagen() {
        val plan = LocationQuestionPlanner.planExecution("Können wir da mit dem Kinderwagen rein?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.FAMILIEN_KINDER, plan!!.primaryCategory)
    }

    @Test
    fun test7_Historie() {
        val plan = LocationQuestionPlanner.planExecution("Wann wurde das Gebäude gebaut?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.HISTORIE_KULTUR, plan!!.primaryCategory)
    }

    @Test
    fun test8_HeutigeOeffnungszeiten() {
        val plan = LocationQuestionPlanner.planExecution("Wann ist heute geöffnet?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.PREISE_OEFFNUNGSZEITEN, plan!!.primaryCategory)
        assertTrue(plan.requiresGrounding)
    }

    @Test
    fun test9_AktuellePreise() {
        val plan = LocationQuestionPlanner.planExecution("Was kostet der Eintritt aktuell?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.PREISE_OEFFNUNGSZEITEN, plan!!.primaryCategory)
        assertTrue(plan.requiresGrounding)
    }

    @Test
    fun test10_SaisonaleSperrung() {
        val plan = LocationQuestionPlanner.planExecution("Gibt es eine Sperrung im Winter?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.SAISON_EVENTS, plan!!.primaryCategory)
        assertTrue(plan.requiresGrounding)
    }

    @Test
    fun test11_StatischeFrageOhneGrounding() {
        val plan = LocationQuestionPlanner.planExecution("Wie ist die Geschichte des Ortes?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.HISTORIE_KULTUR, plan!!.primaryCategory)
        assertFalse(plan.requiresGrounding)
    }

    @Test
    fun test12_ZeitbegriffAktiviertGrounding() {
        val plan = LocationQuestionPlanner.planExecution("Wie ist die Atmosphäre derzeit?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.ATMOSPHAERE_AUSSTATTUNG, plan!!.primaryCategory)
        assertTrue(plan.requiresGrounding)
    }
    
    @Test
    fun test12b_JahreszahlAktiviertGrounding() {
        val plan = LocationQuestionPlanner.planExecution("Welche Ausstellung läuft 2026?")
        assertNotNull(plan)
        assertTrue(plan!!.requiresGrounding)
    }

    @Test
    fun test13_LeereFrage() {
        assertNull(LocationQuestionPlanner.planExecution("   "))
        assertNull(LocationQuestionPlanner.planExecution(""))
    }

    @Test
    fun test14_ReinerSmalltalk() {
        assertNull(LocationQuestionPlanner.planExecution("hallo"))
        assertNull(LocationQuestionPlanner.planExecution("wie geht es"))
    }

    @Test
    fun test15_OrtsfremdeFrage() {
        assertNull(LocationQuestionPlanner.planExecution("Wie backt man einen Kuchen?"))
        assertNull(LocationQuestionPlanner.planExecution("Wer ist der Bundeskanzler?"))
    }

    @Test
    fun test16_IndirekteGueltigeOrtsfrage() {
        val plan1 = LocationQuestionPlanner.planExecution("Ist das für ältere Menschen geeignet?")
        assertNotNull(plan1)
        val plan2 = LocationQuestionPlanner.planExecution("Lohnt es sich bei Regen?")
        assertNotNull(plan2)
    }

    @Test
    fun test17_UnbekannteFrageErgibtSonstige() {
        val plan = LocationQuestionPlanner.planExecution("Wie riecht es dort?")
        assertNotNull(plan)
        assertEquals(QuestionCategory.SONSTIGE, plan!!.primaryCategory)
    }

    @Test
    fun test18_KorrektePflichtquellenJeKategorie() {
        val plan = LocationQuestionPlanner.planExecution("Wann wurde das gebaut?")
        assertTrue(plan!!.requiredSources.contains(DataSourceType.WIKIPEDIA))
        assertTrue(plan.requiredSources.contains(DataSourceType.PLACES))
    }

    @Test
    fun test19_KorrekteOptionaleQuellen() {
        val plan = LocationQuestionPlanner.planExecution("Gibt es Parkplätze?")
        assertTrue(plan!!.requiredSources.contains(DataSourceType.LOCATION_CONTEXT))
    }

    @Test
    fun test20_DeterministischeWiederholung() {
        val plan1 = LocationQuestionPlanner.planExecution("Gibt es Parkplätze?")
        val plan2 = LocationQuestionPlanner.planExecution("Gibt es Parkplätze?")
        assertEquals(plan1, plan2)
    }
}
