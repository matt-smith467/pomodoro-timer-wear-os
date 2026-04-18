package com.example.pomodorotimer.presentation

data class SessionState(
    val type: SessionType,
    val cycle: Int,
    val timeLeftSeconds: Long,
)

class SessionManager(
    var workLenMin: Int = 25,
    var shortRestLenMin: Int = 5,
    var longRestLenMin: Int = 15,
) {
    fun nextSession(currentState: SessionState): SessionState {
        val finished = currentState.type
        val cycle = currentState.cycle

        return when {
            finished == SessionType.WORK && cycle >= 4 -> {
                SessionState(SessionType.LONG_REST, cycle, longRestLenMin * 60L)
            }
            finished == SessionType.WORK -> {
                SessionState(SessionType.SHORT_REST, cycle, shortRestLenMin * 60L)
            }
            finished == SessionType.LONG_REST -> {
                SessionState(SessionType.WORK, 1, workLenMin * 60L)
            }
            else -> { // SHORT_REST
                SessionState(SessionType.WORK, cycle + 1, workLenMin * 60L)
            }
        }
    }

    fun getInitialState() = SessionState(SessionType.WORK, 1, workLenMin * 60L)
}
