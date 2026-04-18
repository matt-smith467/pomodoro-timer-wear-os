package com.example.pomodorotimer.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionManagerTest {

    private val sessionManager = SessionManager(
        workLenMin = 25,
        shortRestLenMin = 5,
        longRestLenMin = 15
    )

    @Test
    fun `initial state is work session cycle 1`() {
        val state = sessionManager.getInitialState()
        assertEquals(SessionType.WORK, state.type)
        assertEquals(1, state.cycle)
        assertEquals(25 * 60L, state.timeLeftSeconds)
    }

    @Test
    fun `work session cycle 1 ends, next is short break cycle 1`() {
        val currentState = SessionState(SessionType.WORK, 1, 0)
        val nextState = sessionManager.nextSession(currentState)
        
        assertEquals(SessionType.SHORT_REST, nextState.type)
        assertEquals(1, nextState.cycle)
        assertEquals(5 * 60L, nextState.timeLeftSeconds)
    }

    @Test
    fun `short break cycle 1 ends, next is work session cycle 2`() {
        val currentState = SessionState(SessionType.SHORT_REST, 1, 0)
        val nextState = sessionManager.nextSession(currentState)
        
        assertEquals(SessionType.WORK, nextState.type)
        assertEquals(2, nextState.cycle)
        assertEquals(25 * 60L, nextState.timeLeftSeconds)
    }

    @Test
    fun `work session cycle 4 ends, next is long break cycle 4`() {
        val currentState = SessionState(SessionType.WORK, 4, 0)
        val nextState = sessionManager.nextSession(currentState)
        
        assertEquals(SessionType.LONG_REST, nextState.type)
        assertEquals(4, nextState.cycle)
        assertEquals(15 * 60L, nextState.timeLeftSeconds)
    }

    @Test
    fun `long break cycle 4 ends, next is work session cycle 1`() {
        val currentState = SessionState(SessionType.LONG_REST, 4, 0)
        val nextState = sessionManager.nextSession(currentState)
        
        assertEquals(SessionType.WORK, nextState.type)
        assertEquals(1, nextState.cycle)
        assertEquals(25 * 60L, nextState.timeLeftSeconds)
    }
}
